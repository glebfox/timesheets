/*
 * Copyright 2026 Gleb Gorelov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tells the current user's part in a project, by the role type of their participation in it.
 * <p>
 * The user is always the effective user, so under user substitution it is the substituted user. The participants are
 * read without the user's own permissions, as the answer must not depend on what the user may read. Soft deletion
 * applies to the participants and their roles: a deleted one grants nothing.
 * <p>
 * Every answer is a count in a value query, never an entity load, because row-level predicates call these methods
 * inside a save. A value query has no fetch plan, so it runs with the commit flush mode and without the refresh hint
 * ({@code JmixEclipseLinkQuery}): it reads the stored participants without flushing the rows the save holds, which
 * would clear the change tracking the predicates read, and without refreshing them.
 * <p>
 * An unknown project, a {@code null} id, gets the answer {@code false}, never an exception: the predicates pass
 * whatever the entity they judge holds, and an entity whose reference is not set has no id for it.
 */
@Component("ts_ProjectAccessSupport")
public class ProjectAccessSupport {

    private static final String MANAGER =
            "@enum(com.glebfox.timesheets.entity.project.ProjectRoleType.MANAGER)";
    private static final String OBSERVER =
            "@enum(com.glebfox.timesheets.entity.project.ProjectRoleType.OBSERVER)";

    private final UnconstrainedDataManager unconstrainedDataManager;
    private final CurrentUserSubstitution currentUserSubstitution;

    public ProjectAccessSupport(UnconstrainedDataManager unconstrainedDataManager,
                                CurrentUserSubstitution currentUserSubstitution) {
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.currentUserSubstitution = currentUserSubstitution;
    }

    /**
     * Tells whether the current user is a participant of the project with a role of type Manager.
     *
     * @param projectId the id of the project, or {@code null} for an unknown one
     * @return {@code true} if the current user manages the project; {@code false} for an unknown project
     */
    public boolean managesProject(@Nullable UUID projectId) {
        return participates(projectId, "p.role.type = " + MANAGER);
    }

    /**
     * Tells whether the current user is a participant of the project with a role of any type but Observer.
     *
     * @param projectId the id of the project, or {@code null} for an unknown one
     * @return {@code true} if the current user works on the project; {@code false} for an unknown project
     */
    public boolean worksOn(@Nullable UUID projectId) {
        return participates(projectId, "p.role.type <> " + OBSERVER);
    }

    private boolean participates(@Nullable UUID projectId, String roleCondition) {
        if (projectId == null) {
            return false;
        }
        Long count = unconstrainedDataManager.loadValue("select count(p) from ts_ProjectParticipant p " +
                        "where p.project.id = :projectId and p.user.id = :userId and " + roleCondition, Long.class)
                .parameter("projectId", projectId)
                .parameter("userId", userId())
                .one();
        return count > 0;
    }

    private UUID userId() {
        return ((User) currentUserSubstitution.getEffectiveUser()).getId();
    }
}

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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.app.ProjectDataAccessSupport;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import io.jmix.security.model.RowLevelBiPredicate;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;
import org.springframework.context.ApplicationContext;

/**
 * Narrows project data and time entries to the projects where the person takes part, and lets them change project
 * data only where they are the project's Manager. It includes the write rules of time entries,
 * {@link TimeEntryRulesRole}.
 * <p>
 * Reading is narrowed by JPQL {@code where} clauses only, as the AI datasets accept no {@code join} and no predicate
 * read policy; their subqueries use aliases of their own, never those of the screens' queries. The write rules are in
 * {@link ProjectDataAccessSupport}.
 */
@RowLevelRole(name = "Project scope", code = ProjectScopeRole.CODE)
public interface ProjectScopeRole extends TimeEntryRulesRole {

    String CODE = "project-scope";

    @JpqlRowLevelPolicy(entityClass = Project.class,
            where = "exists (select sp from ts_ProjectParticipant sp"
                    + " where sp.user.id = :current_user_id and sp.project.id = {E}.id)")
    @JpqlRowLevelPolicy(entityClass = Task.class,
            where = "exists (select sp from ts_ProjectParticipant sp"
                    + " where sp.user.id = :current_user_id and sp.project.id = {E}.project.id)")
    @JpqlRowLevelPolicy(entityClass = ProjectParticipant.class,
            where = "exists (select sp from ts_ProjectParticipant sp"
                    + " where sp.user.id = :current_user_id and sp.project.id = {E}.project.id)")
    @JpqlRowLevelPolicy(entityClass = TagGroup.class,
            where = "exists (select sp from ts_ProjectParticipant sp"
                    + " where sp.user.id = :current_user_id and sp.project.id = {E}.project.id)")
    @JpqlRowLevelPolicy(entityClass = Tag.class,
            where = "exists (select sp from ts_ProjectParticipant sp"
                    + " where sp.user.id = :current_user_id and sp.project.id = {E}.tagGroup.project.id)")
    void projectDataRead();

    // The person's own entries, and every entry of a project where they are a Manager, an Approver, or an Observer.
    // The user and the task are named by their own paths, {E}.user and {E}.task, and navigated only inside the
    // subqueries: a path through them, even {E}.user.id, turns a load's fetch of the reference into an inner join with
    // the soft deletion filter, so the entries of a deleted person, or the person's own entries of a deleted task,
    // would drop out; a condition on the reference's own path makes Jmix fetch it in a batch instead
    @JpqlRowLevelPolicy(entityClass = TimeEntry.class,
            where = "{E}.user in (select su from ts_User su where su.id = :current_user_id)"
                    + " or {E}.task in (select st from ts_Task st"
                    + " where exists (select sp from ts_ProjectParticipant sp"
                    + " where sp.user.id = :current_user_id and sp.project.id = st.project.id"
                    + " and sp.role.type in (@enum(com.glebfox.timesheets.entity.project.ProjectRoleType.MANAGER),"
                    + " @enum(com.glebfox.timesheets.entity.project.ProjectRoleType.APPROVER),"
                    + " @enum(com.glebfox.timesheets.entity.project.ProjectRoleType.OBSERVER))))")
    void timeEntryRead();

    @PredicateRowLevelPolicy(entityClass = Project.class, actions = RowLevelPolicyAction.UPDATE)
    default RowLevelBiPredicate<Project, ApplicationContext> projectUpdate() {
        return (project, applicationContext) ->
                applicationContext.getBean(ProjectDataAccessSupport.class).mayChange(project);
    }

    @PredicateRowLevelPolicy(entityClass = Task.class,
            actions = {RowLevelPolicyAction.CREATE, RowLevelPolicyAction.UPDATE, RowLevelPolicyAction.DELETE})
    default RowLevelBiPredicate<Task, ApplicationContext> taskChange() {
        return (task, applicationContext) ->
                applicationContext.getBean(ProjectDataAccessSupport.class).mayChange(task);
    }

    @PredicateRowLevelPolicy(entityClass = ProjectParticipant.class,
            actions = {RowLevelPolicyAction.CREATE, RowLevelPolicyAction.UPDATE, RowLevelPolicyAction.DELETE})
    default RowLevelBiPredicate<ProjectParticipant, ApplicationContext> participantChange() {
        return (participant, applicationContext) ->
                applicationContext.getBean(ProjectDataAccessSupport.class).mayChange(participant);
    }

    @PredicateRowLevelPolicy(entityClass = TagGroup.class,
            actions = {RowLevelPolicyAction.CREATE, RowLevelPolicyAction.UPDATE, RowLevelPolicyAction.DELETE})
    default RowLevelBiPredicate<TagGroup, ApplicationContext> tagGroupChange() {
        return (tagGroup, applicationContext) ->
                applicationContext.getBean(ProjectDataAccessSupport.class).mayChange(tagGroup);
    }

    @PredicateRowLevelPolicy(entityClass = Tag.class,
            actions = {RowLevelPolicyAction.CREATE, RowLevelPolicyAction.UPDATE, RowLevelPolicyAction.DELETE})
    default RowLevelBiPredicate<Tag, ApplicationContext> tagChange() {
        return (tag, applicationContext) ->
                applicationContext.getBean(ProjectDataAccessSupport.class).mayChange(tag);
    }
}

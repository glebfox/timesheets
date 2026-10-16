/*
 * Copyright 2024 Gleb Gorelov
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

import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.tag.TagGroup;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility class for managing and manipulating {@link Project} entities.
 */
@Component("ts_ProjectSupport")
public class ProjectSupport {

    private final DataManager dataManager;

    public ProjectSupport(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Retrieves a list of all available project roles.
     *
     * @return a list of {@link ProjectRole} instances, representing all project roles
     */
    public List<ProjectRole> getProjectRoles() {
        return dataManager.load(ProjectRole.class)
                .all()
                .list();
    }

    /**
     * Retrieves a list of tag groups associated with the specified project.
     * The returned list is ordered by the name of the tag groups.
     *
     * @param project the project for which to retrieve the associated tag groups
     * @return a list of {@link TagGroup} instances linked to the given project,
     * along with their associated tags
     */
    public List<TagGroup> getProjectTagGroups(Project project) {
        return dataManager.load(TagGroup.class)
                .query("select e from ts_TagGroup e where e.project = :project order by e.name")
                .parameter("project", project)
                .fetchPlan(fetchPlanBuilder ->
                        fetchPlanBuilder
                                .addFetchPlan(FetchPlan.INSTANCE_NAME)
                                .add("tags", FetchPlan.INSTANCE_NAME))
                .list();
    }
}
/*
 * Copyright 2023 Gleb Gorelov
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

package com.glebfox.timesheets.view.projectrole;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.data.renderer.BadgeRender;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "project-roles", layout = MainView.class)
@ViewController("ts_ProjectRole.list")
@ViewDescriptor("project-role-list-view.xml")
@LookupComponent("projectRolesDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class ProjectRoleListView extends StandardListView<ProjectRole> {

    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ComponentSupport componentSupport;

    @Supply(to = "projectRolesDataGrid.type", subject = "renderer")
    private Renderer<ProjectRole> projectRolesDataGridTypeRenderer() {
        return new BadgeRender<>(
                projectRole -> metadataTools.format(projectRole.getType()),
                componentSupport.createProjectRoleTypeThemeProvider(ProjectRole::getType)
        );
    }
}
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

package com.glebfox.timesheets.view.project;

import com.glebfox.timesheets.data.renderer.BadgeRender;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "projects", layout = MainView.class)
@ViewController("ts_Project.list")
@ViewDescriptor("project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class ProjectListView extends StandardListView<Project> {

    @Autowired
    private MetadataTools metadataTools;

    @Supply(to = "projectsDataGrid.status", subject = "renderer")
    private Renderer<Project> projectsDataGridStatusRenderer() {
        return new BadgeRender<>(
                project -> metadataTools.format(project.getStatus()),
                project -> switch (project.getStatus()) {
                    case OPEN -> "success";
                    case CLOSED -> "error";
                }
        );
    }
}
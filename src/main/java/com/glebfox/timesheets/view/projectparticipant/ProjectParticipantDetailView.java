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

package com.glebfox.timesheets.view.projectparticipant;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "project-participants/:id", layout = MainView.class)
@ViewController("ts_ProjectParticipant.detail")
@ViewDescriptor("project-participant-detail-view.xml")
@EditedEntityContainer("projectParticipantDc")
@DialogMode(width = "480px", closeOnEsc = true)
public class ProjectParticipantDetailView extends StandardDetailView<ProjectParticipant> {

    @ViewComponent
    private EntityPicker<Project> projectField;
    @ViewComponent
    private EntityComboBox<ProjectRole> roleField;

    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ComponentSupport componentSupport;

    @Supply(to = "userField", subject = "renderer")
    private Renderer<User> userFieldRenderer() {
        return componentSupport.createUserRenderer();
    }

    @Supply(to = "roleField", subject = "renderer")
    private Renderer<ProjectRole> roleFieldRenderer() {
        String template = "${item.name}" +
                "<div class=\"secondary-text\">" +
                "  ${item.type}" +
                "</div>";

        return LitRenderer.<ProjectRole>of(template)
                .withProperty("name", ProjectRole::getName)
                .withProperty("type", projectRole ->
                        metadataTools.format(projectRole.getType()));
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // in case we open a view for an entity creation by direct navigation
        projectField.setVisible(getEditedEntity().getProject() == null);
    }

    @Subscribe("roleField.entityCreate")
    public void onRoleFieldEntityCreate(final ActionPerformedEvent event) {
        dialogWindows.detail(roleField)
                .newEntity()
                .open();
    }
}
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

import com.glebfox.timesheets.app.ProjectSupport;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.data.renderer.BadgeRender;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.action.BaseAction;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "projects/:id", layout = MainView.class)
@ViewController("ts_Project.detail")
@ViewDescriptor("project-detail-view.xml")
@EditedEntityContainer("projectDc")
@DialogMode(width = "1024px", closeOnEsc = true)
public class ProjectDetailView extends StandardDetailView<Project> {

    @ViewComponent
    private JmixSelect<ProjectStatus> statusField;
    @ViewComponent
    private DataGrid<Task> tasksDataGrid;
    @ViewComponent
    private DropdownButton addParticipantsDropdown;

    @ViewComponent
    private CollectionPropertyContainer<ProjectParticipant> participantsDc;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ProjectSupport projectSupport;
    @Autowired
    private ComponentSupport componentSupport;

    @Subscribe
    public void onInit(final InitEvent event) {
        initAddParticipantsDropdown();
    }

    private void initAddParticipantsDropdown() {
        projectSupport.getProjectRoles().stream()
                .map(AddParticipantsAction::new)
                .forEach(action ->
                        addParticipantsDropdown.addItem(action.getId() + "Item", action));
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        updateStatusFieldIcon();
    }

    @Supply(to = "tasksDataGrid.status", subject = "renderer")
    private Renderer<Task> tasksDataGridStatusRenderer() {
        return new BadgeRender<>(
                task -> metadataTools.format(task.getStatus()),
                task -> switch (task.getStatus()) {
                    case ACTIVE -> BadgeStyle.SUCCESS;
                    case INACTIVE -> BadgeStyle.ERROR;
                }
        );
    }

    @Supply(to = "participantsDataGrid.role", subject = "renderer")
    private Renderer<ProjectParticipant> participantsDataGridRoleRenderer() {
        return new BadgeRender<>(
                projectParticipant -> metadataTools.format(projectParticipant.getRole()),
                componentSupport.createProjectRoleTypeStyleProvider(projectParticipant ->
                        projectParticipant.getRole().getType())
        );
    }

    @Subscribe("statusField")
    public void onStatusFieldValueChange(final ComponentValueChangeEvent<JmixSelect<ProjectStatus>, ProjectStatus> event) {
        updateStatusFieldIcon();
    }

    private void updateStatusFieldIcon() {
        ProjectStatus status = statusField.getValue();
        Icon icon = status == null ? null : switch (status) {
            case OPEN -> {
                Icon openIcon = GravityUiIcon.CREDIT_CARD.create();
                openIcon.setColor("var(--aura-green)");
                yield openIcon;
            }
            case CLOSED -> {
                Icon closedIcon = GravityUiIcon.XMARK.create();
                closedIcon.setColor("var(--aura-red)");
                yield closedIcon;
            }
        };

        statusField.setPrefixComponent(icon);
    }

    @Supply(to = "clientField", subject = "renderer")
    private Renderer<Client> roleFieldRenderer() {
        return new ComponentRenderer<>(client -> {
            FlexLayout wrapper = new FlexLayout();
            wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
            wrapper.setClassName("client-field-renderer");

            String clientName = metadataTools.getInstanceName(client);

            wrapper.add(
                    componentSupport.createAvatar(clientName, client.getImage(), "client-logo"),
                    new Text(clientName)
            );

            return wrapper;
        });
    }

    @Subscribe("tasksDataGrid.taskClose")
    public void onTasksDataGridTaskClose(final ActionPerformedEvent event) {
        Task selectedItem = tasksDataGrid.getSingleSelectedItem();
        if (selectedItem == null
                || TaskStatus.INACTIVE == selectedItem.getStatus()) {
            return;
        }

        selectedItem.setStatus(TaskStatus.INACTIVE);
    }

    @Supply(to = "participantsDataGrid.user", subject = "renderer")
    private Renderer<ProjectParticipant> participantsDataGridUserRenderer() {
        return componentSupport.createUserRenderer(ProjectParticipant::getUser);
    }

    private class AddParticipantsAction extends BaseAction {

        private final ProjectRole projectRole;

        public AddParticipantsAction(ProjectRole projectRole) {
            super("Add" + projectRole.getName() + "Action");
            this.projectRole = projectRole;

            setText(metadataTools.getInstanceName(projectRole));
        }

        @Override
        public void actionPerform(Component component) {
            DialogWindow<View<?>> dialog = dialogWindows.lookup(ProjectDetailView.this, User.class)
                    .withSelectHandler(users -> {
                        for (User user : users) {
                            ProjectParticipant projectParticipant = dataManager.create(ProjectParticipant.class);
                            projectParticipant.setUser(user);
                            projectParticipant.setProject(getEditedEntity());
                            projectParticipant.setRole(projectRole);
                            participantsDc.getMutableItems().add(projectParticipant);
                        }
                    })
                    .build();

            View<?> view = dialog.getView();
            if (view instanceof MultiSelectLookupView multiSelectLookupView) {
                multiSelectLookupView.setLookupComponentMultiSelect(true);
            }

            dialog.open();
        }
    }
}
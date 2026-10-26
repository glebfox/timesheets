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

package com.glebfox.timesheets.view.user;

import com.glebfox.timesheets.app.WorkScheduleAssignmentSupport;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleAssignmentDetailView;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.Sort;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionChangeType;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

@Route(value = "users/:id", layout = MainView.class)
@ViewController("ts_User.detail")
@ViewDescriptor("user-detail-view.xml")
@EditedEntityContainer("userDc")
@DialogMode(width = "640px", closeOnEsc = true)
public class UserDetailView extends StandardDetailView<User> {

    @ViewComponent
    private TypedTextField<String> usernameField;
    @ViewComponent
    private PasswordField passwordField;
    @ViewComponent
    private PasswordField confirmPasswordField;
    @ViewComponent
    private ComboBox<String> timeZoneField;
    @ViewComponent
    private DataGrid<WorkScheduleAssignment> assignmentsDataGrid;
    @ViewComponent
    private CollectionContainer<WorkScheduleAssignment> assignmentsDc;
    @ViewComponent
    private CollectionLoader<WorkScheduleAssignment> assignmentsDl;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private EntityStates entityStates;
    @ViewComponent
    private MessageBundle messageBundle;
    @Autowired
    private MessageTools messageTools;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private Notifications notifications;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private WorkScheduleAssignmentSupport workScheduleAssignmentSupport;

    // The assignments in force today, found when the table is drawn and dropped whenever its assignments change
    @Nullable
    private Set<WorkScheduleAssignment> currentAssignments;

    @Subscribe
    public void onInit(final InitEvent event) {
        timeZoneField.setItems(List.of(TimeZone.getAvailableIDs()));
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<User> event) {
        usernameField.setReadOnly(false);
        passwordField.setVisible(true);
        confirmPasswordField.setVisible(true);
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        // A new user has no assignments to load
        if (!entityStates.isNew(getEditedEntity())) {
            assignmentsDl.setParameter("user", getEditedEntity());
            assignmentsDl.load();
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            usernameField.focus();
        }

        assignmentsDataGrid.sort(newestAssignmentsFirst());
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        if (entityStates.isNew(getEditedEntity())
                && !Objects.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
            event.getErrors().add(messageBundle.getMessage("passwordsDoNotMatch"));
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            getEditedEntity().setPassword(passwordEncoder.encode(passwordField.getValue()));

            String entityCaption = messageTools.getEntityCaption(getEditedEntityContainer().getEntityMetaClass());
            notifications.create(messageBundle.formatMessage("noAssignedRolesNotification", entityCaption))
                    .withType(Notifications.Type.WARNING)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
        }
    }

    @Subscribe(id = "assignmentsDc", target = Target.DATA_CONTAINER)
    public void onAssignmentsDcCollectionChange(
            final CollectionContainer.CollectionChangeEvent<WorkScheduleAssignment> event) {
        currentAssignments = null;

        // A created assignment is added first and an edited one stays in place, whatever its date, while the grid
        // sorts its rows only when its sort order changes. Sorting again also redraws every row, so the Current
        // badge moves when an edit changes which assignment is in force today
        if (event.getChangeType() == CollectionChangeType.ADD_ITEMS
                || event.getChangeType() == CollectionChangeType.SET_ITEM) {
            // Unsorted, the table keeps the order the assignments are loaded in: newest first
            componentSupport.sortAsShown(assignmentsDataGrid, assignmentsDc, Sort.by(Sort.Direction.DESC, "startDate"));
        }
    }

    private List<GridSortOrder<WorkScheduleAssignment>> newestAssignmentsFirst() {
        return GridSortOrder.desc(assignmentsDataGrid.getColumnByKey("startDate")).build();
    }

    @Install(to = "assignmentsDataGrid.create", subject = "initializer")
    private void assignmentsDataGridCreateInitializer(final WorkScheduleAssignment assignment) {
        assignment.setUser(getEditedEntity());
    }

    @Install(to = "assignmentsDataGrid.create", subject = "detailViewConfigurer")
    private void assignmentsDataGridCreateDetailViewConfigurer(final WorkScheduleAssignmentDetailView view) {
        view.setUserFieldVisible(false);
    }

    @Install(to = "assignmentsDataGrid.edit", subject = "detailViewConfigurer")
    private void assignmentsDataGridEditDetailViewConfigurer(final WorkScheduleAssignmentDetailView view) {
        view.setUserFieldVisible(false);
    }

    @Supply(to = "assignmentsDataGrid.workSchedule", subject = "renderer")
    private Renderer<WorkScheduleAssignment> assignmentsDataGridWorkScheduleRenderer() {
        return new ComponentRenderer<>(assignment -> {
            WorkSchedule schedule = assignment.getWorkSchedule();
            Span cell = new Span(metadataTools.format(schedule));
            cell.addClassName("text-with-badges");

            if (getCurrentAssignments().contains(assignment)) {
                cell.add(componentSupport.createBadge(
                        messageBundle.getMessage("currentBadge.text"), BadgeStyle.SUCCESS));
            }
            // An assignment keeps its schedule after the schedule is deleted, and its plan stays as it was
            if (schedule.getDeletedDate() != null) {
                cell.add(componentSupport.createBadge(
                        messageBundle.getMessage("deletedBadge.text"), BadgeStyle.NEUTRAL));
            }
            return cell;
        });
    }

    private Set<WorkScheduleAssignment> getCurrentAssignments() {
        if (currentAssignments == null) {
            currentAssignments = workScheduleAssignmentSupport.findCurrent(assignmentsDc.getItems(), LocalDate.now());
        }
        return currentAssignments;
    }
}
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

package com.glebfox.timesheets.action;

import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleUsersAddView;
import io.jmix.core.AccessManager;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponentProperties;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.ListDataComponentAction;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.icon.Icons;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.KeyCombination;
import io.jmix.flowui.kit.icon.JmixFontIcon;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.StandardOutcome;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Assigns several users at once in the list component of an editor, such as the work schedule editor's
 * assignments, whose container is loaded by query. Opens {@link WorkScheduleUsersAddView}, which asks for
 * the users and a start date, and adds one {@link WorkScheduleAssignment} per user at the top of the list. The
 * assignments are created in the editor's data context: the editor's OK saves them, and its Cancel discards them.
 * <p>
 * Needs no selection, and checks the permission the standard create action checks. It does not follow
 * the view's read-only mode: an editor opened read-only for lack of permission on its own entity still
 * adds assignments where this permission allows.
 */
@ActionType(WorkScheduleUsersAddAction.ID)
public class WorkScheduleUsersAddAction
        extends ListDataComponentAction<WorkScheduleUsersAddAction, WorkScheduleAssignment> {

    public static final String ID = "list_workScheduleUsers_add";

    private AccessManager accessManager;
    private DialogWindows dialogWindows;

    private Consumer<WorkScheduleAssignment> initializer;
    private Consumer<WorkScheduleUsersAddView> viewConfigurer;

    public WorkScheduleUsersAddAction() {
        this(ID);
    }

    public WorkScheduleUsersAddAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        this.variant = ActionVariant.PRIMARY;
    }

    // The look and the shortcut of the standard add action
    @Autowired
    public void setMessages(Messages messages) {
        this.text = messages.getMessage("actions.Add");
    }

    @Autowired
    public void setIcons(Icons icons) {
        this.icon = icons.get(JmixFontIcon.ADD_ACTION);
    }

    @Autowired
    public void setUiComponentProperties(UiComponentProperties uiComponentProperties) {
        setShortcutCombination(KeyCombination.create(uiComponentProperties.getGridAddShortcut()));
    }

    @Autowired
    public void setAccessManager(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    @Autowired
    public void setDialogWindows(DialogWindows dialogWindows) {
        this.dialogWindows = dialogWindows;
    }

    /**
     * Sets the function that fills each new assignment after its user and start date, such as with its schedule.
     */
    public void setInitializer(@Nullable Consumer<WorkScheduleAssignment> initializer) {
        this.initializer = initializer;
    }

    /**
     * Sets the function that sets up the dialog before it opens. The action itself hands the dialog
     * the list's assignments and the ids of those removed in the editor, to check the start date against.
     */
    public void setViewConfigurer(@Nullable Consumer<WorkScheduleUsersAddView> viewConfigurer) {
        this.viewConfigurer = viewConfigurer;
    }

    @Override
    protected boolean isApplicable() {
        return super.isApplicable()
                && target != null
                && target.getItems() instanceof EntityDataUnit;
    }

    @Override
    protected boolean isPermitted() {
        // The entity-level check of the standard create action
        return ListActionTargets.isEntityPermitted(target, accessManager, UiEntityContext::isCreatePermitted)
                && super.isPermitted();
    }

    @Override
    public void execute() {
        checkTarget();
        CollectionContainer<WorkScheduleAssignment> container = ListActionTargets.getContainer(target, getClass());
        // The editor's data context, so the assignments are saved with the editor and discarded with it
        DataContext dataContext = ListActionTargets.getDataContext(target);

        dialogWindows.view(ListActionTargets.getView(target), WorkScheduleUsersAddView.class)
                .withViewConfigurer(view -> {
                    view.setEditorAssignments(container.getItems(),
                            ListActionTargets.getRemovedIds(target, WorkScheduleAssignment.class));
                    if (viewConfigurer != null) {
                        viewConfigurer.accept(view);
                    }
                })
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        addAssignments(closeEvent.getView(), container, dataContext);
                    }
                })
                .open();
    }

    private void addAssignments(WorkScheduleUsersAddView view,
                                CollectionContainer<WorkScheduleAssignment> container,
                                DataContext dataContext) {
        LocalDate startDate = view.getStartDate();
        List<WorkScheduleAssignment> assignments = view.getUsers().stream()
                .map(user -> createAssignment(dataContext, user, startDate))
                .toList();
        // At the top of the list, whatever their start date, in one change of the container for all the users
        container.getMutableItems().addAll(0, assignments);
    }

    private WorkScheduleAssignment createAssignment(DataContext dataContext, User user, LocalDate startDate) {
        WorkScheduleAssignment assignment = dataContext.create(WorkScheduleAssignment.class);
        assignment.setUser(user);
        assignment.setStartDate(startDate);
        if (initializer != null) {
            initializer.accept(assignment);
        }
        return assignment;
    }
}

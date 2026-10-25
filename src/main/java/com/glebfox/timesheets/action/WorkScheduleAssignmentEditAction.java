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
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleAssignmentDetailView;
import io.jmix.core.Messages;
import io.jmix.core.security.EntityOp;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponentProperties;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.SecuredListDataComponentAction;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.icon.Icons;
import io.jmix.flowui.kit.component.KeyCombination;
import io.jmix.flowui.kit.icon.JmixFontIcon;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.StandardOutcome;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

/**
 * Edits the {@link WorkScheduleAssignment} selected in the list component of an editor, such as the user editor's
 * assignments, whose container is loaded by query. Opens {@link WorkScheduleAssignmentDetailView} as a dialog
 * with the editor's data context as its parent, so the dialog's OK changes the assignment in the list and in that
 * data context: the editor's OK saves the change, and its Cancel discards it.
 * <p>
 * Needs one selected assignment, and checks the permissions the standard edit action checks to edit it.
 * It does not follow the view's read-only mode: an editor opened read-only for lack of permission on its own
 * entity still edits assignments where these permissions allow.
 */
@ActionType(WorkScheduleAssignmentEditAction.ID)
public class WorkScheduleAssignmentEditAction
        extends SecuredListDataComponentAction<WorkScheduleAssignmentEditAction, WorkScheduleAssignment> {

    public static final String ID = "list_workScheduleAssignment_edit";

    private DialogWindows dialogWindows;

    private Consumer<WorkScheduleAssignmentDetailView> detailViewConfigurer;
    private Consumer<WorkScheduleAssignment> afterSaveHandler;

    public WorkScheduleAssignmentEditAction() {
        this(ID);
    }

    public WorkScheduleAssignmentEditAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        setConstraintEntityOp(EntityOp.UPDATE);
    }

    // The look and the shortcut of the standard edit action
    @Autowired
    public void setMessages(Messages messages) {
        this.text = messages.getMessage("actions.Edit");
    }

    @Autowired
    public void setIcons(Icons icons) {
        this.icon = icons.get(JmixFontIcon.EDIT_ACTION);
    }

    @Autowired
    public void setUiComponentProperties(UiComponentProperties uiComponentProperties) {
        setShortcutCombination(KeyCombination.create(uiComponentProperties.getGridEditShortcut()));
    }

    @Autowired
    public void setDialogWindows(DialogWindows dialogWindows) {
        this.dialogWindows = dialogWindows;
    }

    /**
     * Sets the function that sets up the dialog before it opens. The action itself hands the dialog
     * the list's assignments to check the start date against.
     */
    public void setDetailViewConfigurer(@Nullable Consumer<WorkScheduleAssignmentDetailView> detailViewConfigurer) {
        this.detailViewConfigurer = detailViewConfigurer;
    }

    /**
     * Sets the handler to be invoked with the assignment as the dialog saved it, after the list shows the change.
     */
    public void setAfterSaveHandler(@Nullable Consumer<WorkScheduleAssignment> afterSaveHandler) {
        this.afterSaveHandler = afterSaveHandler;
    }

    @Override
    protected boolean isApplicable() {
        return super.isApplicable() && target.getSelectedItems().size() == 1;
    }

    @Override
    protected boolean isPermitted() {
        if (target == null
                || target.getSingleSelectedItem() == null
                || !(target.getItems() instanceof EntityDataUnit entityDataUnit)) {
            return false;
        }

        UiEntityContext entityContext = new UiEntityContext(entityDataUnit.getEntityMetaClass());
        accessManager.applyRegisteredConstraints(entityContext);

        if (!entityContext.isEditPermitted()) {
            return false;
        }

        return super.isPermitted();
    }

    @Override
    public void execute() {
        checkTarget();

        WorkScheduleAssignment selectedItem = target.getSingleSelectedItem();
        if (selectedItem == null) {
            throw new IllegalStateException(String.format("There is not selected item in %s target",
                    getClass().getSimpleName()));
        }
        CollectionContainer<WorkScheduleAssignment> container = ListActionTargets.getContainer(target, getClass());

        DialogWindow<WorkScheduleAssignmentDetailView> dialogWindow = dialogWindows.detail(target)
                .withViewClass(WorkScheduleAssignmentDetailView.class)
                .editEntity(selectedItem)
                // The editor's data context, so the change is saved with the editor and discarded with it
                .withParentDataContext(ListActionTargets.getDataContext(target))
                .withViewConfigurer(view -> {
                    view.setSiblingAssignments(container::getItems);
                    if (detailViewConfigurer != null) {
                        detailViewConfigurer.accept(view);
                    }
                })
                .build();

        if (afterSaveHandler != null) {
            // Added after the builder's own listener, which replaces the assignment in the list
            dialogWindow.addAfterCloseListener(closeEvent -> {
                if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                    afterSaveHandler.accept(closeEvent.getView().getEditedEntity());
                }
            });
        }

        dialogWindow.open();
    }
}

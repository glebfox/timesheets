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
import com.vaadin.flow.component.Focusable;
import io.jmix.core.Messages;
import io.jmix.core.security.EntityOp;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponentProperties;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.action.list.SecuredListDataComponentAction;
import io.jmix.flowui.icon.Icons;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.KeyCombination;
import io.jmix.flowui.kit.icon.JmixFontIcon;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Removes the {@link WorkScheduleAssignment}s selected in the list component of an editor, such as the user
 * editor's assignments, whose container is loaded by query. After the usual confirmation it takes them out of
 * the list and removes them in the editor's data context: the editor's OK deletes them, and its Cancel keeps them.
 * The standard remove action would delete them from the database at once.
 * <p>
 * Needs one or more selected assignments, and checks the permissions the standard remove action checks.
 * It does not follow the view's read-only mode: an editor opened read-only for lack of permission on its own
 * entity still removes assignments where these permissions allow.
 */
@ActionType(WorkScheduleAssignmentRemoveAction.ID)
public class WorkScheduleAssignmentRemoveAction
        extends SecuredListDataComponentAction<WorkScheduleAssignmentRemoveAction, WorkScheduleAssignment> {

    public static final String ID = "list_workScheduleAssignment_remove";

    private Dialogs dialogs;
    private Messages messages;

    public WorkScheduleAssignmentRemoveAction() {
        this(ID);
    }

    public WorkScheduleAssignmentRemoveAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        setConstraintEntityOp(EntityOp.DELETE);
        this.variant = ActionVariant.DANGER;
    }

    // The look and the shortcut of the standard remove action
    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
        this.text = messages.getMessage("actions.Remove");
    }

    @Autowired
    public void setIcons(Icons icons) {
        this.icon = icons.get(JmixFontIcon.REMOVE_ACTION);
    }

    @Autowired
    public void setUiComponentProperties(UiComponentProperties uiComponentProperties) {
        setShortcutCombination(KeyCombination.create(uiComponentProperties.getGridRemoveShortcut()));
    }

    @Autowired
    public void setDialogs(Dialogs dialogs) {
        this.dialogs = dialogs;
    }

    @Override
    protected boolean isPermitted() {
        // The entity-level check of the standard remove action, then the row-level one
        return ListActionTargets.isEntityPermitted(target, accessManager, UiEntityContext::isDeletePermitted)
                && super.isPermitted();
    }

    @Override
    public void execute() {
        checkTarget();

        List<WorkScheduleAssignment> selectedItems = List.copyOf(target.getSelectedItems());
        if (selectedItems.isEmpty()) {
            throw new IllegalStateException(String.format("There is not selected item in %s target",
                    getClass().getSimpleName()));
        }
        CollectionContainer<WorkScheduleAssignment> container = ListActionTargets.getContainer(target, getClass());
        DataContext dataContext = ListActionTargets.getDataContext(target);

        // The texts of the standard remove confirmation
        dialogs.createOptionDialog()
                .withHeader(messages.getMessage("dialogs.Confirmation"))
                .withText(messages.getMessage("dialogs.Confirmation.Remove"))
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(event -> remove(selectedItems, container, dataContext))
                                .withVariant(ActionVariant.PRIMARY),
                        new DialogAction(DialogAction.Type.NO)
                                .withHandler(event -> focusTarget()))
                .open();
    }

    private void remove(List<WorkScheduleAssignment> assignments,
                        CollectionContainer<WorkScheduleAssignment> container,
                        DataContext dataContext) {
        // Removed in the data context, so the editor's OK deletes the saved ones; a new one is just dropped
        assignments.forEach(dataContext::remove);
        container.getMutableItems().removeAll(assignments);
        assignments.forEach(target::deselect);
        focusTarget();
    }

    private void focusTarget() {
        if (target instanceof Focusable<?> focusable) {
            focusable.focus();
        }
    }
}

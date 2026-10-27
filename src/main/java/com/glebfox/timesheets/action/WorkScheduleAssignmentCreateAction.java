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
import io.jmix.flowui.view.builder.DetailWindowClassBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Creates a {@link WorkScheduleAssignment} in the list component of an editor, such as the user editor's
 * assignments, whose container is loaded by query. Opens {@link WorkScheduleAssignmentDetailView} as a dialog
 * with the editor's data context as its parent, so the dialog's OK adds the assignment at the top of the list
 * and to that data context: the editor's OK saves it, and its Cancel discards it.
 * <p>
 * Needs no selection, and checks the permission the standard create action checks. It does not follow
 * the view's read-only mode: an editor opened read-only for lack of permission on its own entity still
 * edits assignments where this permission allows.
 */
@ActionType(WorkScheduleAssignmentCreateAction.ID)
public class WorkScheduleAssignmentCreateAction
        extends ListDataComponentAction<WorkScheduleAssignmentCreateAction, WorkScheduleAssignment> {

    public static final String ID = "list_workScheduleAssignment_create";

    private AccessManager accessManager;
    private DialogWindows dialogWindows;

    private Consumer<WorkScheduleAssignment> initializer;
    private Consumer<WorkScheduleAssignmentDetailView> detailViewConfigurer;

    public WorkScheduleAssignmentCreateAction() {
        this(ID);
    }

    public WorkScheduleAssignmentCreateAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        this.variant = ActionVariant.PRIMARY;
    }

    // The look and the shortcut of the standard create action
    @Autowired
    public void setMessages(Messages messages) {
        this.text = messages.getMessage("actions.Create");
    }

    @Autowired
    public void setIcons(Icons icons) {
        this.icon = icons.get(JmixFontIcon.CREATE_ACTION);
    }

    @Autowired
    public void setUiComponentProperties(UiComponentProperties uiComponentProperties) {
        setShortcutCombination(KeyCombination.create(uiComponentProperties.getGridCreateShortcut()));
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
     * Sets the function that fills the new assignment before the dialog opens, such as its user
     * or its schedule.
     */
    public void setInitializer(@Nullable Consumer<WorkScheduleAssignment> initializer) {
        this.initializer = initializer;
    }

    /**
     * Sets the function that sets up the dialog before it opens. The action itself hands the dialog
     * the list's assignments and the ids of those removed in the editor, to check the start date against.
     */
    public void setDetailViewConfigurer(@Nullable Consumer<WorkScheduleAssignmentDetailView> detailViewConfigurer) {
        this.detailViewConfigurer = detailViewConfigurer;
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

        DetailWindowClassBuilder<WorkScheduleAssignment, WorkScheduleAssignmentDetailView> builder =
                dialogWindows.detail(target)
                        .withViewClass(WorkScheduleAssignmentDetailView.class)
                        .newEntity()
                        // The editor's data context, so the assignment is saved with the editor and discarded with it
                        .withParentDataContext(ListActionTargets.getDataContext(target))
                        // At the top of the list, whatever its start date
                        .withAddFirst(true)
                        .withViewConfigurer(view -> {
                            view.setSiblingAssignments(container.getItems(), getRemovedIds());
                            if (detailViewConfigurer != null) {
                                detailViewConfigurer.accept(view);
                            }
                        });

        if (initializer != null) {
            builder = builder.withInitializer(initializer);
        }

        builder.open();
    }

    private List<UUID> getRemovedIds() {
        return ListActionTargets.getRemoved(target, WorkScheduleAssignment.class).stream()
                .map(WorkScheduleAssignment::getId)
                .toList();
    }
}

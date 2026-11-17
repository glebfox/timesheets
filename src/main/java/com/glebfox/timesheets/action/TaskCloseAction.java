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

import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import io.jmix.core.security.EntityOp;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.AdjustWhenViewReadOnly;
import io.jmix.flowui.action.list.SecuredListDataComponentAction;

/**
 * Closes the {@link Task} selected in a list component: an Active task turns Inactive, and an Inactive one stays as it
 * is. It changes the task in its container only, so in an editor the change waits for the editor's save. The
 * descriptor gives its text and icon.
 * <p>
 * It is offered only to a user who may update the selected task, and a read-only editor disables it, as its save
 * would be refused there.
 */
@ActionType(TaskCloseAction.ID)
public class TaskCloseAction extends SecuredListDataComponentAction<TaskCloseAction, Task>
        implements AdjustWhenViewReadOnly {

    public static final String ID = "list_task_close";

    public TaskCloseAction() {
        this(ID);
    }

    public TaskCloseAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        setConstraintEntityOp(EntityOp.UPDATE);
    }

    @Override
    protected boolean isPermitted() {
        // The entity-level check of the standard edit action, then the row-level one
        return ListActionTargets.isEntityPermitted(target, accessManager, UiEntityContext::isEditPermitted)
                && super.isPermitted();
    }

    @Override
    public void execute() {
        checkTarget();

        Task selectedItem = target.getSingleSelectedItem();
        if (selectedItem == null) {
            throw new IllegalStateException(String.format("There is not selected item in %s target",
                    getClass().getSimpleName()));
        }

        if (selectedItem.getStatus() != TaskStatus.INACTIVE) {
            selectedItem.setStatus(TaskStatus.INACTIVE);
        }
    }
}

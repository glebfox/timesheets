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

import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.security.EntityOp;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.AdjustWhenViewReadOnly;
import io.jmix.flowui.action.list.SecuredListDataComponentAction;
import io.jmix.flowui.model.DataLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Makes the {@link WorkSchedule} selected in a list component the default one and saves it at once.
 * The previous default is unmarked in the same save by the rule that keeps one default schedule,
 * so the action reloads the list afterwards. A schedule deleted since the list was loaded is not saved:
 * the list is reloaded and a warning is shown.
 */
@ActionType(WorkScheduleMakeDefaultAction.ID)
public class WorkScheduleMakeDefaultAction
        extends SecuredListDataComponentAction<WorkScheduleMakeDefaultAction, WorkSchedule>
        implements AdjustWhenViewReadOnly {

    public static final String ID = "list_workSchedule_makeDefault";

    private DataManager dataManager;
    private Notifications notifications;
    private Messages messages;

    public WorkScheduleMakeDefaultAction() {
        this(ID);
    }

    public WorkScheduleMakeDefaultAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        setConstraintEntityOp(EntityOp.UPDATE);
        this.icon = GravityUiIcon.STAR.create();
    }

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
        this.text = messages.getMessage("actions.MakeDefault");
    }

    @Autowired
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Autowired
    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    @Override
    protected boolean isApplicable() {
        if (!super.isApplicable() || target.getSelectedItems().size() != 1) {
            return false;
        }

        WorkSchedule selectedItem = target.getSingleSelectedItem();
        return selectedItem != null && !Boolean.TRUE.equals(selectedItem.getIsDefault());
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

        WorkSchedule selectedItem = target.getSingleSelectedItem();
        if (selectedItem == null) {
            throw new IllegalStateException(String.format("There is not selected item in %s target",
                    getClass().getSimpleName()));
        }
        // Found before the save, so nothing is saved for a list that cannot show the result
        DataLoader loader = ListActionTargets.getLoader(target, getClass());

        // A fresh instance, so the listed one stays as it was if the save fails
        WorkSchedule schedule = dataManager.load(Id.of(selectedItem)).optional().orElse(null);
        if (schedule == null) {
            // Deleted in another session after the list was loaded
            loader.load();
            notifications.create(messages.formatMessage(WorkScheduleMakeDefaultAction.class,
                            "workScheduleMakeDefault.deletedWarning", selectedItem.getName()))
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        schedule.setIsDefault(true);
        dataManager.saveWithoutReload(schedule);

        // The previous default has changed too
        loader.load();

        notifications.create(messages.formatMessage(WorkScheduleMakeDefaultAction.class,
                        "workScheduleMakeDefault.notification", schedule.getName()))
                .withType(Notifications.Type.SUCCESS)
                .show();
    }
}

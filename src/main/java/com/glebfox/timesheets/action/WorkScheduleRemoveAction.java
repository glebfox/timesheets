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

import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.view.workschedule.DefaultWorkScheduleDeleteView;
import com.vaadin.flow.component.Component;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.security.EntityOp;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponentProperties;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.AdjustWhenViewReadOnly;
import io.jmix.flowui.action.list.SecuredListDataComponentAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.data.ContainerDataUnit;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.icon.Icons;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.KeyCombination;
import io.jmix.flowui.kit.icon.JmixFontIcon;
import io.jmix.flowui.model.DataLoader;
import io.jmix.flowui.model.HasLoader;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Removes the {@link WorkSchedule} selected in a list component. A schedule that is not the default is removed
 * as the standard remove action does it, after the usual confirmation. The default one opens
 * {@link DefaultWorkScheduleDeleteView} to choose the next default, or, when it is the only schedule,
 * is not removed and a warning is shown.
 */
@ActionType(WorkScheduleRemoveAction.ID)
public class WorkScheduleRemoveAction
        extends SecuredListDataComponentAction<WorkScheduleRemoveAction, WorkSchedule>
        implements AdjustWhenViewReadOnly {

    public static final String ID = "list_workSchedule_remove";

    private RemoveOperation removeOperation;
    private DialogWindows dialogWindows;
    private DataManager dataManager;
    private Notifications notifications;
    private Messages messages;

    public WorkScheduleRemoveAction() {
        this(ID);
    }

    public WorkScheduleRemoveAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        setConstraintEntityOp(EntityOp.DELETE);
        this.variant = ActionVariant.DANGER;
    }

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
        this.text = messages.getMessage("actions.Remove");
    }

    // The look and the shortcut of the standard remove action
    @Autowired
    public void setIcons(Icons icons) {
        this.icon = icons.get(JmixFontIcon.REMOVE_ACTION);
    }

    @Autowired
    public void setUiComponentProperties(UiComponentProperties uiComponentProperties) {
        setShortcutCombination(KeyCombination.create(uiComponentProperties.getGridRemoveShortcut()));
    }

    @Autowired
    public void setRemoveOperation(RemoveOperation removeOperation) {
        this.removeOperation = removeOperation;
    }

    @Autowired
    public void setDialogWindows(DialogWindows dialogWindows) {
        this.dialogWindows = dialogWindows;
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

        if (!entityContext.isDeletePermitted()) {
            return false;
        }

        return super.isPermitted();
    }

    @Override
    public void execute() {
        checkTarget();
        checkTargetItems(ContainerDataUnit.class);

        WorkSchedule selectedItem = target.getSingleSelectedItem();
        if (selectedItem == null) {
            throw new IllegalStateException(String.format("There is not selected item in %s target",
                    getClass().getSimpleName()));
        }

        if (!Boolean.TRUE.equals(selectedItem.getIsDefault())) {
            removeOperation.builder(target)
                    .withConfirmation(true)
                    .remove();
        } else if (hasOtherSchedules(selectedItem)) {
            openDefaultDeleteDialog(selectedItem);
        } else {
            notifications.create(messages.getMessage(WorkScheduleRemoveAction.class,
                            "workScheduleRemove.onlyScheduleWarning"))
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
    }

    private boolean hasOtherSchedules(WorkSchedule schedule) {
        return dataManager.loadValue("select count(e) from ts_WorkSchedule e where e.id <> :scheduleId",
                        Long.class)
                .parameter("scheduleId", schedule.getId())
                .one() > 0;
    }

    private void openDefaultDeleteDialog(WorkSchedule schedule) {
        // Found before the dialog opens, so nothing is saved for a list that cannot show the result
        DataLoader loader = getTargetLoader();
        View<?> origin = UiComponentUtils.findView((Component) target);
        if (origin == null) {
            throw new IllegalStateException(String.format("%s target is not attached to a view",
                    getClass().getSimpleName()));
        }

        dialogWindows.view(origin, DefaultWorkScheduleDeleteView.class)
                .withViewConfigurer(view -> view.setSchedule(schedule))
                .withAfterCloseListener(closeEvent -> {
                    // The deleted schedule and the new default have both changed
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        loader.load();
                    }
                })
                .open();
    }

    private DataLoader getTargetLoader() {
        DataLoader loader = target.getItems() instanceof ContainerDataUnit<?> items
                && items.getContainer() instanceof HasLoader hasLoader
                ? hasLoader.getLoader()
                : null;
        if (loader == null) {
            throw new IllegalStateException(String.format("%s target is not bound to a container with a loader",
                    getClass().getSimpleName()));
        }
        return loader;
    }
}

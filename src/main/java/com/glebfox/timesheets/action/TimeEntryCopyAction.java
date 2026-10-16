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

package com.glebfox.timesheets.action;

import com.glebfox.timesheets.app.TimeEntrySupport;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.security.EntityOp;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.AdjustWhenViewReadOnly;
import io.jmix.flowui.action.list.SecuredListDataComponentAction;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.builder.DetailWindowBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents an action to copy a selected {@link TimeEntry} from a list component, allowing modifications
 * in a detail view dialog.
 */
@ActionType(TimeEntryCopyAction.ID)
public class TimeEntryCopyAction extends SecuredListDataComponentAction<TimeEntryCopyAction, TimeEntry>
        implements AdjustWhenViewReadOnly {

    public static final String ID = "list_timeEntry_copy";

    private DialogWindows dialogWindows;
    private TimeEntrySupport timeEntrySupport;

    private Consumer<DialogWindow.AfterCloseEvent<TimeEntryDetailView>> afterCloseHandler;
    private Consumer<TimeEntry> afterSaveHandler;
    private Function<TimeEntry, TimeEntry> transformation;

    private boolean ownTimeEntry = false;

    public TimeEntryCopyAction() {
        this(ID);
    }

    public TimeEntryCopyAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        setConstraintEntityOp(EntityOp.CREATE);
        this.icon = GravityUiIcon.COPY.create();
    }

    @Autowired
    public void setMessages(Messages messages) {
        this.text = messages.getMessage("actions.Copy");
    }

    @Autowired
    public void setDialogWindowBuilders(DialogWindows dialogWindows) {
        this.dialogWindows = dialogWindows;
    }

    @Autowired
    public void setTimeEntrySupport(TimeEntrySupport timeEntrySupport) {
        this.timeEntrySupport = timeEntrySupport;
    }

    /**
     * Sets the handler to be invoked when the detail view closes.
     *
     * @param afterCloseHandler handler to set
     */
    public void setAfterCloseHandler(
            @Nullable Consumer<DialogWindow.AfterCloseEvent<TimeEntryDetailView>> afterCloseHandler) {
        this.afterCloseHandler = afterCloseHandler;
    }

    /**
     * Sets the handler to be invoked when the detail view saves the entity.
     */
    public void setAfterSaveHandler(@Nullable Consumer<TimeEntry> afterSaveHandler) {
        this.afterSaveHandler = afterSaveHandler;
    }

    /**
     * Sets the function to transform the saved in the detail view entity before setting it to the target data
     * container.
     *
     * @param transformation transformation function to set
     */
    public void setTransformation(@Nullable Function<TimeEntry, TimeEntry> transformation) {
        this.transformation = transformation;
    }

    /**
     * Determines if the current time entry is marked as own.
     *
     * @return {@code true} if the time entry is marked as own, {@code false} otherwise
     */
    public boolean isOwnTimeEntry() {
        return ownTimeEntry;
    }

    /**
     * Sets whether the current time entry is marked as owned by the user.
     *
     * @param ownTimeEntry {@code true} if owned, {@code false} otherwise
     */
    public void setOwnTimeEntry(boolean ownTimeEntry) {
        this.ownTimeEntry = ownTimeEntry;
    }

    @Override
    protected boolean isPermitted() {
        if (target == null
                || target.getSingleSelectedItem() == null
                || !(target.getItems() instanceof EntityDataUnit)) {
            return false;
        }

        MetaClass metaClass = ((EntityDataUnit) target.getItems()).getEntityMetaClass();

        UiEntityContext entityContext = new UiEntityContext(metaClass);
        accessManager.applyRegisteredConstraints(entityContext);

        if (!entityContext.isCreatePermitted()) {
            return false;
        }

        return super.isPermitted();
    }

    @Override
    public boolean isDisabledWhenViewReadOnly() {
        if (!(target.getItems() instanceof EntityDataUnit)) {
            return true;
        }

        MetaClass metaClass = ((EntityDataUnit) target.getItems()).getEntityMetaClass();
        // Even though the view is read-only, this edit action may remain active
        // because the related entity cannot be edited and the corresponding detail view
        // will be opened in read-only mode either.
        UiEntityContext entityContext = new UiEntityContext(metaClass);
        accessManager.applyRegisteredConstraints(entityContext);

        return entityContext.isCreatePermitted();

    }

    @Override
    public void execute() {
        checkTarget();
        checkTargetItems(EntityDataUnit.class);

        TimeEntry selectedItem = target.getSingleSelectedItem();
        if (selectedItem == null) {
            throw new IllegalStateException(String.format("There is not selected item in %s target",
                    getClass().getSimpleName()));
        }

        TimeEntry copiedEntity = timeEntrySupport.copy(selectedItem);
        openDialog(copiedEntity);
    }

    private void openDialog(TimeEntry timeEntry) {
        DetailWindowBuilder<TimeEntry, TimeEntryDetailView> builder =
                dialogWindows.detail(target)
                        .withViewClass(TimeEntryDetailView.class)
                        .withViewConfigurer(timeEntryDetailView ->
                                timeEntryDetailView.setOwnTimeEntry(ownTimeEntry))
                        .newEntity(timeEntry);

        if (afterCloseHandler != null) {
            builder = builder.withAfterCloseListener(afterCloseHandler);
        }

        if (transformation != null) {
            builder = builder.withTransformation(transformation);
        }

        DialogWindow<TimeEntryDetailView> dialogWindow = builder.build();
        TimeEntryDetailView detailView = dialogWindow.getView();
        if (afterSaveHandler != null) {
            dialogWindow.addAfterCloseListener(event -> {
                if (event.closedWith(StandardOutcome.SAVE)) {
                    TimeEntry savedEntity = detailView.getEditedEntity();
                    afterSaveHandler.accept(savedEntity);
                }
            });
        }

        dialogWindow.open();
    }

    /**
     * @see #setAfterCloseHandler(Consumer)
     */
    public TimeEntryCopyAction withAfterCloseHandler(
            Consumer<DialogWindow.AfterCloseEvent<TimeEntryDetailView>> afterCloseHandler) {
        setAfterCloseHandler(afterCloseHandler);
        return this;
    }

    /**
     * @see #setAfterSaveHandler(Consumer)
     */
    public TimeEntryCopyAction withAfterSaveHandler(@Nullable Consumer<TimeEntry> afterSaveHandler) {
        setAfterSaveHandler(afterSaveHandler);
        return this;
    }

    /**
     * @see #setTransformation(Function)
     */
    public TimeEntryCopyAction withTransformation(@Nullable Function<TimeEntry, TimeEntry> transformation) {
        setTransformation(transformation);
        return this;
    }

    /**
     * @see #setOwnTimeEntry(boolean)
     */
    public TimeEntryCopyAction withOwnTimeEntry(boolean ownTimeEntry) {
        setOwnTimeEntry(ownTimeEntry);
        return this;
    }
}

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

package com.glebfox.timesheets.view.task;

import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.vaadin.flow.router.Route;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "my-tasks", layout = MainView.class)
@ViewController("ts_Task.my")
@ViewDescriptor("my-task-list-view.xml")
@LookupComponent("tasksDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class MyTaskListView extends StandardView {

    @ViewComponent
    private DataGrid<Task> tasksDataGrid;

    @Autowired
    private Metadata metadata;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private CurrentUserSubstitution currentUserSubstitution;

    @Subscribe("tasksDataGrid.createTimeEntry")
    public void onTasksDataGridCreateTimeEntry(final ActionPerformedEvent event) {
        Task selectedItem = tasksDataGrid.getSingleSelectedItem();
        if (selectedItem == null) {
            return;
        }

        TimeEntry timeEntry = dataManager.create(TimeEntry.class);
        timeEntry.setUser((User) currentUserSubstitution.getEffectiveUser());
        timeEntry.setTask(selectedItem);

        dialogWindows.detail(this, TimeEntry.class)
                .withViewClass(TimeEntryDetailView.class)
                .newEntity(timeEntry)
                .withViewConfigurer(timeEntryDetailView ->
                        timeEntryDetailView.setOwnTimeEntry(true))
                .open();
    }

    @Install(to = "tasksDataGrid.createTimeEntry", subject = "enabledRule")
    private boolean tasksDataGridCreateTimeEntryEnabledRule() {
        UiEntityContext entityContext = new UiEntityContext(metadata.getClass(TimeEntry.class));
        accessManager.applyRegisteredConstraints(entityContext);
        return entityContext.isCreatePermitted();
    }
}
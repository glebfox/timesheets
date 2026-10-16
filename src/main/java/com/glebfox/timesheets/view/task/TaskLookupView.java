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

import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.jspecify.annotations.Nullable;

@ViewController("ts_Task.lookup")
@ViewDescriptor("task-lookup-view.xml")
@LookupComponent("tasksDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class TaskLookupView extends StandardListView<Task> {

    @ViewComponent
    private CollectionLoader<Task> tasksDl;
    @ViewComponent
    private CollectionLoader<Project> projectsDl;

    public void setUser(@Nullable User user) {
        if (user != null) {
            tasksDl.setParameter("user", user);
            projectsDl.setParameter("user", user);
        } else {
            tasksDl.removeParameter("user");
            projectsDl.removeParameter("user");
        }
    }
}
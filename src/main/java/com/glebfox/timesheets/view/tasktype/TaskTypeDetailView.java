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

package com.glebfox.timesheets.view.tasktype;

import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "task-types/:id", layout = MainView.class)
@ViewController("ts_TaskType.detail")
@ViewDescriptor("task-type-detail-view.xml")
@EditedEntityContainer("taskTypeDc")
@DialogMode(width = "640px", closeOnEsc = true)
public class TaskTypeDetailView extends StandardDetailView<TaskType> {
}
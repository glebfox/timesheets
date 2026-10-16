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

import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.taggroup.TagGroupLookupView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.multiselectcomboboxpicker.JmixMultiSelectComboBoxPicker;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

@Route(value = "tasks/:id", layout = MainView.class)
@ViewController("ts_Task.detail")
@ViewDescriptor("task-detail-view.xml")
@EditedEntityContainer("taskDc")
@DialogMode(width = "800px", closeOnEsc = true)
public class TaskDetailView extends StandardDetailView<Task> {

    @ViewComponent
    private EntityPicker<Project> projectField;
    @ViewComponent
    private JmixSelect<TaskStatus> statusField;
    @ViewComponent
    private JmixMultiSelectComboBoxPicker<TagGroup> requiredTagGroupsField;

    @ViewComponent
    private CollectionLoader<TagGroup> tagGroupsDl;

    @Autowired
    private DialogWindows dialogWindows;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // in case we open a view for an entity creation by direct navigation
        projectField.setVisible(getEditedEntity().getProject() == null);

        updateStatusFieldIcon();
    }

    @Subscribe(id = "taskDc", target = Target.DATA_CONTAINER)
    public void onTaskDcItemChange(final InstanceContainer.ItemChangeEvent<Task> event) {
        Task item = event.getItem();
        if (item != null) {
            tagGroupsDl.setParameter("project", item.getProject());
        }
    }

    @Subscribe("statusField")
    public void onStatusFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixSelect<?>, ?> event) {
        updateStatusFieldIcon();
    }

    private void updateStatusFieldIcon() {
        TaskStatus status = statusField.getValue();
        Icon icon = status == null ? null : switch (status) {
            case ACTIVE -> {
                Icon openIcon = GravityUiIcon.HAMMER.create();
                openIcon.setColor("var(--aura-green)");
                yield openIcon;
            }
            case INACTIVE -> {
                Icon closeIcon = GravityUiIcon.XMARK.create();
                closeIcon.setColor("var(--aura-red)");
                yield closeIcon;
            }
        };

        statusField.setPrefixComponent(icon);
    }

    @Subscribe("requiredTagGroupsField.entityLookup")
    public void onRequiredTagGroupsFieldEntityLookup(final ActionPerformedEvent event) {
        DialogWindow<TagGroupLookupView> dialogWindow = dialogWindows.lookup(this, TagGroup.class)
                .withViewClass(TagGroupLookupView.class)
                // TODO: gg, remove after jmix-framework/jmix#3473 and jmix-framework/jmix#3474 are fixed
                .withSelectHandler(tagGroups ->
                        requiredTagGroupsField.setTypedValue(new ArrayList<>(tagGroups)))
                .withLookupComponentMultiSelect(true)
                .build();

        dialogWindow.getView().setProject(getEditedEntity().getProject());
        dialogWindow.open();
    }
}
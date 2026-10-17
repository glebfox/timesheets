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

package com.glebfox.timesheets.view.timeentry;

import com.glebfox.timesheets.app.ProjectSupport;
import com.glebfox.timesheets.app.TagsSupport;
import com.glebfox.timesheets.app.TaskSupport;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.task.TaskLookupView;
import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.TimeSource;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.markdowneditor.MarkdownEditor;
import io.jmix.flowui.component.multiselectcombobox.JmixMultiSelectComboBox;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "time-entries/:id", layout = MainView.class)
@ViewController("ts_TimeEntry.detail")
@ViewDescriptor("time-entry-detail-view.xml")
@EditedEntityContainer("timeEntryDc")
@DialogMode(width = "640px", closeOnEsc = true)
public class TimeEntryDetailView extends StandardDetailView<TimeEntry> {

    @ViewComponent
    private JmixSelect<TimeEntryStatus> statusField;
    @ViewComponent
    private EntityComboBox<User> userField;
    @ViewComponent
    private EntityComboBox<Task> taskField;
    @ViewComponent
    private MarkdownEditor rejectionReasonField;
    @ViewComponent
    private H4 tagsTitle;
    @ViewComponent
    private JmixFormLayout tagsForm;

    @ViewComponent
    private CollectionPropertyContainer<Tag> tagsDc;

    @Autowired
    private TimeSource timeSource;
    @Autowired
    private TaskSupport taskSupport;
    @Autowired
    private TagsSupport tagsSupport;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private DialogWindows dialogWindows;
    @ViewComponent
    private MessageBundle messageBundle;
    @Autowired
    private ProjectSupport projectSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private CurrentUserSubstitution currentUserSubstitution;

    private boolean ownTimeEntry = false;
    private boolean tagsChanged = false;

    public void setOwnTimeEntry(boolean ownTimeEntry) {
        this.ownTimeEntry = ownTimeEntry;
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<TimeEntry> event) {
        TimeEntry timeEntry = event.getEntity();

        if (timeEntry.getUser() == null) {
            if (ownTimeEntry) {
                final User user = (User) currentUserSubstitution.getEffectiveUser();
                timeEntry.setUser(user);
            } else {
                userField.setReadOnly(false);
                taskField.setReadOnly(true);
            }
        } else {
            taskField.setReadOnly(timeEntry.getTask() != null);
        }

        if (timeEntry.getDate() == null) {
            final ZonedDateTime zonedDateTime = timeSource.now();
            timeEntry.setDate(zonedDateTime.toLocalDate());
        }

        statusField.setVisible(false);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        updateStatusFieldIcon();
        if (getEditedEntity().getTask() != null) {
            updateTagsFields();
        }
    }

    @Supply(to = "userField", subject = "renderer")
    private Renderer<User> userFieldRenderer() {
        return componentSupport.createUserRenderer();
    }

    @Install(to = "taskField", subject = "itemsFetchCallback")
    private Stream<Task> taskFieldItemsFetchCallback(final Query<Task, String> query) {
        User user = getEditedEntity().getUser();
        String filter = query.getFilter().orElse(null);

        return user != null
                ? taskSupport.getUserActiveTasks(user, query.getOffset(), query.getLimit(), filter)
                : taskSupport.getActiveTasks(query.getOffset(), query.getLimit(), filter);
    }

    @Subscribe("userField.assignSelf")
    public void onUserFieldAssignSelf(final ActionPerformedEvent event) {
        final User user = (User) currentUserSubstitution.getEffectiveUser();
        getEditedEntity().setUser(user);
    }

    @Subscribe("taskField.entityLookup")
    public void onTaskFieldEntityLookup(final ActionPerformedEvent event) {
        DialogWindow<TaskLookupView> dialogWindow = dialogWindows.lookup(taskField)
                .withViewClass(TaskLookupView.class)
                .build();

        dialogWindow.getView().setUser(getEditedEntity().getUser());
        dialogWindow.open();
    }

    @Subscribe(id = "timeEntryDc", target = Target.DATA_CONTAINER)
    public void onTimeEntryDcItemChange(final InstanceContainer.ItemChangeEvent<TimeEntry> event) {
        updateRejectionReasonField();
    }

    @Subscribe(id = "timeEntryDc", target = Target.DATA_CONTAINER)
    public void onTimeEntryDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<TimeEntry> event) {
        if ("status".equals(event.getProperty())) {
            updateRejectionReasonField();
        }

        if ("user".equals(event.getProperty())) {
            taskField.setReadOnly(getEditedEntity().getUser() == null);
            taskField.getDataProvider().refreshAll();
        }

        if ("task".equals(event.getProperty())) {
            updateTagsFields();

            if (!ownTimeEntry) {
                userField.setReadOnly(getEditedEntity().getTask() != null);
            }
        }
    }

    private void updateTagsFields() {
        tagsForm.removeAll();

        Task task = getEditedEntity().getTask();
        if (task != null) {
            Project project = task.getProject();
            List<TagGroup> tagGroups = projectSupport.getProjectTagGroups(project);
            tagsForm.setVisible(!tagGroups.isEmpty());
            tagsTitle.setVisible(!tagGroups.isEmpty());
            Map<TagGroup, List<Tag>> tags = getTimeEntryTagsByGroup();

            List<TagGroup> requiredTagGroups = task.getRequiredTagGroups();

            for (TagGroup tagGroup : requiredTagGroups) {
                tagsForm.add(createTagsField(tagGroup, tags.remove(tagGroup)));

                tagGroups.remove(tagGroup);
            }

            tagsForm.add(createTagsField(
                    tagsSupport.mergeTags(tagGroups),
                    messageBundle.getMessage("otherTagsField.label"),
                    false, tagsSupport.mergeTags(tags.values()))
            );
        } else {
            tagsTitle.setVisible(false);
            tagsForm.setVisible(false);
        }
    }

    private JmixMultiSelectComboBox<Tag> createTagsField(TagGroup tagGroup, @Nullable Collection<Tag> initialValue) {
        return createTagsField(tagGroup.getTags(), tagGroup.getName(), true, initialValue);
    }

    @SuppressWarnings("unchecked")
    private JmixMultiSelectComboBox<Tag> createTagsField(Collection<Tag> tags, String label,
                                                         boolean required, @Nullable Collection<Tag> initialValue) {
        JmixMultiSelectComboBox<Tag> field = uiComponents.create(JmixMultiSelectComboBox.class);
        field.setLabel(label);
        field.setItems(tags);
        field.setRequired(required);
        field.setClearButtonVisible(true);
        if (initialValue != null) {
            field.setTypedValue(initialValue);
        }

        field.addTypedValueChangeListener(event -> tagsChanged = true);

        return field;
    }

    private Map<TagGroup, List<Tag>> getTimeEntryTagsByGroup() {
        return tagsDc.getItems().stream()
                .collect(Collectors.groupingBy(Tag::getTagGroup));
    }

    @Override
    public boolean hasUnsavedChanges() {
        return tagsChanged || super.hasUnsavedChanges();
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        Collection<Tag> tags = collectTagsFromFields();
        tagsDc.setItems(tags);
    }

    @SuppressWarnings("unchecked")
    private Collection<Tag> collectTagsFromFields() {
        Stream<List<Tag>> stream = tagsForm.getOwnComponents().stream()
                .filter(component ->
                        component instanceof JmixMultiSelectComboBox<?> comboBox
                                && CollectionUtils.isNotEmpty(comboBox.getTypedValue()))
                .map(component -> {
                    Collection<Tag> tags = ((JmixMultiSelectComboBox<Tag>) component).getTypedValue();
                    return ImmutableList.copyOf(tags);
                });
        return tagsSupport.mergeTags(stream);
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        tagsChanged = false;
    }

    private void updateRejectionReasonField() {
        rejectionReasonField.setVisible(TimeEntryStatus.REJECTED == getEditedEntity().getStatus());
    }

    @Subscribe("statusField")
    public void onStatusFieldComponentValueChange(
            final ComponentValueChangeEvent<JmixSelect<TimeEntryStatus>, TimeEntryStatus> event) {
        updateStatusFieldIcon();
    }

    private void updateStatusFieldIcon() {
        TimeEntryStatus status = statusField.getValue();
        Icon icon = status == null ? null : switch (status) {
            case NEW -> {
                Icon newIcon = GravityUiIcon.CIRCLE_PLUS.create();
                newIcon.setColor("var(--aura-accent-text-color)");
                yield newIcon;
            }
            case APPROVED -> {
                Icon approvedIcon = GravityUiIcon.CIRCLE_CHECK.create();
                approvedIcon.setColor("var(--aura-green)");
                yield approvedIcon;
            }
            case REJECTED -> {
                Icon rejectedIcon = GravityUiIcon.CIRCLE_EXCLAMATION.create();
                rejectedIcon.setColor("var(--aura-red)");
                yield rejectedIcon;
            }
            case CLOSED -> GravityUiIcon.CIRCLE_XMARK.create();
        };

        statusField.setPrefixComponent(icon);
    }
}
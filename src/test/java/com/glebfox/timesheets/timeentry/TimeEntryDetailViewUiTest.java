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

package com.glebfox.timesheets.timeentry;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextField;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.markdowneditor.MarkdownEditor;
import io.jmix.flowui.component.multiselectcombobox.JmixMultiSelectComboBox;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryDetailViewUiTest {

    static final String REASON = "Logged on the **wrong** task.";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    Messages messages;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    ApprovalTestData data;
    Project project;
    Task task;
    Task secondTask;
    User user;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "time-entry-editor");

        TaskType taskType = data.saveTaskType();
        project = data.saveProject("project", data.saveClient());
        task = data.saveTask(project, taskType);
        secondTask = data.saveTask(project, taskType);
        user = data.saveUser("user", "Time", "Entry");
    }

    @AfterEach
    void tearDown() {
        // Removing an entry for good removes its tags too (#8), so no two entries of a test share a tag
        data.removeAll();
    }

    @Test
    void rejectedEntryShowsNoticeWithReason() {
        TimeEntryDetailView view = openEditor(saveEntry(TimeEntryStatus.REJECTED, REASON));

        Div notice = UiTestUtils.getComponent(view, "rejectionNotice");
        Span title = UiTestUtils.getComponent(view, "rejectionNoticeTitle");
        Markdown reason = UiTestUtils.getComponent(view, "rejectionNoticeReason");
        assertThat(notice.isVisible()).isTrue();
        assertThat(title.getText()).isEqualTo(messages.getMessage(TimeEntryStatus.REJECTED));
        assertThat(reason.getContent()).isEqualTo(REASON);
        assertThat(UiComponentUtils.findComponent(view, "rejectionReasonField")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = TimeEntryStatus.class, names = {"NEW", "APPROVED", "CLOSED"})
    void otherStatusShowsNoNotice(TimeEntryStatus status) {
        TimeEntryDetailView view = openEditor(saveEntry(status, "Old reason"));

        Div notice = UiTestUtils.getComponent(view, "rejectionNotice");
        assertThat(notice.isVisible()).isFalse();
    }

    @Test
    void newEntryShowsNoNotice() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                .newEntity()
                .open();
        TimeEntryDetailView view = UiTestUtils.getCurrentView();

        Div notice = UiTestUtils.getComponent(view, "rejectionNotice");
        assertThat(notice.isVisible()).isFalse();
    }

    @Test
    void blankReasonShowsTitleAlone() {
        TimeEntryDetailView view = openEditor(saveEntry(TimeEntryStatus.REJECTED, "  "));

        Div notice = UiTestUtils.getComponent(view, "rejectionNotice");
        assertThat(notice.isVisible()).isTrue();
        assertThat(UiComponentUtils.findComponent(view, "rejectionNoticeReason")).isEmpty();
    }

    @Test
    void noticeFollowsTheEntryAsLoaded() {
        TimeEntryDetailView rejectedView = openEditor(saveEntry(TimeEntryStatus.REJECTED, REASON));
        JmixSelect<TimeEntryStatus> rejectedStatusField = UiTestUtils.getComponent(rejectedView, "statusField");
        rejectedStatusField.setValue(TimeEntryStatus.NEW);
        Div rejectedNotice = UiTestUtils.getComponent(rejectedView, "rejectionNotice");
        assertThat(rejectedNotice.isVisible()).isTrue();

        TimeEntryDetailView newView = openEditor(saveEntry(TimeEntryStatus.NEW, null));
        JmixSelect<TimeEntryStatus> newStatusField = UiTestUtils.getComponent(newView, "statusField");
        newStatusField.setValue(TimeEntryStatus.REJECTED);
        Div newNotice = UiTestUtils.getComponent(newView, "rejectionNotice");
        assertThat(newNotice.isVisible()).isFalse();
    }

    @Test
    void noticeFollowsTheContainersItem() {
        TimeEntryDetailView view = openEditor(saveEntry(TimeEntryStatus.REJECTED, REASON));
        InstanceContainer<TimeEntry> timeEntryDc = ViewControllerUtils.getViewData(view).getContainer("timeEntryDc");
        DataContext dataContext = ViewControllerUtils.getViewData(view).getDataContext();

        timeEntryDc.setItem(dataContext.merge(saveEntry(TimeEntryStatus.REJECTED, "Duplicate")));

        Div notice = UiTestUtils.getComponent(view, "rejectionNotice");
        Div body = UiTestUtils.getComponent(view, "rejectionNoticeBody");
        assertThat(notice.isVisible()).isTrue();
        // The reason's Markdown is the one made for the first entry, given the new reason
        assertThat(body.getChildren().filter(Markdown.class::isInstance))
                .singleElement()
                .satisfies(reason -> assertThat(((Markdown) reason).getContent()).isEqualTo("Duplicate"));

        timeEntryDc.setItem(dataContext.merge(saveEntry(TimeEntryStatus.NEW, null)));

        assertThat(notice.isVisible()).isFalse();
    }

    @Test
    void savingChangedRejectedEntrySendsItToNew() {
        TimeEntry entry = saveEntry(TimeEntryStatus.REJECTED, REASON);
        TimeEntryDetailView view = openEditor(entry);

        MarkdownEditor descriptionField = UiTestUtils.getComponent(view, "descriptionField");
        descriptionField.setValue("Fixed");
        JmixButton saveAndCloseBtn = UiTestUtils.getComponent(view, "saveAndCloseBtn");
        saveAndCloseBtn.click();

        assertThat(dataManager.load(Id.of(entry)).one().getStatus()).isEqualTo(TimeEntryStatus.NEW);
    }

    @Test
    void savingUnchangedRejectedEntryKeepsItRejected() {
        TagGroup tagGroup = saveTagGroup();
        Tag firstTag = saveTag(tagGroup, "first");
        Tag secondTag = saveTag(tagGroup, "second");
        TimeEntry entry = saveRejectedEntry(List.of(firstTag, secondTag));
        TimeEntryDetailView view = openEditor(entry);

        JmixButton saveAndCloseBtn = UiTestUtils.getComponent(view, "saveAndCloseBtn");
        saveAndCloseBtn.click();

        assertThat(dataManager.load(Id.of(entry)).one().getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
    }

    @Test
    void tagFieldsFollowReadOnlyMode() {
        saveTag(saveTagGroup(), "only");
        TimeEntryDetailView view = openEditor(saveEntry(TimeEntryStatus.APPROVED, null));
        assertThat(tagFields(view)).isNotEmpty();

        view.setReadOnly(true);
        assertThat(tagFields(view)).allMatch(JmixMultiSelectComboBox::isReadOnly);

        // taskField.setValue could refuse an item its lazy items callback has not served; both fire the
        // "task" property change that rebuilds the tag fields
        view.getEditedEntity().setTask(secondTask);
        assertThat(tagFields(view)).isNotEmpty().allMatch(JmixMultiSelectComboBox::isReadOnly);

        view.setReadOnly(false);
        assertThat(tagFields(view)).noneMatch(JmixMultiSelectComboBox::isReadOnly);
    }

    @Test
    void spentTimeFieldShowsTheFormsItAccepts() {
        TimeEntryDetailView view = openNewEntry();

        TextField spentTimeField = UiTestUtils.getComponent(view, "spentTimeField");
        assertThat(spentTimeField.getHelperText()).isEqualTo("Such as 1:30 or 1h 30m");
        assertThat(spentTimeField.getPlaceholder()).isNull();
    }

    @Test
    void newGeneralEntryExplainsTheLockedFields() {
        TimeEntryDetailView view = openNewEntry();
        EntityComboBox<Task> taskField = UiTestUtils.getComponent(view, "taskField");
        EntityComboBox<User> userField = UiTestUtils.getComponent(view, "userField");

        assertThat(tooltipOf(taskField)).isEqualTo("Choose a user first");
        assertThat(tooltipOf(userField)).isNull();

        // taskField.setValue could refuse an item its lazy items callback has not served; setting the property fires
        // the same property change
        view.getEditedEntity().setUser(user);
        assertThat(tooltipOf(taskField)).isNull();

        view.getEditedEntity().setTask(task);
        assertThat(tooltipOf(userField)).isEqualTo("Clear the task to change the user");

        view.getEditedEntity().setTask(null);
        assertThat(tooltipOf(userField)).isNull();
    }

    @Test
    void existingGeneralEntryExplainsTheLockedUser() {
        TimeEntryDetailView view = openEditor(saveEntry(TimeEntryStatus.NEW, null));

        assertThat(tooltipOf(UiTestUtils.getComponent(view, "userField")))
                .isEqualTo("Clear the task to change the user");
        assertThat(tooltipOf(UiTestUtils.getComponent(view, "taskField"))).isNull();
    }

    @Test
    void copiedGeneralEntryExplainsNothing() {
        // As TimeEntryCopyAction opens a copy: a new entry with the user and the task chosen, both locked
        TimeEntry copy = dataManager.create(TimeEntry.class);
        copy.setUser(user);
        copy.setTask(task);
        viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                .newEntity(copy)
                .open();
        TimeEntryDetailView view = UiTestUtils.getCurrentView();

        assertThat(tooltipOf(UiTestUtils.getComponent(view, "userField"))).isNull();
        assertThat(tooltipOf(UiTestUtils.getComponent(view, "taskField"))).isNull();
    }

    @Test
    void ownEntryExplainsNothing() {
        // An own entry takes the signed-in user, which the system user of a UI test is not
        systemAuthenticator.runWithUser("admin", () -> {
            viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                    .newEntity()
                    .withViewConfigurer(editor -> editor.setOwnTimeEntry(true))
                    .open();
            TimeEntryDetailView view = UiTestUtils.getCurrentView();

            // With a task, the user field is locked and the task field is not, so only the own-entry rule keeps
            // the user field quiet
            view.getEditedEntity().setTask(task);
            assertThat(tooltipOf(UiTestUtils.getComponent(view, "userField"))).isNull();
            assertThat(tooltipOf(UiTestUtils.getComponent(view, "taskField"))).isNull();
        });
    }

    @Test
    void readOnlyEditorExplainsNothing() {
        TimeEntryDetailView view = openNewEntry();
        EntityComboBox<Task> taskField = UiTestUtils.getComponent(view, "taskField");
        assertThat(tooltipOf(taskField)).isEqualTo("Choose a user first");

        view.setReadOnly(true);

        // The task field stays locked and the entry has no user, so only the read-only rule keeps it quiet
        assertThat(tooltipOf(taskField)).isNull();
        assertThat(tooltipOf(UiTestUtils.getComponent(view, "userField"))).isNull();
    }

    private TimeEntryDetailView openNewEntry() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                .newEntity()
                .open();
        return UiTestUtils.getCurrentView();
    }

    /**
     * Gives the text of a component's tooltip: {@link Tooltip#forComponent} returns the tooltip the component has, or a
     * new one without text.
     */
    private static @Nullable String tooltipOf(Component component) {
        return Tooltip.forComponent(component).getText();
    }

    private TimeEntryDetailView openEditor(TimeEntry entry) {
        viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                .editEntity(entry)
                .open();
        return UiTestUtils.getCurrentView();
    }

    private TimeEntry saveEntry(TimeEntryStatus status, String rejectionReason) {
        return data.saveEntry(task, user, LocalDate.now(), status, rejectionReason);
    }

    private TimeEntry saveRejectedEntry(List<Tag> tags) {
        return data.save(dataManager.create(TimeEntry.class), entry -> {
            entry.setTask(task);
            entry.setUser(user);
            entry.setDate(LocalDate.now());
            entry.setSpentTime(WorkTime.ofHours(4));
            entry.setStatus(TimeEntryStatus.REJECTED);
            entry.setRejectionReason(REASON);
            entry.setTags(new ArrayList<>(tags));
        });
    }

    private TagGroup saveTagGroup() {
        return data.save(dataManager.create(TagGroup.class), group -> {
            group.setName("time-entry-editor tag group");
            group.setProject(project);
        });
    }

    private Tag saveTag(TagGroup tagGroup, String name) {
        return data.save(dataManager.create(Tag.class), tag -> {
            tag.setName("time-entry-editor " + name + " tag");
            tag.setTagGroup(tagGroup);
        });
    }

    @SuppressWarnings("unchecked")
    private List<JmixMultiSelectComboBox<Tag>> tagFields(TimeEntryDetailView view) {
        JmixFormLayout tagsForm = UiTestUtils.getComponent(view, "tagsForm");
        return tagsForm.getChildren()
                .filter(JmixMultiSelectComboBox.class::isInstance)
                .map(component -> (JmixMultiSelectComboBox<Tag>) component)
                .toList();
    }
}

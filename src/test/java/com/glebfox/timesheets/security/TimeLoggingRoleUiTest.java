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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.markdowneditor.MarkdownEditor;
import io.jmix.flowui.component.multiselectcombobox.JmixMultiSelectComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The holder signs in with UI: minimal access and holds the role besides, and no row-level role, so only the role's
 * own grants are checked. The fixture is one project where the holder is a Member, with a task and a tag group of
 * one tag.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeLoggingRoleUiTest {

    static final String USERNAME_PREFIX = "time-logging-ui-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    User holderUser;
    Task task;
    Tag tag;
    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        holderUser = save(dataManager.create(User.class), u -> u.setUsername(USERNAME_PREFIX + UUID.randomUUID()));
        assignRole(holderUser, UiMinimalRole.CODE);
        assignRole(holderUser, TimeLoggingRole.CODE);

        Client client = save(dataManager.create(Client.class), c -> c.setName(USERNAME_PREFIX + "client"));
        TaskType taskType = save(dataManager.create(TaskType.class), t -> t.setName(USERNAME_PREFIX + "task type"));
        Project project = save(dataManager.create(Project.class), p -> {
            p.setName(USERNAME_PREFIX + "project");
            p.setClient(client);
        });
        TagGroup tagGroup = save(dataManager.create(TagGroup.class), g -> {
            g.setName(USERNAME_PREFIX + "tag group");
            g.setProject(project);
        });
        tag = save(dataManager.create(Tag.class), t -> {
            t.setName(USERNAME_PREFIX + "tag");
            t.setTagGroup(tagGroup);
        });
        task = save(dataManager.create(Task.class), t -> {
            t.setName(USERNAME_PREFIX + "task");
            t.setProject(project);
            t.setType(taskType);
        });
        ProjectRole memberRole = save(dataManager.create(ProjectRole.class), r -> {
            r.setName(USERNAME_PREFIX + "member role");
            r.setType(ProjectRoleType.MEMBER);
        });
        save(dataManager.create(ProjectParticipant.class), p -> {
            p.setProject(project);
            p.setUser(holderUser);
            p.setRole(memberRole);
        });
    }

    @Test
    void holderLogsTimeInTheEditor() {
        systemAuthenticator.runWithUser(holderUser.getUsername(), () -> {
            // As the calendar, My time entries, and My tasks open the editor on a new entry of the holder's own
            viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                    .newEntity()
                    .withViewConfigurer(editor -> editor.setOwnTimeEntry(true))
                    .open();
            TimeEntryDetailView editor = UiTestUtils.getCurrentView();
            assertThat(editor.isReadOnly()).isFalse();

            // The editor sets the user, which the role leaves view-only
            EntityComboBox<User> userField = UiTestUtils.getComponent(editor, "userField");
            assertThat(userField.getValue()).extracting(User::getId).isEqualTo(holderUser.getId());
            assertThat(userField.isReadOnly()).isTrue();

            TypedDatePicker<LocalDate> dateField = UiTestUtils.getComponent(editor, "dateField");
            EntityComboBox<Task> taskField = UiTestUtils.getComponent(editor, "taskField");
            TypedTextField<WorkTime> spentTimeField = UiTestUtils.getComponent(editor, "spentTimeField");
            MarkdownEditor descriptionField = UiTestUtils.getComponent(editor, "descriptionField");
            assertThat(dateField.isReadOnly()).as("date").isFalse();
            assertThat(taskField.isReadOnly()).as("task").isFalse();
            assertThat(spentTimeField.isReadOnly()).as("spent time").isFalse();
            assertThat(descriptionField.isReadOnly()).as("description").isFalse();

            // taskField.setValue could refuse an item its lazy items callback has not served; setting the property
            // fires the "task" property change that builds the tag fields, from the project's tag groups the holder
            // reads
            editor.getEditedEntity().setTask(task);
            List<JmixMultiSelectComboBox<Tag>> tagFields = tagFields(editor);
            assertThat(tagFields).singleElement().satisfies(field -> assertThat(field.isReadOnly()).isFalse());
            tagFields.getFirst().setTypedValue(List.of(tag));
            spentTimeField.setTypedValue(WorkTime.ofHours(2));
            descriptionField.setValue("Logged by the holder");

            JmixButton saveAndCloseBtn = UiTestUtils.getComponent(editor, "saveAndCloseBtn");
            saveAndCloseBtn.click();
        });

        TimeEntry stored = dataManager.load(TimeEntry.class)
                .query("e.user = ?1", holderUser)
                .one();
        assertThat(stored.getTask().getId()).isEqualTo(task.getId());
        assertThat(stored.getSpentTime()).isEqualTo(WorkTime.ofHours(2));
        assertThat(stored.getDescription()).isEqualTo("Logged by the holder");
        assertThat(stored.getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(stored.getTags()).extracting(Tag::getId).containsExactly(tag.getId());
    }

    @AfterEach
    void tearDown() {
        // The entry the editor saved first: removing it for good removes its tag too (#8), which the loop below then
        // skips. The rest is soft-deleted, so it is removed for good, reloaded by id
        if (holderUser != null) {
            dataManager.load(TimeEntry.class)
                    .query("e.user = ?1", holderUser)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list()
                    .forEach(entry -> dataManager.save(new SaveContext()
                            .removing(entry)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    @SuppressWarnings("unchecked")
    private static List<JmixMultiSelectComboBox<Tag>> tagFields(TimeEntryDetailView editor) {
        JmixFormLayout tagsForm = UiTestUtils.getComponent(editor, "tagsForm");
        return tagsForm.getChildren()
                .filter(JmixMultiSelectComboBox.class::isInstance)
                .map(component -> (JmixMultiSelectComboBox<Tag>) component)
                .toList();
    }

    private <E> E save(E entity, Consumer<E> init) {
        init.accept(entity);
        E saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }
}

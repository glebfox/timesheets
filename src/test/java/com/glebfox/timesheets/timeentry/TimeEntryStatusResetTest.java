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
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A save that changes what an Approved or Rejected entry says sends it back to New.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TimeEntryStatusResetTest {

    enum ContentEdit {DATE, SPENT_TIME, DESCRIPTION, TASK, USER, TAGS}

    @Autowired
    DataManager dataManager;

    ApprovalTestData data;
    LocalDate today;
    Task task;
    Task secondTask;
    User user;
    User secondUser;
    Tag firstTag;
    Tag secondTag;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "status-reset");
        today = LocalDate.now();

        TaskType taskType = data.saveTaskType();
        Project project = data.saveProject("project", data.saveClient());
        task = data.saveTask(project, taskType);
        secondTask = data.saveTask(project, taskType);
        user = data.saveUser("user", "Status", "Reset");
        secondUser = data.saveUser("second", "Second", "User");

        TagGroup tagGroup = data.save(dataManager.create(TagGroup.class), group -> {
            group.setName("status-reset tag group");
            group.setProject(project);
        });
        firstTag = data.save(dataManager.create(Tag.class), tag -> {
            tag.setName("status-reset first tag");
            tag.setTagGroup(tagGroup);
        });
        secondTag = data.save(dataManager.create(Tag.class), tag -> {
            tag.setName("status-reset second tag");
            tag.setTagGroup(tagGroup);
        });
    }

    @AfterEach
    void tearDown() {
        // Removing an entry for good removes its tags too (#8), so no two entries of a test share a tag
        data.removeAll();
    }

    static Stream<Arguments> statusesAndEdits() {
        return Stream.of(TimeEntryStatus.APPROVED, TimeEntryStatus.REJECTED)
                .flatMap(status -> Arrays.stream(ContentEdit.values())
                        .map(edit -> Arguments.of(status, edit)));
    }

    @ParameterizedTest
    @MethodSource("statusesAndEdits")
    void changingContentOfApprovedOrRejectedEntrySetsNew(TimeEntryStatus status, ContentEdit edit) {
        TimeEntry entry = saveEntry(status);

        TimeEntry loaded = loadAsEditor(entry);
        apply(edit, loaded);
        dataManager.save(loaded);

        TimeEntry reloaded = reload(entry);
        assertThat(reloaded.getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reloaded.getRejectionReason()).isEqualTo("Wrong task");
    }

    @ParameterizedTest
    @EnumSource(value = TimeEntryStatus.class, names = {"NEW", "CLOSED"})
    void changingContentOfNewOrClosedEntryKeepsStatus(TimeEntryStatus status) {
        TimeEntry entry = saveEntry(status);

        TimeEntry loaded = loadAsEditor(entry);
        loaded.setDescription("Fixed");
        dataManager.save(loaded);

        assertThat(reload(entry).getStatus()).isEqualTo(status);
    }

    @Test
    void saveThatSetsStatusKeepsIt() {
        TimeEntry entry = saveEntry(TimeEntryStatus.REJECTED);

        TimeEntry loaded = loadAsEditor(entry);
        loaded.setDescription("Fixed");
        loaded.setStatus(TimeEntryStatus.APPROVED);
        dataManager.save(loaded);

        assertThat(reload(entry).getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
    }

    @Test
    void changingOnlyRejectionReasonKeepsStatus() {
        TimeEntry entry = saveEntry(TimeEntryStatus.REJECTED);

        TimeEntry loaded = loadAsEditor(entry);
        loaded.setRejectionReason("Duplicate");
        dataManager.save(loaded);

        TimeEntry reloaded = reload(entry);
        assertThat(reloaded.getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
        assertThat(reloaded.getRejectionReason()).isEqualTo("Duplicate");
    }

    @Test
    void unchangedSaveKeepsRejected() {
        TimeEntry entry = saveEntry(TimeEntryStatus.REJECTED, "Calibration", List.of(firstTag));

        TimeEntry loaded = loadAsEditor(entry);
        loaded.setDescription("Calibration");
        loaded.setTags(new ArrayList<>(loaded.getTags()));
        dataManager.save(loaded);

        assertThat(reload(entry).getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
    }

    @Test
    void blankDescriptionAgainstNullIsNoChange() {
        TimeEntry entry = saveEntry(TimeEntryStatus.REJECTED);

        TimeEntry loaded = loadAsEditor(entry);
        loaded.setDescription("");
        dataManager.save(loaded);

        assertThat(reload(entry).getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
    }

    @Test
    void sameTagsInAnotherOrderAreNoChange() {
        TimeEntry entry = saveEntry(TimeEntryStatus.REJECTED, null, List.of(firstTag, secondTag));

        TimeEntry loaded = loadAsEditor(entry);
        loaded.setTags(new ArrayList<>(loaded.getTags().reversed()));
        dataManager.save(loaded);

        assertThat(reload(entry).getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
    }

    @Test
    void softDeletedTagIsNoChange() {
        TimeEntry entry = saveEntry(TimeEntryStatus.REJECTED, null, List.of(firstTag, secondTag));
        dataManager.remove(secondTag);

        TimeEntry loaded = loadAsEditor(entry);
        assertThat(loaded.getTags()).containsExactly(firstTag);
        loaded.setTags(new ArrayList<>(loaded.getTags()));
        dataManager.save(loaded);

        assertThat(reload(entry).getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
    }

    @Test
    void newEntryKeepsItsStatus() {
        TimeEntry entry = saveEntry(TimeEntryStatus.APPROVED);

        assertThat(reload(entry).getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
    }

    @Test
    void entryWithUnloadedReferencesIsComparedOnLoadedAttributesOnly() {
        TimeEntry moved = saveEntry(TimeEntryStatus.REJECTED);
        TimeEntry rejectedAgain = saveEntry(TimeEntryStatus.REJECTED);

        TimeEntry loadedMoved = dataManager.load(Id.of(moved)).fetchPlan(FetchPlan.LOCAL).one();
        loadedMoved.setDate(today.plusDays(1));
        dataManager.save(loadedMoved);

        TimeEntry loadedRejectedAgain = dataManager.load(Id.of(rejectedAgain)).fetchPlan(FetchPlan.LOCAL).one();
        loadedRejectedAgain.setRejectionReason("Duplicate");
        dataManager.save(loadedRejectedAgain);

        assertThat(reload(moved).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(rejectedAgain).getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
    }

    private TimeEntry saveEntry(TimeEntryStatus status) {
        return data.saveEntry(task, user, today, status, "Wrong task");
    }

    private TimeEntry saveEntry(TimeEntryStatus status, String description, List<Tag> tags) {
        return data.save(dataManager.create(TimeEntry.class), entry -> {
            entry.setTask(task);
            entry.setUser(user);
            entry.setDate(today);
            entry.setSpentTime(WorkTime.ofHours(4));
            entry.setStatus(status);
            entry.setRejectionReason("Wrong task");
            entry.setDescription(description);
            entry.setTags(new ArrayList<>(tags));
        });
    }

    /**
     * Loads the entry with the attributes the time entry editor loads.
     */
    private TimeEntry loadAsEditor(TimeEntry entry) {
        return dataManager.load(Id.of(entry))
                .fetchPlan(plan -> plan.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE))
                .one();
    }

    private TimeEntry reload(TimeEntry entry) {
        return dataManager.load(Id.of(entry)).one();
    }

    private void apply(ContentEdit edit, TimeEntry entry) {
        switch (edit) {
            case DATE -> entry.setDate(today.plusDays(1));
            case SPENT_TIME -> entry.setSpentTime(WorkTime.ofHours(2));
            case DESCRIPTION -> entry.setDescription("Fixed");
            case TASK -> entry.setTask(secondTask);
            case USER -> entry.setUser(secondUser);
            case TAGS -> entry.setTags(new ArrayList<>(List.of(firstTag)));
        }
    }
}

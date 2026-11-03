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

package com.glebfox.timesheets.app.util;

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.APPROVED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.CLOSED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

class ApprovalOverviewTest {

    static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    final User user = user("Ann");

    @Test
    void totalsCountEachStatus() {
        ApprovalTotals totals = ApprovalTotals.of(List.of(
                entry(user, DAY, "Task", NEW, WorkTime.ofHours(2)),
                entry(user, DAY, "Task", NEW, WorkTime.of(1, 30)),
                entry(user, DAY, "Task", APPROVED, WorkTime.ofHours(8)),
                entry(user, DAY, "Task", REJECTED, WorkTime.ofHours(3)),
                entry(user, DAY, "Task", CLOSED, WorkTime.ofHours(1))));

        assertThat(totals.count()).isEqualTo(5);
        assertThat(totals.time()).isEqualTo(WorkTime.of(15, 30));
        assertThat(totals.count(NEW)).isEqualTo(2);
        assertThat(totals.time(NEW)).isEqualTo(WorkTime.of(3, 30));
        assertThat(totals.count(APPROVED)).isEqualTo(1);
        assertThat(totals.time(APPROVED)).isEqualTo(WorkTime.ofHours(8));
        assertThat(totals.count(REJECTED)).isEqualTo(1);
        assertThat(totals.time(REJECTED)).isEqualTo(WorkTime.ofHours(3));
        assertThat(totals.count(CLOSED)).isEqualTo(1);
        assertThat(totals.time(CLOSED)).isEqualTo(WorkTime.ofHours(1));
        assertThat(totals.hasWaiting()).isTrue();
        assertThat(totals.isFullyApproved()).isFalse();
    }

    @Test
    void emptyTotalsAreZero() {
        ApprovalTotals totals = ApprovalTotals.of(List.of());

        assertThat(totals.count()).isZero();
        assertThat(totals.time()).isEqualTo(WorkTime.ZERO);
        for (TimeEntryStatus status : TimeEntryStatus.values()) {
            assertThat(totals.count(status)).isZero();
            assertThat(totals.time(status)).isEqualTo(WorkTime.ZERO);
        }
        assertThat(totals.hasWaiting()).isFalse();
    }

    @Test
    void onlyApprovedOrClosedIsFullyApproved() {
        assertThat(ApprovalTotals.of(List.of(
                entry(user, DAY, "Task", APPROVED, WorkTime.ofHours(8)),
                entry(user, DAY, "Task", CLOSED, WorkTime.ofHours(1)))).isFullyApproved()).isTrue();
        assertThat(ApprovalTotals.of(List.of()).isFullyApproved()).isFalse();
        assertThat(ApprovalTotals.of(List.of(
                entry(user, DAY, "Task", APPROVED, WorkTime.ofHours(8)),
                entry(user, DAY, "Task", REJECTED, WorkTime.ofHours(1)))).isFullyApproved()).isFalse();
        assertThat(ApprovalTotals.of(List.of(
                entry(user, DAY, "Task", APPROVED, WorkTime.ofHours(8)),
                entry(user, DAY, "Task", NEW, WorkTime.ofHours(1)))).isFullyApproved()).isFalse();
    }

    @Test
    void dayStatusPrefersRejectedThenNewThenApproved() {
        assertThat(dayStatus(APPROVED, NEW)).contains(NEW);
        assertThat(dayStatus(NEW, REJECTED)).contains(REJECTED);
        assertThat(dayStatus(CLOSED, APPROVED)).contains(APPROVED);
        assertThat(dayStatus(CLOSED)).contains(CLOSED);
        assertThat(dayStatus()).isEmpty();
    }

    @Test
    void peopleWithWaitingEntriesComeFirstThenByName() {
        User zoe = user("Zoe");
        User adam = user("adam");
        User bob = user("Bob");

        ApprovalOverview overview = ApprovalOverview.of(List.of(
                entry(zoe, DAY, "Task", NEW, WorkTime.ofHours(1)),
                entry(adam, DAY, "Task", APPROVED, WorkTime.ofHours(1)),
                entry(bob, DAY, "Task", NEW, WorkTime.ofHours(1))), User::getUsername);

        assertThat(overview.people()).extracting(person -> person.user().getUsername())
                .containsExactly("Bob", "Zoe", "adam");
    }

    @Test
    void peopleWithoutWaitingEntriesAreOrderedByNameIgnoringCase() {
        User zoe = user("zoe");
        User adam = user("Adam");
        User bob = user("bob");

        ApprovalOverview overview = ApprovalOverview.of(List.of(
                entry(zoe, DAY, "Task", APPROVED, WorkTime.ofHours(1)),
                entry(adam, DAY, "Task", REJECTED, WorkTime.ofHours(1)),
                entry(bob, DAY, "Task", CLOSED, WorkTime.ofHours(1))), User::getUsername);

        assertThat(overview.people()).extracting(person -> person.user().getUsername())
                .containsExactly("Adam", "bob", "zoe");
    }

    @Test
    void entriesOfEqualUsersCollapseIntoOnePerson() {
        UUID id = UUID.randomUUID();
        User first = user("Ann");
        first.setId(id);
        User same = user("Ann");
        same.setId(id);

        ApprovalOverview overview = ApprovalOverview.of(List.of(
                entry(first, DAY, "Task", NEW, WorkTime.ofHours(1)),
                entry(same, DAY, "Task", APPROVED, WorkTime.ofHours(2))), User::getUsername);

        assertThat(overview.people()).hasSize(1);
        assertThat(overview.people().getFirst().entries()).hasSize(2);
        assertThat(overview.people().getFirst().totals().time()).isEqualTo(WorkTime.ofHours(3));
    }

    @Test
    void overviewTotalsCoverEveryPerson() {
        ApprovalOverview overview = ApprovalOverview.of(List.of(
                entry(user("Ann"), DAY, "Task", NEW, WorkTime.ofHours(1)),
                entry(user("Bob"), DAY, "Task", APPROVED, WorkTime.ofHours(2))), User::getUsername);

        assertThat(overview.totals().count()).isEqualTo(2);
        assertThat(overview.totals().time()).isEqualTo(WorkTime.ofHours(3));
        assertThat(overview.people()).extracting(person -> person.totals().count()).containsExactly(1, 1);
    }

    @Test
    void fullyApprovedPeopleAreCounted() {
        ApprovalOverview overview = ApprovalOverview.of(List.of(
                entry(user("Ann"), DAY, "Task", APPROVED, WorkTime.ofHours(1)),
                entry(user("Bob"), DAY, "Task", NEW, WorkTime.ofHours(1)),
                entry(user("Cat"), DAY, "Task", REJECTED, WorkTime.ofHours(1))), User::getUsername);

        assertThat(overview.fullyApprovedPeople()).isEqualTo(1);
    }

    @Test
    void anEmptyOverviewHasNoPeople() {
        ApprovalOverview overview = ApprovalOverview.of(List.of(), User::getUsername);

        assertThat(overview.people()).isEmpty();
        assertThat(overview.totals().count()).isZero();
        assertThat(overview.fullyApprovedPeople()).isZero();
    }

    @Test
    void findLooksPeopleUpByUserId() {
        User ann = user("Ann");
        User bob = user("Bob");
        ApprovalOverview overview = ApprovalOverview.of(List.of(
                entry(ann, DAY, "Task", NEW, WorkTime.ofHours(1)),
                entry(bob, DAY, "Task", NEW, WorkTime.ofHours(1))), User::getUsername);

        assertThat(overview.find(bob.getId())).map(person -> person.user().getUsername()).contains("Bob");
        assertThat(overview.find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void personEntriesAreSortedByDateThenTask() {
        TimeEntry laterDay = entry(user, DAY.plusDays(1), "Alpha", NEW, WorkTime.ofHours(1));
        TimeEntry sameDayZulu = entry(user, DAY, "Zulu", NEW, WorkTime.ofHours(1));
        TimeEntry sameDayAlpha = entry(user, DAY, "Alpha", NEW, WorkTime.ofHours(1));
        TimeEntry earlierDay = entry(user, DAY.minusDays(1), "Mike", NEW, WorkTime.ofHours(1));

        ApprovalOverview overview = ApprovalOverview.of(
                List.of(laterDay, sameDayZulu, sameDayAlpha, earlierDay), User::getUsername);

        assertThat(overview.people().getFirst().entries())
                .containsExactly(earlierDay, sameDayAlpha, sameDayZulu, laterDay);
    }

    private static Optional<TimeEntryStatus> dayStatus(TimeEntryStatus... statuses) {
        User user = user("Ann");
        return ApprovalTotals.dayStatus(Arrays.stream(statuses)
                .map(status -> entry(user, DAY, "Task", status, WorkTime.ofHours(1)))
                .toList());
    }

    private static User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        return user;
    }

    private static TimeEntry entry(User user, LocalDate date, String taskName, TimeEntryStatus status,
                                   WorkTime spentTime) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setName(taskName);

        TimeEntry entry = new TimeEntry();
        entry.setId(UUID.randomUUID());
        entry.setUser(user);
        entry.setTask(task);
        entry.setDate(date);
        entry.setStatus(status);
        entry.setSpentTime(spentTime);
        return entry;
    }
}

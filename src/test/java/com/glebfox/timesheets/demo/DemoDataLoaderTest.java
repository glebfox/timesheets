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

package com.glebfox.timesheets.demo;

import com.glebfox.timesheets.app.TimeEntryApprovalSupport;
import com.glebfox.timesheets.app.WorkPlanSupport;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.component.avatarupload.AvatarImageFormat;
import com.glebfox.timesheets.demo.DemoDataLoader.DemoAbsence;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.AbsenceManagementRole;
import com.glebfox.timesheets.security.TimeEntryApprovalRole;
import com.glebfox.timesheets.security.UiMinimalRole;
import com.glebfox.timesheets.security.WorkScheduleManagementRole;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.TimeSource;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.ByteBuffer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Starts the application as a {@code dev} start does, on a database of its own: the demo data
 * would otherwise land in the database the other tests share.
 */
@SpringBootTest(properties = "main.datasource.url=jdbc:hsqldb:mem:demo")
@ActiveProfiles({"test", "dev"})
public class DemoDataLoaderTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    DemoDataLoader demoDataLoader;
    @Autowired
    WorkPlanSupport workPlanSupport;
    @Autowired
    TimeEntryApprovalSupport approvalSupport;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    TimeSource timeSource;
    // Not reset after each test: the calls to check come from the load on startup
    @MockitoBean(reset = MockReset.NONE)
    UiEventPublisher uiEventPublisher;

    @Test
    void loadsOnStartup() {
        systemAuthenticator.runWithSystem(() -> {
            assertThat(dataManager.load(Client.class).all().list()).hasSize(6);
            assertThat(dataManager.load(Project.class).all().list()).hasSize(10);
            assertThat(dataManager.load(User.class).all().list())
                    .hasSize(18)
                    .filteredOn(user -> user.getUsername().equals("admin"))
                    .singleElement()
                    .satisfies(admin -> assertThat(admin.getLastName()).isEqualTo("Breen"));
            assertThat(loadEntries()).hasSizeGreaterThan(500);
        });
    }

    @Test
    void runsTheListenersAfterCommitAuthenticated() {
        // After the commit, the listener loads each new entry to tell its user, which fails without authentication
        int entryCount = systemAuthenticator.withSystem(() -> loadEntries().size());
        verify(uiEventPublisher, times(entryCount)).publishEventForUsers(any(), any());
    }

    @Test
    void logsTimeUpToTodayOnWorkdaysOnly() {
        LocalDate today = timeSource.now().toLocalDate();
        systemAuthenticator.runWithSystem(() -> {
            List<TimeEntry> entries = loadEntries();
            assertThat(entries).noneMatch(entry -> entry.getDate().isAfter(today));
            assertThat(entries).anyMatch(entry -> entry.getDate().isAfter(today.minusDays(14)));

            LocalDate start = entries.stream().map(TimeEntry::getDate).min(LocalDate::compareTo).orElseThrow();
            Map<User, List<TimeEntry>> entriesByUser = entries.stream()
                    .collect(Collectors.groupingBy(TimeEntry::getUser));
            entriesByUser.forEach((user, userEntries) -> {
                WorkPlan plan = workPlanSupport.loadWorkPlan(user, start, today.plusDays(1));
                assertThat(userEntries)
                        .as("entries of %s", user.getUsername())
                        .allMatch(entry -> plan.isWorkday(entry.getDate()));
            });
        });
    }

    @Test
    void logsTimeOnlyOnTheProjectsOfItsUser() {
        systemAuthenticator.runWithSystem(() -> {
            Set<String> loggingParticipants = dataManager.load(ProjectParticipant.class)
                    .all()
                    .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                            .add("user", FetchPlan.BASE)
                            .add("role", FetchPlan.BASE)
                            .add("project", FetchPlan.BASE))
                    .list().stream()
                    .filter(participant -> participant.getRole().getType() != ProjectRoleType.OBSERVER)
                    .map(participant -> participant.getUser().getUsername() + "@" + participant.getProject().getName())
                    .collect(Collectors.toSet());

            assertThat(loadEntries()).allMatch(entry -> loggingParticipants.contains(
                    entry.getUser().getUsername() + "@" + entry.getTask().getProject().getName()));
        });
    }

    @Test
    void tagsEveryEntryFromTheRequiredGroupsOfItsTask() {
        systemAuthenticator.runWithSystem(() -> assertThat(loadEntries()).allSatisfy(entry -> {
            Set<TagGroup> taggedGroups = entry.getTags().stream()
                    .map(Tag::getTagGroup)
                    .collect(Collectors.toSet());
            assertThat(taggedGroups).containsAll(entry.getTask().getRequiredTagGroups());
        }));
    }

    @Test
    void rejectsSomeEntriesWithAReason() {
        systemAuthenticator.runWithSystem(() -> assertThat(loadEntries())
                .filteredOn(entry -> entry.getStatus() == TimeEntryStatus.REJECTED)
                .isNotEmpty()
                .allMatch(entry -> entry.getRejectionReason() != null)
                .anyMatch(entry -> entry.getUser().getUsername().equals("admin")));
    }

    @Test
    void mossmanApprovesBorealisRecovery() {
        List<Project> projects = systemAuthenticator.withUser("mossman", approvalSupport::loadProjectsToApprove);
        assertThat(projects).extracting(Project::getName).containsExactly("Borealis Recovery");

        // Her own entries are not hers to approve, and the Manager's are not an Approver's
        List<TimeEntry> entries = systemAuthenticator.withUser("mossman", () ->
                approvalSupport.loadEntriesToApprove(null, LocalDate.of(2000, 1, 1), timeSource.now().toLocalDate()));
        assertThat(entries).isNotEmpty();
        assertThat(entries).extracting(entry -> entry.getTask().getProject().getName())
                .containsOnly("Borealis Recovery");
        assertThat(entries).extracting(entry -> entry.getUser().getUsername())
                .doesNotContain("mossman", "magnusson");
    }

    @Test
    void mossmanHoldsGranularRoles() {
        List<String> roleCodes = systemAuthenticator.withSystem(() -> dataManager.load(RoleAssignmentEntity.class)
                .query("e.username = :username")
                .parameter("username", "mossman")
                .list().stream()
                .map(RoleAssignmentEntity::getRoleCode)
                .toList());

        assertThat(roleCodes).containsExactlyInAnyOrder(UiMinimalRole.CODE, WorkScheduleManagementRole.CODE,
                TimeEntryApprovalRole.CODE, AbsenceManagementRole.CODE);
    }

    @Test
    void createsAbsenceTypesAndAbsences() {
        LocalDate today = timeSource.now().toLocalDate();
        systemAuthenticator.runWithSystem(() -> {
            assertThat(dataManager.load(AbsenceType.class).all().list())
                    .extracting(AbsenceType::getName)
                    .containsExactlyInAnyOrder("Vacation", "Sick leave", "Day off");

            List<DemoAbsence> absences = loadAbsences();
            assertThat(absences)
                    .hasSizeGreaterThanOrEqualTo(5)
                    .anyMatch(absence -> includes(absence, today))
                    .anyMatch(absence -> crossesTheFirstOfTheMonth(absence, today));
            assertNoUserAbsentTwiceOnADay(absences, "demo absences");
        });
    }

    @Test
    void logsNoTimeOnAbsences() {
        LocalDate today = timeSource.now().toLocalDate();
        systemAuthenticator.runWithSystem(() -> {
            List<DemoAbsence> absences = loadAbsences();
            // Some of them fall on the days the time is logged over
            assertThat(absences).anyMatch(absence -> absence.firstDay().isBefore(today));

            assertThat(loadEntries())
                    .filteredOn(entry -> absences.stream().anyMatch(absence ->
                            absence.username().equals(entry.getUser().getUsername())
                                    && includes(absence, entry.getDate())))
                    .extracting(entry -> entry.getUser().getUsername() + " " + entry.getDate())
                    .isEmpty();
        });
    }

    @Test
    void datesAbsencesAsDesignedWhateverTheDay() {
        // Every day of the week on every day of the month, around a leap year
        LocalDate.of(2026, 1, 1).datesUntil(LocalDate.of(2031, 1, 1)).forEach(today -> {
            NavigableSet<LocalDate> holidays = DemoDataLoader.holidaysAround(today).navigableKeySet();
            List<DemoAbsence> absences = DemoDataLoader.absencesOf(today, holidays);
            LocalDate thisWeek = today.with(DayOfWeek.MONDAY);
            LocalDate pastStart = thisWeek.minusWeeks(DemoTimeEntryGenerator.WEEKS);
            String description = "absences on " + today;

            assertThat(absences).as(description)
                    .isNotEmpty()
                    .allMatch(absence -> !absence.lastDay().isBefore(absence.firstDay()))
                    .noneMatch(absence -> Set.of("admin", "mossman").contains(absence.username()))
                    // The past ones are in the weeks the time is logged over
                    .allMatch(absence -> !absence.lastDay().isBefore(thisWeek)
                            || !absence.firstDay().isBefore(pastStart))
                    .as("%s: a two-week vacation in the past weeks across a weekend", description)
                    .anyMatch(absence -> absence.type().equals("Vacation")
                            && !absence.firstDay().isBefore(pastStart) && absence.lastDay().isBefore(thisWeek)
                            && dayCount(absence) == 14 && crossesAWeekend(absence))
                    .as("%s: a sick leave of three workdays in the past weeks", description)
                    .anyMatch(absence -> absence.type().equals("Sick leave")
                            && !absence.firstDay().isBefore(pastStart) && absence.lastDay().isBefore(thisWeek)
                            && dayCount(absence) == 3
                            && days(absence).allMatch(day -> isWorkday(day, holidays)))
                    .as("%s: a vacation through the current week", description)
                    .anyMatch(absence -> absence.type().equals("Vacation")
                            && includes(absence, thisWeek) && includes(absence, thisWeek.plusDays(6)))
                    .as("%s: a day off in the coming two weeks", description)
                    .anyMatch(absence -> absence.type().equals("Day off")
                            && absence.firstDay().equals(absence.lastDay())
                            && absence.firstDay().isAfter(today) && !absence.firstDay().isAfter(today.plusWeeks(2))
                            && isWorkday(absence.firstDay(), holidays))
                    .as("%s: one across the first day of the month", description)
                    .anyMatch(absence -> crossesTheFirstOfTheMonth(absence, today))
                    .as("%s: one over a holiday", description)
                    .anyMatch(absence -> days(absence).anyMatch(holidays::contains));
            assertNoUserAbsentTwiceOnADay(absences, description);
        });
    }

    @Test
    void storesImagesOfAcceptedFormats() {
        systemAuthenticator.runWithSystem(() -> {
            assertThat(dataManager.load(Client.class).all().list())
                    .filteredOn(client -> client.getImage() != null)
                    .hasSize(5)
                    .allMatch(client -> isAcceptedImage(client.getImage()));
            assertThat(dataManager.load(User.class).all().list())
                    .allMatch(user -> user.getAvatar() != null && isAcceptedImage(user.getAvatar()));
        });
    }

    @Test
    void loadsNothingMoreWhenTheDatabaseHasData() {
        Function<Class<?>, Integer> count = entityClass -> systemAuthenticator.withSystem(() ->
                dataManager.load(entityClass).all().list().size());
        int clients = count.apply(Client.class);
        int entries = count.apply(TimeEntry.class);

        // No authentication around the call, as on the startup thread
        demoDataLoader.loadIfEmpty();

        assertThat(count.apply(Client.class)).isEqualTo(clients);
        assertThat(count.apply(TimeEntry.class)).isEqualTo(entries);
    }

    private List<TimeEntry> loadEntries() {
        return dataManager.load(TimeEntry.class)
                .all()
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("task", taskBuilder -> taskBuilder.addFetchPlan(FetchPlan.BASE)
                                .add("project", FetchPlan.BASE)
                                .add("requiredTagGroups", FetchPlan.BASE))
                        .add("tags", tagBuilder -> tagBuilder.addFetchPlan(FetchPlan.BASE)
                                .add("tagGroup", FetchPlan.BASE)))
                .list();
    }

    private List<DemoAbsence> loadAbsences() {
        return dataManager.load(Absence.class)
                .all()
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("type", FetchPlan.BASE))
                .list().stream()
                .map(absence -> new DemoAbsence(absence.getUser().getUsername(), absence.getType().getName(),
                        absence.getFirstDay(), absence.getLastDay(), absence.getComment()))
                .toList();
    }

    private static void assertNoUserAbsentTwiceOnADay(List<DemoAbsence> absences, String description) {
        absences.stream()
                .collect(Collectors.groupingBy(DemoAbsence::username))
                .forEach((username, userAbsences) -> assertThat(userAbsences.stream()
                        .flatMap(DemoDataLoaderTest::days)
                        .toList())
                        .as("%s: the absent days of %s", description, username)
                        .doesNotHaveDuplicates());
    }

    private static Stream<LocalDate> days(DemoAbsence absence) {
        return absence.firstDay().datesUntil(absence.lastDay().plusDays(1));
    }

    private static long dayCount(DemoAbsence absence) {
        return days(absence).count();
    }

    private static boolean includes(DemoAbsence absence, LocalDate date) {
        return !date.isBefore(absence.firstDay()) && !date.isAfter(absence.lastDay());
    }

    private static boolean crossesTheFirstOfTheMonth(DemoAbsence absence, LocalDate today) {
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        return absence.firstDay().isBefore(firstOfMonth) && !absence.lastDay().isBefore(firstOfMonth);
    }

    // A weekend with a day of the absence before it and after it
    private static boolean crossesAWeekend(DemoAbsence absence) {
        return days(absence).anyMatch(day -> day.getDayOfWeek() == DayOfWeek.SATURDAY
                && day.isAfter(absence.firstDay()) && !day.plusDays(2).isAfter(absence.lastDay()));
    }

    private static boolean isWorkday(LocalDate date, Set<LocalDate> holidays) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.contains(date);
    }

    private static boolean isAcceptedImage(byte[] image) {
        return AvatarImageFormat.detect(ByteBuffer.wrap(image)).isPresent();
    }
}

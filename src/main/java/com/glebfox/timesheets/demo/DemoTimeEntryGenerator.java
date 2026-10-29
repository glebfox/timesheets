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

import com.glebfox.timesheets.app.WorkPlanSupport;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import org.jspecify.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Logs the demo time: everyone who works on a project fills the workdays of the weeks up to
 * the current date with entries on its tasks, mostly as their plan says. The older the entry,
 * the further its status has gone.
 */
class DemoTimeEntryGenerator {

    private static final int WEEKS = 8;
    // A closed project and an inactive task took time only in the first weeks
    private static final int WEEKS_BEFORE_CLOSING = 3;
    private static final int SLOT_MINUTES = 30;
    private static final int MAX_ENTRIES_PER_DAY = 3;
    private static final int REJECTED_COUNT = 4;
    private static final int BATCH_SIZE = 200;

    private static final Map<String, List<String>> DESCRIPTIONS_BY_TASK_TYPE = Map.of(
            "Research", List.of("Reviewed the latest readings", "Compared the results with the previous run",
                    "Read up on the earlier studies", "Wrote up the findings"),
            "Experiment", List.of("Ran the experiment series", "Prepared the samples",
                    "Recorded the measurements", "Repeated the failed run"),
            "Engineering", List.of("Assembled the prototype", "Replaced the worn parts",
                    "Calibrated the equipment", "Fixed the wiring"),
            "Testing", List.of("Walked through the test plan", "Logged the defects found",
                    "Repeated the test with new settings"),
            "Analysis", List.of("Analysed the data set", "Prepared the charts for the report", "Checked the numbers"),
            "Management", List.of("Planning meeting", "Status report", "Reviewed the schedule with the team"),
            "Security", List.of("Patrolled the sector", "Checked the access logs", "Escorted the visitors",
                    "Safety drill"));

    private static final List<String> REJECTION_REASONS = List.of(
            "Logged on the wrong task, please move it.",
            "The description is missing, please add what was done.",
            "More time than the lab log shows, please check.",
            "Looks like a duplicate of another entry on the same day.");

    private final DataManager dataManager;
    private final WorkPlanSupport workPlanSupport;
    // A fixed seed: loaded on the same date, the entries are the same
    private final Random random = new Random(1998);

    DemoTimeEntryGenerator(DataManager dataManager, WorkPlanSupport workPlanSupport) {
        this.dataManager = dataManager;
        this.workPlanSupport = workPlanSupport;
    }

    /**
     * Creates and saves the time entries of the weeks up to the given date.
     *
     * @param today the last day to log time on
     * @return the number of entries saved
     */
    int generate(LocalDate today) {
        LocalDate thisWeek = today.with(DayOfWeek.MONDAY);
        LocalDate start = thisWeek.minusWeeks(WEEKS);
        LocalDate closing = start.plusWeeks(WEEKS_BEFORE_CLOSING);

        List<TimeEntry> entries = new ArrayList<>();
        loadBookingsByUser().forEach((user, bookings) -> {
            WorkPlan plan = workPlanSupport.loadWorkPlan(user, start, today.plusDays(1));
            start.datesUntil(today.plusDays(1)).forEach(date -> {
                List<Booking> available = bookings.stream()
                        .filter(booking -> date.isBefore(closing) || booking.isBookable())
                        .toList();
                long minutes = loggedMinutes(plan.getPlannedTime(date), date.equals(today));
                if (!available.isEmpty() && minutes > 0) {
                    entries.addAll(logDay(user, date, available, minutes, thisWeek));
                }
            });
        });
        reject(entries, thisWeek.minusWeeks(1), thisWeek);

        Lists.partition(entries, BATCH_SIZE).forEach(batch ->
                dataManager.save(new SaveContext().saving(batch).setDiscardSaved(true)));
        return entries.size();
    }

    private Map<User, List<Booking>> loadBookingsByUser() {
        List<ProjectParticipant> participants = dataManager.load(ProjectParticipant.class)
                .all()
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("role", FetchPlan.BASE)
                        .add("project", projectBuilder -> projectBuilder.addFetchPlan(FetchPlan.BASE)
                                .add("tasks", taskBuilder -> taskBuilder.addFetchPlan(FetchPlan.BASE)
                                        .add("type", FetchPlan.BASE)
                                        .add("requiredTagGroups", FetchPlan.BASE))
                                .add("tagGroups", groupBuilder -> groupBuilder.addFetchPlan(FetchPlan.BASE)
                                        .add("tags", FetchPlan.BASE))))
                .list();

        // Sorted, so the random choices fall the same way on every load
        Map<User, List<Booking>> bookingsByUser = new LinkedHashMap<>();
        participants.stream()
                .filter(participant -> participant.getRole().getType() != ProjectRoleType.OBSERVER
                        && Boolean.TRUE.equals(participant.getUser().getActive()))
                .sorted(Comparator.comparing((ProjectParticipant participant) -> participant.getUser().getUsername())
                        .thenComparing(participant -> participant.getProject().getName()))
                .forEach(participant -> bookingsByUser
                        .computeIfAbsent(participant.getUser(), user -> new ArrayList<>())
                        .addAll(participant.getProject().getTasks().stream()
                                .sorted(Comparator.comparing(Task::getName))
                                .map(task -> new Booking(task, participant.getProject()))
                                .toList()));
        return bookingsByUser;
    }

    private long loggedMinutes(WorkTime plannedTime, boolean today) {
        long planned = plannedTime.toMinutes();
        if (planned == 0) {
            return 0;
        }
        if (today) {
            // The day is not over yet
            return roundToSlot(planned / 2);
        }
        int dice = random.nextInt(100);
        if (dice < 4) {
            return 0;
        } else if (dice < 14) {
            return Math.max(60, planned - 60L * (1 + random.nextInt(2)));
        } else if (dice < 20) {
            return planned + 60L * (1 + random.nextInt(2));
        }
        return planned;
    }

    private static long roundToSlot(long minutes) {
        return Math.max(SLOT_MINUTES, minutes / SLOT_MINUTES * SLOT_MINUTES);
    }

    private List<TimeEntry> logDay(User user, LocalDate date, List<Booking> bookings, long minutes,
                                   LocalDate thisWeek) {
        // Split the day's time into slots and spread them over one to three different tasks
        int slots = (int) (minutes / SLOT_MINUTES);
        int entryCount = Math.min(slots, 1 + random.nextInt(MAX_ENTRIES_PER_DAY));
        int[] slotsPerEntry = new int[entryCount];
        for (int slot = 0; slot < slots; slot++) {
            slotsPerEntry[slot < entryCount ? slot : random.nextInt(entryCount)]++;
        }

        List<Booking> shuffled = new ArrayList<>(bookings);
        Collections.shuffle(shuffled, random);
        List<TimeEntry> entries = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            Booking booking = shuffled.get(i % shuffled.size());
            TimeEntry entry = dataManager.create(TimeEntry.class);
            entry.setUser(user);
            entry.setTask(booking.task());
            entry.setDate(date);
            entry.setSpentTime(WorkTime.ofMinutes((long) slotsPerEntry[i] * SLOT_MINUTES));
            entry.setDescription(describe(booking.task()));
            entry.setTags(pickTags(booking));
            entry.setStatus(statusOf(date, thisWeek));
            entries.add(entry);
        }
        return entries;
    }

    private @Nullable String describe(Task task) {
        List<String> descriptions = DESCRIPTIONS_BY_TASK_TYPE.get(task.getType().getName());
        // Now and then an entry goes without a description
        if (descriptions == null || random.nextInt(100) < 20) {
            return null;
        }
        return descriptions.get(random.nextInt(descriptions.size()));
    }

    private List<Tag> pickTags(Booking booking) {
        Task task = booking.task();
        List<Tag> tags = new ArrayList<>();
        booking.project().getTagGroups().stream()
                .sorted(Comparator.comparing(TagGroup::getName))
                .forEach(group -> {
                    boolean required = task.getRequiredTagGroups() != null
                            && task.getRequiredTagGroups().contains(group);
                    if ((required || random.nextInt(100) < 50) && !group.getTags().isEmpty()) {
                        List<Tag> groupTags = group.getTags().stream()
                                .sorted(Comparator.comparing(Tag::getName))
                                .toList();
                        tags.add(groupTags.get(random.nextInt(groupTags.size())));
                    }
                });
        return tags;
    }

    private static TimeEntryStatus statusOf(LocalDate date, LocalDate thisWeek) {
        if (date.isBefore(thisWeek.minusWeeks(4))) {
            return TimeEntryStatus.CLOSED;
        } else if (date.isBefore(thisWeek.minusWeeks(1))) {
            return TimeEntryStatus.APPROVED;
        }
        return TimeEntryStatus.NEW;
    }

    private void reject(List<TimeEntry> entries, LocalDate weekStart, LocalDate weekEnd) {
        List<TimeEntry> lastWeek = entries.stream()
                .filter(entry -> !entry.getDate().isBefore(weekStart) && entry.getDate().isBefore(weekEnd))
                .toList();
        List<TimeEntry> rejected = new ArrayList<>();
        // The admin, who signs in first, sees a rejected entry of their own
        lastWeek.stream()
                .filter(entry -> entry.getUser().getUsername().equals("admin"))
                .findFirst()
                .ifPresent(rejected::add);
        while (rejected.size() < Math.min(REJECTED_COUNT, lastWeek.size())) {
            TimeEntry entry = lastWeek.get(random.nextInt(lastWeek.size()));
            if (!rejected.contains(entry)) {
                rejected.add(entry);
            }
        }
        for (int i = 0; i < rejected.size(); i++) {
            rejected.get(i).setStatus(TimeEntryStatus.REJECTED);
            rejected.get(i).setRejectionReason(REJECTION_REASONS.get(i % REJECTION_REASONS.size()));
        }
    }

    /**
     * A task that a user can log time on, with the project the user takes part in.
     */
    private record Booking(Task task, Project project) {

        boolean isBookable() {
            return task.getStatus() == TaskStatus.ACTIVE && project.getStatus() == ProjectStatus.OPEN;
        }
    }
}

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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.app.util.WeekSummary;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Messages;
import io.jmix.core.common.util.Preconditions;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.kit.action.ActionVariant;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility class that provides functionalities for managing calendar-related data,
 * such as time entries, summaries, and handling date manipulations.
 */
@Component("ts_CalendarSupport")
public class CalendarSupport {

    private final TimeEntrySupport timeEntrySupport;
    private final DataManager dataManager;
    private final Messages messages;
    private final Dialogs dialogs;

    public CalendarSupport(TimeEntrySupport timeEntrySupport,
                           DataManager dataManager,
                           Messages messages,
                           Dialogs dialogs) {
        this.timeEntrySupport = timeEntrySupport;
        this.dataManager = dataManager;
        this.messages = messages;
        this.dialogs = dialogs;
    }

    /**
     * Loads time entries for the currently authenticated user within the specified date range
     * and wraps them into {@link TimeEntryCalendarWrapper} objects.
     *
     * @param startDate the starting date of the range to load time entries (inclusive)
     * @param endDate   the ending date of the range to load time entries (exclusive)
     * @return a list of {@link TimeEntryCalendarWrapper} objects representing the loaded time entries
     */
    public List<TimeEntryCalendarWrapper> loadTimeEntries(LocalDate startDate, LocalDate endDate) {
        return dataManager.load(TimeEntry.class)
                .query("""
                        select e from ts_TimeEntry e
                        where
                            e.user.username = :current_user_username
                            and e.date >= ?1 and e.date < ?2
                        order by e.date desc
                        """, startDate, endDate)
                .fetchPlan(FetchPlan.BASE)
                .list().stream()
                .map(this::wrapTimeEntry)
                .toList();
    }

    /**
     * Sums the time spent in the specified time entries by day.
     *
     * @param timeEntries the time entries to sum
     * @return the time spent by day; a day without time entries has no key
     */
    public Map<LocalDate, WorkTime> calculateSpentTimeByDay(Collection<TimeEntryCalendarWrapper> timeEntries) {
        Preconditions.checkNotNullArgument(timeEntries);

        return timeEntries.stream()
                .filter(timeEntry -> timeEntry.getDate() != null && timeEntry.getSpentTime() != null)
                .collect(Collectors.toMap(
                        TimeEntryCalendarWrapper::getDate,
                        TimeEntryCalendarWrapper::getSpentTime,
                        WorkTime::plus
                ));
    }

    /**
     * Calculates the time spent and the time planned in each week of the specified month.
     * <p>
     * Weeks are the rows of a month calendar that starts its weeks on the specified day: the first week
     * contains the first day of the month, the last week contains the last day of the month. Only the days
     * of the month are counted, so the first and the last weeks may be shorter than seven days, and their
     * first and last days are limited to the month accordingly. The time planned for each day is taken
     * from the specified plan.
     *
     * @param month          the month to calculate the summary for
     * @param firstDayOfWeek the day the weeks start on
     * @param spentTimeByDay the time spent by day, as calculated by {@link #calculateSpentTimeByDay(Collection)};
     *                       days outside the month are ignored
     * @param plan           the plan to take the planned time from; it must cover every day of the month
     * @return the first and last day, and the time spent and planned, of each week of the month, in order
     * @throws IllegalArgumentException if the plan does not cover a day of the month
     */
    public List<WeekSummary> calculateWeeksSummary(YearMonth month, DayOfWeek firstDayOfWeek,
                                                   Map<LocalDate, WorkTime> spentTimeByDay, WorkPlan plan) {
        Preconditions.checkNotNullArgument(month);
        Preconditions.checkNotNullArgument(firstDayOfWeek);
        Preconditions.checkNotNullArgument(spentTimeByDay);
        Preconditions.checkNotNullArgument(plan);

        LocalDate firstDayOfMonth = month.atDay(1);
        LocalDate lastDayOfMonth = month.atEndOfMonth();

        List<WeekSummary> weeksSummary = new ArrayList<>();
        for (LocalDate weekStart = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
             !weekStart.isAfter(lastDayOfMonth);
             weekStart = weekStart.plusWeeks(1)) {
            LocalDate firstDay = weekStart.isBefore(firstDayOfMonth) ? firstDayOfMonth : weekStart;
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate lastDay = weekEnd.isAfter(lastDayOfMonth) ? lastDayOfMonth : weekEnd;

            FactAndPlan total = FactAndPlan.ZERO;
            for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
                total = total.plus(new FactAndPlan(
                        spentTimeByDay.getOrDefault(day, WorkTime.ZERO),
                        plan.getPlannedTime(day)));
            }

            weeksSummary.add(new WeekSummary(firstDay, lastDay, total));
        }

        return weeksSummary;
    }

    /**
     * Aggregates the weekly summaries of actual and planned work hours into a single
     * monthly summary.
     *
     * @param weeksSummary the weekly summaries of actual and planned work hours,
     *                     as calculated by {@link #calculateWeeksSummary(YearMonth, DayOfWeek, Map, WorkPlan)}
     * @return a {@link FactAndPlan} object containing the aggregated monthly summary
     * of actual and planned work hours
     */
    public FactAndPlan calculateMonthSummary(Collection<WeekSummary> weeksSummary) {
        Preconditions.checkNotNullArgument(weeksSummary);

        return weeksSummary.stream()
                .map(WeekSummary::total)
                .reduce(FactAndPlan.ZERO, FactAndPlan::plus);
    }

    /**
     * Creates a copy of the time entry encapsulated within the provided {@link TimeEntryCalendarWrapper},
     * persists the copied time entry, and returns a new {@link TimeEntryCalendarWrapper} containing the saved copy.
     *
     * @param timeEntryWrapper the {@link TimeEntryCalendarWrapper} containing the time entry to be copied
     * @return a new {@link TimeEntryCalendarWrapper} that wraps the copied and saved time entry
     */
    public TimeEntryCalendarWrapper copyTimeEntry(TimeEntryCalendarWrapper timeEntryWrapper) {
        TimeEntry copied = timeEntrySupport.copy(timeEntryWrapper.getTimeEntry());
        TimeEntry saved = dataManager.save(copied);

        return wrapTimeEntry(saved);
    }

    /**
     * Removes a time entry represented by a {@link TimeEntryCalendarWrapper}
     * and performs an action after successful removal.
     *
     * @param timeEntryWrapper   the {@link TimeEntryCalendarWrapper} containing the time entry to be removed
     * @param afterRemoveHandler a handler to execute additional logic after the time entry is removed
     */
    public void removeTimeEntry(TimeEntryCalendarWrapper timeEntryWrapper,
                                Consumer<TimeEntryCalendarWrapper> afterRemoveHandler) {
        dialogs.createOptionDialog()
                .withHeader(messages.getMessage("dialogs.Confirmation"))
                .withText(messages.getMessage("dialogs.Confirmation.Remove"))
                .withActions(new DialogAction(DialogAction.Type.YES)
                                .withHandler(__ -> {
                                    TimeEntry timeEntry = timeEntryWrapper.getTimeEntry();
                                    dataManager.remove(timeEntry);

                                    afterRemoveHandler.accept(timeEntryWrapper);
                                })
                                .withVariant(ActionVariant.PRIMARY),
                        new DialogAction(DialogAction.Type.NO)
                )
                .open();
    }

    /**
     * Saves the given {@link TimeEntryCalendarWrapper} by persisting its associated {@link TimeEntry},
     * updating the wrapper with the saved instance, and returning the updated wrapper.
     *
     * @param timeEntryWrapper the {@link TimeEntryCalendarWrapper} containing the time entry to be saved
     * @return the updated {@link TimeEntryCalendarWrapper} containing the persisted {@link TimeEntry}
     */
    public TimeEntryCalendarWrapper saveTimeEntry(TimeEntryCalendarWrapper timeEntryWrapper) {
        TimeEntry saved = dataManager.save(timeEntryWrapper.getTimeEntry());
        timeEntryWrapper.setTimeEntry(saved);

        return timeEntryWrapper;
    }

    /**
     * Wraps the given {@link TimeEntry} into a {@link TimeEntryCalendarWrapper}.
     *
     * @param timeEntry the time entry to be wrapped
     * @return a {@link TimeEntryCalendarWrapper} containing the provided time entry
     */
    public TimeEntryCalendarWrapper wrapTimeEntry(TimeEntry timeEntry) {
        TimeEntryCalendarWrapper wrapper = dataManager.create(TimeEntryCalendarWrapper.class);
        wrapper.setTimeEntry(timeEntry);
        return wrapper;
    }
}

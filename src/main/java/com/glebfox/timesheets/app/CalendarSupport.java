package com.glebfox.timesheets.app;

import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.datatype.SpentTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Messages;
import io.jmix.core.common.util.Preconditions;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.kit.action.ActionVariant;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Utility class that provides functionalities for managing calendar-related data,
 * such as time entries, summaries, and handling date manipulations.
 */
@Component("ts_CalendarSupport")
public class CalendarSupport {

    private final CurrentAuthentication currentAuthentication;
    private final TimeEntrySupport timeEntrySupport;
    private final DataManager dataManager;
    private final Messages messages;
    private final Dialogs dialogs;

    public CalendarSupport(CurrentAuthentication currentAuthentication,
                           TimeEntrySupport timeEntrySupport,
                           DataManager dataManager,
                           Messages messages,
                           Dialogs dialogs) {
        this.currentAuthentication = currentAuthentication;
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
     * Calculates the first day of the month for the specified date.
     *
     * @param date the date from which to determine the first day of the current month
     * @return the first day of the month
     */
    public LocalDate getFirstDayOfCurrentMonth(LocalDate date) {
        Preconditions.checkNotNullArgument(date);

        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Calculates the last day of the month for the specified date.
     *
     * @param date the date from which to determine the last day of the current month
     * @return the first day of the month
     */
    public LocalDate getLastDayOfCurrentMonth(LocalDate date) {
        Preconditions.checkNotNullArgument(date);

        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Calculates the weekly summary of actual and planned work hours for a specified user and date.
     * <p>
     * This method computes the weekly breakdown of work hours based on the given date
     * and user. It aggregates the actual and planned work hours into an array
     * of {@code FactAndPlan} objects, where each element represents a week within
     * the month of the specified date.
     *
     * @param date the date to calculate the weeks summary for
     * @param user the user for whom the summary is calculated
     * @return an array of {@link FactAndPlan} objects, where each element represents
     * the aggregated work hours for a week within the specified date's month
     */
    public FactAndPlan[] calculateWeeksSummary(LocalDate date, User user) {
        return calculateWeeksSummary(date, user, currentAuthentication.getLocale());
    }

    /**
     * Calculates the weekly summary of actual and planned work hours for a specified user, date, and locale.
     * <p>
     * This method calculates weekly summaries of work hours for the month of the given date, based on the
     * specified locale. It aggregates the actual and planned work hours into an array of {@link FactAndPlan}
     * objects, with each element in the array corresponding to a week within the month.
     *
     * @param date   the date to calculate the weeks summary for, determining the month and year
     * @param user   the user for whom the summary is calculated
     * @param locale the locale used to determine the first day of the week
     * @return an array of {@link FactAndPlan} objects, where each element represents the aggregated
     * work hours for a single week in the specified month
     */
    public FactAndPlan[] calculateWeeksSummary(LocalDate date, User user, Locale locale) {
        Preconditions.checkNotNullArgument(date);
        Preconditions.checkNotNullArgument(user);
        Preconditions.checkNotNullArgument(locale);

        LocalDate firstDayOfMonth = getFirstDayOfCurrentMonth(date);
        LocalDate lastDayOfMonth = getLastDayOfCurrentMonth(date);

        DayOfWeek firstDayOfWeekForLocale = WeekFields.of(locale).getFirstDayOfWeek();
        WeekFields weekFields = WeekFields.of(firstDayOfWeekForLocale, 1);
        int weeksNumber = lastDayOfMonth.get(weekFields.weekOfMonth());

        FactAndPlan[] weeksSummary = new FactAndPlan[weeksNumber];

        LocalDate start = firstDayOfMonth;
        for (int i = 0; i < weeksNumber; i++) {
            LocalDate firstDayOfWeek = start.with(weekFields.dayOfWeek(), 1);
            LocalDate lastDayOfWeek = start.with(weekFields.dayOfWeek(), 7);

            if (firstDayOfWeek.isBefore(firstDayOfMonth)) {
                firstDayOfWeek = firstDayOfMonth;
            }

            if (lastDayOfWeek.isAfter(lastDayOfMonth)) {
                lastDayOfWeek = lastDayOfMonth;
            }

            weeksSummary[i] = new FactAndPlan(
                    actualWorkHoursForPeriod(firstDayOfWeek, lastDayOfWeek, user),
                    plannedWorkHoursForPeriod(firstDayOfWeek, lastDayOfWeek, user)
            );

            start = start.plusWeeks(1);
        }

        return weeksSummary;
    }

    /**
     * Aggregates the weekly summaries of actual and planned work hours into a single
     * monthly summary.
     *
     * @param weeksSummary an array of {@link FactAndPlan} objects representing the
     *                     weekly summaries of actual and planned work hours
     * @return a {@link FactAndPlan} object containing the aggregated monthly summary
     * of actual and planned work hours
     */
    public FactAndPlan calculateMonthSummary(FactAndPlan[] weeksSummary) {
        FactAndPlan monthSummary = new FactAndPlan();
        for (FactAndPlan factAndPlan : weeksSummary) {
            monthSummary.plusFact(factAndPlan.getFact());
            monthSummary.plusPlan(factAndPlan.getPlan());
        }

        return monthSummary;
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
     * @param afterRemoveHandler a hadler to execute additional logic after the time entry is removed
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

    private SpentTime plannedWorkHoursForPeriod(LocalDate start, LocalDate end, User user) {
        SpentTime dailyPlan = SpentTime.ofHours(8); // TODO: gg, setting
        SpentTime totalWorkHours = SpentTime.ofHours(0);

        for (LocalDate current = start; !current.isAfter(end); current = current.plusDays(1)) {
            if (isWorkday(current)) { // TODO: gg, bean
                // TODO: gg, add methods
                totalWorkHours = SpentTime.fromHours(totalWorkHours.asHours().add(dailyPlan.asHours()));
            }
        }

        return totalWorkHours;
    }

    private SpentTime actualWorkHoursForPeriod(LocalDate start, LocalDate end, User user) {
        List<TimeEntry> timeEntries = timeEntrySupport.getTimeEntriesForPeriod(start, end, user);
        if (timeEntries.isEmpty()) {
            return SpentTime.ofHours(0);
        }

        SpentTime totalWorkHours = SpentTime.ofHours(0);
        for (TimeEntry timeEntry : timeEntries) {
            // TODO: gg, add methods
            totalWorkHours = SpentTime.fromHours(totalWorkHours.asHours().add(timeEntry.getSpentTime().asHours()));
        }

        return totalWorkHours;
    }

    private boolean isWorkday(LocalDate date) {
        // TODO: gg, settings
        return date.getDayOfWeek().getValue() <= 5;
    }
}

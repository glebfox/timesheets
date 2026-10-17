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

package com.glebfox.timesheets.view.calendar;


import com.glebfox.timesheets.app.CalendarSupport;
import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.app.util.WeekStatus;
import com.glebfox.timesheets.app.util.WeekSummary;
import com.glebfox.timesheets.datatype.SpentTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import io.jmix.core.*;
import io.jmix.core.accesscontext.InMemoryCrudEntityContext;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.component.SupportsTypedValue;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import io.jmix.fullcalendarflowui.component.FullCalendar;
import io.jmix.fullcalendarflowui.component.contextmenu.FullCalendarContextMenu;
import io.jmix.fullcalendarflowui.component.contextmenu.event.DayCell;
import io.jmix.fullcalendarflowui.component.data.CalendarEvent;
import io.jmix.fullcalendarflowui.component.data.EntityCalendarEvent;
import io.jmix.fullcalendarflowui.component.event.*;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Route(value = "calendar", layout = MainView.class)
@ViewController("ts_CalendarView")
@ViewDescriptor("calendar-view.xml")
public class CalendarView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(CalendarView.class);

    private static final String MATCHING_TIME_CLASS_NAME = "matching-time";
    private static final String EXCESS_TIME_CLASS_NAME = "excess-time";
    private static final String INSUFFICIENT_TIME_CLASS_NAME = "insufficient-time";

    private static final String START_DATE_PARAMETER = "startDate";
    private static final String END_DATE_PARAMETER = "endDate";

    @ViewComponent
    private FullCalendar calendar;
    @ViewComponent
    private H2 monthHeader;
    @ViewComponent
    private JmixButton todayBtn;
    @ViewComponent
    private FlexLayout weeksSummaryWrapper;
    @ViewComponent
    private Span monthLoggedValue;
    @ViewComponent
    private Span monthTargetValue;
    @ViewComponent
    private Span monthRemainingValue;
    @ViewComponent
    private Span monthRemainingLabel;
    @ViewComponent
    private Span monthPercentValue;
    @ViewComponent
    private ProgressBar monthPercentBar;

    @ViewComponent
    private CollectionContainer<TimeEntryCalendarWrapper> timeEntriesDc;
    @ViewComponent
    private CollectionLoader<TimeEntryCalendarWrapper> timeEntriesDl;

    @Autowired
    private Messages messages;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private Notifications notifications;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private CalendarSupport calendarSupport;

    @ViewComponent
    private MessageBundle messageBundle;

    private Span monthName;
    private Span monthYear;

    private YearMonth displayedMonth;
    private DayOfWeek firstDayOfWeek;
    private Map<LocalDate, SpentTime> spentTimeByDay = Collections.emptyMap();

    private UiEntityContext entityContext;
    private InMemoryCrudEntityContext crudEntityContext;

    @Subscribe
    private void onInit(final InitEvent event) {
        initMonthHeader();
        initCalendarContextMenu();
    }

    private void initMonthHeader() {
        monthName = new Span();

        monthYear = new Span();
        monthYear.setClassName("calendar-view-month-header-year");

        // The space keeps the heading's text "September 2026" for assistive technology;
        // it is not rendered as a flex item, so the visual gap stays the header's column gap
        monthHeader.add(monthName, new Text(" "), monthYear);
    }

    @Install(to = "timeEntriesDl", target = Target.DATA_LOADER)
    private List<TimeEntryCalendarWrapper> timeEntriesDlLoadDelegate(final LoadContext<TimeEntryCalendarWrapper> loadContext) {
        if (loadContext.getQuery() == null) {
            return Collections.emptyList();
        }

        Map<String, Object> parameters = loadContext.getQuery().getParameters();
        LocalDate startDate = (LocalDate) parameters.get(START_DATE_PARAMETER);
        LocalDate endDate = (LocalDate) parameters.get(END_DATE_PARAMETER);

        if (startDate != null && endDate != null) {
            return calendarSupport.loadTimeEntries(startDate, endDate);
        } else {
            return Collections.emptyList();
        }
    }

    private void initCalendarContextMenu() {
        FullCalendarContextMenu contextMenu = FullCalendarContextMenu.create(calendar);
        contextMenu.setContentMenuHandler(context -> {
            contextMenu.removeAll();

            CalendarEvent calendarEvent = context.getCalendarEvent();
            DayCell dayCell = context.getDayCell();

            if (calendarEvent != null) {
                contextMenu.addItem(messages.getMessage("actions.Copy"), __ ->
                        copyTimeEntry(calendarEvent));
                contextMenu.addItem(messages.getMessage("actions.Remove"), __ ->
                        removeTimeEntry(calendarEvent));

                return true;
            } else if (dayCell != null) {
                contextMenu.addItem(messages.getMessage("actions.Add"), __ ->
                        addTimeEntry(dayCell));

                return !dayCell.isDisabled();
            }

            return false;
        });
    }

    private void addTimeEntry(DayCell dayCell) {
        createTimeEntry(dayCell.getDate());
    }

    private void copyTimeEntry(CalendarEvent calendarEvent) {
        TimeEntryCalendarWrapper timeEntryWrapper = findTimeEntry(calendarEvent);
        if (timeEntryWrapper == null) {
            return;
        }

        TimeEntryCalendarWrapper copied = calendarSupport.copyTimeEntry(timeEntryWrapper);
        timeEntriesDc.getMutableItems().add(copied);

        showTimeEntryCopiedNotification();
    }

    private void removeTimeEntry(CalendarEvent calendarEvent) {
        TimeEntryCalendarWrapper timeEntryWrapper = findTimeEntry(calendarEvent);
        if (timeEntryWrapper == null) {
            return;
        }

        calendarSupport.removeTimeEntry(timeEntryWrapper, removed -> {
            timeEntriesDc.getMutableItems().remove(timeEntryWrapper);
            showTimeEntryRemovedNotification();
        });
    }

    @Subscribe(id = "prevBtn", subject = "clickListener")
    private void onPrevBtnClick(final ClickEvent<JmixButton> event) {
        calendar.navigateToPrevious();
    }

    @Subscribe(id = "todayBtn", subject = "clickListener")
    private void onTodayBtnClick(final ClickEvent<JmixButton> event) {
        calendar.navigateToToday();
    }

    @Subscribe(id = "nextBtn", subject = "clickListener")
    private void onNextBtnClick(final ClickEvent<JmixButton> event) {
        calendar.navigateToNext();
    }

    @Subscribe(id = "timeEntriesDc", target = Target.DATA_CONTAINER)
    private void onTimeEntriesDcCollectionChange(
            final CollectionContainer.CollectionChangeEvent<TimeEntryCalendarWrapper> event) {
        spentTimeByDay = calendarSupport.calculateSpentTimeByDay(event.getSource().getItems());

        updateHoursSummary();
        resetDayCellBottomTextClassNames();
    }

    @Subscribe("calendar")
    private void onCalendarDatesSet(final DatesSetEvent event) {
        DisplayModeInfo displayModeInfo = event.getDisplayModeInfo();
        displayedMonth = YearMonth.from(displayModeInfo.getCurrentStartDate());
        // The month grid starts with the first day of a week, which the calendar takes
        // from its own messages rather than from the locale
        firstDayOfWeek = displayModeInfo.getActiveStartDate().getDayOfWeek();

        updateMonthHeader();
        updateTodayButton();

        loadTimeEntries(event.getStartDate(), event.getEndDate());
    }

    private void updateHoursSummary() {
        if (displayedMonth == null || firstDayOfWeek == null) {
            return;
        }

        List<WeekSummary> weeksSummary =
                calendarSupport.calculateWeeksSummary(displayedMonth, firstDayOfWeek, spentTimeByDay);
        FactAndPlan monthSummary = calendarSupport.calculateMonthSummary(weeksSummary);

        updateWeeksSummary(weeksSummary);
        updateMonthSummary(monthSummary);
    }

    private void updateMonthSummary(FactAndPlan month) {
        monthLoggedValue.setText(formatTotal(month.fact()));
        monthTargetValue.setText(formatTotal(month.plan()));

        String difference = formatTotal(month.difference());
        if (month.isExcess()) {
            monthRemainingValue.setText(messageBundle.formatMessage("monthSummary.overValue.text", difference));
            monthRemainingLabel.setText(messageBundle.getMessage("monthSummary.overTarget.text"));
        } else {
            monthRemainingValue.setText(difference);
            monthRemainingLabel.setText(messageBundle.getMessage("monthSummary.remaining.text"));
        }
        monthRemainingValue.setClassName("calendar-view-month-summary-value-over", month.isExcess());

        int percentOfPlan = month.percentOfPlan();
        monthPercentValue.setText(messageBundle.formatMessage("monthSummary.percentValue.text", percentOfPlan));
        monthPercentBar.setValue(Math.min(percentOfPlan / 100.0, 1));
    }

    private void updateWeeksSummary(List<WeekSummary> weeks) {
        weeksSummaryWrapper.removeAll();

        LocalDate today = getToday();
        for (WeekSummary week : weeks) {
            weeksSummaryWrapper.add(createWeekSummary(week, today));
        }
    }

    private Div createWeekSummary(WeekSummary week, LocalDate today) {
        FactAndPlan total = week.total();
        WeekStatus status = week.getStatus(today);

        Span fact = new Span(formatTotal(total.fact()));
        fact.setClassName("calendar-view-week-summary-fact");

        Span plan = new Span(messageBundle.formatMessage("weekSummary.plan.text", formatTotal(total.plan())));
        plan.setClassName("calendar-view-week-summary-plan");

        Div totals = new Div(fact, plan);
        totals.setClassName("calendar-view-week-summary-totals");

        ProgressBar progress = new ProgressBar();
        progress.setValue(getProgress(total));
        progress.setClassName("calendar-view-week-summary-progress");

        Span note = new Span(getWeekStatusNote(status, total));
        note.setClassName("calendar-view-week-summary-note");

        Div weekSummary = new Div(totals, progress, note);
        weekSummary.addClassNames("calendar-view-week-summary", getWeekStatusClassName(status));
        if (week.isCurrent(today)) {
            weekSummary.addClassName("calendar-view-week-summary-current");
        }

        return weekSummary;
    }

    private double getProgress(FactAndPlan total) {
        long planMinutes = total.plan().toMinutes();
        if (planMinutes == 0) {
            return total.fact().toMinutes() > 0 ? 1 : 0;
        }

        return Math.min((double) total.fact().toMinutes() / planMinutes, 1);
    }

    private String getWeekStatusClassName(WeekStatus status) {
        return switch (status) {
            case OVER -> "calendar-view-week-summary-over";
            case ON_PLAN -> "calendar-view-week-summary-on-plan";
            case SHORT -> "calendar-view-week-summary-short";
            case IN_PROGRESS -> "calendar-view-week-summary-in-progress";
            case UPCOMING -> "calendar-view-week-summary-upcoming";
            case NO_WORKDAYS -> "calendar-view-week-summary-no-workdays";
        };
    }

    private String getWeekStatusNote(WeekStatus status, FactAndPlan total) {
        String difference = formatTotal(total.difference());

        return switch (status) {
            case OVER -> messageBundle.formatMessage("weekStatus.over.text", difference);
            case ON_PLAN -> messageBundle.getMessage("weekStatus.onPlan.text");
            case SHORT -> messageBundle.formatMessage("weekStatus.short.text", difference);
            case IN_PROGRESS -> messageBundle.formatMessage("weekStatus.inProgress.text", difference);
            case UPCOMING -> messageBundle.getMessage("weekStatus.upcoming.text");
            case NO_WORKDAYS -> messageBundle.getMessage("weekStatus.noWorkdays.text");
        };
    }

    private String formatTotal(SpentTime time) {
        return time.getMinutes() == 0
                ? messageBundle.formatMessage("totalHours.format", time.getHours())
                : metadataTools.format(time);
    }

    private String getTimeClassName(FactAndPlan factAndPlan) {
        if (factAndPlan.isInsufficient()) {
            return INSUFFICIENT_TIME_CLASS_NAME;
        } else if (factAndPlan.isExcess()) {
            return EXCESS_TIME_CLASS_NAME;
        } else {
            return MATCHING_TIME_CLASS_NAME;
        }
    }

    private void resetDayCellBottomTextClassNames() {
        // The calendar adds the generated class names to the bottom text, but never removes
        // the previous ones, so a day whose time changes would keep the old state's colors.
        // The bottom text is regenerated after the calendar receives the changed events,
        // which happens after this script runs.
        // TODO: gg, remove when https://github.com/jmix-framework/jmix/issues/5840 is fixed
        calendar.getElement().executeJs("""
                this.querySelectorAll('.jmix-day-cell-bottom-text')
                    .forEach(bottomText => bottomText.className = 'jmix-day-cell-bottom-text');
                """);
    }

    private void loadTimeEntries(LocalDate startDate, LocalDate endDate) {
        timeEntriesDl.setParameter(START_DATE_PARAMETER, startDate);
        timeEntriesDl.setParameter(END_DATE_PARAMETER, endDate);
        timeEntriesDl.load();
    }

    @Subscribe(id = "refreshBtn", subject = "clickListener")
    private void onRefreshBtnClick(final ClickEvent<JmixButton> event) {
        getViewData().loadAll();

        notifications.create(messageBundle.formatMessage("info.dataReloaded.text"))
                .withType(Notifications.Type.SUCCESS)
                .withPosition(Notification.Position.TOP_END)
                .show();
    }

    @Subscribe("calendar")
    private void onCalendarEventClick(final EventClickEvent event) {
        TimeEntryCalendarWrapper timeEntryToEdit = findTimeEntry(event.getCalendarEvent());
        if (timeEntryToEdit == null) {
            return;
        }

        dialogWindows.detail(this, TimeEntry.class)
                .withViewClass(TimeEntryDetailView.class)
                .editEntity(timeEntryToEdit.getTimeEntry())
                .withViewConfigurer(view -> {
                    // Without a reload, the edited entity keeps the version it had before the save,
                    // and a later remove or move of the entry fails as a concurrent change
                    view.setReloadSaved(true);
                    view.setOwnTimeEntry(true);
                })
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        TimeEntry timeEntry = closeEvent.getView().getEditedEntity();
                        timeEntryToEdit.setTimeEntry(timeEntry);
                        timeEntriesDc.replaceItem(timeEntryToEdit);
                    }
                })
                .open();
    }

    @Subscribe("calendar")
    private void onCalendarDayNavigationLinkClick(final DayNavigationLinkClickEvent event) {
        createTimeEntry(event.getDate());
    }

    private void createTimeEntry(LocalDate date) {
        dialogWindows.detail(this, TimeEntry.class)
                .withViewClass(TimeEntryDetailView.class)
                .newEntity()
                .withInitializer(timeEntry ->
                        timeEntry.setDate(date))
                .withViewConfigurer(view -> {
                    view.setReloadSaved(true);
                    view.setOwnTimeEntry(true);
                })
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        TimeEntry timeEntry = closeEvent.getView().getEditedEntity();
                        TimeEntryCalendarWrapper timeEntryWrapper = calendarSupport.wrapTimeEntry(timeEntry);
                        timeEntriesDc.getMutableItems().add(timeEntryWrapper);
                    }
                })
                .open();
    }

    @Subscribe("calendar")
    private void onCalendarEventDrop(final EventDropEvent event) {
        TimeEntryCalendarWrapper timeEntryWrapper = findTimeEntry(event.getCalendarEvent());
        if (timeEntryWrapper == null || !timeEntryWrapper.dateChanged()) {
            return;
        }

        if (editPermitted(timeEntryWrapper.getTimeEntry())) {
            timeEntryWrapper.applyDate();
            timeEntryWrapper = calendarSupport.saveTimeEntry(timeEntryWrapper);
            timeEntriesDc.replaceItem(timeEntryWrapper);

            showTimeEntrySavedNotification();
        } else {
            // EventDropEvent cannot revert the drop, so the calendar is given back the unchanged date.
            // TODO: gg, use revert when https://github.com/jmix-framework/jmix/issues/4696 is fixed
            timeEntryWrapper.discardDate();
            timeEntriesDc.replaceItem(timeEntryWrapper);

            showTimeEntryMovePreventedNotification(timeEntryWrapper.getStatus());
        }
    }

    private boolean editPermitted(TimeEntry timeEntry) {
        return getEntityContext().isEditPermitted()
                && (getCrudEntityContext().updatePredicate() == null
                || getCrudEntityContext().isUpdatePermitted(timeEntry));
    }

    @Install(to = "calendar", subject = "dayCellBottomTextGenerator")
    private String calendarDayCellBottomTextGenerator(final DayCellBottomTextContext context) {
        // Always return a text: the calendar keeps the previous text in place of a null one
        return spentTimeByDay.getOrDefault(context.getDate(), SpentTime.ZERO).toString();
    }

    @Nullable
    @Install(to = "calendar", subject = "dayCellBottomTextClassNamesGenerator")
    private List<String> calendarDayCellBottomTextClassNamesGenerator(final DayCellBottomTextClassNamesContext context) {
        LocalDate date = context.getDate();
        if (!calendarSupport.isWorkday(date)) {
            return null;
        }

        FactAndPlan daySummary = new FactAndPlan(
                spentTimeByDay.getOrDefault(date, SpentTime.ZERO),
                calendarSupport.getPlannedTime(date));

        return List.of(getTimeClassName(daySummary));
    }

    @Subscribe("jumpToDatePicker")
    private void onJumpToDatePickerTypedValueChange(
            final SupportsTypedValue.TypedValueChangeEvent<TypedDatePicker<LocalDate>, LocalDate> event) {
        if (event.getValue() == null) {
            return;
        }

        calendar.navigateToDate(event.getValue());
        event.getSource().clear();
    }

    private void showTimeEntrySavedNotification() {
        showTimeEntryNotification("info.TimeEntry.saved.text");
    }

    private void showTimeEntryCopiedNotification() {
        showTimeEntryNotification("info.TimeEntry.copied.text");
    }

    private void showTimeEntryRemovedNotification() {
        showTimeEntryNotification("info.TimeEntry.removed.text");
    }

    private void showTimeEntryNotification(String messageKey) {
        notifications.create(messageBundle.getMessage(messageKey))
                .withType(Notifications.Type.SUCCESS)
                .withPosition(Notification.Position.TOP_END)
                .show();
    }

    private void showTimeEntryMovePreventedNotification(TimeEntryStatus status) {
        notifications.create(messageBundle.getMessage("info.TimeEntry.movePrevented." + status.getId() + ".text"))
                .withType(Notifications.Type.WARNING)
                .withPosition(Notification.Position.TOP_END)
                .show();
    }

    private void updateMonthHeader() {
        monthName.setText(StringUtils.capitalize(displayedMonth.getMonth()
                .getDisplayName(TextStyle.FULL_STANDALONE, currentAuthentication.getLocale())));
        monthYear.setText(String.valueOf(displayedMonth.getYear()));
    }

    private void updateTodayButton() {
        todayBtn.setEnabled(!displayedMonth.equals(YearMonth.from(getToday())));
    }

    private LocalDate getToday() {
        return LocalDate.now();
    }

    @Nullable
    private TimeEntryCalendarWrapper findTimeEntry(CalendarEvent calendarEvent) {
        if (calendarEvent instanceof EntityCalendarEvent<?> entityCalendarEvent
                && entityCalendarEvent.getEntity() instanceof TimeEntryCalendarWrapper eventEntity) {
            Preconditions.checkState(eventEntity.getTimeEntry() != null, "'%s' has no link to %s"
                    .formatted(eventEntity, TimeEntry.class.getSimpleName()));

            return eventEntity;
        } else {
            log.warn("Clicked event doesn't wrap {}", TimeEntry.class.getSimpleName());
            return null;
        }
    }

    private UiEntityContext getEntityContext() {
        if (entityContext == null) {
            MetaClass metaClass = getApplicationContext().getBean(Metadata.class).getClass(TimeEntry.class);
            entityContext = new UiEntityContext(metaClass);
            accessManager.applyRegisteredConstraints(entityContext);
        }

        return entityContext;
    }

    private InMemoryCrudEntityContext getCrudEntityContext() {
        if (crudEntityContext == null) {
            MetaClass metaClass = getApplicationContext().getBean(Metadata.class).getClass(TimeEntry.class);
            crudEntityContext = new InMemoryCrudEntityContext(metaClass, getApplicationContext());
            accessManager.applyRegisteredConstraints(crudEntityContext);
        }

        return crudEntityContext;
    }
}
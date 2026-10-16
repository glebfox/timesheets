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
import com.glebfox.timesheets.datatype.SpentTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.*;
import io.jmix.core.accesscontext.InMemoryCrudEntityContext;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "calendar", layout = MainView.class)
@ViewController("ts_CalendarView")
@ViewDescriptor("calendar-view.xml")
public class CalendarView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(CalendarView.class);

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
    private Div monthSummary;
    @ViewComponent
    private FlexLayout weeksSummaryWrapper;

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
    private CurrentUserSubstitution currentUserSubstitution;
    @Autowired
    private CalendarSupport calendarSupport;

    @ViewComponent
    private MessageBundle messageBundle;

    private Map<LocalDate, SpentTime> spentTimeByDay = Collections.emptyMap();

    private UiEntityContext entityContext;
    private InMemoryCrudEntityContext crudEntityContext;

    @Subscribe
    private void onInit(final InitEvent event) {
        initCalendarContextMenu();
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

            if (context.getCalendarEvent() != null) {
                contextMenu.addItem(messages.getMessage("actions.Copy"), __ ->
                        copyTimeEntry(context.getCalendarEvent()));
                contextMenu.addItem(messages.getMessage("actions.Remove"), __ ->
                        removeTimeEntry(context.getCalendarEvent()));

                return true;
            } else if (context.getDayCell() != null) {
                contextMenu.addItem(messages.getMessage("actions.Add"), __ ->
                        addTimeEntry(context.getDayCell()));

                return !context.getDayCell().isDisabled();
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
        LocalDate firstDayOfCurrentMonth = calendarSupport.getFirstDayOfCurrentMonth(calendar.getDate());
        updateHoursSummary(firstDayOfCurrentMonth);

        calculateSpentTimeByDay(event.getSource().getItems());
    }

    private void calculateSpentTimeByDay(List<TimeEntryCalendarWrapper> items) {
        spentTimeByDay = items.stream()
                .collect(Collectors.groupingBy(
                        TimeEntryCalendarWrapper::getDate,
                        Collectors.mapping(
                                TimeEntryCalendarWrapper::getSpentTime,
                                Collectors.reducing(
                                        SpentTime.ZERO,
                                        (spentTime1, spentTime2) ->
                                                SpentTime.fromHours(spentTime1.asHours().add(spentTime2.asHours()))
                                )
                        )
                ));
    }

    @Subscribe("calendar")
    private void onCalendarDatesSet(final DatesSetEvent event) {
        LocalDate firstDayOfCurrentMonth = calendarSupport.getFirstDayOfCurrentMonth(calendar.getDate());
        updateMonthHeader(firstDayOfCurrentMonth);
        updateTodayButton(firstDayOfCurrentMonth);

        loadTimeEntries(event.getStartDate(), event.getEndDate());
    }

    private void updateHoursSummary(LocalDate firstDayOfMonth) {
        // TODO: gg, do not load entities again, take from container
        FactAndPlan[] weeksSummary = calendarSupport.calculateWeeksSummary(firstDayOfMonth,
                ((User) currentUserSubstitution.getEffectiveUser()));
        FactAndPlan monthSummary = calendarSupport.calculateMonthSummary(weeksSummary);

        updateWeeksSummary(weeksSummary);
        updateMonthSummary(monthSummary);
    }

    private void updateMonthSummary(FactAndPlan monthTotalHours) {
        if (monthTotalHours.isMatch()) {
            this.monthSummary.setText(messageBundle.formatMessage("monthTotalHours.text",
                    metadataTools.format(monthTotalHours.getFact())));
            this.monthSummary.removeClassNames(EXCESS_TIME_CLASS_NAME, INSUFFICIENT_TIME_CLASS_NAME);
        } else {
            this.monthSummary.setText(messageBundle.formatMessage("monthTotalHours.error.text",
                    metadataTools.format(monthTotalHours.getFact()),
                    metadataTools.format(monthTotalHours.getPlan())));
            this.monthSummary.addClassName(monthTotalHours.isInsufficient()
                    ? INSUFFICIENT_TIME_CLASS_NAME
                    : EXCESS_TIME_CLASS_NAME);
        }
    }

    private void updateWeeksSummary(FactAndPlan[] weekTotals) {
        weeksSummaryWrapper.removeAll();

        for (FactAndPlan weekSummary : weekTotals) {
            Span weekSummaryText = new Span();
            weekSummaryText.setClassName("calendar-view-week-summary");

            if (weekSummary.isMatch()) {
                weekSummaryText.setText(metadataTools.format(weekSummary.getFact()));
            } else {
                weekSummaryText.setText("%s (%s)".formatted(
                        metadataTools.format(weekSummary.getFact()),
                        metadataTools.format(weekSummary.getPlan())
                ));
                weekSummaryText.addClassName(weekSummary.isInsufficient()
                        ? INSUFFICIENT_TIME_CLASS_NAME
                        : EXCESS_TIME_CLASS_NAME);
            }

            weeksSummaryWrapper.add(weekSummaryText);
        }
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
                .withViewConfigurer(view ->
                        view.setOwnTimeEntry(true))
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
            // TODO: gg, WA for
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
        return spentTimeByDay.getOrDefault(context.getDate(), SpentTime.ZERO).toString();
    }

    @Install(to = "calendar", subject = "dayCellBottomTextClassNamesGenerator")
    private List<String> calendarDayCellBottomTextClassNamesGenerator(final DayCellBottomTextClassNamesContext context) {
        SpentTime spentTime = spentTimeByDay.getOrDefault(context.getDate(), SpentTime.ZERO);

        // TODO: gg, actual check
        boolean workDay = context.getDate().getDayOfWeek().getValue() <= 5;

        // TODO: gg, implement
        if (!workDay) {
            return null;
        } else if (spentTime.getHours() > 8) {
            return Collections.singletonList(EXCESS_TIME_CLASS_NAME);
        } else if (spentTime.getHours() < 8) {
            return Collections.singletonList(INSUFFICIENT_TIME_CLASS_NAME);
        } else {
            return Collections.singletonList("day-summary");
        }
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
                .withThemeVariant(NotificationVariant.LUMO_WARNING)
                .withPosition(Notification.Position.TOP_END)
                .show();
    }

    private void updateMonthHeader(LocalDate firstDayOfMonth) {
        monthHeader.setText(String.format("%s %d",
                StringUtils.capitalize(firstDayOfMonth.getMonth()
                        .getDisplayName(TextStyle.FULL_STANDALONE, currentAuthentication.getLocale())),
                firstDayOfMonth.getYear()
        ));
    }

    private void updateTodayButton(LocalDate firstDayOfCurrentMonth) {
        todayBtn.setEnabled(!firstDayOfCurrentMonth.getMonth().equals(LocalDate.now().getMonth()));
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
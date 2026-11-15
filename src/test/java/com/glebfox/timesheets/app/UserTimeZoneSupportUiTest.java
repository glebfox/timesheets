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

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.glebfox.timesheets.view.user.UserDetailView;
import com.glebfox.timesheets.view.workschedule.WorkScheduleDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.TimeSource;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.fullcalendarflowui.component.FullCalendar;
import io.jmix.fullcalendarflowui.component.event.DatesSetEvent;
import io.jmix.fullcalendarflowui.component.event.DisplayModeInfo;
import io.jmix.fullcalendarflowui.kit.component.model.CalendarDisplayModes;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * Every screen counts from today in the current user's time zone, not in the server's. The clock is pinned half an
 * hour into December 1 in the user's zone, UTC+14, when it is still November 30 in nearly every other zone, the
 * server's included, so a screen that counted from the server's today would show November 30 or November.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class UserTimeZoneSupportUiTest {

    static final ZoneId USER_ZONE = ZoneId.of("Pacific/Kiritimati");
    // Tuesday, December 1, 2026, at 00:30 in the user's zone, which is November 30, 10:30 in UTC
    static final ZonedDateTime NOW = ZonedDateTime.of(2026, 12, 1, 0, 30, 0, 0, USER_ZONE);
    static final LocalDate TODAY = NOW.toLocalDate();

    @MockitoSpyBean
    TimeSource timeSource;
    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    UserTimeZoneSupport userTimeZoneSupport;

    ApprovalTestData data;
    String prefix;
    User user;

    @BeforeEach
    void setUp() {
        // The server's clock tells the pinned moment in the server's own zone
        doReturn(NOW.withZoneSameInstant(ZoneId.systemDefault())).when(timeSource).now();

        prefix = "user-today-" + UUID.randomUUID().toString().substring(0, 8);
        data = new ApprovalTestData(dataManager, prefix);
        user = data.save(dataManager.create(User.class), created -> {
            created.setUsername(prefix + "-user");
            created.setTimeZoneId(USER_ZONE.getId());
        });
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(FullAccessRole.CODE);
            assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        });
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    @Test
    void nowAndTodayAreInTheUsersTimeZoneTest() {
        asUser(() -> {
            assertThat(userTimeZoneSupport.now()).isEqualTo(NOW);
            assertThat(userTimeZoneSupport.today()).isEqualTo(TODAY);
        });
    }

    @Test
    void newTimeEntryIsDatedTheUsersTodayTest() {
        asUser(() -> {
            viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                    .newEntity()
                    .open();
            TimeEntryDetailView editor = UiTestUtils.getCurrentView();

            assertThat(editor.getEditedEntity().getDate()).isEqualTo(TODAY);
        });
    }

    @Test
    void calendarTodayIsTheUsersTodayTest() {
        asUser(() -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), CalendarView.class).open();
            CalendarView calendarView = UiTestUtils.getCurrentView();
            JmixButton todayBtn = UiTestUtils.getComponent(calendarView, "todayBtn");

            // The user's month is the current one, so Today has nowhere to move
            showMonth(calendarView, YearMonth.from(TODAY));
            assertThat(todayBtn.isEnabled()).isFalse();

            // The server's month is a past one for the user
            showMonth(calendarView, YearMonth.from(TODAY).minusMonths(1));
            assertThat(todayBtn.isEnabled()).isTrue();
        });
    }

    @Test
    void approvalOpensOnTheUsersMonthTest() {
        asUser(() -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), TimeEntryApprovalView.class).open();
            TimeEntryApprovalView view = UiTestUtils.getCurrentView();
            JmixButton defaultPeriodBtn = UiTestUtils.getComponent(view, "defaultPeriodBtn");

            assertThat(view.getPeriod()).isEqualTo(ApprovalPeriod.monthOf(YearMonth.from(TODAY)));
            assertThat(defaultPeriodBtn.isEnabled()).isFalse();
        });
    }

    @Test
    void userEditorMarksTheAssignmentInForceOnTheUsersTodayTest() {
        User edited = data.saveUser("edited", null, null);
        WorkSchedule schedule = saveSchedule();
        // In force on the server's today, on the user's today, and from the day after it
        saveAssignment(edited, schedule, TODAY.minusMonths(1));
        saveAssignment(edited, schedule, TODAY);
        saveAssignment(edited, schedule, TODAY.plusDays(1));

        asUser(() -> {
            viewBuilders.detail(UiTestUtils.getCurrentView(), User.class).editEntity(edited).open();
            UserDetailView editor = UiTestUtils.getCurrentView();

            assertThat(currentStartDates(UiTestUtils.getComponent(editor, "assignmentsDataGrid"), "workSchedule"))
                    .containsExactly(TODAY);
        });
    }

    @Test
    void scheduleEditorMarksTheAssignmentsInForceOnTheUsersTodayTest() {
        WorkSchedule schedule = saveSchedule();
        // In force from the user's today, and from the day after it
        saveAssignment(data.saveUser("started", null, null), schedule, TODAY);
        saveAssignment(data.saveUser("upcoming", null, null), schedule, TODAY.plusDays(1));

        asUser(() -> {
            viewBuilders.detail(UiTestUtils.getCurrentView(), WorkSchedule.class).editEntity(schedule).open();
            WorkScheduleDetailView editor = UiTestUtils.getCurrentView();

            assertThat(currentStartDates(UiTestUtils.getComponent(editor, "assignmentsDataGrid"), "startDate"))
                    .containsExactly(TODAY);
        });
    }

    private void asUser(Runnable action) {
        systemAuthenticator.runWithUser(user.getUsername(), action);
    }

    // The browser reports the dates of a month: six weeks from the Sunday on or before its first day
    private static void showMonth(CalendarView calendarView, YearMonth month) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        LocalDate firstDay = month.atDay(1);
        LocalDate gridStart = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate gridEnd = gridStart.plusWeeks(6);
        ComponentUtil.fireEvent(calendar,
                new DatesSetEvent(calendar, true, gridStart, gridEnd,
                        new DisplayModeInfo(gridStart, gridEnd, firstDay, firstDay.plusMonths(1),
                                CalendarDisplayModes.DAY_GRID_MONTH)));
    }

    // The start dates of the listed assignments that the column marks with the Current badge
    private static List<LocalDate> currentStartDates(DataGrid<WorkScheduleAssignment> grid, String badgeColumn) {
        ComponentRenderer<?, WorkScheduleAssignment> renderer = (ComponentRenderer<?, WorkScheduleAssignment>)
                Objects.requireNonNull(grid.getColumnByKey(badgeColumn)).getRenderer();
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(assignment -> {
                    Component cell = renderer.createComponent(assignment);
                    return cell.getChildren()
                            .anyMatch(child -> child instanceof Badge badge && "Current".equals(badge.getText()));
                })
                .map(WorkScheduleAssignment::getStartDate)
                .toList();
    }

    private WorkSchedule saveSchedule() {
        return data.save(dataManager.create(WorkSchedule.class), schedule -> {
            schedule.setName(prefix + "-schedule");
            schedule.setWorkdayTime(WorkTime.ofHours(8));
        });
    }

    private void saveAssignment(User assigned, WorkSchedule schedule, LocalDate startDate) {
        data.save(dataManager.create(WorkScheduleAssignment.class), assignment -> {
            assignment.setUser(assigned);
            assignment.setWorkSchedule(schedule);
            assignment.setStartDate(startDate);
        });
    }
}

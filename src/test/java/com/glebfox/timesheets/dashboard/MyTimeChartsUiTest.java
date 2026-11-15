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

package com.glebfox.timesheets.dashboard;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.app.util.WeekStatus;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.security.UiMinimalRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.dashboard.DashboardBlock;
import com.glebfox.timesheets.view.dashboard.DashboardColors;
import com.glebfox.timesheets.view.dashboard.DashboardFragment;
import com.glebfox.timesheets.view.dashboard.RecentWeeksBlock;
import com.glebfox.timesheets.view.dashboard.ThisWeekBlock;
import com.glebfox.timesheets.view.dashboard.TimeByProjectBlock;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import io.jmix.chartsflowui.kit.component.event.ChartClickEvent;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.UI;
import com.glebfox.timesheets.view.dashboard.DashboardCharts;
import io.jmix.chartsflowui.kit.component.JmixChart;
import tools.jackson.databind.JsonNode;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.shared.Color;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.view.View;
import io.jmix.fullcalendarflowui.component.FullCalendar;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The main view is named: by default, the extension looks it up once per class in a view registry that the previous
// class's teardown has emptied, and then opens its own blank view instead
@UiTest(initialView = MainView.class)
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class MyTimeChartsUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    TimeSource timeSource;
    @Autowired
    CurrentAuthentication currentAuthentication;
    @Autowired
    ComponentSupport componentSupport;
    @Autowired
    Messages messages;

    ApprovalTestData data;
    LocalDate today;
    TaskType taskType;
    Project project;
    Task task;
    User user;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "my-time-charts-" + UUID.randomUUID().toString().substring(0, 8));
        taskType = data.saveTaskType();
        project = data.saveProject("project", data.saveClient());
        task = data.saveTask(project, taskType);
        user = data.saveUser("user", "Chell", null);
        assignRole(user, FullAccessRole.CODE);
        // The user's today, as the dashboard counts it: in the user's time zone, which may be a day off the JVM's
        today = systemAuthenticator.withUser(user.getUsername(),
                () -> DashboardBlock.today(timeSource, currentAuthentication));
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    @Test
    void thisWeekShowsSevenDaysTest() {
        AbsenceType vacation = data.save(dataManager.create(AbsenceType.class),
                type -> type.setName(user.getUsername() + " <vacation>"));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()));
            // Monday and Wednesday fall within the week whether it starts on Sunday or on Monday
            LocalDate monday = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            LocalDate wednesday = monday.plusDays(2);
            data.saveEntryOf(task, user, monday, TimeEntryStatus.NEW, WorkTime.of(6, 30));
            data.save(dataManager.create(Absence.class), absence -> {
                absence.setUser(user);
                absence.setType(vacation);
                absence.setFirstDay(wednesday);
                absence.setLastDay(wednesday);
            });

            ThisWeekBlock block = block("thisWeekBlock");
            dashboard().refresh();

            List<MapDataItem> items = items(chart(block));
            assertThat(items).hasSize(7);
            int mondayIndex = (int) (monday.toEpochDay() - weekStart.toEpochDay());
            MapDataItem mondayItem = items.get(mondayIndex);
            assertDayStatus(mondayItem, WeekStatus.SHORT, 6.5);
            assertThat(mondayItem.getValue(ThisWeekBlock.PLAN)).isEqualTo(8.0);
            assertThat((String) mondayItem.getValue(ThisWeekBlock.DAY)).doesNotContain(vacation.getName());

            MapDataItem wednesdayItem = items.get(mondayIndex + 2);
            for (WeekStatus status : ThisWeekBlock.DAY_STATUSES) {
                assertThat(wednesdayItem.getValue(ThisWeekBlock.spentField(status))).isEqualTo(0.0);
            }
            assertThat(wednesdayItem.getValue(ThisWeekBlock.PLAN)).isEqualTo(0.0);
            assertThat((String) wednesdayItem.getValue(ThisWeekBlock.DAY)).contains(vacation.getName());
            // The axis label is drawn as text; the tooltip is HTML, so the name is escaped there
            assertThat((String) wednesdayItem.getValue(ThisWeekBlock.TOOLTIP))
                    .contains(user.getUsername() + " &lt;vacation&gt;")
                    .doesNotContain(vacation.getName());
        });
    }

    @Test
    void thisWeekColorsDaysByStatusTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()));
            // Monday to Thursday fall within the week whether it starts on Sunday or on Monday
            LocalDate monday = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            data.saveEntryOf(task, user, monday, TimeEntryStatus.NEW, WorkTime.of(6, 30));
            data.saveEntryOf(task, user, monday.plusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(9));
            data.saveEntryOf(task, user, monday.plusDays(3), TimeEntryStatus.NEW, WorkTime.ofHours(8));

            ThisWeekBlock block = block("thisWeekBlock");
            dashboard().refresh();

            List<MapDataItem> items = items(chart(block));
            int mondayIndex = (int) (monday.toEpochDay() - weekStart.toEpochDay());
            // Each day's time in the field of its status, as the calendar colors the day: short, over, on plan
            assertDayStatus(items.get(mondayIndex), WeekStatus.SHORT, 6.5);
            assertDayStatus(items.get(mondayIndex + 1), WeekStatus.OVER, 9.0);
            assertDayStatus(items.get(mondayIndex + 3), WeekStatus.ON_PLAN, 8.0);
            for (WeekStatus status : ThisWeekBlock.DAY_STATUSES) {
                BarSeries series = chart(block).getSeries(ThisWeekBlock.spentField(status));
                assertThat(Objects.requireNonNull(series.getItemStyle()).getColor()).as(status.name())
                        .isEqualTo(DashboardColors.of(status));
                BarSeries onPlan = chart(block).getSeries(ThisWeekBlock.spentField(WeekStatus.ON_PLAN));
                // One stack for every status
                assertThat(series.getStack()).isNotNull().isEqualTo(onPlan.getStack());
            }

            // The colors themselves, as the app gives these statuses
            assertThat(DashboardColors.of(WeekStatus.ON_PLAN).getValue()).isEqualTo("#009B31");
            assertThat(DashboardColors.of(WeekStatus.SHORT).getValue()).isEqualTo("#DB373A");
            assertThat(DashboardColors.of(WeekStatus.OVER).getValue()).isEqualTo("#367ED4");

            // The tooltip says where the day stands, in the words of a week's status; a short day is short today
            // and ahead too, as its bar says
            assertThat((String) items.get(mondayIndex).getValue(ThisWeekBlock.TOOLTIP)).contains(
                    componentSupport.formatWeekStatusNote(WeekStatus.SHORT,
                            new FactAndPlan(WorkTime.of(6, 30), WorkTime.ofHours(8))));
            assertThat((String) items.get(mondayIndex + 1).getValue(ThisWeekBlock.TOOLTIP)).contains(
                    componentSupport.formatWeekStatusNote(WeekStatus.OVER,
                            new FactAndPlan(WorkTime.ofHours(9), WorkTime.ofHours(8))));
            assertThat((String) items.get(mondayIndex + 3).getValue(ThisWeekBlock.TOOLTIP)).contains(
                    componentSupport.formatWeekStatusNote(WeekStatus.ON_PLAN,
                            new FactAndPlan(WorkTime.ofHours(8), WorkTime.ofHours(8))));
        });
    }

    @Test
    void thisWeekDrawsAbsenceNamesLiterallyTest() {
        // A name with the axis labels' rich text markup in it
        AbsenceType leave = data.save(dataManager.create(AbsenceType.class),
                type -> type.setName(user.getUsername() + " {today|leave}"));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()));
            LocalDate wednesday = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
            data.save(dataManager.create(Absence.class), absence -> {
                absence.setUser(user);
                absence.setType(leave);
                absence.setFirstDay(wednesday);
                absence.setLastDay(wednesday);
            });

            ThisWeekBlock block = block("thisWeekBlock");
            dashboard().refresh();

            // Its braces are full-width ones, which ECharts does not read as markup
            String label = (String) items(chart(block)).get((int) (wednesday.toEpochDay() - weekStart.toEpochDay()))
                    .getValue(ThisWeekBlock.DAY);
            assertThat(label).contains(user.getUsername() + " \uFF5Btoday|leave\uFF5D");
            if (!wednesday.equals(today)) {
                assertThat(label).doesNotContain("{");
            }
        });
    }

    @Test
    void thisWeekDayWithoutPlanIsNeutralTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()));
            // Saturday falls within the week whether it starts on Sunday or on Monday, and plans nothing
            LocalDate saturday = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            data.saveEntryOf(task, user, saturday, TimeEntryStatus.NEW, WorkTime.ofHours(3));

            ThisWeekBlock block = block("thisWeekBlock");
            dashboard().refresh();

            MapDataItem item = items(chart(block)).get((int) (saturday.toEpochDay() - weekStart.toEpochDay()));
            assertThat(item.getValue(ThisWeekBlock.PLAN)).isEqualTo(0.0);
            // The neutral, as the calendar leaves such a day uncolored, and no status line in its tooltip: its day,
            // its time spent, and its plan
            assertDayStatus(item, WeekStatus.NO_WORKDAYS, 3.0);
            assertThat(((String) item.getValue(ThisWeekBlock.TOOLTIP)).split("<br>")).hasSize(3);
        });
    }

    private static void assertDayStatus(MapDataItem item, WeekStatus expected, double hours) {
        for (WeekStatus status : ThisWeekBlock.DAY_STATUSES) {
            assertThat(item.getValue(ThisWeekBlock.spentField(status))).as(status.name())
                    .isEqualTo(status == expected ? hours : 0.0);
        }
    }

    @Test
    void thisWeekKeepsDaysApartOnNarrowChartTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            ThisWeekBlock block = block("thisWeekBlock");
            dashboard().refresh();

            // The weekday over the day of the month, so the labels of neighboring days do not run together
            String label = (String) items(chart(block)).getFirst().getValue(ThisWeekBlock.DAY);
            assertThat(label).contains("\n");

            // ECharts media queries on the chart's width: the shared label width and bar gap by default, a narrower
            // label and a wider gap between the days when narrow
            JsonNode media = Objects.requireNonNull(chart(block).getNativeJson()).get("media");
            assertThat(media.size()).isEqualTo(2);
            assertThat(media.get(0).has("query")).isFalse();
            assertThat(media.get(0).at("/option/xAxis/0/axisLabel/width").asInt()).isEqualTo(72);
            assertThat(media.get(1).at("/query/maxWidth").asInt()).isEqualTo(599);
            assertThat(media.get(1).at("/option/xAxis/0/axisLabel/width").asInt()).isLessThan(72);
            JsonNode narrowSeries = media.get(1).at("/option/series");
            // The day off, the time spent in a series per status, and the plan
            assertThat(narrowSeries.size()).isEqualTo(2 + ThisWeekBlock.DAY_STATUSES.size());
            for (JsonNode series : narrowSeries) {
                assertThat(series.get("barCategoryGap").asString()).isEqualTo("40%");
            }
        });
    }

    @Test
    void thisWeekMarksTodayAndLeavesHeadroomTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            ThisWeekBlock block = block("thisWeekBlock");
            dashboard().refresh();

            // Today's label, a line at a time, in the rich text style that sets it apart; the other days' plain
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()));
            List<MapDataItem> items = items(chart(block));
            for (int i = 0; i < items.size(); i++) {
                String label = (String) items.get(i).getValue(ThisWeekBlock.DAY);
                if (weekStart.plusDays(i).equals(today)) {
                    assertThat(Objects.requireNonNull(label).split("\n")).allMatch(line -> line.startsWith("{today|") && line.endsWith("}"));
                } else {
                    assertThat(label).doesNotContain("{");
                }
            }
            AxisLabel dayLabel = Objects.requireNonNull(DashboardCharts.xAxis(chart(block)).getAxisLabel());
            assertThat(dayLabel.getRichStyles()).containsKey(ThisWeekBlock.TODAY_STYLE);
            assertThat(dayLabel.getRichStyles().get(ThisWeekBlock.TODAY_STYLE).getFontWeight()).isEqualTo("600");

            // The plan's 8 hours a day: the axis ends a step above, at 10 hours in steps of 2
            YAxis hours = DashboardCharts.yAxis(chart(block));
            assertThat(hours.getMax()).isEqualTo("10");
            assertThat(hours.getInterval()).isEqualTo(2);
        });
    }

    @Test
    void hoursAxisEndsAtLeastHalfAStepAboveTheLargestValueTest() {
        assertThat(axisEnd(1)).isEqualTo("2 by 1");
        assertThat(axisEnd(8)).isEqualTo("10 by 2");
        assertThat(axisEnd(9)).isEqualTo("10 by 2");
        assertThat(axisEnd(12)).isEqualTo("15 by 5");
        assertThat(axisEnd(40)).isEqualTo("50 by 10");
        assertThat(axisEnd(41)).isEqualTo("50 by 10");
        // A fraction of an hour, and a value a hair below a step: at least half a step stays above it
        assertThat(axisEnd(0.25)).isEqualTo("1 by 1");
        assertThat(axisEnd(9.98)).isEqualTo("12 by 2");
        assertThat(axisEnd(51)).isEqualTo("80 by 20");
        // Nothing above zero: an hour, set explicitly, so an end from earlier data does not stay
        assertThat(axisEnd(0)).isEqualTo("1 by 1");
    }

    private static String axisEnd(double largest) {
        YAxis axis = new YAxis();
        DashboardCharts.fitHoursAxis(axis, largest);
        return axis.getMax() + " by " + axis.getInterval();
    }

    @Test
    void recentWeeksLegendKeepsStatusOrderTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            LocalDate previousWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()))
                    .minusWeeks(1);
            LocalDate previousMonday = previousWeekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            // A short week before an over week: the legend still lists Over first, as WeekStatus orders them
            data.saveEntryOf(task, user, previousMonday.minusWeeks(2), TimeEntryStatus.NEW, WorkTime.ofHours(4));
            for (int day = 0; day < 5; day++) {
                data.saveEntryOf(task, user, previousMonday.plusDays(day), TimeEntryStatus.NEW, WorkTime.ofHours(9));
            }

            RecentWeeksBlock block = block("recentWeeksBlock");
            dashboard().refresh();

            assertThat(legend(block)).containsExactly(
                    "bar:" + messages.getMessage(RecentWeeksBlock.class, "recentWeeksBlock.legend.OVER"),
                    "bar:" + messages.getMessage(RecentWeeksBlock.class, "recentWeeksBlock.legend.SHORT"),
                    "plan:" + messages.getMessage(RecentWeeksBlock.class, "chart.legend.plan"));
            // Each mark in its series' color
            assertThat(legendColors(block)).containsExactly(DashboardColors.of(WeekStatus.OVER).getValue(),
                    DashboardColors.of(WeekStatus.SHORT).getValue(), DashboardColors.PLAN.getValue());
        });
    }

    @Test
    void recentWeeksColorsByStatusTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            LocalDate previousWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()))
                    .minusWeeks(1);
            LocalDate previousMonday = previousWeekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            data.saveEntryOf(task, user, previousMonday, TimeEntryStatus.NEW, WorkTime.ofHours(4));

            RecentWeeksBlock block = block("recentWeeksBlock");
            dashboard().refresh();

            Chart chart = chart(block);
            List<MapDataItem> items = items(chart);
            assertThat(items).hasSize(8);

            // The previous week is over and short: its hours are in the Short column alone
            MapDataItem previousWeek = items.get(6);
            for (WeekStatus status : WeekStatus.values()) {
                assertThat(previousWeek.getValue(status.name()))
                        .as(status.name())
                        .isEqualTo(status == WeekStatus.SHORT ? 4.0 : 0.0);
            }
            assertThat(previousWeek.getValue(RecentWeeksBlock.PLAN)).isEqualTo(40.0);

            BarSeries shortSeries = chart.getSeries(WeekStatus.SHORT.name());
            assertThat(shortSeries.getItemStyle())
                    .extracting(style -> style.getColor())
                    .isEqualTo(DashboardColors.of(WeekStatus.SHORT));
            assertThat(shortSeries.getStack()).isNotNull();

            // The legend names only the statuses the weeks show, and the plan
            assertThat(legend(block)).containsExactly(
                    "bar:" + messages.getMessage(RecentWeeksBlock.class, "recentWeeksBlock.legend.SHORT"),
                    "plan:" + messages.getMessage(RecentWeeksBlock.class, "chart.legend.plan"));

            // A week's plan of 40 hours: the axis ends a step above, at 50 hours in steps of 10
            YAxis hours = DashboardCharts.yAxis(chart);
            assertThat(hours.getMax()).isEqualTo("50");
            assertThat(hours.getInterval()).isEqualTo(10);
        });
    }

    @Test
    void timeByProjectGroupsOtherTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            List<Project> projects = new java.util.ArrayList<>();
            for (int hours = 7; hours >= 1; hours--) {
                Project hoursProject = data.saveProject("project of " + hours + "h", project.getClient());
                Task hoursTask = data.saveTask(hoursProject, taskType);
                data.saveEntryOf(hoursTask, user, today, TimeEntryStatus.NEW, WorkTime.ofHours(hours));
                projects.add(hoursProject);
            }

            TimeByProjectBlock block = block("timeByProjectBlock");
            dashboard().refresh();

            List<MapDataItem> items = items(chart(block));
            assertThat(items).hasSize(6);
            assertThat(items.subList(0, 5)).extracting(item -> item.getValue(TimeByProjectBlock.PROJECT))
                    .containsExactlyElementsOf(projects.subList(0, 5).stream().map(Project::getName).toList());
            assertThat(items.subList(0, 5)).extracting(item -> item.getValue(TimeByProjectBlock.HOURS))
                    .containsExactly(7.0, 6.0, 5.0, 4.0, 3.0);

            MapDataItem other = items.getLast();
            assertThat(other.getValue(TimeByProjectBlock.PROJECT))
                    .isNotIn(projects.stream().map(Project::getName).toList());
            assertThat(other.getValue(TimeByProjectBlock.HOURS)).isEqualTo(3.0);

            assertThat(isShown(block, chart(block))).isTrue();
            assertThat(isShown(block, FragmentUtils.getComponent(block, "emptyState"))).isFalse();

            // A slice's color is its slot's by size, Other's the neutral, and the list's marks repeat them
            List<Color> expected = List.of(DashboardColors.ofProject(0), DashboardColors.ofProject(1),
                    DashboardColors.ofProject(2), DashboardColors.ofProject(3), DashboardColors.ofProject(4),
                    DashboardColors.OTHER_PROJECT_SLICE);
            assertThat(chart(block).getColorPalette()).containsExactlyElementsOf(expected);
            assertThat(FragmentUtils.getComponent(block, "projectList").getChildren()
                    .map(row -> row.getChildren().findFirst().orElseThrow().getElement().getStyle()
                            .get("background-color"))
                    .toList())
                    .containsExactlyElementsOf(expected.stream().map(Color::getValue).toList());
        });
    }

    @Test
    void timeByProjectTooltipStandsUnderTheDonutTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            TimeByProjectBlock block = block("timeByProjectBlock");
            dashboard().refresh();

            // Its top left corner at the chart's bottom left one, whatever the donut's size, and a gap below it
            Tooltip tooltip = Objects.requireNonNull(chart(block).getTooltip());
            assertThat(Objects.requireNonNull(tooltip.getPosition()).getCoordinates()).containsExactly("0", "100%");
            assertThat(tooltip.getExtraCssText()).contains("margin-top");
            assertThat(tooltip.getConfine()).isNotEqualTo(Boolean.TRUE);
        });
    }

    @Test
    void timeByProjectEmptyStateTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            TimeByProjectBlock block = block("timeByProjectBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            assertThat(isShown(block, FragmentUtils.getComponent(block, "emptyState"))).isTrue();
            assertThat(isShown(block, chart(block))).isFalse();
        });
    }

    @Test
    void openDayOpensCalendarOnItsMonthTest() {
        LocalDate date = today.minusMonths(1);

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            ThisWeekBlock block = block("thisWeekBlock");

            block.openDay(date);

            View<?> opened = UiTestUtils.getCurrentView();
            assertThat(opened).isInstanceOf(CalendarView.class);
            FullCalendar calendar = UiTestUtils.getComponent(opened, "calendar");
            assertThat(calendar.getInitialDate()).isEqualTo(date);
        });
    }

    @Test
    void clickOnDayOpensCalendarOnItTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            ThisWeekBlock block = block("thisWeekBlock");
            dashboard().refresh();
            LocalDate thirdDay = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek())).plusDays(2);

            // As the browser reports a click on the third bar
            Chart chart = chart(block);
            ObjectNode detail = JsonNodeFactory.instance.objectNode().put("dataIndex", 2);
            ComponentUtil.fireEvent(chart, new ChartClickEvent(chart, true, detail, null));

            View<?> opened = UiTestUtils.getCurrentView();
            assertThat(opened).isInstanceOf(CalendarView.class);
            FullCalendar calendar = UiTestUtils.getComponent(opened, "calendar");
            assertThat(calendar.getInitialDate()).isEqualTo(thirdDay);
        });
    }

    @Test
    void openWeekOpensCalendarOnItsMonthTest() {
        LocalDate firstDay = today.minusWeeks(5).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentWeeksBlock block = block("recentWeeksBlock");

            block.openWeek(firstDay);

            View<?> opened = UiTestUtils.getCurrentView();
            assertThat(opened).isInstanceOf(CalendarView.class);
            FullCalendar calendar = UiTestUtils.getComponent(opened, "calendar");
            assertThat(calendar.getInitialDate()).isEqualTo(firstDay);
        });
    }

    @Test
    void openDayWithoutCalendarAccessDoesNothingTest() {
        User minimal = data.saveUser("minimal", "Minimal", "Access");
        assignRole(minimal, UiMinimalRole.CODE);
        MainView mainView = mainView();

        systemAuthenticator.runWithUser(minimal.getUsername(), () -> {
            ThisWeekBlock block = block("thisWeekBlock");

            block.openDay(today);

            assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(mainView);
        });
    }

    @Test
    void chartsReachTheBrowserTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            data.saveEntryOf(task, user, today, TimeEntryStatus.NEW, WorkTime.ofHours(3));
            dashboard().refresh();

            for (String blockId : List.of("thisWeekBlock", "recentWeeksBlock")) {
                Map<String, JsonNode> calls = browserCalls(chart(block(blockId)));
                JsonNode options = calls.get("_updateChart").get("options");
                assertThat(options.get("backgroundColor").asString()).isEqualTo("transparent");
                assertThat(options.get("tooltip").get("formatterFunction").asString()).contains("data.tooltip");
                JsonNode series = options.get("series");
                JsonNode planSeries = series.get(series.size() - 1);
                assertThat(planSeries.get("barGap").asString()).isEqualTo("-100%");
                assertThat(planSeries.get("itemStyle").get("borderType").asString()).isEqualTo("dashed");

                JsonNode rows = calls.get("_updateChartDataset").get("dataset").get("source");
                assertThat(rows.size()).isEqualTo(blockId.equals("thisWeekBlock") ? 7 : 8);
                assertThat(rows.get(0).has(DashboardCharts.TOOLTIP)).isTrue();
            }

            Map<String, JsonNode> calls = browserCalls(chart(block("timeByProjectBlock")));
            assertThat(calls.get("_updateChart").get("options").get("backgroundColor").asString())
                    .isEqualTo("transparent");
            JsonNode rows = calls.get("_updateChartDataset").get("dataset").get("source");
            assertThat(rows.get(0).get(TimeByProjectBlock.PROJECT).asString()).isEqualTo(project.getName());
            assertThat(rows.get(0).get(TimeByProjectBlock.HOURS).asDouble()).isEqualTo(3.0);
        });
    }

    private DayOfWeek firstDayOfWeek() {
        return componentSupport.getDaysOfWeek().getFirst();
    }

    private MainView mainView() {
        View<?> view = UiTestUtils.getCurrentView();
        assertThat(view).isInstanceOf(MainView.class);
        return (MainView) view;
    }

    private DashboardFragment dashboard() {
        return UiTestUtils.getComponent(mainView(), "dashboard");
    }

    @SuppressWarnings("unchecked")
    private <T extends Fragment<?>> T block(String id) {
        return (T) FragmentUtils.getComponent(dashboard(), id);
    }

    private static Chart chart(Fragment<?> block) {
        return (Chart) FragmentUtils.getComponent(block, "chart");
    }

    /**
     * Returns the colors of the marks of the block's legend, in their order.
     */
    static List<String> legendColors(Fragment<?> block) {
        return FragmentUtils.getComponent(block, "legend").getChildren()
                .map(item -> item.getChildren().findFirst().orElseThrow().getElement().getStyle()
                        .get("--dashboard-mark-color"))
                .toList();
    }

    /**
     * Returns the items of the block's legend, each as its mark's kind and its text, such as {@code plan:Plan}.
     */
    static List<String> legend(Fragment<?> block) {
        Component legend = FragmentUtils.getComponent(block, "legend");
        if (!legend.isVisible()) {
            return List.of();
        }
        return legend.getChildren()
                .map(item -> {
                    List<Component> parts = item.getChildren().toList();
                    String mark = parts.get(0).getElement().getClassList().stream()
                            .filter(className -> !className.equals("mark"))
                            .findFirst()
                            .orElse("");
                    return mark + ":" + parts.get(1).getElement().getText();
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<MapDataItem> items(Chart chart) {
        DataSet dataSet = chart.getDataSet();
        assertThat(dataSet).isNotNull();
        DataSet.Source<?> source = dataSet.getSource();
        assertThat(source).isNotNull();
        return ((ListChartItems<MapDataItem>) source.getDataProvider()).getItems();
    }

    /**
     * Returns the calls to the chart's browser side, the argument of the last call of each function by the
     * function's name. A chart keeps its calls until its browser side reports it is ready, which never happens in a
     * UI test, so the test reports it as the browser would, through the chart's client-callable method.
     */
    private static Map<String, JsonNode> browserCalls(JmixChart chart) {
        UIInternals internals = UI.getCurrent().getInternals();
        // The chart serializes its options and data only before a response is sent
        internals.getStateTree().runExecutionsBeforeClientResponse();
        try {
            Method ready = JmixChart.class.getDeclaredMethod("ready");
            ready.setAccessible(true);
            ready.invoke(chart);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        // An element's calls reach the response only before it is sent, too
        internals.getStateTree().runExecutionsBeforeClientResponse();

        Map<String, JsonNode> calls = new HashMap<>();
        for (PendingJavaScriptInvocation invocation : internals.dumpPendingJavaScriptInvocations()) {
            if (invocation.getOwner() != chart.getElement().getNode()) {
                continue;
            }
            // Such as "return $0._updateChart($1)"
            Matcher function = Pattern.compile("\\$0\\.(\\w+)\\(").matcher(invocation.getInvocation().getExpression());
            if (function.find()) {
                invocation.getInvocation().getParameters().stream()
                        .filter(JsonNode.class::isInstance)
                        .map(JsonNode.class::cast)
                        .findFirst()
                        .ifPresent(argument -> calls.put(function.group(1), argument));
            }
        }
        return calls;
    }

    /**
     * Whether the component and every parent of it up to the block are visible.
     */
    private static boolean isShown(Component block, Component component) {
        Optional<Component> current = Optional.of(component);
        while (current.isPresent()) {
            if (!current.get().isVisible()) {
                return false;
            }
            if (current.get() == block) {
                return true;
            }
            current = current.get().getParent();
        }
        return true;
    }

    private void assignRole(User roleUser, String roleCode) {
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(roleUser.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        });
    }
}

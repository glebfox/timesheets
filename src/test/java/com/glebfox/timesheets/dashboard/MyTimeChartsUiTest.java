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
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
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
            assertThat(mondayItem.getValue(ThisWeekBlock.SPENT)).isEqualTo(6.5);
            assertThat(mondayItem.getValue(ThisWeekBlock.PLAN)).isEqualTo(8.0);
            assertThat((String) mondayItem.getValue(ThisWeekBlock.DAY)).doesNotContain(vacation.getName());

            MapDataItem wednesdayItem = items.get(mondayIndex + 2);
            assertThat(wednesdayItem.getValue(ThisWeekBlock.SPENT)).isEqualTo(0.0);
            assertThat(wednesdayItem.getValue(ThisWeekBlock.PLAN)).isEqualTo(0.0);
            assertThat((String) wednesdayItem.getValue(ThisWeekBlock.DAY)).contains(vacation.getName());
            // The axis label is drawn as text; the tooltip is HTML, so the name is escaped there
            assertThat((String) wednesdayItem.getValue(ThisWeekBlock.TOOLTIP))
                    .contains(user.getUsername() + " &lt;vacation&gt;")
                    .doesNotContain(vacation.getName());
        });
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
            assertThat(narrowSeries.size()).isEqualTo(3);
            for (JsonNode series : narrowSeries) {
                assertThat(series.get("barCategoryGap").asString()).isEqualTo("40%");
            }
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

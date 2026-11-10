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
import com.glebfox.timesheets.app.TeamDashboardSupport;
import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PersonWeek;
import com.glebfox.timesheets.app.util.WaitingApproval;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.security.TimeEntryApprovalRole;
import com.glebfox.timesheets.security.UiMinimalRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.approval.ApprovalRosterScope;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import com.glebfox.timesheets.view.dashboard.DashboardBlock;
import com.glebfox.timesheets.view.dashboard.DashboardColors;
import com.glebfox.timesheets.view.dashboard.DashboardFragment;
import com.glebfox.timesheets.view.dashboard.TeamWeekBlock;
import com.glebfox.timesheets.view.dashboard.WaitingApprovalBlock;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.event.ChartClickEvent;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.series.AbstractSeries;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
import io.jmix.flowuidata.entity.UserSettingsItem;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fixture is project P, where {@code approver}, who holds UI: minimal access and Time entries: approval, is an
 * Approver. Each test adds the members of P it needs; their first names sort in the order the tests expect.
 */
// The main view is named: by default, the extension looks it up once per class in a view registry that the previous
// class's teardown has emptied, and then opens its own blank view instead
@UiTest(initialView = MainView.class)
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class MyTeamUiTest {

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
    TeamDashboardSupport teamDashboardSupport;

    ApprovalTestData data;
    LocalDate today;
    TaskType taskType;
    ProjectRole approverRole;
    ProjectRole memberRole;
    Project project;
    Task task;
    User approver;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "my-team-" + UUID.randomUUID().toString().substring(0, 8));
        taskType = data.saveTaskType();
        approverRole = data.saveRole(ProjectRoleType.APPROVER);
        memberRole = data.saveRole(ProjectRoleType.MEMBER);
        project = data.saveProject("P", data.saveClient());
        task = data.saveTask(project, taskType);

        approver = data.saveUser("approver", "Isaac", "Kleiner");
        assignRole(approver, UiMinimalRole.CODE);
        assignRole(approver, TimeEntryApprovalRole.CODE);
        data.saveParticipant(project, approver, approverRole);
        // The user's today, as the dashboard counts it: in the user's time zone, which may be a day off the JVM's
        today = systemAuthenticator.withUser(approver.getUsername(),
                () -> DashboardBlock.today(timeSource, currentAuthentication));
    }

    @AfterEach
    void tearDown() {
        // The approval screen may save the approver's settings; they go first
        dataManager.load(UserSettingsItem.class)
                .query("e.username = :username")
                .parameter("username", approver.getUsername())
                .list()
                .forEach(dataManager::remove);
        data.removeAll();
    }

    @Test
    void teamSectionShownForApproverTest() {
        saveMember("member", "Cid");

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            DashboardFragment dashboard = dashboard();
            dashboard.refresh();

            assertThat(component(dashboard, "myTeamBlocks").isVisible()).isTrue();
            assertThat(component(dashboard, "myTimeBlocks").isVisible()).isTrue();
            assertThat(component(dashboard, "myTimeHeading").isVisible()).isTrue();
            assertThat(component(dashboard, "myTeamHeading").isVisible()).isTrue();
        });
    }

    @Test
    void teamBlocksNameTheirPlaceOnTheGridTest() {
        // The stylesheet places the team's blocks by these classes, so every row of the grid is full
        DashboardFragment dashboard = dashboard();

        assertThat(component(dashboard, "myTeamBlocks").getElement().getClassList())
                .contains("dashboard-blocks", "dashboard-team-blocks");
        assertThat(cardClasses("waitingApprovalBlock")).contains("waiting-block");
        assertThat(cardClasses("teamWeekBlock")).contains("team-week-block").doesNotContain("wide");
        assertThat(cardClasses("teamAwayBlock")).contains("team-away-block");
    }

    @Test
    void teamSectionHiddenForNonApproverTest() {
        User member = saveMember("member", "Cid");
        assignRole(member, UiMinimalRole.CODE);
        assignRole(member, TimeEntryApprovalRole.CODE);

        systemAuthenticator.runWithUser(member.getUsername(), () -> assertTeamHidden(dashboard()));
    }

    @Test
    void teamSectionHiddenWithoutApprovalScreenTest() {
        User other = data.saveUser("other", "Other", "Approver");
        assignRole(other, UiMinimalRole.CODE);
        data.saveParticipant(project, other, approverRole);

        systemAuthenticator.runWithUser(other.getUsername(), () -> assertTeamHidden(dashboard()));
    }

    @Test
    void teamSectionHiddenForFullAccessApprovingNowhereTest() {
        User full = data.saveUser("full", "Full", "Access");
        assignRole(full, FullAccessRole.CODE);

        systemAuthenticator.runWithUser(full.getUsername(), () -> assertTeamHidden(dashboard()));
    }

    @Test
    void waitingListsTopFivePeopleTest() {
        for (String name : List.of("Ann", "Bob", "Cid", "Dan", "Eve", "Fay")) {
            User member = saveMember(name.toLowerCase(), name);
            data.saveEntry(task, member, today.minusDays(1), TimeEntryStatus.NEW, null);
        }

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            WaitingApprovalBlock block = block("waitingApprovalBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            assertThat(component(block, "count").getElement().getText()).isEqualTo("6");
            List<Component> rows = component(block, "rows").getChildren().toList();
            assertThat(rows.stream().filter(row -> row.hasClassName("dashboard-list-row")))
                    .hasSize(5)
                    .extracting(row -> row.getElement().getTextRecursively())
                    .first().asString().contains("Ann");
            assertThat(rows.getLast().hasClassName("dashboard-list-more")).isTrue();
            assertThat(rows.getLast().getElement().getText()).contains("1");
            assertThat(isShown(block, component(block, "emptyState"))).isFalse();
        });
    }

    @Test
    void waitingEmptyStateTest() {
        saveMember("member", "Cid");

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            WaitingApprovalBlock block = block("waitingApprovalBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            assertThat(isShown(block, component(block, "emptyState"))).isTrue();
            assertThat(isShown(block, component(block, "rows"))).isFalse();
            assertThat(isShown(block, component(block, "figure"))).isFalse();
        });
    }

    @Test
    void waitingPersonOpensApprovalOnOldestMonthTest() {
        // Another person waits in the same month and sorts first, so the screen's own choice would be them
        User first = saveMember("first", "Aaron");
        User person = saveMember("person", "Zed");
        LocalDate oldest = today.minusMonths(2);
        data.saveEntry(task, first, oldest, TimeEntryStatus.NEW, null);
        data.saveEntry(task, person, oldest, TimeEntryStatus.NEW, null);
        data.saveEntry(task, person, today, TimeEntryStatus.NEW, null);

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            WaitingApprovalBlock block = block("waitingApprovalBlock");
            WaitingApproval.Person waiting = teamDashboardSupport.loadWaiting().people().stream()
                    .filter(p -> p.user().getId().equals(person.getId()))
                    .findFirst().orElseThrow();

            block.openPerson(waiting);

            View<?> opened = UiTestUtils.getCurrentView();
            assertThat(opened).isInstanceOf(TimeEntryApprovalView.class);
            TimeEntryApprovalView view = (TimeEntryApprovalView) opened;
            assertThat(view.getPeriod()).isEqualTo(ApprovalPeriod.monthOf(YearMonth.from(oldest)));
            assertThat(UiTestUtils.<JmixRadioButtonGroup<ApprovalMode>>getComponent(view, "modeField").getValue())
                    .isEqualTo(ApprovalMode.MONTH);
            assertThat(UiTestUtils.<JmixRadioButtonGroup<ApprovalRosterScope>>getComponent(view,
                    "rosterScopeField").getValue()).isEqualTo(ApprovalRosterScope.NEEDS_REVIEW);
            assertThat(view.getSelectedPerson()).isNotNull()
                    .extracting(User::getId).isEqualTo(person.getId());
        });
    }

    @Test
    void waitingLinkOpensApprovalTest() {
        User member = saveMember("member", "Cid");
        data.saveEntry(task, member, today, TimeEntryStatus.NEW, null);

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            WaitingApprovalBlock block = block("waitingApprovalBlock");
            dashboard().refresh();

            JmixButton link = (JmixButton) component(block, "approvalLink");
            assertThat(link.isVisible()).isTrue();
            link.click();

            View<?> opened = UiTestUtils.getCurrentView();
            assertThat(opened).isInstanceOf(TimeEntryApprovalView.class);
            // Opened as from the menu: the default period, the current month
            assertThat(((TimeEntryApprovalView) opened).getPeriod())
                    .isEqualTo(ApprovalPeriod.monthOf(YearMonth.from(today)));
        });
    }

    @Test
    void teamWeekPersonOpensApprovalOnWeekTest() {
        User first = saveMember("first", "Aaron");
        User person = saveMember("person", "Zed");
        data.saveEntry(task, first, today, TimeEntryStatus.APPROVED, null);
        data.saveEntry(task, person, today, TimeEntryStatus.APPROVED, null);

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            TeamWeekBlock block = block("teamWeekBlock");
            PersonWeek week = loadTeamWeek().stream()
                    .filter(p -> p.user().getId().equals(person.getId()))
                    .findFirst().orElseThrow();

            block.openPerson(week);

            View<?> opened = UiTestUtils.getCurrentView();
            assertThat(opened).isInstanceOf(TimeEntryApprovalView.class);
            TimeEntryApprovalView view = (TimeEntryApprovalView) opened;
            assertThat(view.getPeriod()).isEqualTo(ApprovalPeriod.weekOf(today, firstDayOfWeek()));
            assertThat(UiTestUtils.<JmixRadioButtonGroup<ApprovalMode>>getComponent(view, "modeField").getValue())
                    .isEqualTo(ApprovalMode.WEEK);
            assertThat(UiTestUtils.<JmixRadioButtonGroup<ApprovalRosterScope>>getComponent(view,
                    "rosterScopeField").getValue()).isEqualTo(ApprovalRosterScope.EVERYONE);
            assertThat(view.getSelectedPerson()).isNotNull()
                    .extracting(User::getId).isEqualTo(person.getId());
        });
    }

    @Test
    void teamWeekPersonWithoutEntriesNotClickableTest() {
        User person = saveMember("person", "Cid");
        MainView mainView = mainView();

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            TeamWeekBlock block = block("teamWeekBlock");
            dashboard().refresh();
            PersonWeek week = loadTeamWeek().getFirst();
            assertThat(week.user().getId()).isEqualTo(person.getId());
            assertThat(week.hasEntries()).isFalse();

            block.openPerson(week);
            assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(mainView);

            // As the browser reports a click on the person's bar
            Chart chart = chart(block);
            ObjectNode detail = JsonNodeFactory.instance.objectNode().put("dataIndex", 0);
            ComponentUtil.fireEvent(chart, new ChartClickEvent(chart, true, detail, null));
            assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(mainView);
        });
    }

    @Test
    void teamWeekDataSplitsByStatusTest() {
        saveMember("first", "Aaron");
        User person = saveMember("person", "Zed");
        // A project where the approver approves nobody: the person's time there is a plain total
        Project other = data.saveProject("other", project.getClient());
        Task otherTask = data.saveTask(other, taskType);
        data.saveEntryOf(task, person, today, TimeEntryStatus.APPROVED, WorkTime.ofHours(2));
        data.saveEntryOf(task, person, today, TimeEntryStatus.NEW, WorkTime.ofHours(3));
        data.saveEntryOf(task, person, today, TimeEntryStatus.NEW, WorkTime.of(0, 30));
        data.saveEntryOf(task, person, today, TimeEntryStatus.REJECTED, WorkTime.ofHours(1));
        data.saveEntryOf(otherTask, person, today, TimeEntryStatus.NEW, WorkTime.ofHours(4));

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            TeamWeekBlock block = block("teamWeekBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            Chart chart = chart(block);
            List<MapDataItem> items = items(chart);
            // One bar per person, alphabetical
            assertThat(items).hasSize(2);
            assertThat((String) items.get(0).getValue(TeamWeekBlock.PERSON)).contains("Aaron");
            MapDataItem row = items.get(1);
            assertThat((String) row.getValue(TeamWeekBlock.PERSON)).contains("Zed");
            assertThat(row.getValue(TimeEntryStatus.APPROVED.name())).isEqualTo(2.0);
            assertThat(row.getValue(TimeEntryStatus.NEW.name())).isEqualTo(3.5);
            assertThat(row.getValue(TimeEntryStatus.REJECTED.name())).isEqualTo(1.0);
            assertThat(row.getValue(TimeEntryStatus.CLOSED.name())).isEqualTo(0.0);
            assertThat(row.getValue(TeamWeekBlock.OTHER)).isEqualTo(4.0);
            assertThat(row.getValue(TeamWeekBlock.PLAN)).isEqualTo(40.0);
            assertThat((String) row.getValue(TeamWeekBlock.TOOLTIP)).contains("Zed");

            // The segments are stacked, in the order of the statuses, then the other projects, each in its color
            List<String> stacked = Objects.requireNonNull(chart.getSeries()).stream()
                    .filter(series -> series instanceof BarSeries bar && bar.getStack() != null)
                    .map(AbstractSeries::getId)
                    .toList();
            assertThat(stacked).containsExactly(TimeEntryStatus.APPROVED.name(), TimeEntryStatus.NEW.name(),
                    TimeEntryStatus.REJECTED.name(), TimeEntryStatus.CLOSED.name(), TeamWeekBlock.OTHER);
            for (TimeEntryStatus status : TimeEntryStatus.values()) {
                BarSeries series = chart.getSeries(status.name());
                assertThat(Objects.requireNonNull(series.getItemStyle()).getColor()).as(status.name())
                        .isEqualTo(DashboardColors.of(status));
            }
        });
    }

    @Test
    void teamWeekNamesGoAboveBarsOnNarrowChartTest() {
        saveMember("person", "Cid");

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            TeamWeekBlock block = block("teamWeekBlock");
            dashboard().refresh();

            Chart chart = chart(block);
            // The first person on top
            assertThat(Objects.requireNonNull(chart.getYAxes()).getFirst().getInverse()).isTrue();
            // The chart grows taller with the team: the stylesheet sizes it by the number of people
            assertThat(chart.getStyle().get("--dashboard-team-size")).isEqualTo("1");

            // ECharts media queries on the chart's width: beside the bars by default, above them when narrow
            JsonNode media = Objects.requireNonNull(chart.getNativeJson()).get("media");
            assertThat(media.size()).isEqualTo(2);
            assertThat(media.get(0).has("query")).isFalse();
            assertThat(media.get(0).at("/option/yAxis/0/axisLabel/inside").asBoolean()).isFalse();
            assertThat(media.get(1).at("/query/maxWidth").asInt()).isEqualTo(599);
            assertThat(media.get(1).at("/option/yAxis/0/axisLabel/inside").asBoolean()).isTrue();
            assertThat(media.get(1).at("/option/yAxis/0/axisLabel/verticalAlign").asString()).isEqualTo("bottom");
        });
    }

    @Test
    void teamWeekWholeWeekAbsenceIsMarkedTest() {
        User person = saveMember("person", "Cid");
        AbsenceType vacation = saveAbsenceType("vacation");
        LocalDate weekStart = ApprovalPeriod.weekStartOf(today, firstDayOfWeek());
        saveAbsence(person, vacation, weekStart, weekStart.plusDays(6));

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            TeamWeekBlock block = block("teamWeekBlock");
            dashboard().refresh();

            MapDataItem row = items(chart(block)).getFirst();
            assertThat(row.getValue(TeamWeekBlock.PLAN)).isEqualTo(0.0);
            assertThat((Double) row.getValue(TeamWeekBlock.OFF)).isPositive();
            assertThat(row.getValue(TeamWeekBlock.OFF_LABEL)).isEqualTo(vacation.getName());
            assertThat((String) row.getValue(TeamWeekBlock.TOOLTIP)).contains(vacation.getName());
        });
    }

    @Test
    void teamAwayListsAbsencesTest() {
        User person = saveMember("person", "Cid");
        AbsenceType vacation = saveAbsenceType("vacation");
        saveAbsence(person, vacation, today, today);
        // Three weeks on: past this week and next
        AbsenceType sick = saveAbsenceType("sick");
        saveAbsence(person, sick, today.plusWeeks(3), today.plusWeeks(3));

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            Fragment<?> block = block("teamAwayBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            List<Component> rows = component(block, "rows").getChildren().toList();
            assertThat(rows).hasSize(1);
            String text = rows.getFirst().getElement().getTextRecursively();
            assertThat(text).contains("Cid").contains(vacation.getName()).doesNotContain(sick.getName());
            assertThat(isShown(block, component(block, "emptyState"))).isFalse();
        });
    }

    @Test
    void teamAwayEmptyStateTest() {
        saveMember("person", "Cid");

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            Fragment<?> block = block("teamAwayBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            assertThat(isShown(block, component(block, "emptyState"))).isTrue();
            assertThat(isShown(block, component(block, "rows"))).isFalse();
        });
    }

    private void assertTeamHidden(DashboardFragment dashboard) {
        dashboard.refresh();

        assertThat(component(dashboard, "myTeamBlocks").isVisible()).isFalse();
        assertThat(component(dashboard, "myTimeBlocks").isVisible()).isTrue();
        assertThat(component(dashboard, "myTimeHeading").isVisible()).isFalse();
        assertThat(component(dashboard, "myTeamHeading").isVisible()).isFalse();
    }

    private User saveMember(String name, String firstName) {
        User member = data.saveUser(name, firstName, "Zimmer");
        data.saveParticipant(project, member, memberRole);
        return member;
    }

    private AbsenceType saveAbsenceType(String name) {
        return data.save(dataManager.create(AbsenceType.class),
                type -> type.setName(approver.getUsername() + " " + name));
    }

    private void saveAbsence(User user, AbsenceType type, LocalDate firstDay, LocalDate lastDay) {
        data.save(dataManager.create(Absence.class), absence -> {
            absence.setUser(user);
            absence.setType(type);
            absence.setFirstDay(firstDay);
            absence.setLastDay(lastDay);
        });
    }

    private List<PersonWeek> loadTeamWeek() {
        LocalDate weekStart = ApprovalPeriod.weekStartOf(today, firstDayOfWeek());
        return teamDashboardSupport.loadTeamWeek(weekStart, weekStart.plusDays(6));
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

    private static Component component(Fragment<?> fragment, String id) {
        return FragmentUtils.getComponent(fragment, id);
    }

    private Set<String> cardClasses(String blockId) {
        Fragment<?> block = block(blockId);
        return block.getContent().getElement().getClassList();
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
     * Whether the component and every parent of it up to the block are visible.
     */
    private static boolean isShown(Component block, Component component) {
        Component current = component;
        while (current != null) {
            if (!current.isVisible()) {
                return false;
            }
            if (current == block) {
                return true;
            }
            current = current.getParent().orElse(null);
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

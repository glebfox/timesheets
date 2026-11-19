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
import com.glebfox.timesheets.app.DashboardSupport;
import com.glebfox.timesheets.app.UserTimeZoneSupport;
import com.glebfox.timesheets.app.util.Attention;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.security.TimeEntryApprovalRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.dashboard.ComingUpBlock;
import com.glebfox.timesheets.view.dashboard.DashboardFragment;
import com.glebfox.timesheets.view.dashboard.NeedsAttentionBlock;
import com.glebfox.timesheets.view.dashboard.RecentEntriesBlock;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// The main view is named: by default, the extension looks it up once per class in a view registry that the previous
// class's teardown has emptied, and then opens its own blank view instead
@UiTest(initialView = MainView.class)
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class MyTimeListsUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserTimeZoneSupport userTimeZoneSupport;
    @Autowired
    ComponentSupport componentSupport;
    @Autowired
    DashboardSupport dashboardSupport;

    ApprovalTestData data;
    String prefix;
    LocalDate today;
    Project project;
    Task task;
    User user;

    @BeforeEach
    void setUp() {
        prefix = "my-time-lists-" + UUID.randomUUID().toString().substring(0, 8);
        data = new ApprovalTestData(dataManager, prefix);
        project = data.saveProject("project", data.saveClient());
        task = data.saveTask(project, data.saveTaskType());
        user = data.saveUser("user", "Chell", null);
        assignRole(user, FullAccessRole.CODE);
        // The user's today, as the dashboard counts it: in the user's time zone, which may be a day off the JVM's
        today = systemAuthenticator.withUser(user.getUsername(), userTimeZoneSupport::today);
    }

    @AfterEach
    void tearDown() {
        // The entries saved through the editor refer to the test's users and tasks, so they go first
        List<TimeEntry> entries = dataManager.load(TimeEntry.class)
                .query("e.user.username like :prefix")
                .parameter("prefix", prefix + "-%")
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list();
        if (!entries.isEmpty()) {
            dataManager.save(new SaveContext()
                    .removing(entries.toArray())
                    .setHint(PersistenceHints.SOFT_DELETION, false));
        }
        data.removeAll();
    }

    @Test
    void attentionTruncatesToFiveRowsTest() {
        // Far outside the weeks the dashboard looks at, so it adds no time to any of them
        data.saveEntry(task, user, today.minusMonths(7), TimeEntryStatus.REJECTED, "no");

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            NeedsAttentionBlock block = block("needsAttentionBlock");
            dashboard().refresh();

            Attention attention = dashboardSupport.loadAttention(today, componentSupport.getDaysOfWeek().getFirst());
            assertThat(attention.rejectedCount()).isEqualTo(1);
            int expectedMore = 1 + attention.shortDays().size() + attention.shortWeeks().size() - 5;
            assertThat(expectedMore).isPositive();

            List<Component> rows = withClass(block, "dashboard-list-row");
            assertThat(rows).hasSize(5);
            // The rejected entries first, their number only in the menu's badge, a row that opens them
            assertThat(text(rows.getFirst())).isEqualTo("Rejected entries");
            assertThat(rows.getFirst()).isInstanceOf(JmixButton.class);
            Badge badge = (Badge) rows.getFirst().getChildren()
                    .filter(Badge.class::isInstance)
                    .findFirst()
                    .orElseThrow();
            assertThat(badge.getNumber()).isEqualTo(1);
            assertThat(badge.getThemeNames()).contains("error");
            // The Rejected status' icon before the name, the row colored as an error
            assertThat(rows.getFirst().getChildren().findFirst().orElseThrow().getElement().getAttribute("icon"))
                    .isEqualTo("gravity-ui:circle-exclamation");
            assertThat(rows.getFirst().getElement().getClassList()).contains("rejected");
            List<Component> more = withClass(block, "dashboard-list-more");
            assertThat(more).hasSize(1);
            assertThat(text(more.getFirst())).contains(String.valueOf(expectedMore));
            assertThat(isEmptyStateShown(block)).isFalse();

            ((JmixButton) rows.getFirst()).click();
            assertThat(UiTestUtils.<View<?>>getCurrentView()).isInstanceOf(MyTimeEntryListView.class);
        });
    }

    @Test
    void attentionAllCaughtUpTest() {
        AbsenceType leave = data.save(dataManager.create(AbsenceType.class), type -> type.setName("leave"));
        // Days of absence plan nothing, so no day or week of the last two months is short
        data.save(dataManager.create(Absence.class), absence -> {
            absence.setUser(user);
            absence.setType(leave);
            absence.setFirstDay(today.minusMonths(3));
            absence.setLastDay(today.plusMonths(1));
        });

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            NeedsAttentionBlock block = block("needsAttentionBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            assertThat(withClass(block, "dashboard-list-row")).isEmpty();
            assertThat(withClass(block, "dashboard-list-more")).isEmpty();
            assertThat(isEmptyStateShown(block)).isTrue();
        });
    }

    @Test
    void attentionRowOpensCalendarTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            NeedsAttentionBlock block = block("needsAttentionBlock");
            dashboard().refresh();

            List<Component> rows = withClass(block, "dashboard-list-row");
            assertThat(rows).isNotEmpty();
            ((JmixButton) rows.getFirst()).click();

            assertThat(UiTestUtils.<View<?>>getCurrentView())
                    .isInstanceOf(com.glebfox.timesheets.view.calendar.CalendarView.class);
        });
    }

    @Test
    void attentionLinkInHeaderOpensMyTimeEntriesTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            NeedsAttentionBlock block = block("needsAttentionBlock");
            dashboard().refresh();

            // A link in the card's header, beside the title
            JmixButton link = (JmixButton) FragmentUtils.getComponent(block, "myEntriesLink");
            assertThat(block.getContent().getHeaderSuffix()).isSameAs(link);
            assertThat(link.getText()).isEqualTo("My entries");
            assertThat(link.getElement().getClassList()).contains("link-button");
            assertThat(link.isVisible()).isTrue();

            link.click();
            View<?> opened = UiTestUtils.getCurrentView();
            assertThat(opened).isInstanceOf(MyTimeEntryListView.class);
        });
    }

    @Test
    void comingUpEmptyStateTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            ComingUpBlock block = block("comingUpBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            assertThat(withClass(block, "dashboard-list-row")).isEmpty();
            assertThat(isEmptyStateShown(block)).isTrue();
        });
    }

    @Test
    void comingUpListsNearestFirstTest() {
        // Saved out of order, and one ended yesterday, which does not come up
        for (int offset : List.of(30, 5, 90, 12, 60)) {
            String name = "leave in " + offset + " days";
            AbsenceType type = data.save(dataManager.create(AbsenceType.class), t -> t.setName(name));
            data.save(dataManager.create(Absence.class), absence -> {
                absence.setUser(user);
                absence.setType(type);
                absence.setFirstDay(today.plusDays(offset));
                absence.setLastDay(today.plusDays(offset));
            });
        }
        AbsenceType ended = data.save(dataManager.create(AbsenceType.class), t -> t.setName("ended leave"));
        data.save(dataManager.create(Absence.class), absence -> {
            absence.setUser(user);
            absence.setType(ended);
            absence.setFirstDay(today.minusDays(3));
            absence.setLastDay(today.minusDays(1));
        });

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            ComingUpBlock block = block("comingUpBlock");
            dashboard().refresh();

            // At most three absences, nearest first
            List<Component> rows = withClass(block, "dashboard-list-row");
            assertThat(rows).hasSize(3);
            assertThat(text(rows.get(0))).contains("leave in 5 days");
            assertThat(text(rows.get(1))).contains("leave in 12 days");
            assertThat(text(rows.get(2))).contains("leave in 30 days");
            assertThat(isEmptyStateShown(block)).isFalse();
        });
    }

    @Test
    void recentEntryOpensEditorTest() {
        TimeEntry older = data.saveEntryOf(task, user, today.minusDays(2), TimeEntryStatus.NEW, WorkTime.ofHours(1));
        TimeEntry latest = data.saveEntryOf(task, user, today.minusDays(1), TimeEntryStatus.APPROVED,
                WorkTime.ofHours(3));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            List<TimeEntry> rows = rows(block);
            assertThat(rows).extracting(TimeEntry::getId).containsExactly(latest.getId(), older.getId());
            assertThat(text(cell(block, "spent", rows.getFirst()))).isEqualTo("3h");
            assertThat(text(cell(block, "spent", rows.getLast()))).isEqualTo("1h");

            // The task is the link that opens the entry, on a click of the pointer
            Component name = cell(block, "task", rows.getFirst());
            JmixButton link = (JmixButton) name.getChildren().findFirst().orElseThrow();
            assertThat(link.getElement().getClassList()).contains("link-button");
            assertThat(link.getTabIndex()).isEqualTo(-1);
            clickWithCount(link, 1);

            View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
            assertThat(dialog).isInstanceOf(TimeEntryDetailView.class);
            TimeEntry edited = ((TimeEntryDetailView) dialog).getEditedEntity();
            assertThat(edited.getId()).isEqualTo(latest.getId());
            assertThat(edited.getId()).isNotEqualTo(older.getId());
            ((TimeEntryDetailView) dialog).closeWithDiscard();
        });
    }

    @Test
    void recentEntryOpensOnceOnlyFromLinkOrKeyboardTest() {
        data.saveEntryOf(task, user, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();
            Component name = cell(block, "task", rows(block).getFirst());
            JmixButton link = (JmixButton) name.getChildren().findFirst().orElseThrow();
            int dialogs = UiTestUtils.getOpenedViewDialogs().size();

            // A click of the pointer on the project or the cell's empty room does nothing
            clickWithCount(name, 1);
            assertThat(UiTestUtils.getOpenedViewDialogs()).hasSize(dialogs);

            // The click that reaches the link's cell from no pointer, as Space on the cell or Enter on the link, opens
            // the entry, once; a click from no pointer on the link itself leaves the opening to the cell
            clickWithCount(link, 0);
            assertThat(UiTestUtils.getOpenedViewDialogs()).hasSize(dialogs);
            clickWithCount(name, 0);
            assertThat(UiTestUtils.getOpenedViewDialogs()).hasSize(dialogs + 1);
            View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
            assertThat(dialog).isInstanceOf(TimeEntryDetailView.class);
            ((TimeEntryDetailView) Objects.requireNonNull(dialog)).closeWithDiscard();
        });
    }

    @Test
    void recentEntryNamesTaskBeforeProjectTest() {
        data.saveEntryOf(task, user, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            // The task first, so a narrow row that cuts the name keeps it, then the project, quieter, in one cell
            Component name = cell(block, "task", rows(block).getFirst());
            List<Component> parts = name.getChildren().toList();
            assertThat(parts).hasSize(3);
            assertThat(parts.getFirst().getElement().getClassList()).contains("task");
            assertThat(text(parts.getFirst())).isEqualTo(task.getName());
            assertThat(parts.get(1).getElement().getClassList()).contains("separator");
            assertThat(parts.getLast().getElement().getClassList()).contains("project");
            assertThat(text(parts.getLast())).isEqualTo(project.getName());
        });
    }

    @Test
    void recentEntriesListLatestFirstTest() {
        TimeEntry thisYear = data.saveEntryOf(task, user, today.withDayOfYear(1), TimeEntryStatus.NEW,
                WorkTime.ofHours(1));
        TimeEntry lastYear = data.saveEntryOf(task, user, today.minusYears(1), TimeEntryStatus.APPROVED,
                WorkTime.ofHours(2));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            // A date of another year shows its year; the date column takes the width of the widest date
            assertThat(rows(block)).extracting(TimeEntry::getId).containsExactly(thisYear.getId(), lastYear.getId());
            assertThat(text(cell(block, "date", rows(block).getLast())))
                    .contains(String.valueOf(lastYear.getDate().getYear()));
            assertThat(text(cell(block, "date", rows(block).getFirst())))
                    .doesNotContain(String.valueOf(thisYear.getDate().getYear()));
        });
    }

    @Test
    void copyToTodayIsAnIconButtonTest() {
        data.saveEntryOf(task, user, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            JmixButton copy = (JmixButton) cell(block, "copy", rows(block).getFirst());
            assertThat(copy.getElement().getClassList()).contains("row-copy");
            assertThat(copy.getText()).isEmpty();
            // Out of the Tab order: it is its cell's only element, so Space on the cell presses it
            assertThat(copy.getTabIndex()).isEqualTo(-1);
            assertThat(copy.getIcon()).isNotNull();
            assertThat(copy.getAriaLabel()).contains("Copy to today");
            assertThat(Tooltip.forComponent(copy).getText()).isEqualTo("Copy to today");
        });
    }

    @Test
    void copyToTodayOpensNewEntryDatedTodayTest() {
        TagGroup group = data.save(dataManager.create(TagGroup.class), g -> {
            g.setName("lists tag group");
            g.setProject(project);
        });
        Tag tag = data.save(dataManager.create(Tag.class), t -> {
            t.setName("lists tag");
            t.setTagGroup(group);
        });
        TimeEntry source = data.save(dataManager.create(TimeEntry.class), entry -> {
            entry.setTask(task);
            entry.setUser(user);
            entry.setDate(today.minusDays(4));
            entry.setSpentTime(WorkTime.of(2, 30));
            entry.setStatus(TimeEntryStatus.APPROVED);
            entry.setDescription("copied description");
            entry.setTags(List.of(tag));
        });

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            assertThat(rows(block)).hasSize(1);
            ((JmixButton) cell(block, "copy", rows(block).getFirst())).click();

            View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
            assertThat(dialog).isInstanceOf(TimeEntryDetailView.class);
            TimeEntryDetailView editor = (TimeEntryDetailView) dialog;
            TimeEntry copy = editor.getEditedEntity();
            assertThat(copy.getId()).isNotEqualTo(source.getId());
            assertThat(copy.getDate()).isEqualTo(today);
            assertThat(copy.getTask().getId()).isEqualTo(task.getId());
            assertThat(copy.getSpentTime()).isEqualTo(WorkTime.of(2, 30));
            assertThat(copy.getDescription()).isEqualTo("copied description");
            assertThat(copy.getTags()).extracting(Tag::getId).containsExactly(tag.getId());
            assertThat(copy.getStatus()).isEqualTo(TimeEntryStatus.NEW);
            editor.closeWithDiscard();
        });
    }

    @Test
    void savingCopyRefreshesDashboardTest() {
        data.saveEntryOf(task, user, today.minusDays(4), TimeEntryStatus.APPROVED, WorkTime.ofHours(2));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();
            assertThat(rows(block)).hasSize(1);

            ((JmixButton) cell(block, "copy", rows(block).getFirst())).click();
            View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
            UiTestUtils.<JmixButton>getComponent(dialog, "saveAndCloseBtn").click();

            assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
            assertThat(rows(block)).hasSize(2);
        });
    }

    @Test
    void listsHiddenWithoutCreatePermissionTest() {
        // May read time entries, and may neither create them nor open the editor or My time entries
        User approver = data.saveUser("approver", "Ap", "Prover");
        assignRole(approver, TimeEntryApprovalRole.CODE);
        data.saveEntryOf(task, approver, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            // My time is for a user who may log time: it hides, and its lists load nothing of the approver's
            assertThat(FragmentUtils.getComponent(dashboard(), "myTimeBlocks").isVisible()).isFalse();
            assertThat(rows(block)).isEmpty();
        });
    }

    @Test
    void savingEntryOpenedFromTaskRefreshesDashboardTest() {
        data.saveEntryOf(task, user, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();
            assertThat(text(cell(block, "spent", rows(block).getFirst()))).isEqualTo("1h");

            clickWithCount(cell(block, "task", rows(block).getFirst()), 0);
            TimeEntryDetailView editor = UiTestUtils.getLastOpenedViewDialog();
            Objects.requireNonNull(editor.getEditedEntity()).setSpentTime(WorkTime.ofHours(4));
            UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();

            assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(editor);
            assertThat(text(cell(block, "spent", rows(block).getFirst()))).isEqualTo("4h");
        });
    }

    @Test
    void freshUserListsShowEmptyStatesTest() {
        User fresh = data.saveUser("fresh", "Fresh", "User");
        assignRole(fresh, FullAccessRole.CODE);

        systemAuthenticator.runWithUser(fresh.getUsername(), () -> {
            NeedsAttentionBlock attention = block("needsAttentionBlock");
            ComingUpBlock comingUp = block("comingUpBlock");
            RecentEntriesBlock recent = block("recentEntriesBlock");
            dashboard().refresh();

            assertThat(attention.isVisible()).isTrue();
            assertThat(comingUp.isVisible()).isTrue();
            assertThat(recent.isVisible()).isTrue();
            assertThat(isEmptyStateShown(comingUp)).isTrue();
            assertThat(rows(recent)).isEmpty();
            assertThat(entriesGrid(recent).getClassNames()).contains("empty");
            assertThat(entriesGrid(recent).getEmptyStateComponent()).isInstanceOf(GridEmptyState.class);
        });
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

    @SuppressWarnings("unchecked")
    private static DataGrid<TimeEntry> entriesGrid(RecentEntriesBlock block) {
        return (DataGrid<TimeEntry>) FragmentUtils.getComponent(block, "entriesGrid");
    }

    private static List<TimeEntry> rows(RecentEntriesBlock block) {
        return List.copyOf(Objects.requireNonNull(entriesGrid(block).getItems()).getItems());
    }

    /**
     * Renders the cell of the column with the key for the row, as the grid renders it in the browser.
     */
    private static Component cell(RecentEntriesBlock block, String columnKey, TimeEntry row) {
        Renderer<TimeEntry> renderer = Objects.requireNonNull(entriesGrid(block).getColumnByKey(columnKey))
                .getRenderer();
        return ((ComponentRenderer<? extends Component, TimeEntry>) renderer).createComponent(row);
    }

    /**
     * Fires a click on the component as the browser sends it, with the number of clicks of the pointer, 0 for a click
     * from no pointer, such as the one the grid sends on Space.
     */
    private static <C extends Component> void clickWithCount(C component, int clickCount) {
        ComponentUtil.fireEvent(component,
                new ClickEvent<>(component, true, 0, 0, 0, 0, clickCount, 0, false, false, false, false));
    }

    private static Stream<Component> descendants(Component root) {
        return root.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }

    private static List<Component> withClass(Component root, String className) {
        return descendants(root).filter(component -> component.getElement().getClassList().contains(className))
                .toList();
    }

    private static String text(Component component) {
        return component.getElement().getTextRecursively();
    }

    /**
     * Whether the block's empty state and every parent of it up to the block are visible.
     */
    private static boolean isEmptyStateShown(Fragment<?> block) {
        Component current = FragmentUtils.getComponent(block, "emptyState");
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

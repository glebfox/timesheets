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
import com.glebfox.timesheets.app.util.Attention;
import com.glebfox.timesheets.app.util.ComponentSupport;
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
import com.glebfox.timesheets.view.dashboard.DashboardBlock;
import com.glebfox.timesheets.view.dashboard.DashboardFragment;
import com.glebfox.timesheets.view.dashboard.NeedsAttentionBlock;
import com.glebfox.timesheets.view.dashboard.RecentEntriesBlock;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.shared.Tooltip;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
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
    TimeSource timeSource;
    @Autowired
    CurrentAuthentication currentAuthentication;
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
        today = systemAuthenticator.withUser(user.getUsername(),
                () -> DashboardBlock.today(timeSource, currentAuthentication));
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
            assertThat(text(rows.getFirst())).contains("1 rejected entry");
            List<Component> more = withClass(block, "dashboard-list-more");
            assertThat(more).hasSize(1);
            assertThat(text(more.getFirst())).contains(String.valueOf(expectedMore));
            assertThat(isShown(block, "emptyState")).isFalse();
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
            assertThat(isShown(block, "emptyState")).isTrue();
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
    void comingUpEmptyStateTest() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            ComingUpBlock block = block("comingUpBlock");
            dashboard().refresh();

            assertThat(block.isVisible()).isTrue();
            assertThat(withClass(block, "dashboard-list-row")).isEmpty();
            assertThat(isShown(block, "emptyState")).isTrue();
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
            assertThat(isShown(block, "emptyState")).isFalse();
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

            List<Component> rows = withClass(block, "dashboard-list-row");
            assertThat(rows).hasSize(2);
            assertThat(text(rows.getFirst())).contains("3h");
            assertThat(text(rows.getLast())).contains("1h");

            JmixButton open = (JmixButton) withClass(rows.getFirst(), "row-open").getFirst();
            open.click();

            View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
            assertThat(dialog).isInstanceOf(TimeEntryDetailView.class);
            TimeEntry edited = ((TimeEntryDetailView) dialog).getEditedEntity();
            assertThat(edited.getId()).isEqualTo(latest.getId());
            assertThat(edited.getId()).isNotEqualTo(older.getId());
            ((TimeEntryDetailView) dialog).closeWithDiscard();
        });
    }

    @Test
    void recentEntryNamesTaskBeforeProjectTest() {
        data.saveEntryOf(task, user, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            // The task first, so a narrow row that cuts the name keeps it, then the project, quieter
            Component name = withClass(block, "name").getFirst();
            List<Component> parts = name.getChildren().toList();
            assertThat(parts).hasSize(2);
            assertThat(parts.getFirst().getElement().getClassList()).contains("task");
            assertThat(text(parts.getFirst())).isEqualTo(task.getName());
            assertThat(parts.getLast().getElement().getClassList()).contains("project");
            assertThat(text(parts.getLast())).isEqualTo(project.getName());
            assertThat(text(name)).isEqualTo(task.getName() + " " + project.getName());
        });
    }

    @Test
    void recentEntriesWidenDatesOfAnotherYearTogetherTest() {
        // The first day of the current year is in it whatever today is
        data.saveEntryOf(task, user, today.withDayOfYear(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            Component rows = FragmentUtils.getComponent(block, "rows");

            dashboard().refresh();
            assertThat(rows.getElement().getClassList()).doesNotContain("with-years");

            // A date of another year shows its year, so every row's date column widens with it
            data.saveEntryOf(task, user, today.minusYears(1), TimeEntryStatus.APPROVED, WorkTime.ofHours(2));
            dashboard().refresh();
            assertThat(withClass(block, "dashboard-list-row")).hasSize(2);
            assertThat(rows.getElement().getClassList()).contains("with-years");
        });
    }

    @Test
    void copyToTodayIsAnIconButtonTest() {
        data.saveEntryOf(task, user, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            JmixButton copy = (JmixButton) withClass(block, "row-copy").getFirst();
            assertThat(copy.getText()).isEmpty();
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

            List<Component> copies = withClass(block, "row-copy");
            assertThat(copies).hasSize(1);
            ((JmixButton) copies.getFirst()).click();

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
            assertThat(withClass(block, "dashboard-list-row")).hasSize(1);

            ((JmixButton) withClass(block, "row-copy").getFirst()).click();
            View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
            UiTestUtils.<JmixButton>getComponent(dialog, "saveAndCloseBtn").click();

            assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
            assertThat(withClass(block, "dashboard-list-row")).hasSize(2);
        });
    }

    @Test
    void copyHiddenWithoutCreatePermissionTest() {
        // May read time entries, and may not create them
        User approver = data.saveUser("approver", "Ap", "Prover");
        assignRole(approver, TimeEntryApprovalRole.CODE);
        data.saveEntryOf(task, approver, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        systemAuthenticator.runWithUser(approver.getUsername(), () -> {
            RecentEntriesBlock block = block("recentEntriesBlock");
            dashboard().refresh();

            assertThat(withClass(block, "dashboard-list-row")).hasSize(1);
            assertThat(withClass(block, "row-copy")).isEmpty();
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
            assertThat(isShown(comingUp, "emptyState")).isTrue();
            assertThat(isShown(recent, "emptyState")).isTrue();
            assertThat(withClass(recent, "dashboard-list-row")).isEmpty();
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
     * Whether the block's component with the id and every parent of it up to the block are visible.
     */
    private static boolean isShown(Fragment<?> block, String id) {
        Component current = FragmentUtils.getComponent(block, id);
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

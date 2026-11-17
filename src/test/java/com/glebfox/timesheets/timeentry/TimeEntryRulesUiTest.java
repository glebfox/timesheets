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

package com.glebfox.timesheets.timeentry;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.security.ProjectScopeRole;
import com.glebfox.timesheets.security.UiMinimalRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.glebfox.timesheets.view.timeentry.TimeEntryListView;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.markdowneditor.MarkdownEditor;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.fullcalendarflowui.component.FullCalendar;
import io.jmix.fullcalendarflowui.component.contextmenu.FullCalendarContextMenu;
import io.jmix.fullcalendarflowui.component.contextmenu.FullCalendarMenuItem;
import io.jmix.fullcalendarflowui.component.contextmenu.event.FullCalendarCellContext;
import io.jmix.fullcalendarflowui.component.data.ContainerCalendarDataProvider;
import io.jmix.fullcalendarflowui.component.data.EntityCalendarEvent;
import io.jmix.fullcalendarflowui.component.event.AbstractEventMoveEvent.OldValues;
import io.jmix.fullcalendarflowui.component.event.DatesSetEvent;
import io.jmix.fullcalendarflowui.component.event.DisplayModeInfo;
import io.jmix.fullcalendarflowui.component.event.EventDropEvent;
import io.jmix.fullcalendarflowui.kit.component.model.CalendarDisplayModes;
import io.jmix.fullcalendarflowui.kit.component.model.CalendarDuration;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.notification.NotificationInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.APPROVED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The time entry screens offer only what the time entry rules allow. The acting users hold Full access and Project
 * scope, so only the rules narrow what they do. The fixture is project P, where {@code worker} is a Member and
 * {@code manager} a Manager. The worker has a New, an Approved, and a Rejected entry on P, the Rejected one with a tag
 * of P's tag group, and the manager an Approved one, all on a day of December 2024, which the calendar shows.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryRulesUiTest {

    static final String USERNAME_PREFIX = "time-entry-rules-ui-test-";

    // December 2024 starts on Sunday, so its month grid runs from December 1 to January 4
    static final LocalDate GRID_START = LocalDate.of(2024, 12, 1);
    static final LocalDate GRID_END = LocalDate.of(2025, 1, 5);
    static final LocalDate DAY = LocalDate.of(2024, 12, 3);

    @Autowired
    DataManager dataManager;
    @Autowired
    EntityStates entityStates;
    @Autowired
    Messages messages;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    ApprovalTestData data;

    User worker;
    User manager;
    Project project;
    ProjectRole managerRole;
    ProjectParticipant workerOnProject;
    Tag tag;
    Task task;

    TimeEntry workerNew;
    TimeEntry workerApproved;
    TimeEntry workerRejected;
    TimeEntry managerApproved;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));

        worker = saveUser("worker", FullAccessRole.CODE);
        manager = saveUser("manager", FullAccessRole.CODE);

        project = data.saveProject("P", data.saveClient());
        task = data.saveTask(project, data.saveTaskType());
        TagGroup tagGroup = data.save(dataManager.create(TagGroup.class), group -> {
            group.setName("tag group");
            group.setProject(project);
        });
        tag = data.save(dataManager.create(Tag.class), t -> {
            t.setName("A");
            t.setTagGroup(tagGroup);
        });

        managerRole = data.saveRole(ProjectRoleType.MANAGER);
        workerOnProject = data.saveParticipant(project, worker, data.saveRole(ProjectRoleType.MEMBER));
        data.saveParticipant(project, manager, managerRole);

        workerNew = data.saveEntry(task, worker, DAY, NEW, null);
        workerApproved = data.saveEntry(task, worker, DAY, APPROVED, null);
        workerRejected = data.save(dataManager.create(TimeEntry.class), entry -> {
            entry.setTask(task);
            entry.setUser(worker);
            entry.setDate(DAY);
            entry.setSpentTime(WorkTime.ofHours(4));
            entry.setStatus(REJECTED);
            entry.setRejectionReason("Too vague");
            entry.setTags(new ArrayList<>(List.of(tag)));
        });
        managerApproved = data.saveEntry(task, manager, DAY, APPROVED, null);
    }

    @AfterEach
    void tearDown() {
        // The entries a screen saved, such as a copy, besides the fixture's. A copy shares its source's tags, and the
        // removal of an entry for good removes its tags too (#8), so each entry lets go of its tags first; the helper
        // then removes the tags
        List<UUID> userIds = Stream.of(worker, manager)
                .filter(Objects::nonNull)
                .map(User::getId)
                .toList();
        dataManager.load(TimeEntry.class)
                .query("e.user.id in ?1", userIds)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list()
                .forEach(entry -> {
                    entry.setTags(new ArrayList<>());
                    dataManager.save(new SaveContext()
                            .removing(dataManager.save(entry))
                            .setHint(PersistenceHints.SOFT_DELETION, false));
                });
        data.removeAll();
    }

    @Test
    void copyIsOfferedOnAnApprovedEntry() {
        as(worker, () -> {
            View<?> list = openList(MyTimeEntryListView.class);
            select(list, workerApproved);
            Action copy = Objects.requireNonNull(grid(list).getAction("copy"));
            assertThat(copy.isEnabled()).isTrue();

            copy.actionPerform(grid(list));

            TimeEntryDetailView editor = UiTestUtils.getCurrentView();
            TimeEntry copied = editor.getEditedEntity();
            assertThat(entityStates.isNew(copied)).isTrue();
            assertThat(copied.getId()).isNotEqualTo(workerApproved.getId());
            assertThat(copied.getStatus()).isEqualTo(NEW);
            assertThat(copied.getRejectionReason()).isNull();
        });
    }

    @Test
    void copyOfAnApprovedEntrySavesAsOpened() {
        Tag sourceTag = data.save(dataManager.create(Tag.class), t -> {
            t.setName("B");
            t.setTagGroup(tag.getTagGroup());
        });
        TimeEntry source = data.save(dataManager.create(TimeEntry.class), entry -> {
            entry.setTask(task);
            entry.setUser(worker);
            entry.setDate(DAY);
            entry.setSpentTime(WorkTime.ofHours(2));
            entry.setStatus(APPROVED);
            entry.setTags(new ArrayList<>(List.of(sourceTag)));
        });

        UUID copyId = systemAuthenticator.withUser(worker.getUsername(), () -> {
            View<?> list = openList(MyTimeEntryListView.class);
            select(list, source);
            Objects.requireNonNull(grid(list).getAction("copy")).actionPerform(grid(list));

            // Saved as opened: the editor sets the copy's tags again from its fields, the only change
            TimeEntryDetailView editor = UiTestUtils.getCurrentView();
            UUID id = editor.getEditedEntity().getId();
            JmixButton saveAndCloseBtn = UiTestUtils.getComponent(editor, "saveAndCloseBtn");
            saveAndCloseBtn.click();

            CollectionContainer<TimeEntry> entries = ViewControllerUtils.getViewData(list)
                    .getContainer("timeEntriesDc");
            assertThat(entries.containsItem(id)).as("the list shows the copy").isTrue();
            return id;
        });

        TimeEntry copy = dataManager.load(TimeEntry.class)
                .id(copyId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE))
                .one();
        assertThat(copy.getStatus()).isEqualTo(NEW);
        assertThat(copy.getDate()).isEqualTo(DAY);
        assertThat(copy.getTags()).extracting(Tag::getId).containsExactly(sourceTag.getId());
        assertThat(stored(source).getStatus()).isEqualTo(APPROVED);
    }

    @Test
    void removeIsDisabledOnALockedEntry() {
        as(worker, () -> {
            View<?> list = openList(MyTimeEntryListView.class);
            Action remove = Objects.requireNonNull(grid(list).getAction("remove"));

            select(list, workerApproved);
            assertThat(remove.isEnabled()).as("Approved").isFalse();

            select(list, workerNew);
            assertThat(remove.isEnabled()).as("New").isTrue();
        });
    }

    @Test
    void timeEntriesListOffersNoCreateOrCopyWithoutUserModify() {
        // A Manager's grants on time entries, which leave the user read only
        User limitedManager = data.saveUser("limited-manager", "limited", "manager");
        assignRole(limitedManager, UiMinimalRole.CODE, RoleAssignmentRoleType.RESOURCE);
        assignRole(limitedManager, TimeEntryWithoutUserTestRole.CODE, RoleAssignmentRoleType.RESOURCE);
        assignRole(limitedManager, ProjectScopeRole.CODE, RoleAssignmentRoleType.ROW_LEVEL);
        data.saveParticipant(project, limitedManager, managerRole);

        as(limitedManager, () -> {
            View<?> list = openList(TimeEntryListView.class);

            assertThat(Objects.requireNonNull(grid(list).getAction("create")).isVisible()).isFalse();
            assertThat(Objects.requireNonNull(grid(list).getAction("copy")).isVisible()).isFalse();
            assertThat(UiTestUtils.<JmixButton>getComponent(list, "createBtn").isVisible()).isFalse();
            assertThat(UiTestUtils.<JmixButton>getComponent(list, "copyBtn").isVisible()).isFalse();
            assertThat(UiTestUtils.<JmixButton>getComponent(list, "editBtn").isVisible()).isTrue();
        });

        // Full access alone
        systemAuthenticator.runWithUser("admin", () -> {
            View<?> list = openList(TimeEntryListView.class);

            assertThat(Objects.requireNonNull(grid(list).getAction("create")).isVisible()).isTrue();
            assertThat(Objects.requireNonNull(grid(list).getAction("copy")).isVisible()).isTrue();
            assertThat(UiTestUtils.<JmixButton>getComponent(list, "createBtn").isVisible()).isTrue();
            assertThat(UiTestUtils.<JmixButton>getComponent(list, "copyBtn").isVisible()).isTrue();
        });
    }

    @Test
    void managersOwnApprovedEntryOpensReadOnly() {
        as(manager, () -> assertThat(openEditor(managerApproved).isReadOnly()).isTrue());

        // The author's own New entry opens for changes
        as(worker, () -> assertThat(openEditor(workerNew).isReadOnly()).isFalse());
    }

    @Test
    void calendarRefusesMovingManagersOwnApprovedEntry() {
        as(manager, () -> {
            CalendarView calendarView = openCalendar();

            moveTimeEntry(calendarView, managerApproved, DAY.plusDays(1));

            assertThat(findCalendarEvent(calendarView, managerApproved).getEntity().getDate()).isEqualTo(DAY);
            NotificationInfo notification = Objects.requireNonNull(UiTestUtils.getLastOpenedNotification(),
                    "a notification");
            assertThat(notification.getType()).isEqualTo(Notifications.Type.WARNING);
            assertThat(notification.getText()).isEqualTo(
                    messages.getMessage(CalendarView.class, "info.TimeEntry.movePrevented.approved.text"));
        });

        assertThat(stored(managerApproved).getDate()).isEqualTo(DAY);
    }

    @Test
    void calendarOffersNoRemoveOnALockedEntry() {
        as(worker, () -> {
            CalendarView calendarView = openCalendar();
            String copy = messages.getMessage("actions.Copy");
            String remove = messages.getMessage("actions.Remove");

            assertThat(openContextMenu(calendarView, workerApproved)).extracting(FullCalendarMenuItem::getText)
                    .containsExactly(copy);
            assertThat(openContextMenu(calendarView, workerNew)).extracting(FullCalendarMenuItem::getText)
                    .containsExactly(copy, remove);
        });
    }

    @Test
    void calendarRefusesACopyOntoAProjectLeft() {
        dataManager.remove(workerOnProject);

        as(worker, () -> {
            CalendarView calendarView = openCalendar();
            int shown = getDataProvider(calendarView).getItems().size();
            String copy = messages.getMessage("actions.Copy");

            click(openContextMenu(calendarView, workerNew).stream()
                    .filter(item -> copy.equals(item.getText()))
                    .findFirst()
                    .orElseThrow());

            NotificationInfo notification = Objects.requireNonNull(UiTestUtils.getLastOpenedNotification(),
                    "a notification");
            assertThat(notification.getType()).isEqualTo(Notifications.Type.WARNING);
            assertThat(notification.getText()).isEqualTo(
                    messages.getMessage(CalendarView.class, "info.TimeEntry.copyPrevented.text"));
            assertThat(getDataProvider(calendarView).getItems()).hasSize(shown);
        });

        assertThat(dataManager.load(TimeEntry.class)
                .query("e.user = ?1", worker)
                .list())
                .extracting(TimeEntry::getId)
                .containsExactlyInAnyOrder(workerNew.getId(), workerApproved.getId(), workerRejected.getId());
    }

    @Test
    void authorWhoLeftTheProjectFixesARejectedEntryInTheEditor() {
        dataManager.remove(workerOnProject);

        as(worker, () -> {
            TimeEntryDetailView editor = openEditor(workerRejected);
            assertThat(editor.isReadOnly()).isFalse();

            MarkdownEditor descriptionField = UiTestUtils.getComponent(editor, "descriptionField");
            descriptionField.setValue("Fixed after leaving");
            JmixButton saveAndCloseBtn = UiTestUtils.getComponent(editor, "saveAndCloseBtn");
            saveAndCloseBtn.click();
        });

        TimeEntry stored = stored(workerRejected);
        assertThat(stored.getDescription()).isEqualTo("Fixed after leaving");
        assertThat(stored.getStatus()).isEqualTo(NEW);
        assertThat(stored.getTags()).extracting(Tag::getId).containsExactly(tag.getId());
    }

    private void as(User user, Runnable action) {
        systemAuthenticator.runWithUser(user.getUsername(), action);
    }

    private View<?> openList(Class<? extends View<?>> viewClass) {
        viewBuilders.view(UiTestUtils.getCurrentView(), viewClass).open();
        return UiTestUtils.getCurrentView();
    }

    private static DataGrid<TimeEntry> grid(View<?> list) {
        return UiTestUtils.getComponent(list, "timeEntriesDataGrid");
    }

    // The grid selects the instance its container holds
    private static void select(View<?> list, TimeEntry entry) {
        CollectionContainer<TimeEntry> container = ViewControllerUtils.getViewData(list).getContainer("timeEntriesDc");
        grid(list).select(container.getItem(entry.getId()));
    }

    // As the screens open the editor on an own entry
    private TimeEntryDetailView openEditor(TimeEntry entry) {
        viewBuilders.detail(UiTestUtils.getCurrentView(), TimeEntry.class, TimeEntryDetailView.class)
                .editEntity(entry)
                .withViewConfigurer(editor -> editor.setOwnTimeEntry(true))
                .open();
        return UiTestUtils.getCurrentView();
    }

    private CalendarView openCalendar() {
        viewBuilders.view(UiTestUtils.getCurrentView(), CalendarView.class).open();
        CalendarView calendarView = UiTestUtils.getCurrentView();

        // The browser reports the displayed dates, which makes the calendar load its plan and entries
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ComponentUtil.fireEvent(calendar,
                new DatesSetEvent(calendar, true, GRID_START, GRID_END, monthDisplayModeInfo()));

        return calendarView;
    }

    private void moveTimeEntry(CalendarView calendarView, TimeEntry timeEntry, LocalDate date) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        EntityCalendarEvent<TimeEntryCalendarWrapper> calendarEvent = findCalendarEvent(calendarView, timeEntry);
        LocalDate oldDate = calendarEvent.getEntity().getDate();

        // The calendar applies the new date to the dropped event before it fires the event
        calendarEvent.setStartDateTime(date.atStartOfDay());
        ComponentUtil.fireEvent(calendar,
                new EventDropEvent(calendar, true, calendarEvent, getDataProvider(calendarView),
                        List.of(), List.of(), new OldValues(oldDate.atStartOfDay(), null, true),
                        CalendarDuration.ofDays(1), null, monthDisplayModeInfo()));
    }

    /**
     * Opens the entry's context menu as a right-click on the entry does: the browser asks the menu's handler, which
     * fills the menu, before it shows the menu. Returns the menu's items.
     */
    private List<FullCalendarMenuItem> openContextMenu(CalendarView calendarView, TimeEntry timeEntry) {
        FullCalendarContextMenu contextMenu = contextMenuOf(calendarView);
        FullCalendarCellContext context = new FullCalendarCellContext(null, findCalendarEvent(calendarView, timeEntry),
                getDataProvider(calendarView), null);

        assertThat(Objects.requireNonNull(contextMenu.getContentMenuHandler()).apply(context))
                .as("the menu opens")
                .isTrue();
        return contextMenu.getItems();
    }

    // The view keeps the menu in a field; nothing else leads to it
    private static FullCalendarContextMenu contextMenuOf(CalendarView calendarView) {
        try {
            Field field = CalendarView.class.getDeclaredField("contextMenu");
            field.setAccessible(true);
            return (FullCalendarContextMenu) field.get(calendarView);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // A menu item's click listener listens to the browser's click on its element
    private static void click(FullCalendarMenuItem item) {
        item.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(item.getElement(), "click", JacksonUtils.createObjectNode()));
    }

    @SuppressWarnings("unchecked")
    private EntityCalendarEvent<TimeEntryCalendarWrapper> findCalendarEvent(CalendarView calendarView,
                                                                           TimeEntry timeEntry) {
        return getDataProvider(calendarView).getItems().stream()
                .map(calendarEvent -> (EntityCalendarEvent<TimeEntryCalendarWrapper>) calendarEvent)
                .filter(calendarEvent -> timeEntry.getId().equals(calendarEvent.getEntity().getTimeEntry().getId()))
                .findFirst()
                .orElseThrow();
    }

    private ContainerCalendarDataProvider<TimeEntryCalendarWrapper> getDataProvider(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        return calendar.getDataProvider("timeEntriesDataProvider");
    }

    private static DisplayModeInfo monthDisplayModeInfo() {
        return new DisplayModeInfo(GRID_START, GRID_END, GRID_START, GRID_START.plusMonths(1),
                CalendarDisplayModes.DAY_GRID_MONTH);
    }

    /**
     * Reads the entry as stored, outside the acting user's block.
     */
    private TimeEntry stored(TimeEntry entry) {
        return dataManager.load(Id.of(entry))
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE))
                .one();
    }

    private User saveUser(String name, String resourceRoleCode) {
        User user = data.saveUser(name, name, name);
        assignRole(user, resourceRoleCode, RoleAssignmentRoleType.RESOURCE);
        assignRole(user, ProjectScopeRole.CODE, RoleAssignmentRoleType.ROW_LEVEL);
        return user;
    }

    private void assignRole(User user, String roleCode, String roleType) {
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(roleType);
        });
    }
}

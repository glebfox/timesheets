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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.app.TimeEntryApprovalSupport;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.accesscontext.UiMenuContext;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.menu.MenuConfig;
import io.jmix.flowui.menu.MenuItem;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.securityflowui.accesscontext.UiLoginToUiContext;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.APPROVED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The job roles, each held alone with the row-level role that the combination table gives it: Project scope for
 * Worker, Approver, Manager, and HR, Client scope for Observer, and Time entry rules for Project office.
 * {@link #WHO_CAN_DO_WHAT} is the design's who-can-do-what table as the main menu and logging time show it; the other
 * tests check the cells the menu cannot show. Saves go through the constrained {@code DataManager}, approvals through
 * {@link TimeEntryApprovalSupport}, which saves with it too.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class JobRolesTest {

    static final String USERNAME_PREFIX = "job-roles-test-";

    // The items of the main menu, by the views they open
    static final String CALENDAR = CalendarView.ID;
    static final String MY_TIME_ENTRIES = MyTimeEntryListView.ID;
    static final String MY_TASKS = "ts_Task.my";
    static final String CLIENTS = "ts_Client.list";
    static final String PROJECTS = "ts_Project.list";
    static final String TIME_ENTRIES = "ts_TimeEntry.list";
    static final String APPROVAL = TimeEntryApprovalView.ID;
    static final String TASK_TYPES = "ts_TaskType.list";
    static final String PROJECT_ROLES = "ts_ProjectRole.list";
    static final String WORK_SCHEDULES = "ts_WorkSchedule.list";
    static final String HOLIDAY_CALENDARS = "ts_HolidayCalendar.list";
    static final String ABSENCE_TYPES = "ts_AbsenceType.list";
    static final String USERS = "ts_User.list";
    static final String ABSENCES = "ts_Absence.list";

    /**
     * The items of the Security, Data tools, and Pessimistic locking menus: role assignments, the roles, the Entity
     * inspector, and locks, which no job role opens.
     */
    static final List<String> ADMINISTRATION = List.of("sec_ResourceRoleModel.list", "sec_RowLevelRoleModel.list",
            "datatl_entityInspectorListView", "pslock_LockInfo.list");

    /**
     * A row of the who-can-do-what table: a job role, the row-level role assigned with it, whether its holder logs
     * time, the menu items it opens, and those it does not open besides {@link #ADMINISTRATION}.
     */
    record JobRoleRow(String jobRole, String rowLevelRole, boolean logsTime, List<String> permitted,
                      List<String> denied) {

        @Override
        public String toString() {
            return jobRole;
        }
    }

    static final List<JobRoleRow> WHO_CAN_DO_WHAT = List.of(
            new JobRoleRow(WorkerRole.CODE, ProjectScopeRole.CODE, true,
                    List.of(CALENDAR, MY_TIME_ENTRIES, MY_TASKS),
                    List.of(CLIENTS, PROJECTS, TIME_ENTRIES, APPROVAL, TASK_TYPES, PROJECT_ROLES, WORK_SCHEDULES,
                            HOLIDAY_CALENDARS, ABSENCE_TYPES, USERS, ABSENCES)),
            new JobRoleRow(ApproverRole.CODE, ProjectScopeRole.CODE, true,
                    List.of(CALENDAR, MY_TIME_ENTRIES, MY_TASKS, APPROVAL, USERS),
                    List.of(CLIENTS, PROJECTS, TIME_ENTRIES, TASK_TYPES, PROJECT_ROLES, WORK_SCHEDULES,
                            HOLIDAY_CALENDARS, ABSENCE_TYPES, ABSENCES)),
            // The client, task type, and project role lists open as the project editor's pickers, with no menu item
            new JobRoleRow(ManagerRole.CODE, ProjectScopeRole.CODE, true,
                    List.of(CALENDAR, MY_TIME_ENTRIES, MY_TASKS, PROJECTS, TIME_ENTRIES, APPROVAL, USERS),
                    List.of(CLIENTS, TASK_TYPES, PROJECT_ROLES, WORK_SCHEDULES, HOLIDAY_CALENDARS, ABSENCE_TYPES,
                            ABSENCES)),
            new JobRoleRow(ObserverRole.CODE, ClientScopeRole.CODE, false,
                    List.of(PROJECTS, TIME_ENTRIES),
                    List.of(CALENDAR, MY_TIME_ENTRIES, MY_TASKS, CLIENTS, APPROVAL, TASK_TYPES, PROJECT_ROLES,
                            WORK_SCHEDULES, HOLIDAY_CALENDARS, ABSENCE_TYPES, USERS, ABSENCES)),
            new JobRoleRow(HrRole.CODE, ProjectScopeRole.CODE, true,
                    List.of(CALENDAR, MY_TIME_ENTRIES, MY_TASKS, WORK_SCHEDULES, HOLIDAY_CALENDARS, ABSENCE_TYPES,
                            USERS, ABSENCES),
                    List.of(CLIENTS, PROJECTS, TIME_ENTRIES, APPROVAL, TASK_TYPES, PROJECT_ROLES)),
            // The user list opens as the participant editor's picker, with no menu item
            new JobRoleRow(ProjectOfficeRole.CODE, TimeEntryRulesRole.CODE, true,
                    List.of(CALENDAR, MY_TIME_ENTRIES, MY_TASKS, CLIENTS, PROJECTS, TASK_TYPES, PROJECT_ROLES),
                    List.of(TIME_ENTRIES, APPROVAL, WORK_SCHEDULES, HOLIDAY_CALENDARS, ABSENCE_TYPES, USERS,
                            ABSENCES)));

    @Autowired
    DataManager dataManager;
    @Autowired
    AccessManager accessManager;
    @Autowired
    Metadata metadata;
    @Autowired
    MenuConfig menuConfig;
    @Autowired
    EntityStates entityStates;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    TimeEntryApprovalSupport approvalSupport;

    ApprovalTestData data;
    // New entities that a test expects to be refused, removed should a save let one through
    List<Id<?>> attemptedIds = new ArrayList<>();
    LocalDate today;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));
        today = LocalDate.now();
    }

    static List<JobRoleRow> whoCanDoWhat() {
        return WHO_CAN_DO_WHAT;
    }

    @Test
    void everyRowOfTheTableSortsEveryMenuItem() {
        List<String> menuItems = leafIds(menuConfig.getRootItems());

        for (JobRoleRow row : WHO_CAN_DO_WHAT) {
            assertThat(Stream.of(row.permitted(), row.denied(), ADMINISTRATION).flatMap(List::stream).toList())
                    .as(row.jobRole())
                    .containsExactlyInAnyOrderElementsOf(menuItems);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("whoCanDoWhat")
    void jobRoleOpensWhatTheTableSays(JobRoleRow row) {
        User holder = saveHolder("holder", row.rowLevelRole(), row.jobRole());

        as(holder, () -> {
            // The job role is the holder's only resource role, so it grants signing in itself
            assertThat(loginPermitted()).as("signing in").isTrue();
            for (String menuItem : row.permitted()) {
                assertThat(menuItemPermitted(menuItem)).as("menu item %s", menuItem).isTrue();
            }
            for (String menuItem : Stream.concat(row.denied().stream(), ADMINISTRATION.stream()).toList()) {
                assertThat(menuItemPermitted(menuItem)).as("menu item %s", menuItem).isFalse();
            }
            assertThat(entityContext(TimeEntry.class).isCreatePermitted())
                    .as("creates time entries")
                    .isEqualTo(row.logsTime());
        });
    }

    @Test
    void projectOfficeWithApproverSeesEveryProjectAndApprovesByTheRule() {
        User office = saveHolder("office", TimeEntryRulesRole.CODE, ProjectOfficeRole.CODE, ApproverRole.CODE);
        User member = data.saveUser("member", "Member", "Member");
        Client client = data.saveClient();
        TaskType taskType = data.saveTaskType();
        ProjectRole approverRole = data.saveRole(ProjectRoleType.APPROVER);
        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);

        // The office is an Approver of the first project, a Member of the second, and takes no part in the third
        Project approved = data.saveProject("approved", client);
        Project worked = data.saveProject("worked", client);
        Project other = data.saveProject("other", client);
        data.saveParticipant(approved, office, approverRole);
        data.saveParticipant(worked, office, memberRole);
        for (Project project : List.of(approved, worked, other)) {
            data.saveParticipant(project, member, memberRole);
        }
        Task approvedTask = data.saveTask(approved, taskType);
        TimeEntry memberOnApproved = data.saveEntry(approvedTask, member, today, NEW, null);
        TimeEntry officeOnApproved = data.saveEntry(approvedTask, office, today, NEW, null);
        TimeEntry memberOnWorked = data.saveEntry(data.saveTask(worked, taskType), member, today, NEW, null);
        TimeEntry memberOnOther = data.saveEntry(data.saveTask(other, taskType), member, today, NEW, null);

        as(office, () -> {
            // Time entry rules narrows no reading, so the holder reads every project
            assertThat(loadIds(Project.class, idsOf(approved, worked, other)))
                    .containsExactlyInAnyOrderElementsOf(idsOf(approved, worked, other));

            // The approval screen's loads apply the approval rule: the member's entry where the holder is an
            // Approver, not the holder's own there
            assertThat(approvalSupport.loadProjectsToApprove()).extracting(Project::getId)
                    .containsExactly(approved.getId());
            assertThat(approvalSupport.loadEntriesToApprove(null, today, today)).extracting(TimeEntry::getId)
                    .containsExactly(memberOnApproved.getId());

            approvalSupport.approve(List.of(load(memberOnApproved)));
            assertThatThrownBy(() -> approvalSupport.approve(List.of(load(memberOnWorked))))
                    .isInstanceOf(IllegalArgumentException.class);

            // A plain save of the status, which the approval operations' own check of the rule does not guard
            refusedToSave(withStatus(load(memberOnWorked), APPROVED));
            refusedToSave(withStatus(load(memberOnOther), APPROVED));
            refusedToSave(withStatus(load(officeOnApproved), APPROVED));
        });

        assertThat(stored(memberOnApproved).getStatus()).isEqualTo(APPROVED);
        for (TimeEntry entry : List.of(memberOnWorked, memberOnOther, officeOnApproved)) {
            assertThat(stored(entry).getStatus()).isEqualTo(NEW);
        }
    }

    @Test
    void observerLogsNoTimeAndSeesNoUsersList() {
        User observer = saveHolder("observer", ClientScopeRole.CODE, ObserverRole.CODE);
        User member = data.saveUser("member", "Member", "Member");
        Project observed = data.saveProject("observed", data.saveClient());
        Task task = data.saveTask(observed, data.saveTaskType());
        data.saveParticipant(observed, observer, data.saveRole(ProjectRoleType.OBSERVER));
        data.saveParticipant(observed, member, data.saveRole(ProjectRoleType.MEMBER));

        as(observer, () -> {
            assertThat(entityContext(TimeEntry.class).isCreatePermitted()).isFalse();
            refusedToSave(entryOf(task, observer));
            assertViewsDenied(CALENDAR, MY_TIME_ENTRIES, MY_TASKS);

            // The holder reads the people of the projects they observe, as the project editor shows them, but has
            // no Users screen
            assertThat(dataManager.load(Id.of(member)).optional()).isPresent();
            assertViewsDenied(USERS, "ts_User.detail");
            assertThat(menuItemPermitted(USERS)).isFalse();
        });
    }

    @Test
    void hrReadsUsersButCannotChangeThem() {
        User hr = saveHolder("hr", ProjectScopeRole.CODE, HrRole.CODE);
        User someone = data.saveUser("someone", "Some", "One");

        as(hr, () -> {
            assertViewsPermitted(USERS, "ts_User.detail");
            assertThat(menuItemPermitted(USERS)).isTrue();

            // Project scope leaves users open, so the holder reads people they share no project with
            UiEntityContext context = entityContext(User.class);
            assertThat(context.isViewPermitted()).as("read").isTrue();
            assertThat(context.isCreatePermitted()).as("create").isFalse();
            assertThat(context.isEditPermitted()).as("update").isFalse();
            assertThat(context.isDeletePermitted()).as("delete").isFalse();
            assertAttributesViewOnly(User.class);

            User loaded = dataManager.load(Id.of(someone)).one();
            loaded.setFirstName("Changed");
            refusedToSave(loaded);
            assertRefused(() -> dataManager.remove(dataManager.load(Id.of(someone)).one()));

            User created = dataManager.create(User.class);
            created.setUsername(hr.getUsername() + "-created");
            refusedToSave(created);
        });

        User stored = dataManager.load(Id.of(someone)).optional().orElseThrow();
        assertThat(stored.getFirstName()).isEqualTo("Some");
    }

    @Test
    void projectOfficeWritesOnlyItsOwnEntries() {
        User office = saveHolder("office", TimeEntryRulesRole.CODE, ProjectOfficeRole.CODE);
        User member = data.saveUser("member", "Member", "Member");
        Project project = data.saveProject("worked", data.saveClient());
        Task task = data.saveTask(project, data.saveTaskType());
        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);
        data.saveParticipant(project, office, memberRole);
        data.saveParticipant(project, member, memberRole);
        TimeEntry memberEntry = data.saveEntry(task, member, today, NEW, null);

        as(office, () -> {
            TimeEntry own = data.save(entryOf(task, office), entry -> {
            });
            own.setDescription("Changed by its author");
            dataManager.save(own);

            refusedToSave(entryOf(task, member));

            TimeEntry others = load(memberEntry);
            others.setDescription("Changed by Project office");
            refusedToSave(others);
            assertRefused(() -> dataManager.remove(load(memberEntry)));
        });

        TimeEntry stored = stored(memberEntry);
        assertThat(stored.getDescription()).isNull();
        assertThat(dataManager.load(TimeEntry.class)
                .query("e.user = ?1", office)
                .list())
                .extracting(TimeEntry::getDescription)
                .containsExactly("Changed by its author");
    }

    @AfterEach
    void tearDown() {
        // The attempts come before the tasks and users they refer to
        for (Id<?> id : attemptedIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        attemptedIds.clear();
        data.removeAll();
    }

    /**
     * Saves a user with the given resource roles and the row-level role.
     */
    private User saveHolder(String name, String rowLevelRole, String... resourceRoles) {
        User user = data.saveUser(name, name, name);
        for (String resourceRole : resourceRoles) {
            assignRole(user, resourceRole, RoleAssignmentRoleType.RESOURCE);
        }
        assignRole(user, rowLevelRole, RoleAssignmentRoleType.ROW_LEVEL);
        return user;
    }

    private void assignRole(User user, String roleCode, String roleType) {
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(roleType);
        });
    }

    private void as(User user, Runnable action) {
        systemAuthenticator.runWithUser(user.getUsername(), action);
    }

    private void refusedToSave(Object entity) {
        // An existing entity is the fixture's, removed with it
        if (entityStates.isNew(entity)) {
            attemptedIds.add(Id.of(entity));
        }
        assertRefused(() -> dataManager.save(entity));
    }

    private static void assertRefused(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    private TimeEntry entryOf(Task task, User user) {
        TimeEntry entry = dataManager.create(TimeEntry.class);
        entry.setTask(task);
        entry.setUser(user);
        entry.setDate(today);
        entry.setSpentTime(WorkTime.ofHours(1));
        return entry;
    }

    private static TimeEntry withStatus(TimeEntry entry, TimeEntryStatus status) {
        entry.setStatus(status);
        return entry;
    }

    /**
     * Loads the entity by its id with the {@code _base} fetch plan, as the acting user when called in their block.
     */
    private <E> E load(E entity) {
        return dataManager.load(Id.of(entity)).one();
    }

    /**
     * Reads the entry as stored, as the administrator, outside the acting user's block.
     */
    private TimeEntry stored(TimeEntry entry) {
        return dataManager.load(Id.of(entry)).one();
    }

    private List<Object> loadIds(Class<?> entityClass, List<Object> ids) {
        return dataManager.load(entityClass)
                .query("e.id in :ids")
                .parameter("ids", ids)
                .list()
                .stream()
                .map(EntityValues::getId)
                .toList();
    }

    private static List<Object> idsOf(Object... entities) {
        return Stream.of(entities).map(EntityValues::getId).toList();
    }

    private boolean loginPermitted() {
        UiLoginToUiContext context = new UiLoginToUiContext();
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private UiEntityContext entityContext(Class<?> entityClass) {
        UiEntityContext context = new UiEntityContext(metadata.getClass(entityClass));
        accessManager.applyRegisteredConstraints(context);
        return context;
    }

    private void assertAttributesViewOnly(Class<?> entityClass) {
        MetaClass metaClass = metadata.getClass(entityClass);
        for (MetaProperty property : metaClass.getProperties()) {
            UiEntityAttributeContext context = new UiEntityAttributeContext(metaClass, property.getName());
            accessManager.applyRegisteredConstraints(context);
            assertThat(context.canView()).as("view %s", property.getName()).isTrue();
            assertThat(context.canModify()).as("modify %s", property.getName()).isFalse();
        }
    }

    private void assertViewsPermitted(String... viewIds) {
        for (String viewId : viewIds) {
            assertThat(viewPermitted(viewId)).as("view %s", viewId).isTrue();
        }
    }

    private void assertViewsDenied(String... viewIds) {
        for (String viewId : viewIds) {
            assertThat(viewPermitted(viewId)).as("view %s", viewId).isFalse();
        }
    }

    private boolean viewPermitted(String viewId) {
        UiShowViewContext context = new UiShowViewContext(viewId);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private boolean menuItemPermitted(String menuItemId) {
        MenuItem menuItem = findMenuItem(menuConfig.getRootItems(), menuItemId);
        assertThat(menuItem).as("menu item %s", menuItemId).isNotNull();
        UiMenuContext context = new UiMenuContext(menuItem);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    @Nullable
    private static MenuItem findMenuItem(List<MenuItem> items, String id) {
        for (MenuItem item : items) {
            if (id.equals(item.getId())) {
                return item;
            }
            MenuItem child = findMenuItem(item.getChildren(), id);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    /**
     * The ids of the items that open a view, the menus' own items left out.
     */
    private static List<String> leafIds(List<MenuItem> items) {
        List<String> ids = new ArrayList<>();
        for (MenuItem item : items) {
            if (item.isMenu()) {
                ids.addAll(leafIds(item.getChildren()));
            } else if (!item.isSeparator()) {
                ids.add(item.getId());
            }
        }
        return ids;
    }
}

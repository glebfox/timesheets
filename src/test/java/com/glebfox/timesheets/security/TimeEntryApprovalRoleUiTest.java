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

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPerson;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.settings.UserSettingsService;
import io.jmix.flowuidata.entity.UserSettingsItem;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The holder signs in with UI: minimal access and holds the role besides. The fixture is one project where the holder
 * is an Approver and {@code member} is a Member, with an entry of the member's today and one a week ago.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryApprovalRoleUiTest {

    static final String USERNAME_PREFIX = "time-entry-approval-ui-test-";
    static final String MODE_SETTING = TimeEntryApprovalView.ID + ".mode";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserSettingsService userSettingsService;

    String holder;
    User member;
    TimeEntry todayEntry;
    TimeEntry lastWeekEntry;
    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

        User holderUser = saveUser();
        assignRole(holderUser, UiMinimalRole.CODE);
        assignRole(holderUser, TimeEntryApprovalRole.CODE);
        holder = holderUser.getUsername();
        member = saveUser();

        Client client = save(dataManager.create(Client.class), c -> c.setName(USERNAME_PREFIX + "client"));
        TaskType taskType = save(dataManager.create(TaskType.class), t -> t.setName(USERNAME_PREFIX + "task type"));
        Project project = save(dataManager.create(Project.class), p -> {
            p.setName(USERNAME_PREFIX + "project");
            p.setClient(client);
        });
        Task task = save(dataManager.create(Task.class), t -> {
            t.setName(USERNAME_PREFIX + "task");
            t.setProject(project);
            t.setType(taskType);
        });
        saveParticipant(project, holderUser, saveProjectRole(ProjectRoleType.APPROVER));
        saveParticipant(project, member, saveProjectRole(ProjectRoleType.MEMBER));
        todayEntry = saveEntry(task, today);
        lastWeekEntry = saveEntry(task, today.minusDays(7));
    }

    @Test
    void holderOpensTheScreenAndSwitchesTheMode() {
        systemAuthenticator.runWithUser(holder, () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), TimeEntryApprovalView.class).open();
            TimeEntryApprovalView view = UiTestUtils.getCurrentView();
            JmixRadioButtonGroup<ApprovalMode> modeField = UiTestUtils.getComponent(view, "modeField");

            // The month is the default mode, and it holds the entry of today
            assertThat(modeField.getValue()).isEqualTo(ApprovalMode.MONTH);
            assertThat(view.getRosterPeople()).extracting(person -> person.user().getUsername())
                    .containsExactly(member.getUsername());
            assertThat(view.getSelectedPerson()).isNotNull()
                    .extracting(User::getUsername).isEqualTo(member.getUsername());
            assertThat(entryIds(view.getRosterPeople())).contains(todayEntry.getId());

            // The week shown first is the one before the current week, which holds the entry of a week ago
            modeField.setValue(ApprovalMode.WEEK);

            assertThat(view.getPeriod().mode()).isEqualTo(ApprovalMode.WEEK);
            assertThat(view.getRosterPeople()).extracting(person -> person.user().getUsername())
                    .containsExactly(member.getUsername());
            assertThat(entryIds(view.getRosterPeople())).containsExactly(lastWeekEntry.getId());
            // The mode is saved for the holder
            assertThat(userSettingsService.load(MODE_SETTING)).contains(ApprovalMode.WEEK.name());
        });

        // A view opened later starts in the saved mode
        systemAuthenticator.runWithUser(holder, () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), TimeEntryApprovalView.class).open();
            TimeEntryApprovalView view = UiTestUtils.getCurrentView();
            JmixRadioButtonGroup<ApprovalMode> modeField = UiTestUtils.getComponent(view, "modeField");

            assertThat(modeField.getValue()).isEqualTo(ApprovalMode.WEEK);
        });
    }

    @AfterEach
    void tearDown() {
        // The holder's settings first, then the rest, which is soft-deleted, so it is removed for good, reloaded by id
        dataManager.load(UserSettingsItem.class)
                .query("e.username = :username")
                .parameter("username", holder)
                .list()
                .forEach(dataManager::remove);
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    private static List<UUID> entryIds(List<ApprovalPerson> people) {
        return people.stream()
                .flatMap(person -> person.entries().stream())
                .map(TimeEntry::getId)
                .toList();
    }

    private <E> E save(E entity, Consumer<E> init) {
        init.accept(entity);
        E saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private User saveUser() {
        return save(dataManager.create(User.class), u -> u.setUsername(USERNAME_PREFIX + UUID.randomUUID()));
    }

    private ProjectRole saveProjectRole(ProjectRoleType type) {
        return save(dataManager.create(ProjectRole.class), r -> {
            r.setName(USERNAME_PREFIX + type.getId() + " role");
            r.setType(type);
        });
    }

    private void saveParticipant(Project project, User user, ProjectRole role) {
        save(dataManager.create(ProjectParticipant.class), p -> {
            p.setProject(project);
            p.setUser(user);
            p.setRole(role);
        });
    }

    private TimeEntry saveEntry(Task task, LocalDate date) {
        return save(dataManager.create(TimeEntry.class), e -> {
            e.setTask(task);
            e.setUser(member);
            e.setDate(date);
            e.setSpentTime(WorkTime.ofHours(1));
            e.setStatus(TimeEntryStatus.NEW);
        });
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }
}

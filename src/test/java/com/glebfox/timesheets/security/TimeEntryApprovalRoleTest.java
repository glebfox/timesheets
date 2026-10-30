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
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.glebfox.timesheets.view.timeentry.approval.TimeEntryApprovalView;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The holder signs in with UI: minimal access and holds the role besides. The fixture is one project where the holder
 * is an Approver and {@code member} is a Member, with an entry of the member's.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TimeEntryApprovalRoleTest {

    static final String USERNAME_PREFIX = "time-entry-approval-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    AccessManager accessManager;
    @Autowired
    Metadata metadata;
    @Autowired
    MenuConfig menuConfig;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    TimeEntryApprovalSupport approvalSupport;

    String holder;
    User holderUser;
    User member;
    Project project;
    TimeEntry memberEntry;
    LocalDate today;
    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        holderUser = saveUser();
        assignRole(holderUser, UiMinimalRole.CODE);
        assignRole(holderUser, TimeEntryApprovalRole.CODE);
        holder = holderUser.getUsername();
        member = saveUser();

        Client client = save(dataManager.create(Client.class), c -> c.setName(USERNAME_PREFIX + "client"));
        TaskType taskType = save(dataManager.create(TaskType.class), t -> t.setName(USERNAME_PREFIX + "task type"));
        project = save(dataManager.create(Project.class), p -> {
            p.setName(USERNAME_PREFIX + "project");
            p.setClient(client);
        });
        TagGroup tagGroup = save(dataManager.create(TagGroup.class), g -> {
            g.setName(USERNAME_PREFIX + "tag group");
            g.setProject(project);
        });
        Tag tag = save(dataManager.create(Tag.class), t -> {
            t.setName(USERNAME_PREFIX + "tag");
            t.setTagGroup(tagGroup);
        });
        Task task = save(dataManager.create(Task.class), t -> {
            t.setName(USERNAME_PREFIX + "task");
            t.setProject(project);
            t.setType(taskType);
        });
        saveParticipant(holderUser, saveProjectRole(ProjectRoleType.APPROVER));
        saveParticipant(member, saveProjectRole(ProjectRoleType.MEMBER));
        memberEntry = save(dataManager.create(TimeEntry.class), e -> {
            e.setTask(task);
            e.setUser(member);
            e.setDate(today);
            e.setSpentTime(WorkTime.ofHours(1));
            e.setStatus(TimeEntryStatus.NEW);
            e.setTags(List.of(tag));
        });
    }

    @Test
    void holderReadsTimeEntriesAndChangesOnlyStatusAndReason() {
        systemAuthenticator.runWithUser(holder, () -> {
            UiEntityContext context = entityContext(TimeEntry.class);
            assertThat(context.isViewPermitted()).isTrue();
            assertThat(context.isEditPermitted()).isTrue();
            assertThat(context.isCreatePermitted()).isFalse();
            assertThat(context.isDeletePermitted()).isFalse();

            MetaClass metaClass = metadata.getClass(TimeEntry.class);
            for (MetaProperty property : metaClass.getProperties()) {
                UiEntityAttributeContext attributeContext =
                        new UiEntityAttributeContext(metaClass, property.getName());
                accessManager.applyRegisteredConstraints(attributeContext);
                assertThat(attributeContext.canView()).as("view %s", property.getName()).isTrue();
                assertThat(attributeContext.canModify())
                        .as("modify %s", property.getName())
                        .isEqualTo(Set.of("status", "rejectionReason").contains(property.getName()));
            }
        });
    }

    @Test
    void holderMayNotCreateOrDeleteTimeEntries() {
        systemAuthenticator.runWithUser(holder, () -> {
            TimeEntry created = dataManager.create(TimeEntry.class);
            created.setTask(memberEntry.getTask());
            created.setUser(member);
            created.setDate(today);
            created.setSpentTime(WorkTime.ofHours(1));
            created.setStatus(TimeEntryStatus.NEW);
            assertThatThrownBy(() -> dataManager.save(created)).isInstanceOf(AccessDeniedException.class);

            assertThatThrownBy(() -> dataManager.remove(memberEntry)).isInstanceOf(AccessDeniedException.class);
        });

        assertThat(dataManager.load(Id.of(memberEntry)).optional()).isPresent();
    }

    @Test
    void holderReadsWhatTheScreenShows() {
        // The data layer checks the entity's READ only for the entity a load starts from, so a load cannot show the
        // grants of Task, ProjectRole, Tag, TagGroup, and User, which the screen reaches through other entities.
        // The entity context of the UI checks each of them.
        systemAuthenticator.runWithUser(holder, () -> {
            for (Class<?> entityClass : List.of(Task.class, Project.class, ProjectParticipant.class,
                    ProjectRole.class, Tag.class, TagGroup.class, User.class)) {
                UiEntityContext context = entityContext(entityClass);
                assertThat(context.isViewPermitted()).as("read %s", entityClass.getSimpleName()).isTrue();
                assertThat(context.isEditPermitted()).as("update %s", entityClass.getSimpleName()).isFalse();
                assertThat(context.isCreatePermitted()).as("create %s", entityClass.getSimpleName()).isFalse();
                assertThat(context.isDeletePermitted()).as("delete %s", entityClass.getSimpleName()).isFalse();

                MetaClass metaClass = metadata.getClass(entityClass);
                for (MetaProperty property : metaClass.getProperties()) {
                    UiEntityAttributeContext attributeContext =
                            new UiEntityAttributeContext(metaClass, property.getName());
                    accessManager.applyRegisteredConstraints(attributeContext);
                    assertThat(attributeContext.canView())
                            .as("view %s.%s", entityClass.getSimpleName(), property.getName())
                            .isTrue();
                    assertThat(attributeContext.canModify())
                            .as("modify %s.%s", entityClass.getSimpleName(), property.getName())
                            .isFalse();
                }
            }
        });
    }

    @Test
    void holderOpensTheApprovalViewAndItsMenuItem() {
        systemAuthenticator.runWithUser(holder, () -> {
            assertThat(viewPermitted(TimeEntryApprovalView.ID)).as("view %s", TimeEntryApprovalView.ID).isTrue();
            assertThat(menuItemPermitted(TimeEntryApprovalView.ID))
                    .as("menu item %s", TimeEntryApprovalView.ID)
                    .isTrue();

            // The role grants the screen alone: the project choice comes from the support bean
            for (String viewId : List.of("ts_Project.list", "ts_Project.detail", "ts_TimeEntry.list",
                    "ts_TimeEntry.detail")) {
                assertThat(viewPermitted(viewId)).as("view %s", viewId).isFalse();
            }
            assertThat(menuItemPermitted("ts_Project.list")).as("menu item ts_Project.list").isFalse();
            assertThat(menuItemPermitted("ts_TimeEntry.list")).as("menu item ts_TimeEntry.list").isFalse();
        });
    }

    @Test
    void holderLoadsWhatTheScreenShows() {
        systemAuthenticator.runWithUser(holder, () -> {
            List<Project> projects = approvalSupport.loadProjectsToApprove();
            assertThat(projects).extracting(Project::getId).containsExactly(project.getId());

            List<TimeEntry> entries = approvalSupport.loadEntriesToApprove(null, today, today);
            assertThat(entries).hasSize(1);
            TimeEntry entry = entries.getFirst();
            assertThat(entry.getId()).isEqualTo(memberEntry.getId());
            assertThat(entry.getUser().getUsername()).isEqualTo(member.getUsername());
            assertThat(entry.getTask().getProject().getName()).isEqualTo(project.getName());
            assertThat(entry.getTags()).extracting(Tag::getName).containsExactly(USERNAME_PREFIX + "tag");
            // The group loads with the tag, through the tag's instance name. This does not check the grant to read
            // groups: a load starting from an entry does not need it, and holderReadsWhatTheScreenShows guards it
            TagGroup tagGroup = entry.getTags().getFirst().getTagGroup();
            assertThat(tagGroup).isNotNull();
            assertThat(tagGroup.getName()).isEqualTo(USERNAME_PREFIX + "tag group");

            assertThat(approvalSupport.loadParticipants(projects))
                    .extracting(p -> p.getUser().getId(), p -> p.getRole().getType())
                    .containsExactlyInAnyOrder(
                            tuple(holderUser.getId(), ProjectRoleType.APPROVER),
                            tuple(member.getId(), ProjectRoleType.MEMBER));
        });
    }

    @Test
    void holderApprovesThroughTheSupportBean() {
        systemAuthenticator.runWithUser(holder, () -> {
            TimeEntry current = approvalSupport.loadEntriesToApprove(null, today, today).getFirst();

            List<TimeEntry> approved = approvalSupport.approve(List.of(current));

            assertThat(approved).extracting(TimeEntry::getStatus).containsExactly(TimeEntryStatus.APPROVED);
            assertThat(approved.getFirst().getUser().getUsername()).isEqualTo(member.getUsername());
        });
        assertThat(dataManager.load(Id.of(memberEntry)).one().getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
    }

    @Test
    void holderRejectsThroughTheSupportBean() {
        systemAuthenticator.runWithUser(holder, () -> {
            TimeEntry current = approvalSupport.loadEntriesToApprove(null, today, today).getFirst();

            List<TimeEntry> rejected = approvalSupport.reject(List.of(current), "Not enough detail");

            assertThat(rejected).extracting(TimeEntry::getStatus).containsExactly(TimeEntryStatus.REJECTED);
            assertThat(rejected).extracting(TimeEntry::getRejectionReason).containsExactly("Not enough detail");
        });
        TimeEntry stored = dataManager.load(Id.of(memberEntry)).one();
        assertThat(stored.getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
        assertThat(stored.getRejectionReason()).isEqualTo("Not enough detail");
    }

    @AfterEach
    void tearDown() {
        // Role assignments, users, and the rest are soft-deleted, so they are removed for good, reloaded by id
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

    private UiEntityContext entityContext(Class<?> entityClass) {
        UiEntityContext context = new UiEntityContext(metadata.getClass(entityClass));
        accessManager.applyRegisteredConstraints(context);
        return context;
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

    private void saveParticipant(User user, ProjectRole role) {
        save(dataManager.create(ProjectParticipant.class), p -> {
            p.setProject(project);
            p.setUser(user);
            p.setRole(role);
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

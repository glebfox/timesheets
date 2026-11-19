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
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.accesscontext.InMemoryCrudEntityContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.core.usersubstitution.UserSubstitutionManager;
import io.jmix.data.PersistenceHints;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.securitydata.entity.UserSubstitutionEntity;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.APPROVED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The acting user {@code k} holds Full access and Project scope, so only the row-level role narrows what they read
 * and change. The fixture is five projects, each with a task, a tag group with a tag, and an entry of {@code other},
 * a Member of every one: {@code k} is a Manager of P1, a Member of P2, an Approver of P3, and an Observer of P4, with
 * an entry on each, and has no part in P5. The entries have no tags. Saves go through the constrained
 * {@code DataManager}.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class ProjectScopeRoleTest {

    static final String USERNAME_PREFIX = "project-scope-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    AccessManager accessManager;
    @Autowired
    Metadata metadata;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserSubstitutionManager userSubstitutionManager;
    @Autowired
    EntityStates entityStates;

    ApprovalTestData data;
    // New entities that a test expects to be refused, removed should a save let one through
    List<Id<?>> attemptedIds = new ArrayList<>();
    LocalDate today;

    Client client;
    TaskType taskType;
    ProjectRole managerRole;
    ProjectRole approverRole;
    ProjectRole memberRole;
    ProjectRole observerRole;

    User k;
    User other;

    ProjectData p1;
    ProjectData p2;
    ProjectData p3;
    ProjectData p4;
    ProjectData p5;

    /**
     * A project of the fixture and what it holds; {@code kOnIt} and {@code kEntry} are null where {@code k} takes no
     * part.
     */
    record ProjectData(Project project, Task task, TagGroup tagGroup, Tag tag, ProjectParticipant otherOnIt,
                       @Nullable ProjectParticipant kOnIt, TimeEntry otherEntry, @Nullable TimeEntry kEntry) {
    }

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));
        today = LocalDate.now();

        k = saveScopeHolder("k");
        other = data.saveUser("other", "Other", "Member");

        client = data.saveClient();
        taskType = data.saveTaskType();
        managerRole = data.saveRole(ProjectRoleType.MANAGER);
        approverRole = data.saveRole(ProjectRoleType.APPROVER);
        memberRole = data.saveRole(ProjectRoleType.MEMBER);
        observerRole = data.saveRole(ProjectRoleType.OBSERVER);

        p1 = saveProject("P1", managerRole);
        p2 = saveProject("P2", memberRole);
        p3 = saveProject("P3", approverRole);
        p4 = saveProject("P4", observerRole);
        p5 = saveProject("P5", null);
    }

    @Test
    void readsProjectDataOnlyWhereTakingPart() {
        List<ProjectData> all = List.of(p1, p2, p3, p4, p5);
        List<ProjectData> takingPart = List.of(p1, p2, p3, p4);

        as(k, () -> {
            assertThat(loadIds(Project.class, idsOf(all, ProjectData::project)))
                    .containsExactlyInAnyOrderElementsOf(idsOf(takingPart, ProjectData::project));
            assertThat(loadIds(Task.class, idsOf(all, ProjectData::task)))
                    .containsExactlyInAnyOrderElementsOf(idsOf(takingPart, ProjectData::task));
            assertThat(loadIds(TagGroup.class, idsOf(all, ProjectData::tagGroup)))
                    .containsExactlyInAnyOrderElementsOf(idsOf(takingPart, ProjectData::tagGroup));
            assertThat(loadIds(Tag.class, idsOf(all, ProjectData::tag)))
                    .containsExactlyInAnyOrderElementsOf(idsOf(takingPart, ProjectData::tag));
            // Every participant of a project where k takes part, k's own rows and the other member's
            assertThat(loadIds(ProjectParticipant.class,
                    concat(idsOf(all, ProjectData::otherOnIt), idsOf(takingPart, ProjectData::kOnIt))))
                    .containsExactlyInAnyOrderElementsOf(
                            concat(idsOf(takingPart, ProjectData::otherOnIt), idsOf(takingPart, ProjectData::kOnIt)));
        });
    }

    @Test
    void readsOwnEntriesAndThoseOfManagedApprovedAndObservedProjects() {
        as(k, () -> assertThat(loadIds(TimeEntry.class, allEntryIds()))
                .containsExactlyInAnyOrderElementsOf(entriesOfK()));
    }

    @Test
    void valueLoadsAreNarrowedToo() {
        as(k, () -> {
            Long count = dataManager.loadValue("select count(e) from ts_TimeEntry e where e.id in :ids", Long.class)
                    .parameter("ids", allEntryIds())
                    .one();
            assertThat(count).isEqualTo(loadIds(TimeEntry.class, allEntryIds()).size()).isEqualTo(7L);
        });
    }

    @Test
    void changesProjectDataOnlyWhereManager() {
        User newcomer = data.saveUser("newcomer", "New", "Comer");

        as(k, () -> {
            Project project = load(p1.project());
            project.setName(project.getName() + " renamed");
            dataManager.save(project);

            Task task = data.save(newTask(p1), t -> {
            });
            task.setName(task.getName() + " renamed");
            dataManager.save(task);
            dataManager.remove(load(task));

            ProjectParticipant participant = data.save(newParticipant(p1, newcomer, memberRole), pt -> {
            });
            participant.setRole(approverRole);
            dataManager.save(participant);
            dataManager.remove(load(participant));

            TagGroup tagGroup = data.save(newTagGroup(p1), g -> {
            });
            tagGroup.setName(tagGroup.getName() + " renamed");
            dataManager.save(tagGroup);

            Tag tag = data.save(newTag(tagGroup), t -> {
            });
            tag.setName(tag.getName() + " renamed");
            dataManager.save(tag);
            dataManager.remove(load(tag));
            dataManager.remove(load(tagGroup));
        });

        assertThat(stored(p1.project()).getName()).endsWith(" renamed");

        for (ProjectData p : List.of(p3, p4)) {
            as(k, () -> {
                Project project = load(p.project());
                project.setName(project.getName() + " renamed");
                refusedToSave(project);

                refusedToSave(newTask(p));
                Task task = load(p.task());
                task.setName(task.getName() + " renamed");
                refusedToSave(task);
                refusedToRemove(load(p.task()));

                refusedToSave(newParticipant(p, newcomer, memberRole));
                ProjectParticipant participant = load(p.otherOnIt());
                participant.setRole(approverRole);
                refusedToSave(participant);
                refusedToRemove(load(p.otherOnIt()));

                refusedToSave(newTagGroup(p));
                TagGroup tagGroup = load(p.tagGroup());
                tagGroup.setName(tagGroup.getName() + " renamed");
                refusedToSave(tagGroup);
                refusedToRemove(load(p.tagGroup()));

                refusedToSave(newTag(p.tagGroup()));
                Tag tag = load(p.tag());
                tag.setName(tag.getName() + " renamed");
                refusedToSave(tag);
                refusedToRemove(load(p.tag()));
            });

            assertThat(stored(p.project()).getName()).doesNotEndWith(" renamed");
            assertThat(stored(p.task()).getName()).doesNotEndWith(" renamed");
            assertThat(stored(p.otherOnIt(), "role").getRole().getId()).isEqualTo(memberRole.getId());
            assertThat(stored(p.tagGroup()).getName()).doesNotEndWith(" renamed");
            assertThat(stored(p.tag()).getName()).doesNotEndWith(" renamed");
        }
    }

    @Test
    void movingAChildNeedsBothProjects() {
        Project p6 = data.saveProject("P6", client);
        data.saveParticipant(p6, k, managerRole);
        TagGroup tagGroupOfP6 = data.save(newTagGroup(p6), g -> {
        });

        as(k, () -> {
            refusedToSave(withProject(load(p1.task()), p3.project()));
            refusedToSave(withProject(load(p3.task()), p1.project()));
            refusedToSave(withTagGroup(load(p1.tag()), p3.tagGroup()));
            refusedToSave(withTagGroup(load(p3.tag()), p1.tagGroup()));
        });
        assertThat(stored(p1.task(), "project").getProject().getId()).isEqualTo(p1.project().getId());
        assertThat(stored(p3.task(), "project").getProject().getId()).isEqualTo(p3.project().getId());
        assertThat(stored(p1.tag(), "tagGroup").getTagGroup().getId()).isEqualTo(p1.tagGroup().getId());
        assertThat(stored(p3.tag(), "tagGroup").getTagGroup().getId()).isEqualTo(p3.tagGroup().getId());

        // A task and a tag of their own, saved after P6, so the teardown removes them before P6 and its compositions,
        // and no entry refers to them
        Task task = data.save(newTask(p1), t -> {
        });
        Tag tag = data.save(newTag(p1.tagGroup()), t -> {
        });
        as(k, () -> {
            dataManager.save(withProject(load(task), p6));
            dataManager.save(withTagGroup(load(tag), tagGroupOfP6));
        });
        assertThat(stored(task, "project").getProject().getId()).isEqualTo(p6.getId());
        assertThat(stored(tag, "tagGroup").getTagGroup().getId()).isEqualTo(tagGroupOfP6.getId());
    }

    @Test
    void includesTimeEntryRules() {
        TimeEntry approved = data.saveEntry(p1.task(), k, today, APPROVED, null);

        as(k, () -> {
            TimeEntry entry = load(approved);
            entry.setDescription("Changed after approval");
            refusedToSave(entry);
        });

        assertThat(stored(approved).getDescription()).isNull();
    }

    @Test
    void nobodyMakesThemselvesManager() {
        ProjectParticipant kOnP2 = Objects.requireNonNull(p2.kOnIt());

        as(k, () -> {
            refusedToSave(newParticipant(p2, k, managerRole));

            ProjectParticipant own = load(kOnP2);
            own.setRole(managerRole);
            refusedToSave(own);
        });

        assertThat(stored(kOnP2, "role").getRole().getId()).isEqualTo(memberRole.getId());
        assertThat(dataManager.load(ProjectParticipant.class)
                .query("e.project.id = ?1 and e.user.id = ?2", p2.project().getId(), k.getId())
                .list())
                .extracting(ProjectParticipant::getId)
                .containsExactly(kOnP2.getId());
    }

    @Test
    void ownEntryOfADeletedTaskStaysReadable() {
        TimeEntry kEntryOnP2 = Objects.requireNonNull(p2.kEntry());
        dataManager.remove(p2.task());

        // As My time entries loads them
        as(k, () -> assertThat(dataManager.load(TimeEntry.class)
                .query("e.user.username = ?1", k.getUsername())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("tags", FetchPlan.BASE))
                .list())
                .extracting(TimeEntry::getId)
                .contains(kEntryOnP2.getId()));
    }

    @Test
    void entriesOfADeletedPersonStayReadable() {
        dataManager.remove(other);

        // As the Time entries list loads them, with their user
        as(k, () -> assertThat(dataManager.load(TimeEntry.class)
                .query("e.id in ?1", allEntryIds())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE))
                .list()
                .stream()
                .map(EntityValues::getId)
                .toList())
                .containsAll(idsOf(List.of(p1, p3, p4), ProjectData::otherEntry)));
    }

    @Test
    void screensAskAboutTheProjectDataTheyShow() {
        // The project editor's question about the project, and the dialogs' about the children it loads; the tag
        // group dialog's about the tags it loads
        as(k, () -> {
            for (ProjectData p : List.of(p1, p3, p4)) {
                boolean manager = p == p1;
                Project project = loadAsTheProjectEditor(p.project());
                assertThat(updatePermitted(project)).as("project").isEqualTo(manager);
                assertThat(project.getTasks()).allSatisfy(task ->
                        assertThat(updatePermitted(task)).as("task").isEqualTo(manager));
                assertThat(project.getParticipants()).allSatisfy(participant ->
                        assertThat(updatePermitted(participant)).as("participant").isEqualTo(manager));
                assertThat(project.getTagGroups()).allSatisfy(tagGroup ->
                        assertThat(updatePermitted(tagGroup)).as("tag group").isEqualTo(manager));
                assertThat(loadAsTheTagGroupEditor(p.tagGroup()).getTags()).allSatisfy(tag ->
                        assertThat(updatePermitted(tag)).as("tag").isEqualTo(manager));
            }
        });
    }

    @Test
    void readsAsTheEffectiveUserOfASubstitution() {
        User substitute = saveScopeHolder("substitute");
        data.saveParticipant(p5.project(), substitute, memberRole);
        data.save(dataManager.create(UserSubstitutionEntity.class), substitution -> {
            substitution.setUsername(substitute.getUsername());
            substitution.setSubstitutedUsername(k.getUsername());
        });
        List<ProjectData> all = List.of(p1, p2, p3, p4, p5);

        as(substitute, () -> {
            // The substitute alone: a Member of P5 only
            assertThat(loadIds(Project.class, idsOf(all, ProjectData::project)))
                    .containsExactly(p5.project().getId());

            userSubstitutionManager.substituteUser(k.getUsername());

            assertThat(loadIds(Project.class, idsOf(all, ProjectData::project)))
                    .containsExactlyInAnyOrderElementsOf(idsOf(List.of(p1, p2, p3, p4), ProjectData::project));
            assertThat(loadIds(TimeEntry.class, allEntryIds())).containsExactlyInAnyOrderElementsOf(entriesOfK());
        });
    }

    @AfterEach
    void tearDown() {
        // The attempts come before the projects, tasks, and tag groups they refer to, the last attempted first
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

    private ProjectData saveProject(String name, @Nullable ProjectRole roleOfK) {
        Project project = data.saveProject(name, client);
        Task task = data.saveTask(project, taskType);
        TagGroup tagGroup = data.save(newTagGroup(project), g -> {
        });
        Tag tag = data.save(newTag(tagGroup), t -> {
        });
        ProjectParticipant otherOnIt = data.saveParticipant(project, other, memberRole);
        TimeEntry otherEntry = data.saveEntry(task, other, today, NEW, null);
        if (roleOfK == null) {
            return new ProjectData(project, task, tagGroup, tag, otherOnIt, null, otherEntry, null);
        }
        ProjectParticipant kOnIt = data.saveParticipant(project, k, roleOfK);
        TimeEntry kEntry = data.saveEntry(task, k, today, NEW, null);
        return new ProjectData(project, task, tagGroup, tag, otherOnIt, kOnIt, otherEntry, kEntry);
    }

    private Task newTask(ProjectData p) {
        Task task = dataManager.create(Task.class);
        task.setName(p.project().getName() + " new task");
        task.setProject(p.project());
        task.setType(taskType);
        return task;
    }

    private ProjectParticipant newParticipant(ProjectData p, User user, ProjectRole role) {
        ProjectParticipant participant = dataManager.create(ProjectParticipant.class);
        participant.setProject(p.project());
        participant.setUser(user);
        participant.setRole(role);
        return participant;
    }

    private TagGroup newTagGroup(ProjectData p) {
        return newTagGroup(p.project());
    }

    private TagGroup newTagGroup(Project project) {
        TagGroup tagGroup = dataManager.create(TagGroup.class);
        tagGroup.setName(project.getName() + " tag group");
        tagGroup.setProject(project);
        return tagGroup;
    }

    private Tag newTag(TagGroup tagGroup) {
        Tag tag = dataManager.create(Tag.class);
        tag.setName(tagGroup.getName() + " tag");
        tag.setTagGroup(tagGroup);
        return tag;
    }

    private static Task withProject(Task task, Project project) {
        task.setProject(project);
        return task;
    }

    private static Tag withTagGroup(Tag tag, TagGroup tagGroup) {
        tag.setTagGroup(tagGroup);
        return tag;
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

    private void refusedToRemove(Object entity) {
        assertRefused(() -> dataManager.remove(entity));
    }

    private static void assertRefused(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    private boolean updatePermitted(Object entity) {
        InMemoryCrudEntityContext context =
                new InMemoryCrudEntityContext(metadata.getClass(entity), applicationContext);
        accessManager.applyRegisteredConstraints(context);
        return context.isUpdatePermitted(entity);
    }

    /**
     * Loads the entity by its id with the {@code _base} fetch plan, as the acting user when called in their block.
     */
    private <E> E load(E entity) {
        return dataManager.load(Id.of(entity)).one();
    }

    /**
     * Reads the entity as stored, with the given references, as the administrator, outside the acting user's block.
     */
    private <E> E stored(E entity, String... references) {
        return dataManager.load(Id.of(entity))
                .fetchPlan(builder -> {
                    builder.addFetchPlan(FetchPlan.BASE);
                    for (String reference : references) {
                        builder.add(reference, FetchPlan.BASE);
                    }
                })
                .one();
    }

    private Project loadAsTheProjectEditor(Project project) {
        return dataManager.load(Id.of(project))
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("client", FetchPlan.BASE)
                        .add("tasks", tasks -> tasks.addFetchPlan(FetchPlan.BASE)
                                .add("type", FetchPlan.INSTANCE_NAME))
                        .add("participants", participants -> participants.addFetchPlan(FetchPlan.BASE)
                                .add("role", FetchPlan.BASE))
                        .add("tagGroups", FetchPlan.BASE))
                .one();
    }

    private TagGroup loadAsTheTagGroupEditor(TagGroup tagGroup) {
        return dataManager.load(Id.of(tagGroup))
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE)
                        .add("project", FetchPlan.BASE))
                .one();
    }

    private List<Object> loadIds(Class<?> entityClass, Collection<Object> ids) {
        return dataManager.load(entityClass)
                .query("e.id in :ids")
                .parameter("ids", ids)
                .list()
                .stream()
                .map(EntityValues::getId)
                .toList();
    }

    /**
     * The ids of the entries {@code k} reads: their own, and the other member's on P1, P3, and P4, but not on P2,
     * where {@code k} is a Member.
     */
    private List<Object> entriesOfK() {
        return concat(idsOf(List.of(p1, p2, p3, p4), ProjectData::kEntry),
                idsOf(List.of(p1, p3, p4), ProjectData::otherEntry));
    }

    private List<Object> allEntryIds() {
        List<ProjectData> all = List.of(p1, p2, p3, p4, p5);
        return concat(idsOf(all, ProjectData::kEntry), idsOf(all, ProjectData::otherEntry));
    }

    private static List<Object> idsOf(List<ProjectData> projects, Function<ProjectData, ?> entity) {
        return projects.stream()
                .map(entity)
                .filter(Objects::nonNull)
                .map(EntityValues::getId)
                .toList();
    }

    private static List<Object> concat(List<Object> first, List<Object> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    private User saveScopeHolder(String name) {
        User user = data.saveUser(name, name, name);
        assignRole(user, FullAccessRole.CODE, RoleAssignmentRoleType.RESOURCE);
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

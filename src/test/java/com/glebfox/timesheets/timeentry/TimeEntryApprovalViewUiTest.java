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
import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ApprovalPerson;
import com.glebfox.timesheets.app.util.ComponentSupport;
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
import com.glebfox.timesheets.view.timeentry.approval.ApprovalRosterCard;
import com.glebfox.timesheets.view.timeentry.approval.ApprovalRosterScope;
import com.glebfox.timesheets.view.timeentry.approval.TimeEntryApprovalView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.settings.UserSettingsService;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.tabbedmode.ViewBuilders;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fixture is one project on which admin is a Manager, with time logged in October 2026 by Ann Ash (a new entry),
 * Bob Birch (an approved one), and Cid Cole (a new and a rejected one).
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryApprovalViewUiTest {

    static final String ADMIN = "admin";
    static final String MODE_SETTING = "ts_TimeEntry.approval.mode";
    // October 2026 starts on a Thursday, so its weeks reach into the neighbouring months in either week order
    static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserSettingsService userSettingsService;
    @Autowired
    ComponentSupport componentSupport;
    @Autowired
    Messages messages;

    String prefix;
    Client client;
    TaskType taskType;
    ProjectRole managerRole;
    ProjectRole memberRole;
    Project project;
    Task task;
    ProjectParticipant adminOnProject;
    User ann;
    User bob;
    User cid;

    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        prefix = "approval-ui-" + UUID.randomUUID().toString().substring(0, 8);

        client = save(dataManager.create(Client.class), c -> c.setName(prefix + " client"));
        taskType = save(dataManager.create(TaskType.class), t -> t.setName(prefix + " task type"));
        managerRole = saveRole(ProjectRoleType.MANAGER);
        memberRole = saveRole(ProjectRoleType.MEMBER);

        project = saveProject("P");
        task = saveTask(project);
        adminOnProject = saveParticipant(project, loadAdmin(), managerRole);

        ann = saveUser("ann", "Ann", "Ash");
        bob = saveUser("bob", "Bob", "Birch");
        cid = saveUser("cid", "Cid", "Cole");
        saveParticipant(project, ann, memberRole);
        saveParticipant(project, bob, memberRole);
        saveParticipant(project, cid, memberRole);

        saveEntry(task, ann, OCTOBER.atDay(5), TimeEntryStatus.NEW);
        saveEntry(task, bob, OCTOBER.atDay(6), TimeEntryStatus.APPROVED);
        saveEntry(task, cid, OCTOBER.atDay(7), TimeEntryStatus.NEW);
        saveEntry(task, cid, OCTOBER.atDay(8), TimeEntryStatus.REJECTED);
    }

    @Test
    void listsPeopleWithWaitingEntriesFirst() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();

            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);

            assertThat(rosterOf(view)).containsExactly(ann.getUsername(), cid.getUsername(), bob.getUsername());
        });
    }

    @Test
    void needsReviewHidesPeopleWithNothingWaiting() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();

            assertThat(scopeField(view).getValue()).isEqualTo(ApprovalRosterScope.NEEDS_REVIEW);
            assertThat(rosterOf(view)).containsExactly(ann.getUsername(), cid.getUsername());
            // The first shown person is selected when the period changes
            assertThat(selectedOf(view)).isEqualTo(ann.getUsername());
            assertThat(emptyStateBox(view).isVisible()).isFalse();
        });
    }

    @Test
    void searchFindsAPersonByName() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            TypedTextField<String> searchField = UiTestUtils.getComponent(view, "rosterSearchField");

            searchField.setValue("ci");
            assertThat(rosterOf(view)).containsExactly(cid.getUsername());

            // The last name and the username match too, whatever the case
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            searchField.setValue("BIRCH");
            assertThat(rosterOf(view)).containsExactly(bob.getUsername());
            searchField.setValue(ann.getUsername().toUpperCase());
            assertThat(rosterOf(view)).containsExactly(ann.getUsername());
        });
    }

    @Test
    void stepsThroughMonthsAndBack() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            JmixButton previousBtn = UiTestUtils.getComponent(view, "previousBtn");
            JmixButton nextBtn = UiTestUtils.getComponent(view, "nextBtn");
            JmixButton defaultPeriodBtn = UiTestUtils.getComponent(view, "defaultPeriodBtn");

            nextBtn.click();
            assertThat(periodTitleOf(view)).isEqualTo("November 2026");
            assertThat(view.getRosterPeople()).isEmpty();
            assertThat(view.getSelectedPerson()).isNull();
            assertThat(emptyStateBox(view).isVisible()).isTrue();
            assertThat(emptyStateOf(view)).containsExactly("No time logged",
                    "Nobody has logged time in this period on the projects you approve.");

            previousBtn.click();
            assertThat(periodTitleOf(view)).isEqualTo("October 2026");
            assertThat(rosterOf(view)).containsExactly(ann.getUsername(), cid.getUsername());
            assertThat(emptyStateBox(view).isVisible()).isFalse();

            // September is never the current month again, so the button that returns to it is enabled
            previousBtn.click();
            assertThat(defaultPeriodBtn.getText()).isEqualTo("This month");
            assertThat(defaultPeriodBtn.isEnabled()).isTrue();
            defaultPeriodBtn.click();
            assertThat(view.getPeriod()).isEqualTo(ApprovalPeriod.monthOf(YearMonth.from(LocalDate.now())));
            assertThat(defaultPeriodBtn.isEnabled()).isFalse();
        });
    }

    @Test
    void weekModeOpensOnLastWeekAndIsRemembered() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            DayOfWeek firstDayOfWeek = componentSupport.getDaysOfWeek().getFirst();
            ApprovalPeriod lastWeek = ApprovalPeriod.defaultOf(ApprovalMode.WEEK, LocalDate.now(), firstDayOfWeek);

            modeField(view).setValue(ApprovalMode.WEEK);
            assertThat(view.getPeriod()).isEqualTo(lastWeek);
            JmixButton defaultPeriodBtn = UiTestUtils.getComponent(view, "defaultPeriodBtn");
            assertThat(defaultPeriodBtn.getText()).isEqualTo("Last week");

            view.close(StandardOutcome.CLOSE);
            TimeEntryApprovalView reopened = open();

            assertThat(modeField(reopened).getValue()).isEqualTo(ApprovalMode.WEEK);
            assertThat(reopened.getPeriod()).isEqualTo(lastWeek);
        });
    }

    @Test
    void projectChoiceNarrowsTheRoster() {
        Project other = saveProject("Q");
        Task otherTask = saveTask(other);
        saveParticipant(other, loadAdmin(), managerRole);
        User dan = saveUser("dan", "Dan", "Dale");
        saveEntry(otherTask, dan, OCTOBER.atDay(9), TimeEntryStatus.NEW);

        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            assertThat(rosterOf(view)).containsExactly(ann.getUsername(), cid.getUsername(), dan.getUsername());

            JmixComboBox<Project> projectField = UiTestUtils.getComponent(view, "projectField");
            Project projectItem = projectField.getListDataView().getItems()
                    .filter(item -> item.getId().equals(project.getId()))
                    .findFirst()
                    .orElseThrow();
            projectField.setValue(projectItem);

            assertThat(rosterOf(view)).containsExactly(ann.getUsername(), cid.getUsername());

            projectField.clear();
            assertThat(rosterOf(view)).containsExactly(ann.getUsername(), cid.getUsername(), dan.getUsername());
        });
    }

    @Test
    void teamProgressCountsFullyApprovedPeople() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            Span teamProgressText = UiTestUtils.getComponent(view, "teamProgressText");
            Span teamProgressDetails = UiTestUtils.getComponent(view, "teamProgressDetails");

            // Only Bob has nothing new or rejected
            assertThat(teamProgressText.getText()).isEqualTo("1 of 3 people fully approved");
            assertThat(teamProgressDetails.getText()).isEqualTo("2 entries waiting · 1 rejected");

            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            assertThat(teamProgressText.getText()).isEqualTo("1 of 3 people fully approved");

            TypedTextField<String> searchField = UiTestUtils.getComponent(view, "rosterSearchField");
            searchField.setValue("ann");
            assertThat(rosterOf(view)).containsExactly(ann.getUsername());
            assertThat(teamProgressText.getText()).isEqualTo("1 of 3 people fully approved");
        });
    }

    @Test
    void aSelectionHiddenByScopeMovesToTheFirstShownPerson() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();

            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            view.selectPerson(bob);
            assertThat(selectedOf(view)).isEqualTo(bob.getUsername());

            scopeField(view).setValue(ApprovalRosterScope.NEEDS_REVIEW);
            assertThat(selectedOf(view)).isEqualTo(ann.getUsername());

            // A selection that stays shown stays selected, after a reload too
            view.selectPerson(cid);
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            assertThat(selectedOf(view)).isEqualTo(cid.getUsername());
            view.reload();
            assertThat(selectedOf(view)).isEqualTo(cid.getUsername());
        });
    }

    @Test
    void rosterCardShowsWhatWaitsWhatWasRejectedAndWhatIsApproved() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);

            // Cid has a new and a rejected entry: the rejected count beside the waiting one is a bare number
            // that names itself to assistive technology
            ApprovalRosterCard cidCard = cardOf(view, cid, List.of(memberRole.getName()));
            List<Badge> cidBadges = badgesOf(cidCard);
            assertThat(cidBadges).extracting(Badge::getText).containsExactly("1 waiting", "1");
            assertThat(cidBadges.get(1).getElement().getAttribute("aria-label")).isEqualTo("1 rejected");
            assertThat(textOf(cidCard, "name")).isEqualTo("Cid Cole");
            assertThat(textOf(cidCard, "role")).isEqualTo(memberRole.getName());
            assertThat(textOf(cidCard, "logged")).isEqualTo("8h logged");
            assertThat(partOf(cidCard, "fingerprint")).isInstanceOf(Svg.class);

            // Bob has only an approved entry; a card given no role names has no role line
            ApprovalRosterCard bobCard = cardOf(view, bob, List.of());
            assertThat(badgesOf(bobCard)).extracting(Badge::getText).containsExactly("✓ Approved");
            assertThat(partsOf(bobCard)).doesNotContain("role");
            assertThat(textOf(bobCard, "logged")).isEqualTo("4h logged");

            // The mark of the selected card
            assertThat(bobCard.getContent().hasClassName("selected")).isFalse();
            bobCard.setSelected(true);
            assertThat(bobCard.getContent().hasClassName("selected")).isTrue();
            assertThat(bobCard.getElement().getAttribute("aria-pressed")).isEqualTo("true");
        });
    }

    @Test
    void userWhoApprovesNowhereSeesTheEmptyState() {
        removeForGood(Id.of(adminOnProject));

        asAdmin(() -> {
            TimeEntryApprovalView view = open();
            assertThat(emptyStateBox(view).isVisible()).isTrue();
            assertThat(emptyStateOf(view)).containsExactly("You approve time on no project",
                    "Time is approved by the managers and approvers of a project. Once you have one of these roles "
                            + "on a project, the time logged on it shows here.");
            assertThat(view.getRosterPeople()).isEmpty();
            // Nobody to look for and no project to choose
            assertThat(UiTestUtils.<Div>getComponent(view, "rosterBox").isVisible()).isFalse();
            assertThat(UiTestUtils.<JmixComboBox<Project>>getComponent(view, "projectField").isVisible()).isFalse();

            view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
            assertThat(emptyStateBox(view).isVisible()).isTrue();
            assertThat(view.getRosterPeople()).isEmpty();
            assertThat(view.getSelectedPerson()).isNull();
        });
    }

    @AfterEach
    void tearDown() {
        try {
            // Soft-deleted rows are removed for good, children before parents
            for (Id<?> id : createdIds.reversed()) {
                removeForGood(id);
            }
            createdIds.clear();
        } finally {
            // The screen opens in Month again for the next test, whatever this one switched or how it ended
            systemAuthenticator.runWithUser(ADMIN, () -> userSettingsService.delete(MODE_SETTING));
        }
    }

    private void asAdmin(Runnable action) {
        systemAuthenticator.runWithUser(ADMIN, action);
    }

    private TimeEntryApprovalView open() {
        viewBuilders.view(UiTestUtils.getCurrentView(), TimeEntryApprovalView.class).open();
        return UiTestUtils.getCurrentView();
    }

    private TimeEntryApprovalView openInOctober() {
        TimeEntryApprovalView view = open();
        view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
        return view;
    }

    private static JmixRadioButtonGroup<ApprovalMode> modeField(TimeEntryApprovalView view) {
        return UiTestUtils.getComponent(view, "modeField");
    }

    private static JmixRadioButtonGroup<ApprovalRosterScope> scopeField(TimeEntryApprovalView view) {
        return UiTestUtils.getComponent(view, "rosterScopeField");
    }

    private static Div emptyStateBox(TimeEntryApprovalView view) {
        return UiTestUtils.getComponent(view, "emptyStateBox");
    }

    private static List<String> emptyStateOf(TimeEntryApprovalView view) {
        H3 header = UiTestUtils.getComponent(view, "emptyStateHeader");
        Span text = UiTestUtils.getComponent(view, "emptyStateText");
        return List.of(header.getText(), text.getText());
    }

    /**
     * Builds a person's roster card as the roster draws it. Without a browser the roster requests no items, so its
     * renderer never runs in a UI test.
     */
    private ApprovalRosterCard cardOf(TimeEntryApprovalView view, User user, List<String> roleNames) {
        ApprovalPerson person = view.getRosterPeople().stream()
                .filter(shown -> shown.user().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();
        return new ApprovalRosterCard(person, roleNames, view.getPeriod(), componentSupport, messages, Locale.ENGLISH);
    }

    private static List<String> partsOf(ApprovalRosterCard card) {
        return card.getContent().getChildren()
                .flatMap(part -> part.getElement().getClassList().stream())
                .toList();
    }

    private static Component partOf(ApprovalRosterCard card, String className) {
        return card.getContent().getChildren()
                .filter(part -> part.getElement().getClassList().contains(className))
                .findFirst()
                .orElseThrow();
    }

    private static String textOf(ApprovalRosterCard card, String className) {
        return partOf(card, className).getElement().getText();
    }

    private static List<Badge> badgesOf(ApprovalRosterCard card) {
        return partOf(card, "badges").getChildren().map(Badge.class::cast).toList();
    }

    private static String periodTitleOf(TimeEntryApprovalView view) {
        H2 periodTitle = UiTestUtils.getComponent(view, "periodTitle");
        return periodTitle.getElement().getTextRecursively();
    }

    private static List<String> rosterOf(TimeEntryApprovalView view) {
        return view.getRosterPeople().stream().map(ApprovalPerson::user).map(User::getUsername).toList();
    }

    private static String selectedOf(TimeEntryApprovalView view) {
        User selected = view.getSelectedPerson();
        return selected != null ? selected.getUsername() : null;
    }

    /**
     * Removes the entity, reloaded by id with soft deletion off, for good; an entity already gone is skipped.
     */
    private void removeForGood(Id<?> id) {
        dataManager.load(id)
                .hint(PersistenceHints.SOFT_DELETION, false)
                .optional()
                .ifPresent(current -> dataManager.save(new SaveContext()
                        .removing(current)
                        .setHint(PersistenceHints.SOFT_DELETION, false)));
    }

    private <E> E save(E entity, Consumer<E> init) {
        init.accept(entity);
        E saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private ProjectRole saveRole(ProjectRoleType type) {
        return save(dataManager.create(ProjectRole.class), r -> {
            r.setName(prefix + " " + type.getId() + " role");
            r.setType(type);
        });
    }

    private Project saveProject(String name) {
        return save(dataManager.create(Project.class), p -> {
            p.setName(prefix + " " + name);
            p.setClient(client);
        });
    }

    private Task saveTask(Project taskProject) {
        return save(dataManager.create(Task.class), t -> {
            t.setName(prefix + " " + taskProject.getName() + " task");
            t.setProject(taskProject);
            t.setType(taskType);
        });
    }

    private User saveUser(String name, String firstName, String lastName) {
        return save(dataManager.create(User.class), u -> {
            u.setUsername(prefix + "-" + name);
            u.setFirstName(firstName);
            u.setLastName(lastName);
        });
    }

    private ProjectParticipant saveParticipant(Project participantProject, User user, ProjectRole role) {
        return save(dataManager.create(ProjectParticipant.class), p -> {
            p.setProject(participantProject);
            p.setUser(user);
            p.setRole(role);
        });
    }

    private void saveEntry(Task entryTask, User user, LocalDate date, TimeEntryStatus status) {
        save(dataManager.create(TimeEntry.class), e -> {
            e.setTask(entryTask);
            e.setUser(user);
            e.setDate(date);
            e.setSpentTime(WorkTime.ofHours(4));
            e.setStatus(status);
        });
    }

    private User loadAdmin() {
        return dataManager.load(User.class)
                .query("e.username = ?1", ADMIN)
                .one();
    }
}

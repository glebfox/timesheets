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
import com.glebfox.timesheets.component.approval.ApprovalEntryCard;
import com.glebfox.timesheets.component.approval.ApprovalRosterCard;
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
import com.glebfox.timesheets.view.approval.ApprovalRosterScope;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.dom.Element;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.UiComponentUtils;
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
import org.jspecify.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fixture is one project on which admin is a Manager, with time logged in October 2026 by Ann Ash (a new entry),
 * Bob Birch (an approved one), and Cid Cole (a rejected one and, a week later, a new one), and by Cid on the last day
 * of September (a new one). Each entry is four hours.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryApprovalViewUiTest {

    static final String ADMIN = "admin";
    static final String MODE_SETTING = "ts_TimeEntry.approval.mode";
    // October 2026 starts on a Thursday, so its weeks reach into the neighbouring months in either week order
    static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    static final Locale RUSSIAN = Locale.of("ru", "RU");

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
    UiComponents uiComponents;

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
    TimeEntry annWaiting;
    TimeEntry bobApproved;
    TimeEntry cidRejected;
    TimeEntry cidWaiting;
    TimeEntry cidInSeptember;

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

        // Two weeks of October in either week order: Ann's, Bob's, and Cid's rejected entry in one, Cid's new one
        // in the next
        annWaiting = saveEntry(task, ann, OCTOBER.atDay(5), TimeEntryStatus.NEW);
        bobApproved = saveEntry(task, bob, OCTOBER.atDay(6), TimeEntryStatus.APPROVED);
        cidRejected = saveEntry(task, cid, OCTOBER.atDay(8), TimeEntryStatus.REJECTED);
        cidWaiting = saveEntry(task, cid, OCTOBER.atDay(14), TimeEntryStatus.NEW);
        // In the first row of October's grid whether weeks start on Sunday or on Monday
        cidInSeptember = saveEntry(task, cid, LocalDate.of(2026, 9, 30), TimeEntryStatus.NEW);
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

            // The full name, as the card shows it, matches across both names, whatever the case
            searchField.setValue("Bob Birch");
            assertThat(rosterOf(view)).containsExactly(bob.getUsername());
            searchField.setValue("cid cole");
            assertThat(rosterOf(view)).containsExactly(cid.getUsername());
            searchField.setValue("Ann Birch");
            assertThat(rosterOf(view)).isEmpty();
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
    void opensWithTheFocusInTheRosterSearch() {
        asAdmin(() -> {
            TimeEntryApprovalView view = open();

            // Not on the first button of the toolbar
            TypedTextField<String> searchField = UiTestUtils.getComponent(view, "rosterSearchField");
            JmixButton previousBtn = UiTestUtils.getComponent(view, "previousBtn");
            assertThat(searchField.getElement().getProperty("autofocus", false)).isTrue();
            assertThat(previousBtn.getElement().getProperty("autofocus", false)).isFalse();
        });
    }

    @Test
    void rosterScrollsToASelectionThatNoClickMade() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            TypedTextField<String> searchField = UiTestUtils.getComponent(view, "rosterSearchField");
            Component rosterList = UiTestUtils.getComponent(view, "rosterList");
            rosterScrollsOf(rosterList);

            // A click selects a card in view, so the roster stays where it is
            view.selectPerson(bob);
            assertThat(rosterOf(view)).containsExactly(ann.getUsername(), cid.getUsername(), bob.getUsername());
            assertThat(rosterScrollsOf(rosterList)).isEmpty();

            // The search moves the selected person's card, and the roster follows it, there and back
            searchField.setValue("birch");
            assertThat(rosterScrollsOf(rosterList)).containsExactly(0);
            searchField.clear();
            assertThat(rosterScrollsOf(rosterList)).containsExactly(2);

            // A reload that leaves the card where it was keeps the roster's scroll
            view.reload();
            assertThat(selectedOf(view)).isEqualTo(bob.getUsername());
            assertThat(rosterScrollsOf(rosterList)).isEmpty();

            // Another period selects the first person, and the roster shows their card
            view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
            assertThat(selectedOf(view)).isEqualTo(ann.getUsername());
            assertThat(rosterScrollsOf(rosterList)).containsExactly(0);
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

    @Test
    void selectingAPersonShowsTheirFigures() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();

            view.selectPerson(cid);

            Component header = descendantOf(personBox(view), "header");
            assertThat(textOf(descendantOf(header, "name"))).isEqualTo("Cid Cole");
            assertThat(textOf(descendantOf(header, "details")))
                    .contains(memberRole.getName(), project.getName());

            assertThat(figureOf(view, "logged")).containsExactly("Logged", "8h", "2 entries");
            assertThat(figureOf(view, "waiting")).containsExactly("Waiting", "1", "entry · 4h");
            assertThat(figureOf(view, "approved")).containsExactly("Approved", "0");
            assertThat(figureOf(view, "rejected")).containsExactly("Rejected", "1", "entry · 4h");
            // The bar of the Logged figure is split by status, settled time first
            assertThat(statusBarOf(UiTestUtils.getComponent(view, "figure-logged")))
                    .containsExactly("new", "rejected");
        });
    }

    @Test
    void monthOpensOnTheFirstWeekWithWaitingEntries() {
        // So that the week that waits is neither Cid's first nor his last week with entries
        saveEntry(task, cid, OCTOBER.atDay(21), TimeEntryStatus.APPROVED);

        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);

            assertThat(selectedOf(view)).isEqualTo(ann.getUsername());
            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(annWaiting));
            assertThat(listedOf(view)).containsExactly(annWaiting.getId());

            // Cid's rejected entry is a week before the one that waits, an approved one a week after
            view.selectPerson(cid);
            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(cidWaiting));
            assertThat(listedOf(view)).containsExactly(cidWaiting.getId());
            assertThat(classesOf(weekCardOf(view, weekStartOf(cidWaiting)))).contains("selected");

            // With nothing waiting, the last week with entries
            view.selectPerson(bob);
            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(bobApproved));
            assertThat(listedOf(view)).containsExactly(bobApproved.getId());

            // A reload keeps the week shown
            view.selectPerson(cid);
            view.selectWeek(weekStartOf(cidRejected));
            view.reload();
            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(cidRejected));
            assertThat(listedOf(view)).containsExactly(cidRejected.getId());
        });
    }

    @Test
    void clickingAWeekCardShowsThatWeek() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            view.selectPerson(cid);

            Component rejectedWeek = weekCardOf(view, weekStartOf(cidRejected));
            click(weekSelectOf(rejectedWeek));

            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(cidRejected));
            assertThat(listedOf(view)).containsExactly(cidRejected.getId());
            assertThat(classesOf(rejectedWeek)).contains("selected");
            assertThat(weekSelectOf(rejectedWeek).getElement().getAttribute("aria-pressed")).isEqualTo("true");
            assertThat(classesOf(weekCardOf(view, weekStartOf(cidWaiting)))).doesNotContain("selected");
            assertThat(weekSelectOf(weekCardOf(view, weekStartOf(cidWaiting))).getElement()
                    .getAttribute("aria-pressed")).isEqualTo("false");
            // The button names the week with what it shows
            assertThat(weekSelectOf(rejectedWeek).getElement().getAttribute("aria-label"))
                    .endsWith(": 4h, 1 entry, 1 rejected");

            // The week's actions sit beside the button, not in it, so a control there neither is named by the button
            // nor selects the week when clicked or pressed
            assertThat(rejectedWeek.getElement().getAttribute("role")).isNull();
            assertThat(weekSelectOf(rejectedWeek).getElement().getAttribute("role")).isEqualTo("button");
            assertThat(descendantOf(rejectedWeek, "actions").getParent()).contains(rejectedWeek);
            // The days of the week are marked too
            assertThat(classesOf(dayOf(view, cidRejected.getDate()))).contains("selected");
            assertThat(classesOf(dayOf(view, cidWaiting.getDate()))).doesNotContain("selected");
        });
    }

    @Test
    void clickingADayShowsItsWeekWithoutScrolling() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            view.selectPerson(cid);
            view.selectWeek(weekStartOf(cidRejected));
            allBrowserCalls();

            click(dayOf(view, cidWaiting.getDate()));

            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(cidWaiting));
            assertThat(listedOf(view)).containsExactly(cidWaiting.getId());
            // Nothing scrolls to the day's entries
            assertThat(allBrowserCalls()).noneMatch(call -> call.contains("scrollIntoView"));
            Component day = entryCardOf(view, cidWaiting).getParent().orElseThrow();
            assertThat(headingOf(descendantOf(day, "day-header"))).isEqualTo("Wed, Oct 14 · 4h");
        });
    }

    @Test
    void monthCountsOnlyItsOwnDays() {
        LocalDate september30 = cidInSeptember.getDate();

        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            view.selectPerson(cid);
            LocalDate firstRow = weekStartOf(OCTOBER.atDay(1));

            // September 30 is in October's first row, dimmed and empty
            Component september30Cell = dayOf(view, september30);
            assertThat(classesOf(september30Cell)).contains("outside");
            assertThat(findDescendant(september30Cell, "hours")).isEmpty();
            assertThat(textOf(descendantOf(weekCardOf(view, firstRow), "hours"))).isEqualTo("0h");
            assertThat(figureOf(view, "logged")).containsExactly("Logged", "8h", "2 entries");
            assertThat(figureOf(view, "waiting")).containsExactly("Waiting", "1", "entry · 4h");
            assertThat(fingerprintBarsOf(view, cid)).isEqualTo(2);
            view.selectWeek(firstRow);
            assertThat(listedOf(view)).isEmpty();
            // Only the row's days of October are marked as the selected week
            assertThat(classesOf(september30Cell)).doesNotContain("selected");
            assertThat(classesOf(dayOf(view, OCTOBER.atDay(1)))).contains("selected");

            // In September it counts, and October's days of the last row show nothing
            view.setPeriod(ApprovalPeriod.monthOf(YearMonth.of(2026, 9)));
            assertThat(selectedOf(view)).isEqualTo(cid.getUsername());
            LocalDate lastRow = weekStartOf(cidInSeptember);
            assertThat(view.getSelectedWeek()).isEqualTo(lastRow);
            assertThat(textOf(descendantOf(dayOf(view, september30), "hours"))).isEqualTo("4h");
            assertThat(textOf(descendantOf(weekCardOf(view, lastRow), "hours"))).isEqualTo("4h");
            assertThat(figureOf(view, "logged")).containsExactly("Logged", "4h", "1 entry");
            assertThat(listedOf(view)).containsExactly(cidInSeptember.getId());
            assertThat(fingerprintBarsOf(view, cid)).isEqualTo(1);
            assertThat(classesOf(dayOf(view, OCTOBER.atDay(1)))).contains("outside");
            assertThat(findDescendant(dayOf(view, OCTOBER.atDay(1)), "hours")).isEmpty();
        });
    }

    @Test
    void weekModeShowsSevenColumnsInTheLocaleOrder() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            modeField(view).setValue(ApprovalMode.WEEK);
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            view.setPeriod(ApprovalPeriod.weekOf(cidRejected.getDate(), firstDayOfWeek()));
            view.selectPerson(cid);

            assertThat(columnDaysOf(view)).containsExactlyElementsOf(componentSupport.getDaysOfWeek());
            assertThat(columnDaysOf(view).getFirst()).isEqualTo(DayOfWeek.SUNDAY);
            Component column = UiTestUtils.getComponent(view, "column-" + cidRejected.getDate());
            assertThat(textOf(descendantOf(column, "day"))).isEqualTo("Thu 8");
            assertThat(textOf(descendantOf(column, "hours"))).isEqualTo("4h");
            assertThat(listedOf(view)).containsExactly(cidRejected.getId());
            assertThat(entryCardOf(view, cidRejected).getParent().flatMap(Component::getParent))
                    .contains(column);
            assertThat(view.getSelectedWeek()).isNull();
            assertThat(figureOf(view, "logged")).containsExactly("Logged", "4h", "1 entry");
        });

        // A week starts on Monday in Russian
        asAdmin(() -> {
            ((AbstractAuthenticationToken) Objects.requireNonNull(SecurityContextHelper.getAuthentication()))
                    .setDetails(ClientDetails.builder().locale(RUSSIAN).build());
            TimeEntryApprovalView view = open();
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            view.setPeriod(ApprovalPeriod.weekOf(cidRejected.getDate(), DayOfWeek.MONDAY));
            view.selectPerson(cid);

            assertThat(columnDaysOf(view)).containsExactly(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            Component monday = UiTestUtils.getComponent(view, "column-" + OCTOBER.atDay(5));
            assertThat(textOf(descendantOf(monday, "day"))).isEqualTo("Пн 5");
        });
    }

    @Test
    void closedEntriesHaveNoButtons() {
        TimeEntry annClosed = saveEntry(task, ann, OCTOBER.atDay(6), TimeEntryStatus.CLOSED);

        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);

            assertThat(listedOf(view)).containsExactly(annWaiting.getId(), annClosed.getId());
            assertThat(buttonsOf(view, annClosed)).isEmpty();
            assertThat(classesOf(entryCardOf(view, annClosed))).contains("approval-entry-card", "closed");

            // ✓ approves a new or a rejected entry, ✕ rejects a new, an approved, or a rejected one
            assertThat(buttonsOf(view, annWaiting)).containsExactly("approve", "reject");
            view.selectPerson(bob);
            assertThat(buttonsOf(view, bobApproved)).containsExactly("reject");
            assertThat(classesOf(entryCardOf(view, bobApproved))).contains("approved");
            view.selectPerson(cid);
            view.selectWeek(weekStartOf(cidRejected));
            assertThat(buttonsOf(view, cidRejected)).containsExactly("approve", "reject");
        });
    }

    @Test
    void anEntryShowsItsTaskDescriptionAndRejectionReason() {
        TimeEntry reasoned = save(dataManager.create(TimeEntry.class), e -> {
            e.setTask(task);
            e.setUser(cid);
            e.setDate(OCTOBER.atDay(9));
            e.setSpentTime(WorkTime.of(2, 30));
            e.setStatus(TimeEntryStatus.REJECTED);
            e.setDescription("Calibrated the **spectrometer**");
            e.setRejectionReason("Say which sample.");
        });

        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            view.selectPerson(cid);
            view.selectWeek(weekStartOf(reasoned));

            Component card = entryCardOf(view, reasoned);
            assertThat(textOf(descendantOf(card, "time"))).isEqualTo("2h 30m");
            assertThat(textOf(descendantOf(card, "task"))).contains(task.getName(), project.getName(), "Rejected");
            assertThat(markdownOf(descendantOf(card, "description"))).isEqualTo("Calibrated the **spectrometer**");
            assertThat(markdownOf(descendantOf(card, "reason"))).isEqualTo("Say which sample.");

            // A new entry has neither a status badge nor a reason
            view.selectWeek(weekStartOf(cidWaiting));
            Component waitingCard = entryCardOf(view, cidWaiting);
            assertThat(textOf(descendantOf(waitingCard, "task"))).doesNotContain("New");
            assertThat(findDescendant(waitingCard, "reason")).isEmpty();
            assertThat(findDescendant(waitingCard, "description")).isEmpty();
        });
    }

    @Test
    void onlyTheWeekBoardShowsTheWholeDescriptionOnHover() {
        String description = "Checked:\n\n- the **pump**\n- the valves";
        TimeEntry described = save(dataManager.create(TimeEntry.class), e -> {
            e.setTask(task);
            e.setUser(cid);
            e.setDate(OCTOBER.atDay(9));
            e.setSpentTime(WorkTime.of(1, 0));
            e.setStatus(TimeEntryStatus.NEW);
            e.setDescription(description);
        });

        asAdmin(() -> {
            TimeEntryApprovalView view = openInOctober();
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            view.selectPerson(cid);
            view.selectWeek(weekStartOf(described));

            // A card of the month's week shows the whole description
            assertThat(tooltipOf(descendantOf(entryCardOf(view, described), "description"))).isNull();

            // The week board cuts it short, so the whole text shows on hover
            modeField(view).setValue(ApprovalMode.WEEK);
            view.setPeriod(ApprovalPeriod.weekOf(described.getDate(), firstDayOfWeek()));
            view.selectPerson(cid);
            Component weekBoardDescription = descendantOf(entryCardOf(view, described), "description");
            assertThat(tooltipOf(weekBoardDescription)).isEqualTo(description);
            // Beside the card rather than below it, where the card's buttons may be
            assertThat(Tooltip.forComponent(weekBoardDescription).getPosition())
                    .isEqualTo(Tooltip.TooltipPosition.END_TOP);
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
        ApprovalRosterCard card = uiComponents.create(ApprovalRosterCard.class);
        card.setPerson(person, roleNames, view.getPeriod());
        return card;
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

    private DayOfWeek firstDayOfWeek() {
        return componentSupport.getDaysOfWeek().getFirst();
    }

    private LocalDate weekStartOf(TimeEntry entry) {
        return weekStartOf(entry.getDate());
    }

    private LocalDate weekStartOf(LocalDate date) {
        return ApprovalPeriod.weekStartOf(date, firstDayOfWeek());
    }

    private static Div personBox(TimeEntryApprovalView view) {
        return UiTestUtils.getComponent(view, "personBox");
    }

    private static Component dayOf(TimeEntryApprovalView view, LocalDate day) {
        return UiTestUtils.getComponent(view, "day-" + day);
    }

    private static Component weekCardOf(TimeEntryApprovalView view, LocalDate weekStart) {
        return UiTestUtils.getComponent(view, "week-" + weekStart);
    }

    /**
     * Returns the part of a week card that selects the week.
     */
    private static Component weekSelectOf(Component weekCard) {
        return descendantOf(weekCard, "summary");
    }

    private static ApprovalEntryCard entryCardOf(TimeEntryApprovalView view, TimeEntry entry) {
        return UiTestUtils.getComponent(view, "entry-" + entry.getId());
    }

    /**
     * Lists the entries the selected person's area shows, in order: the selected week's in Month mode, the week's in
     * Week mode.
     */
    private static List<UUID> listedOf(TimeEntryApprovalView view) {
        return descendantsOf(personBox(view))
                .filter(ApprovalEntryCard.class::isInstance)
                .map(card -> ((ApprovalEntryCard) card).getEntry().getId())
                .toList();
    }

    private static List<String> buttonsOf(TimeEntryApprovalView view, TimeEntry entry) {
        List<String> buttons = new ArrayList<>();
        if (UiComponentUtils.findComponent(view, "approveEntryBtn-" + entry.getId()).isPresent()) {
            buttons.add("approve");
        }
        if (UiComponentUtils.findComponent(view, "rejectEntryBtn-" + entry.getId()).isPresent()) {
            buttons.add("reject");
        }
        return buttons;
    }

    /**
     * Reads a figure's label, value, and details, the parts it shows.
     */
    private static List<String> figureOf(TimeEntryApprovalView view, String name) {
        Component figure = UiTestUtils.getComponent(view, "figure-" + name);
        return Stream.of("label", "value", "details")
                .flatMap(part -> findDescendant(figure, part).stream())
                .map(TimeEntryApprovalViewUiTest::textOf)
                .toList();
    }

    private static List<String> statusBarOf(Component container) {
        // By the elements: the bar is a composite, whose own child is the element it shares with its content
        return descendantOf(container, "approval-status-bar").getElement().getChildren()
                .flatMap(segment -> segment.getClassList().stream())
                .toList();
    }

    private static List<DayOfWeek> columnDaysOf(TimeEntryApprovalView view) {
        return descendantsOf(personBox(view))
                .map(Component::getId)
                .flatMap(Optional::stream)
                .filter(id -> id.startsWith("column-"))
                .map(id -> LocalDate.parse(id.substring("column-".length())).getDayOfWeek())
                .toList();
    }

    private int fingerprintBarsOf(TimeEntryApprovalView view, User user) {
        Svg fingerprint = (Svg) partOf(cardOf(view, user, List.of()), "fingerprint");
        Document svg = Jsoup.parse(fingerprint.getElement().getProperty("innerHTML"), "", Parser.xmlParser());
        return svg.select("rect.bar").size();
    }

    private static String markdownOf(Component container) {
        return descendantsOf(container)
                .filter(Markdown.class::isInstance)
                .map(markdown -> ((Markdown) markdown).getContent())
                .findFirst()
                .orElseThrow();
    }

    /**
     * Gives the text of a component's tooltip: {@link Tooltip#forComponent} returns the tooltip the component has, or a
     * new one without text.
     */
    private static @Nullable String tooltipOf(Component component) {
        return Tooltip.forComponent(component).getText();
    }

    private static void click(Component component) {
        ComponentUtil.fireEvent(component, new ClickEvent<>(component));
    }

    private static List<String> browserCalls(Component component) {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == component.getElement().getNode())
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
    }

    /**
     * Lists the calls the server queued for the browser since the previous dump, whatever their element, and forgets
     * them.
     */
    private static List<String> allBrowserCalls() {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
    }

    /**
     * Lists the indexes the server asked the browser to scroll the roster to since the previous call, and forgets the
     * calls.
     */
    private static List<Object> rosterScrollsOf(Component rosterList) {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == rosterList.getElement().getNode())
                .filter(invocation -> invocation.getInvocation().getExpression().contains("scrollToIndex"))
                .map(invocation -> invocation.getInvocation().getParameters().getFirst())
                .toList();
    }

    private static Stream<Component> descendantsOf(Component root) {
        return root.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendantsOf(child)));
    }

    private static Optional<Component> findDescendant(Component root, String className) {
        return descendantsOf(root)
                .filter(component -> component.getElement().getClassList().contains(className))
                .findFirst();
    }

    private static Component descendantOf(Component root, String className) {
        return findDescendant(root, className).orElseThrow(() -> new AssertionError(
                "No part with the class '%s' in %s".formatted(className, root)));
    }

    private static List<String> classesOf(Component component) {
        return List.copyOf(component.getElement().getClassList());
    }

    private static String textOf(Component component) {
        return component.getElement().getTextRecursively();
    }

    /**
     * Reads what a day's header says of the day, without the buttons in its actions.
     */
    private static String headingOf(Component dayHeader) {
        return dayHeader.getElement().getChildren()
                .filter(part -> !part.getClassList().contains("actions"))
                .map(Element::getTextRecursively)
                .collect(Collectors.joining());
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

    private TimeEntry saveEntry(Task entryTask, User user, LocalDate date, TimeEntryStatus status) {
        return save(dataManager.create(TimeEntry.class), e -> {
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

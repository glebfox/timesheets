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
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.approval.ApprovalRosterScope;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.markdown.Markdown;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.settings.UserSettingsService;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import io.jmix.tabbedmode.testassist.notification.NotificationInfo;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Approving from the approval screen at its four levels: an entry, a day, a week, and a person.
 * <p>
 * The fixture is one project on which admin is a Manager, with time logged by Cid Cole in October 2026: new entries
 * on Friday the 2nd, twice on Monday the 5th, and on Tuesday the 6th, a rejected one on Thursday the 8th, a new one
 * and an approved one a week later, on the 14th and the 15th, and a new one on the last day of September. Ann Ash has a
 * new entry on the 5th, and Bob Birch an approved one on the 6th. Each entry is four hours. In either week order the
 * 5th, the 6th, and the 8th are in one week of October's grid, the 14th and the 15th in the next, and the 2nd in the
 * first one, which also holds September 30.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryApprovalActionsUiTest {

    static final String ADMIN = "admin";
    static final String MODE_SETTING = "ts_TimeEntry.approval.mode";
    static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    static final Locale RUSSIAN = Locale.of("ru", "RU");
    static final String REJECTION_REASON = "Say which sample.";

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

    ApprovalTestData data;
    User ann;
    User bob;
    User cid;
    TimeEntry annWaiting;
    TimeEntry bobApproved;
    TimeEntry cidFriday;
    TimeEntry cidMonday;
    TimeEntry cidMondayToo;
    TimeEntry cidTuesday;
    TimeEntry cidRejected;
    TimeEntry cidWednesday;
    TimeEntry cidApproved;
    TimeEntry cidInSeptember;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "approval-actions-" + UUID.randomUUID().toString().substring(0, 8));

        ProjectRole managerRole = data.saveRole(ProjectRoleType.MANAGER);
        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);
        Project project = data.saveProject("P", data.saveClient());
        Task task = data.saveTask(project, data.saveTaskType());
        data.saveParticipant(project, data.loadUser(ADMIN), managerRole);

        ann = data.saveUser("ann", "Ann", "Ash");
        bob = data.saveUser("bob", "Bob", "Birch");
        cid = data.saveUser("cid", "Cid", "Cole");
        data.saveParticipant(project, ann, memberRole);
        data.saveParticipant(project, bob, memberRole);
        data.saveParticipant(project, cid, memberRole);

        annWaiting = data.saveEntry(task, ann, OCTOBER.atDay(5), TimeEntryStatus.NEW, null);
        bobApproved = data.saveEntry(task, bob, OCTOBER.atDay(6), TimeEntryStatus.APPROVED, null);
        cidFriday = data.saveEntry(task, cid, OCTOBER.atDay(2), TimeEntryStatus.NEW, null);
        cidMonday = data.saveEntry(task, cid, OCTOBER.atDay(5), TimeEntryStatus.NEW, null);
        cidMondayToo = data.saveEntry(task, cid, OCTOBER.atDay(5), TimeEntryStatus.NEW, null);
        cidTuesday = data.saveEntry(task, cid, OCTOBER.atDay(6), TimeEntryStatus.NEW, null);
        cidRejected = data.saveEntry(task, cid, OCTOBER.atDay(8), TimeEntryStatus.REJECTED, REJECTION_REASON);
        cidWednesday = data.saveEntry(task, cid, OCTOBER.atDay(14), TimeEntryStatus.NEW, null);
        cidApproved = data.saveEntry(task, cid, OCTOBER.atDay(15), TimeEntryStatus.APPROVED, null);
        cidInSeptember = data.saveEntry(task, cid, LocalDate.of(2026, 9, 30), TimeEntryStatus.NEW, null);
    }

    @Test
    void approvingAnEntryNeedsNoConfirmation() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCid();
            view.selectWeek(weekStartOf(cidTuesday.getDate()));
            assertThat(figureOf(view, "waiting")).containsExactly("Waiting", "5", "entries · 20h");
            assertThat(figureOf(view, "approved")).containsExactly("Approved", "1", "entry · 4h");

            click(view, "approveEntryBtn-" + cidTuesday.getId());

            // At once: no question, the entry is approved, and the screen follows
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(figureOf(view, "waiting")).containsExactly("Waiting", "4", "entries · 16h");
            assertThat(figureOf(view, "approved")).containsExactly("Approved", "2", "entries · 8h");
            assertThat(UiComponentUtils.findComponent(view, "approveEntryBtn-" + cidTuesday.getId())).isEmpty();
            assertThat(UiComponentUtils.findComponent(view, "approveEntryBtn-" + cidMonday.getId())).isPresent();
            // The person and the week stay
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(cidTuesday.getDate()));
            assertSuccess("1 entry approved");
        });
    }

    @Test
    void approvingARejectedEntryTakesItsOwnButton() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCid();
            view.selectWeek(weekStartOf(cidRejected.getDate()));
            assertThat(figureOf(view, "rejected")).containsExactly("Rejected", "1", "entry · 4h");
            // A rejected entry is no part of what the person's button takes
            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Approve 5 waiting");

            click(view, "approveEntryBtn-" + cidRejected.getId());

            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            // The reason stays, as the data model says
            TimeEntry approved = reload(cidRejected);
            assertThat(approved.getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(approved.getRejectionReason()).isEqualTo(REJECTION_REASON);
            assertThat(figureOf(view, "rejected")).containsExactly("Rejected", "0");
            assertThat(figureOf(view, "approved")).containsExactly("Approved", "2", "entries · 8h");
            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Approve 5 waiting");
            assertSuccess("1 entry approved");
        });
    }

    @Test
    void approvingAPersonAsksAndLeavesRejectedEntries() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCid();
            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Approve 5 waiting");

            click(view, "approvePersonBtn");

            // The question names the count, the person, the period, and the hours; nothing is saved before the answer
            DialogInfo confirmation = confirmation();
            assertThat(confirmation.getDialog().getHeaderTitle()).isEqualTo(messages.getMessage("dialogs.Confirmation"));
            assertThat(textOf(confirmation)).isEqualTo("Approve 5 entries of Cid Cole for October 2026, 20h?");
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);

            // No leaves everything as it was
            dialogButton(confirmation, "no").click();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(UiTestUtils.getOpenedNotifications()).isEmpty();
            assertThat(statusesOfCid()).containsExactly(
                    TimeEntryStatus.NEW, TimeEntryStatus.NEW, TimeEntryStatus.NEW, TimeEntryStatus.NEW,
                    TimeEntryStatus.REJECTED, TimeEntryStatus.NEW, TimeEntryStatus.APPROVED,
                    TimeEntryStatus.NEW);

            click(view, "approvePersonBtn");
            dialogButton(confirmation(), "yes").click();

            // Every new entry of the month, and no other: not the rejected one, not another person's, and not the
            // one of September
            assertThat(statusesOfCid()).containsExactly(
                    TimeEntryStatus.APPROVED, TimeEntryStatus.APPROVED, TimeEntryStatus.APPROVED,
                    TimeEntryStatus.APPROVED, TimeEntryStatus.REJECTED, TimeEntryStatus.APPROVED,
                    TimeEntryStatus.APPROVED, TimeEntryStatus.NEW);
            assertThat(statusOf(annWaiting)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertSuccess("5 entries approved");

            // The screen follows: the figures, and no button where nothing waits
            assertThat(figureOf(view, "waiting")).containsExactly("Waiting", "0");
            assertThat(figureOf(view, "approved")).containsExactly("Approved", "6", "entries · 24h");
            assertThat(figureOf(view, "rejected")).containsExactly("Rejected", "1", "entry · 4h");
            assertThat(UiComponentUtils.findComponent(view, "approvePersonBtn")).isEmpty();
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
        });
    }

    @Test
    void approvingAWeekTakesOnlyThatWeek() {
        LocalDate week = weekStartOf(cidMonday.getDate());
        LocalDate nextWeek = weekStartOf(cidWednesday.getDate());

        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCid();
            view.selectWeek(week);

            click(view, "approveWeekBtn-" + week);

            DialogInfo confirmation = confirmation();
            assertThat(textOf(confirmation)).isEqualTo(
                    "Approve 3 entries of Cid Cole for %s, 12h?".formatted(rangeOf(week, week.plusDays(6))));
            dialogButton(confirmation, "yes").click();

            // The week's new entries, and not the rejected one of the week or the new ones of other weeks
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidMondayToo)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidRejected)).isEqualTo(TimeEntryStatus.REJECTED);
            assertThat(statusOf(cidWednesday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(statusOf(cidFriday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(statusOf(annWaiting)).isEqualTo(TimeEntryStatus.NEW);
            assertSuccess("3 entries approved");

            // The week stays listed, and the buttons follow what still waits
            assertThat(view.getSelectedWeek()).isEqualTo(week);
            assertThat(UiComponentUtils.findComponent(view, "approveWeekBtn-" + week)).isEmpty();
            assertThat(UiComponentUtils.findComponent(view, "approveWeekBtn-" + nextWeek)).isPresent();
            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Approve 2 waiting");
        });
    }

    @Test
    void aWeekAcrossTwoMonthsTakesOnlyTheMonthsDays() {
        LocalDate firstRow = weekStartOf(OCTOBER.atDay(1));
        List<LocalDate> daysOfOctober = ApprovalPeriod.monthOf(OCTOBER).weekRows(firstDayOfWeek()).getFirst().stream()
                .filter(day -> YearMonth.from(day).equals(OCTOBER))
                .toList();

        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCid();
            assertThat(view.getSelectedWeek()).isEqualTo(firstRow);
            // September 30 is in the row, and shows and counts nowhere
            assertThat(cidInSeptember.getDate()).isBefore(OCTOBER.atDay(1));
            assertThat(weekStartOf(cidInSeptember.getDate())).isEqualTo(firstRow);

            click(view, "approveWeekBtn-" + firstRow);

            DialogInfo confirmation = confirmation();
            assertThat(textOf(confirmation)).isEqualTo("Approve 1 entry of Cid Cole for %s, 4h?"
                    .formatted(rangeOf(daysOfOctober.getFirst(), daysOfOctober.getLast())));
            dialogButton(confirmation, "yes").click();

            assertThat(statusOf(cidFriday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidInSeptember)).isEqualTo(TimeEntryStatus.NEW);
            assertSuccess("1 entry approved");
        });
    }

    @Test
    void approvingADayTakesOnlyThatDay() {
        LocalDate monday = cidMonday.getDate();
        LocalDate tuesday = cidTuesday.getDate();

        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCid();
            view.selectWeek(weekStartOf(monday));

            // The day with only a rejected entry has nothing to approve, so no button
            assertThat(UiComponentUtils.findComponent(view, "approveDayBtn-" + cidRejected.getDate())).isEmpty();
            assertThat(buttonTextOf(view, "approveDayBtn-" + monday)).isEqualTo("Approve day");

            click(view, "approveDayBtn-" + monday);

            DialogInfo confirmation = confirmation();
            assertThat(textOf(confirmation)).isEqualTo("Approve 2 entries of Cid Cole for Oct 5, 8h?");
            dialogButton(confirmation, "yes").click();

            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidMondayToo)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(statusOf(annWaiting)).isEqualTo(TimeEntryStatus.NEW);
            assertSuccess("2 entries approved");

            assertThat(UiComponentUtils.findComponent(view, "approveDayBtn-" + monday)).isEmpty();
            assertThat(UiComponentUtils.findComponent(view, "approveDayBtn-" + tuesday)).isPresent();
            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(monday));
        });
    }

    @Test
    void aWeekOnTheBoardApprovesItsDaysAndItsWeek() {
        asAdmin(() -> {
            TimeEntryApprovalView view = open();
            JmixRadioButtonGroup<ApprovalMode> modeField = UiTestUtils.getComponent(view, "modeField");
            modeField.setValue(ApprovalMode.WEEK);
            ApprovalPeriod week = ApprovalPeriod.weekOf(cidMonday.getDate(), firstDayOfWeek());
            view.setPeriod(week);
            selectCid(view);

            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Approve week · 3");

            // The days on the board's column headers, which name themselves on hover too, as a narrow column shows
            // only their icon
            Button dayButton = UiTestUtils.getComponent(view, "approveDayBtn-" + cidMonday.getDate());
            assertThat(dayButton.getTooltip().getText()).isEqualTo("Approve day Oct 5");
            click(view, "approveDayBtn-" + cidMonday.getDate());
            DialogInfo day = confirmation();
            assertThat(textOf(day)).isEqualTo("Approve 2 entries of Cid Cole for Oct 5, 8h?");
            dialogButton(day, "yes").click();
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Approve week · 1");

            // The week, from the header
            click(view, "approvePersonBtn");
            DialogInfo person = confirmation();
            assertThat(textOf(person)).isEqualTo("Approve 1 entry of Cid Cole for %s %d, 4h?"
                    .formatted(rangeOf(week.start(), week.end()), OCTOBER.getYear()));
            dialogButton(person, "yes").click();

            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidRejected)).isEqualTo(TimeEntryStatus.REJECTED);
            assertThat(statusOf(cidWednesday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(UiComponentUtils.findComponent(view, "approvePersonBtn")).isEmpty();
            assertSuccess("1 entry approved");
        });
    }

    @Test
    void noButtonWhereNothingWaits() {
        asAdmin(() -> {
            TimeEntryApprovalView view = open();
            view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
            scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
            view.selectPerson(bob);

            assertThat(UiComponentUtils.findComponent(view, "approvePersonBtn")).isEmpty();
            assertThat(UiComponentUtils.findComponent(view, "approveWeekBtn-" + weekStartOf(bobApproved.getDate())))
                    .isEmpty();
            assertThat(UiComponentUtils.findComponent(view, "approveDayBtn-" + bobApproved.getDate())).isEmpty();
            assertThat(UiComponentUtils.findComponent(view, "approveEntryBtn-" + bobApproved.getId())).isEmpty();
        });
    }

    @Test
    void aStaleEntryFailsTheWholeApproval() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCid();
            view.selectWeek(weekStartOf(cidWednesday.getDate()));

            // Cid edits an entry after the screen loaded it
            TimeEntry edited = reload(cidWednesday);
            edited.setDescription("Fixed the **sample** name");
            dataManager.saveWithoutReload(edited);

            click(view, "approvePersonBtn");
            DialogInfo confirmation = confirmation();
            assertThat(textOf(confirmation)).isEqualTo("Approve 5 entries of Cid Cole for October 2026, 20h?");

            // The framework's handler says what happened; here the exception reaches the caller
            assertThatThrownBy(() -> dialogButton(confirmation, "yes").click())
                    .isInstanceOf(OptimisticLockException.class);

            // The question is gone: left open, it would stand over the reloaded screen and ask again for the stale set
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();

            // Nothing is saved, not even the entries that did not change
            assertThat(statusesOfCid()).containsExactly(
                    TimeEntryStatus.NEW, TimeEntryStatus.NEW, TimeEntryStatus.NEW, TimeEntryStatus.NEW,
                    TimeEntryStatus.REJECTED, TimeEntryStatus.NEW, TimeEntryStatus.APPROVED,
                    TimeEntryStatus.NEW);

            // The screen was loaded again: it shows the entry as it is now, and what still waits
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(view.getSelectedWeek()).isEqualTo(weekStartOf(cidWednesday.getDate()));
            assertThat(markdownOf(UiTestUtils.getComponent(view, "entry-" + cidWednesday.getId())))
                    .isEqualTo("Fixed the **sample** name");
            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Approve 5 waiting");
            assertThat(UiTestUtils.getOpenedNotifications()).isEmpty();
        });
    }

    @Test
    void textsTakeThePluralFormOfTheirCount() {
        LocalDate monday = cidMonday.getDate();

        asAdmin(() -> {
            ((AbstractAuthenticationToken) Objects.requireNonNull(SecurityContextHelper.getAuthentication()))
                    .setDetails(ClientDetails.builder().locale(RUSSIAN).build());
            TimeEntryApprovalView view = openOnCid();
            view.selectWeek(ApprovalPeriod.weekStartOf(monday, DayOfWeek.MONDAY));

            // 5 takes the form of "many", 2 the form of "few", and 1 that of "one"
            assertThat(buttonTextOf(view, "approvePersonBtn")).isEqualTo("Утвердить 5 ожидающих записей");

            // Within the sentence the month is written as Russian writes it there, in lowercase
            click(view, "approveDayBtn-" + monday);
            DialogInfo confirmation = confirmation();
            assertThat(textOf(confirmation)).isEqualTo("Утвердить 2 записи (Cid Cole, 5 окт., 8ч)?");
            dialogButton(confirmation, "yes").click();
            assertThat(lastNotification().getText()).isEqualTo("Утверждены 2 записи");

            click(view, "approveEntryBtn-" + cidTuesday.getId());
            assertThat(lastNotification().getText()).isEqualTo("Утверждена 1 запись");

            click(view, "approvePersonBtn");
            DialogInfo rest = confirmation();
            assertThat(textOf(rest)).isEqualTo("Утвердить 2 записи (Cid Cole, октябрь 2026, 8ч)?");
            dialogButton(rest, "yes").click();
            assertThat(lastNotification().getText()).isEqualTo("Утверждены 2 записи");
        });
    }

    @AfterEach
    void tearDown() {
        try {
            data.removeAll();
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

    /**
     * Opens the screen on Cid's October. Everyone is shown, so that Cid stays shown while nothing of his waits.
     */
    private TimeEntryApprovalView openOnCid() {
        TimeEntryApprovalView view = open();
        view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
        selectCid(view);
        return view;
    }

    private void selectCid(TimeEntryApprovalView view) {
        scopeField(view).setValue(ApprovalRosterScope.EVERYONE);
        view.selectPerson(cid);
    }

    private static JmixRadioButtonGroup<ApprovalRosterScope> scopeField(TimeEntryApprovalView view) {
        return UiTestUtils.getComponent(view, "rosterScopeField");
    }

    private DayOfWeek firstDayOfWeek() {
        return componentSupport.getDaysOfWeek().getFirst();
    }

    private LocalDate weekStartOf(LocalDate date) {
        return ApprovalPeriod.weekStartOf(date, firstDayOfWeek());
    }

    /**
     * Writes the dates of days of October as the screen does, such as "Oct 4 – 10".
     */
    private static String rangeOf(LocalDate first, LocalDate last) {
        return "Oct %d – %d".formatted(first.getDayOfMonth(), last.getDayOfMonth());
    }

    /**
     * Clicks the button as a user does: it must be there, shown, and enabled.
     */
    private static void click(TimeEntryApprovalView view, String id) {
        Button button = UiTestUtils.getComponent(view, id);
        assertThat(button.isVisible()).as("%s is visible", id).isTrue();
        assertThat(button.isEnabled()).as("%s is enabled", id).isTrue();
        ComponentUtil.fireEvent(button, new ClickEvent<>(button));
    }

    private static String buttonTextOf(TimeEntryApprovalView view, String id) {
        Button button = UiTestUtils.getComponent(view, id);
        return button.getText();
    }

    private static DialogInfo confirmation() {
        return Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(), "an approval confirmation");
    }

    private static String textOf(DialogInfo dialog) {
        return dialog.getContent().getElement().getTextRecursively();
    }

    private static Button dialogButton(DialogInfo dialog, String id) {
        Button button = dialog.getButtons().stream()
                .filter(candidate -> id.equals(candidate.getId().orElse(null)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No '" + id + "' button in the dialog"));
        assertThat(button.isVisible()).as("the %s button is visible", id).isTrue();
        assertThat(button.isEnabled()).as("the %s button is enabled", id).isTrue();
        return button;
    }

    private static NotificationInfo lastNotification() {
        return Objects.requireNonNull(UiTestUtils.getLastOpenedNotification(), "a notification");
    }

    private static void assertSuccess(String text) {
        NotificationInfo notification = lastNotification();
        assertThat(notification.getType()).isEqualTo(Notifications.Type.SUCCESS);
        assertThat(notification.getText()).isEqualTo(text);
    }

    /**
     * Reads a figure's label, value, and details, the parts it shows.
     */
    private static List<String> figureOf(TimeEntryApprovalView view, String name) {
        Component figure = UiTestUtils.getComponent(view, "figure-" + name);
        return Stream.of("label", "value", "details")
                .flatMap(part -> descendantsOf(figure)
                        .filter(component -> component.getElement().getClassList().contains(part))
                        .findFirst()
                        .stream())
                .map(component -> component.getElement().getTextRecursively())
                .toList();
    }

    private static String markdownOf(Component container) {
        return descendantsOf(container)
                .filter(Markdown.class::isInstance)
                .map(markdown -> ((Markdown) markdown).getContent())
                .findFirst()
                .orElseThrow();
    }

    private static Stream<Component> descendantsOf(Component root) {
        return root.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendantsOf(child)));
    }

    private static UUID selectedOf(TimeEntryApprovalView view) {
        return Objects.requireNonNull(view.getSelectedPerson(), "a selected person").getId();
    }

    private TimeEntry reload(TimeEntry entry) {
        return dataManager.load(Id.of(entry)).one();
    }

    private TimeEntryStatus statusOf(TimeEntry entry) {
        return reload(entry).getStatus();
    }

    /**
     * Lists the statuses of Cid's entries in the order of the fields: Friday the 2nd, Monday twice, Tuesday, the
     * rejected one, the new and the approved one of the next week, and the one of September.
     */
    private List<TimeEntryStatus> statusesOfCid() {
        return Stream.of(cidFriday, cidMonday, cidMondayToo, cidTuesday, cidRejected, cidWednesday, cidApproved,
                        cidInSeptember)
                .map(this::statusOf)
                .toList();
    }

}

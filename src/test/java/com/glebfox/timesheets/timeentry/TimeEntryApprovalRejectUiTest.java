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
import com.glebfox.timesheets.view.timeentry.approval.ApprovalRosterScope;
import com.glebfox.timesheets.view.timeentry.approval.TimeEntryApprovalView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.internal.StateNode;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.markdowneditor.MarkdownEditor;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.sidedialog.SideDialog;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.sidedialog.SideDialogPosition;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rejecting from the approval screen: the reject basket that ✕ fills, the bar that shows it, the side dialog where the
 * reason is written, and the question before a change of the person, the period, the mode, the project, or the roster
 * discards the basket.
 * <p>
 * The fixture is one project on which admin is a Manager, with time logged in October 2026. Cid Cole has new entries
 * on Monday the 5th and Tuesday the 6th, an approved one on the 7th, a rejected one on the 8th, and a new one a week
 * later, on the 14th; Ann Ash has a new entry on the 5th, and Bob Birch an approved one on the 6th. Each entry is four
 * hours. In either week order the 5th to the 8th are in one week of October's grid, and the 14th in the next.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryApprovalRejectUiTest {

    static final String ADMIN = "admin";
    static final String MODE_SETTING = "ts_TimeEntry.approval.mode";
    static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    static final String EARLIER_REASON = "Say which sample.";
    static final String DESCRIPTION_REASON = "The description is missing, please add what was done.";
    static final String WRONG_TASK_REASON = "Logged on the wrong task, please move it.";

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
    Project project;
    User ann;
    User bob;
    User cid;
    TimeEntry annWaiting;
    TimeEntry bobApproved;
    TimeEntry cidMonday;
    TimeEntry cidTuesday;
    TimeEntry cidApproved;
    TimeEntry cidRejected;
    TimeEntry cidLater;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "approval-reject-" + UUID.randomUUID().toString().substring(0, 8));

        ProjectRole managerRole = data.saveRole(ProjectRoleType.MANAGER);
        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);
        project = data.saveProject("P", data.saveClient());
        Task task = data.saveTask(project, data.saveTaskType());
        data.saveParticipant(project, data.loadUser(ADMIN), managerRole);

        ann = data.saveUser("ann", "Ann", "Ash");
        bob = data.saveUser("bob", "Bob", "Birch");
        cid = data.saveUser("cid", "Cid", "Cole");
        Stream.of(ann, bob, cid).forEach(user -> data.saveParticipant(project, user, memberRole));

        annWaiting = data.saveEntry(task, ann, OCTOBER.atDay(5), TimeEntryStatus.NEW, null);
        bobApproved = data.saveEntry(task, bob, OCTOBER.atDay(6), TimeEntryStatus.APPROVED, null);
        cidMonday = data.saveEntry(task, cid, OCTOBER.atDay(5), TimeEntryStatus.NEW, null);
        cidTuesday = data.saveEntry(task, cid, OCTOBER.atDay(6), TimeEntryStatus.NEW, null);
        cidApproved = data.saveEntry(task, cid, OCTOBER.atDay(7), TimeEntryStatus.APPROVED, null);
        cidRejected = data.saveEntry(task, cid, OCTOBER.atDay(8), TimeEntryStatus.REJECTED, EARLIER_REASON);
        cidLater = data.saveEntry(task, cid, OCTOBER.atDay(14), TimeEntryStatus.NEW, null);
    }

    @Test
    void rejectingCollectsEntriesInTheBasket() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            assertThat(basketBar(view).isVisible()).isFalse();

            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "rejectEntryBtn-" + cidApproved.getId());

            // In the order they were put in, and nothing is saved yet
            assertThat(basketOf(view)).containsExactly(cidMonday.getId(), cidApproved.getId());
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(statusOf(cidApproved)).isEqualTo(TimeEntryStatus.APPROVED);

            // The bar counts the entries and names their dates
            assertThat(basketBar(view).isVisible()).isTrue();
            assertThat(textOf(view, "basketCountText")).isEqualTo("2");
            assertThat(textOf(view, "basketDatesText")).isEqualTo("Oct 5, Oct 7");

            // An entry in the basket is marked, and its only button takes it out again
            Component monday = cardOf(view, cidMonday);
            assertThat(monday.getElement().getClassList()).contains("in-basket");
            assertThat(monday.getElement().getTextRecursively()).contains("In reject basket");
            assertThat(buttonsOf(view, cidMonday)).containsExactly("unreject");
            assertThat(buttonsOf(view, cidApproved)).containsExactly("unreject");
            assertThat(buttonsOf(view, cidTuesday)).containsExactly("approve", "reject");

            click(view, "rejectEntryBtn-" + cidTuesday.getId());
            assertThat(textOf(view, "basketDatesText")).isEqualTo("Oct 5 – 7");

            click(view, "unrejectEntryBtn-" + cidMonday.getId());
            assertThat(basketOf(view)).containsExactly(cidApproved.getId(), cidTuesday.getId());
            assertThat(textOf(view, "basketCountText")).isEqualTo("2");
            assertThat(textOf(view, "basketDatesText")).isEqualTo("Oct 6 – 7");
            assertThat(buttonsOf(view, cidMonday)).containsExactly("approve", "reject");
            assertThat(cardOf(view, cidMonday).getElement().getClassList())
                    .doesNotContain("in-basket");

            // Clear empties the basket at once
            click(view, "clearBasketBtn");
            assertThat(basketOf(view)).isEmpty();
            assertThat(basketBar(view).isVisible()).isFalse();
            assertThat(buttonsOf(view, cidApproved)).containsExactly("reject");
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        });
    }

    @Test
    void writingAReasonRejectsTheBasket() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "rejectEntryBtn-" + cidRejected.getId());

            click(view, "writeReasonBtn");

            // A modal dialog on the right with the entries of the basket
            SideDialog dialog = rejectDialogOf(view);
            assertThat(dialog.isOpened()).isTrue();
            assertThat(dialog.getModality()).isEqualTo(ModalityMode.STRICT);
            assertThat(dialog.getSideDialogPosition()).isEqualTo(SideDialogPosition.RIGHT);
            assertThat(findPart(dialog, "basketEntry-" + cidMonday.getId())).isNotNull();
            assertThat(findPart(dialog, "basketEntry-" + cidRejected.getId())).isNotNull();
            assertThat(this.<Button>partOf(dialog, "rejectBtn").getText()).isEqualTo("Reject 2 entries");

            // Quick reasons append their sentences, each a paragraph of its own once the Markdown is rendered
            MarkdownEditor reasonField = partOf(dialog, "reasonField");
            assertThat(reasonField.getValue()).isEmpty();
            click(partOf(dialog, "quickReason-description"));
            assertThat(reasonField.getValue()).isEqualTo(DESCRIPTION_REASON);
            click(partOf(dialog, "quickReason-wrongTask"));
            assertThat(reasonField.getValue()).isEqualTo(DESCRIPTION_REASON + "\n\n" + WRONG_TASK_REASON);
            reasonField.setValue(DESCRIPTION_REASON);

            click(partOf(dialog, "rejectBtn"));

            // Both are rejected with the reason, which replaces the earlier one
            TimeEntry monday = reload(cidMonday);
            assertThat(monday.getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
            assertThat(monday.getRejectionReason()).isEqualTo(DESCRIPTION_REASON);
            assertThat(reload(cidRejected).getRejectionReason()).isEqualTo(DESCRIPTION_REASON);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.NEW);
            assertSuccess("2 entries rejected");

            // The dialog is closed, the basket empty, and the screen follows
            assertThat(view.getRejectDialog()).isNull();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(basketOf(view)).isEmpty();
            assertThat(basketBar(view).isVisible()).isFalse();
            assertThat(buttonsOf(view, cidMonday)).containsExactly("approve", "reject");
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(cardOf(view, cidMonday).getElement().getClassList())
                    .contains("rejected");
        });
    }

    @Test
    void theDialogRendersTheDescriptionsFromMarkdown() {
        String description = "**Calibrated** the equipment:\n\n- zeroed the emitter\n- aligned the mirrors";
        TimeEntry monday = reload(cidMonday);
        monday.setDescription(description);
        dataManager.saveWithoutReload(monday);

        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "rejectEntryBtn-" + cidTuesday.getId());
            click(view, "writeReasonBtn");

            // Rendered below the day, not shown as its source
            SideDialog dialog = rejectDialogOf(view);
            Component mondayRow = partOf(dialog, "basketEntry-" + cidMonday.getId());
            assertThat(markdownOf(mondayRow)).isEqualTo(description);
            assertThat(mondayRow.getElement().getTextRecursively()).doesNotContain("**", "- zeroed");

            // An entry without a description shows only its day
            Component tuesdayRow = partOf(dialog, "basketEntry-" + cidTuesday.getId());
            assertThat(descendantsOf(tuesdayRow).filter(Markdown.class::isInstance)).isEmpty();
        });
    }

    @Test
    void theReasonIsRequired() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "writeReasonBtn");
            SideDialog dialog = rejectDialogOf(view);
            MarkdownEditor reasonField = partOf(dialog, "reasonField");

            // Marked as required, but not as missing before anything was tried
            assertThat(reasonField.isRequiredIndicatorVisible()).isTrue();
            assertThat(reasonField.isInvalid()).isFalse();

            click(partOf(dialog, "rejectBtn"));

            assertThat(reasonField.isInvalid()).isTrue();
            assertThat(reasonField.getErrorMessage()).isEqualTo("Reason required");
            assertThat(dialog.isOpened()).isTrue();
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);

            // Blanks are no reason either
            reasonField.setValue("  \n ");
            click(partOf(dialog, "rejectBtn"));

            assertThat(reasonField.isInvalid()).isTrue();
            assertThat(dialog.isOpened()).isTrue();
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());
        });
    }

    @Test
    void removingTheLastEntryClosesTheDialog() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "rejectEntryBtn-" + cidTuesday.getId());
            click(view, "writeReasonBtn");
            SideDialog dialog = rejectDialogOf(view);

            click(partOf(dialog, "removeBasketEntryBtn-" + cidMonday.getId()));

            // The entry leaves the dialog and the basket, and gets its own buttons back
            assertThat(dialog.isOpened()).isTrue();
            assertThat(findPart(dialog, "basketEntry-" + cidMonday.getId())).isNull();
            assertThat(findPart(dialog, "basketEntry-" + cidTuesday.getId())).isNotNull();
            assertThat(this.<Button>partOf(dialog, "rejectBtn").getText()).isEqualTo("Reject 1 entry");
            assertThat(basketOf(view)).containsExactly(cidTuesday.getId());
            assertThat(buttonsOf(view, cidMonday)).containsExactly("approve", "reject");
            assertThat(textOf(view, "basketCountText")).isEqualTo("1");

            click(partOf(dialog, "removeBasketEntryBtn-" + cidTuesday.getId()));

            assertThat(dialog.isOpened()).isFalse();
            assertThat(view.getRejectDialog()).isNull();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(basketOf(view)).isEmpty();
            assertThat(basketBar(view).isVisible()).isFalse();
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.NEW);
        });
    }

    @Test
    void closingWithAChangedReasonAsks() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "writeReasonBtn");
            SideDialog dialog = rejectDialogOf(view);
            MarkdownEditor reasonField = partOf(dialog, "reasonField");
            reasonField.setValue(EARLIER_REASON);

            click(partOf(dialog, "cancelBtn"));

            // No keeps the dialog open with what was written
            dialogButton(unsavedQuestion(), "no").click();
            assertThat(dialog.isOpened()).isTrue();
            assertThat(reasonField.getValue()).isEqualTo(EARLIER_REASON);

            // Esc, or a click outside, asks the same
            Dialog overlay = (Dialog) dialog.getChildren().findFirst().orElseThrow();
            ComponentUtil.fireEvent(overlay, new Dialog.DialogCloseActionEvent(overlay, true));
            dialogButton(unsavedQuestion(), "yes").click();

            // Discarding the reason closes the dialog, and the basket stays
            assertThat(dialog.isOpened()).isFalse();
            assertThat(view.getRejectDialog()).isNull();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());
            assertThat(basketBar(view).isVisible()).isTrue();
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);

            // With nothing written, the dialog closes at once
            click(view, "writeReasonBtn");
            SideDialog again = rejectDialogOf(view);
            assertThat(this.<MarkdownEditor>partOf(again, "reasonField").getValue()).isEmpty();
            click(partOf(again, "cancelBtn"));
            assertThat(again.isOpened()).isFalse();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        });
    }

    @Test
    void theReasonHasTheFocus() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            lastFocused();

            click(view, "writeReasonBtn");
            SideDialog dialog = rejectDialogOf(view);
            MarkdownEditor reasonField = partOf(dialog, "reasonField");

            // The dialog opens on the reason, not on itself
            assertThat(lastFocused()).isSameAs(reasonField.getElement().getNode());

            // No to the question puts the focus back in the reason, not on the screen behind the dialog
            reasonField.setValue(EARLIER_REASON);
            click(partOf(dialog, "cancelBtn"));
            lastFocused();
            dialogButton(unsavedQuestion(), "no").click();
            assertThat(lastFocused()).isSameAs(reasonField.getElement().getNode());
        });
    }

    @Test
    void theButtonThatTakesTheClickedOnesPlaceHasTheFocus() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            lastFocused();

            // A click from the browser removes the clicked button, so the focus goes to the one that replaces it
            clickFromBrowser(view, "rejectEntryBtn-" + cidMonday.getId());
            assertThat(lastFocused()).isSameAs(nodeOf(view, "unrejectEntryBtn-" + cidMonday.getId()));

            clickFromBrowser(view, "unrejectEntryBtn-" + cidMonday.getId());
            assertThat(lastFocused()).isSameAs(nodeOf(view, "rejectEntryBtn-" + cidMonday.getId()));

            // What the server does moves no focus: a click it makes, or the buttons changing without a click
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            assertThat(lastFocused()).isNull();
            click(view, "clearBasketBtn");
            assertThat(buttonsOf(view, cidMonday)).containsExactly("approve", "reject");
            assertThat(lastFocused()).isNull();
        });
    }

    @Test
    void changingThePersonAsksBeforeDiscardingTheBasket() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());

            view.selectPerson(ann);

            // No keeps the person and the basket
            DialogInfo question = discardQuestion();
            assertThat(textOf(question)).isEqualTo("Discard 1 entry from the reject basket?");
            dialogButton(question, "no").click();
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());
            assertThat(buttonsOf(view, cidMonday)).containsExactly("unreject");

            // Yes discards the basket and selects the other person
            view.selectPerson(ann);
            dialogButton(discardQuestion(), "yes").click();
            assertThat(selectedOf(view)).isEqualTo(ann.getId());
            assertThat(basketOf(view)).isEmpty();
            assertThat(basketBar(view).isVisible()).isFalse();

            // Choosing the same person again asks nothing
            view.selectPerson(cid);
            view.selectWeek(weekStartOf(cidMonday.getDate()));
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            view.selectPerson(cid);
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());

            // The same for the period
            click(view, "nextBtn");
            dialogButton(discardQuestion(), "no").click();
            assertThat(view.getPeriod()).isEqualTo(ApprovalPeriod.monthOf(OCTOBER));
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());

            click(view, "nextBtn");
            dialogButton(discardQuestion(), "yes").click();
            assertThat(view.getPeriod()).isEqualTo(ApprovalPeriod.monthOf(OCTOBER.plusMonths(1)));
            assertThat(basketOf(view)).isEmpty();
            assertThat(basketBar(view).isVisible()).isFalse();
        });
    }

    @Test
    void changingTheModeOrTheProjectAsksAndNoKeepsTheField() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            JmixRadioButtonGroup<ApprovalMode> modeField = UiTestUtils.getComponent(view, "modeField");
            JmixComboBox<Project> projectField = UiTestUtils.getComponent(view, "projectField");

            // No puts the mode back, and the mode is not remembered
            modeField.setValue(ApprovalMode.WEEK);
            dialogButton(discardQuestion(), "no").click();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(modeField.getValue()).isEqualTo(ApprovalMode.MONTH);
            assertThat(view.getPeriod()).isEqualTo(ApprovalPeriod.monthOf(OCTOBER));
            assertThat(userSettingsService.load(MODE_SETTING)).isEmpty();
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());

            // No puts the project back
            projectField.setValue(project);
            dialogButton(discardQuestion(), "no").click();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(projectField.getValue()).isNull();
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());

            // Yes changes the project and discards the basket
            projectField.setValue(project);
            dialogButton(discardQuestion(), "yes").click();
            assertThat(projectField.getValue()).isEqualTo(project);
            assertThat(basketOf(view)).isEmpty();

            // Yes changes the mode, which is then remembered
            view.selectPerson(cid);
            view.selectWeek(weekStartOf(cidLater.getDate()));
            click(view, "rejectEntryBtn-" + cidLater.getId());
            modeField.setValue(ApprovalMode.WEEK);
            dialogButton(discardQuestion(), "yes").click();
            assertThat(view.getPeriod().mode()).isEqualTo(ApprovalMode.WEEK);
            assertThat(userSettingsService.load(MODE_SETTING)).contains(ApprovalMode.WEEK.name());
            assertThat(basketOf(view)).isEmpty();
        });
    }

    @Test
    void hidingThePersonFromTheRosterAsksBeforeDiscardingTheBasket() {
        asAdmin(() -> {
            TimeEntryApprovalView view = open();
            view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
            JmixRadioButtonGroup<ApprovalRosterScope> scopeField = UiTestUtils.getComponent(view,
                    "rosterScopeField");
            scopeField.setValue(ApprovalRosterScope.EVERYONE);
            view.selectPerson(bob);
            click(view, "rejectEntryBtn-" + bobApproved.getId());

            // Nothing of Bob's waits, so Needs review would hide him
            scopeField.setValue(ApprovalRosterScope.NEEDS_REVIEW);
            dialogButton(discardQuestion(), "no").click();
            assertThat(scopeField.getValue()).isEqualTo(ApprovalRosterScope.EVERYONE);
            assertThat(selectedOf(view)).isEqualTo(bob.getId());
            assertThat(basketOf(view)).containsExactly(bobApproved.getId());

            scopeField.setValue(ApprovalRosterScope.NEEDS_REVIEW);
            dialogButton(discardQuestion(), "yes").click();
            assertThat(scopeField.getValue()).isEqualTo(ApprovalRosterScope.NEEDS_REVIEW);
            assertThat(selectedOf(view)).isEqualTo(ann.getId());
            assertThat(basketOf(view)).isEmpty();
        });
    }

    @Test
    void basketEntriesAreLeftOutOfABulkApprove() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            assertThat(personButtonTextOf(view)).isEqualTo("Approve 3 waiting");

            click(view, "rejectEntryBtn-" + cidMonday.getId());

            // The counts leave the entry out at once, and a day with nothing else to approve has no button
            assertThat(personButtonTextOf(view)).isEqualTo("Approve 2 waiting");
            assertThat(UiComponentUtils.findComponent(view, "approveDayBtn-" + cidMonday.getDate())).isEmpty();
            assertThat(UiComponentUtils.findComponent(view, "approveDayBtn-" + cidTuesday.getDate())).isPresent();

            click(view, "approvePersonBtn");
            DialogInfo confirmation = lastDialog();
            assertThat(textOf(confirmation)).isEqualTo("Approve 2 entries of Cid Cole for October 2026, 8h?");
            dialogButton(confirmation, "yes").click();

            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidLater)).isEqualTo(TimeEntryStatus.APPROVED);
            assertSuccess("2 entries approved");

            // The entry is still in the basket after the screen is loaded again
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());
            assertThat(buttonsOf(view, cidMonday)).containsExactly("unreject");
            assertThat(basketBar(view).isVisible()).isTrue();
            assertThat(UiComponentUtils.findComponent(view, "approvePersonBtn")).isEmpty();
        });
    }

    @Test
    void aStaleEntryFailsTheWholeRejection() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeek();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "rejectEntryBtn-" + cidTuesday.getId());

            // Cid edits an entry after the screen loaded it
            TimeEntry edited = reload(cidTuesday);
            edited.setDescription("Fixed the sample name");
            dataManager.saveWithoutReload(edited);

            click(view, "writeReasonBtn");
            SideDialog dialog = rejectDialogOf(view);
            click(partOf(dialog, "quickReason-description"));

            // The framework's handler says what happened; here the exception reaches the caller
            assertThatThrownBy(() -> click(partOf(dialog, "rejectBtn")))
                    .isInstanceOf(OptimisticLockException.class);

            // Nothing is saved, the dialog is gone, and the basket stays with the entries as they are now
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.NEW);
            assertThat(dialog.isOpened()).isFalse();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(basketOf(view)).containsExactly(cidMonday.getId(), cidTuesday.getId());
            assertThat(markdownOf(cardOf(view, cidTuesday))).isEqualTo("Fixed the sample name");
            assertThat(UiTestUtils.getOpenedNotifications()).isEmpty();

            // The reason written waits in the dialog, which now rejects the entries as they are
            click(view, "writeReasonBtn");
            SideDialog again = rejectDialogOf(view);
            assertThat(this.<MarkdownEditor>partOf(again, "reasonField").getValue()).isEqualTo(DESCRIPTION_REASON);
            click(partOf(again, "rejectBtn"));

            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.REJECTED);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.REJECTED);
            assertSuccess("2 entries rejected");
        });
    }

    @Test
    void aReloadThatLeavesNothingWaitingKeepsThePersonWhoseBasketHasEntries() {
        asAdmin(() -> {
            // Under Needs review, the default, Cid is shown while something of his waits
            TimeEntryApprovalView view = openOnCidsWeekUnderNeedsReview();
            click(view, "rejectEntryBtn-" + cidApproved.getId());

            click(view, "approvePersonBtn");
            dialogButton(lastDialog(), "yes").click();

            // Nothing of Cid's waits now, which Needs review would hide, but his basket has an entry
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidTuesday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(statusOf(cidLater)).isEqualTo(TimeEntryStatus.APPROVED);
            assertSuccess("3 entries approved");
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(view.getRosterPeople()).extracting(person -> person.user().getId()).contains(cid.getId());
            assertThat(basketOf(view)).containsExactly(cidApproved.getId());
            assertThat(basketBar(view).isVisible()).isTrue();

            // A change of the search that would hide him asks all the same
            TypedTextField<String> searchField = UiTestUtils.getComponent(view, "rosterSearchField");
            searchField.setValue("ann");
            dialogButton(discardQuestion(), "no").click();
            assertThat(searchField.getValue()).isEmpty();
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(basketOf(view)).containsExactly(cidApproved.getId());

            searchField.setValue("ann");
            dialogButton(discardQuestion(), "yes").click();
            assertThat(selectedOf(view)).isEqualTo(ann.getId());
            assertThat(basketOf(view)).isEmpty();
        });
    }

    @Test
    void aFailedRejectionKeepsThePersonAndTheReasonWhenTheReloadLeavesNothingWaiting() {
        asAdmin(() -> {
            TimeEntryApprovalView view = openOnCidsWeekUnderNeedsReview();
            click(view, "rejectEntryBtn-" + cidMonday.getId());
            click(view, "writeReasonBtn");
            SideDialog dialog = rejectDialogOf(view);
            click(partOf(dialog, "quickReason-description"));

            // Another approver approves everything of Cid's that waits after the screen loaded it
            approveBehindTheScreen(cidMonday, cidTuesday, cidLater);

            assertThatThrownBy(() -> click(partOf(dialog, "rejectBtn")))
                    .isInstanceOf(OptimisticLockException.class);

            // Nothing is rejected, and Cid, no longer waiting, stays with his basket and the reason written
            assertThat(statusOf(cidMonday)).isEqualTo(TimeEntryStatus.APPROVED);
            assertThat(selectedOf(view)).isEqualTo(cid.getId());
            assertThat(basketOf(view)).containsExactly(cidMonday.getId());

            click(view, "writeReasonBtn");
            assertThat(this.<MarkdownEditor>partOf(rejectDialogOf(view), "reasonField").getValue())
                    .isEqualTo(DESCRIPTION_REASON);
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
     * Opens the screen on Cid's October, with the week of the 5th listed. Everyone is shown, so that Cid stays shown
     * while nothing of his waits.
     */
    private TimeEntryApprovalView openOnCidsWeek() {
        TimeEntryApprovalView view = open();
        view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
        JmixRadioButtonGroup<ApprovalRosterScope> scopeField = UiTestUtils.getComponent(view, "rosterScopeField");
        scopeField.setValue(ApprovalRosterScope.EVERYONE);
        view.selectPerson(cid);
        view.selectWeek(weekStartOf(cidMonday.getDate()));
        return view;
    }

    /**
     * Opens the screen on Cid's October as it opens, under Needs review, with the week of the 5th listed.
     */
    private TimeEntryApprovalView openOnCidsWeekUnderNeedsReview() {
        TimeEntryApprovalView view = open();
        view.setPeriod(ApprovalPeriod.monthOf(OCTOBER));
        view.selectPerson(cid);
        view.selectWeek(weekStartOf(cidMonday.getDate()));
        return view;
    }

    /**
     * Approves entries as another approver does, without the screen knowing.
     */
    private void approveBehindTheScreen(TimeEntry... entries) {
        for (TimeEntry entry : entries) {
            TimeEntry current = reload(entry);
            current.setStatus(TimeEntryStatus.APPROVED);
            dataManager.saveWithoutReload(current);
        }
    }

    private LocalDate weekStartOf(LocalDate date) {
        DayOfWeek firstDayOfWeek = componentSupport.getDaysOfWeek().getFirst();
        return ApprovalPeriod.weekStartOf(date, firstDayOfWeek);
    }

    private static Component basketBar(TimeEntryApprovalView view) {
        return UiTestUtils.getComponent(view, "basketBar");
    }

    private static List<UUID> basketOf(TimeEntryApprovalView view) {
        return view.getBasket().stream().map(TimeEntry::getId).toList();
    }

    private static UUID selectedOf(TimeEntryApprovalView view) {
        return Objects.requireNonNull(view.getSelectedPerson(), "a selected person").getId();
    }

    private static SideDialog rejectDialogOf(TimeEntryApprovalView view) {
        return Objects.requireNonNull(view.getRejectDialog(), "the reject dialog");
    }

    /**
     * Lists the buttons an entry's card shows: ✓, ✕, or ↺.
     */
    private static List<String> buttonsOf(TimeEntryApprovalView view, TimeEntry entry) {
        return Stream.of("approve", "reject", "unreject")
                .filter(name -> UiComponentUtils.findComponent(view, name + "EntryBtn-" + entry.getId()).isPresent())
                .toList();
    }

    /**
     * Finds a part of the reject dialog by its id, in its header, content, or footer.
     */
    @SuppressWarnings("unchecked")
    private <T extends Component> T partOf(SideDialog dialog, String id) {
        Component part = findPart(dialog, id);
        assertThat(part).as("the dialog's %s", id).isNotNull();
        return (T) part;
    }

    private static Component findPart(SideDialog dialog, String id) {
        return Stream.of(dialog.getHeader().getComponents(), dialog.getContentComponents(),
                        dialog.getFooter().getComponents())
                .flatMap(List::stream)
                .flatMap(root -> Stream.concat(Stream.of(root), descendantsOf(root)))
                .filter(component -> id.equals(component.getId().orElse(null)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the node of the component that the server last asked the browser to focus since the previous call, and
     * forgets the calls: a UI test sees a focus only as the call queued for the browser.
     */
    private static StateNode lastFocused() {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getInvocation().getExpression().contains("this.focus()"))
                .map(PendingJavaScriptInvocation::getOwner)
                .reduce((first, second) -> second)
                .orElse(null);
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

    /**
     * Clicks the button of the view as a user does: it must be there, shown, and enabled.
     */
    private static void click(TimeEntryApprovalView view, String id) {
        click(UiTestUtils.getComponent(view, id));
    }

    private static void click(Component button) {
        assertThat(button.isVisible()).as("%s is visible", button.getId().orElse("the button")).isTrue();
        assertThat(((HasEnabled) button).isEnabled()).as("%s is enabled", button.getId().orElse("the button"))
                .isTrue();
        ComponentUtil.fireEvent(button, new ClickEvent<>(button));
    }

    /**
     * Clicks the button as the browser reports a click, unlike {@link #click(TimeEntryApprovalView, String)}, which
     * the server makes.
     */
    private static void clickFromBrowser(TimeEntryApprovalView view, String id) {
        Component button = UiTestUtils.getComponent(view, id);
        assertThat(button.isVisible()).as("%s is visible", id).isTrue();
        assertThat(((HasEnabled) button).isEnabled()).as("%s is enabled", id).isTrue();
        ComponentUtil.fireEvent(button,
                new ClickEvent<>(button, true, 0, 0, 0, 0, 1, 0, false, false, false, false));
    }

    private static StateNode nodeOf(TimeEntryApprovalView view, String id) {
        return UiTestUtils.<Component>getComponent(view, id).getElement().getNode();
    }

    private static String textOf(TimeEntryApprovalView view, String id) {
        return UiTestUtils.<Component>getComponent(view, id).getElement().getTextRecursively();
    }

    private static Component cardOf(TimeEntryApprovalView view, TimeEntry entry) {
        return UiTestUtils.getComponent(view, "entry-" + entry.getId());
    }

    private static String personButtonTextOf(TimeEntryApprovalView view) {
        Button button = UiTestUtils.getComponent(view, "approvePersonBtn");
        return button.getText();
    }

    private static DialogInfo lastDialog() {
        return Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(), "a dialog");
    }

    private DialogInfo discardQuestion() {
        DialogInfo question = lastDialog();
        assertThat(question.getDialog().getHeaderTitle()).isEqualTo(messages.getMessage("dialogs.Confirmation"));
        return question;
    }

    private DialogInfo unsavedQuestion() {
        DialogInfo question = lastDialog();
        assertThat(question.getDialog().getHeaderTitle())
                .isEqualTo(messages.getMessage("dialogs.closeUnsaved.title"));
        return question;
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

    private static void assertSuccess(String text) {
        NotificationInfo notification = Objects.requireNonNull(UiTestUtils.getLastOpenedNotification(),
                "a notification");
        assertThat(notification.getType()).isEqualTo(Notifications.Type.SUCCESS);
        assertThat(notification.getText()).isEqualTo(text);
    }

    private TimeEntry reload(TimeEntry entry) {
        return dataManager.load(Id.of(entry)).one();
    }

    private TimeEntryStatus statusOf(TimeEntry entry) {
        return reload(entry).getStatus();
    }
}

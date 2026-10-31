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

package com.glebfox.timesheets.view.timeentry.approval;

import com.glebfox.timesheets.app.TimeEntryApprovalSupport;
import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalOverview;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ApprovalPerson;
import com.glebfox.timesheets.app.util.ApprovalTotals;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.app.util.UnsavedInputSupport;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.sidedialog.SideDialog;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.virtuallist.JmixVirtualList;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.settings.UserSettingsService;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.flowui.view.ViewValidation;
import jakarta.persistence.OptimisticLockException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The screen where the managers and approvers of projects review the time logged on them, person by person.
 * <p>
 * It shows the time entries of one period, a week or a month, that the current user may approve: a roster of the
 * people who logged them, one of whom is the selected person, the progress of the whole team, and the selected
 * person's period in a {@link ApprovalPersonPanel}. In a month, one week of the person's is listed in full.
 * <p>
 * The selected person's entries are approved from the panel at four levels. A click on an entry's ✓ approves it at
 * once. A click on the button of a day, a week, or the whole period asks first, naming the count, the hours, the
 * person, and the period, and then approves the new entries of that scope: {@link #entriesToApprove} is the one place
 * that says which, for the counts on the buttons, for the question, and for the approval. After an approval the screen
 * is loaded again and a notification gives the number of entries approved. When an entry changed meanwhile, nothing is
 * approved: the screen is loaded again, and the framework tells the user.
 * <p>
 * Rejecting goes through the reject basket, which holds entries of the selected person: an entry's ✕ puts it there and
 * its ↺ takes it out, and a bulk approve leaves the basket's entries out. While the basket has entries, a
 * {@link ApprovalBasketBar} shows them, and its Write a reason opens the {@link ApprovalRejectDialog}, where they are
 * rejected together with one reason. A change that would leave the basket behind, of the person, the period, the
 * mode, the project, or a roster's filter that hides the person, asks first whether to discard it; No keeps
 * everything as it was. A reload never drops the basket by hiding its person: it keeps them in the roster. A
 * rejection, like an approval, fails as a whole for an entry changed meanwhile; the basket then keeps its entries as
 * they are now, and the dialog opens again on the reason written.
 */
@Route(value = "approve-time-entries", layout = MainView.class)
@ViewController(TimeEntryApprovalView.ID)
@ViewDescriptor("time-entry-approval-view.xml")
public class TimeEntryApprovalView extends StandardView {

    public static final String ID = "ts_TimeEntry.approval";

    /**
     * The key of the user setting that remembers the mode, a constant of {@link ApprovalMode}.
     */
    static final String MODE_SETTING = ID + ".mode";

    @ViewComponent
    private JmixRadioButtonGroup<ApprovalMode> modeField;
    @ViewComponent
    private H2 periodTitle;
    @ViewComponent
    private JmixButton defaultPeriodBtn;
    @ViewComponent
    private JmixComboBox<Project> projectField;
    @ViewComponent
    private Div teamProgress;
    @ViewComponent
    private Span teamProgressText;
    @ViewComponent
    private Span teamProgressDetails;
    @ViewComponent
    private Div rosterBox;
    @ViewComponent
    private TypedTextField<String> rosterSearchField;
    @ViewComponent
    private JmixRadioButtonGroup<ApprovalRosterScope> rosterScopeField;
    @ViewComponent
    private JmixVirtualList<ApprovalPerson> rosterList;
    @ViewComponent
    private Div personBox;
    @ViewComponent
    private Div emptyStateBox;
    @ViewComponent
    private H3 emptyStateHeader;
    @ViewComponent
    private Span emptyStateText;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private TimeEntryApprovalSupport approvalSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private UserSettingsService userSettingsService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private Messages messages;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private Notifications notifications;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private ViewValidation viewValidation;
    @Autowired
    private UnsavedInputSupport unsavedInputSupport;

    private Locale locale;
    private DayOfWeek firstDayOfWeek;
    private ApprovalPeriodTitle periodTitles;
    private ApprovalStatusBar teamProgressBar;
    private ApprovalPersonPanel personPanel;
    private ApprovalBasketBar basketBar;
    private ApprovalRejectDialog rejectDialog;

    private List<Project> projectsToApprove = List.of();
    // Null until the view is shown
    @Nullable
    private ApprovalPeriod period;
    private ApprovalOverview overview = new ApprovalOverview(List.of(), ApprovalTotals.of(List.of()));
    private Map<UUID, List<String>> roleNamesByUser = Map.of();
    private List<ApprovalPerson> rosterPeople = List.of();
    @Nullable
    private User selectedPerson;
    // The person the panel shows, as loaded; a new load gives the same user a new instance
    @Nullable
    private ApprovalPerson shownPerson;
    // In a month, the first day of the week listed; null in a week, or with nobody selected
    @Nullable
    private LocalDate selectedWeek;
    // Where the roster last showed the selected person's card; null with nobody selected
    @Nullable
    private RosterPlace rosterPlace;
    // The reject basket: entries of the person shown, by id, in the order they were put in, each as last loaded
    private final Map<UUID, TimeEntry> basket = new LinkedHashMap<>();
    // The reason of a rejection that failed, which the dialog opens on again; empty for none
    private String draftReason = "";
    // Set while a field is put back after the question about the basket was answered No
    private boolean revertingField;

    @Subscribe
    public void onInit(final InitEvent event) {
        locale = currentAuthentication.getLocale();
        // Weeks start as in the Calendar
        firstDayOfWeek = componentSupport.getDaysOfWeek().getFirst();

        periodTitles = new ApprovalPeriodTitle(messageBundle::getMessage, locale);
        initRoster();
        initTeamProgressBar();
        initPersonPanel();
        initBasket();

        projectsToApprove = approvalSupport.loadProjectsToApprove();
        projectField.setItems(projectsToApprove);

        // Set here, as the descriptor's itemsEnum takes Jmix enums only; the captions still come from message keys
        modeField.setItems(List.of(ApprovalMode.values()));
        modeField.setValue(loadMode());
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        setPeriod(defaultPeriod(Objects.requireNonNull(modeField.getValue())));
    }

    private void initRoster() {
        rosterScopeField.setItems(List.of(ApprovalRosterScope.values()));
        rosterScopeField.setValue(ApprovalRosterScope.NEEDS_REVIEW);
        rosterList.setRenderer(new ComponentRenderer<>(this::createRosterCard, this::updateRosterCard));
        rosterList.setItemAccessibleNameGenerator(person -> ApprovalRosterCard.nameOf(person.user()));
    }

    private void initTeamProgressBar() {
        teamProgressBar = new ApprovalStatusBar();
        teamProgressBar.addClassName("time-entry-approval-view-team-progress-bar");
        teamProgress.add(teamProgressBar);
    }

    private void initPersonPanel() {
        personPanel = new ApprovalPersonPanel(componentSupport, messages, metadataTools, locale, periodTitles,
                this::entriesToApprove, entry -> basket.containsKey(entry.getId()));
        // A click on the grid goes through the same way as selectWeek from outside
        personPanel.addWeekClickListener(event -> selectWeek(event.getWeekStart()));
        personPanel.addDayClickListener(event -> {
            selectWeek(ApprovalPeriod.weekStartOf(event.getDay(), firstDayOfWeek));
            personPanel.scrollToDay(event.getDay());
        });
        personPanel.addApproveEntryClickListener(event -> approve(List.of(event.getEntry())));
        personPanel.addApproveScopeClickListener(event -> confirmApprove(event.getScope()));
        personPanel.addRejectEntryClickListener(event -> addToBasket(event.getEntry()));
        personPanel.addUnrejectEntryClickListener(event -> removeFromBasket(event.getEntry()));
        personBox.add(personPanel);
    }

    private void initBasket() {
        // Over the selected person's part, so it is hidden with it
        basketBar = new ApprovalBasketBar(componentSupport, messages, locale, periodTitles);
        basketBar.addClearClickListener(event -> emptyBasket());
        basketBar.addWriteReasonClickListener(event -> openRejectDialog());
        personBox.add(basketBar);

        rejectDialog = new ApprovalRejectDialog(dialogs, uiComponents, viewValidation, unsavedInputSupport,
                componentSupport, messages, metadataTools, locale);
    }

    /**
     * Returns the period the screen shows.
     *
     * @return the current period, a week or a month as the mode switch says
     */
    public ApprovalPeriod getPeriod() {
        Preconditions.checkState(period != null, "The view has not been shown yet");
        return period;
    }

    /**
     * Shows another period, of the mode the switch is set to, and selects the first person shown. Unlike the period's
     * buttons, it does not ask about the reject basket: the entries the period does not show leave it.
     *
     * @param period the period to show
     * @throws IllegalArgumentException if the period is not of the mode the switch is set to
     */
    public void setPeriod(ApprovalPeriod period) {
        Preconditions.checkArgument(period.mode() == modeField.getValue(),
                "The period is a %s, while the mode is %s", period.mode(), modeField.getValue());
        this.period = period;
        updatePeriodControls();
        load(false);
    }

    /**
     * Returns the people the roster shows, after its switch and its search.
     *
     * @return the people shown, in the roster's order
     */
    public List<ApprovalPerson> getRosterPeople() {
        return rosterPeople;
    }

    /**
     * Returns the user of the selected person.
     *
     * @return the selected person's user, {@code null} when the roster shows nobody
     */
    @Nullable
    public User getSelectedPerson() {
        return selectedPerson;
    }

    /**
     * Returns the week whose entries are listed in a month.
     *
     * @return the first day of the week, a row of the month's grid; {@code null} in a week, or with nobody selected
     */
    @Nullable
    public LocalDate getSelectedWeek() {
        return selectedWeek;
    }

    /**
     * Lists the entries of another week of the selected person's month, as a click on its card does.
     *
     * @param weekStart the first day of a row of the month's grid
     * @throws IllegalStateException    if the screen shows a week, or nobody is selected
     * @throws IllegalArgumentException if no row of the month's grid starts on the day
     */
    public void selectWeek(LocalDate weekStart) {
        Preconditions.checkState(getPeriod().mode() == ApprovalMode.MONTH && selectedPerson != null,
                "Only a person's month lists a week");
        Preconditions.checkArgument(isWeekOfPeriod(weekStart), "No week of %s starts on %s", period, weekStart);
        if (weekStart.equals(selectedWeek)) {
            return;
        }

        selectedWeek = weekStart;
        personPanel.showWeek(weekStart);
    }

    /**
     * Selects a person the roster shows, as a click on their card does. Choosing another person while the reject
     * basket has entries asks first whether to discard them, and selects the person only on yes.
     *
     * @param user the person's user
     * @throws IllegalArgumentException if the roster does not show the person
     */
    public void selectPerson(User user) {
        ApprovalPerson person = findShown(user.getId()).orElseThrow(() -> new IllegalArgumentException(
                "The roster does not show user %s".formatted(user.getId())));
        if (isSelected(person)) {
            selectShown(person);
        } else {
            changeDiscardingBasket(() -> selectShown(person), () -> {
            });
        }
    }

    /**
     * Returns the reject basket: the entries of the selected person that their ✕ put there, to be rejected together
     * with one reason.
     *
     * @return the entries, in the order they were put in the basket
     */
    public Set<TimeEntry> getBasket() {
        return Collections.unmodifiableSequencedSet(new LinkedHashSet<>(basket.values()));
    }

    /**
     * Returns the dialog where the entries of the reject basket are rejected, while it is open.
     *
     * @return the dialog, {@code null} while it is closed
     */
    @Nullable
    public SideDialog getRejectDialog() {
        return rejectDialog.getSideDialog();
    }

    private void selectShown(ApprovalPerson person) {
        Optional<ApprovalPerson> previous = selectedPerson != null
                ? findShown(selectedPerson.getId())
                : Optional.empty();
        // Choosing the person shown again keeps their week
        setSelectedPerson(person, true);
        // Chosen on a card the user sees, so the roster stays where it is
        rosterPlace = placeInRoster(person.user().getId());

        // Only the two cards whose mark changes are redrawn, and in place: a refresh of all the items would create
        // every card anew, and the card chosen from the keyboard would lose the focus.
        // The roster has its items, as it shows the person
        DataProvider<ApprovalPerson, ?> items = Objects.requireNonNull(rosterList.getDataProvider());
        previous.filter(other -> other != person).ifPresent(items::refreshItem);
        items.refreshItem(person);
    }

    /**
     * Loads the period again, such as after an action changed its entries. The selected person stays selected while
     * the roster still shows them, or while the reject basket has their entries; otherwise the first person shown is
     * selected.
     */
    public void reload() {
        load(true);
    }

    @Subscribe("modeField")
    public void onModeFieldComponentValueChange(
            final ComponentValueChangeEvent<JmixRadioButtonGroup<ApprovalMode>, ApprovalMode> event) {
        ApprovalMode mode = event.getValue();
        // The mode set when the view opens, before any period, is the remembered one; the mode put back after a No
        // is the period's own
        if (period == null || mode == null || mode == period.mode()) {
            return;
        }

        ApprovalMode shownMode = period.mode();
        // Remembered only once the change is made
        changeDiscardingBasket(() -> {
            userSettingsService.save(MODE_SETTING, mode.name());
            setPeriod(defaultPeriod(mode));
        }, () -> modeField.setValue(shownMode));
    }

    @Subscribe(id = "previousBtn", subject = "clickListener")
    public void onPreviousBtnClick(final ClickEvent<JmixButton> event) {
        showPeriod(getPeriod().previous());
    }

    @Subscribe(id = "nextBtn", subject = "clickListener")
    public void onNextBtnClick(final ClickEvent<JmixButton> event) {
        showPeriod(getPeriod().next());
    }

    @Subscribe(id = "defaultPeriodBtn", subject = "clickListener")
    public void onDefaultPeriodBtnClick(final ClickEvent<JmixButton> event) {
        showPeriod(defaultPeriod(getPeriod().mode()));
    }

    @Subscribe("projectField")
    public void onProjectFieldComponentValueChange(
            final ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (period == null || revertingField) {
            return;
        }

        Project shownProject = event.getOldValue();
        changeDiscardingBasket(() -> load(false), () -> revert(() -> projectField.setValue(shownProject)));
    }

    @Subscribe("rosterSearchField")
    public void onRosterSearchFieldComponentValueChange(
            final ComponentValueChangeEvent<TypedTextField<String>, String> event) {
        if (!revertingField) {
            String shownSearch = event.getOldValue();
            changeRoster(() -> revert(() -> rosterSearchField.setValue(shownSearch)));
        }
    }

    @Subscribe("rosterScopeField")
    public void onRosterScopeFieldComponentValueChange(
            final ComponentValueChangeEvent<JmixRadioButtonGroup<ApprovalRosterScope>, ApprovalRosterScope> event) {
        if (!revertingField) {
            ApprovalRosterScope shownScope = event.getOldValue();
            changeRoster(() -> revert(() -> rosterScopeField.setValue(shownScope)));
        }
    }

    /**
     * Shows another period, as the period's buttons do: asking first while the reject basket has entries.
     */
    private void showPeriod(ApprovalPeriod other) {
        changeDiscardingBasket(() -> setPeriod(other), () -> {
        });
    }

    /**
     * Shows the roster after its switch or its search changed. A change that hides the selected person selects
     * another one, so it asks first while the reject basket has entries; keep puts the field back on No.
     */
    private void changeRoster(Runnable keep) {
        if (shownPerson != null && !isInRoster(shownPerson)) {
            changeDiscardingBasket(() -> showRoster(true), keep);
        } else {
            showRoster(true);
        }
    }

    /**
     * Makes a change that would leave the reject basket's entries behind, such as showing another person or another
     * period: at once while the basket is empty, otherwise after asking whether to discard them. Yes empties the
     * basket and makes the change; No calls keep, which puts back a field the user has already changed, so that
     * everything stays as it was.
     */
    private void changeDiscardingBasket(Runnable change, Runnable keep) {
        if (basket.isEmpty()) {
            change.run();
            return;
        }

        dialogs.createOptionDialog()
                .withHeader(messages.getMessage("dialogs.Confirmation"))
                .withText(countText("discardBasketConfirmation", basket.size()))
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(event -> {
                                    emptyBasket();
                                    change.run();
                                }),
                        // Keeping the basket is the safe answer, as in the framework's question on unsaved changes
                        new DialogAction(DialogAction.Type.NO)
                                .withHandler(event -> keep.run())
                                .withVariant(ActionVariant.PRIMARY))
                .open();
    }

    /**
     * Puts back the value of a field without acting on the change, as after a No to the question about the basket.
     */
    private void revert(Runnable putBack) {
        revertingField = true;
        try {
            putBack.run();
        } finally {
            revertingField = false;
        }
    }

    private ApprovalMode loadMode() {
        return userSettingsService.load(MODE_SETTING)
                .map(value -> EnumUtils.getEnum(ApprovalMode.class, value, ApprovalMode.MONTH))
                .orElse(ApprovalMode.MONTH);
    }

    private ApprovalPeriod defaultPeriod(ApprovalMode mode) {
        return ApprovalPeriod.defaultOf(mode, LocalDate.now(), firstDayOfWeek);
    }

    private void load(boolean keepSelection) {
        ApprovalPeriod current = getPeriod();
        List<TimeEntry> entries = projectsToApprove.isEmpty()
                ? List.of()
                : approvalSupport.loadEntriesToApprove(projectField.getValue(), current.start(), current.end());

        overview = ApprovalOverview.of(entries, User::getDisplayName);
        roleNamesByUser = loadRoleNames(entries);

        updateTeamProgress();
        showRoster(keepSelection);
    }

    /**
     * Collects the names of each person's roles on the projects of their entries. A person who is not a participant
     * of those projects, such as a former participant, has none.
     */
    private Map<UUID, List<String>> loadRoleNames(List<TimeEntry> entries) {
        Map<UUID, Project> projects = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> projectIdsByUser = new HashMap<>();
        for (TimeEntry entry : entries) {
            Project project = entry.getTask().getProject();
            projects.putIfAbsent(project.getId(), project);
            projectIdsByUser.computeIfAbsent(entry.getUser().getId(), id -> new HashSet<>())
                    .add(project.getId());
        }

        Map<UUID, SortedSet<String>> roleNames = new HashMap<>();
        for (ProjectParticipant participant : approvalSupport.loadParticipants(projects.values())) {
            UUID userId = participant.getUser().getId();
            boolean onProjectOfEntries = projectIdsByUser.getOrDefault(userId, Set.of())
                    .contains(participant.getProject().getId());
            if (onProjectOfEntries && participant.getRole() != null) {
                roleNames.computeIfAbsent(userId, id -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
                        .add(participant.getRole().getName());
            }
        }

        Map<UUID, List<String>> result = new HashMap<>();
        roleNames.forEach((userId, names) -> result.put(userId, List.copyOf(names)));
        return result;
    }

    private void updatePeriodControls() {
        ApprovalPeriod current = getPeriod();
        updatePeriodTitle(current);

        defaultPeriodBtn.setText(messageBundle.getMessage(current.mode() == ApprovalMode.MONTH
                ? "defaultPeriodBtn.month.text"
                : "defaultPeriodBtn.week.text"));
        defaultPeriodBtn.setEnabled(!current.equals(defaultPeriod(current.mode())));
    }

    private void updatePeriodTitle(ApprovalPeriod current) {
        periodTitle.removeAll();
        // Text nodes between the parts keep the title, such as "October 2026", whole for assistive technology
        for (ApprovalPeriodTitle.Part part : periodTitles.format(current)) {
            if (part.year()) {
                Span year = new Span(part.text());
                year.setClassName("time-entry-approval-view-period-year");
                periodTitle.add(year);
            } else {
                periodTitle.add(new Text(part.text()));
            }
        }
    }

    private void updateTeamProgress() {
        int people = overview.people().size();
        // Hidden, but keeping its place, so that the toolbar is as tall as before and the roster does not move
        teamProgress.setClassName("empty", people == 0);
        if (people == 0) {
            return;
        }

        ApprovalTotals totals = overview.totals();
        teamProgressText.setText(messageBundle.formatMessage(
                PluralForms.key("teamProgress.approved", people, locale), overview.fullyApprovedPeople(), people));

        String waiting = countText("teamProgress.waiting", totals.count(TimeEntryStatus.NEW));
        int rejected = totals.count(TimeEntryStatus.REJECTED);
        teamProgressDetails.setText(rejected > 0
                ? messageBundle.formatMessage("teamProgress.details", waiting,
                countText("teamProgress.rejected", rejected))
                : waiting);

        teamProgressBar.setTotals(totals);
    }

    private void showRoster(boolean keepSelection) {
        // The fields set when the view opens report their values before there is a period to show
        if (period == null) {
            return;
        }

        // A reload keeps the selected person whose basket still has entries, even where the switch now hides them, as
        // after approving their last waiting entries: moving the selection would drop the basket without asking
        rosterPeople = overview.people().stream()
                .filter(person -> isInRoster(person) || (keepSelection && isSelected(person)
                        && basketHasEntriesOf(person)))
                .toList();

        ApprovalPerson toSelect = keepSelection && selectedPerson != null
                ? findShown(selectedPerson.getId()).orElse(null)
                : null;
        if (toSelect == null && !rosterPeople.isEmpty()) {
            toSelect = rosterPeople.getFirst();
        }
        setSelectedPerson(toSelect, keepSelection);
        rosterList.setItems(rosterPeople);
        scrollRosterToSelected(!keepSelection);

        updateEmptyState();
    }

    /**
     * Scrolls the roster to the selected person's card when the user has not seen it there: in a new roster, such as
     * of another period, or after a reload or a change of the switch or the search that selected another person or
     * moved the card. The roster keeps its items' scroll otherwise, and a card chosen by a click is in view already.
     */
    private void scrollRosterToSelected(boolean newRoster) {
        RosterPlace place = selectedPerson != null ? placeInRoster(selectedPerson.getId()) : null;
        if (place != null && (newRoster || !place.equals(rosterPlace))) {
            rosterList.scrollToIndex(place.index());
        }
        rosterPlace = place;
    }

    @Nullable
    private RosterPlace placeInRoster(UUID userId) {
        for (int index = 0; index < rosterPeople.size(); index++) {
            if (userId.equals(rosterPeople.get(index).user().getId())) {
                return new RosterPlace(userId, index);
            }
        }
        return null;
    }

    private boolean isInRoster(ApprovalPerson person) {
        if (rosterScopeField.getValue() == ApprovalRosterScope.NEEDS_REVIEW && !person.totals().hasWaiting()) {
            return false;
        }

        String search = getSearchText();
        if (search.isEmpty()) {
            return true;
        }

        // The name as the card shows it, so that a full name typed across both names matches, and the username
        User user = person.user();
        return Strings.CI.contains(ApprovalRosterCard.nameOf(user), search)
                || Strings.CI.contains(user.getUsername(), search);
    }

    private String getSearchText() {
        return StringUtils.trimToEmpty(rosterSearchField.getValue());
    }

    private Optional<ApprovalPerson> findShown(UUID userId) {
        return rosterPeople.stream().filter(person -> userId.equals(person.user().getId())).findFirst();
    }

    /**
     * The one place the selection changes: every way of selecting a person comes through here, and it shows the
     * person. In a month, the week listed stays when the same person stays selected and the caller keeps it, as a
     * reload does; otherwise the person opens on their opening week.
     */
    private void setSelectedPerson(@Nullable ApprovalPerson person, boolean keepWeek) {
        boolean samePerson = person != null && selectedPerson != null
                && selectedPerson.getId().equals(person.user().getId());
        selectedPerson = person != null ? person.user() : null;
        // Nothing to redraw, as when the roster's switch or search keeps the person; a redraw would also take the
        // focus from a week card
        if (keepWeek && person != null && person == shownPerson) {
            return;
        }
        shownPerson = person;
        keepBasketOf(person);

        if (person == null) {
            selectedWeek = null;
            personPanel.clear();
            return;
        }

        ApprovalPeriod current = getPeriod();
        if (current.mode() == ApprovalMode.WEEK) {
            selectedWeek = null;
        } else if (!(keepWeek && samePerson && selectedWeek != null && isWeekOfPeriod(selectedWeek))) {
            selectedWeek = ApprovalPersonPanel.openingWeekOf(person, current, firstDayOfWeek);
        }
        personPanel.show(person, roleNamesByUser.getOrDefault(person.user().getId(), List.of()), current,
                selectedWeek);
    }

    /**
     * Says which entries a bulk approve of a scope takes: the selected person's new entries dated in the scope, except
     * those in the reject basket. A rejected entry is left out, as it takes its own approval. The count on each
     * button, the question, and the approval itself all come from here.
     */
    private List<TimeEntry> entriesToApprove(ApprovalScope scope) {
        if (shownPerson == null) {
            return List.of();
        }

        return shownPerson.entries().stream()
                .filter(entry -> entry.getStatus() == TimeEntryStatus.NEW && scope.contains(entry.getDate()))
                .filter(entry -> !basket.containsKey(entry.getId()))
                .toList();
    }

    private void confirmApprove(ApprovalScope scope) {
        List<TimeEntry> entries = entriesToApprove(scope);
        if (shownPerson == null || entries.isEmpty()) {
            return;
        }

        int count = entries.size();
        String text = messageBundle.formatMessage(PluralForms.key("approveConfirmation", count, locale), count,
                ApprovalRosterCard.nameOf(shownPerson.user()), scopeText(scope),
                componentSupport.formatWorkTimeTotal(ApprovalTotals.of(entries).time()));

        // The header and the buttons of the standard remove confirmation
        dialogs.createOptionDialog()
                .withHeader(messages.getMessage("dialogs.Confirmation"))
                .withText(text)
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(event -> {
                                    // The framework closes the dialog only after this handler returns
                                    // (DialogsImpl.createButton, jmix-flowui 3.1.0), and a stale entry makes the
                                    // approval throw: closed first, the question does not stay open over the
                                    // reloaded screen to be answered again with the stale entries
                                    if (event.getComponent() instanceof Dialog dialog) {
                                        dialog.close();
                                    }
                                    approve(entries);
                                })
                                .withVariant(ActionVariant.PRIMARY),
                        new DialogAction(DialogAction.Type.NO))
                .open();
    }

    /**
     * Names the days of a scope as the question does: the period as a sentence names it for the whole period,
     * otherwise the dates.
     */
    private String scopeText(ApprovalScope scope) {
        ApprovalPeriod current = getPeriod();
        if (scope.from().equals(current.start()) && scope.to().equals(current.end())) {
            return periodTitles.formatInText(current);
        }
        return periodTitles.formatDates(scope.from(), scope.to());
    }

    private void approve(List<TimeEntry> entries) {
        List<TimeEntry> approved;
        try {
            approved = approvalSupport.approve(entries);
        } catch (OptimisticLockException e) {
            // Show the entries as they are now, then let the framework's handler tell the user
            reload();
            throw e;
        }

        reload();
        notifications.create(countText("approveNotification", approved.size()))
                .withType(Notifications.Type.SUCCESS)
                .show();
    }

    private void addToBasket(TimeEntry entry) {
        basket.put(entry.getId(), entry);
        basketChanged();
    }

    private void removeFromBasket(TimeEntry entry) {
        basket.remove(entry.getId());
        basketChanged();
    }

    private void emptyBasket() {
        basket.clear();
        basketChanged();
    }

    /**
     * Brings what shows the reject basket up to date with it: the entries' cards and the counts of the bulk approve,
     * the bar, and the dialog, which closes once nothing is left in the basket.
     */
    private void basketChanged() {
        personPanel.updateBasket();
        basketBar.setEntries(basket.values());
        if (basket.isEmpty()) {
            draftReason = "";
            rejectDialog.close();
        } else {
            rejectDialog.setEntries(List.copyOf(basket.values()));
        }
    }

    /**
     * Keeps in the reject basket the entries that the person shown now has and that can still be rejected, as they
     * were loaded now: the basket holds the shown person's entries only, so another person, or nobody, empties it.
     */
    private void keepBasketOf(@Nullable ApprovalPerson person) {
        if (!basket.isEmpty()) {
            Map<UUID, TimeEntry> loaded = person == null ? Map.of() : rejectableEntriesOf(person);

            List<UUID> kept = basket.keySet().stream().filter(loaded::containsKey).toList();
            basket.clear();
            kept.forEach(id -> basket.put(id, loaded.get(id)));
            if (basket.isEmpty()) {
                draftReason = "";
            }
        }
        basketBar.setEntries(basket.values());
    }

    /**
     * Tells whether the basket has an entry of the person that the period still shows and that can still be rejected:
     * what {@link #keepBasketOf} would keep.
     */
    private boolean basketHasEntriesOf(ApprovalPerson person) {
        Map<UUID, TimeEntry> loaded = rejectableEntriesOf(person);
        return basket.keySet().stream().anyMatch(loaded::containsKey);
    }

    private Map<UUID, TimeEntry> rejectableEntriesOf(ApprovalPerson person) {
        ApprovalPeriod current = getPeriod();
        return person.entries().stream()
                .filter(entry -> current.contains(entry.getDate()) && ApprovalEntryCard.canBeRejected(entry))
                .collect(Collectors.toMap(TimeEntry::getId, Function.identity()));
    }

    private void openRejectDialog() {
        if (shownPerson == null || basket.isEmpty()) {
            return;
        }

        rejectDialog.open(this, ApprovalRosterCard.nameOf(shownPerson.user()), List.copyOf(basket.values()),
                draftReason, this::removeFromBasket, this::reject);
    }

    private void reject(String reason) {
        // Closed first: a stale entry makes the rejection throw, and the dialog must not stay open over the reloaded
        // screen, to reject the stale entries again
        rejectDialog.close();

        List<TimeEntry> rejected;
        try {
            rejected = approvalSupport.reject(List.copyOf(basket.values()), reason);
        } catch (OptimisticLockException e) {
            // The basket keeps its entries as they are now, and the dialog opens again on the reason; then the
            // framework's handler tells the user
            reload();
            if (!basket.isEmpty()) {
                draftReason = reason;
            }
            throw e;
        }

        basket.clear();
        draftReason = "";
        reload();
        notifications.create(countText("rejectNotification", rejected.size()))
                .withType(Notifications.Type.SUCCESS)
                .show();
    }

    private boolean isWeekOfPeriod(LocalDate weekStart) {
        return getPeriod().weekRows(firstDayOfWeek).stream().anyMatch(row -> row.getFirst().equals(weekStart));
    }

    private boolean isSelected(ApprovalPerson person) {
        return selectedPerson != null && selectedPerson.getId().equals(person.user().getId());
    }

    /**
     * Brings a card drawn before up to date with the selection, keeping the card itself. A roster item only changes its
     * selection: a reload gives the roster new items, and their cards are created anew.
     */
    private Component updateRosterCard(Component card, ApprovalPerson person) {
        if (card instanceof ApprovalRosterCard rosterCard) {
            rosterCard.setSelected(isSelected(person));
            return rosterCard;
        }
        return createRosterCard(person);
    }

    private ApprovalRosterCard createRosterCard(ApprovalPerson person) {
        ApprovalRosterCard card = new ApprovalRosterCard(person,
                roleNamesByUser.getOrDefault(person.user().getId(), List.of()), getPeriod(),
                componentSupport, messages, locale);
        card.setSelected(isSelected(person));
        // A card drawn before the roster changed may be clicked after it; a person no longer shown is not selected
        card.addClickListener(event -> findShown(person.user().getId()).ifPresent(shown -> selectPerson(shown.user())));
        return card;
    }

    private void updateEmptyState() {
        String headerKey = null;
        String text = null;

        if (projectsToApprove.isEmpty()) {
            headerKey = "emptyState.noProjects.header";
            text = messageBundle.getMessage("emptyState.noProjects.text");
        } else if (overview.people().isEmpty()) {
            headerKey = "emptyState.noEntries.header";
            Project project = projectField.getValue();
            text = project != null
                    ? messageBundle.formatMessage("emptyState.noEntriesOnProject.text",
                    metadataTools.getInstanceName(project))
                    : messageBundle.getMessage("emptyState.noEntries.text");
        } else if (rosterPeople.isEmpty()) {
            if (getSearchText().isEmpty()) {
                // Everyone is shown under Everyone, so only Needs review can leave the roster empty unsearched
                headerKey = "emptyState.allReviewed.header";
                text = messageBundle.getMessage("emptyState.allReviewed.text");
            } else {
                headerKey = "emptyState.noMatch.header";
                text = messageBundle.getMessage(rosterScopeField.getValue() == ApprovalRosterScope.NEEDS_REVIEW
                        ? "emptyState.noMatch.needsReview.text"
                        : "emptyState.noMatch.everyone.text");
            }
        }

        boolean empty = headerKey != null;
        if (empty) {
            emptyStateHeader.setText(messageBundle.getMessage(headerKey));
            emptyStateText.setText(text);
        }
        emptyStateBox.setVisible(empty);
        personBox.setVisible(!empty);

        // A user who approves nowhere has no one to look for and no project to choose
        boolean approves = !projectsToApprove.isEmpty();
        rosterBox.setVisible(approves);
        projectField.setVisible(approves);
    }

    private String countText(String key, int count) {
        return messageBundle.formatMessage(PluralForms.key(key, count, locale), count);
    }

    /**
     * A person's card in the roster.
     *
     * @param userId the person's user
     * @param index  the place of the card among those the roster shows
     */
    private record RosterPlace(UUID userId, int index) {
    }
}

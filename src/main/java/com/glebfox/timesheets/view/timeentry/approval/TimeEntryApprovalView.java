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
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
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
import jakarta.persistence.OptimisticLockException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
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

    private Locale locale;
    private DayOfWeek firstDayOfWeek;
    private ApprovalPeriodTitle periodTitles;
    private ApprovalStatusBar teamProgressBar;
    private ApprovalPersonPanel personPanel;

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

    @Subscribe
    public void onInit(final InitEvent event) {
        locale = currentAuthentication.getLocale();
        // Weeks start as in the Calendar
        firstDayOfWeek = componentSupport.getDaysOfWeek().getFirst();

        periodTitles = new ApprovalPeriodTitle(messageBundle::getMessage, locale);
        initRoster();
        initTeamProgressBar();
        initPersonPanel();

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
                this::entriesToApprove);
        // A click on the grid goes through the same way as selectWeek from outside
        personPanel.addWeekClickListener(event -> selectWeek(event.getWeekStart()));
        personPanel.addDayClickListener(event -> {
            selectWeek(ApprovalPeriod.weekStartOf(event.getDay(), firstDayOfWeek));
            personPanel.scrollToDay(event.getDay());
        });
        personPanel.addApproveEntryClickListener(event -> approve(List.of(event.getEntry())));
        personPanel.addApproveScopeClickListener(event -> confirmApprove(event.getScope()));
        personBox.add(personPanel);
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
     * Shows another period, of the mode the switch is set to, and selects the first person shown.
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
     * Selects a person the roster shows.
     *
     * @param user the person's user
     * @throws IllegalArgumentException if the roster does not show the person
     */
    public void selectPerson(User user) {
        ApprovalPerson person = findShown(user.getId()).orElseThrow(() -> new IllegalArgumentException(
                "The roster does not show user %s".formatted(user.getId())));
        Optional<ApprovalPerson> previous = selectedPerson != null
                ? findShown(selectedPerson.getId())
                : Optional.empty();
        // Choosing the person shown again keeps their week
        setSelectedPerson(person, true);

        // Only the two cards whose mark changes are redrawn, and in place: a refresh of all the items would create
        // every card anew, and the card chosen from the keyboard would lose the focus.
        // The roster has its items, as it shows the person
        DataProvider<ApprovalPerson, ?> items = Objects.requireNonNull(rosterList.getDataProvider());
        previous.filter(other -> other != person).ifPresent(items::refreshItem);
        items.refreshItem(person);
    }

    /**
     * Loads the period again, such as after an action changed its entries. The selected person stays selected while
     * the roster still shows them; otherwise the first person shown is selected.
     */
    public void reload() {
        load(true);
    }

    @Subscribe("modeField")
    public void onModeFieldComponentValueChange(
            final ComponentValueChangeEvent<JmixRadioButtonGroup<ApprovalMode>, ApprovalMode> event) {
        ApprovalMode mode = event.getValue();
        // The mode set when the view opens, before any period, is the remembered one
        if (period == null || mode == null || mode == period.mode()) {
            return;
        }

        userSettingsService.save(MODE_SETTING, mode.name());
        setPeriod(defaultPeriod(mode));
    }

    @Subscribe(id = "previousBtn", subject = "clickListener")
    public void onPreviousBtnClick(final ClickEvent<JmixButton> event) {
        setPeriod(getPeriod().previous());
    }

    @Subscribe(id = "nextBtn", subject = "clickListener")
    public void onNextBtnClick(final ClickEvent<JmixButton> event) {
        setPeriod(getPeriod().next());
    }

    @Subscribe(id = "defaultPeriodBtn", subject = "clickListener")
    public void onDefaultPeriodBtnClick(final ClickEvent<JmixButton> event) {
        setPeriod(defaultPeriod(getPeriod().mode()));
    }

    @Subscribe("projectField")
    public void onProjectFieldComponentValueChange(
            final ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (period != null) {
            load(false);
        }
    }

    @Subscribe("rosterSearchField")
    public void onRosterSearchFieldComponentValueChange(
            final ComponentValueChangeEvent<TypedTextField<String>, String> event) {
        showRoster(true);
    }

    @Subscribe("rosterScopeField")
    public void onRosterScopeFieldComponentValueChange(
            final ComponentValueChangeEvent<JmixRadioButtonGroup<ApprovalRosterScope>, ApprovalRosterScope> event) {
        showRoster(true);
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
        teamProgress.setVisible(people > 0);
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

        rosterPeople = overview.people().stream().filter(this::isInRoster).toList();

        ApprovalPerson toSelect = keepSelection && selectedPerson != null
                ? findShown(selectedPerson.getId()).orElse(null)
                : null;
        if (toSelect == null && !rosterPeople.isEmpty()) {
            toSelect = rosterPeople.getFirst();
        }
        setSelectedPerson(toSelect, keepSelection);
        rosterList.setItems(rosterPeople);

        updateEmptyState();
    }

    private boolean isInRoster(ApprovalPerson person) {
        if (rosterScopeField.getValue() == ApprovalRosterScope.NEEDS_REVIEW && !person.totals().hasWaiting()) {
            return false;
        }

        String search = getSearchText();
        if (search.isEmpty()) {
            return true;
        }

        User user = person.user();
        return Strings.CI.contains(user.getFirstName(), search)
                || Strings.CI.contains(user.getLastName(), search)
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
     * Says which entries a bulk approve of a scope takes: the selected person's new entries dated in the scope. A
     * rejected entry is left out, as it takes its own approval. The count on each button, the question, and the
     * approval itself all come from here.
     */
    private List<TimeEntry> entriesToApprove(ApprovalScope scope) {
        if (shownPerson == null) {
            return List.of();
        }

        return shownPerson.entries().stream()
                .filter(entry -> entry.getStatus() == TimeEntryStatus.NEW && scope.contains(entry.getDate()))
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
                                .withHandler(event -> approve(entries))
                                .withVariant(ActionVariant.PRIMARY),
                        new DialogAction(DialogAction.Type.NO))
                .open();
    }

    /**
     * Names the days of a scope as the question does: the period's title for the whole period, otherwise the dates.
     */
    private String scopeText(ApprovalScope scope) {
        ApprovalPeriod current = getPeriod();
        if (scope.from().equals(current.start()) && scope.to().equals(current.end())) {
            return periodTitles.format(current).stream()
                    .map(ApprovalPeriodTitle.Part::text)
                    .collect(Collectors.joining());
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
}

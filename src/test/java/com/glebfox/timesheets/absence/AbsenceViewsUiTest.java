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

package com.glebfox.timesheets.absence;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.component.daterange.DateRangeField;
import com.glebfox.timesheets.data.renderer.UserRenderer;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.absence.AbsenceDetailView;
import com.glebfox.timesheets.view.absence.AbsenceListView;
import com.glebfox.timesheets.view.absencetype.AbsenceTypeListView;
import com.google.common.collect.Lists;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.renderer.PopoverRenderer;
import io.jmix.flowui.component.propertyfilter.PropertyFilter;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class AbsenceViewsUiTest {

    static final String NAME_PREFIX = "Absence views test ";

    static final LocalDate JAN_12 = LocalDate.of(2026, 1, 12);
    static final LocalDate JAN_16 = LocalDate.of(2026, 1, 16);
    static final LocalDate FEB_23 = LocalDate.of(2026, 2, 23);
    static final LocalDate FEB_27 = LocalDate.of(2026, 2, 27);
    static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_5 = LocalDate.of(2026, 3, 5);
    static final LocalDate MAR_6 = LocalDate.of(2026, 3, 6);
    static final LocalDate MAR_10 = LocalDate.of(2026, 3, 10);
    static final LocalDate MAR_12 = LocalDate.of(2026, 3, 12);
    static final LocalDate MAR_13 = LocalDate.of(2026, 3, 13);
    static final LocalDate MAR_14 = LocalDate.of(2026, 3, 14);
    static final LocalDate MAR_16 = LocalDate.of(2026, 3, 16);
    static final LocalDate MAR_19 = LocalDate.of(2026, 3, 19);
    static final LocalDate MAR_20 = LocalDate.of(2026, 3, 20);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;

    List<Id<?>> createdIds = new ArrayList<>();
    List<UUID> userIds = new ArrayList<>();

    @Test
    void listShowsAbsencesNewestFirst() {
        User user = saveUser();
        AbsenceType type = saveType(uniqueName());
        Absence january = saveAbsence(user, type, JAN_12, JAN_16);
        Absence march = saveAbsence(user, type, MAR_2, MAR_13);
        Absence february = saveAbsence(user, type, FEB_23, FEB_27);

        DataGrid<Absence> grid = openListGrid(user);

        assertThat(listedIds(grid)).containsExactly(march.getId(), february.getId(), january.getId());
        assertThat(grid.getColumns()).extracting(Grid.Column::getKey)
                .containsExactly("user", "type", "dates", "days", "comment");
        // The user cell as the schedule editor's Users section shows it, and the comment cut short
        assertThat(column(grid, "user").getRenderer()).isInstanceOf(UserRenderer.class);
        assertThat(column(grid, "comment").getRenderer()).isInstanceOf(PopoverRenderer.class);
    }

    @Test
    void oneDayAbsenceListsOneDate() {
        User user = saveUser();
        AbsenceType type = saveType(uniqueName());
        Absence oneDay = saveAbsence(user, type, MAR_2, MAR_2);
        Absence week = saveAbsence(user, type, JAN_12, JAN_16);

        DataGrid<Absence> grid = openListGrid(user);

        assertThat(cellText(grid, "dates", listed(grid, oneDay))).isEqualTo("02/03/2026");
        assertThat(cellText(grid, "days", listed(grid, oneDay))).isEqualTo("1 day");
        assertThat(cellText(grid, "dates", listed(grid, week))).isEqualTo("12/01/2026 – 16/01/2026");
        assertThat(cellText(grid, "days", listed(grid, week))).isEqualTo("5 days");
    }

    @Test
    void listShowsAnAbsenceOfADeletedType() {
        User user = saveUser();
        AbsenceType deletedType = saveType(uniqueName());
        AbsenceType keptType = saveType(uniqueName());
        Absence ofDeletedType = saveAbsence(user, deletedType, MAR_2, MAR_13);
        Absence ofKeptType = saveAbsence(user, keptType, MAR_16, MAR_20);
        // Removed with the usual confirmation, although an absence refers to the type
        removeThroughTheTypeList(deletedType);

        DataGrid<Absence> grid = openListGrid(user);

        assertThat(listedIds(grid)).containsExactly(ofKeptType.getId(), ofDeletedType.getId());
        Component deletedTypeCell = renderCell(grid, "type", listed(grid, ofDeletedType));
        assertThat(deletedTypeCell.getElement().getText()).isEqualTo(deletedType.getName());
        assertThat(badges(deletedTypeCell)).singleElement().satisfies(badge -> {
            assertThat(badge.getText()).isEqualTo("Deleted");
            // As BadgeStyle.NEUTRAL marks a badge
            assertThat(badge.hasClassName("aura-accent-neutral")).isTrue();
        });
        Component keptTypeCell = renderCell(grid, "type", listed(grid, ofKeptType));
        assertThat(keptTypeCell.getElement().getText()).isEqualTo(keptType.getName());
        assertThat(badges(keptTypeCell)).isEmpty();

        // A full page is sorted by the database, which orders by the type's name through a join
        CollectionLoader<Absence> loader = ViewControllerUtils.getViewData(UiTestUtils.getCurrentView())
                .getLoader("absencesDl");
        loader.setMaxResults(2);
        loader.load();
        grid.sort(GridSortOrder.asc(column(grid, "type")).build());

        assertThat(listedIds(grid)).containsExactlyInAnyOrder(ofKeptType.getId(), ofDeletedType.getId());
    }

    @Test
    void periodFilterKeepsAbsencesSharingADay() {
        User user = saveUser();
        AbsenceType type = saveType(uniqueName());
        Absence before = saveAbsence(user, type, FEB_23, FEB_27);
        Absence sharing = saveAbsence(user, type, MAR_2, MAR_13);
        Absence after = saveAbsence(user, type, MAR_14, MAR_20);
        DataGrid<Absence> grid = openListGrid(user);

        // The two dates of the period keep their names when a date hides their placeholders
        assertThat(UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(UiTestUtils.getCurrentView(),
                "fromFilterField").getAriaLabel()).hasValue("From");
        assertThat(UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(UiTestUtils.getCurrentView(),
                "toFilterField").getAriaLabel()).hasValue("To");

        applyFilter("fromFilter", MAR_10);

        // An absence that ends before the period is left out, one that starts in it or later is kept
        assertThat(listedIds(grid)).containsExactly(after.getId(), sharing.getId());

        applyFilter("toFilter", MAR_12);

        // An absence that starts after the period is left out, one that started before it and runs into it is kept
        assertThat(listedIds(grid)).containsExactly(sharing.getId());
        assertThat(listedIds(grid)).doesNotContain(before.getId());

        applyFilter("fromFilter", MAR_13);
        applyFilter("toFilter", MAR_14);

        // Both ends of the period count: an absence that ends on its first day, and one that starts on its last day
        assertThat(listedIds(grid)).containsExactly(after.getId(), sharing.getId());
    }

    @Test
    void typeFilterKeepsTheAbsencesOfTheType() {
        User user = saveUser();
        AbsenceType vacation = saveType(uniqueName());
        Absence ofVacation = saveAbsence(user, vacation, MAR_2, MAR_13);
        saveAbsence(user, saveType(uniqueName()), MAR_16, MAR_20);
        DataGrid<Absence> grid = openListGrid(user);

        applyFilter("typeFilter", vacation);

        assertThat(listedIds(grid)).containsExactly(ofVacation.getId());
    }

    @Test
    void createAndEditOpenTheEditorAsADialogAndTheListShowsTheSave() {
        User user = saveUser();
        AbsenceType type = saveType(uniqueName());
        DataGrid<Absence> grid = openListGrid(user);
        AbsenceListView list = UiTestUtils.getCurrentView();

        AbsenceDetailView creator = create(grid);

        // A dialog over the list, not a navigation away from it
        assertThat(isOpen(creator)).isTrue();
        fillMar10To20(creator, user, type);
        save(creator);

        assertThat(isOpen(creator)).isFalse();
        assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(list);
        Absence created = loadAbsences(user).getFirst();
        assertThat(listedIds(grid)).containsExactly(created.getId());

        AbsenceDetailView editor = edit(grid, created);

        assertThat(isOpen(editor)).isTrue();
        commentField(editor).setValue("Booked in January");
        save(editor);

        assertThat(isOpen(editor)).isFalse();
        assertThat(listed(grid, created).getComment()).isEqualTo("Booked in January");
        assertThat(cellText(grid, "dates", listed(grid, created))).isEqualTo("10/03/2026 – 20/03/2026");
    }

    @Test
    void closingTheDialogWithAChangeAsksFirst() {
        User user = saveUser();

        AbsenceDetailView editor = openNewEditor();
        userField(editor).setValue(user);

        // How Esc and the dialog's close button close it
        editor.closeWithDefaultAction();

        assertThat(isOpen(editor)).isTrue();
        dialogButton(unsavedChangesQuestion(), "cancel").click();
        assertThat(isOpen(editor)).isTrue();

        UiTestUtils.<JmixButton>getComponent(editor, "closeBtn").click();

        assertThat(isOpen(editor)).isTrue();
        dialogButton(unsavedChangesQuestion(), "close").click();
        assertThat(isOpen(editor)).isFalse();
        assertThat(loadAbsences(user)).isEmpty();
    }

    @Test
    void emptyLastDaySavesOneDay() {
        User user = saveUser();
        AbsenceType type = saveType(uniqueName());

        AbsenceDetailView editor = openNewEditor();
        userField(editor).setValue(user);
        typeField(editor).setValue(type);
        startPicker(editor).setTypedValue(MAR_2);

        // The line under the dates names the one day
        assertThat(datesField(editor).getHelperText()).isEqualTo("1 day, Monday");

        save(editor);

        assertThat(loadAbsences(user)).singleElement().satisfies(absence -> {
            assertThat(absence.getType()).isEqualTo(type);
            assertThat(absence.getFirstDay()).isEqualTo(MAR_2);
            assertThat(absence.getLastDay()).isEqualTo(MAR_2);
        });
    }

    @Test
    void oneDayAbsenceMovesWithItsFirstDay() {
        User user = saveUser();
        Absence saved = saveAbsence(user, saveType(uniqueName()), MAR_5, MAR_5);

        AbsenceDetailView editor = openEditor(user, saved);

        // The last day is empty, as when the editor creates a one-day absence, and opening changes nothing
        assertThat(startPicker(editor).getTypedValue()).isEqualTo(MAR_5);
        assertThat(endPicker(editor).getTypedValue()).isNull();
        assertThat(datesField(editor).getHelperText()).isEqualTo("1 day, Thursday");
        assertThat(editor.hasUnsavedChanges()).isFalse();

        startPicker(editor).setTypedValue(MAR_6);

        assertThat(endPicker(editor).isInvalid()).isFalse();
        assertThat(datesField(editor).getHelperText()).isEqualTo("1 day, Friday");

        save(editor);

        assertThat(isOpen(editor)).isFalse();
        assertThat(loadAbsences(user)).singleElement().satisfies(absence -> {
            assertThat(absence.getFirstDay()).isEqualTo(MAR_6);
            assertThat(absence.getLastDay()).isEqualTo(MAR_6);
        });
    }

    @Test
    void overlapIsRefusedWithTheMessage() {
        User user = saveUser();
        saveAbsence(user, saveType("Vacation"), MAR_2, MAR_13);
        AbsenceType type = saveType(uniqueName());

        AbsenceDetailView editor = openNewEditor();
        fillMar10To20(editor, user, type);
        assertThat(datesField(editor).getHelperText()).isEqualTo("11 days, Tuesday to Friday");
        save(editor);

        // Both dates are marked, as the range is; the absences are named once, under the field
        assertThat(datesField(editor).isInvalid()).isTrue();
        assertThat(datesField(editor).getErrorMessage())
                .isEqualTo("Overlaps an absence: 02/03/2026 – 13/03/2026 Vacation");
        assertThat(startPicker(editor).isInvalid()).isTrue();
        assertThat(endPicker(editor).isInvalid()).isTrue();
        assertThat(isOpen(editor)).isTrue();
        assertThat(loadAbsences(user)).hasSize(1);

        // A changed last day clears the refusal
        endPicker(editor).setTypedValue(MAR_19);

        assertThat(datesField(editor).isInvalid()).isFalse();
        assertThat(startPicker(editor).isInvalid()).isFalse();
        assertThat(endPicker(editor).isInvalid()).isFalse();

        // and so does a changed first day, after the range is refused again
        save(editor);
        assertThat(datesField(editor).isInvalid()).isTrue();

        startPicker(editor).setTypedValue(MAR_14);

        assertThat(datesField(editor).isInvalid()).isFalse();
        assertThat(startPicker(editor).isInvalid()).isFalse();
        assertThat(endPicker(editor).isInvalid()).isFalse();

        save(editor);

        assertThat(isOpen(editor)).isFalse();
        assertThat(loadAbsences(user)).extracting(Absence::getFirstDay, Absence::getLastDay)
                .containsExactly(tuple(MAR_2, MAR_13), tuple(MAR_14, MAR_19));
    }

    @Test
    void changingADateClearsTheOverlap() {
        User user = saveUser();
        saveAbsence(user, saveType(uniqueName()), MAR_2, MAR_13);
        AbsenceType type = saveType(uniqueName());

        AbsenceDetailView editor = openNewEditor();
        fillMar10To20(editor, user, type);
        save(editor);
        assertThat(datesField(editor).isInvalid()).isTrue();

        endPicker(editor).setTypedValue(MAR_14);

        assertThat(datesField(editor).isInvalid()).isFalse();
    }

    @Test
    void changingTheUserClearsTheOverlap() {
        User user = saveUser();
        saveAbsence(user, saveType(uniqueName()), MAR_2, MAR_13);
        User userWithoutAbsences = saveUser();
        AbsenceType type = saveType(uniqueName());

        AbsenceDetailView editor = openNewEditor();
        fillMar10To20(editor, user, type);
        save(editor);
        assertThat(datesField(editor).isInvalid()).isTrue();

        userField(editor).setValue(userWithoutAbsences);

        assertThat(datesField(editor).isInvalid()).isFalse();
        assertThat(startPicker(editor).isInvalid()).isFalse();
        assertThat(endPicker(editor).isInvalid()).isFalse();

        save(editor);

        assertThat(loadAbsences(userWithoutAbsences)).singleElement().satisfies(absence -> {
            assertThat(absence.getFirstDay()).isEqualTo(MAR_10);
            assertThat(absence.getLastDay()).isEqualTo(MAR_20);
        });
        assertThat(loadAbsences(user)).hasSize(1);
    }

    @Test
    void changingTheUserOnANewAbsenceMarksNoDates() {
        User user = saveUser();

        AbsenceDetailView editor = openNewEditor();
        // Jmix checks a required field the moment it is required, so the empty dates of a new absence carry
        // the required message from the start, as the user field does
        boolean invalidBefore = datesField(editor).isInvalid();
        String messageBefore = datesField(editor).getErrorMessage();

        userField(editor).setValue(user);

        // The change of the user touches the dates only to clear an overlap refusal, and there is none
        assertThat(datesField(editor).isInvalid()).isEqualTo(invalidBefore);
        assertThat(datesField(editor).getErrorMessage()).isEqualTo(messageBefore);
        assertThat(messageBefore).isEqualTo("Dates required");
    }

    @Test
    void lastDayBeforeFirstIsRefusedByThePicker() {
        User user = saveUser();
        Absence saved = saveAbsence(user, saveType(uniqueName()), MAR_2, MAR_13);

        AbsenceDetailView editor = openEditor(user, saved);

        // The stored days as saved, the last day's minimum from the stored first day, and nothing changed by opening
        assertThat(startPicker(editor).getTypedValue()).isEqualTo(MAR_2);
        assertThat(endPicker(editor).getTypedValue()).isEqualTo(MAR_13);
        assertThat(endPicker(editor).getMin()).isEqualTo(MAR_2);
        assertThat(datesField(editor).getHelperText()).isEqualTo("12 days, Monday to Friday");
        assertThat(editor.hasUnsavedChanges()).isFalse();

        startPicker(editor).setTypedValue(MAR_10);
        assertThat(endPicker(editor).getMin()).isEqualTo(MAR_10);

        endPicker(editor).setTypedValue(MAR_5);
        save(editor);

        // The field refuses the range with its own message, before the entity is checked
        assertThat(datesField(editor).isInvalid()).isTrue();
        assertThat(datesField(editor).getErrorMessage()).isEqualTo("The last day must not be before the first day");
        assertThat(isOpen(editor)).isTrue();
        Absence reloaded = dataManager.load(Id.of(saved)).one();
        assertThat(reloaded.getFirstDay()).isEqualTo(MAR_2);
        assertThat(reloaded.getLastDay()).isEqualTo(MAR_13);
    }

    @Test
    void lastDayPickerOpensAtTheFirstDay() {
        User user = saveUser();
        Absence saved = saveAbsence(user, saveType(uniqueName()), JAN_12, JAN_16);

        // An empty last day's picker opens where the first day is, not at today's month, which is far from
        // a past absence; with a last day, the picker opens at that day anyway
        AbsenceDetailView created = openNewEditor();
        startPicker(created).setTypedValue(MAR_10);

        assertThat(endPicker(created).getInitialPosition()).isEqualTo(MAR_10);

        startPicker(created).setTypedValue(FEB_23);

        assertThat(endPicker(created).getInitialPosition()).isEqualTo(FEB_23);
        created.closeWithDiscard();

        // and from the stored first day when the absence opens
        AbsenceDetailView edited = openEditor(user, saved);

        assertThat(endPicker(edited).getInitialPosition()).isEqualTo(JAN_12);
    }

    @Test
    void datesFollowTheReadOnlyMode() {
        User user = saveUser();
        Absence saved = saveAbsence(user, saveType(uniqueName()), MAR_2, MAR_13);

        AbsenceDetailView editor = openEditor(user, saved);
        assertThat(datesField(editor).isReadOnly()).isFalse();

        // As for a user who may not update the absence
        editor.setReadOnly(true);
        assertThat(datesField(editor).isReadOnly()).isTrue();
        assertThat(startPicker(editor).isReadOnly()).isTrue();
        assertThat(endPicker(editor).isReadOnly()).isTrue();

        editor.setReadOnly(false);
        assertThat(datesField(editor).isReadOnly()).isFalse();
        assertThat(startPicker(editor).isReadOnly()).isFalse();
        assertThat(endPicker(editor).isReadOnly()).isFalse();
    }

    @Test
    void deletedTypeShowsItsHelper() {
        User user = saveUser();
        AbsenceType deletedType = saveType(uniqueName());
        Absence absence = saveAbsence(user, deletedType, MAR_2, MAR_13);
        dataManager.remove(deletedType);

        AbsenceDetailView editor = openEditor(user, absence);

        assertThat(typeField(editor).getValue()).isNotNull()
                .extracting(AbsenceType::getId).isEqualTo(deletedType.getId());
        assertThat(typeField(editor).getHelperText()).isEqualTo("This type is deleted");

        // The absence saves as it is, with its deleted type
        commentField(editor).setValue("Booked in January");
        save(editor);

        Absence reloaded = dataManager.load(Id.of(absence))
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE).add("type", FetchPlan.BASE))
                .one();
        assertThat(reloaded.getComment()).isEqualTo("Booked in January");
        assertThat(reloaded.getType().getId()).isEqualTo(deletedType.getId());

        // The field offers only non-deleted types, so another choice is not deleted
        AbsenceDetailView reopened = openEditor(user, absence);
        assertThat(typeField(reopened).getHelperText()).isEqualTo("This type is deleted");
        typeField(reopened).setValue(saveType(uniqueName()));

        assertThat(typeField(reopened).getHelperText()).isNull();
    }

    @AfterEach
    void tearDown() {
        // The absences saved through the editor too, before the users and types they refer to
        if (!userIds.isEmpty()) {
            List<Absence> absences = dataManager.load(Absence.class)
                    .query("e.user.id in :userIds")
                    .parameter("userIds", userIds)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list();
            if (!absences.isEmpty()) {
                dataManager.save(new SaveContext()
                        .removing(absences.toArray())
                        .setHint(PersistenceHints.SOFT_DELETION, false));
            }
        }
        // Reload by id right before removal: soft deletes may have changed a row after it was saved
        for (Id<?> id : Lists.reverse(createdIds)) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
        userIds.clear();
    }

    private DataGrid<Absence> openListGrid() {
        viewBuilders.view(UiTestUtils.getCurrentView(), AbsenceListView.class).open();
        View<?> currentView = UiTestUtils.getCurrentView();
        assertThat(currentView).isInstanceOf(AbsenceListView.class);
        return UiTestUtils.getComponent(currentView, "absencesDataGrid");
    }

    /**
     * Opens the absence list filtered to one user's absences.
     */
    private DataGrid<Absence> openListGrid(User user) {
        DataGrid<Absence> grid = openListGrid();
        applyFilter("userFilter", user);
        return grid;
    }

    private static <V> void applyFilter(String filterId, V value) {
        PropertyFilter<V> filter = UiTestUtils.getComponent(UiTestUtils.getCurrentView(), filterId);
        filter.setValue(value);
        // A value set from the server does not apply the filter by itself
        filter.apply();
    }

    private AbsenceDetailView openNewEditor() {
        return create(openListGrid());
    }

    private static AbsenceDetailView create(DataGrid<Absence> grid) {
        Action create = Objects.requireNonNull(grid.getAction("create"));

        assertThat(create.isEnabled()).isTrue();
        create.actionPerform(grid);
        return openedEditor();
    }

    private AbsenceDetailView openEditor(User user, Absence absence) {
        return edit(openListGrid(user), absence);
    }

    private static AbsenceDetailView edit(DataGrid<Absence> grid, Absence absence) {
        Action edit = Objects.requireNonNull(grid.getAction("edit"));

        grid.select(listed(grid, absence));
        assertThat(edit.isEnabled()).isTrue();
        edit.actionPerform(grid);
        return openedEditor();
    }

    /**
     * Returns the absence editor that the list opened as a dialog over itself.
     */
    private static AbsenceDetailView openedEditor() {
        View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
        assertThat(dialog).as("the absence editor opened as a dialog").isInstanceOf(AbsenceDetailView.class);
        return (AbsenceDetailView) dialog;
    }

    private static boolean isOpen(AbsenceDetailView editor) {
        return UiTestUtils.getOpenedViewDialogs().contains(editor);
    }

    private void removeThroughTheTypeList(AbsenceType type) {
        viewBuilders.view(UiTestUtils.getCurrentView(), AbsenceTypeListView.class).open();
        AbsenceTypeListView view = UiTestUtils.getCurrentView();
        applyFilter("nameFilter", type.getName());
        DataGrid<AbsenceType> grid = UiTestUtils.getComponent(view, "absenceTypesDataGrid");
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(item -> item.getId().equals(type.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(type.getName() + " in the type list")));
        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);
        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a remove confirmation");
        dialogButton(confirmation, "yes").click();

        assertThat(dataManager.load(Id.of(type)).optional()).isEmpty();
    }

    /**
     * Fills the editor with an absence of 10–20 Mar, which overlaps one of 2–13 Mar.
     */
    private static void fillMar10To20(AbsenceDetailView editor, User user, AbsenceType type) {
        userField(editor).setValue(user);
        typeField(editor).setValue(type);
        startPicker(editor).setTypedValue(MAR_10);
        endPicker(editor).setTypedValue(MAR_20);
    }

    private static void save(AbsenceDetailView editor) {
        UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();
    }

    private static EntityComboBox<User> userField(AbsenceDetailView editor) {
        return UiTestUtils.getComponent(editor, "userField");
    }

    private static EntityComboBox<AbsenceType> typeField(AbsenceDetailView editor) {
        return UiTestUtils.getComponent(editor, "typeField");
    }

    private static DateRangeField datesField(AbsenceDetailView editor) {
        return UiTestUtils.getComponent(editor, "datesField");
    }

    private static TypedDatePicker<LocalDate> startPicker(AbsenceDetailView editor) {
        return datesField(editor).getStartPicker();
    }

    private static TypedDatePicker<LocalDate> endPicker(AbsenceDetailView editor) {
        return datesField(editor).getEndPicker();
    }

    private static JmixTextArea commentField(AbsenceDetailView editor) {
        return UiTestUtils.getComponent(editor, "commentField");
    }

    private static Grid.Column<Absence> column(DataGrid<Absence> grid, String key) {
        return Objects.requireNonNull(grid.getColumnByKey(key), key + " column");
    }

    private static Component renderCell(DataGrid<Absence> grid, String key, Absence absence) {
        ComponentRenderer<?, Absence> renderer = (ComponentRenderer<?, Absence>) column(grid, key).getRenderer();
        return renderer.createComponent(absence);
    }

    private static String cellText(DataGrid<Absence> grid, String key, Absence absence) {
        return renderCell(grid, key, absence).getElement().getTextRecursively();
    }

    private static List<Badge> badges(Component cell) {
        return cell.getChildren()
                .filter(Badge.class::isInstance)
                .map(Badge.class::cast)
                .toList();
    }

    private static List<UUID> listedIds(DataGrid<Absence> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .map(Absence::getId)
                .toList();
    }

    private static Absence listed(DataGrid<Absence> grid, Absence absence) {
        Optional<Absence> listed = Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(item -> item.getId().equals(absence.getId()))
                .findFirst();
        assertThat(listed).as("absence %s in the list", absence.getId()).isPresent();
        return listed.get();
    }

    private static DialogInfo unsavedChangesQuestion() {
        return Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(), "a dialog about the unsaved changes");
    }

    private static Button dialogButton(DialogInfo dialog, String id) {
        return dialog.getButtons().stream()
                .filter(button -> id.equals(button.getId().orElse(null)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No '" + id + "' button in the dialog"));
    }

    private static String uniqueName() {
        return NAME_PREFIX + UUID.randomUUID();
    }

    private List<Absence> loadAbsences(User user) {
        return dataManager.load(Absence.class)
                .query("e.user = :user order by e.firstDay")
                .parameter("user", user)
                .list();
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername("absence-views-test-" + UUID.randomUUID());
        User saved = track(dataManager.save(user));
        userIds.add(saved.getId());
        return saved;
    }

    private AbsenceType saveType(String name) {
        AbsenceType type = dataManager.create(AbsenceType.class);
        type.setName(name);
        return track(dataManager.save(type));
    }

    private Absence saveAbsence(User user, AbsenceType type, LocalDate firstDay, LocalDate lastDay) {
        Absence absence = dataManager.create(Absence.class);
        absence.setUser(user);
        absence.setType(type);
        absence.setFirstDay(firstDay);
        absence.setLastDay(lastDay);
        return track(dataManager.save(absence));
    }

    private <T> T track(T entity) {
        createdIds.add(Id.of(entity));
        return entity;
    }
}

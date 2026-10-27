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

package com.glebfox.timesheets.schedule;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.view.holidaycalendar.HolidayCalendarDetailView;
import com.glebfox.timesheets.view.holidaycalendar.HolidayCalendarListView;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDayDetailView;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDaysAddView;
import com.glebfox.timesheets.view.workschedule.WorkScheduleDetailView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.propertyfilter.PropertyFilter;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.flowui.view.builder.DetailWindowClassBuilder;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class HolidayCalendarViewsUiTest {

    static final String NAME_PREFIX = "Calendar views test ";

    static final Locale RUSSIAN = Locale.of("ru", "RU");

    static final LocalDate JAN_1_2026 = LocalDate.of(2026, 1, 1);
    static final LocalDate NOV_4_2026 = LocalDate.of(2026, 11, 4);
    static final LocalDate DEC_31_2026 = LocalDate.of(2026, 12, 31);
    static final LocalDate JAN_1_2027 = LocalDate.of(2027, 1, 1);
    static final LocalDate JAN_8_2027 = LocalDate.of(2027, 1, 8);
    static final LocalDate FEB_1_2027 = LocalDate.of(2027, 2, 1);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    DatatypeFormatter datatypeFormatter;
    @Autowired
    DialogWindows dialogWindows;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    List<Id<HolidayCalendar>> calendarIds = new ArrayList<>();

    @Test
    void daysLoadNewestFirstFromTheModel() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2026, JAN_1_2027, NOV_4_2026);

        HolidayCalendarDetailView view = openCalendar(calendar);
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);

        // The order is the calendar's own, not a sort of the table
        assertThat(grid.getSortOrder()).isEmpty();
        assertThat(dates(grid)).containsExactly(JAN_1_2027, NOV_4_2026, JAN_1_2026);
    }

    @Test
    void editedDayStaysInPlace() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2026, NOV_4_2026);
        HolidayCalendarDetailView view = openCalendar(calendar);

        HolidayCalendarDayDetailView dayEditor = editDay(view, JAN_1_2026);
        dateField(dayEditor).setTypedValue(JAN_1_2027);
        clickOk(dayEditor);

        assertThat(dates(daysDataGrid(view))).containsExactly(NOV_4_2026, JAN_1_2027);
    }

    @Test
    void dateColumnShowsTheWeekday() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2027);
        HolidayCalendarDetailView view = openCalendar(calendar);
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);

        String cell = renderText(grid, "date", listed(grid, day -> JAN_1_2027.equals(day.getDate())));

        assertThat(cell).isEqualTo("Fri, 01/01/2027");
    }

    @Test
    void dateColumnFollowsTheRussianFormat() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2027);

        systemAuthenticator.runWithUser("admin", () -> {
            ((AbstractAuthenticationToken) Objects.requireNonNull(SecurityContextHelper.getAuthentication()))
                    .setDetails(ClientDetails.builder().locale(RUSSIAN).build());
            HolidayCalendarDetailView view = openCalendar(calendar);
            DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);

            String cell = renderText(grid, "date", listed(grid, day -> JAN_1_2027.equals(day.getDate())));

            // The date format writes the Russian weekday in lowercase
            assertThat(cell).isEqualTo("пт, 01.01.2027");
        });
    }

    @Test
    void dateAndTypeColumnsAreAutoWidth() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);

        for (String key : List.of("date", "type")) {
            Grid.Column<HolidayCalendarDay> column = Objects.requireNonNull(grid.getColumnByKey(key));
            assertThat(column.isAutoWidth()).as(key).isTrue();
            assertThat(column.getWidth()).as(key).isNull();
            // The column keeps its content width, and the name column takes the rest
            assertThat(column.getFlexGrow()).as(key).isZero();
        }
    }

    @Test
    void columnsAreMeasuredAgainWhenDaysAreAddedOrEdited() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), NOV_4_2026);
        HolidayCalendarDetailView view = openCalendar(calendar);
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);
        // Drops what opening the editor queued for the browser
        browserCalls(grid);

        // An added day can have a wider date or the longer type than the rows the table was sized to
        addNewYear(view);
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("recalculateColumnWidths"));

        // So can an edited one
        HolidayCalendarDayDetailView dayEditor = editDay(view, NOV_4_2026);
        UiTestUtils.<JmixRadioButtonGroup<HolidayCalendarDayType>>getComponent(dayEditor, "typeField")
                .setValue(HolidayCalendarDayType.WORKDAY);
        browserCalls(grid);
        clickOk(dayEditor);
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("recalculateColumnWidths"));
    }

    @Test
    void dayEditorRefusesADateTheCalendarHas() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2026, JAN_1_2027);
        HolidayCalendarDetailView view = openCalendar(calendar);

        HolidayCalendarDayDetailView dayEditor = editDay(view, JAN_1_2026);
        dateField(dayEditor).setTypedValue(JAN_1_2027);
        clickOk(dayEditor);

        assertThat(dateField(dayEditor).isInvalid()).isTrue();
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dayEditor);
        assertThat(dates(daysDataGrid(view))).containsExactly(JAN_1_2027, JAN_1_2026);

        // The day's own date is not a duplicate
        dateField(dayEditor).setTypedValue(JAN_1_2026);
        nameField(dayEditor).setTypedValue("Renamed");
        clickOk(dayEditor);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dayEditor);
        assertThat(listed(daysDataGrid(view), day -> JAN_1_2026.equals(day.getDate())).getName())
                .isEqualTo("Renamed");
    }

    @Test
    void dayEditorKeepsTheDaysItWasGiven() {
        HolidayCalendarDay otherDay = dataManager.create(HolidayCalendarDay.class);
        otherDay.setDate(JAN_1_2027);
        List<HolidayCalendarDay> days = new ArrayList<>(List.of(otherDay));
        HolidayCalendarDayDetailView dayEditor = newDayEditor()
                .withViewConfigurer(view -> view.setCalendarDays(days))
                .open()
                .getView();

        // A change of the given collection after the editor opens does not reach it
        days.clear();
        dateField(dayEditor).setTypedValue(JAN_1_2027);
        clickOk(dayEditor);

        assertThat(dateField(dayEditor).isInvalid()).isTrue();
        assertThat(dateField(dayEditor).getErrorMessage()).isEqualTo("This day is already in the calendar");
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dayEditor);
    }

    @Test
    void dayEditorRefusesNoCalendarDays() {
        HolidayCalendarDayDetailView dayEditor = newDayEditor().build().getView();

        assertThatThrownBy(() -> dayEditor.setCalendarDays(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void daysSaveWithTheCalendarAndCancelDiscards() {
        HolidayCalendar calendar = saveCalendar(uniqueName());

        HolidayCalendarDetailView discarded = openCalendar(calendar);
        addNewYear(discarded);
        assertThat(dates(daysDataGrid(discarded))).containsExactly(JAN_1_2027);

        UiTestUtils.<JmixButton>getComponent(discarded, "closeBtn").click();
        DialogInfo saveConfirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a dialog about the unsaved changes");
        dialogButton(saveConfirmation, "close").click();

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(discarded);
        assertThat(loadAllDays(calendar)).isEmpty();

        HolidayCalendarDetailView saved = openCalendar(calendar);
        addNewYear(saved);
        UiTestUtils.<JmixButton>getComponent(saved, "saveAndCloseBtn").click();

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(saved);
        assertThat(loadAllDays(calendar)).singleElement().satisfies(day -> {
            assertThat(day.getDate()).isEqualTo(JAN_1_2027);
            assertThat(day.getName()).isEqualTo("New Year");
            assertThat(day.getType()).isEqualTo(HolidayCalendarDayType.DAY_OFF);
            assertThat(day.getDeletedDate()).isNull();
        });
    }

    @Test
    void removeTakesEverySelectedDayOnOk() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2026, NOV_4_2026, JAN_1_2027);
        HolidayCalendarDetailView view = openCalendar(calendar);
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(listed(grid, day -> JAN_1_2026.equals(day.getDate())));
        grid.select(listed(grid, day -> NOV_4_2026.equals(day.getDate())));
        assertThat(grid.getSelectedItems()).hasSize(2);
        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);
        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a remove confirmation");
        dialogButton(confirmation, "yes").click();

        assertThat(dates(grid)).containsExactly(JAN_1_2027);
        // Nothing is removed until the calendar's OK
        assertThat(loadDays(calendar)).hasSize(3);

        UiTestUtils.<JmixButton>getComponent(view, "saveAndCloseBtn").click();

        assertThat(loadDays(calendar)).extracting(HolidayCalendarDay::getDate).containsExactly(JAN_1_2027);
    }

    @Test
    void filledUntilShowsTheLatestDay() {
        HolidayCalendar filled = saveCalendar(uniqueName(), DEC_31_2026, JAN_8_2027);
        // A removed day does not count
        dataManager.remove(saveDay(filled, FEB_1_2027));
        HolidayCalendar empty = saveCalendar(uniqueName());

        DataGrid<HolidayCalendar> grid = openListGrid();

        assertThat(renderText(grid, "filledUntil", listed(grid, byId(filled))))
                .isEqualTo(datatypeFormatter.formatLocalDate(JAN_8_2027));
        assertThat(renderText(grid, "filledUntil", listed(grid, byId(empty)))).isEmpty();
    }

    @Test
    void filledUntilFollowsAnEditorSave() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), DEC_31_2026);
        DataGrid<HolidayCalendar> grid = openListGrid();
        Action edit = Objects.requireNonNull(grid.getAction("edit"));

        grid.select(listed(grid, byId(calendar)));
        assertThat(edit.isEnabled()).isTrue();
        edit.actionPerform(grid);

        HolidayCalendarDetailView editor = UiTestUtils.getCurrentView();
        HolidayCalendarDayDetailView dayEditor = editDay(editor, DEC_31_2026);
        dateField(dayEditor).setTypedValue(JAN_8_2027);
        clickOk(dayEditor);
        UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();

        View<?> currentView = UiTestUtils.getCurrentView();
        assertThat(currentView).isInstanceOf(HolidayCalendarListView.class);
        DataGrid<HolidayCalendar> shownGrid = UiTestUtils.getComponent(currentView, "holidayCalendarsDataGrid");
        assertThat(renderText(shownGrid, "filledUntil", listed(shownGrid, byId(calendar))))
                .isEqualTo(datatypeFormatter.formatLocalDate(JAN_8_2027));
    }

    @Test
    void filledUntilSortsByTheLatestDayWithEmptyCalendarsEarliest() {
        String name = uniqueName();
        // The name order differs from both orders by Filled until
        HolidayCalendar earlier = saveCalendar(name + " A", DEC_31_2026);
        HolidayCalendar empty = saveCalendar(name + " B");
        HolidayCalendar later = saveCalendar(name + " C", JAN_1_2026, JAN_8_2027);
        DataGrid<HolidayCalendar> grid = openListGrid(name);
        Grid.Column<HolidayCalendar> filledUntil = Objects.requireNonNull(grid.getColumnByKey("filledUntil"));

        assertThat(filledUntil.isSortable()).isTrue();

        grid.sort(GridSortOrder.asc(filledUntil).build());
        assertThat(listedIds(grid)).containsExactly(empty.getId(), earlier.getId(), later.getId());

        grid.sort(GridSortOrder.desc(filledUntil).build());
        assertThat(listedIds(grid)).containsExactly(later.getId(), earlier.getId(), empty.getId());
    }

    @Test
    void nameSortsAfterFilledUntil() {
        String name = uniqueName();
        HolidayCalendar a = saveCalendar(name + " A", DEC_31_2026);
        HolidayCalendar b = saveCalendar(name + " B");
        HolidayCalendar c = saveCalendar(name + " C", JAN_8_2027);
        DataGrid<HolidayCalendar> grid = openListGrid(name);

        grid.sort(GridSortOrder.desc(Objects.requireNonNull(grid.getColumnByKey("filledUntil"))).build());
        grid.sort(GridSortOrder.desc(Objects.requireNonNull(grid.getColumnByKey("name"))).build());
        assertThat(listedIds(grid)).containsExactly(c.getId(), b.getId(), a.getId());

        grid.sort(GridSortOrder.asc(Objects.requireNonNull(grid.getColumnByKey("name"))).build());
        assertThat(listedIds(grid)).containsExactly(a.getId(), b.getId(), c.getId());
    }

    @Test
    void filledUntilSortHoldsWhenTheListReloads() {
        String name = uniqueName();
        HolidayCalendar earlier = saveCalendar(name + " A", DEC_31_2026);
        HolidayCalendar empty = saveCalendar(name + " B");
        HolidayCalendar later = saveCalendar(name + " C", JAN_8_2027);
        DataGrid<HolidayCalendar> grid = openListGrid(name);

        grid.sort(GridSortOrder.asc(Objects.requireNonNull(grid.getColumnByKey("filledUntil"))).build());
        // A changed filter reloads the list from the database
        filterByName(UiTestUtils.getCurrentView(), name + " ");

        assertThat(listedIds(grid)).containsExactly(empty.getId(), earlier.getId(), later.getId());
    }

    @Test
    void filledUntilSortsAFullPageAsLoaded() {
        String name = uniqueName();
        HolidayCalendar earlier = saveCalendar(name + " A", DEC_31_2026);
        HolidayCalendar empty = saveCalendar(name + " B");
        saveCalendar(name + " C", JAN_8_2027);
        DataGrid<HolidayCalendar> grid = openListGrid(name);
        // A page that holds fewer calendars than the list has, so a header sort goes to the loader
        CollectionLoader<HolidayCalendar> loader =
                ViewControllerUtils.getViewData(UiTestUtils.getCurrentView()).getLoader("holidayCalendarsDl");
        loader.setMaxResults(2);
        loader.load();
        Grid.Column<HolidayCalendar> filledUntil = Objects.requireNonNull(grid.getColumnByKey("filledUntil"));

        grid.sort(GridSortOrder.asc(filledUntil).build());
        assertThat(listedIds(grid)).containsExactly(empty.getId(), earlier.getId());

        grid.sort(GridSortOrder.desc(filledUntil).build());
        assertThat(listedIds(grid)).containsExactly(earlier.getId(), empty.getId());
    }

    @Test
    void scheduleEditorLooksUpACalendarInTheCalendarList() {
        HolidayCalendar calendar = saveCalendar(uniqueName());
        viewBuilders.detail(UiTestUtils.getCurrentView(), WorkSchedule.class).newEntity().open();
        WorkScheduleDetailView scheduleEditor = UiTestUtils.getCurrentView();
        EntityComboBox<HolidayCalendar> holidayCalendarField =
                UiTestUtils.getComponent(scheduleEditor, "holidayCalendarField");
        Action lookup = Objects.requireNonNull(holidayCalendarField.getAction("entityLookup"));

        assertThat(lookup.isEnabled()).isTrue();
        lookup.actionPerform(holidayCalendarField);

        View<?> lookupView = UiTestUtils.getLastOpenedViewDialog();
        assertThat(lookupView).isInstanceOf(HolidayCalendarListView.class);
        DataGrid<HolidayCalendar> grid = UiTestUtils.getComponent(lookupView, "holidayCalendarsDataGrid");
        grid.select(listed(grid, byId(calendar)));
        UiTestUtils.<JmixButton>getComponent(lookupView, "selectBtn").click();

        assertThat(holidayCalendarField.getValue()).isNotNull()
                .extracting(HolidayCalendar::getId).isEqualTo(calendar.getId());
    }

    @AfterEach
    void tearDown() {
        // Each calendar's days, those saved through the editor and those removed included, before the calendar;
        // the days are loaded right before removal, since the tests change them through the editor
        for (Id<HolidayCalendar> id : calendarIds) {
            List<HolidayCalendarDay> days = dataManager.load(HolidayCalendarDay.class)
                    .query("e.calendar.id = :calendarId")
                    .parameter("calendarId", id.getValue())
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list();
            if (!days.isEmpty()) {
                dataManager.save(new SaveContext()
                        .removing(days.toArray())
                        .setHint(PersistenceHints.SOFT_DELETION, false));
            }
            removeIfPresent(id);
        }
        calendarIds.clear();
    }

    private HolidayCalendarDetailView openCalendar(HolidayCalendar calendar) {
        viewBuilders.detail(UiTestUtils.getCurrentView(), HolidayCalendar.class).editEntity(calendar).open();
        return UiTestUtils.getCurrentView();
    }

    private DataGrid<HolidayCalendar> openListGrid() {
        viewBuilders.view(UiTestUtils.getCurrentView(), HolidayCalendarListView.class).open();
        HolidayCalendarListView view = UiTestUtils.getCurrentView();
        return UiTestUtils.getComponent(view, "holidayCalendarsDataGrid");
    }

    /**
     * Opens the calendar list filtered to the calendars whose name contains the given text.
     */
    private DataGrid<HolidayCalendar> openListGrid(String nameFilter) {
        DataGrid<HolidayCalendar> grid = openListGrid();
        filterByName(UiTestUtils.getCurrentView(), nameFilter);
        return grid;
    }

    private static void filterByName(View<?> listView, String value) {
        PropertyFilter<String> nameFilter = UiTestUtils.getComponent(listView, "nameFilter");
        nameFilter.setValue(value);
        // A value set from the server does not apply the filter by itself
        nameFilter.apply();
    }

    /**
     * Builds the day editor for a new day, opened on its own rather than from a calendar editor.
     */
    private DetailWindowClassBuilder<HolidayCalendarDay, HolidayCalendarDayDetailView> newDayEditor() {
        return dialogWindows.detail(UiTestUtils.getCurrentView(), HolidayCalendarDay.class)
                .withViewClass(HolidayCalendarDayDetailView.class)
                .newEntity();
    }

    private static HolidayCalendarDayDetailView editDay(HolidayCalendarDetailView view, LocalDate date) {
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);
        Action edit = Objects.requireNonNull(grid.getAction("edit"));

        grid.select(listed(grid, day -> date.equals(day.getDate())));
        assertThat(edit.isEnabled()).isTrue();
        edit.actionPerform(grid);

        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the day editor");
    }

    /**
     * Adds the day off "New Year" on 2027-01-01 through the days' create action and the days dialog.
     */
    private static void addNewYear(HolidayCalendarDetailView view) {
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);
        Action create = Objects.requireNonNull(grid.getAction("create"));

        assertThat(create.isEnabled()).isTrue();
        create.actionPerform(grid);

        HolidayCalendarDaysAddView daysDialog = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(),
                "the days dialog");
        UiTestUtils.<TypedTextField<String>>getComponent(daysDialog, "nameField").setTypedValue("New Year");
        UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(daysDialog, "fromField").setTypedValue(JAN_1_2027);
        UiTestUtils.<JmixRadioButtonGroup<HolidayCalendarDayType>>getComponent(daysDialog, "typeField")
                .setValue(HolidayCalendarDayType.DAY_OFF);
        UiTestUtils.<JmixButton>getComponent(daysDialog, "okBtn").click();
    }

    private static void clickOk(HolidayCalendarDayDetailView dayEditor) {
        UiTestUtils.<JmixButton>getComponent(dayEditor, "saveAndCloseBtn").click();
    }

    private static DataGrid<HolidayCalendarDay> daysDataGrid(HolidayCalendarDetailView view) {
        return UiTestUtils.getComponent(view, "daysDataGrid");
    }

    private static TypedTextField<String> nameField(HolidayCalendarDayDetailView dayEditor) {
        return UiTestUtils.getComponent(dayEditor, "nameField");
    }

    private static TypedDatePicker<LocalDate> dateField(HolidayCalendarDayDetailView dayEditor) {
        return UiTestUtils.getComponent(dayEditor, "dateField");
    }

    private static List<LocalDate> dates(DataGrid<HolidayCalendarDay> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .map(HolidayCalendarDay::getDate)
                .toList();
    }

    private static List<UUID> listedIds(DataGrid<HolidayCalendar> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .map(HolidayCalendar::getId)
                .toList();
    }

    private static <T> T listed(DataGrid<T> grid, Predicate<T> condition) {
        Optional<T> listed = Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(condition)
                .findFirst();
        assertThat(listed).as("a matching row in %s", grid.getId().orElse("the grid")).isPresent();
        return listed.get();
    }

    private static Predicate<HolidayCalendar> byId(HolidayCalendar calendar) {
        return item -> item.getId().equals(calendar.getId());
    }

    private static <T> String renderText(DataGrid<T> grid, String columnKey, T item) {
        ComponentRenderer<?, T> renderer = (ComponentRenderer<?, T>)
                Objects.requireNonNull(grid.getColumnByKey(columnKey)).getRenderer();
        return renderer.createComponent(item).getElement().getText();
    }

    private static List<String> browserCalls(DataGrid<HolidayCalendarDay> grid) {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == grid.getElement().getNode())
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
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

    private HolidayCalendar saveCalendar(String name, LocalDate... dates) {
        HolidayCalendar calendar = dataManager.create(HolidayCalendar.class);
        calendar.setName(name);

        HolidayCalendar saved = dataManager.save(calendar);
        calendarIds.add(Id.of(saved));
        for (LocalDate date : dates) {
            saveDay(saved, date);
        }
        return saved;
    }

    private HolidayCalendarDay saveDay(HolidayCalendar calendar, LocalDate date) {
        HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
        day.setCalendar(calendar);
        day.setDate(date);
        day.setType(HolidayCalendarDayType.DAY_OFF);
        day.setName("Holiday");
        return dataManager.save(day);
    }

    /**
     * Loads the non-deleted days of a calendar.
     */
    private List<HolidayCalendarDay> loadDays(HolidayCalendar calendar) {
        return dataManager.load(HolidayCalendarDay.class)
                .query("e.calendar.id = :calendarId")
                .parameter("calendarId", calendar.getId())
                .list();
    }

    /**
     * Loads the days of a calendar, deleted ones included.
     */
    private List<HolidayCalendarDay> loadAllDays(HolidayCalendar calendar) {
        return dataManager.load(HolidayCalendarDay.class)
                .query("e.calendar.id = :calendarId")
                .parameter("calendarId", calendar.getId())
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list();
    }

    private void removeIfPresent(Id<?> id) {
        dataManager.load(id)
                .hint(PersistenceHints.SOFT_DELETION, false)
                .optional()
                .ifPresent(entity -> dataManager.save(new SaveContext()
                        .removing(entity)
                        .setHint(PersistenceHints.SOFT_DELETION, false)));
    }
}

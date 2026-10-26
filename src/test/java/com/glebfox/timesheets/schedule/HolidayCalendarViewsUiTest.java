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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.TextRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    List<Id<HolidayCalendar>> calendarIds = new ArrayList<>();

    @Test
    void daysAreSortedNewestFirst() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2026, JAN_1_2027, NOV_4_2026);

        HolidayCalendarDetailView view = openCalendar(calendar);

        assertThat(dates(daysDataGrid(view))).containsExactly(JAN_1_2027, NOV_4_2026, JAN_1_2026);
    }

    @Test
    void editedDayTakesItsPlaceByDate() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2026, NOV_4_2026);
        HolidayCalendarDetailView view = openCalendar(calendar);

        HolidayCalendarDayDetailView dayEditor = editDay(view, JAN_1_2026);
        dateField(dayEditor).setTypedValue(JAN_1_2027);
        clickOk(dayEditor);

        assertThat(dates(daysDataGrid(view))).containsExactly(JAN_1_2027, NOV_4_2026);
    }

    @Test
    void dateColumnShowsTheWeekday() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), JAN_1_2027);
        HolidayCalendarDetailView view = openCalendar(calendar);
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);

        String cell = renderText(grid, "date", listed(grid, day -> JAN_1_2027.equals(day.getDate())));

        assertThat(cell).isEqualTo("Fri, " + datatypeFormatter.formatLocalDate(JAN_1_2027));
    }

    @Test
    void dateAndTypeColumnsHaveFixedWidths() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);

        // Days added or edited after the table is sized can have a wider date or the longer type,
        // so neither column's width comes from the rows it first shows
        for (String key : List.of("date", "type")) {
            Grid.Column<HolidayCalendarDay> column = Objects.requireNonNull(grid.getColumnByKey(key));
            assertThat(column.isAutoWidth()).as(key).isFalse();
            assertThat(column.getFlexGrow()).as(key).isZero();
            assertThat(column.getWidth()).as(key).endsWith("rem");
        }
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
    void dayEditorNeedsTheCalendarDays() {
        DetailWindowClassBuilder<HolidayCalendarDay, HolidayCalendarDayDetailView> builder =
                dialogWindows.detail(UiTestUtils.getCurrentView(), HolidayCalendarDay.class)
                        .withViewClass(HolidayCalendarDayDetailView.class)
                        .newEntity();

        assertThatThrownBy(builder::open)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("setCalendarDays() must be called before the view opens");
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
        TextRenderer<T> renderer = (TextRenderer<T>) Objects.requireNonNull(grid.getColumnByKey(columnKey))
                .getRenderer();
        return renderer.createComponent(item).getElement().getText();
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

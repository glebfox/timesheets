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
import com.glebfox.timesheets.action.HolidayCalendarDaysAddAction;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.view.holidaycalendar.HolidayCalendarDetailView;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDaysAddView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.flowui.view.builder.WindowBuilder;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class HolidayCalendarDaysAddUiTest {

    static final String NAME_PREFIX = "Calendar days add test ";
    static final String NEW_YEAR_HOLIDAYS = "New Year holidays";
    static final Locale RUSSIAN = Locale.of("ru", "RU");

    static final LocalDate DEC_30_2026 = LocalDate.of(2026, 12, 30);
    static final LocalDate DEC_31_2026 = LocalDate.of(2026, 12, 31);
    static final LocalDate JAN_1_2027 = LocalDate.of(2027, 1, 1);
    static final LocalDate JAN_2_2027 = LocalDate.of(2027, 1, 2);
    static final LocalDate JAN_3_2027 = LocalDate.of(2027, 1, 3);
    static final LocalDate JAN_8_2027 = LocalDate.of(2027, 1, 8);
    static final LocalDate JAN_31_2027 = LocalDate.of(2027, 1, 31);
    static final LocalDate FEB_1_2027 = LocalDate.of(2027, 2, 1);
    static final LocalDate FEB_23_2027 = LocalDate.of(2027, 2, 23);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    DatatypeFormatter datatypeFormatter;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    DialogWindows dialogWindows;

    List<Id<HolidayCalendar>> calendarIds = new ArrayList<>();

    @Test
    void addIsEnabledOnAnEmptyTable() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), HolidayCalendar.class).newEntity().open();
        HolidayCalendarDetailView view = UiTestUtils.getCurrentView();
        Action create = Objects.requireNonNull(daysDataGrid(view).getAction("create"));

        assertThat(create).isInstanceOf(HolidayCalendarDaysAddAction.class);
        assertThat(daysDc(view).getItems()).isEmpty();
        assertThat(create.isEnabled()).isTrue();
    }

    @Test
    void rangeAddsEveryDayWeekendsIncluded() {
        HolidayCalendar calendar = saveCalendar(uniqueName());
        HolidayCalendarDetailView view = openCalendar(calendar);

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        fill(dialog, NEW_YEAR_HOLIDAYS, DEC_31_2026, JAN_8_2027);
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        List<LocalDate> newYearHolidays = DEC_31_2026.datesUntil(JAN_8_2027.plusDays(1)).toList();
        // Newest first, as the table is sorted
        assertThat(dates(daysDataGrid(view))).containsExactlyElementsOf(newYearHolidays.reversed());
        assertThat(daysDc(view).getItems())
                .extracting(day -> day.getDate().getDayOfWeek())
                .contains(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(daysDc(view).getItems()).allSatisfy(day -> {
            assertThat(day.getName()).isEqualTo(NEW_YEAR_HOLIDAYS);
            assertThat(day.getType()).isEqualTo(HolidayCalendarDayType.DAY_OFF);
            assertThat(day.getCalendar()).isSameAs(view.getEditedEntity());
        });
        // Nothing is saved until the calendar's OK
        assertThat(loadAllDays(calendar)).isEmpty();

        UiTestUtils.<JmixButton>getComponent(view, "saveAndCloseBtn").click();

        assertThat(loadDays(calendar)).extracting(HolidayCalendarDay::getDate)
                .containsExactlyInAnyOrderElementsOf(newYearHolidays);
    }

    @Test
    void emptyEndAddsOneDay() {
        HolidayCalendar calendar = saveCalendar(uniqueName());
        HolidayCalendarDetailView view = openCalendar(calendar);

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        fill(dialog, "Defender of the Fatherland Day", FEB_23_2027, null);
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(daysDc(view).getItems()).singleElement().satisfies(day -> {
            assertThat(day.getDate()).isEqualTo(FEB_23_2027);
            assertThat(day.getName()).isEqualTo("Defender of the Fatherland Day");
        });
    }

    @Test
    void typeDefaultsToDayOff() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));

        HolidayCalendarDaysAddView dialog = openAddDialog(view);

        assertThat(typeField(dialog).getValue()).isEqualTo(HolidayCalendarDayType.DAY_OFF);
    }

    @Test
    void typeIsGivenToEveryDay() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        fill(dialog, "Working Saturday", JAN_2_2027, null);
        typeField(dialog).setValue(HolidayCalendarDayType.WORKDAY);
        clickOk(dialog);

        assertThat(daysDc(view).getItems()).singleElement()
                .extracting(HolidayCalendarDay::getType).isEqualTo(HolidayCalendarDayType.WORKDAY);
    }

    @Test
    void nameAndFirstDayAreRequired() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        clickOk(dialog);

        // The required messages of bound fields
        assertThat(nameField(dialog).isInvalid()).isTrue();
        assertThat(nameField(dialog).getErrorMessage()).isEqualTo("Name required");
        assertThat(fromField(dialog).isInvalid()).isTrue();
        assertThat(fromField(dialog).getErrorMessage()).isEqualTo("Dates required");
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(daysDc(view).getItems()).isEmpty();
    }

    @Test
    void endBeforeStartIsRefused() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        fill(dialog, NEW_YEAR_HOLIDAYS, JAN_8_2027, JAN_1_2027);
        clickOk(dialog);

        assertThat(toField(dialog).isInvalid()).isTrue();
        assertThat(toField(dialog).getErrorMessage()).isNotBlank();
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(daysDc(view).getItems()).isEmpty();
    }

    @Test
    void rangeLongerThan31DaysIsRefused() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        fill(dialog, "January", JAN_1_2027, FEB_1_2027);
        clickOk(dialog);

        assertThat(toField(dialog).isInvalid()).isTrue();
        assertThat(toField(dialog).getErrorMessage()).isEqualTo("No more than 31 days");
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(daysDc(view).getItems()).isEmpty();

        toField(dialog).setTypedValue(JAN_31_2027);
        assertThat(toField(dialog).isInvalid()).isFalse();
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(daysDc(view).getItems()).hasSize(31);
    }

    @Test
    void rangeOverlappingTheCalendarIsRefusedListingDates() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), DEC_31_2026);
        HolidayCalendarDetailView view = openCalendar(calendar);
        HolidayCalendarDaysAddView firstDialog = openAddDialog(view);
        fill(firstDialog, "Holiday", JAN_2_2027, null);
        clickOk(firstDialog);
        assertThat(dates(daysDataGrid(view))).containsExactly(JAN_2_2027, DEC_31_2026);

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        fill(dialog, NEW_YEAR_HOLIDAYS, DEC_30_2026, JAN_3_2027);
        clickOk(dialog);

        // The saved day and the day added in this editor, in date order
        assertThat(fromField(dialog).isInvalid()).isTrue();
        assertThat(fromField(dialog).getErrorMessage()).isEqualTo("Already in the calendar: "
                + datatypeFormatter.formatLocalDate(DEC_31_2026) + ", "
                + datatypeFormatter.formatLocalDate(JAN_2_2027));
        assertThat(toField(dialog).isInvalid()).isTrue();
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(dates(daysDataGrid(view))).containsExactly(JAN_2_2027, DEC_31_2026);

        // A changed range is checked again
        fromField(dialog).setTypedValue(JAN_3_2027);
        assertThat(fromField(dialog).isInvalid()).isFalse();
        assertThat(toField(dialog).isInvalid()).isFalse();
    }

    @Test
    void dayRemovedInTheEditorDoesNotBlockTheRange() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), DEC_31_2026);
        HolidayCalendarDetailView view = openCalendar(calendar);
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(daysDc(view).getItems().getFirst());
        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);
        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a remove confirmation");
        yesButton(confirmation).click();
        assertThat(daysDc(view).getItems()).isEmpty();

        HolidayCalendarDaysAddView dialog = openAddDialog(view);
        fill(dialog, NEW_YEAR_HOLIDAYS, DEC_31_2026, JAN_1_2027);
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(dates(grid)).containsExactly(JAN_1_2027, DEC_31_2026);

        UiTestUtils.<JmixButton>getComponent(view, "saveAndCloseBtn").click();

        assertThat(loadDays(calendar)).extracting(HolidayCalendarDay::getName)
                .containsExactly(NEW_YEAR_HOLIDAYS, NEW_YEAR_HOLIDAYS);
        assertThat(loadAllDays(calendar)).hasSize(3);
    }

    @Test
    void rangeSummaryNamesTheDays() {
        HolidayCalendarDetailView view = openCalendar(saveCalendar(uniqueName()));
        HolidayCalendarDaysAddView dialog = openAddDialog(view);

        assertThat(rangeSummary(dialog).getText()).isEmpty();

        fill(dialog, NEW_YEAR_HOLIDAYS, JAN_1_2027, JAN_8_2027);
        assertThat(rangeSummary(dialog).getText()).isEqualTo("8 days, Friday to Friday");

        toField(dialog).setTypedValue(null);
        assertThat(rangeSummary(dialog).getText()).isEqualTo("1 day, Friday");

        toField(dialog).setTypedValue(JAN_1_2027);
        assertThat(rangeSummary(dialog).getText()).isEqualTo("1 day, Friday");

        // A range that ends before it starts names no days
        toField(dialog).setTypedValue(DEC_31_2026);
        assertThat(rangeSummary(dialog).getText()).isEmpty();
    }

    @Test
    void dialogNeedsTheCalendarDays() {
        WindowBuilder<HolidayCalendarDaysAddView> builder =
                dialogWindows.view(UiTestUtils.getCurrentView(), HolidayCalendarDaysAddView.class);

        assertThatThrownBy(builder::open)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("setCalendarDays() must be called before the view opens");
    }

    @Test
    void russianTextsTakeTheCasesOfTheDays() {
        HolidayCalendar calendar = saveCalendar(uniqueName(), DEC_31_2026);

        systemAuthenticator.runWithUser("admin", () -> {
            ((AbstractAuthenticationToken) Objects.requireNonNull(SecurityContextHelper.getAuthentication()))
                    .setDetails(ClientDetails.builder().locale(RUSSIAN).build());
            HolidayCalendarDetailView view = openCalendar(calendar);
            HolidayCalendarDaysAddView dialog = openAddDialog(view);

            assertSummary(dialog, JAN_1_2027, JAN_8_2027, "8 дней, с пятницы по пятницу");
            assertSummary(dialog, JAN_1_2027, null, "1 день, пятница");
            assertSummary(dialog, LocalDate.of(2027, 1, 4), LocalDate.of(2027, 1, 6),
                    "3 дня, с понедельника по среду");
            assertSummary(dialog, LocalDate.of(2026, 12, 29), LocalDate.of(2027, 1, 18),
                    "21 день, со вторника по понедельник");
            assertSummary(dialog, DEC_30_2026, DEC_31_2026, "2 дня, со среды по четверг");
            assertSummary(dialog, LocalDate.of(2027, 1, 7), LocalDate.of(2027, 1, 12),
                    "6 дней, с четверга по вторник");
            assertSummary(dialog, LocalDate.of(2027, 1, 9), LocalDate.of(2027, 1, 10),
                    "2 дня, с субботы по воскресенье");
            assertSummary(dialog, JAN_3_2027, LocalDate.of(2027, 1, 9), "7 дней, с воскресенья по субботу");

            fill(dialog, "Новогодние каникулы", DEC_30_2026, JAN_1_2027);
            clickOk(dialog);

            assertThat(fromField(dialog).getErrorMessage()).isEqualTo("Уже есть в календаре: 31.12.2026");
        });
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

    private static HolidayCalendarDaysAddView openAddDialog(HolidayCalendarDetailView view) {
        DataGrid<HolidayCalendarDay> grid = daysDataGrid(view);
        Action create = Objects.requireNonNull(grid.getAction("create"));

        assertThat(create.isEnabled()).isTrue();
        create.actionPerform(grid);

        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the days dialog");
    }

    private static void fill(HolidayCalendarDaysAddView dialog, String name, LocalDate from, @Nullable LocalDate to) {
        nameField(dialog).setTypedValue(name);
        fromField(dialog).setTypedValue(from);
        toField(dialog).setTypedValue(to);
    }

    private static void assertSummary(HolidayCalendarDaysAddView dialog, LocalDate from, @Nullable LocalDate to,
                                      String expected) {
        fromField(dialog).setTypedValue(from);
        toField(dialog).setTypedValue(to);
        assertThat(rangeSummary(dialog).getText()).isEqualTo(expected);
    }

    private static void clickOk(HolidayCalendarDaysAddView dialog) {
        UiTestUtils.<JmixButton>getComponent(dialog, "okBtn").click();
    }

    private static DataGrid<HolidayCalendarDay> daysDataGrid(HolidayCalendarDetailView view) {
        return UiTestUtils.getComponent(view, "daysDataGrid");
    }

    private static CollectionContainer<HolidayCalendarDay> daysDc(HolidayCalendarDetailView view) {
        return ViewControllerUtils.getViewData(view).getContainer("daysDc");
    }

    private static TypedTextField<String> nameField(HolidayCalendarDaysAddView dialog) {
        return UiTestUtils.getComponent(dialog, "nameField");
    }

    private static TypedDatePicker<LocalDate> fromField(HolidayCalendarDaysAddView dialog) {
        return UiTestUtils.getComponent(dialog, "fromField");
    }

    private static TypedDatePicker<LocalDate> toField(HolidayCalendarDaysAddView dialog) {
        return UiTestUtils.getComponent(dialog, "toField");
    }

    private static JmixRadioButtonGroup<HolidayCalendarDayType> typeField(HolidayCalendarDaysAddView dialog) {
        return UiTestUtils.getComponent(dialog, "typeField");
    }

    private static Span rangeSummary(HolidayCalendarDaysAddView dialog) {
        return UiTestUtils.getComponent(dialog, "rangeSummary");
    }

    private static List<LocalDate> dates(DataGrid<HolidayCalendarDay> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .map(HolidayCalendarDay::getDate)
                .toList();
    }

    private static Button yesButton(DialogInfo dialog) {
        return dialog.getButtons().stream()
                .filter(button -> "yes".equals(button.getId().orElse(null)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No 'yes' button in the dialog"));
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
            HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
            day.setCalendar(saved);
            day.setDate(date);
            day.setType(HolidayCalendarDayType.DAY_OFF);
            day.setName("Holiday");
            dataManager.saveWithoutReload(day);
        }
        return saved;
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

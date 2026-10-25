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
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.view.workschedule.WorkScheduleDetailView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.checkboxgroup.JmixCheckboxGroup;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class WorkScheduleEditorUiTest {

    static final String NAME_PREFIX = "Editor test ";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    CurrentAuthentication currentAuthentication;

    List<Id<WorkSchedule>> scheduleIds = new ArrayList<>();
    List<Id<HolidayCalendar>> calendarIds = new ArrayList<>();

    @Test
    void zeroWorkdayHoursAreRefused() {
        WorkScheduleDetailView view = openNewSchedule();
        String name = uniqueName();
        nameField(view).setValue(name);
        workdayTimeField(view).setTypedValue(WorkTime.ZERO);

        clickOk(view);

        assertThat(loadByName(name)).isEmpty();
        assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(view);
        assertThat(workdayTimeField(view).isInvalid()).isTrue();
    }

    @Test
    void scheduleWithoutWorkdaysIsRefused() {
        WorkScheduleDetailView view = openNewSchedule();
        String name = uniqueName();
        nameField(view).setValue(name);
        workdayTimeField(view).setTypedValue(WorkTime.ofHours(8));
        workdaysField(view).clear();

        clickOk(view);

        assertThat(loadByName(name)).isEmpty();
        assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(view);
        assertThat(workdaysField(view).isInvalid()).isTrue();
    }

    @Test
    void workdaysFollowTheLocaleWeekAndBindToFlags() {
        WorkScheduleDetailView view = openNewSchedule();
        DayOfWeek firstDayOfWeek = WeekFields.of(currentAuthentication.getLocale()).getFirstDayOfWeek();

        List<DayOfWeek> items = workdaysField(view).getListDataView().getItems().toList();
        assertThat(items).containsExactly(
                firstDayOfWeek,
                firstDayOfWeek.plus(1),
                firstDayOfWeek.plus(2),
                firstDayOfWeek.plus(3),
                firstDayOfWeek.plus(4),
                firstDayOfWeek.plus(5),
                firstDayOfWeek.plus(6));

        String name = uniqueName();
        nameField(view).setValue(name);
        workdayTimeField(view).setTypedValue(WorkTime.ofHours(8));
        workdaysField(view).setValue(EnumSet.range(MONDAY, SATURDAY));
        clickOk(view);

        List<WorkSchedule> saved = loadByName(name);
        assertThat(saved).singleElement().satisfies(schedule -> {
            assertThat(schedule.getMonday()).isTrue();
            assertThat(schedule.getTuesday()).isTrue();
            assertThat(schedule.getWednesday()).isTrue();
            assertThat(schedule.getThursday()).isTrue();
            assertThat(schedule.getFriday()).isTrue();
            assertThat(schedule.getSaturday()).isTrue();
            assertThat(schedule.getSunday()).isFalse();
        });
    }

    @Test
    void workdaysFollowTheViewReadOnlyMode() {
        WorkScheduleDetailView view = openNewSchedule();

        view.setReadOnly(true);
        assertThat(workdaysField(view).isReadOnly()).isTrue();

        view.setReadOnly(false);
        assertThat(workdaysField(view).isReadOnly()).isFalse();
    }

    @Test
    void workdaysSummaryCountsTheWeek() {
        WorkScheduleDetailView view = openNewSchedule();
        Span workdaysSummary = UiTestUtils.getComponent(view, "workdaysSummary");

        workdayTimeField(view).setTypedValue(WorkTime.ofHours(8));
        workdaysField(view).setValue(EnumSet.range(MONDAY, FRIDAY));
        assertThat(workdaysSummary.getText()).isEqualTo("5 workdays, 40h a week");

        workdaysField(view).setValue(EnumSet.of(MONDAY));
        assertThat(workdaysSummary.getText()).isEqualTo("1 workday, 8h a week");

        workdayTimeField(view).setTypedValue(WorkTime.of(7, 30));
        workdaysField(view).setValue(EnumSet.range(MONDAY, FRIDAY));
        assertThat(workdaysSummary.getText()).isEqualTo("5 workdays, 37h 30m a week");
    }

    @Test
    void defaultSwitchIsReadOnlyOnTheDefault() {
        WorkSchedule defaultSchedule = dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .one();
        WorkScheduleDetailView defaultView = openSchedule(defaultSchedule);

        assertThat(isDefaultField(defaultView).getValue()).isTrue();
        assertThat(isDefaultField(defaultView).isReadOnly()).isTrue();
        assertThat(isDefaultField(defaultView).getHelperText()).isNotBlank();

        WorkScheduleDetailView otherView = openSchedule(saveSchedule(uniqueName(), null));

        assertThat(isDefaultField(otherView).getValue()).isFalse();
        assertThat(isDefaultField(otherView).isReadOnly()).isFalse();
    }

    @Test
    void scheduleOfADeletedCalendarSaves() {
        HolidayCalendar calendar = saveCalendar(uniqueName());
        WorkSchedule schedule = saveSchedule(uniqueName(), calendar);
        dataManager.remove(calendar);

        WorkScheduleDetailView view = openSchedule(schedule);
        EntityComboBox<HolidayCalendar> holidayCalendarField = holidayCalendarField(view);
        assertThat(holidayCalendarField.getValue()).isNotNull()
                .extracting(HolidayCalendar::getId).isEqualTo(calendar.getId());
        assertThat(holidayCalendarField.getHelperText()).isEqualTo("This calendar is deleted");

        String newName = uniqueName();
        nameField(view).setValue(newName);
        clickOk(view);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(view);
        WorkSchedule saved = dataManager.load(Id.of(schedule))
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("holidayCalendar", FetchPlan.INSTANCE_NAME))
                .one();
        assertThat(saved.getName()).isEqualTo(newName);
        assertThat(saved.getHolidayCalendar()).isNotNull()
                .extracting(HolidayCalendar::getId).isEqualTo(calendar.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void calendarChoiceOffersOnlyNonDeletedCalendars() {
        HolidayCalendar kept = saveCalendar(uniqueName());
        HolidayCalendar deleted = saveCalendar(uniqueName());
        dataManager.remove(deleted);

        WorkScheduleDetailView view = openNewSchedule();
        DataProvider<HolidayCalendar, String> dataProvider =
                (DataProvider<HolidayCalendar, String>) holidayCalendarField(view).getDataProvider();

        // The empty filter is what the field sends when its drop-down opens
        List<UUID> optionIds = dataProvider.fetch(new Query<>(""))
                .map(HolidayCalendar::getId)
                .toList();
        assertThat(optionIds).contains(kept.getId()).doesNotContain(deleted.getId());
    }

    @AfterEach
    void tearDown() {
        // Schedules before the calendars they refer to; each reloaded by id right before removal,
        // since the tests change and delete what they created
        for (Id<WorkSchedule> id : scheduleIds) {
            removeIfPresent(id);
        }
        for (Id<HolidayCalendar> id : calendarIds) {
            removeIfPresent(id);
        }
        scheduleIds.clear();
        calendarIds.clear();
    }

    private WorkScheduleDetailView openNewSchedule() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), WorkSchedule.class).newEntity().open();
        return UiTestUtils.getCurrentView();
    }

    private WorkScheduleDetailView openSchedule(WorkSchedule schedule) {
        viewBuilders.detail(UiTestUtils.getCurrentView(), WorkSchedule.class).editEntity(schedule).open();
        return UiTestUtils.getCurrentView();
    }

    private static void clickOk(WorkScheduleDetailView view) {
        JmixButton saveAndCloseBtn = UiTestUtils.getComponent(view, "saveAndCloseBtn");
        saveAndCloseBtn.click();
    }

    private static TypedTextField<String> nameField(WorkScheduleDetailView view) {
        return UiTestUtils.getComponent(view, "nameField");
    }

    private static TypedTextField<WorkTime> workdayTimeField(WorkScheduleDetailView view) {
        return UiTestUtils.getComponent(view, "workdayTimeField");
    }

    private static JmixCheckboxGroup<DayOfWeek> workdaysField(WorkScheduleDetailView view) {
        return UiTestUtils.getComponent(view, "workdaysField");
    }

    private static EntityComboBox<HolidayCalendar> holidayCalendarField(WorkScheduleDetailView view) {
        return UiTestUtils.getComponent(view, "holidayCalendarField");
    }

    private static Switch isDefaultField(WorkScheduleDetailView view) {
        return UiTestUtils.getComponent(view, "isDefaultField");
    }

    private static String uniqueName() {
        return NAME_PREFIX + UUID.randomUUID();
    }

    private HolidayCalendar saveCalendar(String name) {
        HolidayCalendar calendar = dataManager.create(HolidayCalendar.class);
        calendar.setName(name);

        HolidayCalendar saved = dataManager.save(calendar);
        calendarIds.add(Id.of(saved));
        return saved;
    }

    private WorkSchedule saveSchedule(String name, HolidayCalendar calendar) {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(name);
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        schedule.setHolidayCalendar(calendar);

        WorkSchedule saved = dataManager.save(schedule);
        scheduleIds.add(Id.of(saved));
        return saved;
    }

    /**
     * Loads the schedules with the given name, deleted ones included, and tracks them for removal,
     * since the editor saves the schedules that the tests create through it.
     */
    private List<WorkSchedule> loadByName(String name) {
        List<WorkSchedule> schedules = dataManager.load(WorkSchedule.class)
                .query("e.name = ?1", name)
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list();
        schedules.forEach(schedule -> scheduleIds.add(Id.of(schedule)));
        return schedules;
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

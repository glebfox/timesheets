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
import com.glebfox.timesheets.view.workschedule.WorkScheduleListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.Action;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.notification.NotificationInfo;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class WorkScheduleListUiTest {

    static final String NAME_PREFIX = "List test ";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    CurrentAuthentication currentAuthentication;

    UUID seedDefaultId;
    List<Id<WorkSchedule>> scheduleIds = new ArrayList<>();
    List<Id<HolidayCalendar>> calendarIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        seedDefaultId = dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .one()
                .getId();
    }

    @Test
    void makeDefaultMarksTheSelectedSchedule() {
        WorkSchedule schedule = saveSchedule(uniqueName(), null);
        DataGrid<WorkSchedule> grid = openListGrid();
        Action makeDefault = Objects.requireNonNull(grid.getAction("makeDefault"));

        grid.select(listed(grid, schedule.getId()));
        assertThat(makeDefault.isEnabled()).isTrue();
        makeDefault.actionPerform(grid);

        assertThat(reload(schedule.getId()).getIsDefault()).isTrue();
        assertThat(reload(seedDefaultId).getIsDefault()).isFalse();

        // The list is reloaded, so it shows the new default, and the action follows the selected row
        assertThat(listed(grid, schedule.getId()).getIsDefault()).isTrue();
        assertThat(listed(grid, seedDefaultId).getIsDefault()).isFalse();
        assertThat(makeDefault.isEnabled()).isFalse();

        NotificationInfo notification = UiTestUtils.getLastOpenedNotification();
        assertThat(notification).isNotNull();
        assertThat(notification.getType()).isEqualTo(Notifications.Type.SUCCESS);
        assertThat(notification.getText()).contains(schedule.getName());
    }

    @Test
    void makeDefaultIsDisabledForTheDefaultOrNoSelection() {
        WorkSchedule schedule = saveSchedule(uniqueName(), null);
        DataGrid<WorkSchedule> grid = openListGrid();
        Action makeDefault = Objects.requireNonNull(grid.getAction("makeDefault"));

        assertThat(makeDefault.isEnabled()).isFalse();

        grid.select(listed(grid, seedDefaultId));
        assertThat(makeDefault.isEnabled()).isFalse();

        grid.select(listed(grid, schedule.getId()));
        assertThat(makeDefault.isEnabled()).isTrue();

        grid.deselectAll();
        assertThat(makeDefault.isEnabled()).isFalse();
    }

    @Test
    void workdaysColumnFollowsTheLocaleWeek() {
        // A new schedule works Monday to Friday
        WorkSchedule schedule = saveSchedule(uniqueName(), null);
        DataGrid<WorkSchedule> grid = openListGrid();

        List<Badge> chips = renderCell(grid, "workdays", listed(grid, schedule.getId()))
                .getChildren()
                .map(Badge.class::cast)
                .toList();

        Locale locale = currentAuthentication.getLocale();
        DayOfWeek firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();
        List<DayOfWeek> week = IntStream.range(0, 7)
                .mapToObj(firstDayOfWeek::plus)
                .toList();
        assertThat(chips).extracting(Badge::getText).containsExactlyElementsOf(week.stream()
                .map(day -> StringUtils.capitalize(day.getDisplayName(TextStyle.SHORT_STANDALONE, locale)))
                .toList());
        for (int i = 0; i < week.size(); i++) {
            boolean workday = week.get(i) != SATURDAY && week.get(i) != SUNDAY;
            assertThat(isMuted(chips.get(i))).as(week.get(i).toString()).isEqualTo(!workday);
        }
    }

    @Test
    void nameColumnMarksTheDefault() {
        WorkSchedule schedule = saveSchedule(uniqueName(), null);
        DataGrid<WorkSchedule> grid = openListGrid();

        Component defaultCell = renderCell(grid, "name", listed(grid, seedDefaultId));
        assertThat(defaultCell.getChildren().filter(Badge.class::isInstance).map(Badge.class::cast))
                .singleElement()
                .satisfies(badge -> {
                    assertThat(badge.getText()).isEqualTo("Default");
                    assertThat(isMuted(badge)).isFalse();
                });

        Component otherCell = renderCell(grid, "name", listed(grid, schedule.getId()));
        assertThat(otherCell.getChildren().filter(Badge.class::isInstance)).isEmpty();
    }

    @Test
    void scheduleOfADeletedCalendarIsListedWithTheCalendar() {
        HolidayCalendar calendar = saveCalendar(uniqueName());
        WorkSchedule schedule = saveSchedule(uniqueName(), calendar);
        dataManager.remove(calendar);

        DataGrid<WorkSchedule> grid = openListGrid();

        assertThat(listed(grid, schedule.getId()).getHolidayCalendar()).isNotNull()
                .extracting(HolidayCalendar::getName).isEqualTo(calendar.getName());
    }

    @AfterEach
    void tearDown() {
        // Leave "Full time" the default, as the tests found it; marking it unmarks any test schedule
        WorkSchedule seed = reload(seedDefaultId);
        if (!Boolean.TRUE.equals(seed.getIsDefault())) {
            seed.setIsDefault(true);
            dataManager.saveWithoutReload(seed);
        }

        // Schedules before the calendars they refer to; each reloaded by id right before removal,
        // since the default listener and the tests change what they created
        for (Id<WorkSchedule> id : scheduleIds) {
            removeIfPresent(id);
        }
        for (Id<HolidayCalendar> id : calendarIds) {
            removeIfPresent(id);
        }
        scheduleIds.clear();
        calendarIds.clear();
    }

    private DataGrid<WorkSchedule> openListGrid() {
        viewBuilders.view(UiTestUtils.getCurrentView(), WorkScheduleListView.class).open();
        WorkScheduleListView view = UiTestUtils.getCurrentView();
        return UiTestUtils.getComponent(view, "workSchedulesDataGrid");
    }

    private static WorkSchedule listed(DataGrid<WorkSchedule> grid, UUID id) {
        Optional<WorkSchedule> listed = Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(schedule -> schedule.getId().equals(id))
                .findFirst();
        assertThat(listed).as("schedule %s in the list", id).isPresent();
        return listed.get();
    }

    private static Component renderCell(DataGrid<WorkSchedule> grid, String columnKey, WorkSchedule schedule) {
        ComponentRenderer<?, WorkSchedule> renderer = (ComponentRenderer<?, WorkSchedule>)
                Objects.requireNonNull(grid.getColumnByKey(columnKey)).getRenderer();
        return renderer.createComponent(schedule);
    }

    // As BadgeStyle.NEUTRAL marks a badge
    private static boolean isMuted(Badge badge) {
        return badge.hasClassName("aura-accent-neutral");
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

    private WorkSchedule saveSchedule(String name, @Nullable HolidayCalendar calendar) {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(name);
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        schedule.setHolidayCalendar(calendar);

        WorkSchedule saved = dataManager.save(schedule);
        scheduleIds.add(Id.of(saved));
        return saved;
    }

    private WorkSchedule reload(UUID id) {
        return dataManager.load(WorkSchedule.class).id(id).one();
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

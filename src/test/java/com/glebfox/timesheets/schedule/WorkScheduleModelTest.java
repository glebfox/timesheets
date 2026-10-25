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

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.MetadataTools;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.data.PersistenceHints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class WorkScheduleModelTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    MetadataTools metadataTools;
    @Autowired
    DatatypeFormatter datatypeFormatter;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void defaultScheduleIsSeeded() {
        List<WorkSchedule> defaults = dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .list();

        assertThat(defaults).hasSize(1);
        WorkSchedule fullTime = defaults.getFirst();
        assertThat(fullTime.getId()).isEqualTo(UUID.fromString("b7e2c1a4-5d3f-4e8b-9a6c-1f2e3d4c5b6a"));
        assertThat(fullTime.getName()).isEqualTo("Full time");
        assertThat(fullTime.getWorkdayTime()).isEqualTo(WorkTime.ofHours(8));
        assertThat(List.of(fullTime.getMonday(), fullTime.getTuesday(), fullTime.getWednesday(),
                fullTime.getThursday(), fullTime.getFriday())).containsOnly(true);
        assertThat(List.of(fullTime.getSaturday(), fullTime.getSunday())).containsOnly(false);
        assertThat(fullTime.getHolidayCalendar()).isNull();
    }

    @Test
    void newScheduleDefaultsToMondayToFriday() {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);

        assertThat(List.of(schedule.getMonday(), schedule.getTuesday(), schedule.getWednesday(),
                schedule.getThursday(), schedule.getFriday())).containsOnly(true);
        assertThat(List.of(schedule.getSaturday(), schedule.getSunday())).containsOnly(false);
        assertThat(schedule.getIsDefault()).isFalse();
    }

    @Test
    void entitiesPersistAndLoad() {
        /* Given */
        User user = dataManager.create(User.class);
        user.setUsername("work-schedule-model-test-" + System.currentTimeMillis());

        HolidayCalendar calendar = dataManager.create(HolidayCalendar.class);
        calendar.setName("Model test calendar");
        calendar.setDescription("Holidays of the model test");

        HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
        day.setCalendar(calendar);
        day.setDate(LocalDate.of(2026, 11, 4));
        day.setType(HolidayCalendarDayType.DAY_OFF);
        day.setName("Unity Day");

        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName("Model test schedule");
        schedule.setDescription("Half of a full-time rate");
        schedule.setWorkdayTime(WorkTime.ofHours(4));
        schedule.setFriday(false);
        schedule.setSaturday(true);
        schedule.setHolidayCalendar(calendar);

        WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
        assignment.setUser(user);
        assignment.setWorkSchedule(schedule);
        assignment.setStartDate(LocalDate.of(2026, 11, 9));

        dataManager.save(new SaveContext().saving(user, calendar, day, schedule, assignment));
        track(user, calendar, day, schedule, assignment);

        /* When */
        HolidayCalendar loadedCalendar = dataManager.load(HolidayCalendar.class)
                .id(calendar.getId())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("days", FetchPlan.BASE))
                .one();
        WorkSchedule loadedSchedule = dataManager.load(WorkSchedule.class)
                .id(schedule.getId())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("holidayCalendar", FetchPlan.BASE))
                .one();
        WorkScheduleAssignment loadedAssignment = dataManager.load(WorkScheduleAssignment.class)
                .id(assignment.getId())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("workSchedule", FetchPlan.BASE))
                .one();

        /* Then */
        assertThat(loadedCalendar.getName()).isEqualTo("Model test calendar");
        assertThat(loadedCalendar.getDescription()).isEqualTo("Holidays of the model test");
        assertThat(loadedCalendar.getDays()).singleElement().satisfies(loadedDay -> {
            assertThat(loadedDay.getId()).isEqualTo(day.getId());
            assertThat(loadedDay.getDate()).isEqualTo(LocalDate.of(2026, 11, 4));
            assertThat(loadedDay.getType()).isEqualTo(HolidayCalendarDayType.DAY_OFF);
            assertThat(loadedDay.getName()).isEqualTo("Unity Day");
        });

        assertThat(loadedSchedule.getName()).isEqualTo("Model test schedule");
        assertThat(loadedSchedule.getDescription()).isEqualTo("Half of a full-time rate");
        assertThat(loadedSchedule.getWorkdayTime()).isEqualTo(WorkTime.ofHours(4));
        assertThat(List.of(loadedSchedule.getMonday(), loadedSchedule.getTuesday(), loadedSchedule.getWednesday(),
                loadedSchedule.getThursday(), loadedSchedule.getSaturday())).containsOnly(true);
        assertThat(List.of(loadedSchedule.getFriday(), loadedSchedule.getSunday())).containsOnly(false);
        assertThat(loadedSchedule.getHolidayCalendar()).isEqualTo(calendar);
        assertThat(loadedSchedule.getIsDefault()).isFalse();

        assertThat(loadedAssignment.getUser()).isEqualTo(user);
        assertThat(loadedAssignment.getWorkSchedule()).isEqualTo(schedule);
        assertThat(loadedAssignment.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void holidayCalendarDayNameIsRequired() {
        HolidayCalendar calendar = dataManager.create(HolidayCalendar.class);
        calendar.setName("Name required test calendar");
        HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
        day.setCalendar(calendar);
        day.setDate(LocalDate.of(2026, 11, 4));
        day.setType(HolidayCalendarDayType.DAY_OFF);
        dataManager.saveWithoutReload(calendar);
        track(calendar, day);

        assertThatThrownBy(() -> dataManager.save(day)).isNotNull();
    }

    @Test
    void holidayCalendarDayIsShownByDateNameAndType() {
        HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
        day.setDate(LocalDate.of(2026, 11, 4));
        day.setName("Unity Day");
        day.setType(HolidayCalendarDayType.DAY_OFF);

        assertThat(metadataTools.getInstanceName(day))
                .isEqualTo(datatypeFormatter.formatLocalDate(LocalDate.of(2026, 11, 4)) + " Unity Day (Day off)");
    }

    @AfterEach
    void tearDown() {
        // Reload by id right before removal: listeners and soft deletes may have changed a row after it was saved
        for (Id<?> id : Lists.reverse(createdIds)) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    private void track(Object... entities) {
        for (Object entity : entities) {
            createdIds.add(Id.of(entity));
        }
    }
}

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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
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

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class WorkScheduleAssignmentSupportTest {

    static final LocalDate TODAY = LocalDate.of(2026, 12, 15);

    @Autowired
    DataManager dataManager;
    @Autowired
    WorkScheduleAssignmentSupport workScheduleAssignmentSupport;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void currentIsTheLatestStartOnOrBeforeToday() {
        User user = newUser();
        WorkScheduleAssignment march = newAssignment(user, LocalDate.of(2026, 3, 1));
        WorkScheduleAssignment november = newAssignment(user, LocalDate.of(2026, 11, 1));
        WorkScheduleAssignment february = newAssignment(user, LocalDate.of(2027, 2, 1));

        assertThat(workScheduleAssignmentSupport.findCurrent(List.of(march, february, november), TODAY))
                .containsExactly(november);
    }

    @Test
    void futureOnlyHasNoCurrent() {
        WorkScheduleAssignment february = newAssignment(newUser(), LocalDate.of(2027, 2, 1));

        assertThat(workScheduleAssignmentSupport.findCurrent(List.of(february), TODAY)).isEmpty();
    }

    @Test
    void currentIsPerUser() {
        User anderson = newUser();
        User smith = newUser();
        WorkScheduleAssignment andersonMarch = newAssignment(anderson, LocalDate.of(2026, 3, 1));
        WorkScheduleAssignment andersonNovember = newAssignment(anderson, LocalDate.of(2026, 11, 1));
        WorkScheduleAssignment smithJune = newAssignment(smith, LocalDate.of(2026, 6, 1));
        // Starting today counts as current
        WorkScheduleAssignment smithToday = newAssignment(smith, TODAY);
        WorkScheduleAssignment smithFebruary = newAssignment(smith, LocalDate.of(2027, 2, 1));

        assertThat(workScheduleAssignmentSupport.findCurrent(
                List.of(andersonMarch, smithJune, andersonNovember, smithFebruary, smithToday), TODAY))
                .containsExactlyInAnyOrder(andersonNovember, smithToday);
    }

    @Test
    void assignmentsFromADateAreFound() {
        LocalDate november = LocalDate.of(2026, 11, 1);
        User anderson = saveUser();
        User smith = saveUser();
        User batty = saveUser();
        WorkSchedule deletedSchedule = saveSchedule();
        WorkScheduleAssignment andersonNovember = saveAssignment(anderson, deletedSchedule, november);
        saveAssignment(anderson, saveSchedule(), LocalDate.of(2026, 12, 1));
        remove(saveAssignment(smith, saveSchedule(), november));
        saveAssignment(batty, saveSchedule(), november);
        remove(deletedSchedule);

        List<WorkScheduleAssignment> found = workScheduleAssignmentSupport.loadAssignmentsFrom(
                List.of(anderson, smith), november);

        // A deleted assignment does not count; an assignment of a deleted schedule does
        assertThat(found).extracting(WorkScheduleAssignment::getId).containsExactly(andersonNovember.getId());
    }

    @Test
    void assignmentsOfUsersKeepDeletedSchedulesAndSkipDeletedAssignments() {
        User anderson = saveUser();
        User smith = saveUser();
        User batty = saveUser();
        WorkSchedule deletedSchedule = saveSchedule();
        WorkSchedule otherSchedule = saveSchedule();
        WorkScheduleAssignment andersonMarch = saveAssignment(anderson, deletedSchedule, LocalDate.of(2026, 3, 1));
        WorkScheduleAssignment andersonNovember = saveAssignment(anderson, otherSchedule, LocalDate.of(2026, 11, 1));
        WorkScheduleAssignment smithJune = saveAssignment(smith, otherSchedule, LocalDate.of(2026, 6, 1));
        remove(saveAssignment(smith, otherSchedule, LocalDate.of(2026, 12, 1)));
        saveAssignment(batty, otherSchedule, LocalDate.of(2026, 6, 1));
        remove(deletedSchedule);

        List<WorkScheduleAssignment> found = workScheduleAssignmentSupport.loadAssignmentsOfUsers(
                List.of(anderson, smith));

        assertThat(found).extracting(WorkScheduleAssignment::getId)
                .containsExactlyInAnyOrder(andersonMarch.getId(), andersonNovember.getId(), smithJune.getId());
        WorkScheduleAssignment ofDeletedSchedule = found.stream()
                .filter(assignment -> assignment.getId().equals(andersonMarch.getId()))
                .findFirst().orElseThrow();
        assertThat(ofDeletedSchedule.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(ofDeletedSchedule.getUser().getUsername()).isEqualTo(anderson.getUsername());
        assertThat(ofDeletedSchedule.getWorkSchedule().getName()).isEqualTo(deletedSchedule.getName());
    }

    @AfterEach
    void tearDown() {
        // Reload by id right before removal: soft deletes in the tests changed rows after they were saved
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

    private User newUser() {
        User user = dataManager.create(User.class);
        user.setUsername("assignment-support-test-" + UUID.randomUUID());
        return user;
    }

    private WorkScheduleAssignment newAssignment(User user, LocalDate startDate) {
        WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
        assignment.setUser(user);
        assignment.setStartDate(startDate);
        return assignment;
    }

    private User saveUser() {
        return save(newUser());
    }

    private WorkSchedule saveSchedule() {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName("Assignment support test " + UUID.randomUUID());
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        return save(schedule);
    }

    private WorkScheduleAssignment saveAssignment(User user, WorkSchedule schedule, LocalDate startDate) {
        WorkScheduleAssignment assignment = newAssignment(user, startDate);
        assignment.setWorkSchedule(schedule);
        return save(assignment);
    }

    private <T> T save(T entity) {
        T saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void remove(Object entity) {
        dataManager.remove(dataManager.load(Id.of(entity)).one());
    }
}

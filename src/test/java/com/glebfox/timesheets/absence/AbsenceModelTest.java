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

import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.MetadataTools;
import io.jmix.core.SaveContext;
import io.jmix.core.validation.group.UiCrossFieldChecks;
import io.jmix.data.PersistenceHints;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class AbsenceModelTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    MetadataTools metadataTools;
    @Autowired
    Validator validator;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void lastDayBeforeFirstDayIsRefused() {
        Absence absence = createAbsenceOfNewUser(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9));

        Throwable thrown = catchThrowable(() -> dataManager.save(absence));

        assertThat(ExceptionUtils.throwableOfType(thrown, ConstraintViolationException.class))
                .as("the constraint violation that refused the save")
                .isNotNull()
                .extracting(ConstraintViolationException::getConstraintViolations)
                .satisfies(violations -> assertThat(violations)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("The last day must not be before the first day"));
        assertThat(dataManager.load(Id.of(absence))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .optional()).isEmpty();
    }

    @Test
    void sameFirstAndLastDayIsAccepted() {
        Absence absence = createAbsenceOfNewUser(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10));

        dataManager.saveWithoutReload(absence);

        Absence loaded = dataManager.load(Id.of(absence)).one();
        assertThat(loaded.getFirstDay()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(loaded.getLastDay()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void deletingAUserDeletesTheirAbsences() {
        Absence absence = dataManager.save(
                createAbsenceOfNewUser(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13)));

        dataManager.remove(dataManager.load(Id.of(absence.getUser())).one());

        assertThat(dataManager.load(Id.of(absence)).optional()).isEmpty();
        assertThat(dataManager.load(Id.of(absence))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .one()
                .getDeletedDate()).isNotNull();
    }

    @Test
    void aDeletedTypeStaysOnItsAbsences() {
        Absence absence = dataManager.save(
                createAbsenceOfNewUser(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13)));
        String typeName = absence.getType().getName();

        dataManager.remove(dataManager.load(Id.of(absence.getType())).one());

        Absence loaded = dataManager.load(Id.of(absence))
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("type", FetchPlan.BASE))
                .one();
        assertThat(loaded.getType()).isNotNull();
        assertThat(loaded.getType().getName()).isEqualTo(typeName);
    }

    @Test
    void instanceNameShowsTheDaysAndTheType() {
        AbsenceType vacation = dataManager.create(AbsenceType.class);
        vacation.setName("Vacation");
        Absence absence = dataManager.create(Absence.class);
        absence.setType(vacation);
        absence.setFirstDay(LocalDate.of(2026, 3, 2));
        absence.setLastDay(LocalDate.of(2026, 3, 13));

        assertThat(metadataTools.getInstanceName(absence)).isEqualTo("02/03/2026 – 13/03/2026 Vacation");

        absence.setLastDay(LocalDate.of(2026, 3, 2));

        assertThat(metadataTools.getInstanceName(absence)).isEqualTo("02/03/2026 Vacation");
    }

    @Test
    void instanceNameOfAnUnsavedAbsenceShowsOnlyTheDaysSet() {
        AbsenceType vacation = dataManager.create(AbsenceType.class);
        vacation.setName("Vacation");
        Absence absence = dataManager.create(Absence.class);
        absence.setType(vacation);
        absence.setFirstDay(LocalDate.of(2026, 3, 2));

        assertThat(metadataTools.getInstanceName(absence)).isEqualTo("02/03/2026 Vacation");

        absence.setFirstDay(null);
        absence.setLastDay(LocalDate.of(2026, 3, 13));

        assertThat(metadataTools.getInstanceName(absence)).isEqualTo("13/03/2026 Vacation");

        absence.setLastDay(null);

        assertThat(metadataTools.getInstanceName(absence)).isEqualTo("Vacation");
    }

    @Test
    void lastDayBeforeFirstDayFailsTheCrossFieldChecksOfAnEditor() {
        Absence absence = dataManager.create(Absence.class);
        absence.setFirstDay(LocalDate.of(2026, 11, 10));
        absence.setLastDay(LocalDate.of(2026, 11, 9));

        // The group a detail view validates the whole entity with on save
        Set<ConstraintViolation<Absence>> violations = validator.validate(absence, UiCrossFieldChecks.class);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("The last day must not be before the first day");
    }

    @AfterEach
    void tearDown() {
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
    }

    /**
     * Saves a new user and a new absence type, and creates an absence of theirs that is not saved yet but is
     * removed after the test if it gets saved.
     */
    private Absence createAbsenceOfNewUser(LocalDate firstDay, LocalDate lastDay) {
        User user = dataManager.create(User.class);
        user.setUsername("absence-model-test-" + System.nanoTime());

        AbsenceType type = dataManager.create(AbsenceType.class);
        type.setName("Absence model test type " + System.nanoTime());

        dataManager.save(new SaveContext().saving(user, type));
        track(user, type);

        Absence absence = dataManager.create(Absence.class);
        absence.setUser(user);
        absence.setType(type);
        absence.setFirstDay(firstDay);
        absence.setLastDay(lastDay);
        track(absence);
        return absence;
    }

    private void track(Object... entities) {
        for (Object entity : entities) {
            createdIds.add(Id.of(entity));
        }
    }
}

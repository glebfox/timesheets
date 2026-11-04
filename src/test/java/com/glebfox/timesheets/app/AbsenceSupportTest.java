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

import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.MetadataTools;
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
public class AbsenceSupportTest {

    static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_10 = LocalDate.of(2026, 3, 10);
    static final LocalDate MAR_13 = LocalDate.of(2026, 3, 13);
    static final LocalDate MAR_14 = LocalDate.of(2026, 3, 14);
    static final LocalDate MAR_15 = LocalDate.of(2026, 3, 15);
    static final LocalDate MAR_16 = LocalDate.of(2026, 3, 16);
    static final LocalDate MAR_18 = LocalDate.of(2026, 3, 18);
    static final LocalDate MAR_20 = LocalDate.of(2026, 3, 20);

    @Autowired
    DataManager dataManager;
    @Autowired
    MetadataTools metadataTools;
    @Autowired
    AbsenceSupport absenceSupport;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void overlapIsFound() {
        User user = saveUser();
        AbsenceType type = saveType();
        // Saved before the earlier one, so the order comes from the first days
        Absence later = saveAbsence(user, type, MAR_16, MAR_18);
        Absence earlier = saveAbsence(user, type, MAR_2, MAR_13);

        assertThat(findOverlapping(user, MAR_10, MAR_20, null)).containsExactly(earlier.getId(), later.getId());
    }

    @Test
    void touchingRangesDoNotOverlap() {
        User user = saveUser();
        saveAbsence(user, saveType(), MAR_2, MAR_13);

        // The day after the other's last day
        assertThat(findOverlapping(user, MAR_14, MAR_20, null)).isEmpty();
    }

    @Test
    void theEditedAbsenceDoesNotOverlapItself() {
        User user = saveUser();
        AbsenceType type = saveType();
        Absence edited = saveAbsence(user, type, MAR_2, MAR_13);

        // Extended to 16 Mar, the range covers its own saved days
        assertThat(findOverlapping(user, MAR_2, MAR_16, edited.getId())).isEmpty();

        Absence other = saveAbsence(user, type, MAR_15, MAR_20);

        assertThat(findOverlapping(user, MAR_2, MAR_16, edited.getId())).containsExactly(other.getId());
    }

    @Test
    void otherUsersAndDeletedAbsencesDoNotOverlap() {
        User user = saveUser();
        AbsenceType type = saveType();
        saveAbsence(saveUser(), type, MAR_2, MAR_13);
        dataManager.remove(saveAbsence(user, type, MAR_2, MAR_13));

        assertThat(findOverlapping(user, MAR_10, MAR_20, null)).isEmpty();
    }

    @Test
    void absenceOfADeletedTypeOverlaps() {
        User user = saveUser();
        AbsenceType type = saveType();
        Absence absence = saveAbsence(user, type, MAR_2, MAR_13);
        dataManager.remove(type);

        List<Absence> found = absenceSupport.findOverlapping(user, MAR_10, MAR_20, null);

        assertThat(found).singleElement().satisfies(overlapping -> {
            assertThat(overlapping.getId()).isEqualTo(absence.getId());
            // The refusal names the absence with its type
            assertThat(overlapping.getType().getName()).isEqualTo(type.getName());
            assertThat(metadataTools.getInstanceName(overlapping))
                    .isEqualTo("02/03/2026 – 13/03/2026 " + type.getName());
        });
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

    private List<UUID> findOverlapping(User user, LocalDate firstDay, LocalDate lastDay, UUID excludedId) {
        return absenceSupport.findOverlapping(user, firstDay, lastDay, excludedId).stream()
                .map(Absence::getId)
                .toList();
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername("absence-support-test-" + UUID.randomUUID());
        return track(dataManager.save(user));
    }

    private AbsenceType saveType() {
        AbsenceType type = dataManager.create(AbsenceType.class);
        type.setName("Absence support test " + UUID.randomUUID());
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

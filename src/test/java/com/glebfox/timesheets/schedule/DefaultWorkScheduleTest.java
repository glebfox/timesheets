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
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class DefaultWorkScheduleTest {

    static final String REFUSAL_MESSAGE =
            "There must be one default work schedule. Make another schedule the default first.";

    @Autowired
    DataManager dataManager;

    UUID seedDefaultId;
    List<Id<WorkSchedule>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        seedDefaultId = nonDeletedDefaults().getFirst().getId();
    }

    @Test
    void markingAScheduleDefaultUnmarksThePrevious() {
        WorkSchedule schedule = saveSchedule("Default test A", true);

        assertThat(reload(seedDefaultId).getIsDefault()).isFalse();
        assertThat(reload(schedule.getId()).getIsDefault()).isTrue();
        assertThat(nonDeletedDefaults()).hasSize(1);
    }

    @Test
    void savingANonDefaultScheduleKeepsTheDefault() {
        saveSchedule("Default test B", false);

        assertThat(reload(seedDefaultId).getIsDefault()).isTrue();
        assertThat(nonDeletedDefaults()).hasSize(1);
    }

    @Test
    void unmarkingTheOnlyDefaultIsRefused() {
        WorkSchedule seed = reload(seedDefaultId);
        seed.setIsDefault(false);

        assertRefused(() -> dataManager.save(seed));
        assertThat(reload(seedDefaultId).getIsDefault()).isTrue();
    }

    @Test
    void deletingTheOnlyDefaultIsRefused() {
        assertRefused(() -> dataManager.remove(reload(seedDefaultId)));
        assertThat(nonDeletedDefaults()).extracting(WorkSchedule::getId).containsExactly(seedDefaultId);
    }

    @Test
    void aScheduleThatIsNoLongerTheDefaultCanBeDeleted() {
        WorkSchedule first = saveSchedule("Default test C", true);
        saveSchedule("Default test D", true);

        assertThatCode(() -> dataManager.remove(reload(first.getId()))).doesNotThrowAnyException();
        assertThat(nonDeletedDefaults()).hasSize(1);
    }

    @AfterEach
    void tearDown() {
        // Leave the seeded schedule as the test found it: not deleted, and the default.
        // Making it the default again unmarks any test schedule, so each can be removed.
        WorkSchedule seed = dataManager.load(WorkSchedule.class)
                .id(seedDefaultId)
                .hint(PersistenceHints.SOFT_DELETION, false)
                .one();
        if (seed.getDeletedDate() != null || !Boolean.TRUE.equals(seed.getIsDefault())) {
            seed.setDeletedDate(null);
            seed.setDeletedBy(null);
            seed.setIsDefault(true);
            dataManager.save(new SaveContext()
                    .saving(seed)
                    .setHint(PersistenceHints.SOFT_DELETION, false));
        }

        // Reload by id right before removal: the default listener changes rows after the test saved them
        for (Id<WorkSchedule> id : Lists.reverse(createdIds)) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(schedule -> dataManager.save(new SaveContext()
                            .removing(schedule)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    private WorkSchedule saveSchedule(String name, boolean isDefault) {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(name);
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        schedule.setIsDefault(isDefault);

        WorkSchedule saved = dataManager.save(schedule);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private WorkSchedule reload(UUID id) {
        return dataManager.load(WorkSchedule.class).id(id).one();
    }

    private List<WorkSchedule> nonDeletedDefaults() {
        return dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .list();
    }

    private static void assertRefused(ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .satisfies(e -> assertThat(Throwables.getRootCause(e))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(REFUSAL_MESSAGE));
    }
}

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

package com.glebfox.timesheets.component.daterange;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.util.DateRange;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.UiMinimalRole;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class DateRangeFieldBindingUiTest {

    static final String USERNAME_PREFIX = "date-range-field-binding-ui-test-";

    static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_5 = LocalDate.of(2026, 3, 5);
    static final LocalDate MAR_13 = LocalDate.of(2026, 3, 13);
    static final LocalDate MAR_14 = LocalDate.of(2026, 3, 14);
    static final LocalDate MAR_20 = LocalDate.of(2026, 3, 20);

    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    UiComponents uiComponents;
    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void oneDayRangeShowsAnEmptyEnd() {
        systemAuthenticator.runWithUser("admin", () -> {
            DateRangeField field = openBoundField(absence(MAR_2, MAR_2));

            assertThat(field.getValue()).isEqualTo(new DateRange(MAR_2, null));
            assertThat(field.getStartPicker().getTypedValue()).isEqualTo(MAR_2);
            assertThat(field.getEndPicker().getTypedValue()).isNull();
        });
    }

    @Test
    void oneDayRangeMovesWithItsStart() {
        systemAuthenticator.runWithUser("admin", () -> {
            Absence absence = absence(MAR_2, MAR_2);
            DateRangeField field = openBoundField(absence);

            field.getStartPicker().setTypedValue(MAR_5);

            assertThat(absence.getFirstDay()).isEqualTo(MAR_5);
            assertThat(absence.getLastDay()).isEqualTo(MAR_5);
        });
    }

    @Test
    void writesBothDays() {
        systemAuthenticator.runWithUser("admin", () -> {
            Absence absence = absence(null, null);
            DateRangeField field = openBoundField(absence);

            field.getStartPicker().setTypedValue(MAR_2);
            field.getEndPicker().setTypedValue(MAR_13);

            assertThat(absence.getFirstDay()).isEqualTo(MAR_2);
            assertThat(absence.getLastDay()).isEqualTo(MAR_13);

            // An empty end is one day
            field.getEndPicker().setTypedValue(null);

            assertThat(absence.getFirstDay()).isEqualTo(MAR_2);
            assertThat(absence.getLastDay()).isEqualTo(MAR_2);
        });
    }

    @Test
    void endTypedEqualToStartStaysShown() {
        systemAuthenticator.runWithUser("admin", () -> {
            Absence absence = absence(MAR_2, MAR_13);
            DateRangeField field = openBoundField(absence);

            // The item's last day changes, and read back it would be an empty end
            field.getEndPicker().setTypedValue(MAR_2);

            assertThat(field.getEndPicker().getTypedValue()).isEqualTo(MAR_2);
            assertThat(field.getValue()).isEqualTo(new DateRange(MAR_2, MAR_2));
            assertThat(absence.getFirstDay()).isEqualTo(MAR_2);
            assertThat(absence.getLastDay()).isEqualTo(MAR_2);
        });
    }

    @Test
    void newItemShowsItsDays() {
        systemAuthenticator.runWithUser("admin", () -> {
            DateRangeField field = openBoundField(absence(MAR_2, MAR_13));

            absenceDc().setItem(absence(MAR_14, MAR_20));

            assertThat(field.getStartPicker().getTypedValue()).isEqualTo(MAR_14);
            assertThat(field.getEndPicker().getTypedValue()).isEqualTo(MAR_20);
        });
    }

    @Test
    void requiredFollowsTheStartAttribute() {
        systemAuthenticator.runWithUser("admin", () -> {
            DateRangeField boundField = openBoundField(absence(null, null));

            assertThat(boundField.isRequired()).isTrue();

            DateRangeField field = uiComponents.create(DateRangeField.class);
            assertThat(field.isRequired()).isFalse();

            // A required message given before the binding stays
            field.setRequiredMessage("Dates required");
            field.setValueSource(boundField.getValueSource());

            assertThat(field.isRequired()).isTrue();
            assertThat(field.getRequiredMessage()).isEqualTo("Dates required");
        });
    }

    @Test
    void readOnlyWhenTheUserMayNotChangeTheDays() {
        User user = saveUser();
        assignRole(user, UiMinimalRole.CODE);
        assignRole(user, AbsenceViewOnlyTestRole.CODE);

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), DateRangeFieldTestView.class).open();
            DateRangeField field = UiTestUtils.getComponent(UiTestUtils.getCurrentView(), "boundField");

            assertThat(field.isReadOnly()).isTrue();
        });

        systemAuthenticator.runWithUser("admin", () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), DateRangeFieldTestView.class).open();
            DateRangeField field = UiTestUtils.getComponent(UiTestUtils.getCurrentView(), "boundField");

            assertThat(field.isReadOnly()).isFalse();
        });
    }

    @AfterEach
    void tearDown() {
        // Users and role assignments are soft-deleted, so they are removed for good, reloaded by id
        for (Id<?> id : createdIds.reversed()) {
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
     * Opens the test view with the given absence as the item of its container, unsaved, and returns the field bound to
     * the absence's days.
     */
    private DateRangeField openBoundField(Absence absence) {
        viewBuilders.view(UiTestUtils.getCurrentView(), DateRangeFieldTestView.class).open();
        absenceDc().setItem(absence);
        return UiTestUtils.getComponent(UiTestUtils.getCurrentView(), "boundField");
    }

    private static InstanceContainer<Absence> absenceDc() {
        DateRangeFieldTestView view = UiTestUtils.getCurrentView();
        return ViewControllerUtils.getViewData(view).getContainer("absenceDc");
    }

    private Absence absence(@Nullable LocalDate firstDay, @Nullable LocalDate lastDay) {
        Absence absence = dataManager.create(Absence.class);
        absence.setFirstDay(firstDay);
        absence.setLastDay(lastDay);
        return absence;
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        User saved = dataManager.save(user);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }
}

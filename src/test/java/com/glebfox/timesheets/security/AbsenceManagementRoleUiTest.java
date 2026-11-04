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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.absence.AbsenceDetailView;
import com.glebfox.timesheets.view.absence.AbsenceListView;
import com.glebfox.timesheets.view.absencetype.AbsenceTypeListView;
import com.glebfox.timesheets.view.user.UserListView;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class AbsenceManagementRoleUiTest {

    static final String USERNAME_PREFIX = "absence-management-ui-test-";
    static final String NAME_PREFIX = "Absence management UI test ";

    static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_13 = LocalDate.of(2026, 3, 13);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    String holder;
    List<Id<?>> createdIds = new ArrayList<>();
    List<UUID> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User user = saveUser();
        assignRole(user, UiMinimalRole.CODE);
        assignRole(user, AbsenceManagementRole.CODE);
        holder = user.getUsername();
    }

    @Test
    void holderSavesAnAbsenceInTheEditor() {
        User absent = saveUser();
        AbsenceType type = saveType();

        systemAuthenticator.runWithUser(holder, () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), AbsenceListView.class).open();
            AbsenceListView list = UiTestUtils.getCurrentView();
            DataGrid<Absence> grid = UiTestUtils.getComponent(list, "absencesDataGrid");

            AbsenceDetailView creator = performForEditor(grid, "create");

            // Both pickers open their lists for the holder
            EntityComboBox<User> userField = UiTestUtils.getComponent(creator, "userField");
            EntityComboBox<AbsenceType> typeField = UiTestUtils.getComponent(creator, "typeField");
            openAndCloseLookup(userField, UserListView.class);
            openAndCloseLookup(typeField, AbsenceTypeListView.class);

            userField.setValue(absent);
            typeField.setValue(type);
            UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(creator, "firstDayField").setTypedValue(MAR_2);
            UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(creator, "lastDayField").setTypedValue(MAR_13);
            UiTestUtils.<JmixButton>getComponent(creator, "saveAndCloseBtn").click();

            assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(creator);

            // The list shows the saved absence, and its editor opens editable
            Absence created = Objects.requireNonNull(grid.getItems()).getItems().stream()
                    .filter(absence -> absence.getUser().getId().equals(absent.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("The saved absence in the list"));
            grid.select(created);
            AbsenceDetailView editor = performForEditor(grid, "edit");
            assertThat(editor.getEditedEntity().getId()).isEqualTo(created.getId());
            editor.closeWithDiscard();
        });

        List<Absence> absences = dataManager.load(Absence.class)
                .query("e.user.id = :userId")
                .parameter("userId", absent.getId())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE).add("type", FetchPlan.BASE))
                .list();
        assertThat(absences).singleElement().satisfies(absence -> {
            assertThat(absence.getType().getId()).isEqualTo(type.getId());
            assertThat(absence.getFirstDay()).isEqualTo(MAR_2);
            assertThat(absence.getLastDay()).isEqualTo(MAR_13);
        });
    }

    @AfterEach
    void tearDown() {
        // The absences saved through the editor, before the users and the type they refer to.
        // All are soft-deleted, so they are removed for good, reloaded by id
        if (!userIds.isEmpty()) {
            List<Absence> absences = dataManager.load(Absence.class)
                    .query("e.user.id in :userIds")
                    .parameter("userIds", userIds)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list();
            if (!absences.isEmpty()) {
                hardRemove(absences.toArray());
            }
        }
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(this::hardRemove);
        }
        createdIds.clear();
        userIds.clear();
    }

    /**
     * Performs a list action that opens the absence editor as a dialog over the list, and checks that the editor is
     * editable.
     */
    private static AbsenceDetailView performForEditor(DataGrid<Absence> grid, String actionId) {
        Action action = Objects.requireNonNull(grid.getAction(actionId), "action " + actionId);
        assertThat(action.isEnabled()).as("action %s", actionId).isTrue();
        action.actionPerform(grid);

        View<?> dialog = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the dialog of " + actionId);
        assertThat(dialog).as("the absence editor opened by %s", actionId).isInstanceOf(AbsenceDetailView.class);
        AbsenceDetailView editor = (AbsenceDetailView) dialog;
        assertThat(editor.isReadOnly()).isFalse();
        return editor;
    }

    private static void openAndCloseLookup(EntityComboBox<?> field, Class<? extends View<?>> lookupClass) {
        Action lookup = Objects.requireNonNull(field.getAction("entityLookup"), "the lookup of " + lookupClass);
        assertThat(lookup.isEnabled()).isTrue();
        lookup.actionPerform(field);

        View<?> opened = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the lookup dialog");
        assertThat(opened).isInstanceOf(lookupClass);
        opened.close(StandardOutcome.CLOSE);
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        User saved = dataManager.save(user);
        createdIds.add(Id.of(saved));
        userIds.add(saved.getId());
        return saved;
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }

    private AbsenceType saveType() {
        AbsenceType type = dataManager.create(AbsenceType.class);
        type.setName(NAME_PREFIX + UUID.randomUUID());
        AbsenceType saved = dataManager.save(type);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void hardRemove(Object... entities) {
        dataManager.save(new SaveContext()
                .removing(entities)
                .setHint(PersistenceHints.SOFT_DELETION, false));
    }
}

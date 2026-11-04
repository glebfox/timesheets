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

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.view.absencetype.AbsenceTypeDetailView;
import com.glebfox.timesheets.view.absencetype.AbsenceTypeListView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.renderer.PopoverRenderer;
import io.jmix.flowui.component.propertyfilter.PropertyFilter;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class AbsenceTypeViewsUiTest {

    static final String NAME_PREFIX = "Absence type views test ";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;

    @Test
    void listOpensAndShowsTypes() {
        AbsenceType vacation = saveType(uniqueName(), "Paid annual leave");
        // Another type, which the name filter leaves out
        saveType(uniqueName(), null);

        DataGrid<AbsenceType> grid = openListGrid(vacation.getName());

        assertThat(names(grid)).containsExactly(vacation.getName());
        assertThat(grid.getColumns()).extracting(Grid.Column::getKey).containsExactly("name", "description");
        // The description is cut short, with the full text in a popover
        assertThat(Objects.requireNonNull(grid.getColumnByKey("description")).getRenderer())
                .isInstanceOf(PopoverRenderer.class);
    }

    @Test
    void editorSavesNameAndDescription() {
        String name = uniqueName();
        DataGrid<AbsenceType> grid = openListGrid();
        Action create = Objects.requireNonNull(grid.getAction("create"));

        assertThat(create.isEnabled()).isTrue();
        create.actionPerform(grid);
        AbsenceTypeDetailView creator = UiTestUtils.getCurrentView();
        nameField(creator).setValue(name);
        descriptionField(creator).setValue("Paid annual leave");
        UiTestUtils.<JmixButton>getComponent(creator, "saveAndCloseBtn").click();

        AbsenceType created = loadType(name);
        assertThat(created.getDescription()).isEqualTo("Paid annual leave");
        DataGrid<AbsenceType> shownGrid = shownListGrid();
        assertThat(names(shownGrid)).contains(name);

        Action edit = Objects.requireNonNull(shownGrid.getAction("edit"));
        shownGrid.select(listed(shownGrid, created));
        assertThat(edit.isEnabled()).isTrue();
        edit.actionPerform(shownGrid);
        AbsenceTypeDetailView editor = UiTestUtils.getCurrentView();
        assertThat(nameField(editor).getValue()).isEqualTo(name);
        assertThat(descriptionField(editor).getValue()).isEqualTo("Paid annual leave");
        descriptionField(editor).setValue("Paid annual leave, 28 days a year");
        UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();

        assertThat(loadType(name).getDescription()).isEqualTo("Paid annual leave, 28 days a year");
    }

    @Test
    void removedTypeLeavesTheList() {
        AbsenceType type = saveType(uniqueName(), null);
        DataGrid<AbsenceType> grid = openListGrid(type.getName());
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(listed(grid, type));
        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);
        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a remove confirmation");
        yesButton(confirmation).click();

        assertThat(names(grid)).isEmpty();
        assertThat(dataManager.load(Id.of(type)).optional()).isEmpty();
        // Soft-deleted: the absences of the type keep it
        AbsenceType removed = dataManager.load(Id.of(type))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .one();
        assertThat(removed.getDeletedDate()).isNotNull();
    }

    @AfterEach
    void tearDown() {
        // The types saved through the editor and the removed ones included
        List<AbsenceType> types = dataManager.load(AbsenceType.class)
                .query("e.name like :prefix")
                .parameter("prefix", NAME_PREFIX + "%")
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list();
        if (!types.isEmpty()) {
            dataManager.save(new SaveContext()
                    .removing(types.toArray())
                    .setHint(PersistenceHints.SOFT_DELETION, false));
        }
    }

    private DataGrid<AbsenceType> openListGrid() {
        viewBuilders.view(UiTestUtils.getCurrentView(), AbsenceTypeListView.class).open();
        return shownListGrid();
    }

    /**
     * Opens the type list filtered to the types whose name contains the given text.
     */
    private DataGrid<AbsenceType> openListGrid(String nameFilterValue) {
        DataGrid<AbsenceType> grid = openListGrid();
        PropertyFilter<String> nameFilter = UiTestUtils.getComponent(UiTestUtils.getCurrentView(), "nameFilter");
        nameFilter.setValue(nameFilterValue);
        // A value set from the server does not apply the filter by itself
        nameFilter.apply();
        return grid;
    }

    private static DataGrid<AbsenceType> shownListGrid() {
        View<?> currentView = UiTestUtils.getCurrentView();
        assertThat(currentView).isInstanceOf(AbsenceTypeListView.class);
        return UiTestUtils.getComponent(currentView, "absenceTypesDataGrid");
    }

    private static TypedTextField<String> nameField(AbsenceTypeDetailView editor) {
        return UiTestUtils.getComponent(editor, "nameField");
    }

    private static JmixTextArea descriptionField(AbsenceTypeDetailView editor) {
        return UiTestUtils.getComponent(editor, "descriptionField");
    }

    private static List<String> names(DataGrid<AbsenceType> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .map(AbsenceType::getName)
                .toList();
    }

    private static AbsenceType listed(DataGrid<AbsenceType> grid, AbsenceType type) {
        Optional<AbsenceType> listed = Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(item -> item.getId().equals(type.getId()))
                .findFirst();
        assertThat(listed).as("%s in the list", type.getName()).isPresent();
        return listed.get();
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

    private AbsenceType saveType(String name, String description) {
        AbsenceType type = dataManager.create(AbsenceType.class);
        type.setName(name);
        type.setDescription(description);
        return dataManager.save(type);
    }

    private AbsenceType loadType(String name) {
        return dataManager.load(AbsenceType.class)
                .query("e.name = :name")
                .parameter("name", name)
                .one();
    }
}

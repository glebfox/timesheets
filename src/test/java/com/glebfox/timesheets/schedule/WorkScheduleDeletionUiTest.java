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
import com.glebfox.timesheets.action.WorkScheduleRemoveAction;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.view.workschedule.DefaultWorkScheduleDeleteView;
import com.glebfox.timesheets.view.workschedule.WorkScheduleListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import io.jmix.tabbedmode.testassist.dialog.OpenedDialogs;
import io.jmix.tabbedmode.testassist.notification.NotificationInfo;
import io.jmix.tabbedmode.testassist.notification.OpenedNotifications;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class WorkScheduleDeletionUiTest {

    static final String NAME_PREFIX = "Deletion test ";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    Messages messages;
    @Autowired
    OpenedDialogs openedDialogs;
    @Autowired
    OpenedNotifications openedNotifications;

    UUID seedDefaultId;
    List<Id<WorkSchedule>> scheduleIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        seedDefaultId = dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .one()
                .getId();
    }

    @Test
    void removeIsDisabledWithoutSelection() {
        WorkSchedule schedule = saveSchedule(uniqueName());
        DataGrid<WorkSchedule> grid = openListGrid();
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        assertThat(remove).isInstanceOf(WorkScheduleRemoveAction.class);
        assertThat(remove.isEnabled()).isFalse();

        grid.select(listed(grid, schedule.getId()));
        assertThat(remove.isEnabled()).isTrue();

        grid.select(listed(grid, seedDefaultId));
        assertThat(remove.isEnabled()).isTrue();

        grid.deselectAll();
        assertThat(remove.isEnabled()).isFalse();
    }

    @Test
    void removingAnotherScheduleAsksTheUsualConfirmation() {
        WorkSchedule schedule = saveSchedule(uniqueName());
        DataGrid<WorkSchedule> grid = openListGrid();
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(listed(grid, schedule.getId()));
        assertThat(remove.isEnabled()).isTrue();
        openedDialogs.closeOpenedDialogs();
        remove.actionPerform(grid);

        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "No confirmation dialog opened");
        assertThat(UiTestUtils.<View<?>>getCurrentView()).isInstanceOf(WorkScheduleListView.class);
        // Nothing is deleted before the answer
        assertThat(dataManager.load(Id.of(schedule)).optional()).isPresent();

        yesButton(confirmation).click();

        assertThat(dataManager.load(Id.of(schedule)).optional()).isEmpty();
        assertThat(loadIncludingDeleted(schedule.getId()).getDeletedDate()).isNotNull();
        assertThat(reload(seedDefaultId).getIsDefault()).isTrue();
        assertThat(listedIds(grid)).doesNotContain(schedule.getId());
    }

    @Test
    void removingTheDefaultAsksForTheNextDefault() {
        WorkSchedule next = saveSchedule(uniqueName());
        WorkSchedule deleted = saveSchedule(uniqueName());
        dataManager.remove(deleted);
        DataGrid<WorkSchedule> grid = openListGrid();
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(listed(grid, seedDefaultId));
        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);

        DefaultWorkScheduleDeleteView dialog = openedDeleteDialog();
        String seedName = reload(seedDefaultId).getName();
        assertThat(UiTestUtils.<HasText>getComponent(dialog, "messageText").getText()).contains(seedName);

        // The choice offers the other non-deleted schedules only
        EntityComboBox<WorkSchedule> newDefaultField = newDefaultField(dialog);
        List<WorkSchedule> options = newDefaultField.getGenericDataView().getItems().toList();
        assertThat(options).extracting(WorkSchedule::getId)
                .contains(next.getId())
                .doesNotContain(seedDefaultId, deleted.getId());

        newDefaultField.setValue(options.stream()
                .filter(option -> option.getId().equals(next.getId()))
                .findFirst()
                .orElseThrow());
        UiTestUtils.<JmixButton>getComponent(dialog, "deleteBtn").click();

        assertThat(reload(next.getId()).getIsDefault()).isTrue();
        assertThat(dataManager.load(WorkSchedule.class).id(seedDefaultId).optional()).isEmpty();
        assertThat(loadIncludingDeleted(seedDefaultId).getDeletedDate()).isNotNull();

        // The dialog is closed, and the list shows the new default as the only one, without the deleted row
        View<?> currentView = UiTestUtils.getCurrentView();
        assertThat(currentView).isInstanceOf(WorkScheduleListView.class);
        DataGrid<WorkSchedule> shownGrid = UiTestUtils.getComponent(currentView, "workSchedulesDataGrid");
        List<WorkSchedule> items = Objects.requireNonNull(shownGrid.getItems()).getItems().stream().toList();
        assertThat(items).extracting(WorkSchedule::getId).doesNotContain(seedDefaultId);
        assertThat(items).filteredOn(schedule -> Boolean.TRUE.equals(schedule.getIsDefault()))
                .extracting(WorkSchedule::getId)
                .containsExactly(next.getId());
        assertThat(items).filteredOn(schedule -> renderNameCell(shownGrid, schedule).getChildren()
                        .anyMatch(Badge.class::isInstance))
                .extracting(WorkSchedule::getId)
                .containsExactly(next.getId());
    }

    @Test
    void deleteNeedsAChoice() {
        saveSchedule(uniqueName());
        DataGrid<WorkSchedule> grid = openListGrid();
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(listed(grid, seedDefaultId));
        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);

        DefaultWorkScheduleDeleteView dialog = openedDeleteDialog();
        EntityComboBox<WorkSchedule> newDefaultField = newDefaultField(dialog);
        assertThat(newDefaultField.getValue()).isNull();

        UiTestUtils.<JmixButton>getComponent(dialog, "deleteBtn").click();

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(dialog);
        assertThat(newDefaultField.isInvalid()).isTrue();
        WorkSchedule seed = loadIncludingDeleted(seedDefaultId);
        assertThat(seed.getDeletedDate()).isNull();
        assertThat(seed.getIsDefault()).isTrue();
    }

    @Test
    void removingTheOnlyScheduleIsRefused() {
        // A deleted schedule does not count
        dataManager.remove(saveSchedule(uniqueName()));
        // The test profile seeds "Full time" alone, and every test removes what it created
        assertThat(dataManager.load(WorkSchedule.class).all().list())
                .extracting(WorkSchedule::getId)
                .containsExactly(seedDefaultId);
        DataGrid<WorkSchedule> grid = openListGrid();
        Action remove = Objects.requireNonNull(grid.getAction("remove"));

        grid.select(listed(grid, seedDefaultId));
        assertThat(remove.isEnabled()).isTrue();
        openedDialogs.closeOpenedDialogs();
        openedNotifications.closeOpenedNotifications();
        remove.actionPerform(grid);

        NotificationInfo notification = UiTestUtils.getLastOpenedNotification();
        assertThat(notification).isNotNull();
        assertThat(notification.getType()).isEqualTo(Notifications.Type.WARNING);
        // Resolved as the action resolves it; a missing key would come back as the key itself
        String warning = messages.getMessage(WorkScheduleRemoveAction.class, "workScheduleRemove.onlyScheduleWarning");
        assertThat(warning).isNotEqualTo("workScheduleRemove.onlyScheduleWarning");
        assertThat(notification.getText()).isEqualTo(warning);
        // The Russian text echoes the label of the Make default action
        assertThat(messages.getMessage(WorkScheduleRemoveAction.class, "workScheduleRemove.onlyScheduleWarning",
                Locale.of("ru", "RU")))
                .isEqualTo("Единственный график работы нельзя удалить. "
                        + "Сначала создайте другой график и сделайте его по умолчанию.");
        assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        assertThat(UiTestUtils.<View<?>>getCurrentView()).isInstanceOf(WorkScheduleListView.class);

        WorkSchedule seed = loadIncludingDeleted(seedDefaultId);
        assertThat(seed.getDeletedDate()).isNull();
        assertThat(seed.getIsDefault()).isTrue();
    }

    @AfterEach
    void tearDown() {
        // Leave "Full time" as the tests found it: not deleted, and the default.
        // Restoring it may unmark it while a test schedule is the default, so it is marked in a second save,
        // which unmarks any test schedule, so each can be removed.
        WorkSchedule seed = loadIncludingDeleted(seedDefaultId);
        if (seed.getDeletedDate() != null) {
            seed.setDeletedDate(null);
            seed.setDeletedBy(null);
            dataManager.save(new SaveContext()
                    .saving(seed)
                    .setHint(PersistenceHints.SOFT_DELETION, false));
            seed = loadIncludingDeleted(seedDefaultId);
        }
        if (!Boolean.TRUE.equals(seed.getIsDefault())) {
            seed.setIsDefault(true);
            dataManager.saveWithoutReload(seed);
        }

        // Reloaded by id right before removal: the default listener and the tests change what they created
        for (Id<WorkSchedule> id : scheduleIds) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(schedule -> dataManager.save(new SaveContext()
                            .removing(schedule)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        scheduleIds.clear();
    }

    private DataGrid<WorkSchedule> openListGrid() {
        viewBuilders.view(UiTestUtils.getCurrentView(), WorkScheduleListView.class).open();
        WorkScheduleListView view = UiTestUtils.getCurrentView();
        return UiTestUtils.getComponent(view, "workSchedulesDataGrid");
    }

    private static DefaultWorkScheduleDeleteView openedDeleteDialog() {
        View<?> currentView = UiTestUtils.getCurrentView();
        assertThat(currentView).isInstanceOf(DefaultWorkScheduleDeleteView.class);
        return (DefaultWorkScheduleDeleteView) currentView;
    }

    private static EntityComboBox<WorkSchedule> newDefaultField(DefaultWorkScheduleDeleteView dialog) {
        return UiTestUtils.getComponent(dialog, "newDefaultField");
    }

    private static Button yesButton(DialogInfo dialog) {
        return dialog.getButtons().stream()
                .filter(button -> "yes".equals(button.getId().orElse(null)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No 'yes' button in the dialog"));
    }

    private static WorkSchedule listed(DataGrid<WorkSchedule> grid, UUID id) {
        Optional<WorkSchedule> listed = Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(schedule -> schedule.getId().equals(id))
                .findFirst();
        assertThat(listed).as("schedule %s in the list", id).isPresent();
        return listed.get();
    }

    private static List<UUID> listedIds(DataGrid<WorkSchedule> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .map(WorkSchedule::getId)
                .toList();
    }

    private static Component renderNameCell(DataGrid<WorkSchedule> grid, WorkSchedule schedule) {
        ComponentRenderer<?, WorkSchedule> renderer = (ComponentRenderer<?, WorkSchedule>)
                Objects.requireNonNull(grid.getColumnByKey("name")).getRenderer();
        return renderer.createComponent(schedule);
    }

    private static String uniqueName() {
        return NAME_PREFIX + UUID.randomUUID();
    }

    private WorkSchedule saveSchedule(String name) {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(name);
        schedule.setWorkdayTime(WorkTime.ofHours(8));

        WorkSchedule saved = dataManager.save(schedule);
        scheduleIds.add(Id.of(saved));
        return saved;
    }

    private WorkSchedule reload(UUID id) {
        return dataManager.load(WorkSchedule.class).id(id).one();
    }

    private WorkSchedule loadIncludingDeleted(UUID id) {
        return dataManager.load(WorkSchedule.class)
                .id(id)
                .hint(PersistenceHints.SOFT_DELETION, false)
                .one();
    }
}

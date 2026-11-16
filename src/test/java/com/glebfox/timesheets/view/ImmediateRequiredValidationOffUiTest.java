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

package com.glebfox.timesheets.view;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.UnsavedInputSupport;
import com.glebfox.timesheets.component.approval.ApprovalRejectDialog;
import com.glebfox.timesheets.component.daterange.DateRangeField;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDaysAddView;
import com.glebfox.timesheets.view.workschedule.DefaultWorkScheduleDeleteView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleUsersAddView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.markdowneditor.MarkdownEditor;
import io.jmix.flowui.component.multiselectcomboboxpicker.JmixMultiSelectComboBoxPicker;
import io.jmix.flowui.component.sidedialog.SideDialog;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewValidation;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With {@code jmix.ui.component.immediate-required-validation-enabled} off, the project's own input dialogs mark an
 * empty required field only once it changes or the confirming button is pressed, as detail views do. The property
 * gives the class a Spring context of its own. Each dialog is opened directly, configured as the screen that opens it
 * configures it; the screens' paths are covered by each area's own tests, with the setting on.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class},
        properties = "jmix.ui.component.immediate-required-validation-enabled=false")
public class ImmediateRequiredValidationOffUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    DialogWindows dialogWindows;
    @Autowired
    Dialogs dialogs;
    @Autowired
    UiComponents uiComponents;
    @Autowired
    ViewValidation viewValidation;
    @Autowired
    UnsavedInputSupport unsavedInputSupport;
    @Autowired
    ComponentSupport componentSupport;
    @Autowired
    Messages messages;
    @Autowired
    MetadataTools metadataTools;

    @Test
    void calendarDaysDialogMarksNothingUntilOkOrAChange() {
        HolidayCalendarDaysAddView dialog = openDialog(HolidayCalendarDaysAddView.class,
                view -> view.setCalendarDays(List.of()));
        TypedTextField<String> nameField = UiTestUtils.getComponent(dialog, "nameField");
        DateRangeField datesField = UiTestUtils.getComponent(dialog, "datesField");

        assertThat(nameField.isInvalid()).isFalse();
        assertThat(datesField.isInvalid()).isFalse();

        UiTestUtils.<JmixButton>getComponent(dialog, "okBtn").click();

        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("Name required");
        assertThat(datesField.isInvalid()).isTrue();
        assertThat(datesField.getErrorMessage()).isEqualTo("Dates required");
        assertThat(UiTestUtils.getOpenedViewDialogs()).contains(dialog);

        // A change checks the field too: filled, it is no longer marked, and emptied, it is marked again
        nameField.setTypedValue("New Year holidays");
        assertThat(nameField.isInvalid()).isFalse();
        nameField.setTypedValue(null);
        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("Name required");
    }

    @Test
    void addUsersDialogMarksNothingUntilOk() {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName("Full time");
        WorkScheduleUsersAddView dialog = openDialog(WorkScheduleUsersAddView.class, view -> {
            view.setEditorAssignments(List.of(), List.of());
            view.setWorkSchedule(schedule);
        });
        JmixMultiSelectComboBoxPicker<?> usersField = UiTestUtils.getComponent(dialog, "usersField");
        TypedDatePicker<LocalDate> startDateField = UiTestUtils.getComponent(dialog, "startDateField");

        assertThat(usersField.isInvalid()).isFalse();
        assertThat(startDateField.isInvalid()).isFalse();

        UiTestUtils.<JmixButton>getComponent(dialog, "okBtn").click();

        assertThat(usersField.isInvalid()).isTrue();
        assertThat(usersField.getErrorMessage()).isEqualTo("Users required");
        assertThat(startDateField.isInvalid()).isTrue();
        assertThat(startDateField.getErrorMessage()).isEqualTo("Start date required");
        assertThat(UiTestUtils.getOpenedViewDialogs()).contains(dialog);
    }

    @Test
    void deleteDefaultScheduleDialogMarksNothingUntilDelete() {
        WorkSchedule defaultSchedule = loadDefaultSchedule();
        DefaultWorkScheduleDeleteView dialog = openDialog(DefaultWorkScheduleDeleteView.class,
                view -> view.setSchedule(defaultSchedule));
        EntityComboBox<?> newDefaultField = UiTestUtils.getComponent(dialog, "newDefaultField");

        assertThat(newDefaultField.isInvalid()).isFalse();

        UiTestUtils.<JmixButton>getComponent(dialog, "deleteBtn").click();

        assertThat(newDefaultField.isInvalid()).isTrue();
        assertThat(newDefaultField.getErrorMessage()).isEqualTo("New default schedule required");
        assertThat(UiTestUtils.getOpenedViewDialogs()).contains(dialog);
        WorkSchedule reloaded = dataManager.load(Id.of(defaultSchedule))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .one();
        assertThat(reloaded.getDeletedDate()).isNull();
        assertThat(reloaded.getIsDefault()).isTrue();
    }

    @Test
    void rejectDialogMarksNothingUntilRejectOrAChange() {
        Task task = dataManager.create(Task.class);
        task.setName("Sample analysis");
        TimeEntry entry = dataManager.create(TimeEntry.class);
        entry.setTask(task);
        entry.setDate(LocalDate.of(2026, 11, 16));
        entry.setSpentTime(WorkTime.ofHours(4));
        entry.setStatus(TimeEntryStatus.NEW);
        List<String> rejectedWith = new ArrayList<>();

        ApprovalRejectDialog rejectDialog = new ApprovalRejectDialog(dialogs, uiComponents, viewValidation,
                unsavedInputSupport, componentSupport, messages, metadataTools, Locale.ENGLISH);
        rejectDialog.open(UiTestUtils.getCurrentView(), "Cid", List.of(entry), "", removed -> {
        }, rejectedWith::add);
        SideDialog sideDialog = Objects.requireNonNull(rejectDialog.getSideDialog(), "the reject dialog");
        MarkdownEditor reasonField = partOf(sideDialog, "reasonField");

        // Shown as required, but not marked as missing
        assertThat(reasonField.isRequiredIndicatorVisible()).isTrue();
        assertThat(reasonField.isInvalid()).isFalse();

        click(partOf(sideDialog, "rejectBtn"));

        assertThat(reasonField.isInvalid()).isTrue();
        assertThat(reasonField.getErrorMessage()).isEqualTo("Reason required");
        assertThat(rejectedWith).isEmpty();
        assertThat(sideDialog.isOpened()).isTrue();

        // A change checks the reason too: filled, it is no longer marked, and emptied, it is marked again
        reasonField.setValue("Say which sample.");
        assertThat(reasonField.isInvalid()).isFalse();
        reasonField.setValue("");
        assertThat(reasonField.isInvalid()).isTrue();

        rejectDialog.close();
    }

    private <V extends View<?>> V openDialog(Class<V> viewClass, Consumer<V> configurer) {
        dialogWindows.view(UiTestUtils.getCurrentView(), viewClass)
                .withViewConfigurer(configurer)
                .open();
        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the dialog");
    }

    private WorkSchedule loadDefaultSchedule() {
        return dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .one();
    }

    /**
     * Finds a part of the reject dialog by its id, in its header, content, or footer.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Component> T partOf(SideDialog dialog, String id) {
        Component part = Stream.of(dialog.getHeader().getComponents(), dialog.getContentComponents(),
                        dialog.getFooter().getComponents())
                .flatMap(List::stream)
                .flatMap(root -> Stream.concat(Stream.of(root), descendantsOf(root)))
                .filter(component -> id.equals(component.getId().orElse(null)))
                .findFirst()
                .orElse(null);
        assertThat(part).as("the dialog's %s", id).isNotNull();
        return (T) part;
    }

    private static Stream<Component> descendantsOf(Component root) {
        return root.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendantsOf(child)));
    }

    private static void click(Component button) {
        ComponentUtil.fireEvent(button, new ClickEvent<>(button));
    }
}

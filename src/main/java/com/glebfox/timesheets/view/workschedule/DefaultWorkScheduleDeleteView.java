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

package com.glebfox.timesheets.view.workschedule;

import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Deletes the default {@link WorkSchedule} after the user chooses which schedule becomes the default instead.
 * Opened as a dialog with the schedule to delete; closes with {@link StandardOutcome#SAVE} once deleted.
 */
@ViewController(DefaultWorkScheduleDeleteView.ID)
@ViewDescriptor("default-work-schedule-delete-view.xml")
@DialogMode(width = "480px", closeOnEsc = true)
public class DefaultWorkScheduleDeleteView extends StandardView {

    public static final String ID = "ts_DefaultWorkScheduleDeleteView";

    @ViewComponent
    private CollectionLoader<WorkSchedule> otherSchedulesDl;
    @ViewComponent
    private Span messageText;
    @ViewComponent
    private EntityComboBox<WorkSchedule> newDefaultField;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private ViewValidation viewValidation;

    private WorkSchedule schedule;

    /**
     * Sets the default schedule to delete. Must be called before the view is opened.
     */
    public void setSchedule(WorkSchedule schedule) {
        this.schedule = schedule;
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        if (schedule == null) {
            throw new IllegalStateException("The schedule to delete is not set");
        }

        messageText.setText(messageBundle.formatMessage("defaultWorkScheduleDeleteView.text", schedule.getName()));

        otherSchedulesDl.setParameter("scheduleId", schedule.getId());
        otherSchedulesDl.load();
    }

    @Subscribe("deleteBtn")
    public void onDeleteBtnClick(final ClickEvent<JmixButton> event) {
        ValidationErrors errors = viewValidation.validateUiComponents(getContent());
        if (!errors.isEmpty()) {
            viewValidation.showValidationErrors(errors);
            viewValidation.focusProblemComponent(errors);
            return;
        }

        // A fresh instance, so the offered one stays as it was if the save fails
        WorkSchedule newDefault = dataManager.load(Id.of(newDefaultField.getValue())).one();
        newDefault.setIsDefault(true);
        // One save, so the old default is not deleted unless the new one is marked
        dataManager.save(new SaveContext()
                .saving(newDefault)
                .removing(schedule));

        close(StandardOutcome.SAVE);
    }
}

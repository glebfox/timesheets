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

package com.glebfox.timesheets.view.task;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.multiselectcomboboxpicker.JmixMultiSelectComboBoxPicker;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TaskDetailViewUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;

    ApprovalTestData data;
    Task task;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "task-editor");
        task = data.saveTask(data.saveProject("project", data.saveClient()), data.saveTaskType());
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    @Test
    void requiredTagGroupsSayWhoNeedsThem() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), Task.class, TaskDetailView.class)
                .editEntity(task)
                .open();
        TaskDetailView view = UiTestUtils.getCurrentView();

        JmixMultiSelectComboBoxPicker<?> requiredTagGroupsField =
                UiTestUtils.getComponent(view, "requiredTagGroupsField");
        assertThat(requiredTagGroupsField.getHelperText())
                .isEqualTo("Every time entry on this task needs a tag from each of these groups");
    }
}

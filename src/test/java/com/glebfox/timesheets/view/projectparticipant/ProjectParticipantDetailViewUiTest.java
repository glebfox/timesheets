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

package com.glebfox.timesheets.view.projectparticipant;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs as the system user, who may do everything. A user who may not create project roles is checked in
 * {@code ProjectManagementRoleUiTest}.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class ProjectParticipantDetailViewUiTest {

    @Autowired
    ViewBuilders viewBuilders;

    @Test
    void roleFieldOffersToCreateARoleToWhoeverMayCreateOne() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), ProjectParticipant.class,
                        ProjectParticipantDetailView.class)
                .newEntity()
                .open();
        ProjectParticipantDetailView view = UiTestUtils.getCurrentView();

        EntityComboBox<ProjectRole> roleField = UiTestUtils.getComponent(view, "roleField");
        assertThat(Objects.requireNonNull(roleField.getAction("entityCreate")).isVisible()).isTrue();
    }
}

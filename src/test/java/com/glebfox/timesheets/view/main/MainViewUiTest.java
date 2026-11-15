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

package com.glebfox.timesheets.view.main;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.event.TimeEntryStatusChangedEvent;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.vaadin.flow.component.Component;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import io.jmix.flowui.component.main.JmixListMenu;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// The main view is named: by default, the extension looks it up once per class in a view registry that the previous
// class's teardown has emptied, and then opens its own blank view instead
@UiTest(initialView = MainView.class)
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class MainViewUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UiEventPublisher uiEventPublisher;

    ApprovalTestData data;
    Task task;
    User admin;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "main-view");
        task = data.saveTask(data.saveProject("project", data.saveClient()), data.saveTaskType());
        // The count is of the signed-in user's entries, and the system user of a UI test has none
        admin = data.loadUser("admin");
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    @Test
    void rejectedCountBadgeNamesItself() {
        saveRejectedEntry();
        refreshRejectedCountAsAdmin();

        Component badge = rejectedCountBadge();
        assertThat(badge.getElement().getAttribute("title")).isEqualTo("1 rejected entry");
        assertThat(badge.getElement().getAttribute("aria-label")).isEqualTo("1 rejected entry");

        saveRejectedEntry();
        saveRejectedEntry();
        refreshRejectedCountAsAdmin();

        Component updatedBadge = rejectedCountBadge();
        assertThat(updatedBadge.getElement().getAttribute("title")).isEqualTo("3 rejected entries");
        assertThat(updatedBadge.getElement().getAttribute("aria-label")).isEqualTo("3 rejected entries");
    }

    private void saveRejectedEntry() {
        data.saveEntry(task, admin, LocalDate.now(), TimeEntryStatus.REJECTED, "reason");
    }

    // UiEventsManager calls the main view's listener at once, in the signed-in user's context
    private void refreshRejectedCountAsAdmin() {
        systemAuthenticator.runWithUser(admin.getUsername(),
                () -> uiEventPublisher.publishEventForCurrentUI(new TimeEntryStatusChangedEvent(this)));
    }

    private Component rejectedCountBadge() {
        View<?> view = UiTestUtils.getCurrentView();
        assertThat(view).isInstanceOf(MainView.class);
        JmixListMenu menu = UiTestUtils.getComponent(view, "menu");
        Component badge = menu.getMenuItem("ts_TimeEntry.my").getSuffixComponent();
        assertThat(badge).isNotNull();
        return badge;
    }
}

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

package com.glebfox.timesheets.view.client;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.client.Client;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class ClientDetailViewUiTest {

    @Autowired
    ViewBuilders viewBuilders;

    @Test
    void editorShowsContactInformationUnderADividerHeading() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), Client.class).newEntity().open();
        ClientDetailView view = UiTestUtils.getCurrentView();

        H4 title = UiTestUtils.getComponent(view, "contactInformationTitle");
        assertThat(title.isVisible()).isTrue();
        assertThat(title.hasClassName("text-divider")).isTrue();

        FormLayout contactInformationForm = UiTestUtils.getComponent(view, "contactInformationForm");
        Component addressField = UiTestUtils.getComponent(view, "addressField");
        assertThat(contactInformationForm.getColspan(addressField)).isEqualTo(2);

        // The contact information is a section under a heading, not a details panel
        assertThat(descendants(view.getContent())).noneMatch(Details.class::isInstance);
    }

    private static Stream<Component> descendants(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }
}

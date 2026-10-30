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

package com.glebfox.timesheets.app.util;

import com.glebfox.timesheets.TimesheetsApplication;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class ComponentSupportUiTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired
    ComponentSupport componentSupport;

    Div content;

    @AfterEach
    void tearDown() {
        if (content != null) {
            content.removeFromParent();
        }
    }

    @Test
    void avatarImageIsServedWhileItsContainerIsDisabled() throws IOException {
        Avatar avatar = componentSupport.createAvatar("Black Mesa", PNG, "client-logo");
        content = new Div(avatar);
        UI.getCurrent().add(content);
        // Tabbed mode disables the content of a tab that is not selected this way
        content.getElement().getNode().setEnabled(false);

        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getPathInfo()).thenReturn("/" + avatar.getElement().getAttribute("img"));
        VaadinResponse response = mock(VaadinResponse.class);
        when(response.getService()).thenReturn(VaadinSession.getCurrent().getService());
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(body);

        new StreamRequestHandler().handleRequest(VaadinSession.getCurrent(), request, response);

        verify(response, never()).sendError(anyInt(), anyString());
        assertArrayEquals(PNG, body.toByteArray());
    }
}

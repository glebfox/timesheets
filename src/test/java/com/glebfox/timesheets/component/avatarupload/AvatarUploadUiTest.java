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

package com.glebfox.timesheets.component.avatarupload;

import com.glebfox.timesheets.TimesheetsApplication;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.customfield.CustomField;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.notification.NotificationInfo;
import io.jmix.tabbedmode.testassist.notification.OpenedNotifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class AvatarUploadUiTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired
    UiComponents uiComponents;

    @Autowired
    OpenedNotifications openedNotifications;

    AvatarUpload field;

    List<AbstractField.ComponentValueChangeEvent<CustomField<byte[]>, byte[]>> valueChanges;

    @BeforeEach
    void setUp() {
        field = uiComponents.create(AvatarUpload.class);
        valueChanges = new ArrayList<>();
        field.addValueChangeListener(valueChanges::add);
    }

    @Test
    void newFieldIsEditableWithBadgeAndUploadOnly() {
        assertTrue(field.isEditable());
        assertTrue(field.getBadge().isVisible());
        assertTrue(field.getUploadItem().isVisible());
        assertFalse(field.getRemoveItem().isVisible());
        assertEquals(AvatarUpload.DEFAULT_MAX_FILE_SIZE, field.getMaxFileSize());
    }

    @Test
    void uploadSetsValueAndShowsRemove() {
        field.handleUploadSucceeded(PNG);

        assertArrayEquals(PNG, field.getValue());
        assertTrue(field.getRemoveItem().isVisible());
        assertEquals(1, valueChanges.size());
        assertTrue(valueChanges.getFirst().isFromClient());
    }

    @Test
    void sameBytesAgainIsNoChange() {
        field.handleUploadSucceeded(PNG.clone());
        field.handleUploadSucceeded(PNG.clone());

        assertEquals(1, valueChanges.size());
    }

    @Test
    void removeClearsValue() {
        field.handleUploadSucceeded(PNG);

        field.handleRemove();

        assertNull(field.getValue());
        assertFalse(field.getRemoveItem().isVisible());
    }

    @Test
    void readOnlyHidesBadgeAndItems() {
        field.setReadOnly(true);

        assertFalse(field.isEditable());
        assertFalse(field.getBadge().isVisible());
        assertFalse(field.getUploadItem().isVisible());
        assertFalse(field.getRemoveItem().isVisible());

        field.handleUploadSucceeded(PNG);
        assertNull(field.getValue());
    }

    @Test
    void disabledHidesBadge() {
        field.setEnabled(false);

        assertFalse(field.isEditable());
        assertFalse(field.getBadge().isVisible());
    }

    @Test
    void eachRejectionShowsWarningAndKeepsValue() {
        field.handleUploadSucceeded(PNG);

        for (AvatarUploadRejection rejection : AvatarUploadRejection.values()) {
            openedNotifications.closeOpenedNotifications();

            field.handleRejection(rejection);

            NotificationInfo notification = UiTestUtils.getLastOpenedNotification();
            assertNotNull(notification, rejection.name());
            assertEquals(Notifications.Type.WARNING, notification.getType(), rejection.name());
            assertArrayEquals(PNG, field.getValue(), rejection.name());
        }
    }

    @Test
    void tooLargeMessageNamesTheLimit() {
        field.setMaxFileSize(1024 * 1024);
        openedNotifications.closeOpenedNotifications();

        field.handleRejection(AvatarUploadRejection.FILE_TOO_LARGE);

        assertTrue(UiTestUtils.getLastOpenedNotification().getText().contains("1 MB"));
    }

    @Test
    void acceptedFileTypesAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> field.setAcceptedFileTypes("image/svg+xml"));

        field.setAcceptedFileTypes("image/png", ".jpg");

        assertEquals(List.of("image/png", ".jpg"), field.getAcceptedFileTypes());
        assertEquals(Arrays.asList(AvatarImageFormat.PNG, AvatarImageFormat.JPEG),
                field.getAcceptedFormats().stream().sorted().toList());
        assertThrows(IllegalArgumentException.class, () -> field.setMaxFileSize(0));
    }
}

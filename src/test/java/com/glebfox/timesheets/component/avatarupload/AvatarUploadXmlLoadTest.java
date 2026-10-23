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
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class AvatarUploadXmlLoadTest {

    @Autowired
    ViewBuilders viewBuilders;

    @Test
    void loadsEveryAttribute() {
        viewBuilders.view(UiTestUtils.getCurrentView(), AvatarUploadTestView.class).open();
        AvatarUploadTestView view = UiTestUtils.getCurrentView();

        AvatarUpload field = UiTestUtils.getComponent(view, "configuredField");

        assertTrue(field.hasClassName("avatar-upload"));
        assertEquals("Logo", field.getLabel());
        assertEquals("PNG or JPEG", field.getHelperText());
        assertTrue(field.isReadOnly());
        assertFalse(field.isEditable());
        assertEquals(1024 * 1024, field.getMaxFileSize());
        assertEquals(List.of("image/png", ".jpg"), field.getAcceptedFileTypes());
        assertEquals(Set.of(AvatarImageFormat.PNG, AvatarImageFormat.JPEG), field.getAcceptedFormats());
    }
}

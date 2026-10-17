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
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.client.ClientDetailView;
import com.glebfox.timesheets.view.user.UserDetailView;
import io.jmix.flowui.data.EntityValueSource;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.glebfox.timesheets.component.avatarupload.AvatarUploadUiTest.PNG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class AvatarUploadEditorsUiTest {

    @Autowired
    ViewBuilders viewBuilders;

    @Test
    void userEditorBindsAvatar() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), User.class).newEntity().open();
        UserDetailView view = UiTestUtils.getCurrentView();

        AvatarUpload field = UiTestUtils.getComponent(view, "avatarUpload");

        assertBound(field, "avatar");

        field.handleUploadSucceeded(PNG);

        InstanceContainer<User> userDc = ViewControllerUtils.getViewData(view).getContainer("userDc");
        assertArrayEquals(PNG, userDc.getItem().getAvatar());
    }

    @Test
    void clientEditorBindsImage() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), Client.class).newEntity().open();
        ClientDetailView view = UiTestUtils.getCurrentView();

        AvatarUpload field = UiTestUtils.getComponent(view, "avatarUpload");

        assertBound(field, "image");

        field.handleUploadSucceeded(PNG);

        InstanceContainer<Client> clientDc = ViewControllerUtils.getViewData(view).getContainer("clientDc");
        assertArrayEquals(PNG, clientDc.getItem().getImage());
    }

    @Test
    void readOnlyEditorMakesFieldReadOnly() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), User.class).newEntity().open();
        UserDetailView view = UiTestUtils.getCurrentView();
        AvatarUpload field = UiTestUtils.getComponent(view, "avatarUpload");

        view.setReadOnly(true);

        assertFalse(field.isEditable());
        assertFalse(field.getBadge().isVisible());
    }

    private static void assertBound(AvatarUpload field, String property) {
        EntityValueSource<?, ?> valueSource = assertInstanceOf(EntityValueSource.class, field.getValueSource());
        assertEquals(property, valueSource.getMetaPropertyPath().toPathString());
        assertFalse(field.getLabel() == null || field.getLabel().isBlank());
        assertEquals(AvatarUpload.DEFAULT_MAX_FILE_SIZE, field.getMaxFileSize());
    }
}

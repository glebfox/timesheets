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

package com.glebfox.timesheets.data.renderer;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.user.User;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Div;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AvatarCellRendererTest {

    @Test
    void clientCellWithoutImageShowsTitleOnly() {
        Client client = new Client();
        client.setName("Aperture Science");
        AvatarCellRenderer<Client> renderer = new ComponentSupport(mock(UiComponents.class), mock(Messages.class),
                mock(MetadataTools.class), mock(CurrentAuthentication.class)).createClientRenderer();

        Div cell = renderer.createComponent(client);

        assertThat(cell).isNotNull();
        assertThat(cell.getClassNames()).containsExactly("avatar-cell");
        List<Component> children = cell.getChildren().toList();
        assertThat(children).hasSize(2);
        Avatar avatar = (Avatar) children.get(0);
        assertThat(avatar.getName()).isEqualTo("Aperture Science");
        assertThat(avatar.getClassNames()).containsExactly("prefix");
        assertThat(avatar.getElement().getAttribute("tabindex")).isEqualTo("-1");
        Div title = (Div) children.get(1);
        assertThat(title.getClassNames()).containsExactlyInAnyOrder("title", "title-body");
        assertThat(title.getText()).isEqualTo("Aperture Science");
    }

    @Test
    void userCellWithFullNameShowsUsernameAsBody() {
        User user = user("Gordon", "Freeman");

        Div cell = new UserRenderer<User>(u -> u).createComponent(user);

        assertThat(cell).isNotNull();
        assertThat(cell.getClassNames()).containsExactly("avatar-cell");
        List<Component> children = cell.getChildren().toList();
        assertThat(children).hasSize(3);
        assertThat(((Avatar) children.get(0)).getName()).isEqualTo("Gordon Freeman");
        Div title = (Div) children.get(1);
        assertThat(title.getClassNames()).containsExactly("title");
        assertThat(title.getText()).isEqualTo("Gordon Freeman");
        Div body = (Div) children.get(2);
        assertThat(body.getClassNames()).containsExactly("body");
        assertThat(body.getText()).isEqualTo("freeman");
    }

    @Test
    void userCellWithoutFullNameShowsUsernameAsTitle() {
        User user = user(null, null);
        user.setActive(true);

        Div cell = new UserRenderer<User>(u -> u, u -> "Active").createComponent(user);

        assertThat(cell).isNotNull();
        List<Component> children = cell.getChildren().toList();
        assertThat(children).hasSize(3);
        assertThat(((Avatar) children.get(0)).getName()).isEqualTo("freeman");
        Div title = (Div) children.get(1);
        assertThat(title.getClassNames()).containsExactlyInAnyOrder("title", "title-body");
        assertThat(title.getText()).isEqualTo("freeman");
        Badge badge = (Badge) children.get(2);
        assertThat(badge.getClassNames()).containsExactlyInAnyOrder("title-suffix", "title-suffix-body");
        assertThat(badge.getText()).isEqualTo("Active");
        assertThat(children).noneMatch(child -> child.getClassNames().contains("body"));
    }

    @Test
    void nullItemValueRendersNothing() {
        AvatarCellRenderer<Object> renderer = new AvatarCellRenderer<>(item -> null);

        assertThat(renderer.createComponent(new Object())).isNull();
    }

    private static User user(String firstName, String lastName) {
        User user = new User();
        user.setUsername("freeman");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}

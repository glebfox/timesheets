/*
 * Copyright 2024 Gleb Gorelov
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

import com.glebfox.timesheets.entity.user.User;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

/**
 * A renderer that generates a custom visual representation for a user item.
 * This class extends {@link ComponentRenderer} to create a UI component
 * representing a user with additional visual elements such as avatar, name, and an optional status badge.
 *
 * @param <ITEM> the type of the items being rendered
 */
@NullMarked
public class UserRenderer<ITEM> extends ComponentRenderer<Div, ITEM> {

    private final Function<ITEM, @Nullable User> valueProvider;
    private final @Nullable Function<User, String> statusProvider;

    /**
     * Creates a renderer that shows a user without a status badge.
     *
     * @param valueProvider a function that extracts the {@link User} from an item; an item without a user
     *                      renders as an empty cell
     */
    public UserRenderer(Function<ITEM, @Nullable User> valueProvider) {
        this(valueProvider, null);
    }

    /**
     * Creates a renderer that shows a user with a status badge, if a status provider is given.
     *
     * @param valueProvider  a function that extracts the {@link User} from an item; an item without a user
     *                       renders as an empty cell
     * @param statusProvider a function that gives the text of the status badge, which is green for an active
     *                       user and red for an inactive one, or {@code null} for no badge
     */
    public UserRenderer(Function<ITEM, @Nullable User> valueProvider,
                        @Nullable Function<User, String> statusProvider) {
        super(((SerializableSupplier<Div>) null));
        this.valueProvider = valueProvider;
        this.statusProvider = statusProvider;
    }

    @Nullable
    @Override
    public Div createComponent(ITEM item) {
        User user = valueProvider.apply(item);
        if (user == null) {
            return null;
        }

        String fullName = getFullName(user);
        String name = fullName.isEmpty() ? user.getUsername() : fullName;

        Avatar avatar = createAvatar(name, user.getAvatar());
        Div title = createTitle(name);
        Badge activeBadge = createStatusBadge(user);

        Div wrapper = new Div();
        wrapper.setClassName("user-name-layout");
        wrapper.add(avatar, title);
        if (activeBadge != null) {
            wrapper.add(activeBadge);
        }

        if (name.equals(fullName)) {
            Div body = createBody(user.getUsername());
            wrapper.add(body);
        } else {
            title.addClassNames("title-body");
            if (activeBadge != null) {
                activeBadge.addClassNames("title-suffix-body");
            }
        }

        return wrapper;
    }

    private String getFullName(User user) {
        return String.format("%s %s",
                (user.getFirstName() != null ? user.getFirstName() : ""),
                (user.getLastName() != null ? user.getLastName() : "")
        ).trim();
    }

    private Avatar createAvatar(String name, byte @Nullable [] data) {
        Avatar avatar = new Avatar(name);
        avatar.getElement().setAttribute("tabindex", "-1");

        if (data != null) {
            InputStreamDownloadHandler downloadHandler = DownloadHandler.fromInputStream(downloadEvent ->
                    new DownloadResponse(new ByteArrayInputStream(data),
                            "avatar.png", null, data.length));

            avatar.setImageHandler(downloadHandler);
        }

        avatar.addClassName("prefix");
        return avatar;
    }

    private Div createTitle(String fullName) {
        Div fullNameDiv = new Div();
        fullNameDiv.setText(fullName);
        fullNameDiv.setClassName("title text-m font-bold text-body");
        return fullNameDiv;
    }

    @Nullable
    private Badge createStatusBadge(User user) {
        if (statusProvider == null) {
            return null;
        }

        Badge activeBadge = new Badge(statusProvider.apply(user));
        activeBadge.setClassName("title-suffix");
        activeBadge.addThemeVariants(BadgeVariant.SMALL,
                Boolean.TRUE.equals(user.getActive()) ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);

        return activeBadge;
    }

    private Div createBody(String username) {
        Div usernameDiv = new Div();
        usernameDiv.setText(username);
        usernameDiv.setClassName("body text-s text-secondary");
        return usernameDiv;
    }
}

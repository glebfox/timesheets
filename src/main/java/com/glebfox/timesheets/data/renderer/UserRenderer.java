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
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * An {@link AvatarCellRenderer} for a user: the avatar, the full name with the username under it, or the username
 * alone for a user without a name, and an optional status badge.
 *
 * @param <ITEM> the type of the items being rendered
 */
@NullMarked
public class UserRenderer<ITEM> extends AvatarCellRenderer<ITEM> {

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
        super(item -> {
            User user = valueProvider.apply(item);
            return user == null ? null : createCell(user, statusProvider);
        });
    }

    private static AvatarCell createCell(User user, @Nullable Function<User, String> statusProvider) {
        String fullName = getFullName(user);
        boolean hasFullName = !fullName.isEmpty();
        return new AvatarCell(
                hasFullName ? fullName : user.getUsername(),
                user.getAvatar(),
                hasFullName ? user.getUsername() : null,
                statusProvider != null ? createStatusBadge(user, statusProvider) : null);
    }

    private static String getFullName(User user) {
        return String.format("%s %s",
                (user.getFirstName() != null ? user.getFirstName() : ""),
                (user.getLastName() != null ? user.getLastName() : "")
        ).trim();
    }

    private static Badge createStatusBadge(User user, Function<User, String> statusProvider) {
        Badge badge = new Badge(statusProvider.apply(user));
        badge.addThemeVariants(BadgeVariant.SMALL,
                Boolean.TRUE.equals(user.getActive()) ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
        return badge;
    }
}

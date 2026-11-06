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

import com.vaadin.flow.component.badge.Badge;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * What an {@link AvatarCellRenderer} shows for one item: an avatar beside a title, with an optional badge after the
 * title and an optional body line under it.
 *
 * @param title the title, which also names the avatar and gives its initials when there is no image
 * @param image the avatar image, or {@code null} for the initials of the title
 * @param body  the line under the title, or {@code null} for none; without it the title is centered on the avatar
 * @param badge the badge after the title, or {@code null} for none; a new instance for every cell, since the
 *              renderer adds its placement classes to it
 */
@NullMarked
public record AvatarCell(String title, byte @Nullable [] image, @Nullable String body, @Nullable Badge badge) {
}

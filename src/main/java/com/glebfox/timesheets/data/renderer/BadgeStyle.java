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
import com.vaadin.flow.component.badge.BadgeVariant;

/**
 * The look of a {@link Badge}, expressed in what the Aura theme styles.
 */
public enum BadgeStyle {

    /**
     * The accent color on a tinted surface.
     */
    DEFAULT,

    /**
     * A solid accent background.
     */
    FILLED,

    SUCCESS,

    WARNING,

    ERROR,

    /**
     * Gray. Aura does not style {@link BadgeVariant#CONTRAST}, so the badge takes Aura's neutral accent class instead.
     */
    NEUTRAL;

    /**
     * Applies this style to a badge that has none yet.
     *
     * @param badge the badge to style
     */
    public void applyTo(Badge badge) {
        switch (this) {
            case DEFAULT -> {
            }
            case FILLED -> badge.addThemeVariants(BadgeVariant.FILLED);
            case SUCCESS -> badge.addThemeVariants(BadgeVariant.SUCCESS);
            case WARNING -> badge.addThemeVariants(BadgeVariant.WARNING);
            case ERROR -> badge.addThemeVariants(BadgeVariant.ERROR);
            case NEUTRAL -> badge.addClassName("aura-accent-neutral");
        }
    }
}

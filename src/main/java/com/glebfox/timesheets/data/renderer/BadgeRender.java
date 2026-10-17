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

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.function.Function;

/**
 * A renderer for displaying badges in a user interface.
 * This class is a generic specialization of ComponentRenderer, designed to render
 * items into a {@link Badge} component styled based on the item's properties.
 *
 * @param <ITEM> the type of the items being rendered
 */
public class BadgeRender<ITEM> extends ComponentRenderer<Badge, ITEM> {

    public BadgeRender(Function<ITEM, String> valueProvider,
                       Function<ITEM, BadgeStyle> styleProvider) {
        super(Badge::new, (badge, item) -> {
            badge.setText(valueProvider.apply(item));
            styleProvider.apply(item).applyTo(badge);
        });
    }
}

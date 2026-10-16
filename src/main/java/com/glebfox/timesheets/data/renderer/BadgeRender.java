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

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.function.Function;

/**
 * A renderer for displaying badge-style components in a user interface.
 * This class is a generic specialization of ComponentRenderer, designed to render
 * items into a {@link Span} component with a "badge" theme applied based on the item's properties.
 *
 * @param <ITEM> the type of the items being rendered
 */
public class BadgeRender<ITEM> extends ComponentRenderer<Span, ITEM> {

    public BadgeRender(Function<ITEM, String> valueProvider,
                       Function<ITEM, String> themeProvider) {
        super(Span::new, (span, item) -> {
            String theme = themeProvider.apply(item);
            span.getElement().setAttribute("theme", "badge " + theme);
            span.setText(valueProvider.apply(item));
        });
    }
}

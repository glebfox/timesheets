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

package com.glebfox.timesheets.view.dashboard;

import com.glebfox.timesheets.app.util.PluralForms;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.kit.component.button.JmixButton;

import java.util.Locale;

/**
 * Builds the rows of the dashboard's list blocks, so every list looks and reads the same: a row is a line of parts,
 * a name that takes the room, and a value, and it is a button where it does something on a click.
 */
final class DashboardRows {

    /**
     * The class of a row of a list.
     */
    static final String ROW = "dashboard-list-row";

    /**
     * The class of the "and N more" line under a list.
     */
    static final String MORE = "dashboard-list-more";

    private DashboardRows() {
    }

    /**
     * Creates a text of a row with the class that tells its part, such as {@code name} or {@code value}.
     */
    static Span part(UiComponents uiComponents, String className, String text) {
        Span span = uiComponents.create(Span.class);
        span.addClassName(className);
        span.setText(text);
        return span;
    }

    /**
     * Creates a row that only shows its parts.
     */
    static Div row(UiComponents uiComponents, Component... parts) {
        Div row = uiComponents.create(Div.class);
        row.addClassName(ROW);
        row.add(parts);
        return row;
    }

    /**
     * Creates a row that runs the listener on a click, as a button, so the keyboard reaches it.
     */
    static JmixButton buttonRow(UiComponents uiComponents,
                                ComponentEventListener<ClickEvent<Button>> listener,
                                Component... parts) {
        JmixButton row = actionButton(uiComponents, listener, parts);
        row.addClassName(ROW);
        return row;
    }

    /**
     * Creates the "and N more" line under a list that shows only some of its items.
     *
     * @param count how many items the list does not show
     */
    static Div more(UiComponents uiComponents, Messages messages, int count, Locale locale) {
        Div more = uiComponents.create(Div.class);
        more.addClassName(MORE);
        more.setText(messages.formatMessage(DashboardRows.class, PluralForms.key("list.more", count, locale), locale,
                count));
        return more;
    }

    /**
     * Creates a tertiary button that holds the parts, and runs the listener on a click.
     */
    static JmixButton actionButton(UiComponents uiComponents,
                                   ComponentEventListener<ClickEvent<Button>> listener,
                                   Component... parts) {
        JmixButton button = uiComponents.create(JmixButton.class);
        button.addThemeVariants(ButtonVariant.TERTIARY);
        for (Component part : parts) {
            button.getElement().appendChild(part.getElement());
        }
        button.addClickListener(listener);
        return button;
    }
}

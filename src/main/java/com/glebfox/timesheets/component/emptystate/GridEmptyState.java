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

package com.glebfox.timesheets.component.emptystate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.ListDataComponent;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.HasAction;
import io.jmix.flowui.kit.component.button.JmixButton;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The empty state of a grid: a line of the area's icon and a heading, such as "No projects", and under it a button
 * of an action, such as the grid's Refresh. It is placed as the single child of a grid's {@code emptyStateComponent}.
 * <p>
 * The button exists only while an action is set: the first action creates it, a later one takes it over, and
 * {@code null} removes it. The button takes its text, icon, visibility, and enablement from the action, as any button
 * bound to an action does. When a click on it brings the grid rows, the empty state is no longer shown, so the
 * keyboard focus moves from the button to the grid; while the grid stays empty, it stays on the button.
 * <p>
 * The component takes the beans it needs from the application context, so it is created by {@link UiComponents}, as a
 * descriptor's {@code gridEmptyState} element creates it.
 * <p>
 * The component has the class {@code grid-empty-state}. Its parts are the {@code heading}, which holds the
 * {@code icon} and the {@code text}, and the {@code action-button}.
 */
public class GridEmptyState extends Composite<Div>
        implements HasStyle, HasAction, ApplicationContextAware, InitializingBean {

    static final String CLASS_NAME = "grid-empty-state";
    static final String ICON_CLASS_NAME = "icon";
    static final String ACTION_BUTTON_CLASS_NAME = "action-button";

    private final Div heading = new Div();
    private final Span textComponent = new Span();

    private ApplicationContext applicationContext;
    private UiComponents uiComponents;

    @Nullable
    private Component icon;
    @Nullable
    private JmixButton actionButton;

    /**
     * Creates the empty state with no icon, no text, and no button.
     */
    public GridEmptyState() {
        textComponent.addClassName("text");

        heading.addClassName("heading");
        heading.add(textComponent);

        getContent().addClassName(CLASS_NAME);
        getContent().add(heading);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        uiComponents = applicationContext.getBean(UiComponents.class);
    }

    /**
     * Sets the icon shown before the text. The icon only repeats what the heading says, so it is hidden from assistive
     * technologies.
     *
     * @param icon the icon component, or {@code null} for no icon
     */
    public void setIcon(@Nullable Component icon) {
        if (this.icon != null) {
            // The caller may have moved the previous icon elsewhere, where removing it from the heading would throw
            if (this.icon.getParent().filter(heading::equals).isPresent()) {
                heading.remove(this.icon);
            }
        }
        this.icon = icon;
        if (icon != null) {
            icon.getElement().setAttribute("aria-hidden", "true");
            icon.getElement().getClassList().add(ICON_CLASS_NAME);
            heading.addComponentAsFirst(icon);
        }
    }

    /**
     * @return the icon component, or {@code null} if none is set
     */
    @Nullable
    public Component getIcon() {
        return icon;
    }

    /**
     * Sets the heading, which says what is missing, such as "No projects".
     *
     * @param text the heading
     */
    public void setText(@Nullable String text) {
        textComponent.setText(text);
    }

    /**
     * @return the heading
     */
    public String getText() {
        return textComponent.getText();
    }

    /**
     * Sets the action of the button under the heading. The first action creates the button, a later one is set to the
     * same button, and {@code null} removes the button.
     *
     * @param action                      the action, or {@code null} for no button
     * @param overrideComponentProperties whether the action's properties override those of the button
     */
    @Override
    public void setAction(@Nullable Action action, boolean overrideComponentProperties) {
        if (action == null) {
            removeActionButton();
            return;
        }

        if (actionButton == null) {
            actionButton = createActionButton();
        }
        actionButton.setAction(action, overrideComponentProperties);
    }

    @Nullable
    @Override
    public Action getAction() {
        return actionButton != null ? actionButton.getAction() : null;
    }

    private JmixButton createActionButton() {
        JmixButton button = uiComponents.create(JmixButton.class);
        button.addClassName(ACTION_BUTTON_CLASS_NAME);
        button.addThemeVariants(ButtonVariant.TERTIARY);
        // Checked once the whole click has been handled: the button runs its action from a click listener of its
        // own, which a later action registers anew, after this one
        button.addClickListener(event -> button.getUI()
                .ifPresent(ui -> ui.beforeClientResponse(this, context -> focusGridIfFilled())));
        getContent().add(button);
        return button;
    }

    private void removeActionButton() {
        if (actionButton != null) {
            // Unbinds the button, so the action no longer holds it
            actionButton.setAction(null);
            getContent().remove(actionButton);
            actionButton = null;
        }
    }

    // The empty state, and its button with it, is hidden once the grid has rows, which would leave the focus nowhere
    private void focusGridIfFilled() {
        Component parent = getParent().orElse(null);
        if (parent instanceof ListDataComponent<?> grid
                && grid.getItems() instanceof DataGridItems<?> items
                && !items.getItems().isEmpty()) {
            // Grid.focus() does nothing: the vaadin-grid host is not focusable, and no Flow or Jmix API focuses a
            // cell. The grid's own body focus target, the cell that Tab lands on, is focused instead. The timeout,
            // as in Focusable.focus(), runs after the response has given the grid its rows.
            // _itemsFocusable is an internal of vaadin-grid: an upgrade that renames it turns this call into a silent
            // no-op, which only a live check notices.
            parent.getElement().executeJs("setTimeout(() => this._itemsFocusable?.focus())");
        }
    }
}

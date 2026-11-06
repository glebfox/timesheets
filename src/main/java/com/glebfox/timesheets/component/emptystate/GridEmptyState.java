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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.shared.Registration;
import io.jmix.flowui.action.list.RefreshAction;
import io.jmix.flowui.component.ListDataComponent;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.kit.component.HasActions;
import io.jmix.flowui.kit.component.button.JmixButton;
import org.jspecify.annotations.Nullable;

/**
 * The empty state of a grid: a line of the area's icon and a heading, such as "No projects", and under it Refresh,
 * which loads the grid's rows again. It is placed as the single child of a grid's {@code emptyStateComponent}.
 * <p>
 * Refresh runs the grid's refresh action, the first of its actions that is a {@link RefreshAction}, whatever its id,
 * and is shown only while the component is attached to a grid with such an action, a data grid or a tree data grid,
 * and that action is both visible and enabled. When the refresh brings rows, the empty state is no longer shown, so
 * the keyboard focus moves from Refresh to the grid; while the grid stays empty, it stays on Refresh. The
 * component finds the action when it is attached, since a descriptor's grid gets its actions after its empty state;
 * it follows the action's state until it is detached, so a component attached again is bound once.
 * <p>
 * The button does not take the action with {@link JmixButton#setAction}: the button's own action support follows the
 * action's visibility and enablement separately, while Refresh is hidden whenever the action cannot be performed.
 * <p>
 * The component has the class {@code grid-empty-state}. Its parts are the {@code heading}, which holds the
 * {@code icon} and the {@code text}, and the {@code refresh-button}.
 */
public class GridEmptyState extends Composite<Div> implements HasStyle {

    static final String CLASS_NAME = "grid-empty-state";

    private final Icon iconComponent = new Icon();
    private final Span textComponent = new Span();
    private final JmixButton refreshButton = new JmixButton();

    @Nullable
    private String icon;
    @Nullable
    private Action refreshAction;
    @Nullable
    private Registration refreshActionRegistration;
    @Nullable
    private Registration clickRegistration;

    /**
     * Creates the empty state with no icon, no text, and Refresh hidden.
     */
    public GridEmptyState() {
        // The heading says what is missing; the icon only repeats it
        iconComponent.getElement().setAttribute("aria-hidden", "true");
        iconComponent.addClassName("icon");
        iconComponent.setVisible(false);

        textComponent.addClassName("text");

        Div heading = new Div(iconComponent, textComponent);
        heading.addClassName("heading");

        refreshButton.addClassName("refresh-button");
        refreshButton.addThemeVariants(ButtonVariant.TERTIARY);
        refreshButton.setVisible(false);

        getContent().addClassName(CLASS_NAME);
        getContent().add(heading, refreshButton);
    }

    /**
     * Sets the icon shown before the text, by its full name, such as {@code "ts:projects"}.
     *
     * @param icon the icon's full name, or {@code null} for no icon
     */
    public void setIcon(@Nullable String icon) {
        this.icon = icon;
        if (icon != null) {
            iconComponent.setIcon(icon);
        }
        iconComponent.setVisible(icon != null);
    }

    /**
     * @return the icon's full name, such as {@code "ts:projects"}, or {@code null} if none is set
     */
    @Nullable
    public String getIcon() {
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
     * @return Refresh, which always exists and is visible only while the component is bound to its grid's refresh
     * action and that action is both visible and enabled
     */
    public JmixButton getRefreshButton() {
        return refreshButton;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        Action action = findRefreshAction();
        if (action == null) {
            refreshButton.setVisible(false);
            return;
        }

        bind(action);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        if (refreshActionRegistration != null) {
            refreshActionRegistration.remove();
            refreshActionRegistration = null;
        }
        refreshAction = null;
    }

    @Nullable
    private Action findRefreshAction() {
        Component parent = getParent().orElse(null);
        if (!(parent instanceof HasActions grid)) {
            return null;
        }
        return grid.getActions().stream()
                .filter(RefreshAction.class::isInstance)
                .findFirst()
                .orElse(null);
    }

    private void bind(Action action) {
        refreshAction = action;
        if (clickRegistration == null) {
            refreshButton.setText(action.getText());
            Component actionIcon = action.getIcon();
            if (actionIcon != null) {
                refreshButton.setIcon(ComponentUtils.copyIcon(actionIcon));
            }
            clickRegistration = refreshButton.addClickListener(event -> performRefresh());
        }

        refreshActionRegistration = action.addPropertyChangeListener(event -> updateRefreshButton());
        updateRefreshButton();
    }

    private void performRefresh() {
        if (refreshAction != null && refreshAction.isEnabled()) {
            refreshAction.actionPerform(refreshButton);
            focusGridIfFilled();
        }
    }

    // The empty state, and Refresh with it, is hidden once the grid has rows, which would leave the focus nowhere
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

    private void updateRefreshButton() {
        refreshButton.setVisible(refreshAction != null && refreshAction.isVisible() && refreshAction.isEnabled());
    }
}

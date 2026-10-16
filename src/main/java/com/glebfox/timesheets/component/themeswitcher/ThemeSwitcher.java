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

package com.glebfox.timesheets.component.themeswitcher;

import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.HasMenuItems;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.page.WebStorage;
import io.jmix.core.Messages;
import io.jmix.flowui.kit.theme.ThemeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The component that provides a UI component that allows users to toggle
 * between different themes in an application. Themes include system, light,
 * and dark modes.
 */
public class ThemeSwitcher extends Composite<MenuBar> {

    public static final String SYSTEM_THEME = "system";
    public static final String LIGHT_THEME = "light";
    public static final String DARK_THEME = "dark";

    private Messages messages;

    private final Map<String, MenuItem> menuItems = new HashMap<>(3);

    private MenuItem rootItem;
    private Icon icon;

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    @Override
    protected MenuBar initContent() {
        MenuBar menuBar = super.initContent();
        menuBar.addThemeNames("theme-switcher", "icon");

        icon = createIcon(SYSTEM_THEME);
        rootItem = menuBar.addItem(icon);
        menuBar.setTooltipText(rootItem, messages.getMessage(ThemeSwitcher.class, "rootItem.tooltip"));

        initItems();

        WebStorage.getItem(WebStorage.Storage.LOCAL_STORAGE,
                ThemeUtils.THEME_STORAGE_KEY,
                this::updateState);

        return menuBar;
    }

    private void initItems() {
        SubMenu subMenu = rootItem.getSubMenu();

        menuItems.put(SYSTEM_THEME, createItem(subMenu, createIcon(SYSTEM_THEME),
                messages.getMessage(ThemeSwitcher.class, "theme." + SYSTEM_THEME),
                event -> {
                    ThemeUtils.applySystemTheme();
                    updateState(SYSTEM_THEME);
                }));
        menuItems.put(LIGHT_THEME, createItem(subMenu, createIcon(LIGHT_THEME),
                messages.getMessage(ThemeSwitcher.class, "theme." + LIGHT_THEME),
                event -> {
                    ThemeUtils.applyLightTheme();
                    updateState(LIGHT_THEME);
                }));
        menuItems.put(DARK_THEME, createItem(subMenu, createIcon(DARK_THEME),
                messages.getMessage(ThemeSwitcher.class, "theme." + DARK_THEME),
                event -> {
                    ThemeUtils.applyDarkTheme();
                    updateState(DARK_THEME);
                }));
    }

    private MenuItem createItem(HasMenuItems menu, Component icon, String text,
                                ComponentEventListener<ClickEvent<MenuItem>> clickListener) {
        MenuItem menuItem = menu.addItem(icon, clickListener);
        menuItem.add(new Text(text));
        menuItem.setCheckable(true);

        return menuItem;
    }

    private void updateState(@Nullable String theme) {
        String value = theme == null ? SYSTEM_THEME : theme.toLowerCase();
        menuItems.forEach((key, menuItem) ->
                menuItem.setChecked(key.equals(value)));

        rootItem.remove(icon);
        icon = createIcon(value);
        rootItem.addComponentAsFirst(icon);
    }

    private Icon createIcon(String theme) {
        return switch (theme) {
            case SYSTEM_THEME -> VaadinIcon.ADJUST.create();
            case LIGHT_THEME -> GravityUiIcon.SUN.create();
            case DARK_THEME -> GravityUiIcon.MOON.create();
            default -> throw new IllegalStateException("Unknown theme: " + theme);
        };
    }
}

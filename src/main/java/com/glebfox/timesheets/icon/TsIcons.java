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

package com.glebfox.timesheets.icon;

import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.vaadin.flow.component.Component;
import io.jmix.core.common.util.Preconditions;
import io.jmix.flowui.icon.impl.IconsImpl;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Primary;

/**
 * Draws the icons Jmix names by purpose from the Gravity UI set: each takes the Gravity UI counterpart of the
 * Vaadin icon it maps to in Jmix's {@code jmix-font-icons.css}, except Save, which takes a floppy disk rather than
 * Jmix's archive box. Of the names of a glyph, only those of the close and maximize buttons of dialogs and
 * notifications are drawn here; any other goes on to Jmix. The buttons of standard dialogs
 * ({@code DialogAction.Type}), pagination, the combo button, the twin column, and the tab close button create their
 * icons without this bean and keep the Vaadin ones.
 */
@NullMarked
@Primary
@org.springframework.stereotype.Component("ts_Icons")
public class TsIcons extends IconsImpl {

    @Override
    public Component get(String iconName) {
        Preconditions.checkNotNullArgument(iconName);
        return switch (iconName) {
            case "OK" -> GravityUiIcon.CHECK.create();
            case "CANCEL" -> GravityUiIcon.BAN.create();
            case "YES" -> GravityUiIcon.CHECK.create();
            case "NO" -> GravityUiIcon.BAN.create();

            case "DIALOG_OK" -> GravityUiIcon.CHECK.create();
            case "DIALOG_CANCEL" -> GravityUiIcon.BAN.create();
            case "DIALOG_YES" -> GravityUiIcon.CHECK.create();
            case "DIALOG_NO" -> GravityUiIcon.BAN.create();
            case "DIALOG_CLOSE" -> GravityUiIcon.XMARK.create();

            case "CREATE_ACTION" -> GravityUiIcon.PLUS.create();
            case "EDIT_ACTION" -> GravityUiIcon.PENCIL.create();
            case "REMOVE_ACTION" -> GravityUiIcon.TRASH_BIN.create();
            case "ADD_ACTION" -> GravityUiIcon.PLUS.create();
            case "EXCLUDE_ACTION" -> GravityUiIcon.XMARK.create();
            case "READ_ACTION" -> GravityUiIcon.EYE.create();
            case "REFRESH_ACTION" -> GravityUiIcon.ARROWS_ROTATE_RIGHT.create();

            case "VIEW_CLOSE_ACTION" -> GravityUiIcon.BAN.create();
            case "DETAIL_SAVE_CLOSE_ACTION" -> GravityUiIcon.CHECK.create();
            case "DETAIL_SAVE_ACTION" -> GravityUiIcon.FLOPPY_DISK.create();
            case "DETAIL_CLOSE_ACTION" -> GravityUiIcon.BAN.create();
            case "DETAIL_DISCARD_ACTION" -> GravityUiIcon.BAN.create();
            case "DETAIL_ENABLE_EDITING_ACTION" -> GravityUiIcon.PENCIL.create();
            case "LOOKUP_SELECT_ACTION" -> GravityUiIcon.CHECK.create();
            case "LOOKUP_DISCARD_ACTION" -> GravityUiIcon.BAN.create();

            case "ENTITY_LOOKUP_ACTION" -> GravityUiIcon.ELLIPSIS.create();
            case "ENTITY_CLEAR_ACTION" -> GravityUiIcon.XMARK.create();
            case "ENTITY_OPEN_ACTION" -> GravityUiIcon.MAGNIFIER_PLUS.create();

            case "VALUE_CLEAR_ACTION" -> GravityUiIcon.XMARK.create();
            case "MULTI_VALUE_SELECT_ACTION" -> GravityUiIcon.ELLIPSIS.create();
            case "DATE_INTERVAL_ACTION" -> GravityUiIcon.ELLIPSIS.create();
            case "DAYS_OF_WEEK_EDIT_ACTION" -> GravityUiIcon.ELLIPSIS.create();

            case "LOGOUT_ACTION" -> GravityUiIcon.ARROW_RIGHT_FROM_SQUARE.create();
            case "USER_MENU_THEME_SWITCH_ACTION_SYSTEM_THEME" -> GravityUiIcon.DISPLAY.create();
            case "USER_MENU_THEME_SWITCH_ACTION_LIGHT_THEME" -> GravityUiIcon.SUN.create();
            case "USER_MENU_THEME_SWITCH_ACTION_DARK_THEME" -> GravityUiIcon.MOON.create();
            case "USER_MENU_SUBSTITUTE_USER_ACTION" -> GravityUiIcon.ARROW_RIGHT_ARROW_LEFT.create();

            case "JSON_EXPORT_ACTION" -> GravityUiIcon.FILE_CODE.create();
            case "EXCEL_EXPORT_ACTION" -> GravityUiIcon.FILE_LETTER_X.create();

            case "SHOW_ROLE_ASSIGNMENTS_ACTION" -> GravityUiIcon.SHIELD.create();
            case "ASSIGN_TO_USERS_ACTION" -> GravityUiIcon.PERSONS.create();

            case "GENERIC_FILTER_SAVE_ACTION" -> GravityUiIcon.FLOPPY_DISK.create();
            case "GENERIC_FILTER_EDIT_ACTION" -> GravityUiIcon.PENCIL.create();
            case "GENERIC_FILTER_COPY_ACTION" -> GravityUiIcon.COPY.create();
            case "GENERIC_FILTER_REMOVE_ACTION" -> GravityUiIcon.TRASH_BIN.create();
            case "GENERIC_FILTER_CLEAR_VALUES_ACTION" -> GravityUiIcon.ERASER.create();
            case "GENERIC_FILTER_MAKE_DEFAULT_ACTION" -> GravityUiIcon.STAR.create();
            case "GENERIC_FILTER_ADD_CONDITION_ACTION" -> GravityUiIcon.PLUS.create();
            case "GENERIC_FILTER_SETTINGS" -> GravityUiIcon.GEAR.create();
            case "GENERIC_FILTER_CONDITION_REMOVE" -> GravityUiIcon.TRASH_BIN.create();
            case "INTERVAL_FIELD_HELP" -> GravityUiIcon.CIRCLE_QUESTION.create();

            case "DATA_GRID_HEADER_FILTER" -> GravityUiIcon.FUNNEL.create();
            case "DATA_GRID_HEADER_FILTER_APPLY" -> GravityUiIcon.CHECK.create();
            case "DATA_GRID_HEADER_FILTER_CANCEL" -> GravityUiIcon.BAN.create();
            case "DATA_GRID_HEADER_FILTER_CLEAR" -> GravityUiIcon.ERASER.create();

            case "SIDE_PANEL_LAYOUT_CLOSER" -> GravityUiIcon.XMARK.create();

            // Glyph names: the close and maximize buttons of dialog views, notifications, and pickers' dialogs.
            case "CLOSE_SMALL" -> GravityUiIcon.XMARK.create();
            case "EXPAND" -> GravityUiIcon.CHEVRONS_EXPAND_UP_RIGHT.create();
            case "COMPRESS" -> GravityUiIcon.CHEVRONS_COLLAPSE_UP_RIGHT.create();

            default -> super.get(iconName);
        };
    }
}

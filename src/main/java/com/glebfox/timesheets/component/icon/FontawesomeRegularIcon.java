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

package com.glebfox.timesheets.component.icon;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.IconFactory;

import java.util.Locale;

/**
 * Enumeration of all icons in the <a href="https://fontawesome.com/icons">Font
 * Awesome Free - Regular</a> collection.
 * <p>
 * These instances can be used to create {@link Icon} components either by using
 * their {@link #create()} method or by passing them to Icon's constructor.
 */
@JsModule("./src/component/icon/fontawesome-regular-icons.js")
public enum FontawesomeRegularIcon implements IconFactory {

    ADDRESS_BOOK, ADDRESS_CARD, BELL_SLASH, BELL, BOOKMARK, BUILDING, CALENDAR_CHECK, CALENDAR_DAYS, CALENDAR_MINUS, CALENDAR_PLUS, CALENDAR_XMARK, CALENDAR, CHART_BAR, CHESS_BISHOP, CHESS_KING, CHESS_KNIGHT, CHESS_PAWN, CHESS_QUEEN, CHESS_ROOK, CIRCLE_CHECK, CIRCLE_DOT, CIRCLE_DOWN, CIRCLE_LEFT, CIRCLE_PAUSE, CIRCLE_PLAY, CIRCLE_QUESTION, CIRCLE_RIGHT, CIRCLE_STOP, CIRCLE_UP, CIRCLE_USER, CIRCLE_XMARK, CIRCLE, CLIPBOARD, CLOCK, CLONE, CLOSED_CAPTIONING, COMMENT_DOTS, COMMENT, COMMENTS, COMPASS, COPY, COPYRIGHT, CREDIT_CARD, ENVELOPE_OPEN, ENVELOPE, EYE_SLASH, EYE, FACE_ANGRY, FACE_DIZZY, FACE_FLUSHED, FACE_FROWN_OPEN, FACE_FROWN, FACE_GRIMACE, FACE_GRIN_BEAM_SWEAT, FACE_GRIN_BEAM, FACE_GRIN_HEARTS, FACE_GRIN_SQUINT_TEARS, FACE_GRIN_SQUINT, FACE_GRIN_STARS, FACE_GRIN_TEARS, FACE_GRIN_TONGUE_SQUINT, FACE_GRIN_TONGUE_WINK, FACE_GRIN_TONGUE, FACE_GRIN_WIDE, FACE_GRIN_WINK, FACE_GRIN, FACE_KISS_BEAM, FACE_KISS_WINK_HEART, FACE_KISS, FACE_LAUGH_BEAM, FACE_LAUGH_SQUINT, FACE_LAUGH_WINK, FACE_LAUGH, FACE_MEH_BLANK, FACE_MEH, FACE_ROLLING_EYES, FACE_SAD_CRY, FACE_SAD_TEAR, FACE_SMILE_BEAM, FACE_SMILE_WINK, FACE_SMILE, FACE_SURPRISE, FACE_TIRED, FILE_AUDIO, FILE_CODE, FILE_EXCEL, FILE_IMAGE, FILE_LINES, FILE_PDF, FILE_POWERPOINT, FILE_VIDEO, FILE_WORD, FILE_ZIPPER, FILE, FLAG, FLOPPY_DISK, FOLDER_CLOSED, FOLDER_OPEN, FOLDER, FONT_AWESOME, FUTBOL, GEM, HAND_BACK_FIST, HAND_LIZARD, HAND_PEACE, HAND_POINT_DOWN, HAND_POINT_LEFT, HAND_POINT_RIGHT, HAND_POINT_UP, HAND_POINTER, HAND_SCISSORS, HAND_SPOCK, HAND, HANDSHAKE, HARD_DRIVE, HEART, HOSPITAL, HOURGLASS_HALF, HOURGLASS, ID_BADGE, ID_CARD, IMAGE, IMAGES, KEYBOARD, LEMON, LIFE_RING, LIGHTBULB, MAP, MESSAGE, MONEY_BILL_1, MOON, NEWSPAPER, NOTE_STICKY, OBJECT_GROUP, OBJECT_UNGROUP, PAPER_PLANE, PASTE, PEN_TO_SQUARE, RECTANGLE_LIST, RECTANGLE_XMARK, REGISTERED, SHARE_FROM_SQUARE, SNOWFLAKE, SQUARE_CARET_DOWN, SQUARE_CARET_LEFT, SQUARE_CARET_RIGHT, SQUARE_CARET_UP, SQUARE_CHECK, SQUARE_FULL, SQUARE_MINUS, SQUARE_PLUS, SQUARE, STAR_HALF_STROKE, STAR_HALF, STAR, SUN, THUMBS_DOWN, THUMBS_UP, TRASH_CAN, USER, WINDOW_MAXIMIZE, WINDOW_MINIMIZE, WINDOW_RESTORE;

    /**
     * Creates a new {@link Icon} instance with the icon determined by the name
     * of this instance.
     *
     * @return a new instance of {@link Icon} component
     */
    public Icon create() {
        return new Icon(this.name().toLowerCase(Locale.ENGLISH).replace('_', '-').replaceAll("^-", ""));
    }

    /**
     * A specialized implementation of the {@link com.vaadin.flow.component.icon.Icon}
     * component, utilizing the "fontawesome-regular" icon collection. This class
     * simplifies the creation of icons by predefining the icon collection, requiring
     * only the specific icon name to be provided as a parameter.
     */
    public static final class Icon extends com.vaadin.flow.component.icon.Icon {
        Icon(String icon) {
            super("fontawesome-regular", icon);
        }
    }
}

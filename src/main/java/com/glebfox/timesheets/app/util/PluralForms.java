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

package com.glebfox.timesheets.app.util;

import java.util.Locale;

/**
 * Picks the message key of a counted text, such as "5 workdays", by the plural rules of a locale.
 * <p>
 * A counted text has three keys in every message bundle, so the bundles stay key-for-key in sync:
 * {@code <key>.one}, {@code <key>.few}, and {@code <key>.many}. Russian uses all three: 1, 2, and 5
 * workdays take three different forms. Every other language follows English, which takes {@code one}
 * for 1 and {@code many} for any other count, so its {@code few} text repeats {@code many}.
 */
public final class PluralForms {

    private PluralForms() {
    }

    /**
     * Returns the key of the form that a count takes in a locale.
     *
     * @param key    the key of the counted text, without the form
     * @param count  the count, not negative
     * @param locale the locale of the text
     * @return the key followed by {@code .one}, {@code .few}, or {@code .many}
     */
    public static String key(String key, long count, Locale locale) {
        return key + "." + form(count, locale);
    }

    private static String form(long count, Locale locale) {
        if (!"ru".equals(locale.getLanguage())) {
            return count == 1 ? "one" : "many";
        }

        long lastDigit = count % 10;
        long lastTwoDigits = count % 100;
        if (lastDigit == 1 && lastTwoDigits != 11) {
            return "one";
        }
        if (lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14)) {
            return "few";
        }
        return "many";
    }
}

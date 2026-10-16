/*
 * Copyright 2023 Gleb Gorelov
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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.datatype.SpentTime;
import io.jmix.core.Messages;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility component for parsing time strings and converting them into {@link SpentTime} instances.
 * This class supports multiple formats of time representation, including hours and minutes, total minutes,
 * and strings with units for hours and minutes.
 */
@Component("ts_TimeParser")
public class TimeParser {

    private final Messages messages;

    public TimeParser(Messages messages) {
        this.messages = messages;
    }

    /**
     * Parses a time string and converts it into a {@link SpentTime} instance.
     * The method supports various time formats, including:
     * - Hours and minutes separated by ":" or " " (e.g., "2:30", "2 30").
     * - Numeric values representing hours or total minutes (e.g., "5", "120").
     * - Strings containing time values with unit suffixes for hours or minutes.
     *
     * @param time the time string to be parsed
     * @return a {@link SpentTime} instance representing the parsed time
     * @throws ParseException if the given time string cannot be parsed
     */
    public SpentTime parse(String time) throws ParseException {
        if (time.matches("\\d+[: ]\\d{1,2}")) {
            String[] parts = time.split("[: ]");
            if (parts.length == 2) {
                return new SpentTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }

            throw new ParseException("Cannot parse time string: " + time, 0);
        }

        if (StringUtils.isNumeric(time)) {
            int numericTime = Math.abs(Integer.parseInt(time));
            long hoursInDay = TimeUnit.DAYS.toHours(1);
            if (numericTime <= hoursInDay) {
                return SpentTime.ofHours(numericTime);
            } else {
                return SpentTime.ofMinutes(numericTime);
            }
        }

        int hours = findHours(time);
        int minutes = findMinutes(time);

        if (hours >= 0 || minutes >= 0) {
            return new SpentTime(Math.max(0, hours), Math.max(0, minutes));
        }

        throw new ParseException("Cannot parse time string: " + time, 0);
    }

    private int findHours(String time) {
        return findTimeValue(time, messages.getMessage("datatype.spentTime.hours.units"));
    }

    private int findMinutes(String time) {
        return findTimeValue(time, messages.getMessage("datatype.spentTime.minutes.units"));
    }

    private int findTimeValue(String time, String units) {
        String regex = "-?\\d+\\s*(" + units + ")";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(time);
        if (matcher.find()) {
            String value = matcher.group();
            regex = "-?\\d+";
            pattern = Pattern.compile(regex);
            matcher = pattern.matcher(value);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        }

        return -1;
    }
}
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

package com.glebfox.timesheets.datatype;

import com.glebfox.timesheets.app.TimeParser;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.annotation.DatatypeDef;
import io.jmix.core.metamodel.annotation.Ddl;
import io.jmix.core.metamodel.datatype.Datatype;
import org.springframework.beans.factory.annotation.Autowired;
import org.jspecify.annotations.Nullable;

import java.text.ParseException;
import java.util.Locale;

/**
 * SpentTimeDatatype implements the {@link Datatype} interface to provide formatting and parsing
 * capabilities for the {@link SpentTime} class. This datatype is used to represent and handle
 * time durations expressed in hours and minutes.
 */
@DatatypeDef(
        id = "spentTime",
        javaClass = SpentTime.class,
        defaultForClass = true,
        value = "ts_SpentTimeDatatype"
)
@Ddl("BIGINT")
public class SpentTimeDatatype implements Datatype<SpentTime> {

    @Autowired
    private TimeParser timeParser;
    @Autowired
    private Messages messages;

    @Override
    public String format(@Nullable Object value) {
        return value instanceof SpentTime spentTime
                ? messages.formatMessage("", "datatype.spentTime.format", spentTime.getHours(), spentTime.getMinutes())
                : "";
    }

    @Override
    public String format(@Nullable Object value, Locale locale) {
        return format(value);
    }

    @Nullable
    @Override
    public SpentTime parse(@Nullable String value) throws ParseException {
        if (value == null) {
            return null;
        }

        return timeParser.parse(value);
    }

    @Nullable
    @Override
    public SpentTime parse(@Nullable String value, Locale locale) throws ParseException {
        return parse(value);
    }
}

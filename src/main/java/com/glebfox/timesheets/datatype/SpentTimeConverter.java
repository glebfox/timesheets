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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.Nullable;

/**
 * Converts between {@link SpentTime} objects and their database representation as {@link Long}.
 */
@Converter(autoApply = true)
public class SpentTimeConverter implements AttributeConverter<SpentTime, Long> {

    @Nullable
    @Override
    public Long convertToDatabaseColumn(@Nullable SpentTime attribute) {
        return attribute != null
                ? attribute.toMinutes()
                : null;
    }

    @Nullable
    @Override
    public SpentTime convertToEntityAttribute(@Nullable Long dbData) {
        return dbData != null
                ? SpentTime.ofMinutes(dbData)
                : null;
    }
}

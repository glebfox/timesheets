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

package com.glebfox.timesheets.entity.absence;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates {@link AbsenceDays}: a missing day is left to the days' own required checks.
 */
public class AbsenceDaysValidator implements ConstraintValidator<AbsenceDays, Absence> {

    @Override
    public boolean isValid(Absence absence, ConstraintValidatorContext context) {
        if (absence == null || absence.getFirstDay() == null || absence.getLastDay() == null) {
            return true;
        }
        return !absence.getLastDay().isBefore(absence.getFirstDay());
    }
}

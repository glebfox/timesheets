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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents a period of time in hours and minutes. This class is immutable
 * and thread-safe. It provides utility methods to create and convert time
 * representations using hours, minutes, or decimal hour values.
 */
public class SpentTime implements Serializable, Comparable<SpentTime> {

    /**
     * A constant representing a time period of zero hours and zero minutes.
     */
    public static final SpentTime ZERO = new SpentTime(0, 0);

    private static final BigDecimal MINUTES_IN_HOUR = BigDecimal.valueOf(60);

    private final int hours;
    private final int minutes;

    public SpentTime(int hours, int minutes) {
        Preconditions.checkArgument(hours >= 0, "hours must be positive");
        Preconditions.checkArgument(minutes >= 0 && minutes < 60, "minutes must be between 0 and 59 inclusive");

        this.hours = hours;
        this.minutes = minutes;
    }

    /**
     * Creates a new instance of {@link SpentTime} with the specified hours and minutes.
     *
     * @param hours   the number of hours, must be positive
     * @param minutes the number of minutes, must be between 0 and 59 inclusive
     * @return a new {@link SpentTime} instance representing the specified time period
     */
    public static SpentTime of(int hours, int minutes) {
        return new SpentTime(hours, minutes);
    }

    /**
     * Creates a new instance of {@link SpentTime} with the specified number of hours.
     *
     * @param hours the number of hours, must be posititve
     * @return a new {@link SpentTime} instance representing the specified hours and 0 minutes
     */
    public static SpentTime ofHours(int hours) {
        return new SpentTime(hours, 0);
    }

    /**
     * Creates a new instance of {@link SpentTime} based on the specified number of minutes.
     * The method calculates the equivalent number of hours and remaining minutes.
     *
     * @param minutes the total number of minutes to be converted, must be positive
     * @return a new {@link SpentTime} instance representing the calculated hours and minutes
     */
    public static SpentTime ofMinutes(long minutes) {
        int hours = 0;
        if (minutes >= 60) {
            hours = (int) (minutes / 60);
            minutes = minutes % 60;
        }
        return new SpentTime(hours, (int) minutes);
    }

    /**
     * Creates a new instance of {@link SpentTime} based on the specified number of hours.
     * The hours and fractional minutes are extracted from the input and used to create the instance.
     *
     * @param hours the total number of hours, where the integer part represents
     *              full hours and the fractional part represents minutes
     * @return a new {@link SpentTime} instance with the calculated hours and minutes
     */
    public static SpentTime fromHours(BigDecimal hours) {
        int h = hours.intValue();
        int m = hours.remainder(BigDecimal.ONE).multiply(MINUTES_IN_HOUR).intValue();

        return new SpentTime(h, m);
    }

    /**
     * Retrieves the number of hours stored in this instance.
     *
     * @return the number of hours
     */
    public int getHours() {
        return hours;
    }

    /**
     * Retrieves the number of minutes stored in this instance.
     *
     * @return the number of minutes
     */
    public int getMinutes() {
        return minutes;
    }

    /**
     * Converts the stored hours and minutes into the total number of minutes.
     *
     * @return the total number of minutes represented by the hours and minutes in this instance
     */
    public long toMinutes() {
        return this.hours * 60L + this.minutes;
    }

    /**
     * Converts the time represented by this instance into a decimal value of hours.
     *
     * @return the equivalent time as a {@link BigDecimal}, where the integer part
     * represents full hours and the fractional part represents remaining minutes
     */
    public BigDecimal asHours() {
        return BigDecimal.valueOf(toMinutes()).divide(MINUTES_IN_HOUR, 2, RoundingMode.DOWN);
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpentTime spentTime = (SpentTime) o;
        return hours == spentTime.hours && minutes == spentTime.minutes;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hours, minutes);
    }

    @Override
    public int compareTo(SpentTime o) {
        return asHours().compareTo(o.asHours());
    }
}

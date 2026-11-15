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

package com.glebfox.timesheets.app;

import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Tells the current date and time in the current user's time zone: the one set for the user, or the browser's when
 * none is set. Screens take today from here rather than from the server's clock, so near midnight they agree with
 * the user's own date and with each other.
 */
@Component("ts_UserTimeZoneSupport")
public class UserTimeZoneSupport {

    private final TimeSource timeSource;
    private final CurrentAuthentication currentAuthentication;

    public UserTimeZoneSupport(TimeSource timeSource, CurrentAuthentication currentAuthentication) {
        this.timeSource = timeSource;
        this.currentAuthentication = currentAuthentication;
    }

    /**
     * Returns the current date and time in the current user's time zone.
     *
     * @return the current moment as the user's clock shows it
     */
    public ZonedDateTime now() {
        return timeSource.now().withZoneSameInstant(currentAuthentication.getTimeZone().toZoneId());
    }

    /**
     * Returns today's date in the current user's time zone.
     *
     * @return today's date for the current user
     */
    public LocalDate today() {
        return now().toLocalDate();
    }
}

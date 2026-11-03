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

package com.glebfox.timesheets.component.approval;

import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;

/**
 * The CSS classes by which the parts of the approval screen take the colors of the time entry statuses from the
 * theme, such as the bars of a fingerprint and the segments of a status bar.
 */
final class ApprovalStatusClassNames {

    private ApprovalStatusClassNames() {
    }

    /**
     * Returns the class of a status.
     *
     * @param status the status of an entry or of a day
     * @return {@code new}, {@code approved}, {@code rejected}, or {@code closed}
     */
    static String of(TimeEntryStatus status) {
        return switch (status) {
            case NEW -> "new";
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
            case CLOSED -> "closed";
        };
    }
}

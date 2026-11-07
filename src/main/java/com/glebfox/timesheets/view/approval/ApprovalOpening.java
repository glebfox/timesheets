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

package com.glebfox.timesheets.view.approval;

import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.entity.user.User;
import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * The state another screen opens the approval screen in.
 *
 * @param mode   the mode to show, which is not remembered as the user's own choice
 * @param period the period to show, of the same mode
 * @param scope  which people the roster shows
 * @param person the person to select; when the roster does not show them, the first person shown is selected
 */
public record ApprovalOpening(ApprovalMode mode, ApprovalPeriod period, ApprovalRosterScope scope, User person) {

    public ApprovalOpening {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(person, "person");
        Preconditions.checkArgument(period.mode() == mode, "The period is a %s, while the mode is %s",
                period.mode(), mode);
    }
}

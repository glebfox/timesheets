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

import com.glebfox.timesheets.datatype.SpentTime;
import com.google.common.base.Preconditions;

/**
 * The time actually spent in a period against the time planned for it.
 *
 * @param fact the time spent
 * @param plan the time planned
 */
public record FactAndPlan(SpentTime fact, SpentTime plan) {

    /**
     * No time spent and none planned.
     */
    public static final FactAndPlan ZERO = new FactAndPlan(SpentTime.ZERO, SpentTime.ZERO);

    public FactAndPlan {
        Preconditions.checkNotNull(fact);
        Preconditions.checkNotNull(plan);
    }

    /**
     * Returns the sum of this and the specified fact and plan, adding facts to facts and plans to plans.
     *
     * @param other the fact and plan to add
     * @return a new {@link FactAndPlan} instance representing the sum
     */
    public FactAndPlan plus(FactAndPlan other) {
        return new FactAndPlan(fact.plus(other.fact), plan.plus(other.plan));
    }

    /**
     * @return {@code true} if the time spent equals the time planned
     */
    public boolean isMatch() {
        return fact.compareTo(plan) == 0;
    }

    /**
     * @return {@code true} if less time is spent than planned
     */
    public boolean isInsufficient() {
        return fact.compareTo(plan) < 0;
    }

    /**
     * @return {@code true} if more time is spent than planned
     */
    public boolean isExcess() {
        return fact.compareTo(plan) > 0;
    }
}

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

public class FactAndPlan {

    private SpentTime fact;
    private SpentTime plan;

    public FactAndPlan() {
        this(SpentTime.ofHours(0), SpentTime.ofHours(0));
    }

    public FactAndPlan(SpentTime fact, SpentTime plan) {
        this.fact = fact;
        this.plan = plan;
    }

    public SpentTime getFact() {
        return fact;
    }

    public SpentTime getPlan() {
        return plan;
    }

    public void plusPlan(SpentTime plan) {
        this.plan = SpentTime.fromHours(this.plan.asHours().add(plan.asHours()));
    }

    public void plusFact(SpentTime fact) {
        this.fact = SpentTime.fromHours(this.fact.asHours().add(fact.asHours()));
    }

    public boolean isMatch() {
        return fact.compareTo(plan) == 0;
    }

    public boolean isInsufficient() {
        return fact.compareTo(plan) < 0;
    }

    public boolean isExcess() {
        return fact.compareTo(plan) > 0;
    }
}

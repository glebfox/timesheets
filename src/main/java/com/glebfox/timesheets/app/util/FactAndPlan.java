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

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

package com.glebfox.timesheets.component.daterange;

import com.glebfox.timesheets.app.util.DateRange;
import com.google.common.base.Strings;
import com.vaadin.flow.component.customfield.CustomField;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.querycondition.PropertyCondition;
import io.jmix.flowui.component.filter.SingleFilterComponentBase;
import io.jmix.flowui.model.DataLoader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Filters the entities of a {@link DataLoader} by a range of days that the user enters in a {@link DateRangeField}:
 * it keeps the entities whose own range, from the start property to the end property, shares at least one day with
 * the field's range, both ends included. An empty side of the field's range sets no limit on that side, and an empty
 * field sets none.
 * <p>
 * A range whose end is before its start is not applied: the field shows its error, and the loader keeps the last
 * valid range until the range is valid again.
 */
@NullMarked
public class DateRangeFilter extends SingleFilterComponentBase<DateRange> {

    // Each condition has no property and no parameter name until the setters give them, and no value while its side
    // of the field's range is empty, which drops it from the query

    // The entity's last day is not before the field's first day
    private final PropertyCondition endPropertyCondition =
            createCondition(PropertyCondition.Operation.GREATER_OR_EQUAL);
    // The entity's first day is not after the field's last day
    private final PropertyCondition startPropertyCondition =
            createCondition(PropertyCondition.Operation.LESS_OR_EQUAL);

    private @Nullable String parameterName;

    private static PropertyCondition createCondition(String operation) {
        PropertyCondition condition = new PropertyCondition().skipNullOrEmpty();
        condition.setOperation(operation);
        return condition;
    }

    @Override
    protected LogicalCondition createQueryCondition() {
        // Called by the base class once the fields are initialized
        return LogicalCondition.and(endPropertyCondition, startPropertyCondition);
    }

    @Override
    protected void onFilterValueChanged(ComponentValueChangeEvent<CustomField<DateRange>, DateRange> event) {
        // A range whose end is before its start neither changes the condition nor loads: the list stays as it was,
        // on the same page
        if (isApplicable(event.getValue())) {
            super.onFilterValueChanged(event);
        }
    }

    @Override
    protected void updateQueryCondition(@Nullable DateRange value) {
        // The condition keeps the last valid range, which apply() then loads by
        if (isApplicable(value)) {
            endPropertyCondition.setParameterValue(value != null ? value.start() : null);
            startPropertyCondition.setParameterValue(value != null ? value.end() : null);
        }
    }

    private static boolean isApplicable(@Nullable DateRange value) {
        return value == null || !value.isEndBeforeStart();
    }

    /**
     * @return the property of the entity's first day
     */
    public @Nullable String getStartProperty() {
        return startPropertyCondition.getProperty();
    }

    /**
     * Sets the property of the entity's first day, which may not be after the field's last day.
     *
     * @param startProperty a property of the loader's entity
     */
    public void setStartProperty(String startProperty) {
        checkState(getStartProperty() == null, "Start property has already been initialized");
        checkNotNull(startProperty);

        startPropertyCondition.setProperty(startProperty);
        updateLabelId();
    }

    /**
     * @return the property of the entity's last day
     */
    public @Nullable String getEndProperty() {
        return endPropertyCondition.getProperty();
    }

    /**
     * Sets the property of the entity's last day, which may not be before the field's first day.
     *
     * @param endProperty a property of the loader's entity
     */
    public void setEndProperty(String endProperty) {
        checkState(getEndProperty() == null, "End property has already been initialized");
        checkNotNull(endProperty);

        endPropertyCondition.setProperty(endProperty);
        updateLabelId();
    }

    // The label was created before the filter had its id and properties
    private void updateLabelId() {
        if (label != null) {
            label.setId(getInnerComponentPrefix() + "label");
        }
    }

    @Override
    public String getInnerComponentPrefix() {
        return getId().orElseGet(() -> "dateRangeFilter_" + Strings.nullToEmpty(getStartProperty())
                + "_" + Strings.nullToEmpty(getEndProperty())) + "_";
    }

    /**
     * Returns the base of the two parameter names: the field's first day is the parameter {@code <base>_start}, and
     * its last day is {@code <base>_end}.
     *
     * @return the base, or {@code null} until it is set
     */
    // Null until set, although the interface declares it non-null, as Jmix's PropertyFilter returns its own
    @SuppressWarnings("NullableProblems")
    @Override
    public @Nullable String getParameterName() {
        return parameterName;
    }

    /**
     * Sets the base of the two parameter names, {@code <base>_start} and {@code <base>_end}. Without it, the loader's
     * query fails on the first load.
     *
     * @param parameterName the base, unique among the loader's parameters
     */
    @Override
    public void setParameterName(String parameterName) {
        checkState(this.parameterName == null, "Parameter name has already been initialized");
        checkNotNull(parameterName);

        this.parameterName = parameterName;
        endPropertyCondition.setParameterName(parameterName + "_start");
        startPropertyCondition.setParameterName(parameterName + "_end");
    }
}

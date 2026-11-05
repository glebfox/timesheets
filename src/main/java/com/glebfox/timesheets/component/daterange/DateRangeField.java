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
import com.glebfox.timesheets.app.util.DateRangeFormatter;
import com.google.common.base.Strings;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.datatype.Datatype;
import io.jmix.core.metamodel.datatype.DatatypeRegistry;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.HasRequired;
import io.jmix.flowui.component.SupportsValidation;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.delegate.FieldDelegate;
import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.exception.ValidationException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDate;
import java.util.List;

/**
 * A range of days entered in two date pickers joined by a dash, which the user sees and hears as one field: one
 * label, one required mark, one helper line, and one error message under both pickers. Its value is a
 * {@link DateRange} of the dates as entered, or {@code null} while both are empty; what an empty end means is up to
 * the use.
 * <p>
 * The end picker offers no day before the start and opens at the start's month, and an end before the start is
 * refused with the field's own message. Required means that the start is required. An error that a view sets on
 * the field, with {@link #setInvalid(boolean)} and then {@link #setErrorMessage(String)}, is dropped at the next
 * change of either date. The field validates a new range and updates its summary before any other listener of its
 * value hears of the change.
 * <p>
 * Bound to two attributes of an item through a {@link DateRangeValueSource}, the field is required when the first
 * day's attribute is mandatory.
 */
public class DateRangeField extends CustomField<DateRange>
        implements SupportsValueSource<DateRange>, SupportsValidation<DateRange>, HasRequired,
        ApplicationContextAware, InitializingBean {

    public static final String CLASS_NAME = "date-range-field";
    // On the host while the pickers have clear buttons, for the stylesheet's wider stacking width
    public static final String CLEARABLE_CLASS_NAME = CLASS_NAME + "-clearable";

    private ApplicationContext applicationContext;
    private Messages messages;
    private DateRangeFormatter dateRangeFormatter;

    private FieldDelegate<DateRangeField, DateRange, DateRange> fieldDelegate;

    private TypedDatePicker<LocalDate> startPicker;
    private TypedDatePicker<LocalDate> endPicker;

    private @Nullable String startAriaLabel;
    private @Nullable String endAriaLabel;
    private boolean summaryVisible;

    public DateRangeField() {
        // The value is updated from the pickers' own changes: a plain custom field regenerates it only on the
        // client's change event, which a change made on the server never fires
        super(null, true);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        messages = applicationContext.getBean(Messages.class);
        dateRangeFormatter = applicationContext.getBean(DateRangeFormatter.class);

        //noinspection unchecked
        fieldDelegate = applicationContext.getBean(FieldDelegate.class, this);
        fieldDelegate.addValidator(this::validateRange);

        initComponent();
    }

    private void initComponent() {
        addClassName(CLASS_NAME);

        // Registered before anything else can listen, so every other listener of the value sees the new range's
        // validation and summary, and a view's mark set from such a listener stays
        addValueChangeListener(event -> onValueChange());

        UiComponents uiComponents = applicationContext.getBean(UiComponents.class);
        Datatype<LocalDate> datatype = applicationContext.getBean(DatatypeRegistry.class).get(LocalDate.class);

        startPicker = createPicker(uiComponents, datatype);
        startPicker.addTypedValueChangeListener(event -> {
            followStart(event.getValue());
            onDatesChange(event.isFromClient());
        });

        endPicker = createPicker(uiComponents, datatype);
        endPicker.addTypedValueChangeListener(event -> onDatesChange(event.isFromClient()));

        setStartAriaLabel(null);
        setEndAriaLabel(null);

        Span separator = new Span(getMessage("separator.text"));
        separator.addClassName(CLASS_NAME + "-separator");

        Div row = new Div(startPicker, separator, endPicker);
        row.addClassName(CLASS_NAME + "-row");
        add(row);
    }

    @SuppressWarnings("unchecked")
    private static TypedDatePicker<LocalDate> createPicker(UiComponents uiComponents, Datatype<LocalDate> datatype) {
        TypedDatePicker<LocalDate> picker = uiComponents.create(TypedDatePicker.class);
        picker.setDatatype(datatype);
        return picker;
    }

    // The end picker offers no day before the start and, while the end is empty, opens at the start rather than at
    // today, which may be months away from the range
    private void followStart(@Nullable LocalDate start) {
        endPicker.setMin(start);
        // Once the picker has opened, Vaadin 25.3's overlay keeps the month of its first opening: a later change of
        // the first day moves the minimum and the position set here, but not the month the picker opens at, as
        // vaadin-date-picker-mixin's _getInitialPosition prefers the overlay content's own initialPosition. Seen
        // live on 2026-11-04; left as it is, since a workaround would reach into the component's internals
        endPicker.setInitialPosition(start);
    }

    private void onDatesChange(boolean fromClient) {
        // While the field's own value is being shown, the field support folds the pickers' changes into that value,
        // which then changes once. A date the user picks stays a change from the client, as a filter applies only
        // those
        setModelValue(generateModelValue(), fromClient);
    }

    private void onValueChange() {
        // Unlike executeValidators(), this also resets the mark of a view's error, so the error is dropped and the
        // field's own validation runs again
        setInvalid(false);
        updateSummary();
    }

    private void validateRange(@Nullable DateRange range) {
        // An empty field is refused by the delegate's own required check, before the validators run
        if (range == null) {
            return;
        }

        if (isRequired() && range.start() == null) {
            throw new ValidationException(getRequiredErrorMessage());
        }
        if (range.isEndBeforeStart()) {
            throw new ValidationException(getMessage("endBeforeStartError"));
        }
    }

    // The message the delegate gives an empty required field
    private String getRequiredErrorMessage() {
        String requiredMessage = getRequiredMessage();
        if (!Strings.isNullOrEmpty(requiredMessage)) {
            return requiredMessage;
        }

        String label = getLabel();
        return Strings.isNullOrEmpty(label)
                ? messages.getMessage("validationFail.defaultRequiredMessage")
                : messages.formatMessage("", "validationFail.defaultRequiredMessageWithFieldLabel", label);
    }

    // The pickers look refused while the field is, and show no message of their own: the field's one message is
    // under both. The end picker validates itself on each change of its date and refuses one before its minimum
    // with the framework's "Value should be greater than" message, which is cleared here; marking a picker does not
    // run its validators, so the message stays cleared, and a date picker validates manually, so an empty message
    // shows nothing
    private void updatePickersInvalid() {
        boolean invalid = isInvalid();
        for (TypedDatePicker<LocalDate> picker : List.of(startPicker, endPicker)) {
            if (invalid) {
                picker.setInvalid(true);
                picker.setErrorMessage(null);
            } else if (picker.isInvalid()) {
                // Also validates the end against its new minimum, which a new minimum alone does not
                picker.setInvalid(false);
            }
        }
    }

    @Override
    protected @Nullable DateRange generateModelValue() {
        return DateRange.of(startPicker.getTypedValue(), endPicker.getTypedValue());
    }

    @Override
    protected void setPresentationValue(@Nullable DateRange range) {
        // The start first, so that the end picker's minimum follows it before the end is set
        startPicker.setTypedValue(range != null ? range.start() : null);
        endPicker.setTypedValue(range != null ? range.end() : null);
    }

    public TypedDatePicker<LocalDate> getStartPicker() {
        return startPicker;
    }

    public TypedDatePicker<LocalDate> getEndPicker() {
        return endPicker;
    }

    public @Nullable String getStartPlaceholder() {
        return startPicker.getPlaceholder();
    }

    public void setStartPlaceholder(@Nullable String placeholder) {
        startPicker.setPlaceholder(placeholder);
    }

    public @Nullable String getEndPlaceholder() {
        return endPicker.getPlaceholder();
    }

    public void setEndPlaceholder(@Nullable String placeholder) {
        endPicker.setPlaceholder(placeholder);
    }

    public String getStartAriaLabel() {
        return startAriaLabel != null ? startAriaLabel : getMessage("startAriaLabel");
    }

    /**
     * Sets the start picker's name for screen readers.
     *
     * @param ariaLabel the name, or {@code null} for the default, "First day"
     */
    public void setStartAriaLabel(@Nullable String ariaLabel) {
        startAriaLabel = ariaLabel;
        startPicker.setAriaLabel(getStartAriaLabel());
    }

    public String getEndAriaLabel() {
        return endAriaLabel != null ? endAriaLabel : getMessage("endAriaLabel");
    }

    /**
     * Sets the end picker's name for screen readers.
     *
     * @param ariaLabel the name, or {@code null} for the default, "Last day"
     */
    public void setEndAriaLabel(@Nullable String ariaLabel) {
        endAriaLabel = ariaLabel;
        endPicker.setAriaLabel(getEndAriaLabel());
    }

    public boolean isClearButtonVisible() {
        return startPicker.isClearButtonVisible();
    }

    /**
     * Sets whether both pickers show a button that clears their date.
     */
    public void setClearButtonVisible(boolean clearButtonVisible) {
        startPicker.setClearButtonVisible(clearButtonVisible);
        endPicker.setClearButtonVisible(clearButtonVisible);
        setClassName(CLEARABLE_CLASS_NAME, clearButtonVisible);
    }

    public boolean isSummaryVisible() {
        return summaryVisible;
    }

    /**
     * Sets whether the helper line shows the number of days and their weekdays, such as "14 days, Monday to Sunday",
     * an empty end counting as one day. The summary is empty while the start is empty or the end is before it.
     * Off by default.
     */
    public void setSummaryVisible(boolean summaryVisible) {
        if (this.summaryVisible != summaryVisible) {
            this.summaryVisible = summaryVisible;
            setHelperText(summaryVisible ? formatSummary() : null);
        }
    }

    private void updateSummary() {
        if (summaryVisible) {
            setHelperText(formatSummary());
        }
    }

    private String formatSummary() {
        DateRange range = getValue();
        LocalDate start = range != null ? range.start() : null;
        return range != null && start != null
                ? dateRangeFormatter.formatSummary(start, range.endOrStart())
                : "";
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        startPicker.setReadOnly(readOnly);
        endPicker.setReadOnly(readOnly);
    }

    @Override
    public @Nullable ValueSource<DateRange> getValueSource() {
        return fieldDelegate.getValueSource();
    }

    @Override
    public void setValueSource(@Nullable ValueSource<DateRange> valueSource) {
        fieldDelegate.setValueSource(valueSource);

        // The binding marks a field required only for a value source of one attribute. The required message stays
        // as given, or the field's own with its label
        if (valueSource instanceof DateRangeValueSource<?> rangeSource && rangeSource.isStartMandatory()) {
            setRequired(true);
        }
    }

    @Override
    public Registration addValidator(Validator<? super DateRange> validator) {
        return fieldDelegate.addValidator(validator);
    }

    @Override
    public void executeValidators() throws ValidationException {
        try {
            fieldDelegate.executeValidators();
        } finally {
            updatePickersInvalid();
        }
    }

    @Override
    public boolean isInvalid() {
        return fieldDelegate.isInvalid();
    }

    @Override
    public void setInvalid(boolean invalid) {
        fieldDelegate.setInvalid(invalid);
        updatePickersInvalid();
    }

    @Override
    public @Nullable String getErrorMessage() {
        return fieldDelegate.getErrorMessage();
    }

    @Override
    public void setErrorMessage(@Nullable String errorMessage) {
        fieldDelegate.setErrorMessage(errorMessage);
    }

    @Override
    public void setRequired(boolean required) {
        HasRequired.super.setRequired(required);
        fieldDelegate.updateRequiredState();
        updatePickersInvalid();
    }

    @Override
    public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        super.setRequiredIndicatorVisible(requiredIndicatorVisible);
        fieldDelegate.updateRequiredState();
        updatePickersInvalid();
    }

    @Override
    public @Nullable String getRequiredMessage() {
        return fieldDelegate.getRequiredMessage();
    }

    @Override
    public void setRequiredMessage(@Nullable String requiredMessage) {
        fieldDelegate.setRequiredMessage(requiredMessage);
        updatePickersInvalid();
    }

    private String getMessage(String key) {
        return messages.getMessage(DateRangeField.class, key);
    }
}

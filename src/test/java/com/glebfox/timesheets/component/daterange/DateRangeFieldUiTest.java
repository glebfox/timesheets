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

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.util.DateRange;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class DateRangeFieldUiTest {

    static final Locale RUSSIAN = Locale.of("ru", "RU");

    static final LocalDate MAR_1 = LocalDate.of(2026, 3, 1);
    // A Monday
    static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_3 = LocalDate.of(2026, 3, 3);
    static final LocalDate MAR_8 = LocalDate.of(2026, 3, 8);
    static final LocalDate MAR_9 = LocalDate.of(2026, 3, 9);
    static final LocalDate MAR_10 = LocalDate.of(2026, 3, 10);
    static final LocalDate MAR_11 = LocalDate.of(2026, 3, 11);
    static final LocalDate MAR_13 = LocalDate.of(2026, 3, 13);
    static final LocalDate MAR_14 = LocalDate.of(2026, 3, 14);
    static final LocalDate MAR_15 = LocalDate.of(2026, 3, 15);

    @Autowired
    UiComponents uiComponents;

    @Autowired
    Messages messages;

    DateRangeField field;

    @BeforeEach
    void setUp() {
        field = uiComponents.create(DateRangeField.class);
    }

    @Test
    void valueHoldsTheDatesAsEntered() {
        field.getStartPicker().setTypedValue(MAR_2);
        assertThat(field.getValue()).isEqualTo(new DateRange(MAR_2, null));

        field.getEndPicker().setTypedValue(MAR_13);
        assertThat(field.getValue()).isEqualTo(new DateRange(MAR_2, MAR_13));

        field.getStartPicker().setTypedValue(null);
        assertThat(field.getValue()).isEqualTo(new DateRange(null, MAR_13));

        field.getEndPicker().setTypedValue(null);
        assertThat(field.getValue()).isNull();
    }

    @Test
    void setValueShowsTheDates() {
        field.setValue(new DateRange(MAR_2, MAR_13));

        assertThat(field.getStartPicker().getTypedValue()).isEqualTo(MAR_2);
        assertThat(field.getEndPicker().getTypedValue()).isEqualTo(MAR_13);
        assertThat(field.getValue()).isEqualTo(new DateRange(MAR_2, MAR_13));

        // The field has no one-day rule of its own: an end equal to the start is shown as it is
        field.setValue(new DateRange(MAR_2, MAR_2));

        assertThat(field.getEndPicker().getTypedValue()).isEqualTo(MAR_2);
        assertThat(field.getValue()).isEqualTo(new DateRange(MAR_2, MAR_2));
    }

    @Test
    void endPickerFollowsTheStart() {
        field.getStartPicker().setTypedValue(MAR_10);

        assertThat(field.getEndPicker().getMin()).isEqualTo(MAR_10);
        assertThat(field.getEndPicker().getInitialPosition()).isEqualTo(MAR_10);

        field.getStartPicker().setTypedValue(null);

        assertThat(field.getEndPicker().getMin()).isNull();
    }

    @Test
    void endBeforeStartIsRefusedWithTheFieldsMessage() {
        field.getStartPicker().setTypedValue(MAR_10);
        field.getEndPicker().setTypedValue(MAR_9);

        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo("The last day must not be before the first day");
        assertThatThrownBy(field::executeValidators)
                .isInstanceOf(ValidationException.class)
                .hasMessage("The last day must not be before the first day");
        // Both dates look refused, and only the field's message shows, not the end picker's "Value should be greater"
        assertThat(field.getStartPicker().isInvalid()).isTrue();
        assertThat(field.getEndPicker().isInvalid()).isTrue();
        assertThat(field.getStartPicker().getErrorMessage()).isNullOrEmpty();
        assertThat(field.getEndPicker().getErrorMessage()).isNullOrEmpty();
        assertThat(messages.getMessage(DateRangeField.class, "endBeforeStartError", RUSSIAN))
                .isEqualTo("Последний день не может быть раньше первого");
    }

    @Test
    void endBeforeStartClearsWhenTheStartMovesBeforeIt() {
        field.getStartPicker().setTypedValue(MAR_10);
        field.getEndPicker().setTypedValue(MAR_9);

        field.getStartPicker().setTypedValue(MAR_8);

        assertThat(field.isInvalid()).isFalse();
        assertThat(field.getStartPicker().isInvalid()).isFalse();
        assertThat(field.getEndPicker().isInvalid()).isFalse();
    }

    @Test
    void requiredRefusesAnEmptyStartEvenWithAnEnd() {
        field.setRequired(true);
        field.setRequiredMessage("Dates required");

        field.getEndPicker().setTypedValue(MAR_13);

        assertThatThrownBy(field::executeValidators)
                .isInstanceOf(ValidationException.class)
                .hasMessage("Dates required");

        field.getEndPicker().setTypedValue(null);

        assertThatThrownBy(field::executeValidators)
                .isInstanceOf(ValidationException.class)
                .hasMessage("Dates required");

        field.getStartPicker().setTypedValue(MAR_2);

        assertThatCode(field::executeValidators).doesNotThrowAnyException();
    }

    @Test
    void viewErrorIsDroppedOnTheNextChangeOfEitherDate() {
        field.setValue(new DateRange(MAR_2, MAR_13));

        refuseAsAView();
        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo("Overlaps");

        field.getEndPicker().setTypedValue(MAR_14);
        assertThat(field.isInvalid()).isFalse();

        // A change of the required state re-evaluates the invalid state, which would mark the field again had it
        // kept the view's error; a new required message re-evaluates only a field that is invalid and empty
        field.setRequiredMessage("Dates required");
        field.setRequired(true);
        assertThat(field.isInvalid()).isFalse();

        refuseAsAView();
        field.getStartPicker().setTypedValue(MAR_3);
        assertThat(field.isInvalid()).isFalse();
    }

    @Test
    void valueListenersSeeTheNewRangesValidationAndSummary() {
        record Seen(boolean invalid, @Nullable String errorMessage, @Nullable String helperText) {
        }
        field.setSummaryVisible(true);
        List<Seen> seen = new ArrayList<>();
        field.addValueChangeListener(event ->
                seen.add(new Seen(field.isInvalid(), field.getErrorMessage(), field.getHelperText())));

        field.getStartPicker().setTypedValue(MAR_10);
        field.getEndPicker().setTypedValue(MAR_9);
        field.getEndPicker().setTypedValue(MAR_11);
        // Both dates at once, one change
        field.setValue(new DateRange(MAR_2, MAR_15));

        assertThat(seen).containsExactly(
                new Seen(false, null, "1 day, Tuesday"),
                new Seen(true, "The last day must not be before the first day", ""),
                new Seen(false, null, "2 days, Tuesday to Wednesday"),
                new Seen(false, null, "14 days, Monday to Sunday"));
    }

    @Test
    void viewErrorSetByAValueListenerStays() {
        field.addValueChangeListener(event -> {
            if (event.getValue() != null && MAR_14.equals(event.getValue().end())) {
                refuseAsAView();
            }
        });
        field.setValue(new DateRange(MAR_2, MAR_13));

        field.getEndPicker().setTypedValue(MAR_14);

        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo("Overlaps");
        assertThat(field.getStartPicker().isInvalid()).isTrue();
        assertThat(field.getEndPicker().isInvalid()).isTrue();
    }

    private void refuseAsAView() {
        // As a view refuses a range: the field is marked, then given the message
        field.setInvalid(true);
        field.setErrorMessage("Overlaps");
    }

    @Test
    void summaryNamesTheDaysWhenVisible() {
        field.setValue(new DateRange(MAR_2, MAR_15));

        assertThat(field.isSummaryVisible()).isFalse();
        assertThat(field.getHelperText()).isNullOrEmpty();

        field.setSummaryVisible(true);
        assertThat(field.getHelperText()).isEqualTo("14 days, Monday to Sunday");

        field.getEndPicker().setTypedValue(null);
        assertThat(field.getHelperText()).isEqualTo("1 day, Monday");

        field.getEndPicker().setTypedValue(MAR_1);
        assertThat(field.getHelperText()).isNullOrEmpty();

        field.getEndPicker().setTypedValue(MAR_15);
        field.getStartPicker().setTypedValue(null);
        assertThat(field.getHelperText()).isNullOrEmpty();

        field.getStartPicker().setTypedValue(MAR_2);
        field.setSummaryVisible(false);
        assertThat(field.getHelperText()).isNullOrEmpty();
    }

    @Test
    void pickersHaveTheirNamesForScreenReaders() {
        assertThat(field.getStartPicker().getAriaLabel()).contains("First day");
        assertThat(field.getEndPicker().getAriaLabel()).contains("Last day");

        field.setStartAriaLabel("From");
        field.setEndAriaLabel("To");

        assertThat(field.getStartPicker().getAriaLabel()).contains("From");
        assertThat(field.getEndPicker().getAriaLabel()).contains("To");
        assertThat(field.getStartAriaLabel()).isEqualTo("From");
        assertThat(field.getEndAriaLabel()).isEqualTo("To");

        field.setStartAriaLabel(null);
        field.setEndAriaLabel(null);

        assertThat(field.getStartPicker().getAriaLabel()).contains("First day");
        assertThat(field.getEndPicker().getAriaLabel()).contains("Last day");
        assertThat(field.getStartAriaLabel()).isEqualTo("First day");
        assertThat(field.getEndAriaLabel()).isEqualTo("Last day");
    }

    @Test
    void placeholdersAndClearButtonsReachThePickers() {
        field.setStartPlaceholder("From");
        field.setEndPlaceholder("To");

        assertThat(field.getStartPicker().getPlaceholder()).isEqualTo("From");
        assertThat(field.getEndPicker().getPlaceholder()).isEqualTo("To");
        assertThat(field.getStartPlaceholder()).isEqualTo("From");
        assertThat(field.getEndPlaceholder()).isEqualTo("To");

        assertThat(field.isClearButtonVisible()).isFalse();

        field.setClearButtonVisible(true);

        assertThat(field.isClearButtonVisible()).isTrue();
        assertThat(field.getStartPicker().isClearButtonVisible()).isTrue();
        assertThat(field.getEndPicker().isClearButtonVisible()).isTrue();

        field.setClearButtonVisible(false);

        assertThat(field.isClearButtonVisible()).isFalse();
        assertThat(field.getStartPicker().isClearButtonVisible()).isFalse();
        assertThat(field.getEndPicker().isClearButtonVisible()).isFalse();
    }

    @Test
    void readOnlyReachesBothPickers() {
        field.setReadOnly(true);

        assertThat(field.getStartPicker().isReadOnly()).isTrue();
        assertThat(field.getEndPicker().isReadOnly()).isTrue();

        field.setReadOnly(false);

        assertThat(field.getStartPicker().isReadOnly()).isFalse();
        assertThat(field.getEndPicker().isReadOnly()).isFalse();
    }
}

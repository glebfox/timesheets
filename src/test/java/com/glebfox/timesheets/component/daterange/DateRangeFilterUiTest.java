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
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class DateRangeFilterUiTest {

    static final String NAME_PREFIX = "date-range-filter-ui-test-";

    static final LocalDate FEB_23 = LocalDate.of(2026, 2, 23);
    static final LocalDate FEB_27 = LocalDate.of(2026, 2, 27);
    static final LocalDate MAR_1 = LocalDate.of(2026, 3, 1);
    static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_9 = LocalDate.of(2026, 3, 9);
    static final LocalDate MAR_10 = LocalDate.of(2026, 3, 10);
    static final LocalDate MAR_12 = LocalDate.of(2026, 3, 12);
    static final LocalDate MAR_13 = LocalDate.of(2026, 3, 13);
    static final LocalDate MAR_14 = LocalDate.of(2026, 3, 14);
    static final LocalDate MAR_20 = LocalDate.of(2026, 3, 20);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;

    List<Id<?>> createdIds = new ArrayList<>();

    User user;
    Absence before;
    Absence sharing;
    Absence after;

    @BeforeEach
    void setUp() {
        user = saveUser();
        AbsenceType type = saveType();
        before = saveAbsence(type, FEB_23, FEB_27);
        sharing = saveAbsence(type, MAR_2, MAR_13);
        after = saveAbsence(type, MAR_14, MAR_20);
    }

    @Test
    void keepsRangesSharingADay() {
        DateRangeFilter filter = openFilter();

        filter(filter, new DateRange(MAR_10, null));

        // A range that ends before the start is left out, one that starts in the range or later is kept
        assertThat(listed()).containsExactly(after.getId(), sharing.getId());

        filter(filter, new DateRange(MAR_10, MAR_12));

        // A range that starts after the end is left out, one that started before the range and runs into it is kept
        assertThat(listed()).containsExactly(sharing.getId());

        filter(filter, new DateRange(MAR_13, MAR_14));

        // Both ends count: a range that ends on the first day, and one that starts on the last day
        assertThat(listed()).containsExactly(after.getId(), sharing.getId());
    }

    @Test
    void emptySideSetsNoLimit() {
        DateRangeFilter filter = openFilter();

        filter(filter, new DateRange(null, MAR_12));

        assertThat(listed()).containsExactly(sharing.getId(), before.getId());

        filter(filter, null);

        assertThat(listed()).containsExactly(after.getId(), sharing.getId(), before.getId());
    }

    @Test
    void endBeforeStartIsNotApplied() {
        DateRangeFilter filter = openFilter();

        filter(filter, new DateRange(MAR_10, MAR_12));

        assertThat(listed()).containsExactly(sharing.getId());

        filter(filter, new DateRange(MAR_10, MAR_9));

        // The list keeps the last valid range, and the field shows why the range is not applied
        assertThat(listed()).containsExactly(sharing.getId());
        assertThat(field(filter).isInvalid()).isTrue();

        // Applied, the range would keep the absences that end on MAR_10 or later and start on MAR_9 or earlier: the
        // same one as above. After a last valid range that keeps more, the list shows that it is not applied
        filter(filter, new DateRange(MAR_10, null));
        filter(filter, new DateRange(MAR_10, MAR_9));

        assertThat(listed()).containsExactly(after.getId(), sharing.getId());
        assertThat(field(filter).isInvalid()).isTrue();
    }

    @Test
    void invalidRangeIsAppliedOnceFixed() {
        DateRangeFilter filter = openFilter();
        // A last valid range that keeps more than the fixed one will, so that the list shows which one applies
        filter(filter, new DateRange(MAR_10, null));
        filter(filter, new DateRange(MAR_10, MAR_9));

        filter(filter, new DateRange(MAR_1, MAR_9));

        // The fixed range applies, not the last valid one
        assertThat(listed()).containsExactly(sharing.getId());
        assertThat(field(filter).isInvalid()).isFalse();

        filter(filter, new DateRange(MAR_14, MAR_14));

        assertThat(listed()).containsExactly(after.getId());
    }

    @Test
    void loadsItsAttributes() {
        DateRangeFilter filter = openFilter();

        assertThat(filter.getStartProperty()).isEqualTo("firstDay");
        assertThat(filter.getEndProperty()).isEqualTo("lastDay");
        assertThat(filter.isLabelVisible()).isFalse();
        assertThat(filter.getChildren().anyMatch(child -> child.hasClassName(DateRangeFilter.CLASS_NAME))).isTrue();
        assertThat(filter.getDataLoader()).isSameAs(absencesDl());

        DateRangeField field = field(filter);

        assertThat(field.getStartPlaceholder()).isEqualTo("From");
        assertThat(field.getEndPlaceholder()).isEqualTo("To");
        assertThat(field.isClearButtonVisible()).isTrue();
    }

    @AfterEach
    void tearDown() {
        // Absences before the user and the type they refer to, each reloaded by id and removed for good
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    /**
     * Opens the test view with its loader limited to the user's absences, and returns the filter.
     */
    private DateRangeFilter openFilter() {
        viewBuilders.view(UiTestUtils.getCurrentView(), DateRangeFilterTestView.class).open();
        absencesDl().setParameter("user", user);
        return UiTestUtils.getComponent(UiTestUtils.getCurrentView(), "periodFilter");
    }

    private static void filter(DateRangeFilter filter, @Nullable DateRange range) {
        filter.setValue(range);
        // A value set from the server does not apply the filter by itself
        filter.apply();
    }

    private static DateRangeField field(DateRangeFilter filter) {
        return (DateRangeField) filter.getValueComponent();
    }

    private static CollectionLoader<Absence> absencesDl() {
        DateRangeFilterTestView view = UiTestUtils.getCurrentView();
        return ViewControllerUtils.getViewData(view).getLoader("absencesDl");
    }

    private static List<UUID> listed() {
        DateRangeFilterTestView view = UiTestUtils.getCurrentView();
        CollectionContainer<Absence> container = ViewControllerUtils.getViewData(view).getContainer("absencesDc");
        return container.getItems().stream()
                .map(Absence::getId)
                .toList();
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername(NAME_PREFIX + UUID.randomUUID());
        return track(dataManager.save(user));
    }

    private AbsenceType saveType() {
        AbsenceType type = dataManager.create(AbsenceType.class);
        type.setName(NAME_PREFIX + UUID.randomUUID());
        return track(dataManager.save(type));
    }

    private Absence saveAbsence(AbsenceType type, LocalDate firstDay, LocalDate lastDay) {
        Absence absence = dataManager.create(Absence.class);
        absence.setUser(user);
        absence.setType(type);
        absence.setFirstDay(firstDay);
        absence.setLastDay(lastDay);
        return track(dataManager.save(absence));
    }

    private <T> T track(T entity) {
        createdIds.add(Id.of(entity));
        return entity;
    }
}

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class CalendarSupportTest {

    @Test
    void weeksTest() {
        LocalDate firstDayOfMonth = LocalDate.of(2024, 12, 1);

        Locale locale = Locale.forLanguageTag("ru-RU");
        DayOfWeek firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();
        WeekFields weekFields = WeekFields.of(firstDayOfWeek, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        int weeksNumber = lastDayOfMonth.get(weekFields.weekOfMonth());

        assertThat(weeksNumber).isEqualTo(6);
    }
}

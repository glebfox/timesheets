/*
 * Copyright 2023 Gleb Gorelov
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

package com.glebfox.timesheets.datatype;

import com.glebfox.timesheets.app.TimeParser;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.metamodel.datatype.Datatype;
import io.jmix.core.metamodel.datatype.DatatypeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.text.ParseException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class WorkTimeTest {

    @Autowired
    DatatypeRegistry datatypeRegistry;
    @Autowired
    TimeParser timeParser;

    @Test
    void typeTest() {
        WorkTime workTime = new WorkTime(4, 30);
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);
        assertThat(workTime.toString()).isEqualTo("04:30");
        assertThat(workTime.asHours()).isEqualTo(new BigDecimal("4.50"));

        workTime = new WorkTime(100, 30);
        assertThat(workTime.getHours()).isEqualTo(100);
        assertThat(workTime.getMinutes()).isEqualTo(30);
        assertThat(workTime.toString()).isEqualTo("100:30");
        assertThat(workTime.asHours()).isEqualTo(new BigDecimal("100.50"));

        workTime = WorkTime.of(4, 30);
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);
        assertThat(workTime.toString()).isEqualTo("04:30");
        assertThat(workTime.asHours()).isEqualTo(new BigDecimal("4.50"));

        workTime = WorkTime.ofHours(100);
        assertThat(workTime.getHours()).isEqualTo(100);
        assertThat(workTime.getMinutes()).isEqualTo(0);
        assertThat(workTime.toString()).isEqualTo("100:00");
        assertThat(workTime.asHours()).isEqualTo(new BigDecimal("100.00"));

        workTime = WorkTime.ofMinutes(150);
        assertThat(workTime.getHours()).isEqualTo(2);
        assertThat(workTime.getMinutes()).isEqualTo(30);
        assertThat(workTime.toString()).isEqualTo("02:30");
        assertThat(workTime.asHours()).isEqualTo(new BigDecimal("2.50"));

        workTime = WorkTime.fromHours(new BigDecimal("2.50"));
        assertThat(workTime.getHours()).isEqualTo(2);
        assertThat(workTime.getMinutes()).isEqualTo(30);
        assertThat(workTime.toString()).isEqualTo("02:30");
        assertThat(workTime.asHours()).isEqualTo(new BigDecimal("2.50"));

        assertThatIllegalArgumentException().isThrownBy(() -> new WorkTime(0, 60));
        assertThatIllegalArgumentException().isThrownBy(() -> new WorkTime(-1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new WorkTime(0, -30));
    }

    @Test
    void arithmeticTest() {
        // 20 minutes is 0.33 as decimal hours, so summing decimal hours loses a minute
        WorkTime third = WorkTime.ofMinutes(20);
        assertThat(third.plus(third).plus(third)).isEqualTo(WorkTime.ofHours(1));

        assertThat(WorkTime.of(7, 45).plus(WorkTime.of(1, 30))).isEqualTo(WorkTime.of(9, 15));
        assertThat(WorkTime.ofHours(8).plus(WorkTime.ZERO)).isEqualTo(WorkTime.ofHours(8));

        assertThat(WorkTime.of(8, 1)).isGreaterThan(WorkTime.ofHours(8));
        assertThat(WorkTime.of(7, 59)).isLessThan(WorkTime.ofHours(8));
        assertThat(WorkTime.ofMinutes(480)).isEqualByComparingTo(WorkTime.ofHours(8));
    }

    @Test
    void timeParserTest() throws ParseException {
        WorkTime workTime = timeParser.parse("04:30");
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = timeParser.parse("4 30");
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = timeParser.parse("4");
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(0);

        workTime = timeParser.parse("30");
        assertThat(workTime.getHours()).isEqualTo(0);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = timeParser.parse("4h30m");
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = timeParser.parse("4hr30min");
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = timeParser.parse("4hour30minutes");
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = timeParser.parse("4h");
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(0);

        workTime = timeParser.parse("30m");
        assertThat(workTime.getHours()).isEqualTo(0);
        assertThat(workTime.getMinutes()).isEqualTo(30);
    }

    @Test
    void datatypeFormatTest() {
        Datatype<WorkTime> datatype = datatypeRegistry.get(WorkTime.class);
        WorkTime workTime = new WorkTime(4, 30);

        assertThat(datatype.format(workTime)).isEqualTo("4h 30m");

        workTime = new WorkTime(100, 30);
        assertThat(datatype.format(workTime)).isEqualTo("100h 30m");
    }

    @Test
    void datatypeParseTest() throws ParseException {
        Datatype<WorkTime> datatype = datatypeRegistry.get(WorkTime.class);

        WorkTime workTime = datatype.parse("04:30");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = datatype.parse("4 30");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = datatype.parse("4");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(0);

        workTime = datatype.parse("30");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(0);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = datatype.parse("4h30m");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = datatype.parse("4hr30min");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = datatype.parse("4hour30minutes");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(30);

        workTime = datatype.parse("4h");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(4);
        assertThat(workTime.getMinutes()).isEqualTo(0);

        workTime = datatype.parse("30m");
        assertThat(workTime).isNotNull();
        assertThat(workTime.getHours()).isEqualTo(0);
        assertThat(workTime.getMinutes()).isEqualTo(30);
    }
}

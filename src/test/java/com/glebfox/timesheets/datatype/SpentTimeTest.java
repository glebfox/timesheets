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
public class SpentTimeTest {

    @Autowired
    DatatypeRegistry datatypeRegistry;
    @Autowired
    TimeParser timeParser;

    @Test
    void typeTest() {
        SpentTime spentTime = new SpentTime(4, 30);
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);
        assertThat(spentTime.toString()).isEqualTo("04:30");
        assertThat(spentTime.asHours()).isEqualTo(new BigDecimal("4.50"));

        spentTime = new SpentTime(100, 30);
        assertThat(spentTime.getHours()).isEqualTo(100);
        assertThat(spentTime.getMinutes()).isEqualTo(30);
        assertThat(spentTime.toString()).isEqualTo("100:30");
        assertThat(spentTime.asHours()).isEqualTo(new BigDecimal("100.50"));

        spentTime = SpentTime.of(4, 30);
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);
        assertThat(spentTime.toString()).isEqualTo("04:30");
        assertThat(spentTime.asHours()).isEqualTo(new BigDecimal("4.50"));

        spentTime = SpentTime.ofHours(100);
        assertThat(spentTime.getHours()).isEqualTo(100);
        assertThat(spentTime.getMinutes()).isEqualTo(0);
        assertThat(spentTime.toString()).isEqualTo("100:00");
        assertThat(spentTime.asHours()).isEqualTo(new BigDecimal("100.00"));

        spentTime = SpentTime.ofMinutes(150);
        assertThat(spentTime.getHours()).isEqualTo(2);
        assertThat(spentTime.getMinutes()).isEqualTo(30);
        assertThat(spentTime.toString()).isEqualTo("02:30");
        assertThat(spentTime.asHours()).isEqualTo(new BigDecimal("2.50"));

        spentTime = SpentTime.fromHours(new BigDecimal("2.50"));
        assertThat(spentTime.getHours()).isEqualTo(2);
        assertThat(spentTime.getMinutes()).isEqualTo(30);
        assertThat(spentTime.toString()).isEqualTo("02:30");
        assertThat(spentTime.asHours()).isEqualTo(new BigDecimal("2.50"));

        assertThatIllegalArgumentException().isThrownBy(() -> new SpentTime(0, 60));
        assertThatIllegalArgumentException().isThrownBy(() -> new SpentTime(-1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new SpentTime(0, -30));
    }

    @Test
    void timeParserTest() throws ParseException {
        SpentTime spentTime = timeParser.parse("04:30");
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = timeParser.parse("4 30");
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = timeParser.parse("4");
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(0);

        spentTime = timeParser.parse("30");
        assertThat(spentTime.getHours()).isEqualTo(0);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = timeParser.parse("4h30m");
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = timeParser.parse("4hr30min");
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = timeParser.parse("4hour30minutes");
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = timeParser.parse("4h");
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(0);

        spentTime = timeParser.parse("30m");
        assertThat(spentTime.getHours()).isEqualTo(0);
        assertThat(spentTime.getMinutes()).isEqualTo(30);
    }

    @Test
    void datatypeFormatTest() {
        Datatype<SpentTime> datatype = datatypeRegistry.get(SpentTime.class);
        SpentTime spentTime = new SpentTime(4, 30);

        assertThat(datatype.format(spentTime)).isEqualTo("4h 30m");

        spentTime = new SpentTime(100, 30);
        assertThat(datatype.format(spentTime)).isEqualTo("100h 30m");
    }

    @Test
    void datatypeParseTest() throws ParseException {
        Datatype<SpentTime> datatype = datatypeRegistry.get(SpentTime.class);

        SpentTime spentTime = datatype.parse("04:30");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = datatype.parse("4 30");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = datatype.parse("4");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(0);

        spentTime = datatype.parse("30");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(0);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = datatype.parse("4h30m");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = datatype.parse("4hr30min");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = datatype.parse("4hour30minutes");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(30);

        spentTime = datatype.parse("4h");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(4);
        assertThat(spentTime.getMinutes()).isEqualTo(0);

        spentTime = datatype.parse("30m");
        assertThat(spentTime).isNotNull();
        assertThat(spentTime.getHours()).isEqualTo(0);
        assertThat(spentTime.getMinutes()).isEqualTo(30);
    }
}

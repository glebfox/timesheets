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

package com.glebfox.timesheets.entity.schedule;

import io.jmix.core.MetadataTools;
import io.jmix.core.annotation.DeletedBy;
import io.jmix.core.annotation.DeletedDate;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Table(name = "TS_HOLIDAY_CALENDAR_DAY", indexes = {
        @Index(name = "IDX_TS_HOLIDAY_CALENDAR_DAY_CALENDAR", columnList = "CALENDAR_ID"),
        @Index(name = "IDX_TS_HOLIDAY_CALENDAR_DAY_UNQ", columnList = "CALENDAR_ID, DATE_", unique = true)
})
@Entity(name = "ts_HolidayCalendarDay")
public class HolidayCalendarDay {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @JoinColumn(name = "CALENDAR_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private HolidayCalendar calendar;

    @Column(name = "DATE_", nullable = false)
    @NotNull
    private LocalDate date;

    @Column(name = "TYPE_", nullable = false)
    @NotNull
    private String type;

    @Column(name = "NAME", nullable = false)
    @NotNull
    private String name;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @DeletedBy
    @Column(name = "DELETED_BY")
    private String deletedBy;

    @DeletedDate
    @Column(name = "DELETED_DATE")
    private OffsetDateTime deletedDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HolidayCalendarDayType getType() {
        return type == null ? null : HolidayCalendarDayType.fromId(type);
    }

    public void setType(HolidayCalendarDayType type) {
        this.type = type == null ? null : type.getId();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public HolidayCalendar getCalendar() {
        return calendar;
    }

    public void setCalendar(HolidayCalendar calendar) {
        this.calendar = calendar;
    }

    public OffsetDateTime getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(OffsetDateTime deletedDate) {
        this.deletedDate = deletedDate;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @InstanceName
    @DependsOnProperties({"date", "name", "type"})
    public String getInstanceName(MetadataTools metadataTools,
                                  DatatypeFormatter datatypeFormatter) {
        return String.format("%s %s (%s)",
                datatypeFormatter.formatLocalDate(date),
                name,
                metadataTools.format(getType()));
    }
}

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

import com.glebfox.timesheets.datatype.WorkTime;
import io.jmix.core.annotation.DeletedBy;
import io.jmix.core.annotation.DeletedDate;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Table(name = "TS_WORK_SCHEDULE", indexes = {
        @Index(name = "IDX_TS_WORK_SCHEDULE_UNQ", columnList = "NAME", unique = true),
        @Index(name = "IDX_TS_WORK_SCHEDULE_HOLIDAY_CALENDAR", columnList = "HOLIDAY_CALENDAR_ID")
})
@Entity(name = "ts_WorkSchedule")
public class WorkSchedule {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME", nullable = false)
    @NotNull
    private String name;

    @Column(name = "DESCRIPTION")
    @Lob
    private String description;

    @NotNull
    @Column(name = "WORKDAY_TIME", nullable = false)
    private WorkTime workdayTime;

    // The flags are not mandatory: a mandatory Boolean is shown as a checkbox that must be checked.
    // The initializers give a new schedule its values; a null flag counts as off.
    @Column(name = "MONDAY")
    private Boolean monday = true;

    @Column(name = "TUESDAY")
    private Boolean tuesday = true;

    @Column(name = "WEDNESDAY")
    private Boolean wednesday = true;

    @Column(name = "THURSDAY")
    private Boolean thursday = true;

    @Column(name = "FRIDAY")
    private Boolean friday = true;

    @Column(name = "SATURDAY")
    private Boolean saturday = false;

    @Column(name = "SUNDAY")
    private Boolean sunday = false;

    @JoinColumn(name = "HOLIDAY_CALENDAR_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private HolidayCalendar holidayCalendar;

    // Exactly one non-deleted schedule is the default, kept so by WorkScheduleEventListener;
    // no unique index (docs/decisions/0001)
    @Column(name = "IS_DEFAULT")
    private Boolean isDefault = false;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @DeletedBy
    @Column(name = "DELETED_BY")
    private String deletedBy;

    @DeletedDate
    @Column(name = "DELETED_DATE")
    private OffsetDateTime deletedDate;

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public HolidayCalendar getHolidayCalendar() {
        return holidayCalendar;
    }

    public void setHolidayCalendar(HolidayCalendar holidayCalendar) {
        this.holidayCalendar = holidayCalendar;
    }

    public Boolean getSunday() {
        return sunday;
    }

    public void setSunday(Boolean sunday) {
        this.sunday = sunday;
    }

    public Boolean getSaturday() {
        return saturday;
    }

    public void setSaturday(Boolean saturday) {
        this.saturday = saturday;
    }

    public Boolean getFriday() {
        return friday;
    }

    public void setFriday(Boolean friday) {
        this.friday = friday;
    }

    public Boolean getThursday() {
        return thursday;
    }

    public void setThursday(Boolean thursday) {
        this.thursday = thursday;
    }

    public Boolean getWednesday() {
        return wednesday;
    }

    public void setWednesday(Boolean wednesday) {
        this.wednesday = wednesday;
    }

    public Boolean getTuesday() {
        return tuesday;
    }

    public void setTuesday(Boolean tuesday) {
        this.tuesday = tuesday;
    }

    public Boolean getMonday() {
        return monday;
    }

    public void setMonday(Boolean monday) {
        this.monday = monday;
    }

    public WorkTime getWorkdayTime() {
        return workdayTime;
    }

    public void setWorkdayTime(WorkTime workdayTime) {
        this.workdayTime = workdayTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}

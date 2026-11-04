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

package com.glebfox.timesheets.entity.absence;

import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DeletePolicy;
import io.jmix.core.MetadataTools;
import io.jmix.core.annotation.DeletedBy;
import io.jmix.core.annotation.DeletedDate;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.validation.group.UiCrossFieldChecks;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

// Default refuses a DataManager save; UiCrossFieldChecks lets a detail view's save show the violation as a message
@AbsenceDays(groups = {Default.class, UiCrossFieldChecks.class})
@JmixEntity
@Table(name = "TS_ABSENCE", indexes = {
        @Index(name = "IDX_TS_ABSENCE_USER", columnList = "USER_ID"),
        @Index(name = "IDX_TS_ABSENCE_TYPE", columnList = "TYPE_ID")
})
@Entity(name = "ts_Absence")
public class Absence {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "USER_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @JoinColumn(name = "TYPE_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AbsenceType type;

    @Column(name = "FIRST_DAY", nullable = false)
    @NotNull
    private LocalDate firstDay;

    @Column(name = "LAST_DAY", nullable = false)
    @NotNull
    private LocalDate lastDay;

    @Column(name = "COMMENT_")
    @Lob
    private String comment;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @DeletedBy
    @Column(name = "DELETED_BY")
    private String deletedBy;

    @DeletedDate
    @Column(name = "DELETED_DATE")
    private OffsetDateTime deletedDate;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDate getLastDay() {
        return lastDay;
    }

    public void setLastDay(LocalDate lastDay) {
        this.lastDay = lastDay;
    }

    public LocalDate getFirstDay() {
        return firstDay;
    }

    public void setFirstDay(LocalDate firstDay) {
        this.firstDay = firstDay;
    }

    public AbsenceType getType() {
        return type;
    }

    public void setType(AbsenceType type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
    @DependsOnProperties({"firstDay", "lastDay", "type"})
    public String getInstanceName(MetadataTools metadataTools,
                                  DatatypeFormatter datatypeFormatter) {
        String days = Objects.equals(firstDay, lastDay)
                ? datatypeFormatter.formatLocalDate(firstDay)
                : datatypeFormatter.formatLocalDate(firstDay) + " – " + datatypeFormatter.formatLocalDate(lastDay);
        return String.format("%s %s", days, metadataTools.format(type));
    }
}

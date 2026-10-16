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

package com.glebfox.timesheets.entity.timeentry;

import com.glebfox.timesheets.datatype.SpentTime;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DeletePolicy;
import io.jmix.core.MetadataTools;
import io.jmix.core.annotation.DeletedBy;
import io.jmix.core.annotation.DeletedDate;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "TS_TIME_ENTRY", indexes = {
        @Index(name = "IDX_TS_TIME_ENTRY_TASK", columnList = "TASK_ID"),
        @Index(name = "IDX_TS_TIME_ENTRY_USER", columnList = "USER_ID")
})
@Entity(name = "ts_TimeEntry")
public class TimeEntry {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "DATE_", nullable = false)
    @NotNull
    private LocalDate date;

    @JoinColumn(name = "TASK_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Task task;

    @JoinColumn(name = "USER_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @NotNull
    @Column(name = "SPENT_TIME", nullable = false)
    private SpentTime spentTime;

    @Column(name = "STATUS", nullable = false)
    @NotNull
    private String status;

    @Column(name = "DESCRIPTION")
    @Lob
    private String description;

    @Column(name = "REJECTION_REASON")
    @Lob
    private String rejectionReason;

    @OnDelete(DeletePolicy.CASCADE)
    @JoinTable(name = "TS_TIME_ENTRY_TAG_LINK",
            joinColumns = @JoinColumn(name = "TIME_ENTRY_ID"),
            inverseJoinColumns = @JoinColumn(name = "TAG_ID"))
    @ManyToMany
    private List<Tag> tags;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @DeletedBy
    @Column(name = "DELETED_BY")
    private String deletedBy;

    @DeletedDate
    @Column(name = "DELETED_DATE")
    private OffsetDateTime deletedDate;

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void setSpentTime(SpentTime spentTime) {
        this.spentTime = spentTime;
    }

    public SpentTime getSpentTime() {
        return spentTime;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TimeEntryStatus getStatus() {
        return status == null ? null : TimeEntryStatus.fromId(status);
    }

    public void setStatus(TimeEntryStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    @PostConstruct
    public void postConstruct() {
        setStatus(TimeEntryStatus.NEW);
    }

    @InstanceName
    @DependsOnProperties({"date", "task"})
    public String getInstanceName(MetadataTools metadataTools, DatatypeFormatter datatypeFormatter) {
        return String.format("%s %s",
                datatypeFormatter.formatLocalDate(date),
                metadataTools.format(task));
    }
}
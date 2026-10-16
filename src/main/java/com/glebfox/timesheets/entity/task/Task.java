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

package com.glebfox.timesheets.entity.task;

import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.tag.TagGroup;
import io.jmix.core.MetadataTools;
import io.jmix.core.annotation.DeletedBy;
import io.jmix.core.annotation.DeletedDate;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "TS_TASK", indexes = {
        @Index(name = "IDX_TS_TASK_PROJECT", columnList = "PROJECT_ID"),
        @Index(name = "IDX_TS_TASK_TYPE", columnList = "TYPE_ID"),
        @Index(name = "IDX_TS_TASK_UNQ", columnList = "NAME, PROJECT_ID", unique = true)
})
@Entity(name = "ts_Task")
public class Task {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "NAME", nullable = false)
    @NotNull
    private String name;

    @Column(name = "DESCRIPTION")
    @Lob
    private String description;

    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Project project;

    @JoinColumn(name = "TYPE_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private TaskType type;

    @Column(name = "STATUS", nullable = false)
    @NotNull
    private String status;

    @JoinTable(name = "TS_TASK_TAG_GROUP_LINK",
            joinColumns = @JoinColumn(name = "TASK_ID"),
            inverseJoinColumns = @JoinColumn(name = "TAG_GROUP_ID"))
    @ManyToMany
    private List<TagGroup> requiredTagGroups;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @DeletedBy
    @Column(name = "DELETED_BY")
    private String deletedBy;

    @DeletedDate
    @Column(name = "DELETED_DATE")
    private OffsetDateTime deletedDate;

    public List<TagGroup> getRequiredTagGroups() {
        return requiredTagGroups;
    }

    public void setRequiredTagGroups(List<TagGroup> requiredTagGroups) {
        this.requiredTagGroups = requiredTagGroups;
    }

    public TaskStatus getStatus() {
        return status == null ? null : TaskStatus.fromId(status);
    }

    public void setStatus(TaskStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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

    @InstanceName
    @DependsOnProperties({"project", "name"})
    public String getInstanceName(MetadataTools metadataTools) {
        return String.format("[%s] %s",
                metadataTools.format(project),
                metadataTools.format(name));
    }

    @PostConstruct
    public void postConstruct() {
        setStatus(TaskStatus.ACTIVE);
    }
}
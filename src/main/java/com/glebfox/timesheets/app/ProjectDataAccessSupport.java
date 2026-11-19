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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import io.jmix.core.EntityStates;
import io.jmix.core.Metadata;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.entity.EntityValues;
import io.jmix.data.AttributeChangesProvider;
import io.jmix.data.ReferenceIdProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Judges whether the current user may create, change, or remove project data: a project, its tasks, participants,
 * tag groups, and tags. The predicates of the row-level role Project scope call it.
 * <p>
 * The user changes project data only where they are the project's Manager ({@link ProjectAccessSupport}). A task, a
 * participant, or a tag group belongs to the project it refers to, and a tag to the project of its tag group. A save
 * is judged by the project the entity belongs to as stored and, if the save moves it to another project, by that one
 * too: the user must manage both. A new entity has no stored project and is judged by the one it is created in. An
 * entity whose project is unknown is refused.
 * <p>
 * The data store asks after the merge, about the merged, managed instance, so the stored project comes from its change
 * tracking: the old value of a reference the save changes, the current value of one it does not. A screen asks about
 * an entity it has loaded, a detached instance, which reports no change, so the screen's question is judged on the
 * entity's project as loaded.
 * <p>
 * The rules never load an entity and never flush: an entity query inside a save refreshes the rows the save holds, and
 * a flush clears the change tracking the rules read. A reference is read where it is loaded, or from the row of a
 * managed instance; a detached instance loaded without it holds the stored one, which a value query reads. The project
 * of a tag group the instance does not hold comes from a value query too.
 * <p>
 * The user is always the effective user, so under user substitution it is the substituted user.
 */
@Component("ts_ProjectDataAccessSupport")
public class ProjectDataAccessSupport {

    private static final String PROJECT = "project";
    private static final String TAG_GROUP = "tagGroup";

    private final ProjectAccessSupport projectAccessSupport;
    private final AttributeChangesProvider attributeChangesProvider;
    private final ReferenceIdProvider referenceIdProvider;
    private final EntityStates entityStates;
    private final Metadata metadata;
    private final UnconstrainedDataManager unconstrainedDataManager;

    public ProjectDataAccessSupport(ProjectAccessSupport projectAccessSupport,
                                    AttributeChangesProvider attributeChangesProvider,
                                    ReferenceIdProvider referenceIdProvider,
                                    EntityStates entityStates,
                                    Metadata metadata,
                                    UnconstrainedDataManager unconstrainedDataManager) {
        this.projectAccessSupport = projectAccessSupport;
        this.attributeChangesProvider = attributeChangesProvider;
        this.referenceIdProvider = referenceIdProvider;
        this.entityStates = entityStates;
        this.metadata = metadata;
        this.unconstrainedDataManager = unconstrainedDataManager;
    }

    /**
     * Tells whether the current user may change the project: they are its Manager.
     *
     * @param project the project being saved, or the project a screen asks about
     * @return {@code true} if the current user may change the project
     */
    public boolean mayChange(Project project) {
        return projectAccessSupport.managesProject(project.getId());
    }

    /**
     * Tells whether the current user may create, change, or remove the task: they are the Manager of its project, as
     * stored and as saved.
     *
     * @param task the task being saved or removed, or the task a screen asks about
     * @return {@code true} if the current user may create, change, or remove the task
     */
    public boolean mayChange(Task task) {
        return managesProjectOf(task);
    }

    /**
     * Tells whether the current user may create, change, or remove the participant: they are the Manager of its
     * project, as stored and as saved. A participant's role is judged by the stored participants, so no one makes
     * themselves a project's Manager.
     *
     * @param participant the participant being saved or removed, or the participant a screen asks about
     * @return {@code true} if the current user may create, change, or remove the participant
     */
    public boolean mayChange(ProjectParticipant participant) {
        return managesProjectOf(participant);
    }

    /**
     * Tells whether the current user may create, change, or remove the tag group: they are the Manager of its
     * project, as stored and as saved.
     *
     * @param tagGroup the tag group being saved or removed, or the tag group a screen asks about
     * @return {@code true} if the current user may create, change, or remove the tag group
     */
    public boolean mayChange(TagGroup tagGroup) {
        return managesProjectOf(tagGroup);
    }

    /**
     * Tells whether the current user may create, change, or remove the tag: they are the Manager of the project of
     * its tag group, as stored and as saved.
     *
     * @param tag the tag being saved or removed, or the tag a screen asks about
     * @return {@code true} if the current user may create, change, or remove the tag
     */
    public boolean mayChange(Tag tag) {
        UUID currentProjectId = currentTagProjectId(tag);
        UUID storedProjectId = referenceChanged(tag, TAG_GROUP)
                ? loadTagGroupProjectId(idOf(attributeChangesProvider.getOldValue(tag, TAG_GROUP)))
                : currentProjectId;
        return managesBoth(storedProjectId, currentProjectId);
    }

    // An entity that refers to its project
    private boolean managesProjectOf(Object entity) {
        UUID currentProjectId = currentProjectId(entity);
        UUID storedProjectId = referenceChanged(entity, PROJECT)
                ? idOf(attributeChangesProvider.getOldValue(entity, PROJECT))
                : currentProjectId;
        return managesBoth(storedProjectId, currentProjectId);
    }

    private boolean managesBoth(@Nullable UUID storedProjectId, @Nullable UUID currentProjectId) {
        return storedProjectId != null
                && currentProjectId != null
                && projectAccessSupport.managesProject(storedProjectId)
                && (storedProjectId.equals(currentProjectId) || projectAccessSupport.managesProject(currentProjectId));
    }

    // A new instance has no stored state: change tracking reports each of its attributes changed, with no old value
    private boolean referenceChanged(Object entity, String attribute) {
        return !entityStates.isNew(entity) && attributeChangesProvider.isChanged(entity, attribute);
    }

    @Nullable
    private UUID currentProjectId(Object entity) {
        if (entityStates.isLoaded(entity, PROJECT)) {
            return idOf(EntityValues.getValue(entity, PROJECT));
        }
        if (entityStates.isManaged(entity)) {
            return referenceId(entity, PROJECT);
        }
        // A detached instance loaded without its project still refers to the stored one
        return entityStates.isNew(entity) ? null : loadProjectId(entity, PROJECT);
    }

    @Nullable
    private UUID currentTagProjectId(Tag tag) {
        if (entityStates.isLoaded(tag, TAG_GROUP)) {
            TagGroup tagGroup = tag.getTagGroup();
            return tagGroup == null ? null : currentProjectId(tagGroup);
        }
        if (entityStates.isManaged(tag)) {
            return loadTagGroupProjectId(referenceId(tag, TAG_GROUP));
        }
        // A detached instance loaded without its tag group still refers to the stored one
        return entityStates.isNew(tag) ? null : loadProjectId(tag, TAG_GROUP + "." + PROJECT);
    }

    // The id of a reference that is not loaded, from the row of a managed instance, without loading the referenced
    // entity
    @Nullable
    private UUID referenceId(Object entity, String attribute) {
        ReferenceIdProvider.RefId refId = referenceIdProvider.getReferenceId(entity, attribute);
        return refId.isLoaded() && refId.getValue() instanceof UUID id ? id : null;
    }

    @Nullable
    private UUID loadTagGroupProjectId(@Nullable UUID tagGroupId) {
        return tagGroupId == null
                ? null
                : loadProjectId(metadata.getClass(TagGroup.class).getName(), PROJECT, tagGroupId);
    }

    @Nullable
    private UUID loadProjectId(Object entity, String projectPath) {
        Object id = EntityValues.getId(entity);
        return id == null ? null : loadProjectId(metadata.getClass(entity).getName(), projectPath, id);
    }

    // A value query, as in ProjectAccessSupport: it neither flushes the rows the save holds nor refreshes them. A
    // deleted entity, or one of a deleted project or tag group, is not found, so it counts as one of no project
    @Nullable
    private UUID loadProjectId(String entityName, String projectPath, Object id) {
        return unconstrainedDataManager.loadValue(
                        "select e." + projectPath + ".id from " + entityName + " e where e.id = :id", UUID.class)
                .parameter("id", id)
                .optional()
                .orElse(null);
    }

    @Nullable
    private static UUID idOf(@Nullable Object reference) {
        return reference != null && EntityValues.getId(reference) instanceof UUID id ? id : null;
    }
}

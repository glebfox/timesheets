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

package com.glebfox.timesheets.security;

import io.jmix.security.role.annotation.ResourceRole;

/**
 * The job role of a person who sets up projects across the company: {@link WorkerRole},
 * {@link ClientManagementRole}, {@link ProjectCreationRole}, {@link ProjectManagementRole}, and
 * {@link ProjectCatalogsRole}.
 * <p>
 * Assigned with {@link TimeEntryRulesRole} as the row-level role, whatever other job roles the person holds besides
 * Full access, so the holder keeps every project and writes only their own time entries.
 */
@ResourceRole(name = "Project office", code = ProjectOfficeRole.CODE)
public interface ProjectOfficeRole extends WorkerRole, ClientManagementRole, ProjectCreationRole,
        ProjectManagementRole, ProjectCatalogsRole {

    String CODE = "project-office";
}

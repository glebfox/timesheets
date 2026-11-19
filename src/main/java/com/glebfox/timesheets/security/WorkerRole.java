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
 * The job role of a person who logs their own time: signing in, {@link TimeLoggingRole}, and their own filter
 * configurations.
 * <p>
 * Assigned with {@link ProjectScopeRole} as the row-level role, unless the person also holds Project office or Full
 * access. Without a row-level role a holder reads and writes anyone's time entries.
 */
@ResourceRole(name = "Worker", code = WorkerRole.CODE)
public interface WorkerRole extends UiMinimalRole, TimeLoggingRole, OwnFiltersRole {

    String CODE = "worker";
}

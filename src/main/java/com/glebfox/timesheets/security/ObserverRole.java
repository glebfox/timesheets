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
 * The job role of a person who follows projects without working on them, such as a project's client: signing in,
 * {@link ProjectReadOnlyRole}, {@link TimeEntryReadOnlyRole}, and their own filter configurations. It logs no time.
 * <p>
 * Assigned with {@link ClientScopeRole} as the row-level role when it is the person's only job role, and with the
 * row-level role of their other job roles otherwise.
 */
@ResourceRole(name = "Observer", code = ObserverRole.CODE)
public interface ObserverRole extends UiMinimalRole, ProjectReadOnlyRole, TimeEntryReadOnlyRole, OwnFiltersRole {

    String CODE = "observer";
}

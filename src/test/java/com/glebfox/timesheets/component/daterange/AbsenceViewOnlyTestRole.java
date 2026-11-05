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

package com.glebfox.timesheets.component.daterange;

import com.glebfox.timesheets.entity.absence.Absence;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Reading absences with every attribute view-only, and opening {@link DateRangeFieldTestView}: a user who sees an
 * absence's days but may not change them, for {@link DateRangeFieldBindingUiTest}.
 */
@ResourceRole(name = "Test: absence view only", code = AbsenceViewOnlyTestRole.CODE)
public interface AbsenceViewOnlyTestRole {

    String CODE = "test-absence-view-only";

    @EntityAttributePolicy(entityClass = Absence.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Absence.class, actions = EntityPolicyAction.READ)
    void absence();

    // Tabbed mode opens only the views the user is permitted
    @ViewPolicy(viewIds = "ts_DateRangeFieldTestView")
    void testView();
}

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

import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Managing absences and absence types: who is absent on which days, and the catalog of types they are recorded as.
 * <p>
 * Users are only read, for the user drop-downs of the absence editor and of the absence list's filter, and their
 * pickers.
 */
@ResourceRole(name = "Absence management", code = AbsenceManagementRole.CODE)
public interface AbsenceManagementRole extends UserReadOnlyRole {

    String CODE = "absence-management";

    @EntityAttributePolicy(entityClass = Absence.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Absence.class, actions = EntityPolicyAction.ALL)
    void absence();

    @EntityAttributePolicy(entityClass = AbsenceType.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = AbsenceType.class, actions = EntityPolicyAction.ALL)
    void absenceType();

    @ViewPolicy(viewIds = {"ts_Absence.list", "ts_Absence.detail"})
    @MenuPolicy(menuIds = "ts_Absence.list")
    void absenceScreens();

    @ViewPolicy(viewIds = {"ts_AbsenceType.list", "ts_AbsenceType.detail"})
    @MenuPolicy(menuIds = "ts_AbsenceType.list")
    void absenceTypeScreens();
}

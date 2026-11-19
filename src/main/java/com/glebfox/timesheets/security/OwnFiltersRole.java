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

import io.jmix.flowui.accesscontext.UiGenericFilterModifyConfigurationContext;
import io.jmix.flowui.entity.filter.FilterCondition;
import io.jmix.flowui.entity.filter.GroupFilterCondition;
import io.jmix.flowui.entity.filter.PropertyFilterCondition;
import io.jmix.flowuidata.entity.FilterConfiguration;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.model.SecurityScope;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.security.role.annotation.SpecificPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * A list filter's conditions and the holder's own saved configurations: the framework's "UI: edit filters"
 * ({@link io.jmix.securityflowui.role.UiFilterRole}) without configurations for everybody and without JPQL conditions,
 * which only Full access grants.
 * <p>
 * The filter shows a holder their own configurations and those for everybody, and changes or removes only their own;
 * the data layer does not narrow configurations to the holder's own.
 */
@ResourceRole(name = "Filters: own configurations", code = OwnFiltersRole.CODE, scope = SecurityScope.UI)
public interface OwnFiltersRole {

    String CODE = "own-filters";

    @ViewPolicy(viewIds = {"flowui_AddConditionView", "flowui_DateIntervalDialog"})
    @EntityPolicy(entityClass = FilterConfiguration.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = FilterConfiguration.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @SpecificPolicy(resources = UiGenericFilterModifyConfigurationContext.NAME)
    void configuration();

    @ViewPolicy(viewIds = "flowui_GroupFilterCondition.detail")
    @EntityPolicy(entityClass = GroupFilterCondition.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = GroupFilterCondition.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = FilterCondition.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = FilterCondition.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    void groupFilterCondition();

    @ViewPolicy(viewIds = "flowui_PropertyFilterCondition.detail")
    @EntityPolicy(entityClass = PropertyFilterCondition.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = PropertyFilterCondition.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    void propertyFilterCondition();
}

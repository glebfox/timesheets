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

import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.accesscontext.SpecificOperationAccessContext;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.accesscontext.UiGenericFilterModifyConfigurationContext;
import io.jmix.flowui.accesscontext.UiGenericFilterModifyGlobalConfigurationContext;
import io.jmix.flowui.accesscontext.UiGenericFilterModifyJpqlConditionContext;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.entity.filter.FilterCondition;
import io.jmix.flowui.entity.filter.GroupFilterCondition;
import io.jmix.flowui.entity.filter.JpqlFilterCondition;
import io.jmix.flowui.entity.filter.PropertyFilterCondition;
import io.jmix.flowuidata.entity.FilterConfiguration;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The holder signs in with UI: minimal access and holds the role besides. The generic filter, its actions, and the
 * add condition dialog ask the same specific contexts as this test.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class OwnFiltersRoleTest {

    static final String USERNAME_PREFIX = "own-filters-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    AccessManager accessManager;
    @Autowired
    Metadata metadata;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    String holder;
    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User holderUser = dataManager.create(User.class);
        holderUser.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        holderUser = dataManager.save(holderUser);
        createdIds.add(Id.of(holderUser));
        assignRole(holderUser, UiMinimalRole.CODE);
        assignRole(holderUser, OwnFiltersRole.CODE);
        holder = holderUser.getUsername();
    }

    @Test
    void holderConfiguresOwnFiltersOnly() {
        systemAuthenticator.runWithUser(holder, () -> {
            // Add condition and the settings button, but no configuration for everybody and no JPQL condition
            assertThat(specificPermitted(new UiGenericFilterModifyConfigurationContext())).isTrue();
            assertThat(specificPermitted(new UiGenericFilterModifyGlobalConfigurationContext())).isFalse();
            assertThat(specificPermitted(new UiGenericFilterModifyJpqlConditionContext())).isFalse();

            for (String viewId : List.of("flowui_AddConditionView", "flowui_DateIntervalDialog",
                    "flowui_GroupFilterCondition.detail", "flowui_PropertyFilterCondition.detail")) {
                assertThat(viewPermitted(viewId)).as("view %s", viewId).isTrue();
            }
            assertThat(viewPermitted("flowui_JpqlFilterCondition.detail"))
                    .as("view flowui_JpqlFilterCondition.detail")
                    .isFalse();

            for (Class<?> entityClass : List.of(FilterConfiguration.class, GroupFilterCondition.class,
                    FilterCondition.class, PropertyFilterCondition.class)) {
                UiEntityContext context = entityContext(entityClass);
                assertThat(context.isViewPermitted()).as("read %s", entityClass.getSimpleName()).isTrue();
                assertThat(context.isCreatePermitted()).as("create %s", entityClass.getSimpleName()).isTrue();
                assertThat(context.isEditPermitted()).as("update %s", entityClass.getSimpleName()).isTrue();
                assertThat(context.isDeletePermitted()).as("delete %s", entityClass.getSimpleName()).isTrue();

                MetaClass metaClass = metadata.getClass(entityClass);
                for (MetaProperty property : metaClass.getProperties()) {
                    UiEntityAttributeContext attributeContext =
                            new UiEntityAttributeContext(metaClass, property.getName());
                    accessManager.applyRegisteredConstraints(attributeContext);
                    assertThat(attributeContext.canModify())
                            .as("modify %s.%s", entityClass.getSimpleName(), property.getName())
                            .isTrue();
                }
            }

            UiEntityContext jpqlCondition = entityContext(JpqlFilterCondition.class);
            assertThat(jpqlCondition.isViewPermitted()).as("read JpqlFilterCondition").isFalse();
            assertThat(jpqlCondition.isCreatePermitted()).as("create JpqlFilterCondition").isFalse();
            assertThat(jpqlCondition.isEditPermitted()).as("update JpqlFilterCondition").isFalse();
            assertThat(jpqlCondition.isDeletePermitted()).as("delete JpqlFilterCondition").isFalse();
        });
    }

    @AfterEach
    void tearDown() {
        // Role assignments and users are soft-deleted, so they are removed for good, reloaded by id
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    private boolean specificPermitted(SpecificOperationAccessContext context) {
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private UiEntityContext entityContext(Class<?> entityClass) {
        UiEntityContext context = new UiEntityContext(metadata.getClass(entityClass));
        accessManager.applyRegisteredConstraints(context);
        return context;
    }

    private boolean viewPermitted(String viewId) {
        UiShowViewContext context = new UiShowViewContext(viewId);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }
}

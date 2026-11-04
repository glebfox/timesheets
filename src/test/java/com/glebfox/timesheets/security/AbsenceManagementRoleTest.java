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
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.accesscontext.UiMenuContext;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.menu.MenuConfig;
import io.jmix.flowui.menu.MenuItem;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.jspecify.annotations.Nullable;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class AbsenceManagementRoleTest {

    static final String USERNAME_PREFIX = "absence-management-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    AccessManager accessManager;
    @Autowired
    Metadata metadata;
    @Autowired
    MenuConfig menuConfig;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    String holder;
    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User user = saveUser();
        assignRole(user, UiMinimalRole.CODE);
        assignRole(user, AbsenceManagementRole.CODE);
        holder = user.getUsername();
    }

    @Test
    void holderManagesAbsencesAndTypes() {
        systemAuthenticator.runWithUser(holder, () -> {
            for (Class<?> entityClass : List.of(Absence.class, AbsenceType.class)) {
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
        });
    }

    @Test
    void holderReadsUsersButMayNotUpdateThem() {
        User other = saveUser();

        systemAuthenticator.runWithUser(holder, () -> {
            UiEntityContext userContext = entityContext(User.class);
            assertThat(userContext.isViewPermitted()).isTrue();
            assertThat(userContext.isEditPermitted()).isFalse();
            assertThat(userContext.isCreatePermitted()).isFalse();
            assertThat(userContext.isDeletePermitted()).isFalse();

            MetaClass userMetaClass = metadata.getClass(User.class);
            for (MetaProperty property : userMetaClass.getProperties()) {
                UiEntityAttributeContext attributeContext =
                        new UiEntityAttributeContext(userMetaClass, property.getName());
                accessManager.applyRegisteredConstraints(attributeContext);
                assertThat(attributeContext.canView()).as("view %s", property.getName()).isTrue();
                assertThat(attributeContext.canModify()).as("modify %s", property.getName()).isFalse();
            }

            // The data layer refuses the change, whatever the screens allow
            User loaded = dataManager.load(Id.of(other)).one();
            loaded.setFirstName("Changed");
            assertThatThrownBy(() -> dataManager.save(loaded)).isInstanceOf(AccessDeniedException.class);
        });

        User reloaded = dataManager.load(Id.of(other)).one();
        assertThat(reloaded.getFirstName()).isNull();
        assertThat(reloaded.getVersion()).isEqualTo(other.getVersion());
    }

    @Test
    void holderSeesItsScreensAndMenuItems() {
        systemAuthenticator.runWithUser(holder, () -> {
            // The user list is User: read only's, for the absence editor's user picker
            for (String viewId : List.of("ts_Absence.list", "ts_Absence.detail",
                    "ts_AbsenceType.list", "ts_AbsenceType.detail", "ts_User.list")) {
                assertThat(viewPermitted(viewId)).as("view %s", viewId).isTrue();
            }
            for (String menuItemId : List.of("ts_Absence.list", "ts_AbsenceType.list")) {
                assertThat(menuItemPermitted(menuItemId)).as("menu item %s", menuItemId).isTrue();
            }

            // The role grants the absence screens alone, not the rest of Settings
            for (String viewId : List.of("ts_WorkSchedule.list", "ts_WorkSchedule.detail")) {
                assertThat(viewPermitted(viewId)).as("view %s", viewId).isFalse();
            }
            assertThat(menuItemPermitted("ts_WorkSchedule.list")).as("menu item ts_WorkSchedule.list").isFalse();
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

    private boolean menuItemPermitted(String menuItemId) {
        MenuItem menuItem = findMenuItem(menuConfig.getRootItems(), menuItemId);
        assertThat(menuItem).as("menu item %s", menuItemId).isNotNull();
        UiMenuContext context = new UiMenuContext(menuItem);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    @Nullable
    private static MenuItem findMenuItem(List<MenuItem> items, String id) {
        for (MenuItem item : items) {
            if (id.equals(item.getId())) {
                return item;
            }
            MenuItem child = findMenuItem(item.getChildren(), id);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        User saved = dataManager.save(user);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }
}

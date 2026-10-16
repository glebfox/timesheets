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

package com.glebfox.timesheets.entity.project;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.jspecify.annotations.Nullable;


public enum ProjectRoleType implements EnumClass<String> {

    MANAGER("manager"),
    APPROVER("approver"),
    MEMBER("member"),
    OBSERVER("observer");

    private final String id;

    ProjectRoleType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static ProjectRoleType fromId(String id) {
        for (ProjectRoleType at : ProjectRoleType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}
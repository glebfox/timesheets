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

import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

/**
 * The row-level role of a project's client, who holds the Observer job role alone: {@link ProjectScopeRole}, and
 * users and clients narrowed to those of the projects where the person takes part, which the resource roles grant
 * entity-wide and Project scope leaves open.
 * <p>
 * The policies compare only the root's own id, so no fetched reference of a user or a client turns into an inner join;
 * their subqueries use aliases of their own, never those of the screens' queries.
 */
@RowLevelRole(name = "Client scope", code = ClientScopeRole.CODE)
public interface ClientScopeRole extends ProjectScopeRole {

    String CODE = "client-scope";

    @JpqlRowLevelPolicy(entityClass = User.class,
            where = "{E}.id = :current_user_id"
                    + " or exists (select theirs from ts_ProjectParticipant theirs, ts_ProjectParticipant mine"
                    + " where theirs.user.id = {E}.id and mine.project.id = theirs.project.id"
                    + " and mine.user.id = :current_user_id)")
    void userRead();

    @JpqlRowLevelPolicy(entityClass = Client.class,
            where = "exists (select mine from ts_ProjectParticipant mine"
                    + " where mine.project.client.id = {E}.id and mine.user.id = :current_user_id)")
    void clientRead();
}

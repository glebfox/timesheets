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

package com.glebfox.timesheets.action;

import io.jmix.flowui.action.ActionType;

/**
 * The copy action of a list of time entries that opens the copy as a general entry, not an own one, offered only to a
 * user who may set an entry's user.
 * <p>
 * Like {@link GeneralTimeEntryCreateAction}, it logs time for any person, which only someone who may set an entry's
 * user does; everyone else copies their own entries as own ones.
 */
@ActionType(GeneralTimeEntryCopyAction.ID)
public class GeneralTimeEntryCopyAction extends TimeEntryCopyAction {

    public static final String ID = "list_generalTimeEntry_copy";

    public GeneralTimeEntryCopyAction() {
        this(ID);
    }

    public GeneralTimeEntryCopyAction(String id) {
        super(id);
    }

    @Override
    protected boolean isPermitted() {
        return super.isPermitted() && ListActionTargets.isAttributeModifiable(target, accessManager, "user");
    }
}

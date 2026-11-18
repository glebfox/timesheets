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

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.CreateAction;

/**
 * The standard create action of a list of time entries that opens a general entry, one whose user is chosen in the
 * editor, offered only to a user who may set an entry's user.
 * <p>
 * It logs time for any person, which only someone who may set an entry's user does; everyone else logs their own time
 * on the screens that open an own entry.
 */
@ActionType(GeneralTimeEntryCreateAction.ID)
public class GeneralTimeEntryCreateAction extends CreateAction<TimeEntry> {

    public static final String ID = "list_generalTimeEntry_create";

    public GeneralTimeEntryCreateAction() {
        this(ID);
    }

    public GeneralTimeEntryCreateAction(String id) {
        super(id);
    }

    @Override
    protected boolean isPermitted() {
        return super.isPermitted() && ListActionTargets.isAttributeModifiable(target, accessManager, "user");
    }
}

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
import io.jmix.flowui.action.list.EditAction;

/**
 * The standard edit action of an editor's table of the edited entity's children, which a read-only editor leaves
 * enabled, so a user who may not change the children still opens each of them read-only.
 * <p>
 * The standard action disables itself in a read-only editor whenever the user may update the children's entity at
 * entity level, even where a row-level role refuses the selected child; it then never opens the child at all. This
 * one keeps its permission check and its Read label for a child the user may not update, and the child's editor
 * opens read-only by the same permissions. It suits an editor whose children a user may change exactly where they
 * may change the edited entity, as Project scope rules for a project and its tasks, participants, and tag groups, and
 * for a tag group and its tags: a read-only editor then opens no child editable.
 */
@ActionType(ChildEditAction.ID)
public class ChildEditAction<E> extends EditAction<E> {

    public static final String ID = "list_childEdit";

    public ChildEditAction() {
        this(ID);
    }

    public ChildEditAction(String id) {
        super(id);
    }

    @Override
    public boolean isDisabledWhenViewReadOnly() {
        return false;
    }
}

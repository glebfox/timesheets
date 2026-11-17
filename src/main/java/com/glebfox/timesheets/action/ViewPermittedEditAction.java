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

import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.EditAction;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewInfo;
import io.jmix.flowui.view.ViewRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The standard edit action of a list, offered only to a user who may also open the editor it opens.
 * <p>
 * The standard action checks the entity's permissions and its row-level constraints, never the view it opens, so in a
 * list that a role grants without its editor, such as a picker, it stays enabled as Read and fails when clicked. The
 * editor is the action's view, given by id or class, or else the entity's detail view.
 */
@ActionType(ViewPermittedEditAction.ID)
public class ViewPermittedEditAction<E> extends EditAction<E> {

    public static final String ID = "list_viewPermittedEdit";

    private ViewRegistry viewRegistry;

    public ViewPermittedEditAction() {
        this(ID);
    }

    public ViewPermittedEditAction(String id) {
        super(id);
    }

    @Autowired
    public void setViewRegistry(ViewRegistry viewRegistry) {
        this.viewRegistry = viewRegistry;
    }

    @Override
    protected boolean isPermitted() {
        return super.isPermitted() && isEditorPermitted();
    }

    private boolean isEditorPermitted() {
        if (target == null || !(target.getItems() instanceof EntityDataUnit entityDataUnit)) {
            return false;
        }

        UiShowViewContext context = new UiShowViewContext(getEditorId(entityDataUnit.getEntityMetaClass()));
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private String getEditorId(MetaClass metaClass) {
        String viewId = getViewId();
        if (viewId != null) {
            return viewId;
        }

        Class<? extends View> viewClass = getViewClass();
        if (viewClass != null) {
            return viewRegistry.getViewInfos().stream()
                    .filter(viewInfo -> viewInfo.getControllerClass().equals(viewClass))
                    .map(ViewInfo::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No view is registered for " + viewClass));
        }

        return viewRegistry.getDetailViewInfo(metaClass).getId();
    }
}

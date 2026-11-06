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

package com.glebfox.timesheets.component.emptystate;

import io.jmix.flowui.xml.layout.loader.AbstractComponentLoader;

/**
 * Loads the {@code gridEmptyState} element. It does not look for the grid's refresh action: a grid loads its empty
 * state before its actions, so the component finds the action itself when it is attached.
 */
public class GridEmptyStateLoader extends AbstractComponentLoader<GridEmptyState> {

    @Override
    protected GridEmptyState createComponent() {
        return factory.create(GridEmptyState.class);
    }

    @Override
    public void loadComponent() {
        componentLoader().loadClassNames(resultComponent, element);

        loadString(element, "icon", resultComponent::setIcon);
        loadResourceString(element, "text", context.getMessageGroup(), resultComponent::setText);
    }
}

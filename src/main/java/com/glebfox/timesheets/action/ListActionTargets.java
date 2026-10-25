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

import com.vaadin.flow.component.Component;
import io.jmix.flowui.component.ListDataComponent;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.data.ContainerDataUnit;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.ViewControllerUtils;

/**
 * Finds what a custom list action works on through its target: the container the list component shows
 * and the data context of the view that holds the component.
 */
final class ListActionTargets {

    private ListActionTargets() {
    }

    /**
     * Returns the collection container that a list component shows.
     *
     * @param target      the action's list component
     * @param actionClass the action, named in the exception
     * @return the container of the component's items
     * @throws IllegalStateException if the component's items are not bound to a container
     */
    @SuppressWarnings("unchecked")
    static <E> CollectionContainer<E> getContainer(ListDataComponent<E> target, Class<?> actionClass) {
        if (!(target.getItems() instanceof ContainerDataUnit<?> items)) {
            throw new IllegalStateException(String.format("%s target is not bound to a data container",
                    actionClass.getSimpleName()));
        }
        return (CollectionContainer<E>) items.getContainer();
    }

    /**
     * Returns the data context of the view that holds a list component.
     *
     * @param target the action's list component
     * @return the view's data context
     * @throws IllegalStateException if the component is not attached to a view, or the view has no data context
     */
    static DataContext getDataContext(ListDataComponent<?> target) {
        return ViewControllerUtils.getViewData(UiComponentUtils.getView((Component) target)).getDataContext();
    }
}

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
import io.jmix.core.AccessManager;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.component.ListDataComponent;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.data.ContainerDataUnit;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.DataLoader;
import io.jmix.flowui.model.HasLoader;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Finds what a custom list action works on through its target: the container the list component shows and its
 * loader, the view that holds the component, its data context and the entities removed there, and whether
 * the current user may perform the action's operation on the entity the component shows.
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
     * Returns the property container that a list component shows, such as the days of a calendar.
     *
     * @param target      the action's list component
     * @param actionClass the action, named in the exception
     * @return the container of the component's items
     * @throws IllegalStateException if the component's items are not bound to a property container
     */
    @SuppressWarnings("unchecked")
    static <E> CollectionPropertyContainer<E> getPropertyContainer(ListDataComponent<E> target,
                                                                   Class<?> actionClass) {
        if (!(getContainer(target, actionClass) instanceof CollectionPropertyContainer<?> container)) {
            throw new IllegalStateException(String.format("%s target is not bound to a property container",
                    actionClass.getSimpleName()));
        }
        return (CollectionPropertyContainer<E>) container;
    }

    /**
     * Returns the loader of the container that a list component shows.
     *
     * @param target      the action's list component
     * @param actionClass the action, named in the exception
     * @return the loader of the component's container
     * @throws IllegalStateException if the component's items are not bound to a container with a loader
     */
    static DataLoader getLoader(ListDataComponent<?> target, Class<?> actionClass) {
        DataLoader loader = getContainer(target, actionClass) instanceof HasLoader hasLoader
                ? hasLoader.getLoader()
                : null;
        if (loader == null) {
            throw new IllegalStateException(String.format("%s target is not bound to a container with a loader",
                    actionClass.getSimpleName()));
        }
        return loader;
    }

    /**
     * Returns the view that holds a list component.
     *
     * @param target the action's list component
     * @return the view
     * @throws IllegalStateException if the component is not attached to a view
     */
    static View<?> getView(ListDataComponent<?> target) {
        return UiComponentUtils.getView((Component) target);
    }

    /**
     * Returns the data context of the view that holds a list component.
     *
     * @param target the action's list component
     * @return the view's data context
     * @throws IllegalStateException if the component is not attached to a view, or the view has no data context
     */
    static DataContext getDataContext(ListDataComponent<?> target) {
        return ViewControllerUtils.getViewData(getView(target)).getDataContext();
    }

    /**
     * Returns the entities of a class removed in the data context of the view that holds a list component:
     * gone from the view's lists, though not deleted until the view saves its changes.
     *
     * @param target      the action's list component
     * @param entityClass the class of the entities to return
     * @return the removed entities of that class, possibly none
     * @throws IllegalStateException if the component is not attached to a view, or the view has no data context
     */
    static <E> List<E> getRemoved(ListDataComponent<?> target, Class<E> entityClass) {
        return getDataContext(target).getRemoved().stream()
                .filter(entityClass::isInstance)
                .map(entityClass::cast)
                .toList();
    }

    /**
     * Checks an entity-level permission on the entity that a list component shows, as the standard list actions
     * check it before the row-level constraints.
     *
     * @param target        the action's list component
     * @param accessManager applies the registered constraints
     * @param check         the operation to check, such as {@code UiEntityContext::isCreatePermitted}
     * @return {@code false} without a target or when its items are not entities, otherwise the check's result
     */
    static boolean isEntityPermitted(@Nullable ListDataComponent<?> target,
                                     AccessManager accessManager,
                                     Predicate<UiEntityContext> check) {
        if (target == null || !(target.getItems() instanceof EntityDataUnit entityDataUnit)) {
            return false;
        }

        UiEntityContext entityContext = new UiEntityContext(entityDataUnit.getEntityMetaClass());
        accessManager.applyRegisteredConstraints(entityContext);
        return check.test(entityContext);
    }
}

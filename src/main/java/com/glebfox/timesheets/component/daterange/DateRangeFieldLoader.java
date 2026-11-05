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

package com.glebfox.timesheets.component.daterange;

import io.jmix.flowui.exception.GuiDevelopmentException;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.xml.layout.loader.AbstractComponentLoader;

public class DateRangeFieldLoader extends AbstractComponentLoader<DateRangeField> {

    @Override
    protected DateRangeField createComponent() {
        return factory.create(DateRangeField.class);
    }

    @Override
    public void loadComponent() {
        // First, as Jmix's loaders bind a field, so that the attributes below override what the binding sets
        loadValueSource();

        componentLoader().loadLabel(resultComponent, element);
        componentLoader().loadRequired(resultComponent, element, context);
        componentLoader().loadEnabled(resultComponent, element);
        componentLoader().loadClassNames(resultComponent, element);
        componentLoader().loadSizeAttributes(resultComponent, element);
        componentLoader().loadValueAndElementAttributes(resultComponent, element);

        loadResourceString(element, "startPlaceholder", context.getMessageGroup(), resultComponent::setStartPlaceholder);
        loadResourceString(element, "endPlaceholder", context.getMessageGroup(), resultComponent::setEndPlaceholder);
        loadResourceString(element, "startAriaLabel", context.getMessageGroup(), resultComponent::setStartAriaLabel);
        loadResourceString(element, "endAriaLabel", context.getMessageGroup(), resultComponent::setEndAriaLabel);
        loadBoolean(element, "clearButtonVisible", resultComponent::setClearButtonVisible);
        loadBoolean(element, "summaryVisible", resultComponent::setSummaryVisible);
    }

    // Not through DataLoaderSupport, which binds one property and refuses a container without it
    private void loadValueSource() {
        String containerId = loadString(element, "dataContainer").orElse(null);
        String startProperty = loadString(element, "startProperty").orElse(null);
        String endProperty = loadString(element, "endProperty").orElse(null);
        if (containerId == null && startProperty == null && endProperty == null) {
            return;
        }
        if (containerId == null || startProperty == null || endProperty == null) {
            throw new GuiDevelopmentException(String.format("Can't bind component '%s': 'dataContainer', "
                    + "'startProperty', and 'endProperty' go together", element.attributeValue("id")), context);
        }

        InstanceContainer<?> container = context.getDataHolder().getContainer(containerId);
        resultComponent.setValueSource(new DateRangeValueSource<>(container, startProperty, endProperty));
    }
}

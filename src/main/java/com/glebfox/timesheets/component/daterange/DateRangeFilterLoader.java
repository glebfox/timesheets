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

import com.vaadin.flow.component.Component;
import io.jmix.flowui.exception.GuiDevelopmentException;
import io.jmix.flowui.xml.layout.loader.component.AbstractSingleFilterComponentLoader;
import org.dom4j.Element;

import java.util.List;

import static io.jmix.core.querycondition.PropertyConditionUtils.generateParameterName;

public class DateRangeFilterLoader extends AbstractSingleFilterComponentLoader<DateRangeFilter> {

    private static final String VALUE_COMPONENT_ELEMENT = "dateRangeField";

    @Override
    protected DateRangeFilter createComponent() {
        return factory.create(DateRangeFilter.class);
    }

    @Override
    protected void loadAttributesBeforeValueComponent() {
        super.loadAttributesBeforeValueComponent();

        String startProperty = loadRequiredProperty("startProperty");
        resultComponent.setStartProperty(startProperty);
        resultComponent.setEndProperty(loadRequiredProperty("endProperty"));

        // Unique among the loader's parameters, as Jmix's property filter names its own
        resultComponent.setParameterName(generateParameterName(startProperty));
    }

    private String loadRequiredProperty(String attribute) {
        return loadString(element, attribute)
                .orElseThrow(() -> new GuiDevelopmentException(String.format("'%s' is required for %s '%s'",
                        attribute, element.getName(), element.attributeValue("id")), context));
    }

    // The nested dateRangeField element, loaded by its own loader with all of its attributes
    @Override
    protected boolean isValueComponent(Element element) {
        return VALUE_COMPONENT_ELEMENT.equals(element.getName());
    }

    @Override
    protected Element getValueComponentElement(List<Element> elements) {
        List<Element> valueComponentElements = elements.stream()
                .filter(this::isValueComponent)
                .toList();
        if (valueComponentElements.size() > 1) {
            throw new GuiDevelopmentException("Only one value component can be defined", context);
        }
        return valueComponentElements.getFirst();
    }

    // Without a nested element
    @Override
    protected Component generateValueComponent() {
        return factory.create(DateRangeField.class);
    }
}

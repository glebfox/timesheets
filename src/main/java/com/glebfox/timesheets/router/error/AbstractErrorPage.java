/*
 * Copyright 2024 Gleb Gorelov
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

package com.glebfox.timesheets.router.error;

import com.vaadin.flow.component.Component;
import io.jmix.core.Resources;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * An abstract base class for rendering error pages within an application.
 * This class provides templating functionality and a mechanism for defining
 * fallback error messages when specific templates are unavailable. It is designed
 * to be extended by concrete classes that implement domain-specific error page behavior.
 */
public abstract class AbstractErrorPage extends Component {

    private final Resources resources;

    public AbstractErrorPage(Resources resources) {
        this.resources = resources;
    }

    protected String readTemplate(String templatePath) {
        String template = null;
        try {
            template = resources.getResourceAsString(templatePath);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass())
                    .error("Unable to read '{}'", templatePath, e);
        }

        // Use a very simple error page if the real one could not be found or empty
        return Objects.requireNonNullElse(template, getFallbackErrorMessage());
    }

    protected abstract String getFallbackErrorMessage();
}

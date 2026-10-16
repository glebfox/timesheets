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

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.jmix.core.Messages;
import io.jmix.core.Resources;

/**
 * Represents a custom error page that handles route-not-found errors during navigation.
 * This page is displayed to the user when they attempt to access a non-existing route.
 * <p>
 * The error page uses an HTML template specified by the `TEMPLATE_PATH` constant to
 * render the content. The template is populated with localized messages using the
 * {@link Messages} API. It includes a subtitle, details regarding the error,
 * and a return link.
 */
@Tag(Tag.DIV)
@AnonymousAllowed
public class TsRouteNotFoundErrorPage extends AbstractErrorPage
        implements HasErrorParameter<NotFoundException> {

    public static final String TEMPLATE_PATH = "META-INF/resources/errorpages/route-not-found.html";

    private final Messages messages;

    public TsRouteNotFoundErrorPage(Messages messages, Resources resources) {
        super(resources);

        this.messages = messages;
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        addClassNames("error-page", "route-not-found");

        String template = readTemplate(TEMPLATE_PATH)
                .replace("{{subtitle}}", messages.getMessage("errorPage.RouteNotFound.subtitle"))
                .replace("{{message}}", messages.getMessage("errorPage.RouteNotFound.message"))
                .replace("{{returnLink}}", messages.getMessage("errorPage.RouteNotFound.returnLink"));

        getElement().setChild(0, new Html(template).getElement());

        return HttpStatusCode.NOT_FOUND.getCode();
    }

    @Override
    protected String getFallbackErrorMessage() {
        return "Nothing was found. The page you are looking for might have been removed, " +
                "had its name changed or is temporarily unavailable";
    }
}

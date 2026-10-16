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
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.jmix.core.Messages;
import io.jmix.core.Resources;

/**
 * This class represents a custom error page for handling access denied errors
 * during navigation. It displays an appropriate message to the user when they
 * attempt to access a route for which they don't have the required permissions.
 * <p>
 * The error page uses an HTML template specified by the `TEMPLATE_PATH`
 * constant. The template is populated with localized content using
 * the {@link Messages} API, providing a subtitle, details about the
 * issue, and a return link.
 */
@Tag(Tag.DIV)
@AnonymousAllowed
public class TsRouteAccessDeniedErrorPage extends AbstractErrorPage
        implements HasErrorParameter<AccessDeniedException> {

    public static final String TEMPLATE_PATH = "META-INF/resources/errorpages/route-access-denied.html";

    private final Messages messages;

    public TsRouteAccessDeniedErrorPage(Messages messages, Resources resources) {
        super(resources);

        this.messages = messages;
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        addClassNames("error-page", "route-access-denied");

        String template = readTemplate(TEMPLATE_PATH)
                .replace("{{subtitle}}", messages.getMessage("errorPage.RouteAccessDenied.subtitle"))
                .replace("{{message}}", messages.getMessage("errorPage.RouteAccessDenied.message"))
                .replace("{{returnLink}}", messages.getMessage("errorPage.RouteAccessDenied.returnLink"));

        getElement().setChild(0, new Html(template).getElement());

        return HttpStatusCode.FORBIDDEN.getCode();
    }

    @Override
    protected String getFallbackErrorMessage() {
        return "Access denied. Looks like you don't have permission to access this page";
    }
}

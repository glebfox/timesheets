/*
 * Copyright 2023 Gleb Gorelov
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

package com.glebfox.timesheets.view.client;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.misc.ContactInformation;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "clients", layout = MainView.class)
@ViewController("ts_Client.list")
@ViewDescriptor("client-list-view.xml")
@LookupComponent("clientsDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class ClientListView extends StandardListView<Client> {

    @Autowired
    private ComponentSupport componentSupport;

    @Supply(to = "clientsDataGrid.name", subject = "renderer")
    private Renderer<Client> clientsDataGridNameRenderer() {
        return componentSupport.createClientRenderer();
    }

    @Supply(to = "clientsDataGrid.url", subject = "renderer")
    private Renderer<Client> clientsDataGridUrlRenderer() {
        return new ComponentRenderer<>(client -> {
            ContactInformation contactInformation = client.getContactInformation();
            String url = contactInformation == null ? null : contactInformation.getUrl();
            return StringUtils.isBlank(url) ? null : new Anchor(url, url, AnchorTarget.BLANK);
        });
    }
}

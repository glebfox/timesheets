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
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.view.main.MainView;
import com.google.common.base.Strings;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import io.jmix.core.AccessManager;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.accesscontext.InMemoryCrudEntityContext;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Route(value = "clients", layout = MainView.class)
@ViewController("ts_Client.list")
@ViewDescriptor("client-list-view.xml")
@DialogMode(width = "1024px", closeOnEsc = true)
public class ClientListView extends StandardView {

    @ViewComponent
    private JmixButton createBtn;
    @ViewComponent
    private VerticalLayout noDataContainer;

    @ViewComponent
    private CollectionContainer<Client> clientsDc;

    @Autowired
    private Messages messages;
    @Autowired
    private MessageTools messageTools;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private RemoveOperation removeOperation;
    @Autowired
    private ComponentSupport componentSupport;

    private UiEntityContext entityContext;
    private InMemoryCrudEntityContext crudEntityContext;

    @Subscribe
    public void onInit(final InitEvent event) {
        createBtn.setEnabled(getEntityContext().isCreatePermitted());
    }

    @Supply(to = "clientsList", subject = "renderer")
    private Renderer<Client> clientsListRenderer() {
        return new ComponentRenderer<>(client -> {
            HorizontalLayout layout = uiComponents.create(HorizontalLayout.class);
            layout.setPadding(true);
            layout.addClassNames("client-card", "aura-surface");

            layout.add(
                    componentSupport.createAvatar(client.getName(), client.getImage(), "client-logo"),
                    createInfoLayout(client),
                    createActionsLayout(client)
            );

            return layout;
        });
    }

    private Component createInfoLayout(Client client) {
        VerticalLayout infoBox = uiComponents.create(VerticalLayout.class);
        infoBox.setPadding(false);

        infoBox.getElement().appendChild(
                ElementFactory.createStrong(client.getName())
        );

        if (client.getContactInformation() != null) {
            infoBox.add(createContactInformationLayout(client));
        }

        return infoBox;
    }

    private Component createContactInformationLayout(Client client) {
        VerticalLayout contactInformationLayout = uiComponents.create(VerticalLayout.class);

        createContactInformationItem(
                client.getContactInformation().getEmail(),
                GravityUiIcon.ENVELOPE.create(),
                Text::new
        ).ifPresent(contactInformationLayout::add);

        createContactInformationItem(
                client.getContactInformation().getPhone(),
                GravityUiIcon.HANDSET.create(),
                Text::new
        ).ifPresent(contactInformationLayout::add);

        createContactInformationItem(
                client.getContactInformation().getAddress(),
                GravityUiIcon.MAP_PIN.create(),
                Text::new
        ).ifPresent(contactInformationLayout::add);

        createContactInformationItem(
                client.getContactInformation().getUrl(),
                GravityUiIcon.LINK.create(),
                text -> new Anchor(text, text, AnchorTarget.BLANK)
        ).ifPresent(contactInformationLayout::add);

        Details contactDetails = uiComponents.create(Details.class);
        contactDetails.setOpened(true);
        contactDetails.addThemeVariants(DetailsVariant.AURA_NO_PADDING);
        contactDetails.setSummaryText(
                messageTools.getPropertyCaption(clientsDc.getEntityMetaClass(), "contactInformation"));
        contactDetails.add(contactInformationLayout);

        return contactDetails;
    }

    private Optional<Component> createContactInformationItem(@Nullable String value, Icon icon,
                                                             Function<String, Component> valueRenderer) {
        return Strings.isNullOrEmpty(value) ?
                Optional.empty() :
                Optional.of(new HorizontalLayout(icon, valueRenderer.apply(value)));
    }

    private Component createActionsLayout(Client client) {
        VerticalLayout actionsBox = uiComponents.create(VerticalLayout.class);
        actionsBox.setPadding(false);
        actionsBox.setWidth("auto");

        Button editBtn = createEditButton(client);
        Button removeBtn = createRemoveButton(client);
        actionsBox.add(editBtn, removeBtn);
        return actionsBox;
    }

    private Button createEditButton(Client client) {
        boolean editPermitted = editPermitted(client);

        Button button = uiComponents.create(Button.class);
        button.setTooltipText(messages.getMessage(editPermitted ? "actions.Edit" : "actions.Read"));
        button.setIcon(editPermitted ? GravityUiIcon.PENCIL_TO_SQUARE.create() : GravityUiIcon.EYE.create());
        button.addClickListener(event -> editClient(client));

        return button;
    }

    private Button createRemoveButton(Client client) {
        Button button = uiComponents.create(Button.class);
        button.setTooltipText(messages.getMessage("actions.Remove"));
        button.setIcon(GravityUiIcon.TRASH_BIN.create());
        button.addThemeVariants(ButtonVariant.ERROR);
        button.addClickListener(event -> removeClient(client));

        button.setVisible(deletePermitted(client));

        return button;
    }

    private boolean editPermitted(Client client) {
        return getEntityContext().isEditPermitted()
                && (getCrudEntityContext().updatePredicate() == null
                || getCrudEntityContext().isUpdatePermitted(client));
    }

    private boolean deletePermitted(Client client) {
        return getEntityContext().isDeletePermitted()
                && (getCrudEntityContext().updatePredicate() == null
                || getCrudEntityContext().isDeletePermitted(client));
    }

    private void editClient(Client client) {
        viewNavigators.detailView(this, Client.class)
                .editEntity(client)
                .withBackwardNavigation(true)
                .navigate();
    }

    private void removeClient(Client client) {
        removeOperation.builder(Client.class, this)
                .withConfirmation(true)
                .withItems(List.of(client))
                .withContainer(clientsDc)
                .remove();
    }

    @Subscribe(id = "createBtn", subject = "clickListener")
    public void onCreateBtnClick(final ClickEvent<JmixButton> event) {
        viewNavigators.detailView(this, Client.class)
                .newEntity()
                .withBackwardNavigation(true)
                .navigate();
    }

    private UiEntityContext getEntityContext() {
        if (entityContext == null) {
            MetaClass metaClass = clientsDc.getEntityMetaClass();
            entityContext = new UiEntityContext(metaClass);
            accessManager.applyRegisteredConstraints(entityContext);
        }

        return entityContext;
    }

    private InMemoryCrudEntityContext getCrudEntityContext() {
        if (crudEntityContext == null) {
            crudEntityContext = new InMemoryCrudEntityContext(clientsDc.getEntityMetaClass(), getApplicationContext());
            accessManager.applyRegisteredConstraints(crudEntityContext);
        }

        return crudEntityContext;
    }

    @Subscribe(id = "clientsDc", target = Target.DATA_CONTAINER)
    public void onClientsDcCollectionChange(final CollectionContainer.CollectionChangeEvent<Client> event) {
        noDataContainer.setVisible(clientsDc.getItems().isEmpty());
    }
}
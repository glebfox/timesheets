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

package com.glebfox.timesheets.view.client;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.data.renderer.AvatarCellRenderer;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.misc.ContactInformation;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.view.project.ProjectDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.renderer.PopoverRenderer;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class ClientListViewUiTest {

    static final String NAME_PREFIX = "Client list test ";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void listShowsClientColumns() {
        DataGrid<Client> grid = openListGrid();

        assertThat(grid.getColumns()).extracting(Grid.Column::getKey)
                .containsExactly("name", "email", "phone", "url", "address");
        // The logo and name cell, and the address cut short
        assertThat(column(grid, "name").getRenderer()).isInstanceOf(AvatarCellRenderer.class);
        assertThat(column(grid, "address").getRenderer()).isInstanceOf(PopoverRenderer.class);
    }

    @Test
    void urlCellLinksToTheSiteInANewTab() {
        Client saved = saveClient("https://example.test");

        DataGrid<Client> grid = openListGrid();

        Component cell = urlCell(grid, listed(grid, saved));
        assertThat(cell).isInstanceOfSatisfying(Anchor.class, anchor -> {
            assertThat(anchor.getHref()).isEqualTo("https://example.test");
            assertThat(anchor.getTarget()).contains("_blank");
        });
    }

    @Test
    void urlCellIsEmptyWithoutUrl() {
        Client saved = saveClient(null);

        DataGrid<Client> grid = openListGrid();

        Component cell = urlCell(grid, listed(grid, saved));
        assertThat(cell).satisfiesAnyOf(
                component -> assertThat(component).isNull(),
                component -> assertThat(component).isNotInstanceOf(Anchor.class));
    }

    @Test
    void clientLookupOpensTheList() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), Project.class).newEntity().open();
        ProjectDetailView editor = UiTestUtils.getCurrentView();
        EntityComboBox<Client> clientField = UiTestUtils.getComponent(editor, "clientField");

        Action lookup = Objects.requireNonNull(clientField.getAction("entityLookup"), "the lookup action");
        assertThat(lookup.isEnabled()).isTrue();
        lookup.actionPerform(clientField);

        View<?> dialog = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the client picker");
        assertThat(dialog).isInstanceOf(ClientListView.class);
        assertThat(UiTestUtils.<Component>getComponent(dialog, "lookupActions").isVisible()).isTrue();
    }

    @AfterEach
    void tearDown() {
        // Clients are soft-deleted, so they are removed for good, reloaded by id
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    private DataGrid<Client> openListGrid() {
        viewBuilders.view(UiTestUtils.getCurrentView(), ClientListView.class).open();
        return UiTestUtils.getComponent(UiTestUtils.getCurrentView(), "clientsDataGrid");
    }

    private Client saveClient(@Nullable String url) {
        Client client = dataManager.create(Client.class);
        client.setName(NAME_PREFIX + UUID.randomUUID());
        if (url != null) {
            ContactInformation contactInformation = dataManager.create(ContactInformation.class);
            contactInformation.setUrl(url);
            client.setContactInformation(contactInformation);
        }
        Client saved = dataManager.save(client);
        createdIds.add(Id.of(saved));
        return saved;
    }

    /**
     * The row of the given client as the list loaded it.
     */
    private static Client listed(DataGrid<Client> grid, Client client) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(item -> item.getId().equals(client.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the client is not listed"));
    }

    private static Grid.Column<Client> column(DataGrid<Client> grid, String key) {
        return Objects.requireNonNull(grid.getColumnByKey(key), key + " column");
    }

    private static @Nullable Component urlCell(DataGrid<Client> grid, Client client) {
        ComponentRenderer<?, Client> renderer = (ComponentRenderer<?, Client>) column(grid, "url").getRenderer();
        return renderer.createComponent(client);
    }
}

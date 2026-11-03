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

package com.glebfox.timesheets.menu;

import com.glebfox.timesheets.TimesheetsApplication;
import com.vaadin.flow.component.icon.SvgIcon;
import io.jmix.flowui.menu.MenuConfig;
import io.jmix.flowui.menu.MenuItem;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

// The menu creates its icon components through the current UI, so it loads only in a UI test
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class MenuIconsTest {

    static final String SPRITE = "themes/timesheets-aura/icons/menu-icons.svg";
    static final String STYLE = "fill: none; stroke: var(--vaadin-icon-color, currentColor); "
            + "stroke-width: var(--ts-menu-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round";
    static final Map<String, String> SYMBOLS = Map.ofEntries(
            entry("timesheets", "timesheets"), entry("ts_CalendarView", "calendar"),
            entry("ts_TimeEntry.my", "my-time-entries"), entry("ts_Task.my", "my-tasks"),
            entry("projects", "project-management"), entry("ts_Client.list", "clients"),
            entry("ts_Project.list", "projects"), entry("ts_TimeEntry.list", "time-entries"),
            entry("ts_TimeEntry.approval", "approve-time-entries"), entry("settings", "settings"),
            entry("ts_TaskType.list", "task-types"), entry("ts_ProjectRole.list", "project-roles"),
            entry("ts_WorkSchedule.list", "work-schedules"), entry("ts_HolidayCalendar.list", "holiday-calendars"),
            entry("administration", "administration"), entry("ts_User.list", "users"),
            entry("security", "security"), entry("sec_ResourceRoleModel.list", "resource-roles"),
            entry("sec_RowLevelRoleModel.list", "row-level-roles"), entry("datatools", "data-tools"),
            entry("datatl_entityInspectorListView", "entity-inspector"),
            entry("pessimisticlock", "pessimistic-locking"), entry("pslock_LockInfo.list", "locks"));

    @Autowired
    MenuConfig menuConfig;

    @Test
    void everyMenuEntryShowsItsOwnSymbolFromTheSprite() {
        Map<String, String> collected = new HashMap<>();
        collectSymbols(menuConfig.getRootItems(), collected);

        assertThat(collected).isEqualTo(SYMBOLS);
        assertThat(new HashSet<>(SYMBOLS.values())).hasSize(SYMBOLS.size());
    }

    @Test
    void spriteDefinesExactlyTheMenuSymbolsOnTheSixteenGrid() throws Exception {
        List<Element> symbols = loadSymbols();

        assertThat(symbols).extracting(symbol -> symbol.getAttribute("id"))
                .containsExactlyInAnyOrderElementsOf(SYMBOLS.values());
        for (Element symbol : symbols) {
            assertThat(symbol.getAttribute("viewBox")).as(symbol.getAttribute("id")).isEqualTo("0 0 16 16");
        }
    }

    @Test
    void everySymbolDrawsWithTheProjectStrokeOnly() throws Exception {
        for (Element symbol : loadSymbols()) {
            String id = symbol.getAttribute("id");
            List<Element> children = childElements(symbol);

            assertThat(children).as(id).hasSize(1);
            Element group = children.getFirst();
            assertThat(group.getLocalName()).as(id).isEqualTo("g");
            assertThat(group.getAttribute("style")).as(id).isEqualTo(STYLE);

            NodeList shapes = group.getElementsByTagNameNS("*", "*");
            for (int i = 0; i < shapes.getLength(); i++) {
                Element shape = (Element) shapes.item(i);
                for (String attribute : List.of("fill", "stroke", "stroke-width", "style")) {
                    assertThat(shape.hasAttribute(attribute))
                            .as("%s: <%s> has %s", id, shape.getLocalName(), attribute)
                            .isFalse();
                }
            }
        }
    }

    @Test
    void everySymbolIsOnePathSoThatTouchingLinesDoNotDarken() throws Exception {
        // A menu item's text color is translucent, and every element of a symbol is painted on its own,
        // so lines of two elements that touch would be painted twice where they meet
        for (Element symbol : loadSymbols()) {
            String id = symbol.getAttribute("id");
            List<Element> shapes = childElements(childElements(symbol).getFirst());

            assertThat(shapes).as(id).hasSize(1);
            assertThat(shapes.getFirst().getLocalName()).as(id).isEqualTo("path");
        }
    }

    void collectSymbols(List<MenuItem> items, Map<String, String> collected) {
        for (MenuItem item : items) {
            if (item.isSeparator()) {
                continue;
            }
            assertThat(item.getIcon()).as(item.getId()).isInstanceOfSatisfying(SvgIcon.class, icon -> {
                assertThat(icon.getSrc()).as(item.getId()).isEqualTo(SPRITE);
                collected.put(item.getId(), icon.getSymbol());
            });
            collectSymbols(item.getChildren(), collected);
        }
    }

    List<Element> loadSymbols() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/resources/" + SPRITE)) {
            assertThat(stream).as(SPRITE).isNotNull();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(stream);
            NodeList nodes = document.getElementsByTagNameNS("http://www.w3.org/2000/svg", "symbol");
            List<Element> symbols = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                symbols.add((Element) nodes.item(i));
            }
            return symbols;
        }
    }

    List<Element> childElements(Element parent) {
        List<Element> children = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) {
                children.add(element);
            }
        }
        return children;
    }
}

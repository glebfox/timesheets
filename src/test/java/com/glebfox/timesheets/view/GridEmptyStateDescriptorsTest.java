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

package com.glebfox.timesheets.view;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain guard over the view descriptors: every grid declares the project's empty state, with an icon from the
 * {@code ts} set, a text key present in both message bundles, and the grid's {@code list_refresh} action where the
 * grid has one and no action where it has none.
 */
class GridEmptyStateDescriptorsTest {

    static final String APP_NS = "http://glebfox.com/schema/app-ui-components.xsd";
    static final String ICON_PREFIX = "ts:";
    static final String MSG_PREFIX = "msg://";
    static final String REFRESH_TYPE = "list_refresh";
    static final Path RESOURCES = Path.of("src/main/resources/com/glebfox/timesheets");
    static final Path VIEWS = RESOURCES.resolve("view");
    static final Path ICONSET = Path.of("src/main/frontend/src/component/icon/ts-icons.js");
    static final Set<String> GRIDS = Set.of("dataGrid", "treeDataGrid");

    record Grid(Path file, String id, Element element) {
        String label() {
            return VIEWS.relativize(file) + " grid '" + id + "'";
        }
    }

    @Test
    void everyGridHasAGridEmptyState() throws Exception {
        List<Grid> grids = grids();
        assertThat(grids).isNotEmpty();

        List<String> problems = new ArrayList<>();
        for (Grid grid : grids) {
            List<Element> slots = children(grid.element(), "emptyStateComponent", null);
            if (slots.size() != 1) {
                problems.add(grid.label() + ": expected one emptyStateComponent, found " + slots.size());
                continue;
            }
            List<Element> content = children(slots.getFirst(), null, null);
            if (content.size() != 1 || !isEmptyState(content.getFirst())) {
                problems.add(grid.label() + ": emptyStateComponent must hold only app:gridEmptyState");
            }
        }
        assertThat(problems).isEmpty();
    }

    @Test
    void everyEmptyStateIconIsInTheSet() throws Exception {
        Set<String> icons = iconIds();
        assertThat(icons).isNotEmpty();

        List<String> problems = new ArrayList<>();
        for (Grid grid : grids()) {
            for (Element state : emptyStates(grid)) {
                String icon = state.getAttribute("icon");
                if (!icon.startsWith(ICON_PREFIX) || !icons.contains(icon.substring(ICON_PREFIX.length()))) {
                    problems.add(grid.label() + ": icon '" + icon + "' is not in ts-icons.js");
                }
            }
        }
        assertThat(problems).isEmpty();
    }

    @Test
    void everyEmptyStateTextIsAKeyInBothBundles() throws Exception {
        Properties en = bundle("messages_en.properties");
        Properties ru = bundle("messages_ru_RU.properties");

        List<String> problems = new ArrayList<>();
        for (Grid grid : grids()) {
            String messageGroup = "com.glebfox.timesheets.view." + VIEWS.relativize(grid.file()).getParent();
            for (Element state : emptyStates(grid)) {
                String text = state.getAttribute("text");
                if (!text.startsWith(MSG_PREFIX)) {
                    problems.add(grid.label() + ": text '" + text + "' is not a msg:// key");
                    continue;
                }
                String key = messageGroup.replace('/', '.') + "/" + text.substring(MSG_PREFIX.length());
                if (!en.containsKey(key)) {
                    problems.add(grid.label() + ": '" + key + "' is missing in messages_en.properties");
                }
                if (!ru.containsKey(key)) {
                    problems.add(grid.label() + ": '" + key + "' is missing in messages_ru_RU.properties");
                }
            }
        }
        assertThat(problems).isEmpty();
    }

    @Test
    void everyEmptyStateTakesItsGridsRefresh() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Grid grid : grids()) {
            List<String> refreshIds = refreshActionIds(grid);
            String expected = refreshIds.isEmpty() ? "" : grid.id() + "." + refreshIds.getFirst();
            for (Element state : emptyStates(grid)) {
                String action = state.getAttribute("action");
                if (!expected.equals(action)) {
                    problems.add(grid.label() + ": action '" + action + "', expected '" + expected + "'");
                }
            }
        }
        assertThat(problems).isEmpty();
    }

    List<String> refreshActionIds(Grid grid) {
        List<String> ids = new ArrayList<>();
        for (Element actions : children(grid.element(), "actions", null)) {
            for (Element action : children(actions, "action", null)) {
                if (REFRESH_TYPE.equals(action.getAttribute("type"))) {
                    ids.add(action.getAttribute("id"));
                }
            }
        }
        return ids;
    }

    List<Element> emptyStates(Grid grid) {
        List<Element> states = new ArrayList<>();
        for (Element slot : children(grid.element(), "emptyStateComponent", null)) {
            states.addAll(children(slot, "gridEmptyState", APP_NS));
        }
        return states;
    }

    boolean isEmptyState(Element element) {
        return "gridEmptyState".equals(element.getLocalName()) && APP_NS.equals(element.getNamespaceURI());
    }

    List<Grid> grids() throws Exception {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(VIEWS)) {
            files = walk.filter(p -> p.toString().endsWith(".xml")).sorted().toList();
        }
        List<Grid> grids = new ArrayList<>();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        for (Path file : files) {
            Element root = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
            NodeList all = root.getElementsByTagNameNS("*", "*");
            for (int i = 0; i < all.getLength(); i++) {
                Element element = (Element) all.item(i);
                if (GRIDS.contains(element.getLocalName())) {
                    grids.add(new Grid(file, element.getAttribute("id"), element));
                }
            }
        }
        return grids;
    }

    List<Element> children(Element parent, String localName, String namespace) {
        List<Element> children = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element
                    && (localName == null || localName.equals(element.getLocalName()))
                    && (namespace == null || namespace.equals(element.getNamespaceURI()))) {
                children.add(element);
            }
        }
        return children;
    }

    Set<String> iconIds() throws IOException {
        Matcher matcher = Pattern.compile("<g id=\"" + ICON_PREFIX + "([^\"]+)\"").matcher(Files.readString(ICONSET));
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    Properties bundle(String name) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(RESOURCES.resolve(name), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}

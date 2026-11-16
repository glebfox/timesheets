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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain guard over the view and fragment descriptors: every entity drop-down has the {@code entity_lookup} and
 * {@code entity_clear} actions. {@code entityPicker} is not checked.
 */
class EntityComboBoxActionsDescriptorTest {

    static final Path VIEWS = Path.of("src/main/resources/com/glebfox/timesheets/view");

    @Test
    void everyEntityDropDownHasAPickerAndAClearButton() throws Exception {
        assertThat(entityComboBoxesWithoutPickerOrClear(VIEWS)).containsExactlyInAnyOrder(
                // Older views that predate the rule
                "task/task-lookup-view.xml#projectFilterField",
                "workschedule/default-work-schedule-delete-view.xml#newDefaultField",
                "workscheduleassignment/work-schedule-assignment-detail-view.xml#workScheduleField");
    }

    /**
     * Returns the entity drop-downs in the descriptors under {@code root} that lack the picker or the clear button,
     * as {@code <path relative to root>#<field id>}. Elements are matched by local name in any namespace, so view,
     * fragment, and main-view descriptors are all read.
     */
    static List<String> entityComboBoxesWithoutPickerOrClear(Path root) throws Exception {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".xml")).sorted().toList();
        }
        List<String> offenders = new ArrayList<>();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        for (Path file : files) {
            Element descriptor = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
            NodeList all = descriptor.getElementsByTagNameNS("*", "*");
            for (int i = 0; i < all.getLength(); i++) {
                Element field = (Element) all.item(i);
                if ("entityComboBox".equals(field.getLocalName())
                        && !actionTypes(field).containsAll(Set.of("entity_lookup", "entity_clear"))) {
                    offenders.add(root.relativize(file) + "#" + field.getAttribute("id"));
                }
            }
        }
        return offenders;
    }

    static Set<String> actionTypes(Element field) {
        Set<String> types = new HashSet<>();
        for (Element actions : children(field, "actions")) {
            for (Element action : children(actions, "action")) {
                types.add(action.getAttribute("type"));
            }
        }
        return types;
    }

    static List<Element> children(Element parent, String localName) {
        List<Element> children = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                children.add(element);
            }
        }
        return children;
    }
}

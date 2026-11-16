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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain guard over the view and fragment descriptors: a button that shows an icon and no text has a tooltip. Buttons
 * built in Java are not seen.
 */
class IconButtonTooltipsDescriptorTest {

    static final Path VIEWS = Path.of("src/main/resources/com/glebfox/timesheets/view");
    static final Path FIXTURES = Path.of("src/test/resources/com/glebfox/timesheets/view/iconbuttons");
    static final Set<String> BUTTONS = Set.of("button", "dropdownButton", "comboButton");
    static final String TYPE_WITHOUT_DEFAULTS = "list_itemTracking";

    @Test
    void findsIconOnlyButtonsWithoutATooltip() throws Exception {
        assertThat(iconOnlyButtonsWithoutTooltip(FIXTURES)).containsExactlyInAnyOrder(
                "buttons-view.xml#iconNoTooltip",
                "buttons-view.xml#iconEmptyTooltip",
                "buttons-view.xml#iconChildNoTooltip",
                "buttons-view.xml#prefixChildNoTooltip",
                "buttons-view.xml#actionIconNoText",
                "buttons-view.xml#typedActionEmptyText",
                "buttons-view.xml#actionWithTextEmptyText",
                "buttons-view.xml#trackingActionIconNoText",
                "buttons-view.xml#dropdownIconOnly",
                "buttons-fragment.xml#fragmentIconNoTooltip");
    }

    @Test
    void everyIconOnlyButtonHasATooltip() throws Exception {
        assertThat(iconOnlyButtonsWithoutTooltip(VIEWS)).isEmpty();
    }

    /**
     * Returns the icon-only buttons with no tooltip in the descriptors under {@code root}, as
     * {@code <path relative to root>#<button id>}. Elements are matched by local name in any namespace, so view,
     * fragment, and main-view descriptors are all read.
     */
    static List<String> iconOnlyButtonsWithoutTooltip(Path root) throws Exception {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".xml")).sorted().toList();
        }
        List<String> offenders = new ArrayList<>();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        for (Path file : files) {
            Element descriptor = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
            List<Element> elements = elements(descriptor);
            Map<String, Element> actions = new HashMap<>();
            for (Element element : elements) {
                if ("action".equals(element.getLocalName())) {
                    actions.putIfAbsent(element.getAttribute("id"), element);
                }
            }
            for (Element element : elements) {
                if (BUTTONS.contains(element.getLocalName()) && isIconOnly(element, actions) && !hasTooltip(element)) {
                    offenders.add(root.relativize(file) + "#" + element.getAttribute("id"));
                }
            }
        }
        return offenders;
    }

    static boolean isIconOnly(Element button, Map<String, Element> actions) {
        String actionRef = button.getAttribute("action");
        // A <component>.<action> reference names the action by the part after the last dot
        Element action = actionRef.isBlank() ? null : actions.get(actionRef.substring(actionRef.lastIndexOf('.') + 1));
        boolean hasIcon = !button.getAttribute("icon").isBlank()
                || !children(button, Set.of("icon", "prefix", "suffix")).isEmpty()
                || (action != null && (!action.getAttribute("icon").isBlank() || hasDefaults(action)));
        boolean hasText;
        if (button.hasAttribute("text")) {
            // The loader sets the button's own text after the action's, even an empty one, which clears the action's
            hasText = !button.getAttribute("text").isBlank();
        } else if (action != null) {
            hasText = !action.getAttribute("text").isBlank() || hasDefaults(action);
        } else {
            // An action the descriptor does not declare has an unknown text, so its button is not judged
            hasText = !actionRef.isBlank();
        }
        return hasIcon && !hasText;
    }

    /**
     * Whether the action's type brings a text and an icon of its own, as the list, entity, and detail actions and the
     * project's own types do. The item tracking action has neither.
     */
    static boolean hasDefaults(Element action) {
        String type = action.getAttribute("type");
        return !type.isBlank() && !TYPE_WITHOUT_DEFAULTS.equals(type);
    }

    static boolean hasTooltip(Element button) {
        return children(button, Set.of("tooltip")).stream().anyMatch(tooltip -> !tooltip.getAttribute("text").isBlank());
    }

    static List<Element> elements(Element root) {
        NodeList all = root.getElementsByTagNameNS("*", "*");
        List<Element> elements = new ArrayList<>(all.getLength());
        for (int i = 0; i < all.getLength(); i++) {
            elements.add((Element) all.item(i));
        }
        return elements;
    }

    static List<Element> children(Element parent, Set<String> localNames) {
        List<Element> children = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && localNames.contains(element.getLocalName())) {
                children.add(element);
            }
        }
        return children;
    }
}

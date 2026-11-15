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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain guard over the view descriptors: every field of a form that can take a prefix has one. Multi-select drop-downs,
 * the date range field, switches, segmented bars, Markdown editors, and avatar fields have no prefix area and are not
 * checked.
 */
class FieldPrefixIconsDescriptorTest {

    static final String JMIX_NS = "http://jmix.io/schema/flowui/view";
    static final Path VIEWS = Path.of("src/main/resources/com/glebfox/timesheets/view");
    static final Set<String> PREFIX_CAPABLE = Set.of(
            "textField", "textArea", "passwordField", "emailField", "numberField", "integerField", "bigDecimalField",
            "datePicker", "timePicker", "comboBox", "entityComboBox", "entityPicker", "valuePicker",
            "multiValuePicker", "select");

    @Test
    void everyFormFieldThatTakesAPrefixHasOne() throws Exception {
        assertThat(fieldsWithoutPrefix()).containsExactlyInAnyOrder(
                // Their controllers set the prefix by the value, an icon per status
                "project/project-detail-view.xml#statusField",
                "task/task-detail-view.xml#statusField",
                "timeentry/time-entry-detail-view.xml#statusField");
    }

    List<String> fieldsWithoutPrefix() throws Exception {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(VIEWS)) {
            files = walk.filter(p -> p.toString().endsWith(".xml")).sorted().toList();
        }
        List<String> missing = new ArrayList<>();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        for (Path file : files) {
            Element root = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
            NodeList forms = root.getElementsByTagNameNS(JMIX_NS, "formLayout");
            for (int i = 0; i < forms.getLength(); i++) {
                NodeList descendants = ((Element) forms.item(i)).getElementsByTagNameNS(JMIX_NS, "*");
                for (int j = 0; j < descendants.getLength(); j++) {
                    Element field = (Element) descendants.item(j);
                    String label = VIEWS.relativize(file) + "#" + field.getAttribute("id");
                    // A form inside a form lists its fields twice
                    if (PREFIX_CAPABLE.contains(field.getLocalName()) && !hasPrefix(field)
                            && !missing.contains(label)) {
                        missing.add(label);
                    }
                }
            }
        }
        return missing;
    }

    boolean hasPrefix(Element field) {
        for (Node node = field.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && "prefix".equals(element.getLocalName())) {
                return true;
            }
        }
        return false;
    }
}

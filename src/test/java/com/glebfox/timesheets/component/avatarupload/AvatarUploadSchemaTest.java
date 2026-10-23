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

package com.glebfox.timesheets.component.avatarupload;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AvatarUploadSchemaTest {

    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    @Test
    void schemaSuggestsExactlyTheSupportedFileTypes() throws Exception {
        Set<String> supported = Arrays.stream(AvatarImageFormat.values())
                .flatMap(format -> Stream.concat(Stream.of(format.getMimeType()), format.getExtensions().stream()))
                .collect(Collectors.toSet());

        assertEquals(supported, schemaEnumeration("avatarUploadAcceptedFileTypes"));
    }

    private static Set<String> schemaEnumeration(String simpleTypeName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document schema;
        try (InputStream in = AvatarUploadSchemaTest.class.getResourceAsStream(
                "/com/glebfox/timesheets/app-ui-components.xsd")) {
            assertNotNull(in);
            schema = factory.newDocumentBuilder().parse(in);
        }

        NodeList simpleTypes = schema.getElementsByTagNameNS(XSD_NS, "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            Element simpleType = (Element) simpleTypes.item(i);
            if (simpleTypeName.equals(simpleType.getAttribute("name"))) {
                Set<String> values = new HashSet<>();
                NodeList enumerations = simpleType.getElementsByTagNameNS(XSD_NS, "enumeration");
                for (int j = 0; j < enumerations.getLength(); j++) {
                    values.add(((Element) enumerations.item(j)).getAttribute("value"));
                }
                return values;
            }
        }
        throw new AssertionError("No simple type " + simpleTypeName);
    }
}

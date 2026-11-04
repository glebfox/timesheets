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

import com.glebfox.timesheets.component.icon.MenuIconset;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.Icon;
import io.jmix.flowui.menu.MenuConfig;
import io.jmix.flowui.menu.MenuItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class MenuIconsTest {

    static final String ICONSET = "ts-menu";
    static final String MODULE = "src/component/icon/ts-menu-icons.js";
    static final String STYLE = "fill: none; stroke: var(--vaadin-icon-color, currentColor); "
            + "stroke-width: var(--ts-menu-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round";
    static final Map<String, String> ICONS = Map.ofEntries(
            entry("timesheets", "timesheets"), entry("ts_CalendarView", "calendar"),
            entry("ts_TimeEntry.my", "my-time-entries"), entry("ts_Task.my", "my-tasks"),
            entry("projects", "project-management"), entry("ts_Client.list", "clients"),
            entry("ts_Project.list", "projects"), entry("ts_TimeEntry.list", "time-entries"),
            entry("ts_TimeEntry.approval", "approve-time-entries"), entry("settings", "settings"),
            entry("ts_TaskType.list", "task-types"), entry("ts_ProjectRole.list", "project-roles"),
            entry("ts_WorkSchedule.list", "work-schedules"), entry("ts_HolidayCalendar.list", "holiday-calendars"),
            entry("ts_AbsenceType.list", "absence-types"),
            entry("administration", "administration"), entry("ts_User.list", "users"),
            entry("security", "security"), entry("sec_ResourceRoleModel.list", "resource-roles"),
            entry("sec_RowLevelRoleModel.list", "row-level-roles"), entry("datatools", "data-tools"),
            entry("datatl_entityInspectorListView", "entity-inspector"),
            entry("pessimisticlock", "pessimistic-locking"), entry("pslock_LockInfo.list", "locks"));

    @Autowired
    MenuConfig menuConfig;

    @Test
    void everyMenuEntryShowsItsOwnIconFromTheMenuSet() {
        Map<String, String> collected = new HashMap<>();
        collectIcons(menuConfig.getRootItems(), collected);

        assertThat(collected).isEqualTo(ICONS.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ICONSET + ":" + e.getValue())));
        assertThat(new HashSet<>(ICONS.values())).hasSize(ICONS.size());
    }

    @Test
    void theMenuSetIsBundledWithTheApplication() {
        assertThat(MenuIconset.class.getAnnotation(JsModule.class).value()).isEqualTo("./" + MODULE);
    }

    @Test
    void iconsetDefinesExactlyTheMenuIconsOnTheSixteenGrid() throws Exception {
        Element iconset = loadIconset();

        assertThat(iconset.getTagName()).isEqualTo("vaadin-iconset");
        assertThat(iconset.getAttribute("name")).isEqualTo(ICONSET);
        assertThat(iconset.getAttribute("size")).isEqualTo("16");
        assertThat(icons(iconset)).extracting(icon -> icon.getAttribute("id"))
                .containsExactlyInAnyOrderElementsOf(ICONS.values().stream().map(id -> ICONSET + ":" + id).toList());
    }

    @Test
    void everyIconDrawsWithTheProjectStrokeOnly() throws Exception {
        for (Element icon : icons(loadIconset())) {
            String id = icon.getAttribute("id");
            assertThat(icon.getAttribute("style")).as(id).isEqualTo(STYLE);

            NodeList shapes = icon.getElementsByTagName("*");
            for (int i = 0; i < shapes.getLength(); i++) {
                Element shape = (Element) shapes.item(i);
                for (String attribute : List.of("fill", "stroke", "stroke-width", "style")) {
                    assertThat(shape.hasAttribute(attribute))
                            .as("%s: <%s> has %s", id, shape.getTagName(), attribute)
                            .isFalse();
                }
            }
        }
    }

    @Test
    void everyIconIsOnePathSoThatTouchingLinesDoNotDarken() throws Exception {
        // A menu item's text color is translucent, and every element of an icon is painted on its own,
        // so lines of two elements that touch would be painted twice where they meet
        for (Element icon : icons(loadIconset())) {
            String id = icon.getAttribute("id");
            List<Element> shapes = childElements(icon);

            assertThat(shapes).as(id).hasSize(1);
            assertThat(shapes.getFirst().getTagName()).as(id).isEqualTo("path");
        }
    }

    void collectIcons(List<MenuItem> items, Map<String, String> collected) {
        for (MenuItem item : items) {
            if (item.isSeparator()) {
                continue;
            }
            assertThat(item.getIcon()).as(item.getId()).isInstanceOfSatisfying(Icon.class,
                    icon -> collected.put(item.getId(), icon.getElement().getAttribute("icon")));
            collectIcons(item.getChildren(), collected);
        }
    }

    /**
     * Parses the {@code <vaadin-iconset>} markup that the module puts into its template.
     */
    Element loadIconset() throws Exception {
        String module = Files.readString(Path.of("src/main/frontend", MODULE));
        int start = module.indexOf("<vaadin-iconset");
        int end = module.indexOf("</vaadin-iconset>") + "</vaadin-iconset>".length();
        assertThat(start).as(MODULE).isNotNegative();
        byte[] markup = module.substring(start, end).getBytes(StandardCharsets.UTF_8);
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(markup))
                .getDocumentElement();
    }

    List<Element> icons(Element iconset) {
        List<Element> icons = new ArrayList<>();
        NodeList groups = iconset.getElementsByTagName("g");
        for (int i = 0; i < groups.getLength(); i++) {
            Element group = (Element) groups.item(i);
            if (group.hasAttribute("id")) {
                icons.add(group);
            }
        }
        return icons;
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

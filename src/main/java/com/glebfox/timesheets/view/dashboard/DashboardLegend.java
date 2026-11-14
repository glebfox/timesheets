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

package com.glebfox.timesheets.view.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.jmix.chartsflowui.kit.component.model.shared.Color;
import io.jmix.flowui.UiComponents;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The legend under a bar chart of the dashboard: a line of items, each a mark that repeats how the chart draws a
 * series, in the series' color, and the series' name. A block lists only what its chart shows, in an order of its
 * own that does not change with the data, so an item never moves to another place.
 */
final class DashboardLegend {

    /**
     * The custom properties of a mark that hold its colors, from {@link DashboardColors}, for the stylesheet.
     */
    private static final String COLOR_PROPERTY = "--dashboard-mark-color";
    private static final String STRIPE_PROPERTY = "--dashboard-mark-stripe";

    private DashboardLegend() {
    }

    /**
     * Fills the legend with the items, and hides it without any.
     *
     * @param legend       the legend's container
     * @param items        the items, in their order
     * @param uiComponents the factory of the items' parts
     */
    static void show(Div legend, List<Item> items, UiComponents uiComponents) {
        legend.removeAll();
        legend.setVisible(!items.isEmpty());
        for (Item item : items) {
            legend.add(createItem(item, uiComponents));
        }
    }

    /**
     * Returns an item of a series drawn as a plain bar, in its color.
     */
    static Item bar(Color color, String text) {
        return new Item(Mark.BAR, color, null, text);
    }

    /**
     * Returns the item of the plan, drawn as a dashed outline.
     */
    static Item plan(String text) {
        return new Item(Mark.PLAN, DashboardColors.PLAN, null, text);
    }

    /**
     * Returns the item of the time on other projects, drawn as a bar in the neutral with stripes over it.
     */
    static Item otherProjects(String text) {
        return new Item(Mark.OTHER_PROJECTS, DashboardColors.OTHER_PROJECTS, DashboardColors.OTHER_PROJECTS_STRIPE,
                text);
    }

    /**
     * Returns the item of time off, drawn hatched in the color of a day off.
     */
    static Item off(String text) {
        return new Item(Mark.OFF, DashboardColors.OFF_DAY, DashboardColors.OFF_DAY_HATCH, text);
    }

    private static Span createItem(Item item, UiComponents uiComponents) {
        Span mark = uiComponents.create(Span.class);
        mark.addClassNames("mark", item.mark().className);
        // The series' colors, as the chart draws it
        mark.getStyle().set(COLOR_PROPERTY, item.color().getValue());
        Color stripe = item.stripe();
        if (stripe != null) {
            mark.getStyle().set(STRIPE_PROPERTY, stripe.getValue());
        }

        Span text = uiComponents.create(Span.class);
        text.addClassName("text");
        text.setText(item.text());

        Span row = uiComponents.create(Span.class);
        row.addClassName("dashboard-legend-item");
        row.add(mark, text);
        return row;
    }

    /**
     * How an item's mark repeats its series: the class that the stylesheet draws it by.
     */
    enum Mark {
        BAR("bar"),
        PLAN("plan"),
        OTHER_PROJECTS("other-projects"),
        OFF("off");

        private final String className;

        Mark(String className) {
            this.className = className;
        }
    }

    /**
     * An item of the legend.
     *
     * @param mark   how its mark is drawn
     * @param color  the series' color
     * @param stripe the color of the stripes over it, if it is hatched
     * @param text   the series' name
     */
    record Item(Mark mark, Color color, @Nullable Color stripe, String text) {
    }
}

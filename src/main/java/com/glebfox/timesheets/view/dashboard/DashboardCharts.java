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

import com.glebfox.timesheets.datatype.WorkTime;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.axis.AbstractCartesianAxis;
import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.Encode;
import io.jmix.chartsflowui.kit.component.model.series.ItemStyleWithDecal;
import io.jmix.chartsflowui.kit.component.model.shared.AbstractTooltip;
import io.jmix.chartsflowui.kit.component.model.shared.Decal;
import io.jmix.chartsflowui.kit.component.model.shared.Overflow;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;

/**
 * The options that the dashboard's bar charts share: a category per bar on the x axis, hours on the y axis, a
 * tooltip that shows the text each row carries, and the plan marked as a dashed outline over the bar of time
 * spent.
 * <p>
 * Each row of a chart's data set carries its tooltip, ready-made in HTML, in the {@link #TOOLTIP} field: the
 * text and its formats come from the server, in the user's locale, so the browser only shows it.
 */
public final class DashboardCharts {

    /**
     * The field of a row that holds the row's tooltip, in HTML whose user-entered parts are escaped.
     */
    public static final String TOOLTIP = "tooltip";

    /**
     * The widest a bar gets, in pixels, so a wide card does not draw slabs.
     */
    private static final String BAR_MAX_WIDTH = "40";

    /**
     * The widest a category's label gets on a vertical bar chart, in pixels.
     */
    static final int CATEGORY_LABEL_WIDTH = 72;

    /**
     * The gap between the bars of neighboring categories, as a share of a category's room: ECharts' default.
     */
    static final String BAR_CATEGORY_GAP = "20%";

    // The options are parsed in the browser into a function: arguments up to the first ")", the body from the first
    // "{" to the last "}"
    private static final String AXIS_TOOLTIP_FORMATTER = "function (params) { return params[0].data.tooltip; }";
    private static final String ITEM_TOOLTIP_FORMATTER = "function (params) { return params.data.tooltip; }";

    private DashboardCharts() {
    }

    /**
     * Sets up a bar chart: transparent over the card, a category axis, an hours axis, and a tooltip on the whole
     * column under the pointer. Series are added by the caller.
     *
     * @param chart          the chart
     * @param hoursAxisLabel the label of a value on the hours axis, an ECharts template such as {@code {value}h}
     */
    public static void setUpBarChart(Chart chart, String hoursAxisLabel) {
        setUpChart(chart);
        chart.withXAxis(categoryAxis(new XAxis(), CATEGORY_LABEL_WIDTH))
                .withYAxis(hoursAxis(new YAxis(), hoursAxisLabel));
    }

    /**
     * Sets up a horizontal bar chart: as {@link #setUpBarChart}, with the categories on the y axis, the first at the
     * top, and the hours on the x axis. Its series are made with {@link #horizontalBar} and
     * {@link #horizontalPlanBar}.
     *
     * @param chart              the chart
     * @param hoursAxisLabel     the label of a value on the hours axis, an ECharts template such as {@code {value}h}
     * @param categoryLabelWidth the widest a category's label gets, in pixels, before it is cut with an ellipsis
     */
    public static void setUpHorizontalBarChart(Chart chart, String hoursAxisLabel, int categoryLabelWidth) {
        setUpChart(chart);
        YAxis categoryAxis = categoryAxis(new YAxis(), categoryLabelWidth);
        categoryAxis.setInverse(true);
        chart.withXAxis(hoursAxis(new XAxis(), hoursAxisLabel))
                .withYAxis(categoryAxis);
    }

    /**
     * Sets up a donut chart: transparent over the card, with a tooltip on the slice under the pointer.
     *
     * @param chart the chart
     */
    public static void setUpDonutChart(Chart chart) {
        chart.withBackgroundColor(DashboardColors.TRANSPARENT)
                .withTooltip(new Tooltip()
                        .withTrigger(AbstractTooltip.Trigger.ITEM)
                        .withConfine(true)
                        .withFormatterFunction(ITEM_TOOLTIP_FORMATTER));
        chart.setNativeJson(nativeJson(JsonNodeFactory.instance.objectNode()));
    }

    /**
     * The class of a chart's tooltip, which the stylesheet keeps out of the page's width.
     */
    static final String TOOLTIP_CLASS = "dashboard-chart-tooltip";

    /**
     * Returns the chart's native options: the given ones, such as media queries, and the tooltip drawn on the page's
     * body rather than in the chart, so the chart clips what it draws to its box, and a tooltip wider than the chart,
     * as the donut's, still shows whole; the tooltip has the class {@link #TOOLTIP_CLASS}. The Charts add-on has no
     * API for where the tooltip goes or its class, so they go in as native JSON; a chart takes one native JSON, so
     * every chart of the dashboard sets its own through this.
     *
     * @param root the chart's other native options, or an empty object
     * @return the native JSON
     */
    static String nativeJson(ObjectNode root) {
        root.putObject("tooltip")
                .put("appendTo", "body")
                .put("className", TOOLTIP_CLASS);
        return root.toString();
    }

    /**
     * Creates a bar of a value of the rows, as wide as the other bars of the chart.
     *
     * @param id            the series' id
     * @param categoryField the field of a row that names its category
     * @param valueField    the field of a row that holds the bar's height, in hours
     * @return the series
     */
    public static BarSeries bar(String id, String categoryField, String valueField) {
        return bar(id, new Encode().withX(categoryField).withY(valueField));
    }

    /**
     * Creates a bar of a value of the rows in a horizontal bar chart, as thick as the other bars of the chart.
     *
     * @param id            the series' id
     * @param categoryField the field of a row that names its category
     * @param valueField    the field of a row that holds the bar's length, in hours
     * @return the series
     */
    public static BarSeries horizontalBar(String id, String categoryField, String valueField) {
        return bar(id, new Encode().withX(valueField).withY(categoryField));
    }

    /**
     * Creates the plan's mark: a dashed outline as tall as the plan, drawn over the bars of time spent, which it
     * overlaps entirely. It is the last bar series of its chart, as ECharts takes the overlap from the last one.
     *
     * @param categoryField the field of a row that names its category
     * @param planField     the field of a row that holds the plan, in hours
     * @return the series
     */
    public static BarSeries planBar(String categoryField, String planField) {
        return asPlan(bar(planField, categoryField, planField));
    }

    /**
     * Creates the plan's mark of a horizontal bar chart, as {@link #planBar} does for a vertical one.
     *
     * @param categoryField the field of a row that names its category
     * @param planField     the field of a row that holds the plan, in hours
     * @return the series
     */
    public static BarSeries horizontalPlanBar(String categoryField, String planField) {
        return asPlan(horizontalBar(planField, categoryField, planField));
    }

    /**
     * Creates the style of a bar that marks time off, such as a day off or a week of absence: hatched in the color
     * of a day off.
     *
     * @return the style
     */
    public static ItemStyleWithDecal offDayStyle() {
        return new ItemStyleWithDecal()
                .withColor(DashboardColors.OFF_DAY)
                .withDecal(new Decal()
                        .withSymbol("rect")
                        .withColor(DashboardColors.OFF_DAY_HATCH)
                        .withDashArrayX(1, 0)
                        .withDashArrayY(2, 4)
                        .withRotation(-Math.PI / 4));
    }

    /**
     * Returns the time in hours, the unit of the hours axis.
     */
    public static double hours(WorkTime time) {
        return time.toMinutes() / 60.0;
    }

    /**
     * Escapes a text for the HTML of a tooltip: only the characters that HTML gives a meaning, so other characters,
     * such as a dash or a minus sign, stay as they are.
     */
    public static String escape(String text) {
        return HtmlUtils.htmlEscape(text, StandardCharsets.UTF_8.name());
    }

    private static void setUpChart(Chart chart) {
        chart.withBackgroundColor(DashboardColors.TRANSPARENT)
                .withGrid(new Grid()
                        .withLeft("8")
                        .withRight("8")
                        .withTop("12")
                        .withBottom("8")
                        // The axis labels fit inside the grid's box, as ECharts' former containLabel did
                        .withOuterBoundsMode(Grid.OuterBoundsMode.SAME)
                        .withOuterBoundsContain(Grid.OuterBoundsContain.AXIS_LABEL))
                .withTooltip(new Tooltip()
                        .withTrigger(AbstractTooltip.Trigger.AXIS)
                        .withAxisPointer(new AbstractTooltip.AxisPointer()
                                .withType(AbstractTooltip.AxisPointer.IndicatorType.SHADOW))
                        .withConfine(true)
                        .withFormatterFunction(AXIS_TOOLTIP_FORMATTER));
        chart.setNativeJson(nativeJson(JsonNodeFactory.instance.objectNode()));
    }

    private static <A extends AbstractCartesianAxis<A>> A categoryAxis(A axis, int labelWidth) {
        AxisLabel label = new AxisLabel()
                // Every category is labeled, however narrow the chart; a long label is cut with an ellipsis
                .withInterval(0)
                .withHideOverlap(false);
        label.setOverflow(Overflow.TRUNCATE);
        label.setWidth(labelWidth);

        return axis.withType(AxisType.CATEGORY)
                .withAxisLabel(label);
    }

    private static <A extends AbstractCartesianAxis<A>> A hoursAxis(A axis, String hoursAxisLabel) {
        return axis.withType(AxisType.VALUE)
                .withMinInterval(1)
                .withAxisLabel(new AxisLabel().withFormatter(hoursAxisLabel));
    }

    private static BarSeries bar(String id, Encode encode) {
        return new BarSeries()
                .withId(id)
                .withEncode(encode)
                .withBarMaxWidth(BAR_MAX_WIDTH);
    }

    private static BarSeries asPlan(BarSeries bar) {
        ItemStyleWithDecal style = new ItemStyleWithDecal().withColor(DashboardColors.TRANSPARENT);
        style.setBorderColor(DashboardColors.PLAN);
        style.setBorderWidth(1);
        style.setBorderType("dashed");

        return bar.withItemStyle(style)
                .withBarGap("-100%");
    }
}

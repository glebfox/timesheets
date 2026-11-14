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

import com.glebfox.timesheets.app.DashboardSupport;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.DaySummary;
import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.app.util.MyWeek;
import com.glebfox.timesheets.app.util.WeekSummary;
import com.vaadin.flow.component.html.Span;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.event.ChartClickEvent;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.shared.RichStyle;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The This week block of the dashboard: the week's time spent against its plan, and a bar per day of the time
 * spent, with the day's plan marked and a day off hatched and named. A day's label puts the weekday over the day of
 * the month, and on a chart narrower than {@link #NARROW_WIDTH} pixels the labels and the bars narrow so neighboring
 * days keep apart. A click on a day opens the calendar on it.
 */
@FragmentDescriptor("this-week-block.xml")
public class ThisWeekBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * The field of a row that labels the day: its weekday and day of the month, and why it is off, if it is.
     */
    public static final String DAY = "day";
    /**
     * The field of a row that holds the time spent on the day, in hours.
     */
    public static final String SPENT = "spent";
    /**
     * The field of a row that holds the day's plan, in hours.
     */
    public static final String PLAN = "plan";
    /**
     * The field of a row that holds the height of the hatching of a day off, in hours, or zero for a day that is
     * not off.
     */
    public static final String OFF = "off";
    /**
     * The field of a row that holds its tooltip.
     */
    public static final String TOOLTIP = DashboardCharts.TOOLTIP;

    /**
     * The width of the chart, in pixels, below which a day's label and its bars take less of the day's column, so
     * the labels and the bars of neighboring days keep apart, as on a phone.
     */
    static final int NARROW_WIDTH = 600;

    /**
     * The widest a day's label gets on a narrow chart, in pixels, before a line of it is cut with an ellipsis.
     */
    private static final int NARROW_LABEL_WIDTH = 36;

    /**
     * The gap between the bars of neighboring days on a narrow chart, as a share of the day's column.
     */
    private static final String NARROW_BAR_GAP = "40%";

    /**
     * The rich text style of today's label, which stands out from the other days' by its weight: semibold, as
     * Aura's {@code --aura-font-weight-semibold}. It keeps the color of the other labels, ECharts' theme's, which
     * the Charts add-on switches with the application's theme; a color of its own would be one literal for both
     * themes.
     */
    public static final String TODAY_STYLE = "today";
    private static final String TODAY_FONT_WEIGHT = "600";

    @ViewComponent
    private Span spentTotal;
    @ViewComponent
    private Span planNote;
    @ViewComponent
    private Chart chart;

    @Autowired
    private DashboardSupport dashboardSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private CalendarOpener calendarOpener;
    @Autowired
    private Messages messages;
    @Autowired
    private DashboardDates dashboardDates;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private List<DaySummary> days = List.of();

    @Subscribe
    public void onReady(final ReadyEvent event) {
        DashboardCharts.setUpBarChart(chart, messages.getMessage(getClass(), "chart.hoursAxisLabel"));
        RichStyle todayStyle = new RichStyle();
        todayStyle.setFontWeight(TODAY_FONT_WEIGHT);
        Objects.requireNonNull(DashboardCharts.xAxis(chart).getAxisLabel()).addRichStyle(TODAY_STYLE, todayStyle);

        // A day off is hatched over the whole height of the chart, behind the time spent on it, if any
        chart.withSeries(
                DashboardCharts.bar(OFF, DAY, OFF).withItemStyle(DashboardCharts.offDayStyle()),
                DashboardCharts.bar(SPENT, DAY, SPENT),
                DashboardCharts.planBar(DAY, PLAN));

        chart.setNativeJson(narrowDaysJson());
    }

    @Override
    public void refresh() {
        LocalDate today = DashboardBlock.today(timeSource, currentAuthentication);
        MyWeek week = dashboardSupport.loadMyWeek(today, componentSupport.getDaysOfWeek().getFirst());
        days = week.days();

        WeekSummary summary = week.summary();
        FactAndPlan total = summary.total();
        spentTotal.setText(componentSupport.formatWorkTimeTotal(total.fact()));
        planNote.setText(messages.formatMessage(getClass(), "thisWeekBlock.planNote",
                componentSupport.formatWorkTimeTotal(total.plan()),
                componentSupport.formatWeekStatusNote(summary.getStatus(today), total)));

        // The axis ends a step above the tallest bar, and a day off is hatched up to the axis' top, so it reads as
        // the whole day
        double largest = days.stream()
                .mapToDouble(day -> Math.max(DashboardCharts.hours(day.total().fact()),
                        DashboardCharts.hours(day.total().plan())))
                .max()
                .orElse(0);
        double offHeight = Math.max(1, DashboardCharts.fitHoursAxis(DashboardCharts.yAxis(chart), largest));
        List<MapDataItem> items = days.stream()
                .map(day -> new MapDataItem()
                        .add(DAY, formatDayLabel(day, today))
                        .add(SPENT, DashboardCharts.hours(day.total().fact()))
                        .add(PLAN, DashboardCharts.hours(day.total().plan()))
                        .add(OFF, day.offReason() != null ? offHeight : 0.0)
                        .add(TOOLTIP, formatTooltip(day)))
                .toList();
        chart.setDataSet(new DataSet().withSource(new DataSet.Source<MapDataItem>()
                .withDataProvider(new ListChartItems<>(items))
                .withCategoryField(DAY)
                .withValueFields(SPENT, PLAN, OFF, TOOLTIP)));
    }

    /**
     * Opens the calendar on the month of the day, if the current user may open the calendar.
     *
     * @param date the day
     */
    public void openDay(LocalDate date) {
        calendarOpener.open(this, date);
    }

    @Subscribe("chart")
    public void onChartClick(final ChartClickEvent event) {
        Integer index = event.getDetail().getDataIndex();
        if (index != null && index >= 0 && index < days.size()) {
            openDay(days.get(index).date());
        }
    }

    private String formatDayLabel(DaySummary day, LocalDate today) {
        String weekday = componentSupport.formatDayOfWeekShort(day.date().getDayOfWeek());
        String offReason = day.offReason();
        String label = offReason == null
                ? messages.formatMessage(getClass(), "thisWeekBlock.dayLabel", weekday, day.date().getDayOfMonth())
                : messages.formatMessage(getClass(), "thisWeekBlock.offDayLabel", weekday,
                        day.date().getDayOfMonth(), offReason);
        // ECharts' rich text styles a line at a time
        return day.date().equals(today)
                ? Arrays.stream(label.split("\n", -1))
                        .map(line -> "{" + TODAY_STYLE + "|" + line + "}")
                        .collect(Collectors.joining("\n"))
                : label;
    }

    /**
     * Returns the options that narrow the days' labels and bars on a narrow chart: an ECharts media query on the
     * chart's width, with the shared options as the default, so a chart that widens again puts them back. The Charts
     * add-on has no API for media queries, so they go in as native JSON.
     */
    private static String narrowDaysJson() {
        JsonNodeFactory json = JsonNodeFactory.instance;

        ObjectNode wide = json.objectNode();
        ObjectNode wideOption = wide.putObject("option");
        wideOption.putArray("xAxis").addObject().putObject("axisLabel")
                .put("width", DashboardCharts.CATEGORY_LABEL_WIDTH);
        addBarCategoryGap(wideOption.putArray("series"), DashboardCharts.BAR_CATEGORY_GAP);

        ObjectNode narrow = json.objectNode();
        narrow.putObject("query").put("maxWidth", NARROW_WIDTH - 1);
        ObjectNode narrowOption = narrow.putObject("option");
        narrowOption.putArray("xAxis").addObject().putObject("axisLabel")
                .put("width", NARROW_LABEL_WIDTH);
        addBarCategoryGap(narrowOption.putArray("series"), NARROW_BAR_GAP);

        ObjectNode root = json.objectNode();
        ArrayNode media = root.putArray("media");
        media.add(wide);
        media.add(narrow);
        return DashboardCharts.nativeJson(root);
    }

    // The gap between the days is the last bar series' for the whole axis, as the bars overlap, and is set on each,
    // matched by id, so no series keeps another
    private static void addBarCategoryGap(ArrayNode series, String gap) {
        for (String id : List.of(OFF, SPENT, PLAN)) {
            series.addObject().put("id", id).put("barCategoryGap", gap);
        }
    }

    private String formatTooltip(DaySummary day) {
        StringBuilder tooltip = new StringBuilder()
                .append("<b>")
                .append(DashboardCharts.escape(messages.formatMessage(getClass(), "chart.tooltip.day",
                        componentSupport.formatDayOfWeekShort(day.date().getDayOfWeek()),
                        dashboardDates.formatDate(day.date()))))
                .append("</b><br>")
                .append(DashboardCharts.escape(messages.formatMessage(getClass(), "chart.tooltip.spent",
                        componentSupport.formatWorkTimeTotal(day.total().fact()))))
                .append("<br>")
                .append(DashboardCharts.escape(messages.formatMessage(getClass(), "chart.tooltip.planned",
                        componentSupport.formatWorkTimeTotal(day.total().plan()))));
        String offReason = day.offReason();
        if (offReason != null) {
            tooltip.append("<br>").append(DashboardCharts.escape(offReason));
        }
        return tooltip.toString();
    }
}

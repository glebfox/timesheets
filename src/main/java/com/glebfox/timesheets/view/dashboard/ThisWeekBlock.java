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
import io.jmix.chartsflowui.kit.component.model.series.ItemStyleWithDecal;
import io.jmix.chartsflowui.kit.component.model.shared.Decal;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

/**
 * The This week block of the dashboard: the week's time spent against its plan, and a bar per day of the time
 * spent, with the day's plan marked and a day off hatched and named. A click on a day opens the calendar on it.
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
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private List<DaySummary> days = List.of();

    @Subscribe
    public void onReady(final ReadyEvent event) {
        DashboardCharts.setUpBarChart(chart, messages.getMessage(getClass(), "chart.hoursAxisLabel"));

        // A day off is hatched over the whole height of the chart, behind the time spent on it, if any
        ItemStyleWithDecal offStyle = new ItemStyleWithDecal()
                .withColor(DashboardColors.OFF_DAY)
                .withDecal(new Decal()
                        .withSymbol("rect")
                        .withColor(DashboardColors.OFF_DAY_HATCH)
                        .withDashArrayX(1, 0)
                        .withDashArrayY(2, 4)
                        .withRotation(-Math.PI / 4));
        chart.withSeries(
                DashboardCharts.bar(OFF, DAY, OFF).withItemStyle(offStyle),
                DashboardCharts.bar(SPENT, DAY, SPENT),
                DashboardCharts.planBar(DAY, PLAN));
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

        // A day off is hatched up to the tallest bar, so it reads as the whole day
        double offHeight = Math.max(1, days.stream()
                .mapToDouble(day -> Math.max(DashboardCharts.hours(day.total().fact()),
                        DashboardCharts.hours(day.total().plan())))
                .max()
                .orElse(0));
        List<MapDataItem> items = days.stream()
                .map(day -> new MapDataItem()
                        .add(DAY, formatDayLabel(day))
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

    private String formatDayLabel(DaySummary day) {
        String weekday = componentSupport.formatDayOfWeekShort(day.date().getDayOfWeek());
        String offReason = day.offReason();
        return offReason == null
                ? messages.formatMessage(getClass(), "thisWeekBlock.dayLabel", weekday, day.date().getDayOfMonth())
                : messages.formatMessage(getClass(), "thisWeekBlock.offDayLabel", weekday,
                        day.date().getDayOfMonth(), offReason);
    }

    private String formatTooltip(DaySummary day) {
        StringBuilder tooltip = new StringBuilder()
                .append("<b>")
                .append(DashboardCharts.escape(messages.formatMessage(getClass(), "chart.tooltip.day",
                        componentSupport.formatDayOfWeekShort(day.date().getDayOfWeek()),
                        datatypeFormatter.formatLocalDate(day.date()))))
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

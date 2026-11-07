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
import com.glebfox.timesheets.app.util.DateRangeFormatter;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.app.util.WeekStatus;
import com.glebfox.timesheets.app.util.WeekSummary;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.event.ChartClickEvent;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.series.ItemStyleWithDecal;
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

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The Last 8 weeks block of the dashboard: a bar per week, the current one and the ones before it, of the time
 * spent, colored by the week's status as the calendar colors it, with the week's plan marked. A click on a week
 * opens the calendar on it.
 * <p>
 * A series has one color, so the chart has a stacked series per status: each row holds its week's time in the
 * field of its status, named after the status, and zero in the others.
 */
@FragmentDescriptor("recent-weeks-block.xml")
public class RecentWeeksBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * The field of a row that labels the week: its number in the year.
     */
    public static final String WEEK = "week";
    /**
     * The field of a row that holds the week's plan, in hours.
     */
    public static final String PLAN = "plan";
    /**
     * The field of a row that holds its tooltip.
     */
    public static final String TOOLTIP = DashboardCharts.TOOLTIP;

    private static final String SPENT_STACK = "spent";

    @ViewComponent
    private Chart chart;

    @Autowired
    private DashboardSupport dashboardSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private DateRangeFormatter dateRangeFormatter;
    @Autowired
    private CalendarOpener calendarOpener;
    @Autowired
    private Messages messages;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private List<WeekSummary> weeks = List.of();

    @Subscribe
    public void onReady(final ReadyEvent event) {
        Locale locale = currentAuthentication.getLocale();
        getContent().setTitle(messages.formatMessage(getClass(),
                PluralForms.key("recentWeeksBlock.title", DashboardSupport.RECENT_WEEKS, locale), locale,
                DashboardSupport.RECENT_WEEKS));

        DashboardCharts.setUpBarChart(chart, messages.getMessage(getClass(), "chart.hoursAxisLabel"));
        for (WeekStatus status : WeekStatus.values()) {
            chart.addSeries(DashboardCharts.bar(status.name(), WEEK, status.name())
                    .withStack(SPENT_STACK)
                    .withItemStyle(new ItemStyleWithDecal().withColor(DashboardColors.of(status))));
        }
        chart.addSeries(DashboardCharts.planBar(WEEK, PLAN));
    }

    @Override
    public void refresh() {
        LocalDate today = DashboardBlock.today(timeSource, currentAuthentication);
        weeks = dashboardSupport.loadRecentWeeks(today, componentSupport.getDaysOfWeek().getFirst(),
                DashboardSupport.RECENT_WEEKS);
        WeekFields weekFields = WeekFields.of(currentAuthentication.getLocale());

        List<MapDataItem> items = weeks.stream()
                .map(week -> {
                    WeekStatus status = week.getStatus(today);
                    MapDataItem item = new MapDataItem()
                            .add(WEEK, String.valueOf(week.firstDay().get(weekFields.weekOfWeekBasedYear())))
                            .add(PLAN, DashboardCharts.hours(week.total().plan()))
                            .add(TOOLTIP, formatTooltip(week, status));
                    for (WeekStatus column : WeekStatus.values()) {
                        item.add(column.name(), column == status ? DashboardCharts.hours(week.total().fact()) : 0.0);
                    }
                    return item;
                })
                .toList();

        List<String> valueFields = new ArrayList<>(Arrays.stream(WeekStatus.values()).map(Enum::name).toList());
        valueFields.add(PLAN);
        valueFields.add(TOOLTIP);

        chart.setDataSet(new DataSet().withSource(new DataSet.Source<MapDataItem>()
                .withDataProvider(new ListChartItems<>(items))
                .withCategoryField(WEEK)
                .withValueFields(valueFields.toArray(String[]::new))));
    }

    /**
     * Opens the calendar on the month of the week's first day, if the current user may open the calendar.
     *
     * @param firstDay the week's first day
     */
    public void openWeek(LocalDate firstDay) {
        calendarOpener.open(this, firstDay);
    }

    @Subscribe("chart")
    public void onChartClick(final ChartClickEvent event) {
        Integer index = event.getDetail().getDataIndex();
        if (index != null && index >= 0 && index < weeks.size()) {
            openWeek(weeks.get(index).firstDay());
        }
    }

    private String formatTooltip(WeekSummary week, WeekStatus status) {
        return "<b>" + DashboardCharts.escape(dateRangeFormatter.formatDates(week.firstDay(), week.lastDay()))
                + "</b><br>"
                + DashboardCharts.escape(messages.formatMessage(getClass(), "chart.tooltip.spent",
                componentSupport.formatWorkTimeTotal(week.total().fact())))
                + "<br>"
                + DashboardCharts.escape(messages.formatMessage(getClass(), "chart.tooltip.planned",
                componentSupport.formatWorkTimeTotal(week.total().plan())))
                + "<br>"
                + DashboardCharts.escape(componentSupport.formatWeekStatusNote(status, week.total()));
    }
}

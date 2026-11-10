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
import com.glebfox.timesheets.app.util.ProjectTime;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.glebfox.timesheets.datatype.WorkTime;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.series.Encode;
import io.jmix.chartsflowui.kit.component.model.series.Label;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.shared.Color;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The Time by project block of the dashboard: the current month's time spent by project, as a donut with the total
 * in the middle and a list of the projects with their time beside it, the largest first. Beyond
 * {@link #MAX_PROJECTS} projects, the smallest are grouped as Other. Nothing spent this month shows an empty state.
 */
@FragmentDescriptor("time-by-project-block.xml")
public class TimeByProjectBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * The field of a row that names its project, or Other.
     */
    public static final String PROJECT = "project";
    /**
     * The field of a row that holds the time spent on its project, in hours.
     */
    public static final String HOURS = "hours";
    /**
     * The field of a row that holds its tooltip.
     */
    public static final String TOOLTIP = DashboardCharts.TOOLTIP;

    /**
     * How many projects the block shows each on its own; with more, the largest but one are shown, and the rest as
     * Other, so the donut never has more slices than this.
     */
    static final int MAX_PROJECTS = 6;

    /**
     * The palette of the slices: the Charts add-on's default, ECharts 6's nine colors, the same in the light and the
     * dark theme. It is set on the chart explicitly so the list's color marks match the slices.
     */
    private static final List<Color> PALETTE = List.of(
            new Color("#5070dd"), new Color("#b6d634"), new Color("#505372"),
            new Color("#ff994d"), new Color("#0ca8df"), new Color("#ffd10a"),
            new Color("#fb628b"), new Color("#785db0"), new Color("#3fbe95"));

    @ViewComponent
    private Div projects;
    @ViewComponent
    private Chart chart;
    @ViewComponent
    private Span total;
    @ViewComponent
    private Div projectList;
    @ViewComponent
    private GridEmptyState emptyState;

    @Autowired
    private DashboardSupport dashboardSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Messages messages;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        DashboardCharts.setUpDonutChart(chart);
        chart.setColorPalette(PALETTE.toArray(Color[]::new));

        // The list beside the donut names the slices, so the slices carry no labels
        PieSeries series = new PieSeries()
                .withId("projects")
                .withRadius("62%", "92%")
                .withAvoidLabelOverlap(false)
                .withLabel(new Label().withShow(false));
        series.setEncode(new Encode().withValue(HOURS));
        chart.addSeries(series);
    }

    @Override
    public void refresh() {
        YearMonth month = YearMonth.from(DashboardBlock.today(timeSource, currentAuthentication));
        getContent().setSubtitle(formatMonth(month));

        List<ProjectTime> projectTimes = dashboardSupport.loadTimeByProject(month).stream()
                .filter(projectTime -> projectTime.time().toMinutes() > 0)
                .toList();
        boolean empty = projectTimes.isEmpty();
        projects.setVisible(!empty);
        emptyState.setVisible(empty);
        if (empty) {
            projectList.removeAll();
            return;
        }

        WorkTime totalTime = projectTimes.stream()
                .map(ProjectTime::time)
                .reduce(WorkTime.ZERO, WorkTime::plus);
        total.setText(componentSupport.formatWorkTimeTotal(totalTime));

        List<Slice> slices = toSlices(projectTimes);
        List<MapDataItem> items = slices.stream()
                .map(slice -> new MapDataItem()
                        .add(PROJECT, slice.name())
                        .add(HOURS, DashboardCharts.hours(slice.time()))
                        .add(TOOLTIP, formatTooltip(slice, totalTime)))
                .toList();
        chart.setDataSet(new DataSet().withSource(new DataSet.Source<MapDataItem>()
                .withDataProvider(new ListChartItems<>(items))
                .withCategoryField(PROJECT)
                .withValueFields(HOURS, TOOLTIP)));

        projectList.removeAll();
        for (int i = 0; i < slices.size(); i++) {
            projectList.add(createRow(slices.get(i), PALETTE.get(i % PALETTE.size())));
        }
    }

    private List<Slice> toSlices(List<ProjectTime> projectTimes) {
        List<Slice> slices = new ArrayList<>();
        int shown = projectTimes.size() > MAX_PROJECTS ? MAX_PROJECTS - 1 : projectTimes.size();
        for (ProjectTime projectTime : projectTimes.subList(0, shown)) {
            slices.add(new Slice(Objects.toString(projectTime.project().getName(), ""), projectTime.time()));
        }
        if (shown < projectTimes.size()) {
            WorkTime otherTime = projectTimes.subList(shown, projectTimes.size()).stream()
                    .map(ProjectTime::time)
                    .reduce(WorkTime.ZERO, WorkTime::plus);
            slices.add(new Slice(messages.getMessage(getClass(), "timeByProjectBlock.other"), otherTime));
        }
        return slices;
    }

    private Div createRow(Slice slice, Color color) {
        Span mark = uiComponents.create(Span.class);
        mark.addClassName("mark");
        // The slice's color, from the chart's palette
        mark.getStyle().set("background-color", color.getValue());

        Span name = uiComponents.create(Span.class);
        name.addClassName("name");
        name.setText(slice.name());
        name.setTitle(slice.name());

        Span time = uiComponents.create(Span.class);
        time.addClassName("time");
        time.setText(componentSupport.formatWorkTimeTotal(slice.time()));

        Div row = uiComponents.create(Div.class);
        row.addClassName("dashboard-project-row");
        row.add(mark, name, time);
        return row;
    }

    private String formatTooltip(Slice slice, WorkTime totalTime) {
        long percent = Math.round(slice.time().toMinutes() * 100.0 / totalTime.toMinutes());
        return "<b>" + DashboardCharts.escape(slice.name()) + "</b><br>"
                + DashboardCharts.escape(messages.formatMessage(getClass(), "timeByProjectBlock.tooltip",
                componentSupport.formatWorkTimeTotal(slice.time()), percent));
    }

    private String formatMonth(YearMonth month) {
        Locale locale = currentAuthentication.getLocale();
        // The month is always the current one, so it goes without its year, as every date on the dashboard does
        return StringUtils.capitalize(DateTimeFormatter.ofPattern("LLLL", locale).format(month));
    }

    /**
     * A slice of the donut: a project, or Other, and its time.
     */
    private record Slice(String name, WorkTime time) {
    }
}

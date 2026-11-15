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

import com.glebfox.timesheets.app.TeamDashboardSupport;
import com.glebfox.timesheets.app.UserTimeZoneSupport;
import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PersonWeek;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.component.approval.ApprovalRosterCard;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.view.approval.ApprovalOpening;
import com.glebfox.timesheets.view.approval.ApprovalRosterScope;
import com.vaadin.flow.component.html.Div;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.event.ChartClickEvent;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.ItemStyleWithDecal;
import io.jmix.chartsflowui.kit.component.model.series.Label;
import io.jmix.chartsflowui.kit.component.model.shared.Decal;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Team this week block of the dashboard: a horizontal bar per person of the current user's team, in alphabetical
 * order, of the time they spent this week. The time on the projects where the user approves them is split by status,
 * and the time on any other project is one more segment; the person's plan is marked, and a week the person is
 * absent whole is hatched and named. A click on a person with entries to approve opens the approval screen on the
 * week, with them selected. A team of nobody hides the block.
 * <p>
 * The names stand beside the bars; on a chart narrower than {@link #NARROW_WIDTH} pixels, they stand above them.
 * The chart grows taller with the team: its stylesheet gives each person a row, by the number of people the block
 * sets in the custom property {@link #TEAM_SIZE_PROPERTY}.
 */
@FragmentDescriptor("team-week-block.xml")
public class TeamWeekBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * The field of a row that names the person, as the y axis labels them.
     */
    public static final String PERSON = "person";
    /**
     * The field of a row that holds the time on other projects, in hours, and the id of its series. The time on
     * the approved projects is in a field per status, named after the status.
     */
    public static final String OTHER = "other";
    /**
     * The field of a row that holds the person's plan, in hours.
     */
    public static final String PLAN = "plan";
    /**
     * The field of a row that holds the length of the hatching of a week of absence, in hours, or zero.
     */
    public static final String OFF = "off";
    /**
     * The field of a row that names the absence of a week of absence, drawn on its hatching, or is empty.
     */
    public static final String OFF_LABEL = "offLabel";
    /**
     * The field of a row that holds its tooltip.
     */
    public static final String TOOLTIP = DashboardCharts.TOOLTIP;

    /**
     * The order of the time spent along a bar: the statuses as the approval screen orders them, then other projects.
     */
    static final List<TimeEntryStatus> STATUS_ORDER = List.of(TimeEntryStatus.APPROVED, TimeEntryStatus.NEW,
            TimeEntryStatus.REJECTED, TimeEntryStatus.CLOSED);

    /**
     * The width of the chart, in pixels, below which the names stand above the bars. Kept in step with the
     * stylesheet's container query on the block's body, which gives each person a taller row there.
     */
    static final int NARROW_WIDTH = 600;

    /**
     * The custom property of the chart's area, the chart with its legend, that holds the number of people.
     */
    static final String TEAM_SIZE_PROPERTY = "--dashboard-team-size";

    private static final String SPENT_STACK = "spent";

    /**
     * The thickness of a bar: half the row it stands in, so the bars keep their proportion to the rows when a taller
     * block beside the chart stretches it, up to {@link #BAR_MAX_WIDTH} pixels.
     */
    private static final String BAR_WIDTH = "50%";

    /**
     * The thickest a bar gets, in pixels, when a stretched chart gives each person a tall row.
     */
    private static final int BAR_MAX_WIDTH = 28;

    /**
     * The thickest a bar gets, in pixels, on a narrow chart, where the names stand above the bars and clear them.
     */
    private static final int NARROW_BAR_MAX_WIDTH = 16;

    /**
     * The widest a name beside its bar gets, in pixels, and above it, on a narrow chart.
     */
    private static final int NAME_WIDTH = 128;
    private static final int NARROW_NAME_WIDTH = 280;

    @ViewComponent
    private Div chartArea;
    @ViewComponent
    private Chart chart;
    @ViewComponent
    private Div legend;

    @Autowired
    private TeamDashboardSupport teamDashboardSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private DashboardDates dashboardDates;
    @Autowired
    private ApprovalOpener approvalOpener;
    @Autowired
    private Messages messages;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private UserTimeZoneSupport userTimeZoneSupport;

    private List<PersonWeek> people = List.of();

    @Subscribe
    public void onReady(final ReadyEvent event) {
        DashboardCharts.setUpHorizontalBarChart(chart, messages.getMessage(getClass(), "chart.hoursAxisLabel"),
                NAME_WIDTH);

        // A week of absence is hatched along the whole axis, behind the time spent, if any, and named on it
        chart.addSeries(bar(OFF)
                .withItemStyle(DashboardCharts.offDayStyle())
                .withLabel(new Label()
                        .withShow(true)
                        .withPosition(Label.Position.PositionType.INSIDE_LEFT)
                        .withFormatter("{@" + OFF_LABEL + "}")
                        .withColor(DashboardColors.PLAN)));

        for (TimeEntryStatus status : STATUS_ORDER) {
            chart.addSeries(bar(status.name())
                    .withStack(SPENT_STACK)
                    .withItemStyle(new ItemStyleWithDecal().withColor(DashboardColors.of(status))));
        }
        chart.addSeries(bar(OTHER)
                .withStack(SPENT_STACK)
                .withItemStyle(new ItemStyleWithDecal()
                        .withColor(DashboardColors.OTHER_PROJECTS)
                        .withDecal(new Decal()
                                .withSymbol("rect")
                                .withColor(DashboardColors.OTHER_PROJECTS_STRIPE)
                                .withDashArrayX(1, 0)
                                .withDashArrayY(2, 3)
                                .withRotation(Math.PI / 4))));
        chart.addSeries(DashboardCharts.horizontalPlanBar(PERSON, PLAN)
                .withBarWidth(BAR_WIDTH)
                .withBarMaxWidth(String.valueOf(BAR_MAX_WIDTH)));

        chart.setNativeJson(narrowNamesJson());
    }

    @Override
    public void refresh() {
        LocalDate weekStart = ApprovalPeriod.weekStartOf(userTimeZoneSupport.today(), firstDayOfWeek());
        people = teamDashboardSupport.loadTeamWeek(weekStart, weekStart.plusDays(6));
        setVisible(!people.isEmpty());
        if (people.isEmpty()) {
            return;
        }

        // The axis ends a step beyond the longest bar, and a week of absence is hatched up to the axis' end, so it
        // reads as the whole week
        double longest = people.stream()
                .mapToDouble(person -> Math.max(DashboardCharts.hours(spent(person)),
                        DashboardCharts.hours(person.plan())))
                .max()
                .orElse(0);
        double offLength = DashboardCharts.fitHoursAxis(DashboardCharts.xAxis(chart), longest);

        Set<String> names = new HashSet<>();
        List<MapDataItem> items = people.stream()
                .map(person -> {
                    String offLabel = isWeekOff(person) ? absenceNames(person) : "";
                    MapDataItem item = new MapDataItem()
                            .add(PERSON, uniqueName(ApprovalRosterCard.nameOf(person.user()), names))
                            .add(OTHER, DashboardCharts.hours(person.otherProjects()))
                            .add(PLAN, DashboardCharts.hours(person.plan()))
                            .add(OFF, offLabel.isEmpty() ? 0.0 : offLength)
                            .add(OFF_LABEL, offLabel)
                            .add(TOOLTIP, formatTooltip(person));
                    for (TimeEntryStatus status : STATUS_ORDER) {
                        item.add(status.name(), DashboardCharts.hours(
                                person.approvable().getOrDefault(status, WorkTime.ZERO)));
                    }
                    return item;
                })
                .toList();

        List<String> valueFields = new ArrayList<>(STATUS_ORDER.stream().map(Enum::name).toList());
        valueFields.addAll(List.of(OTHER, PLAN, OFF, OFF_LABEL, TOOLTIP));
        chart.setDataSet(new DataSet().withSource(new DataSet.Source<MapDataItem>()
                .withDataProvider(new ListChartItems<>(items))
                .withCategoryField(PERSON)
                .withValueFields(valueFields.toArray(String[]::new))));

        // A one-off value from the data: the stylesheet sizes the chart by it
        chartArea.getStyle().set(TEAM_SIZE_PROPERTY, String.valueOf(people.size()));

        DashboardLegend.show(legend, legendItems(), uiComponents);
    }

    /**
     * Returns the legend's items: the statuses with time spent, in the order the bars stack them, other projects,
     * the plan, and a week of absence, each only if some person's bar shows it.
     */
    private List<DashboardLegend.Item> legendItems() {
        List<DashboardLegend.Item> items = new ArrayList<>();
        for (TimeEntryStatus status : STATUS_ORDER) {
            if (people.stream().anyMatch(person ->
                    person.approvable().getOrDefault(status, WorkTime.ZERO).toMinutes() > 0)) {
                items.add(DashboardLegend.bar(DashboardColors.of(status),
                        messages.getMessage(getClass(), "teamWeekBlock.legend." + status.name())));
            }
        }
        if (people.stream().anyMatch(person -> person.otherProjects().toMinutes() > 0)) {
            items.add(DashboardLegend.otherProjects(
                    messages.getMessage(getClass(), "teamWeekBlock.legend.otherProjects")));
        }
        if (people.stream().anyMatch(person -> person.plan().toMinutes() > 0)) {
            items.add(DashboardLegend.plan(messages.getMessage(getClass(), "chart.legend.plan")));
        }
        if (people.stream().anyMatch(TeamWeekBlock::isWeekOff)) {
            items.add(DashboardLegend.off(messages.getMessage(getClass(), "teamWeekBlock.legend.absence")));
        }
        return items;
    }

    /**
     * Opens the approval screen on this week, showing everyone, with the person selected, if the person has entries
     * to approve this week and the current user may open the approval screen; otherwise does nothing.
     *
     * @param week the person's week
     */
    public void openPerson(PersonWeek week) {
        if (!week.hasEntries()) {
            return;
        }

        LocalDate today = userTimeZoneSupport.today();
        approvalOpener.open(this, new ApprovalOpening(ApprovalMode.WEEK,
                ApprovalPeriod.weekOf(today, firstDayOfWeek()), ApprovalRosterScope.EVERYONE, week.user()));
    }

    @Subscribe("chart")
    public void onChartClick(final ChartClickEvent event) {
        Integer index = event.getDetail().getDataIndex();
        if (index != null && index >= 0 && index < people.size()) {
            openPerson(people.get(index));
        }
    }

    private static BarSeries bar(String field) {
        return DashboardCharts.horizontalBar(field, PERSON, field)
                .withBarWidth(BAR_WIDTH)
                .withBarMaxWidth(String.valueOf(BAR_MAX_WIDTH));
    }

    /**
     * Returns the options that move the names above the bars on a narrow chart: an ECharts media query on the
     * chart's width, with the names beside the bars as the default, so a chart that widens again puts them back.
     * Above the bars the names start where the bars do, so the axis' line and ticks, which would run through their
     * first letters, are hidden there, and the names, the bars, and the hours' first label start on the card's
     * content edge and the last label ends on the other: the grid takes the chart's whole width, with room below for
     * the hours' labels, and the end labels align to the ends instead of centering on them. The Charts add-on has no
     * API for media queries, so they go in as native JSON.
     */
    private static String narrowNamesJson() {
        JsonNodeFactory json = JsonNodeFactory.instance;

        ObjectNode wide = json.objectNode();
        ObjectNode wideAxis = wide.putObject("option").putArray("yAxis").addObject();
        wideAxis.putObject("axisLine").put("show", true);
        wideAxis.putObject("axisTick").put("show", true);
        wideAxis.putObject("axisLabel")
                .put("inside", false)
                .put("align", "right")
                .put("verticalAlign", "middle")
                .put("padding", 0)
                .put("margin", 8)
                .put("width", NAME_WIDTH);

        ObjectNode narrow = json.objectNode();
        narrow.putObject("query").put("maxWidth", NARROW_WIDTH - 1);
        ObjectNode narrowAxis = narrow.putObject("option").putArray("yAxis").addObject();
        narrowAxis.putObject("axisLine").put("show", false);
        narrowAxis.putObject("axisTick").put("show", false);
        ObjectNode narrowLabel = narrowAxis.putObject("axisLabel")
                .put("inside", true)
                .put("align", "left")
                .put("verticalAlign", "bottom")
                .put("margin", 0)
                .put("width", NARROW_NAME_WIDTH);
        // The name's bottom edge clears the bar, which is centered on the row and no thicker there, by a few pixels
        narrowLabel.putArray("padding").add(0).add(0).add(NARROW_BAR_MAX_WIDTH / 2 + 4).add(0);
        addBarMaxWidth(((ObjectNode) wide.get("option")).putArray("series"), BAR_MAX_WIDTH);
        addBarMaxWidth(((ObjectNode) narrow.get("option")).putArray("series"), NARROW_BAR_MAX_WIDTH);

        // The grid as every bar chart has it, sized around its labels
        ((ObjectNode) wide.get("option")).putArray("grid").addObject()
                .put("left", DashboardCharts.GRID_SIDE)
                .put("right", DashboardCharts.GRID_SIDE)
                .put("bottom", DashboardCharts.GRID_BOTTOM)
                .put("outerBoundsMode", "same");
        ObjectNode wideHoursLabel = ((ObjectNode) wide.get("option")).putArray("xAxis").addObject()
                .putObject("axisLabel");
        wideHoursLabel.putNull("alignMinLabel");
        wideHoursLabel.putNull("alignMaxLabel");

        ((ObjectNode) narrow.get("option")).putArray("grid").addObject()
                .put("left", 0)
                .put("right", 0)
                .put("bottom", DashboardCharts.GRID_BOTTOM + DashboardCharts.AXIS_LABEL_MARGIN
                        + DashboardCharts.AXIS_LABEL_FONT_SIZE)
                .put("outerBoundsMode", "none");
        ((ObjectNode) narrow.get("option")).putArray("xAxis").addObject().putObject("axisLabel")
                .put("alignMinLabel", "left")
                .put("alignMaxLabel", "right");

        ObjectNode root = json.objectNode();
        ArrayNode media = root.putArray("media");
        media.add(wide);
        media.add(narrow);
        return DashboardCharts.nativeJson(root);
    }

    /**
     * Sets the thickest a bar gets on every series of the chart, by its id.
     */
    private static void addBarMaxWidth(ArrayNode series, int maxWidth) {
        List<String> ids = new ArrayList<>(List.of(OFF));
        STATUS_ORDER.forEach(status -> ids.add(status.name()));
        ids.add(OTHER);
        ids.add(PLAN);
        ids.forEach(id -> series.addObject().put("id", id).put("barMaxWidth", maxWidth));
    }

    private DayOfWeek firstDayOfWeek() {
        return componentSupport.getDaysOfWeek().getFirst();
    }

    private static WorkTime spent(PersonWeek person) {
        return person.approvable().values().stream().reduce(person.otherProjects(), WorkTime::plus);
    }

    /**
     * Whether the person is absent the whole week: their absences leave nothing planned.
     */
    private static boolean isWeekOff(PersonWeek person) {
        return !person.absences().isEmpty() && person.plan().toMinutes() == 0;
    }

    private static String absenceNames(PersonWeek person) {
        return person.absences().stream()
                .map(WorkPlan.AbsencePeriod::typeName)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns the name, made different from the names already taken by zero-width spaces (U+200B), which do not show:
     * ECharts draws the rows of one category name on one bar.
     */
    private static String uniqueName(String name, Set<String> taken) {
        String unique = name;
        while (!taken.add(unique)) {
            // A zero-width space, written as an escape so the source shows it
            unique = unique + "\u200B";
        }
        return unique;
    }

    private String formatTooltip(PersonWeek person) {
        StringBuilder tooltip = new StringBuilder()
                .append("<b>")
                .append(DashboardCharts.escape(ApprovalRosterCard.nameOf(person.user())))
                .append("</b><br>")
                .append(DashboardCharts.escape(messages.formatMessage(getClass(), "chart.tooltip.spent",
                        componentSupport.formatWorkTimeTotal(spent(person)))));

        Map<TimeEntryStatus, WorkTime> approvable = person.approvable();
        for (TimeEntryStatus status : STATUS_ORDER) {
            WorkTime time = approvable.get(status);
            if (time != null) {
                tooltip.append("<br>").append(DashboardCharts.escape(messages.formatMessage(getClass(),
                        "teamWeekBlock.segment." + status.name(), componentSupport.formatWorkTimeTotal(time))));
            }
        }
        if (person.otherProjects().toMinutes() > 0) {
            tooltip.append("<br>").append(DashboardCharts.escape(messages.formatMessage(getClass(),
                    "teamWeekBlock.otherProjects", componentSupport.formatWorkTimeTotal(person.otherProjects()))));
        }

        tooltip.append("<br>").append(DashboardCharts.escape(messages.formatMessage(getClass(),
                "chart.tooltip.planned", componentSupport.formatWorkTimeTotal(person.plan()))));
        for (WorkPlan.AbsencePeriod absence : person.absences()) {
            tooltip.append("<br>").append(DashboardCharts.escape(messages.formatMessage(getClass(),
                    "teamWeekBlock.absence", absence.typeName(),
                    dashboardDates.formatDates(absence.firstDay(), absence.lastDay()))));
        }
        return tooltip.toString();
    }
}

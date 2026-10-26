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

package com.glebfox.timesheets.action;

import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDaysAddView;
import io.jmix.core.AccessManager;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponentProperties;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.AdjustWhenViewReadOnly;
import io.jmix.flowui.action.list.ListDataComponentAction;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.icon.Icons;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.KeyCombination;
import io.jmix.flowui.kit.icon.JmixFontIcon;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.StandardOutcome;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

/**
 * Adds days to the holiday calendar whose days a list component shows: opens {@link HolidayCalendarDaysAddView}
 * and adds one {@link HolidayCalendarDay} per date of the range it gives. The days are created in the data context
 * of the view that shows the list, so they are saved with the calendar.
 * <p>
 * Needs no selection, and checks the permission the standard create action checks.
 */
@ActionType(HolidayCalendarDaysAddAction.ID)
public class HolidayCalendarDaysAddAction
        extends ListDataComponentAction<HolidayCalendarDaysAddAction, HolidayCalendarDay>
        implements AdjustWhenViewReadOnly {

    public static final String ID = "list_holidayCalendarDays_add";

    private AccessManager accessManager;
    private DialogWindows dialogWindows;

    public HolidayCalendarDaysAddAction() {
        this(ID);
    }

    public HolidayCalendarDaysAddAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();

        this.variant = ActionVariant.PRIMARY;
    }

    // The look and the shortcut of the standard create action
    @Autowired
    public void setMessages(Messages messages) {
        this.text = messages.getMessage("actions.Create");
    }

    @Autowired
    public void setIcons(Icons icons) {
        this.icon = icons.get(JmixFontIcon.CREATE_ACTION);
    }

    @Autowired
    public void setUiComponentProperties(UiComponentProperties uiComponentProperties) {
        setShortcutCombination(KeyCombination.create(uiComponentProperties.getGridCreateShortcut()));
    }

    @Autowired
    public void setAccessManager(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    @Autowired
    public void setDialogWindows(DialogWindows dialogWindows) {
        this.dialogWindows = dialogWindows;
    }

    @Override
    protected boolean isApplicable() {
        return super.isApplicable()
                && target != null
                && target.getItems() instanceof EntityDataUnit;
    }

    @Override
    protected boolean isPermitted() {
        // The entity-level check of the standard create action
        return ListActionTargets.isEntityPermitted(target, accessManager, UiEntityContext::isCreatePermitted)
                && super.isPermitted();
    }

    @Override
    public void execute() {
        checkTarget();
        CollectionPropertyContainer<HolidayCalendarDay> daysContainer =
                ListActionTargets.getPropertyContainer(target, getClass());
        // The editor's data context, so the days are saved with the calendar and discarded with it
        DataContext dataContext = ListActionTargets.getDataContext(target);

        dialogWindows.view(ListActionTargets.getView(target), HolidayCalendarDaysAddView.class)
                .withViewConfigurer(view -> view.setCalendarDays(daysContainer::getItems))
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        addDays(closeEvent.getView(), daysContainer, dataContext);
                    }
                })
                .open();
    }

    private static void addDays(HolidayCalendarDaysAddView view,
                                CollectionPropertyContainer<HolidayCalendarDay> daysContainer,
                                DataContext dataContext) {
        HolidayCalendar calendar = (HolidayCalendar) daysContainer.getMaster().getItem();
        List<HolidayCalendarDay> days = view.getDates().stream()
                .map(date -> createDay(dataContext, calendar, date, view))
                .toList();
        // One change of the container for the whole range
        daysContainer.getMutableItems().addAll(days);
    }

    private static HolidayCalendarDay createDay(DataContext dataContext, HolidayCalendar calendar, LocalDate date,
                                                HolidayCalendarDaysAddView view) {
        HolidayCalendarDay day = dataContext.create(HolidayCalendarDay.class);
        day.setCalendar(calendar);
        day.setDate(date);
        day.setName(view.getDayName());
        day.setType(view.getDayType());
        return day;
    }
}

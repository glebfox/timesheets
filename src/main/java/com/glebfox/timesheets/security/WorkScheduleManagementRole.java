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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDaysAddView;
import com.glebfox.timesheets.view.workschedule.DefaultWorkScheduleDeleteView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleUsersAddView;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Managing work schedules, holiday calendars with their days, and who works to which schedule from which date.
 * <p>
 * Users are only read. Permission to update users would let the holder change or reset any user's password from the
 * user list, since those actions check nothing else. So the user editor opens read-only for the holder, and its
 * assignment actions still work in it.
 */
@ResourceRole(name = "Work schedule management", code = WorkScheduleManagementRole.CODE)
public interface WorkScheduleManagementRole extends UserReadOnlyRole {

    String CODE = "work-schedule-management";

    @EntityAttributePolicy(entityClass = WorkSchedule.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = WorkSchedule.class, actions = EntityPolicyAction.ALL)
    void workSchedule();

    @EntityAttributePolicy(entityClass = HolidayCalendar.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = HolidayCalendar.class, actions = EntityPolicyAction.ALL)
    void holidayCalendar();

    @EntityAttributePolicy(entityClass = HolidayCalendarDay.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = HolidayCalendarDay.class, actions = EntityPolicyAction.ALL)
    void holidayCalendarDay();

    @EntityAttributePolicy(entityClass = WorkScheduleAssignment.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = WorkScheduleAssignment.class, actions = EntityPolicyAction.ALL)
    void workScheduleAssignment();

    @ViewPolicy(viewIds = {"ts_WorkSchedule.list", "ts_WorkSchedule.detail", DefaultWorkScheduleDeleteView.ID})
    @MenuPolicy(menuIds = "ts_WorkSchedule.list")
    void workScheduleScreens();

    @ViewPolicy(viewIds = {"ts_HolidayCalendar.list", "ts_HolidayCalendar.detail", "ts_HolidayCalendarDay.detail",
            HolidayCalendarDaysAddView.ID})
    @MenuPolicy(menuIds = "ts_HolidayCalendar.list")
    void holidayCalendarScreens();

    @ViewPolicy(viewIds = {"ts_WorkScheduleAssignment.detail", WorkScheduleUsersAddView.ID})
    void workScheduleAssignmentScreens();
}

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

package com.glebfox.timesheets.view.absence;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.DateRangeFormatter;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Lists every user's absences, newest first, with filters by user, type, and a period.
 */
@Route(value = "absences", layout = MainView.class)
@ViewController("ts_Absence.list")
@ViewDescriptor("absence-list-view.xml")
@LookupComponent("absencesDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class AbsenceListView extends StandardListView<Absence> {

    @ViewComponent
    private DataGrid<Absence> absencesDataGrid;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private DateRangeFormatter dateRangeFormatter;

    @Subscribe(id = "absencesDc", target = Target.DATA_CONTAINER)
    public void onAbsencesDcCollectionChange(final CollectionContainer.CollectionChangeEvent<Absence> event) {
        // The browser sizes the auto-width columns to the rows it shows first, while a longer name or a Deleted
        // badge may come only with another page, a filter, a reload, or a save
        absencesDataGrid.recalculateColumnWidths();
    }

    @Supply(to = "absencesDataGrid.user", subject = "renderer")
    private Renderer<Absence> absencesDataGridUserRenderer() {
        return componentSupport.createUserRenderer(Absence::getUser);
    }

    @Supply(to = "absencesDataGrid.type", subject = "renderer")
    private Renderer<Absence> absencesDataGridTypeRenderer() {
        return new ComponentRenderer<>(absence -> {
            AbsenceType type = absence.getType();
            Span cell = new Span(metadataTools.format(type));
            cell.addClassName("text-with-badges");

            // An absence keeps its type after the type is deleted, and still plans nothing
            if (type.getDeletedDate() != null) {
                cell.add(componentSupport.createBadge(
                        messageBundle.getMessage("deletedBadge.text"), BadgeStyle.NEUTRAL));
            }
            return cell;
        });
    }

    @Supply(to = "absencesDataGrid.dates", subject = "renderer")
    private Renderer<Absence> absencesDataGridDatesRenderer() {
        return new TextRenderer<>(absence ->
                dateRangeFormatter.formatDates(absence.getFirstDay(), absence.getLastDay()));
    }

    @Supply(to = "absencesDataGrid.days", subject = "renderer")
    private Renderer<Absence> absencesDataGridDaysRenderer() {
        // Calendar days, weekends and holidays included
        return new TextRenderer<>(absence -> dateRangeFormatter.formatDays(
                dateRangeFormatter.countDays(absence.getFirstDay(), absence.getLastDay())));
    }
}

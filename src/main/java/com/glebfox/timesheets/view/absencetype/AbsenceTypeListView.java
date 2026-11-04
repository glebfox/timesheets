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

package com.glebfox.timesheets.view.absencetype;

import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "absence-types", layout = MainView.class)
@ViewController("ts_AbsenceType.list")
@ViewDescriptor("absence-type-list-view.xml")
@LookupComponent("absenceTypesDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class AbsenceTypeListView extends StandardListView<AbsenceType> {
}

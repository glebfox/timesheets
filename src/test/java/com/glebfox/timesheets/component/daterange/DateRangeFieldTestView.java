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

package com.glebfox.timesheets.component.daterange;

import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

/**
 * Loads a {@link DateRangeField} with every attribute of its XML element set, for {@link DateRangeFieldXmlLoadTest}, and
 * one bound to the days of an absence in a container without a loader, for {@link DateRangeFieldBindingUiTest}.
 */
@Route(value = "date-range-field-test", layout = MainView.class)
@ViewController("ts_DateRangeFieldTestView")
@ViewDescriptor("date-range-field-test-view.xml")
public class DateRangeFieldTestView extends StandardView {
}

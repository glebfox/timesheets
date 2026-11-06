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

package com.glebfox.timesheets.component.emptystate;

import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

/**
 * Three grids with a {@link GridEmptyState}, for {@link GridEmptyStateUiTest}: one that loads its own rows and whose
 * empty state takes its standard refresh action, one over a container without a loader and with no actions, whose
 * empty state has no action and a nested icon component, and a tree data grid that loads its own rows, which never
 * exist, and whose empty state takes its standard refresh action.
 */
@Route(value = "grid-empty-state-test", layout = MainView.class)
@ViewController("ts_GridEmptyStateTestView")
@ViewDescriptor("grid-empty-state-test-view.xml")
public class GridEmptyStateTestView extends StandardView {
}

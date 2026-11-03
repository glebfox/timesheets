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

package com.glebfox.timesheets.component.icon;

import com.vaadin.flow.component.dependency.JsModule;

/**
 * Brings the main menu's own icon set, {@code ts-menu}, into the frontend bundle. The menu names its icons
 * as {@code ts-menu:<id>}, so nothing refers to this class; its annotation is all it is for.
 */
@JsModule("./src/component/icon/ts-menu-icons.js")
public final class MenuIconset {

    private MenuIconset() {
    }
}

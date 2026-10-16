/*
 * Copyright 2023 Gleb Gorelov
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

package com.glebfox.timesheets.view.client;

import com.glebfox.timesheets.entity.client.Client;
import io.jmix.flowui.view.*;

@ViewController("ts_Client.lookup")
@ViewDescriptor("client-lookup-view.xml")
@LookupComponent("clientsDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class ClientLookupView extends StandardListView<Client> {
}
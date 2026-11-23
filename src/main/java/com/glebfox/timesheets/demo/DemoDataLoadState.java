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

package com.glebfox.timesheets.demo;

import com.vaadin.flow.shared.Registration;

import java.util.Optional;

/**
 * Whether this start is loading the demo data, for the screens that must wait for it. Only a
 * {@code dev} start has an implementation, so a bean of this type may be absent.
 */
public interface DemoDataLoadState {

    /**
     * Registers the listener to run once when the load under way ends, whether it succeeded or
     * not. Reading the state and registering are one atomic step, so a listener is either told
     * or never registered.
     *
     * @param listener runs once, on the thread that ran the load, when the load ends
     * @return the listener's registration, or empty, registering nothing, when no load is under way
     */
    Optional<Registration> whenLoadEnds(Runnable listener);
}

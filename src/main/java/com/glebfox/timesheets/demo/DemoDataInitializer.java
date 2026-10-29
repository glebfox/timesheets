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

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Loads the demo data on a {@code dev} start against a database without it.
 */
@Profile("dev")
@Component("ts_DemoDataInitializer")
public class DemoDataInitializer {

    private final DemoDataLoader demoDataLoader;

    public DemoDataInitializer(DemoDataLoader demoDataLoader) {
        this.demoDataLoader = demoDataLoader;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        demoDataLoader.loadIfEmpty();
    }
}

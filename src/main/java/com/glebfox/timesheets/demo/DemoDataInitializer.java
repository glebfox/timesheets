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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Loads the demo data on a {@code dev} start against a database without it, and tells the
 * screens waiting for it, such as the login page, when the load ends.
 */
@Profile("dev")
@Component("ts_DemoDataInitializer")
public class DemoDataInitializer implements DemoDataLoadState, SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final DemoDataLoader demoDataLoader;

    private final Object lock = new Object();
    private boolean loading;
    private final List<Runnable> listeners = new ArrayList<>();

    public DemoDataInitializer(DemoDataLoader demoDataLoader) {
        this.demoDataLoader = demoDataLoader;
    }

    /**
     * Decides whether this start loads the demo data. It runs after every singleton, Liquibase
     * included, and before the web server starts, which happens when the context finishes its
     * refresh: so the login page knows of the load from the first request. The load itself runs
     * on {@link ApplicationReadyEvent}, after the web server has started: in a measured start on
     * an empty database, its data was saved about 8 s after the web server's start, and the
     * listeners that run after its commit took about 10 s more.
     */
    @Override
    public void afterSingletonsInstantiated() {
        boolean loads = !demoDataLoader.hasData();
        synchronized (lock) {
            loading = loads;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        synchronized (lock) {
            if (!loading) {
                return;
            }
        }
        try {
            demoDataLoader.loadIfEmpty();
        } finally {
            // Only now, after the listeners that run after the load's commit: they tell the user of
            // each new time entry, and a session signed in before them would get all those events
            endLoad();
        }
    }

    @Override
    public Optional<Registration> whenLoadEnds(Runnable listener) {
        synchronized (lock) {
            if (!loading) {
                return Optional.empty();
            }
            listeners.add(listener);
        }
        return Optional.of(() -> {
            synchronized (lock) {
                listeners.remove(listener);
            }
        });
    }

    private void endLoad() {
        List<Runnable> toTell;
        synchronized (lock) {
            loading = false;
            toTell = List.copyOf(listeners);
            listeners.clear();
        }
        for (Runnable listener : toTell) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                // A page whose UI is gone must neither keep the others waiting nor fail the start,
                // which would stop the application
                log.warn("A listener of the demo data load failed", e);
            }
        }
    }
}

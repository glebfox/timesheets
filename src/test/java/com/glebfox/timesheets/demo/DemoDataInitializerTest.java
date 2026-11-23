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

import com.vaadin.flow.component.UIDetachedException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoDataInitializerTest {

    private final DemoDataLoader loader = mock(DemoDataLoader.class);
    private final DemoDataInitializer initializer = new DemoDataInitializer(loader);

    @Test
    void aLoadUnderWayTellsItsListenersOnceWhenItEnds() {
        startOnAnEmptyDatabase();
        AtomicInteger told = new AtomicInteger();
        AtomicInteger toldLater = new AtomicInteger();

        assertThat(initializer.whenLoadEnds(told::incrementAndGet)).isPresent();
        assertThat(told).hasValue(0);

        initializer.onApplicationReady();

        verify(loader).loadIfEmpty();
        assertThat(told).hasValue(1);
        assertThat(initializer.whenLoadEnds(toldLater::incrementAndGet)).isEmpty();
        assertThat(toldLater).hasValue(0);
    }

    @Test
    void aDatabaseWithDataLoadsNothingAndTellsNoOne() {
        when(loader.hasData()).thenReturn(true);
        initializer.afterSingletonsInstantiated();
        AtomicInteger told = new AtomicInteger();

        assertThat(initializer.whenLoadEnds(told::incrementAndGet)).isEmpty();

        initializer.onApplicationReady();

        verify(loader, never()).loadIfEmpty();
        assertThat(told).hasValue(0);
    }

    @Test
    void aFailedLoadStillTellsItsListeners() {
        startOnAnEmptyDatabase();
        RuntimeException failure = new IllegalStateException("The load failed");
        doThrow(failure).when(loader).loadIfEmpty();
        AtomicInteger told = new AtomicInteger();
        initializer.whenLoadEnds(told::incrementAndGet);

        assertThatThrownBy(initializer::onApplicationReady).isSameAs(failure);
        assertThat(told).hasValue(1);
    }

    @Test
    void aFailingListenerStopsNeitherTheOthersNorTheStart() {
        startOnAnEmptyDatabase();
        AtomicInteger told = new AtomicInteger();
        initializer.whenLoadEnds(() -> {
            throw new UIDetachedException();
        });
        initializer.whenLoadEnds(told::incrementAndGet);

        assertThatCode(initializer::onApplicationReady).doesNotThrowAnyException();
        assertThat(told).hasValue(1);
    }

    @Test
    void aRemovedListenerIsNeverTold() {
        startOnAnEmptyDatabase();
        AtomicInteger told = new AtomicInteger();

        initializer.whenLoadEnds(told::incrementAndGet).orElseThrow().remove();
        initializer.onApplicationReady();

        assertThat(told).hasValue(0);
    }

    @Test
    void aListenerRegisteredAfterTheEndIsNeverTold() {
        startOnAnEmptyDatabase();
        assertThat(initializer.whenLoadEnds(() -> {
        })).isPresent();
        AtomicInteger told = new AtomicInteger();

        initializer.onApplicationReady();

        assertThat(initializer.whenLoadEnds(told::incrementAndGet)).isEmpty();
        assertThat(told).hasValue(0);
    }

    private void startOnAnEmptyDatabase() {
        when(loader.hasData()).thenReturn(false);
        initializer.afterSingletonsInstantiated();
    }
}

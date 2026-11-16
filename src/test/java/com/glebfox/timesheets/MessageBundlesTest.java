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

package com.glebfox.timesheets;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain guard over the message bundles: every locale's bundle has the same keys.
 */
class MessageBundlesTest {

    static final Path RESOURCES = Path.of("src/main/resources/com/glebfox/timesheets");

    @Test
    void everyBundleHasTheSameKeys() throws Exception {
        Map<String, Set<String>> bundles = bundleKeys();
        assertThat(bundles).hasSizeGreaterThanOrEqualTo(2);

        Set<String> allKeys = new TreeSet<>();
        bundles.values().forEach(allKeys::addAll);
        Map<String, Set<String>> missing = new TreeMap<>();
        bundles.forEach((bundle, keys) -> {
            Set<String> lacking = new TreeSet<>(allKeys);
            lacking.removeAll(keys);
            missing.put(bundle, lacking);
        });
        assertThat(missing).allSatisfy((bundle, keys) ->
                assertThat(keys).as("keys missing from %s", bundle).isEmpty());
    }

    Map<String, Set<String>> bundleKeys() throws IOException {
        List<Path> files;
        try (Stream<Path> list = Files.list(RESOURCES)) {
            files = list.filter(p -> {
                String name = RESOURCES.relativize(p).toString();
                return name.startsWith("messages_") && name.endsWith(".properties");
            }).sorted().toList();
        }
        Map<String, Set<String>> bundles = new TreeMap<>();
        for (Path file : files) {
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            bundles.put(RESOURCES.relativize(file).toString(), new TreeSet<>(properties.stringPropertyNames()));
        }
        return bundles;
    }
}

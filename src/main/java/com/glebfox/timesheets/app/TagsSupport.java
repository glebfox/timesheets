/*
 * Copyright 2024 Gleb Gorelov
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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;


/**
 * Utility class for managing and manipulating {@link Tag} and {@link TagGroup} entities.
 */
@Component("ts_TagsSupport")
public class TagsSupport {

    /**
     * Merges a collection of lists of {@link Tag} instances into a single collection containing all tags.
     *
     * @param tags a collection of lists of tags to be merged
     * @return a collection containing all {@link Tag} instances from the provided lists
     */
    public Collection<Tag> mergeTags(Collection<List<Tag>> tags) {
        return mergeTags(tags.stream());
    }

    /**
     * Merges a list of {@link TagGroup} instances into a single collection containing all {@link Tag} entities
     * across the provided tag groups.
     *
     * @param tagGroups the list of {@link TagGroup} instances whose tags need to be merged
     * @return a collection containing all {@link Tag} entities from the provided tag groups
     */
    public Collection<Tag> mergeTags(List<TagGroup> tagGroups) {
        return mergeTags(tagGroups.stream()
                .map(TagGroup::getTags));
    }

    /**
     * Merges a stream of lists of {@link Tag} instances into a single collection containing all tags.
     *
     * @param tags a stream of lists of tags to be merged
     * @return a collection containing all {@link Tag} instances from the provided lists
     */
    public Collection<Tag> mergeTags(Stream<List<Tag>> tags) {
        return tags.reduce(new ArrayList<>(), (tags1, tags2) -> {
            tags1.addAll(tags2);
            return tags1;
        });
    }
}
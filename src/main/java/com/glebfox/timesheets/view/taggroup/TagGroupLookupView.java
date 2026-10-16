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

package com.glebfox.timesheets.view.taggroup;

import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.tag.TagGroup;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

@ViewController("ts_TagGroup.lookup")
@ViewDescriptor("tag-group-lookup-view.xml")
@LookupComponent("tagGroupsDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class TagGroupLookupView extends StandardListView<TagGroup> {

    @ViewComponent
    private CollectionLoader<TagGroup> tagGroupsDl;

    public void setProject(Project project) {
        if (project != null) {
            tagGroupsDl.setParameter("project", project);
        } else {
            tagGroupsDl.removeParameter("project");
        }
    }
}
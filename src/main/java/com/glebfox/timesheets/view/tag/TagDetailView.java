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

package com.glebfox.timesheets.view.tag;

import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.view.*;

@Route(value = "tags/:id", layout = MainView.class)
@ViewController("ts_Tag.detail")
@ViewDescriptor("tag-detail-view.xml")
@EditedEntityContainer("tagDc")
@DialogMode(width = "480px", closeOnEsc = true)
public class TagDetailView extends StandardDetailView<Tag> {

    @ViewComponent
    private EntityPicker<TagGroup> tagGroupField;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // in case we open a view for an entity creation by direct navigation
        tagGroupField.setVisible(getEditedEntity().getTagGroup() == null);
    }
}
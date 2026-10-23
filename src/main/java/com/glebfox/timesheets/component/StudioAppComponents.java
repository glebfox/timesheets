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

package com.glebfox.timesheets.component;

import com.glebfox.timesheets.component.avatarupload.AvatarUpload;
import io.jmix.flowui.kit.meta.StudioComponent;
import io.jmix.flowui.kit.meta.StudioPropertiesBinding;
import io.jmix.flowui.kit.meta.StudioProperty;
import io.jmix.flowui.kit.meta.StudioPropertyGroups;
import io.jmix.flowui.kit.meta.StudioPropertyType;
import io.jmix.flowui.kit.meta.StudioUiKit;
import io.jmix.flowui.kit.meta.StudioXmlAttributes;

/**
 * Jmix Studio meta of the components declared in {@code app-ui-components.xsd}: palette entries and
 * the attributes the property inspector offers. Read from bytecode by Studio only.
 */
@StudioUiKit
public interface StudioAppComponents {

    @StudioComponent(
            name = "AvatarUpload",
            classFqn = "com.glebfox.timesheets.component.avatarupload.AvatarUpload",
            category = "Components",
            xmlElement = "avatarUpload",
            xmlns = "http://glebfox.com/schema/app-ui-components.xsd",
            xmlnsAlias = "app",
            icon = "com/glebfox/timesheets/meta/icon/component/avatarUpload.svg",
            propertyGroups = {
                    StudioPropertyGroups.BaseSizedEnabledComponentWithClassName.class,
                    StudioPropertyGroups.CollectionOrInstanceDataContainer.class,
                    StudioPropertyGroups.Property.class,
                    StudioPropertyGroups.Label.class,
                    StudioPropertyGroups.HelperText.class,
                    StudioPropertyGroups.ReadOnly.class
            },
            properties = {
                    @StudioProperty(
                            xmlAttribute = StudioXmlAttributes.MAX_FILE_SIZE,
                            type = StudioPropertyType.INTEGER,
                            category = StudioProperty.Category.VALIDATION,
                            defaultValue = "" + AvatarUpload.DEFAULT_MAX_FILE_SIZE),
                    // Kept in step with AvatarImageFormat and the schema's avatarUploadAcceptedFileTypes
                    @StudioProperty(
                            xmlAttribute = StudioXmlAttributes.ACCEPTED_FILE_TYPES,
                            type = StudioPropertyType.VALUES_LIST,
                            category = StudioProperty.Category.VALIDATION,
                            options = {"image/png", "image/jpeg", "image/gif", "image/webp",
                                    ".png", ".jpg", ".jpeg", ".gif", ".webp"})
            },
            propertiesBindings = @StudioPropertiesBinding(source = "dataContainer", item = "property")
    )
    AvatarUpload avatarUpload();
}

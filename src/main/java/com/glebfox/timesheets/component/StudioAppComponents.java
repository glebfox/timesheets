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
import com.glebfox.timesheets.component.daterange.DateRangeField;
import com.glebfox.timesheets.component.daterange.DateRangeFilter;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import io.jmix.flowui.kit.meta.StudioAvailableChildrenInfo;
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

    @StudioComponent(
            name = "DateRangeField",
            classFqn = "com.glebfox.timesheets.component.daterange.DateRangeField",
            category = "Components",
            xmlElement = "dateRangeField",
            xmlns = "http://glebfox.com/schema/app-ui-components.xsd",
            xmlnsAlias = "app",
            icon = "com/glebfox/timesheets/meta/icon/component/dateRangeField.svg",
            propertyGroups = {
                    StudioPropertyGroups.BaseSizedEnabledComponentWithClassName.class,
                    StudioPropertyGroups.CollectionOrInstanceDataContainer.class,
                    StudioPropertyGroups.Label.class,
                    StudioPropertyGroups.RequiredAttributes.class,
                    StudioPropertyGroups.ReadOnly.class
            },
            properties = {
                    @StudioProperty(xmlAttribute = "startProperty", type = StudioPropertyType.PROPERTY_REF,
                            category = StudioProperty.Category.DATA_BINDING),
                    @StudioProperty(xmlAttribute = "endProperty", type = StudioPropertyType.PROPERTY_REF,
                            category = StudioProperty.Category.DATA_BINDING),
                    @StudioProperty(xmlAttribute = "startPlaceholder", type = StudioPropertyType.LOCALIZED_STRING),
                    @StudioProperty(xmlAttribute = "endPlaceholder", type = StudioPropertyType.LOCALIZED_STRING),
                    @StudioProperty(xmlAttribute = "startAriaLabel", type = StudioPropertyType.LOCALIZED_STRING),
                    @StudioProperty(xmlAttribute = "endAriaLabel", type = StudioPropertyType.LOCALIZED_STRING),
                    @StudioProperty(xmlAttribute = StudioXmlAttributes.CLEAR_BUTTON_VISIBLE,
                            type = StudioPropertyType.BOOLEAN, defaultValue = "false"),
                    @StudioProperty(xmlAttribute = "summaryVisible", type = StudioPropertyType.BOOLEAN,
                            defaultValue = "false")
            },
            propertiesBindings = {
                    @StudioPropertiesBinding(source = "dataContainer", item = "startProperty"),
                    @StudioPropertiesBinding(source = "dataContainer", item = "endProperty")
            }
    )
    DateRangeField dateRangeField();

    @StudioComponent(
            name = "DateRangeFilter",
            classFqn = "com.glebfox.timesheets.component.daterange.DateRangeFilter",
            category = "Components",
            xmlElement = "dateRangeFilter",
            xmlns = "http://glebfox.com/schema/app-ui-components.xsd",
            xmlnsAlias = "app",
            icon = "com/glebfox/timesheets/meta/icon/component/dateRangeFilter.svg",
            // As Jmix's propertyFilter declares its value component; the schema allows only a dateRangeField
            availableChildren = @StudioAvailableChildrenInfo(
                    availableTags = @StudioAvailableChildrenInfo.TagInfo(
                            qualifiedName = StudioAvailableChildrenInfo.ANY_TAG,
                            maxCount = 1)),
            propertyGroups = {
                    StudioPropertyGroups.BaseSizedComponentWithClassNames.class,
                    StudioPropertyGroups.DataLoader.class,
                    StudioPropertyGroups.AutoApply.class,
                    StudioPropertyGroups.Label.class,
                    StudioPropertyGroups.LabelVisible.class
            },
            properties = {
                    @StudioProperty(xmlAttribute = "startProperty", type = StudioPropertyType.PROPERTY_REF,
                            category = StudioProperty.Category.DATA_BINDING, required = true),
                    @StudioProperty(xmlAttribute = "endProperty", type = StudioPropertyType.PROPERTY_REF,
                            category = StudioProperty.Category.DATA_BINDING, required = true)
            },
            propertiesBindings = {
                    @StudioPropertiesBinding(source = "dataLoader", item = "startProperty"),
                    @StudioPropertiesBinding(source = "dataLoader", item = "endProperty")
            }
    )
    DateRangeFilter dateRangeFilter();

    @StudioComponent(
            name = "GridEmptyState",
            classFqn = "com.glebfox.timesheets.component.emptystate.GridEmptyState",
            category = "Components",
            xmlElement = "gridEmptyState",
            xmlns = "http://glebfox.com/schema/app-ui-components.xsd",
            xmlnsAlias = "app",
            icon = "com/glebfox/timesheets/meta/icon/component/gridEmptyState.svg",
            propertyGroups = {
                    StudioPropertyGroups.BaseComponentWithClassNames.class,
                    StudioPropertyGroups.Action.class,
                    StudioPropertyGroups.Icon.class
            },
            properties = {
                    @StudioProperty(xmlAttribute = "text", type = StudioPropertyType.LOCALIZED_STRING,
                            required = true)
            }
    )
    GridEmptyState gridEmptyState();
}

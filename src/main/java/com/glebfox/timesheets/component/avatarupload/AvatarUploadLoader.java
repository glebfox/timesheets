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

package com.glebfox.timesheets.component.avatarupload;

import io.jmix.flowui.kit.xml.layout.support.LoaderUtils;
import io.jmix.flowui.xml.layout.loader.AbstractComponentLoader;
import io.jmix.flowui.xml.layout.support.DataLoaderSupport;

public class AvatarUploadLoader extends AbstractComponentLoader<AvatarUpload> {

    protected DataLoaderSupport dataLoaderSupport;

    @Override
    protected AvatarUpload createComponent() {
        return factory.create(AvatarUpload.class);
    }

    @Override
    public void loadComponent() {
        getDataLoaderSupport().loadData(resultComponent, element);

        componentLoader().loadLabel(resultComponent, element);
        componentLoader().loadHelperText(resultComponent, element);
        componentLoader().loadEnabled(resultComponent, element);
        componentLoader().loadClassNames(resultComponent, element);
        componentLoader().loadSizeAttributes(resultComponent, element);
        componentLoader().loadValueAndElementAttributes(resultComponent, element);

        loadInteger(element, "maxFileSize", resultComponent::setMaxFileSize);
        loadString(element, "acceptedFileTypes")
                .map(LoaderUtils::split)
                .ifPresent(fileTypes -> resultComponent.setAcceptedFileTypes(fileTypes.toArray(String[]::new)));
    }

    protected DataLoaderSupport getDataLoaderSupport() {
        if (dataLoaderSupport == null) {
            dataLoaderSupport = applicationContext.getBean(DataLoaderSupport.class, context);
        }
        return dataLoaderSupport;
    }
}

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

package com.glebfox.timesheets.data.renderer;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

/**
 * A renderer that shows an item as an avatar beside a title, with an optional badge after the title and an optional
 * body line under it, as {@code app/avatar-cell.css} in the theme lays them out.
 *
 * @param <ITEM> the type of the items being rendered
 * @see AvatarCell
 */
@NullMarked
public class AvatarCellRenderer<ITEM> extends ComponentRenderer<Div, ITEM> {

    private final Function<ITEM, @Nullable AvatarCell> cellProvider;

    /**
     * Creates a renderer that shows what the provider gives for each item.
     *
     * @param cellProvider a function that gives the content of an item's cell; an item it gives {@code null} for
     *                     renders as an empty cell
     */
    public AvatarCellRenderer(Function<ITEM, @Nullable AvatarCell> cellProvider) {
        super(((SerializableSupplier<Div>) null));
        this.cellProvider = cellProvider;
    }

    @Nullable
    @Override
    public Div createComponent(ITEM item) {
        AvatarCell cell = cellProvider.apply(item);
        if (cell == null) {
            return null;
        }

        Avatar avatar = createAvatar(cell.title(), cell.image());
        Div title = createTitle(cell.title());
        Badge badge = cell.badge();

        Div wrapper = new Div();
        wrapper.setClassName("avatar-cell");
        wrapper.add(avatar, title);
        if (badge != null) {
            badge.addClassName("title-suffix");
            wrapper.add(badge);
        }

        String body = cell.body();
        if (body != null) {
            wrapper.add(createBody(body));
        } else {
            title.addClassNames("title-body");
            if (badge != null) {
                badge.addClassNames("title-suffix-body");
            }
        }

        return wrapper;
    }

    private Avatar createAvatar(String name, byte @Nullable [] data) {
        Avatar avatar = new Avatar(name);
        avatar.getElement().setAttribute("tabindex", "-1");

        if (data != null) {
            InputStreamDownloadHandler downloadHandler = DownloadHandler.fromInputStream(downloadEvent ->
                    new DownloadResponse(new ByteArrayInputStream(data),
                            "avatar.png", null, data.length));

            // allowDisabled(): Vaadin answers 403 for the image of a disabled avatar, which every avatar in a tab
            // that tabbed mode disables is; see ComponentSupport.createAvatar for the details
            avatar.setImageHandler(downloadHandler.inline().allowDisabled());
        }

        avatar.addClassName("prefix");
        return avatar;
    }

    private Div createTitle(String text) {
        Div title = new Div();
        title.setText(text);
        title.setClassName("title");
        return title;
    }

    private Div createBody(String text) {
        Div body = new Div();
        body.setText(text);
        body.setClassName("body");
        return body;
    }
}

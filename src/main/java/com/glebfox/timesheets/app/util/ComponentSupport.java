/*
 * Copyright 2023 Gleb Gorelov
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

package com.glebfox.timesheets.app.util;

import com.glebfox.timesheets.data.renderer.BadgeRender;
import com.glebfox.timesheets.data.renderer.UserRenderer;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.google.common.base.Strings;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.UiComponents;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

@Component("ts_ComponentSupport")
public class ComponentSupport {

    private final UiComponents uiComponents;
    private final Messages messages;
    private final MetadataTools metadataTools;

    public ComponentSupport(UiComponents uiComponents,
                            Messages messages,
                            MetadataTools metadataTools) {
        this.uiComponents = uiComponents;
        this.messages = messages;
        this.metadataTools = metadataTools;
    }

    /**
     * Creates and configures an {@link Avatar} component.
     *
     * @param name      the name to set for the avatar
     * @param data      the image data for the avatar in byte array form, or null if no image is provided
     * @return a configured {@link Avatar} instance
     */
    public Avatar createAvatar(String name, @Nullable byte[] data) {
        return createAvatar(name, data, null);
    }

    /**
     * Creates and configures an {@link Avatar} component.
     *
     * @param name      the name to set for the avatar
     * @param data      the image data for the avatar in byte array form, or null if no image is provided
     * @param className the CSS class name to be added to the avatar
     * @return a configured {@link Avatar} instance
     */
    public Avatar createAvatar(String name, @Nullable byte[] data, @Nullable String className) {
        Avatar avatar = uiComponents.create(Avatar.class);
        avatar.setName(name);
        avatar.getElement().setAttribute("tabindex", "-1");

        if (data != null) {
            InputStreamDownloadHandler downloadHandler = DownloadHandler.fromInputStream(downloadEvent ->
                    new DownloadResponse(new ByteArrayInputStream(data),
                            "avatar.png", null, data.length));

            avatar.setImageHandler(downloadHandler);
        }

        if (!Strings.isNullOrEmpty(className)) {
            avatar.addClassName(className);
        }

        return avatar;
    }

    /**
     * Creates a default {@link UserRenderer} for rendering a {@link User} entity.
     * The renderer generates a visual representation of a user, including their avatar,
     * name, and an activity status badge.
     *
     * @return a {@link UserRenderer} instance configured to render users with their user data.
     */
    public UserRenderer<User> createUserRenderer() {
        return createUserRenderer(user -> user);
    }

    /**
     * Creates a {@link UserRenderer} for rendering custom user visualizations based on the provided value provider.
     * The renderer utilizes the specified function to extract user information from the given items
     * and renders the User properties including their status.
     *
     * @param valueProvider a function that extracts {@link User} instances from items of the specified type
     * @param <ITEM>        the type of the items being rendered
     * @return a {@link UserRenderer} instance configured to render user information with associated status
     */
    public <ITEM> UserRenderer<ITEM> createUserRenderer(Function<ITEM, User> valueProvider) {
        return new UserRenderer<>(valueProvider, user ->
                messages.getMessage(UserRenderer.class, Boolean.TRUE.equals(user.getActive())
                        ? "UserRenderer.userActive"
                        : "UserRenderer.userInactive"));
    }

    /**
     * Creates a {@link BadgeRender} specifically configured for rendering the status of {@link TimeEntry} entities.
     * The returned renderer formats the status as a string and applies a theme based on the status value.
     *
     * @return a {@link BadgeRender} instance for rendering {@link TimeEntry} status with appropriate formatting
     * and theming.
     */
    public BadgeRender<TimeEntry> createTimeEntryStatusRender() {
        return new BadgeRender<>(
                timeEntry -> metadataTools.format(timeEntry.getStatus()),
                timeEntry -> switch (timeEntry.getStatus()) {
                    case NEW -> "warning";
                    case APPROVED -> "success";
                    case REJECTED -> "error";
                    case CLOSED -> "contrast";
                }
        );
    }

    /**
     * Creates a function that maps items to theme names based on their {@link ProjectRoleType}.
     * The returned function uses the provided {@link Function} to determine the {@link ProjectRoleType}
     * of a given item and maps it to a string representing a theme.
     *
     * @param <ITEM>   the type of the items to be processed
     * @param provider a {@link Function} that determines the {@link ProjectRoleType} of a given item
     * @return a function mapping items to theme names based on their {@link ProjectRoleType}
     */
    public <ITEM> Function<ITEM, String> createProjectRoleTypeThemeProvider(Function<ITEM, ProjectRoleType> provider) {
        return item -> switch (provider.apply(item)) {
            case MANAGER -> "primary";
            case APPROVER -> "success";
            case OBSERVER -> "contrast";
            case MEMBER -> "";
        };
    }

    /**
     * Creates a {@link Renderer} for rendering {@link Task} objects that includes both
     * the task's name and its associated project in a formatted representation.
     * <p>
     * The generated renderer includes the task name prominently and the project
     * below it in a smaller, secondary style.
     *
     * @return a {@link Renderer} instance for rendering task details, including
     * task names and their associated projects
     */
    public Renderer<Task> createTaskWithProjectRenderer() {
        return createTaskWithProjectRenderer(task -> task);
    }

    /**
     * Creates a {@link Renderer} for rendering items based on a specified {@link Task} provider.
     * The renderer generates a visual representation that includes the task's name prominently
     * and its associated project below in a smaller, secondary style.
     *
     * @param <ITEM>        the type of the items being rendered
     * @param valueProvider a function that extracts {@link Task} instances from the given items
     * @return a {@link Renderer} instance for rendering task details, including task names and their
     * associated projects
     */
    public <ITEM> Renderer<ITEM> createTaskWithProjectRenderer(Function<ITEM, Task> valueProvider) {
        String template = "${item.name}" +
                "<div class=\"secondary-text\">" +
                "  ${item.project}" +
                "</div>";

        return LitRenderer.<ITEM>of(template)
                .withProperty("name", item ->
                        valueProvider.apply(item).getName())
                .withProperty("project", item ->
                        metadataTools.format(valueProvider.apply(item).getProject()));
    }
}
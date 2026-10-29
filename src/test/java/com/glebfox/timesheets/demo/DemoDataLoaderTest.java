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

import com.glebfox.timesheets.app.WorkPlanSupport;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.component.avatarupload.AvatarImageFormat;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.TimeSource;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Starts the application as a {@code dev} start does, on a database of its own: the demo data
 * would otherwise land in the database the other tests share.
 */
@SpringBootTest(properties = "main.datasource.url=jdbc:hsqldb:mem:demo")
@ActiveProfiles({"test", "dev"})
public class DemoDataLoaderTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    DemoDataLoader demoDataLoader;
    @Autowired
    WorkPlanSupport workPlanSupport;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    TimeSource timeSource;
    // Not reset after each test: the calls to check come from the load on startup
    @MockitoBean(reset = MockReset.NONE)
    UiEventPublisher uiEventPublisher;

    @Test
    void loadsOnStartup() {
        systemAuthenticator.runWithSystem(() -> {
            assertThat(dataManager.load(Client.class).all().list()).hasSize(6);
            assertThat(dataManager.load(Project.class).all().list()).hasSize(10);
            assertThat(dataManager.load(User.class).all().list())
                    .hasSize(18)
                    .filteredOn(user -> user.getUsername().equals("admin"))
                    .singleElement()
                    .satisfies(admin -> assertThat(admin.getLastName()).isEqualTo("Breen"));
            assertThat(loadEntries()).hasSizeGreaterThan(500);
        });
    }

    @Test
    void runsTheListenersAfterCommitAuthenticated() {
        // After the commit, the listener loads each new entry to tell its user, which fails without authentication
        int entryCount = systemAuthenticator.withSystem(() -> loadEntries().size());
        verify(uiEventPublisher, times(entryCount)).publishEventForUsers(any(), any());
    }

    @Test
    void logsTimeUpToTodayOnWorkdaysOnly() {
        LocalDate today = timeSource.now().toLocalDate();
        systemAuthenticator.runWithSystem(() -> {
            List<TimeEntry> entries = loadEntries();
            assertThat(entries).noneMatch(entry -> entry.getDate().isAfter(today));
            assertThat(entries).anyMatch(entry -> entry.getDate().isAfter(today.minusDays(14)));

            LocalDate start = entries.stream().map(TimeEntry::getDate).min(LocalDate::compareTo).orElseThrow();
            Map<User, List<TimeEntry>> entriesByUser = entries.stream()
                    .collect(Collectors.groupingBy(TimeEntry::getUser));
            entriesByUser.forEach((user, userEntries) -> {
                WorkPlan plan = workPlanSupport.loadWorkPlan(user, start, today.plusDays(1));
                assertThat(userEntries)
                        .as("entries of %s", user.getUsername())
                        .allMatch(entry -> plan.isWorkday(entry.getDate()));
            });
        });
    }

    @Test
    void logsTimeOnlyOnTheProjectsOfItsUser() {
        systemAuthenticator.runWithSystem(() -> {
            Set<String> loggingParticipants = dataManager.load(ProjectParticipant.class)
                    .all()
                    .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                            .add("user", FetchPlan.BASE)
                            .add("role", FetchPlan.BASE)
                            .add("project", FetchPlan.BASE))
                    .list().stream()
                    .filter(participant -> participant.getRole().getType() != ProjectRoleType.OBSERVER)
                    .map(participant -> participant.getUser().getUsername() + "@" + participant.getProject().getName())
                    .collect(Collectors.toSet());

            assertThat(loadEntries()).allMatch(entry -> loggingParticipants.contains(
                    entry.getUser().getUsername() + "@" + entry.getTask().getProject().getName()));
        });
    }

    @Test
    void tagsEveryEntryFromTheRequiredGroupsOfItsTask() {
        systemAuthenticator.runWithSystem(() -> assertThat(loadEntries()).allSatisfy(entry -> {
            Set<TagGroup> taggedGroups = entry.getTags().stream()
                    .map(Tag::getTagGroup)
                    .collect(Collectors.toSet());
            assertThat(taggedGroups).containsAll(entry.getTask().getRequiredTagGroups());
        }));
    }

    @Test
    void rejectsSomeEntriesWithAReason() {
        systemAuthenticator.runWithSystem(() -> assertThat(loadEntries())
                .filteredOn(entry -> entry.getStatus() == TimeEntryStatus.REJECTED)
                .isNotEmpty()
                .allMatch(entry -> entry.getRejectionReason() != null)
                .anyMatch(entry -> entry.getUser().getUsername().equals("admin")));
    }

    @Test
    void storesImagesOfAcceptedFormats() {
        systemAuthenticator.runWithSystem(() -> {
            assertThat(dataManager.load(Client.class).all().list())
                    .filteredOn(client -> client.getImage() != null)
                    .hasSize(5)
                    .allMatch(client -> isAcceptedImage(client.getImage()));
            assertThat(dataManager.load(User.class).all().list())
                    .allMatch(user -> user.getAvatar() != null && isAcceptedImage(user.getAvatar()));
        });
    }

    @Test
    void loadsNothingMoreWhenTheDatabaseHasData() {
        Function<Class<?>, Integer> count = entityClass -> systemAuthenticator.withSystem(() ->
                dataManager.load(entityClass).all().list().size());
        int clients = count.apply(Client.class);
        int entries = count.apply(TimeEntry.class);

        // No authentication around the call, as on the startup thread
        demoDataLoader.loadIfEmpty();

        assertThat(count.apply(Client.class)).isEqualTo(clients);
        assertThat(count.apply(TimeEntry.class)).isEqualTo(entries);
    }

    private List<TimeEntry> loadEntries() {
        return dataManager.load(TimeEntry.class)
                .all()
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("task", taskBuilder -> taskBuilder.addFetchPlan(FetchPlan.BASE)
                                .add("project", FetchPlan.BASE)
                                .add("requiredTagGroups", FetchPlan.BASE))
                        .add("tags", tagBuilder -> tagBuilder.addFetchPlan(FetchPlan.BASE)
                                .add("tagGroup", FetchPlan.BASE)))
                .list();
    }

    private static boolean isAcceptedImage(byte[] image) {
        return AvatarImageFormat.detect(ByteBuffer.wrap(image)).isPresent();
    }
}

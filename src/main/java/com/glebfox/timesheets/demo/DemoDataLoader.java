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
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.misc.ContactInformation;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.security.UiMinimalRole;
import com.glebfox.timesheets.security.WorkScheduleManagementRole;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.TimeSource;
import io.jmix.core.security.Authenticated;
import io.jmix.data.PersistenceHints;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Creates the demo data: people and organizations of the Half-Life and Portal games, their
 * projects, and time logged over the weeks up to the current date, so the calendar opens on data.
 */
@Profile("dev")
@Component("ts_DemoDataLoader")
public class DemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private static final String AVATARS = "com/glebfox/timesheets/demo/avatars/";
    private static final String LOGOS = "com/glebfox/timesheets/demo/logos/";
    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "webp", "gif");

    private static final DateTimeFormatter MOVED_FROM_FORMAT = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);

    private final DataManager dataManager;
    private final PasswordEncoder passwordEncoder;
    private final WorkPlanSupport workPlanSupport;
    private final TimeSource timeSource;
    private final TransactionTemplate transactionTemplate;

    public DemoDataLoader(DataManager dataManager,
                          PasswordEncoder passwordEncoder,
                          WorkPlanSupport workPlanSupport,
                          TimeSource timeSource,
                          TransactionTemplate transactionTemplate) {
        this.dataManager = dataManager;
        this.passwordEncoder = passwordEncoder;
        this.workPlanSupport = workPlanSupport;
        this.timeSource = timeSource;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Loads the demo data unless the database has a client, a deleted one included. Runs under
     * the system user, so it works on the startup thread, and in one transaction, so a failed load
     * leaves nothing behind and runs again on the next start.
     */
    @Authenticated
    public void loadIfEmpty() {
        // The transaction opens inside the authentication: the listeners that run after its commit load data
        // too. With @Transactional beside @Authenticated, the transaction would wrap the authentication, and
        // those listeners would run unauthenticated.
        transactionTemplate.executeWithoutResult(status -> load());
    }

    private void load() {
        boolean hasClients = dataManager.load(Client.class)
                .all()
                .hint(PersistenceHints.SOFT_DELETION, false)
                .maxResults(1)
                .optional()
                .isPresent();
        if (hasClients) {
            log.info("Demo data skipped: the database already has clients");
            return;
        }

        LocalDate today = timeSource.now().toLocalDate();
        DataSet dataSet = new DataSet(today);
        dataSet.create();
        int entryCount = new DemoTimeEntryGenerator(dataManager, workPlanSupport).generate(today);
        log.info("Demo data loaded: {} users, {} time entries", dataSet.users.size(), entryCount);
    }

    private byte @Nullable [] loadImage(String folder, @Nullable String name) {
        if (name == null) {
            return null;
        }
        for (String extension : IMAGE_EXTENSIONS) {
            ClassPathResource resource = new ClassPathResource(folder + name + "." + extension);
            if (resource.exists()) {
                try (InputStream stream = resource.getInputStream()) {
                    return stream.readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        log.debug("No demo image {}{} with any of the extensions {}", folder, name, IMAGE_EXTENSIONS);
        return null;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * The records of one load, saved together.
     */
    private class DataSet {

        private final LocalDate today;
        private final SaveContext context = new SaveContext();
        private final Map<String, ProjectRole> projectRoles = new LinkedHashMap<>();
        private final Map<String, TaskType> taskTypes = new LinkedHashMap<>();
        private final Map<String, User> users = new LinkedHashMap<>();

        private DataSet(LocalDate today) {
            this.today = today;
        }

        private void create() {
            HolidayCalendar calendar = createHolidayCalendar();
            Map<String, WorkSchedule> schedules = createWorkSchedules(calendar);
            createProjectRoles();
            createTaskTypes();
            createUsers();
            assignSchedules(schedules);
            createProjects(createClients());
            dataManager.save(context.setDiscardSaved(true));
        }

        // ---------------------------------------------------------------- work schedules

        private HolidayCalendar createHolidayCalendar() {
            HolidayCalendar calendar = dataManager.create(HolidayCalendar.class);
            calendar.setName("Russia");
            calendar.setDescription("Public holidays of Russia; a holiday on a weekend moves to the next workday");
            context.saving(calendar);
            for (int year = today.getYear() - 1; year <= today.getYear() + 1; year++) {
                addHolidays(calendar, year);
            }
            return calendar;
        }

        private void addHolidays(HolidayCalendar calendar, int year) {
            Map<LocalDate, String> holidays = new LinkedHashMap<>();
            for (int day = 1; day <= 8; day++) {
                holidays.put(LocalDate.of(year, 1, day), day == 7 ? "Christmas" : "New Year holidays");
            }
            holidays.put(LocalDate.of(year, 2, 23), "Defender of the Fatherland Day");
            holidays.put(LocalDate.of(year, 3, 8), "International Women's Day");
            holidays.put(LocalDate.of(year, 5, 1), "Spring and Labour Day");
            holidays.put(LocalDate.of(year, 5, 9), "Victory Day");
            holidays.put(LocalDate.of(year, 6, 12), "Russia Day");
            holidays.put(LocalDate.of(year, 11, 4), "Unity Day");
            holidays.forEach((date, name) -> addCalendarDay(calendar, date, name));

            // The New Year holidays already include their weekends; any other holiday on a weekend
            // gives a day off on the next workday
            Set<LocalDate> movedDaysOff = new HashSet<>();
            holidays.keySet().stream()
                    .filter(date -> date.getMonthValue() != 1 && isWeekend(date))
                    .forEach(date -> {
                        LocalDate dayOff = date.plusDays(1);
                        while (isWeekend(dayOff) || holidays.containsKey(dayOff) || movedDaysOff.contains(dayOff)) {
                            dayOff = dayOff.plusDays(1);
                        }
                        movedDaysOff.add(dayOff);
                        addCalendarDay(calendar, dayOff, "Day off moved from " + MOVED_FROM_FORMAT.format(date));
                    });
        }

        private void addCalendarDay(HolidayCalendar calendar, LocalDate date, String name) {
            HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
            day.setCalendar(calendar);
            day.setDate(date);
            day.setType(HolidayCalendarDayType.DAY_OFF);
            day.setName(name);
            context.saving(day);
        }

        private Map<String, WorkSchedule> createWorkSchedules(HolidayCalendar calendar) {
            // Full time, the default schedule, comes with the schema
            WorkSchedule fullTime = dataManager.load(WorkSchedule.class)
                    .query("e.isDefault = true")
                    .one();
            fullTime.setDescription("8 hours, Monday to Friday");
            fullTime.setHolidayCalendar(calendar);
            context.saving(fullTime);

            WorkSchedule halfTime = dataManager.create(WorkSchedule.class);
            halfTime.setName("Half time");
            halfTime.setDescription("Half of a full-time rate: 4 hours, Monday to Friday");
            halfTime.setWorkdayTime(WorkTime.ofHours(4));
            halfTime.setHolidayCalendar(calendar);
            context.saving(halfTime);

            WorkSchedule fourDayWeek = dataManager.create(WorkSchedule.class);
            fourDayWeek.setName("Four-day week");
            fourDayWeek.setDescription("8 hours, Monday to Thursday");
            fourDayWeek.setWorkdayTime(WorkTime.ofHours(8));
            fourDayWeek.setFriday(false);
            fourDayWeek.setHolidayCalendar(calendar);
            context.saving(fourDayWeek);

            return Map.of("full", fullTime, "half", halfTime, "four-day", fourDayWeek);
        }

        private void assignSchedules(Map<String, WorkSchedule> schedules) {
            LocalDate thisMonth = today.withDayOfMonth(1);
            // Changed schedule a month ago, so the past weeks show both plans
            assignSchedule("rosenberg", schedules.get("half"), thisMonth.minusMonths(1));
            // Changes schedule next month, so the plan ahead differs from the logged weeks
            assignSchedule("kleiner", schedules.get("half"), thisMonth.plusMonths(1));
            assignSchedule("caroline", schedules.get("four-day"), thisMonth.minusYears(1));
        }

        private void assignSchedule(String username, WorkSchedule schedule, LocalDate startDate) {
            WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
            assignment.setUser(users.get(username));
            assignment.setWorkSchedule(schedule);
            assignment.setStartDate(startDate);
            context.saving(assignment);
        }

        // ---------------------------------------------------------------- reference data

        private void createProjectRoles() {
            for (Object[] row : new Object[][]{
                    // name, type, description
                    {"Project Manager", ProjectRoleType.MANAGER, "Plans the work and leads the team"},
                    {"Approver", ProjectRoleType.APPROVER, "Reviews and approves the logged time"},
                    {"Analyst", ProjectRoleType.MEMBER, "Analyses the data and the results"},
                    {"Research Scientist", ProjectRoleType.MEMBER, "Runs the research and the experiments"},
                    {"Engineer", ProjectRoleType.MEMBER, "Builds and maintains the equipment"},
                    {"Security Officer", ProjectRoleType.MEMBER, "Keeps the site and the team safe"},
                    {"Test Subject", ProjectRoleType.MEMBER, "Takes part in the tests"},
                    {"Observer", ProjectRoleType.OBSERVER, "Follows the project without logging time"},
            }) {
                ProjectRole role = dataManager.create(ProjectRole.class);
                role.setName((String) row[0]);
                role.setType((ProjectRoleType) row[1]);
                role.setDescription((String) row[2]);
                context.saving(role);
                projectRoles.put(role.getName(), role);
            }
        }

        private void createTaskTypes() {
            for (String[] row : new String[][]{
                    // name, description
                    {"Research", "Studying a question and writing up the findings"},
                    {"Experiment", "Preparing and running experiments"},
                    {"Engineering", "Building, repairing, and calibrating equipment"},
                    {"Testing", "Checking that equipment and procedures work as intended"},
                    {"Analysis", "Analysing data and preparing reports"},
                    {"Management", "Planning, meetings, and paperwork"},
                    {"Security", "Protecting the site, the people, and the equipment"},
            }) {
                TaskType type = dataManager.create(TaskType.class);
                type.setName(row[0]);
                type.setDescription(row[1]);
                context.saving(type);
                taskTypes.put(type.getName(), type);
            }
        }

        // ---------------------------------------------------------------- users

        private void createUsers() {
            User admin = dataManager.load(User.class)
                    .query("e.username = 'admin'")
                    .one();
            admin.setFirstName("Wallace");
            admin.setLastName("Breen");
            admin.setEmail("breen@black-mesa.example");
            admin.setAvatar(loadImage(AVATARS, "wallace-breen"));
            context.saving(admin);
            users.put(admin.getUsername(), admin);

            for (DemoUser demoUser : List.of(
                    new DemoUser("freeman", "Gordon", "Freeman", "black-mesa.example", "gordon-freeman"),
                    new DemoUser("kleiner", "Isaac", "Kleiner", "black-mesa.example", "isaac-kleiner"),
                    new DemoUser("barney", "Barney", "Calhoun", "black-mesa.example", "barney-calhoun"),
                    new DemoUser("rosenberg", null, "Rosenberg", "black-mesa.example", "rosenberg"),
                    new DemoUser("gina", "Gina", "Cross", "black-mesa.example", "gina-cross"),
                    new DemoUser("colette", "Colette", "Green", "black-mesa.example", "colette-green"),
                    new DemoUser("eli", "Eli", "Vance", "black-mesa-east.example", "eli-vance"),
                    new DemoUser("alyx", "Alyx", "Vance", "black-mesa-east.example", "alyx-vance"),
                    new DemoUser("mossman", "Judith", "Mossman", "black-mesa-east.example", "judith-mossman"),
                    new DemoUser("magnusson", "Arne", "Magnusson", "white-forest.example", "arne-magnusson"),
                    new DemoUser("odessa", "Odessa", "Cubbage", "white-forest.example", "odessa-cubbage"),
                    new DemoUser("shephard", "Adrian", "Shephard", "hecu.example", "adrian-shephard"),
                    new DemoUser("cave", "Cave", "Johnson", "aperture-science.example", "cave-johnson"),
                    new DemoUser("caroline", "Caroline", null, "aperture-science.example", "caroline"),
                    new DemoUser("rattmann", "Doug", "Rattmann", "aperture-science.example", "doug-rattmann"),
                    new DemoUser("chell", "Chell", null, "aperture-science.example", "chell"))) {
                User user = createUser(demoUser.username(), demoUser.firstName(), demoUser.lastName());
                user.setEmail(demoUser.username() + "@" + demoUser.emailDomain());
                user.setAvatar(loadImage(AVATARS, demoUser.avatar()));
                // Judith Mossman shows a user with granular roles; everyone else may see everything
                if (user.getUsername().equals("mossman")) {
                    assignRole(user, UiMinimalRole.CODE);
                    assignRole(user, WorkScheduleManagementRole.CODE);
                } else {
                    assignRole(user, FullAccessRole.CODE);
                }
            }

            // An inactive user with no roles: cannot sign in
            User gman = createUser("gman", "G-Man", null);
            gman.setAvatar(loadImage(AVATARS, "g-man"));
            gman.setActive(false);
        }

        private User createUser(String username, @Nullable String firstName, @Nullable String lastName) {
            User user = dataManager.create(User.class);
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(username));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            context.saving(user);
            users.put(username, user);
            return user;
        }

        private void assignRole(User user, String roleCode) {
            RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
            context.saving(assignment);
        }

        // ---------------------------------------------------------------- clients and projects

        private Map<String, Client> createClients() {
            Map<String, Client> clients = new LinkedHashMap<>();
            for (DemoClient demoClient : List.of(
                    new DemoClient("Black Mesa", "info@black-mesa.example", "+1 505 555 0100",
                            "https://black-mesa.example", "Black Mesa Research Facility, New Mexico, USA",
                            "black-mesa"),
                    new DemoClient("Aperture Science", "contact@aperture-science.example", "+1 906 555 0142",
                            "https://aperture-science.example",
                            "Aperture Science Enrichment Center, Upper Michigan, USA", "aperture-science"),
                    new DemoClient("Black Mesa East", "eli@black-mesa-east.example", null, null,
                            "Black Mesa East, the outskirts of City 17", "black-mesa-east"),
                    new DemoClient("White Forest", "magnusson@white-forest.example", null, null,
                            "White Forest base, the northern mountains", "white-forest"),
                    new DemoClient("Universal Union", "office@universal-union.example", "+7 017 555 0001",
                            "https://universal-union.example", "The Citadel, City 17", "universal-union"),
                    new DemoClient("Ravenholm Parish", null, null, null, "Ravenholm", null))) {
                Client client = dataManager.create(Client.class);
                client.setName(demoClient.name());
                ContactInformation contactInformation = dataManager.create(ContactInformation.class);
                contactInformation.setEmail(demoClient.email());
                contactInformation.setPhone(demoClient.phone());
                contactInformation.setUrl(demoClient.url());
                contactInformation.setAddress(demoClient.address());
                client.setContactInformation(contactInformation);
                client.setImage(loadImage(LOGOS, demoClient.logo()));
                context.saving(client);
                clients.put(client.getName(), client);
            }
            return clients;
        }

        private void createProjects(Map<String, Client> clients) {
            DemoProject anomalousMaterials = project(clients.get("Black Mesa"), "Anomalous Materials",
                    ProjectStatus.OPEN, "Spectral analysis of the anomalous crystal samples in Sector C");
            task(anomalousMaterials, "Sample spectral analysis", "Analysis", true);
            task(anomalousMaterials, "Anti-mass spectrometer calibration", "Engineering", false);
            task(anomalousMaterials, "Resonance cascade review", "Research", false).setStatus(TaskStatus.INACTIVE);
            task(anomalousMaterials, "Sector safety drills", "Security", false);
            task(anomalousMaterials, "Team meetings", "Management", false);
            participants(anomalousMaterials,
                    "kleiner", "Project Manager", "admin", "Approver", "freeman", "Research Scientist",
                    "gina", "Research Scientist", "colette", "Research Scientist", "rosenberg", "Analyst",
                    "barney", "Security Officer", "magnusson", "Approver", "gman", "Observer");

            DemoProject lambdaComplex = project(clients.get("Black Mesa"), "Lambda Complex Teleportation",
                    ProjectStatus.OPEN, "Teleportation research and the survey of the Xen border world");
            task(lambdaComplex, "Teleporter calibration", "Engineering", true);
            task(lambdaComplex, "Xen border world survey", "Research", false);
            task(lambdaComplex, "Portal stability experiments", "Experiment", true);
            task(lambdaComplex, "Weekly status", "Management", false);
            participants(lambdaComplex,
                    "eli", "Project Manager", "kleiner", "Approver", "rosenberg", "Research Scientist",
                    "freeman", "Engineer", "mossman", "Analyst", "shephard", "Security Officer");

            DemoProject sectorC = project(clients.get("Black Mesa"), "Sector C Retrofit",
                    ProjectStatus.CLOSED, "Retrofit of the Sector C test labs");
            task(sectorC, "Equipment teardown", "Engineering", false);
            task(sectorC, "Safety inspection", "Security", false);
            participants(sectorC,
                    "magnusson", "Project Manager", "gina", "Engineer", "barney", "Security Officer");

            DemoProject portalDevice = project(clients.get("Aperture Science"), "Portal Device",
                    ProjectStatus.OPEN, "The Aperture Science Handheld Portal Device");
            task(portalDevice, "Portal gun prototyping", "Engineering", true);
            task(portalDevice, "Momentum experiments", "Experiment", true);
            task(portalDevice, "Field reliability tests", "Testing", false);
            task(portalDevice, "Patent paperwork", "Management", false);
            participants(portalDevice,
                    "cave", "Project Manager", "caroline", "Approver", "rattmann", "Research Scientist",
                    "chell", "Test Subject", "gman", "Observer");

            DemoProject testChambers = project(clients.get("Aperture Science"), "Enrichment Center Test Chambers",
                    ProjectStatus.OPEN, "Design and testing of the Enrichment Center test chambers");
            task(testChambers, "Test chamber 19 design", "Engineering", true);
            task(testChambers, "Chamber walkthroughs", "Testing", false);
            task(testChambers, "Turret calibration", "Engineering", false);
            task(testChambers, "Cake procurement", "Management", false);
            participants(testChambers,
                    "caroline", "Project Manager", "cave", "Approver", "rattmann", "Engineer", "chell", "Test Subject");

            DemoProject repulsionGel = project(clients.get("Aperture Science"), "Repulsion Gel",
                    ProjectStatus.CLOSED, "Trials of the propulsion and repulsion gels");
            task(repulsionGel, "Gel dispersion trials", "Experiment", false);
            task(repulsionGel, "Safer formula research", "Research", false);
            participants(repulsionGel,
                    "cave", "Project Manager", "rattmann", "Research Scientist", "chell", "Test Subject");

            DemoProject gravityGun = project(clients.get("Black Mesa East"), "Gravity Gun",
                    ProjectStatus.OPEN, "A prototype of the Zero Point Energy Field Manipulator");
            task(gravityGun, "Field manipulator prototype", "Engineering", false);
            task(gravityGun, "Field tests", "Testing", false);
            task(gravityGun, "Dog maintenance", "Engineering", false);
            participants(gravityGun,
                    "eli", "Project Manager", "kleiner", "Approver", "alyx", "Engineer",
                    "mossman", "Research Scientist");

            DemoProject borealis = project(clients.get("White Forest"), "Borealis Recovery",
                    ProjectStatus.OPEN, "Locating and recovering the research vessel Borealis");
            task(borealis, "Coordinates decoding", "Analysis", false);
            task(borealis, "Expedition planning", "Management", false);
            task(borealis, "Arctic field work", "Research", true);
            participants(borealis,
                    "magnusson", "Project Manager", "mossman", "Research Scientist", "alyx", "Engineer",
                    "odessa", "Security Officer");

            DemoProject rocketLaunch = project(clients.get("White Forest"), "Rocket Launch",
                    ProjectStatus.OPEN, "Launching the satellite that closes the superportal");
            task(rocketLaunch, "Rocket assembly", "Engineering", true);
            task(rocketLaunch, "Satellite payload", "Engineering", true);
            task(rocketLaunch, "Launch site defense", "Security", false);
            task(rocketLaunch, "Countdown rehearsals", "Testing", false);
            participants(rocketLaunch,
                    "magnusson", "Project Manager", "kleiner", "Research Scientist", "freeman", "Engineer",
                    "odessa", "Security Officer", "shephard", "Security Officer");

            DemoProject citadelAudit = project(clients.get("Universal Union"), "Citadel Energy Audit",
                    ProjectStatus.OPEN, "An audit of the dark energy reactor of the Citadel");
            task(citadelAudit, "Reactor survey", "Analysis", true);
            task(citadelAudit, "Audit report", "Management", false);
            participants(citadelAudit,
                    "admin", "Project Manager", "mossman", "Analyst", "barney", "Security Officer");
        }

        private DemoProject project(Client client, String name, ProjectStatus status,
                                    String description) {
            Project project = dataManager.create(Project.class);
            project.setClient(client);
            project.setName(name);
            project.setStatus(status);
            project.setDescription(description);
            context.saving(project);

            tagGroup(project, "Activity", "Lab work", "Field work", "Meeting", "Paperwork");
            TagGroup billing = tagGroup(project, "Billing", "Billable", "Non-billable");
            return new DemoProject(project, billing);
        }

        private TagGroup tagGroup(Project project, String name, String... tagNames) {
            TagGroup group = dataManager.create(TagGroup.class);
            group.setProject(project);
            group.setName(name);
            context.saving(group);
            for (String tagName : tagNames) {
                Tag tag = dataManager.create(Tag.class);
                tag.setTagGroup(group);
                tag.setName(tagName);
                context.saving(tag);
            }
            return group;
        }

        private Task task(DemoProject project, String name, String type, boolean billingRequired) {
            Task task = dataManager.create(Task.class);
            task.setProject(project.project());
            task.setName(name);
            task.setType(taskTypes.get(type));
            if (billingRequired) {
                task.setRequiredTagGroups(new ArrayList<>(List.of(project.billing())));
            }
            context.saving(task);
            return task;
        }

        private void participants(DemoProject project, String... usernamesAndRoles) {
            for (int i = 0; i < usernamesAndRoles.length; i += 2) {
                ProjectParticipant participant = dataManager.create(ProjectParticipant.class);
                participant.setProject(project.project());
                participant.setUser(users.get(usernamesAndRoles[i]));
                participant.setRole(projectRoles.get(usernamesAndRoles[i + 1]));
                context.saving(participant);
            }
        }
    }

    private record DemoUser(String username, @Nullable String firstName, @Nullable String lastName,
                            String emailDomain, String avatar) {
    }

    private record DemoClient(String name, @Nullable String email, @Nullable String phone, @Nullable String url,
                              String address, @Nullable String logo) {
    }

    private record DemoProject(Project project, TagGroup billing) {
    }
}

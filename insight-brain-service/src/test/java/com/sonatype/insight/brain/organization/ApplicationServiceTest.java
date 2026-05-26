/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApplicationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private ApplicationService applicationService;

  private Organization org;

  private Application app1;

  private Application app2;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("Application 1", "app1", org.getId());
    app2 = tempEntity.newApplicationWithParent("app2", "Application 2");
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_NullParams() {
    List<Application> apps = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null /* organisationIds */, null /* applicationIds */,
            null /* tagIds */);
    assertThat(apps).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_EmptyParams() {
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.emptySet() /* organisationIds */, Collections.emptySet() /* applicationIds */,
        Collections.emptySet() /* tagIds */);
    assertThat(apps).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_AppId() {
    List<Application> apps = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Collections.singleton(app1.getId()), null /* tagIds */);
    assertThat(apps).extracting(Application::getId).containsExactlyInAnyOrder(app1.getId());
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgId() {
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(app1.getParentOwnerId()), null, null /* tagIds */);
    assertThat(apps).extracting(Application::getId).containsExactlyInAnyOrder(app1.getId());
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_TagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        null /* organisationIds */, null /* applicationIds */, Collections.singleton(tag.getId()));
    assertThat(apps).extracting(Application::getId).containsExactlyInAnyOrder(app2.getId());
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_AppIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null,
        Collections.singleton(app1.getId()), Collections.singleton(tag.getId()));
    assertThat(apps).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(app1.getParentOwnerId()), null, Collections.singleton(tag.getId()));
    assertThat(apps).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgWithNoChildrenAndNullTagIds() {
    Organization org = tempEntity.newOrganization();
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(org.getId()), null, null);
    assertThat(apps).isEmpty();
  }

  @Test
  public void testAddApplication_LicenseWithoutApplicationLimit() {
    // Given
    testProductLicense.setMaxApplications(null);
    Application app = new Application("appPublicId", "appName", org.getId());

    // When
    app = applicationService.addApplication(app);

    // Then
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    OwnerMaintenanceTelemetry ownerMaintenanceTelemetryData =
        (OwnerMaintenanceTelemetry) telemetryData.getAttributes()
            .get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY);
    assertThat(ownerMaintenanceTelemetryData).isNotNull();

    assertThat(ownerMaintenanceTelemetryData.getOwnerId()).isEqualTo(app.getId());
    assertThat(ownerMaintenanceTelemetryData.getOwnerName()).isEqualTo(app.getName());
    assertThat(ownerMaintenanceTelemetryData.getOwnerMaintenanceType()).isEqualTo(OwnerMaintenanceTelemetry.TYPE_ADD);
  }

  @Test
  public void testAddApplication_RootOrgIsNoValidParent() {
    Application app = new Application("appPublicId", "appName", Organization.ROOT_ORGANIZATION_ID);
    assertThatExceptionOfType(InvalidApplicationException.class)
        .isThrownBy(() -> applicationService.addApplication(app))
        .withMessageContaining("cannot have the root organization as parent");
  }

  @Test
  public void testAddApplication_addsUserToOwnerRole() {
    Organization org = tempEntity.newOrganization();
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    List<MembershipMapping> mappings = membershipMappingDAO.getByContextIdAndRoleId(app.getId(),
        Role.OWNER_ROLE_ID);
    assertThat(mappings).hasSize(1);
    assertThat(mappings.get(0).getMemberName()).isEqualTo(USERNAME);
    assertThat(mappings.get(0).getMemberType()).isEqualTo(MemberType.USER);
  }

  @Test
  public void testAddUpdateAndDeleteApplicationPostEvents() throws Exception {
    TestEventHandler<WebhookEvent> handler = new TestEventHandler<>(new CountDownLatch(2), WebhookEvent.class);
    eventBus.register(handler);

    Organization org = tempEntity.newOrganization();
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    final String applicationId = app.getId();

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    OwnerEvent ownerEvent = (OwnerEvent) handler.getAllEvents()
        .stream()
        .filter(event -> event instanceof OwnerEvent)
        .findFirst()
        .get();
    OrganizationApplicationManagementEvent orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents()
            .stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent)
            .findFirst()
            .get();

    assertThat(ownerEvent.action).isEqualTo(CREATED);
    assertThat(ownerEvent.ownerId).isEqualTo(applicationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(applicationId);
    assertThat(orgAppSummaryEvent.organizations).hasSize(3);
    assertThat(orgAppSummaryEvent.applications).hasSize(3);

    handler.setLatch(new CountDownLatch(2));

    app.setName("new appId");
    applicationService.updateApplication(app);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    ownerEvent = (OwnerEvent) handler.getAllEvents()
        .stream()
        .filter(event -> event instanceof OwnerEvent)
        .findFirst()
        .get();
    orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents()
            .stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent)
            .findFirst()
            .get();

    assertThat(ownerEvent.action).isEqualTo(UPDATED);
    assertThat(ownerEvent.ownerId).isEqualTo(applicationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(applicationId);
    assertThat(orgAppSummaryEvent.organizations).hasSize(3);
    assertThat(orgAppSummaryEvent.applications).hasSize(3);

    handler.setLatch(new CountDownLatch(2));

    applicationService.deleteApplicationByPublicId(app.getPublicId());

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    ownerEvent = (OwnerEvent) handler.getAllEvents()
        .stream()
        .filter(event -> event instanceof OwnerEvent)
        .findFirst()
        .get();
    orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents()
            .stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent)
            .findFirst()
            .get();

    assertThat(ownerEvent.action).isEqualTo(DELETED);
    assertThat(ownerEvent.ownerId).isEqualTo(applicationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(applicationId);
    assertThat(orgAppSummaryEvent.organizations).hasSize(3);
    assertThat(orgAppSummaryEvent.applications).hasSize(2);

    eventBus.unregister(handler);
  }

  @Test
  public void testAddApplication_NoOrganization() {
    String applicationPublicId = "testAddApplication_NoOrganization";
    String applicationName = "testAddApplication-NoOrganization";

    Application application = new Application();
    application.setName(applicationName);
    application.setPublicId(applicationPublicId);

    assertThatExceptionOfType(InvalidApplicationException.class)
        .isThrownBy(() -> applicationService.addApplication(application))
        .withMessageContaining("must have a parent organization");
  }

  @Test
  public void testGetApplicationIdsByOrganizationIds() {
    String sameOrgAppId = tempEntity.newApplication(org.getId()).getId();

    Set<String> applicationIds = applicationService
        .getApplicationIdsByOrganizationIds(Collections.singleton(org.getId()));
    assertThat(applicationIds).containsExactlyInAnyOrder(app1.getId(), sameOrgAppId);
  }

  @Test
  public void testGetApplicationIdsByOrganizationIds_null() {
    tempEntity.newApplication(org.getId()).getId();

    Set<String> applicationIds = applicationService.getApplicationIdsByOrganizationIds(null);
    assertThat(applicationIds).isNotNull();
  }

  @Test
  public void testDeleteApplicationByPublicId_ApplicationWithData() throws Exception {
    final String applicationId = app1.getId();

    final InsightWork insightWork = lookup(InsightWork.class);
    Files.createDirectories(insightWork.getScanDir(applicationId).toPath());
    Files.createDirectories(insightWork.getAuditDir(applicationId).toPath());
    Files.createDirectories(insightWork.getReportDir(applicationId).toPath());
    Files.createDirectories(insightWork.getSourceControlDir(applicationId).toPath());

    applicationService.deleteApplicationByPublicId(app1.getPublicId());
    assertThat(applicationDAO.getById(applicationId)).isNull();

    assertThat(insightWork.getScanDir(applicationId)).doesNotExist();
    assertThat(insightWork.getAuditDir(applicationId)).doesNotExist();
    assertThat(insightWork.getReportDir(applicationId)).doesNotExist();
    assertThat(insightWork.getSourceControlDir(applicationId)).doesNotExist();
  }

  @Test
  public void testDeleteApplicationByPublicId_NonExistingApplication() {
    String applicationPublicId = "NoSuchAppPublicId";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationService.deleteApplicationByPublicId(applicationPublicId))
        .withMessage("Could not find an application with public ID " + applicationPublicId + ".");
  }

  @Test
  public void testUpdateApplication_NoOrganization() {
    app1.setOrganizationId(null);

    assertThatExceptionOfType(InvalidApplicationException.class)
        .isThrownBy(() -> applicationService.updateApplication(app1))
        .withMessageContaining("not change the parent organization of an application");
  }

  @Test
  public void testUpdateApplication_ChangeOrganization() {
    app1.setOrganizationId("newOrganizationId");

    assertThatExceptionOfType(InvalidApplicationException.class)
        .isThrownBy(() -> applicationService.updateApplication(app1))
        .withMessageContaining("not change the parent organization of an application");
  }

  @Test
  public void testDeleteApplicationByPublicId_PolicyViolationLogger_LogsClearEvent() throws Exception {
    Date before = new Date();
    applicationService.deleteApplicationByPublicId(app1.getPublicId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(logOutput, 1);
    PolicyViolationLogDTOAssert
        .assertApplicationPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR, org, app1,
            before, after);
  }

  @Test
  public void testGetByPublicIdsNoAuthz() {
    List<Application> apps = applicationService.getByPublicIdsNoAuthz(Collections.singleton(app1.getPublicId()));
    assertThat(apps).extracting(Application::getPublicId).containsExactlyInAnyOrder(app1.getPublicId());
  }

  @Test
  public void testGetParentOrganizationsForApplicationsNoAuthz() {
    List<Application> applications = IntStream.range(0, 100)
        .mapToObj(number -> {
          String orgId = number % 2 == 0
              ? tempEntity.newOrganizationWithSpecificId("org" + number, "orgName" + number).getId()
              : "org" + (number - 1);
          return tempEntity.newApplication(orgId);
        })
        .collect(Collectors.toList());

    Set<Organization> orgs = applicationService.getParentOrganizationsForApplicationsNoAuthz(applications);
    List<String> orgIds = orgs.stream().map(Organization::getId).collect(Collectors.toList());
    assertThat(orgs).hasSize(50);
    applications.forEach(application -> assertThat(application.getOrganizationId()).isIn(orgIds));
  }

  @Test
  public void testGetParentOrganizationsForApplicationsNoAuthz_Null() {
    Set<Organization> orgs = applicationService.getParentOrganizationsForApplicationsNoAuthz(null);
    assertThat(orgs).isEmpty();
  }

  @Test
  public void testGetParentOrganizationsForApplicationsNoAuthz_Empty() {
    Set<Organization> orgs = applicationService.getParentOrganizationsForApplicationsNoAuthz(Collections.emptyList());
    assertThat(orgs).isEmpty();
  }

  @Test
  public void testGetApplicationsOrderedByName() {
    tempEntity.newApplicationWithParent("application-1", "Application Z1");
    tempEntity.newApplicationWithParent("application-2", "Application A3");
    tempEntity.newApplicationWithParent("application-3", "Application A2");
    tempEntity.newApplicationWithParent("application-4", "Application A1");
    tempEntity.newApplicationWithParent("application-5", "Application M1");

    assertThat(applicationService.getApplicationsOrderedByName().stream().map(a -> a.getName())).isEqualTo(
        Arrays.asList(
            "Application 1",
            "Application 2",
            "Application A1",
            "Application A2",
            "Application A3",
            "Application M1",
            "Application Z1"));
  }

  @Test
  public void testGetApplicationsWithoutRelatedRepositoriesOrderedByName() {
    tempEntity.newApplicationWithParent("application-1", "Application Z1");
    tempEntity.newApplicationWithParent("application-2", "Application A3");
    tempEntity.newApplicationWithParent("application-3", "Application A2");
    tempEntity.newApplicationWithParent("application-4", "Application A1");
    tempEntity.newApplicationWithParent("application-5", "Application M1");

    // Create an app with both a related repository manager and repository
    Organization orgWithRelatedRepo = tempEntity.newOrganizationWithRepositoryManager("org-with-repo");
    tempEntity.newApplication(orgWithRelatedRepo.getId());

    assertThat(applicationService.getApplicationsWithoutRelatedRepositoriesOrderedByName())
        .extracting(Nameable::getName)
        .containsExactly(
            "Application 1",
            "Application 2",
            "Application A1",
            "Application A2",
            "Application A3",
            "Application M1",
            "Application Z1");
  }

  @Test
  public void testGetApplicationByPublicIdForLegalReviewer() {
    Application application = tempEntity.newApplicationWithParent();
    Application result = applicationService.getApplicationByPublicIdForLegalReviewer(application.getPublicId());
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(application.getId());
    assertThat(result.getPublicId()).isEqualTo(application.getPublicId());
  }

  @Test
  public void testGetLatestReportInformation_ShouldReturnTheLatestReportWhenAReportExists() {
    // === Given ===
    // === App 1 Evaluations
    final String buildEval1ForApp1ScanId = UUID.randomUUID().toString();
    final Date buildEval1ForApp1Date = new GregorianCalendar(2024, Calendar.JANUARY, 2).getTime();

    final String buildEval2ForApp1ScanId = UUID.randomUUID().toString();
    final Date buildEval2ForApp1Date = new GregorianCalendar(2024, Calendar.JANUARY, 3).getTime();

    // build stage 1st evaluation for app1 (this should not get returned because it's not latest)
    tempEntity.newPolicyEvaluation(
        app1.getId(),
        "build",
        buildEval1ForApp1ScanId,
        buildEval1ForApp1Date);

    // build stage 2nd evaluation for app1 (this should be returned for app1/build)
    tempEntity.newPolicyEvaluation(
        app1.getId(),
        "build",
        buildEval2ForApp1ScanId,
        buildEval2ForApp1Date);

    final String releaseEvalForApp1ScanId = UUID.randomUUID().toString();
    final Date reeleaseEvalForApp1Date = new GregorianCalendar(2024, Calendar.JANUARY, 3).getTime();

    // a single release stage evaluation for app 1 (this should be returned for app1/release)
    tempEntity.newPolicyEvaluation(
        app1.getId(),
        "release",
        releaseEvalForApp1ScanId,
        reeleaseEvalForApp1Date);

    // a re-evaluation newer than our latest eval, this should be filtered out and not returned
    final String releaseReEvalForApp1ScanId = UUID.randomUUID().toString();
    final Date reeleaseReEvalForApp1Date = new GregorianCalendar(2024, Calendar.JANUARY, 20).getTime();
    tempEntity.newPolicyReEvaluation(
        app1.getId(),
        "release",
        releaseReEvalForApp1ScanId,
        reeleaseReEvalForApp1Date);

    // === App 2 Evaluations
    final String releaseEvalForApp2ScanId = UUID.randomUUID().toString();
    final Date reeleaseEvalForApp2Date = new GregorianCalendar(2024, Calendar.JANUARY, 3).getTime();

    // release stage 1st evaluation (this should be returned for app 2/release)
    tempEntity.newPolicyEvaluation(
        app2.getId(),
        "release",
        releaseEvalForApp2ScanId,
        reeleaseEvalForApp2Date);

    // === Then ===
    var results = applicationService.getLatestReportInformation(app1.getPublicId(), "build");
    assertThat(results).isEqualTo(new LatestReportInformation(buildEval2ForApp1ScanId, true));

    results = applicationService.getLatestReportInformation(app1.getPublicId(), "release");
    assertThat(results).isEqualTo(new LatestReportInformation(releaseEvalForApp1ScanId, true));

    results = applicationService.getLatestReportInformation(app1.getPublicId(), "release");
    assertThat(results).isEqualTo(new LatestReportInformation(releaseEvalForApp1ScanId, true));

    results = applicationService.getLatestReportInformation(app2.getPublicId(), "release");
    assertThat(results).isEqualTo(new LatestReportInformation(releaseEvalForApp2ScanId, true));
  }

  @Test
  public void testGetLatestReportInformation_ShouldReturnEntityIndicatingThereIsNoReportWhenNoneExists() {
    var results = applicationService.getLatestReportInformation(app1.getPublicId(), "build");
    assertThat(results).isEqualTo(new LatestReportInformation(null, false));
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_ExcludesDockerApplications() {
    // Create regular applications
    Application regularApp1 = tempEntity.newApplicationWithParent("regular-app-1", "Regular Application 1");
    Application regularApp2 = tempEntity.newApplicationWithParent("regular-app-2", "Regular Application 2");

    // Create library applications with -library- pattern (should be excluded)
    Application libraryApp1 = tempEntity.newApplicationWithParent("app-library-123", "Library Application 1");
    Application libraryApp2 = tempEntity.newApplicationWithParent("test-library-app", "Library Application 2");

    // Create applications with -docker- and -doc- patterns (should NOT be excluded)
    Application dockerApp1 = tempEntity.newApplicationWithParent("app-docker-456", "Docker Application 1");
    Application docApp1 = tempEntity.newApplicationWithParent("test-doc-app", "Doc Application 1");

    // When
    var applicationNames = applicationService.getApplicationNamesForEvaluateComponent();

    // Then - regular, docker, and doc applications should be included
    assertThat(applicationNames).containsKeys(
        app1.getPublicId(),
        app2.getPublicId(),
        regularApp1.getPublicId(),
        regularApp2.getPublicId(),
        dockerApp1.getPublicId(),
        docApp1.getPublicId());

    // Library applications should be excluded
    assertThat(applicationNames).doesNotContainKeys(
        libraryApp1.getPublicId(),
        libraryApp2.getPublicId());
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_IncludesApplicationWithLibraryInName() {
    // Create an application with "library" in the name but not in the ID pattern
    Application appWithLibraryInName = tempEntity.newApplicationWithParent(
        "regular-app-id",
        "My Library Management Application");

    // When
    var applicationNames = applicationService.getApplicationNamesForEvaluateComponent();

    // Then - should be included because the ID doesn't match the -library- pattern
    assertThat(applicationNames).containsKey(appWithLibraryInName.getPublicId());
    assertThat(applicationNames.get(appWithLibraryInName.getPublicId()))
        .isEqualTo("My Library Management Application");
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_HandlesEdgeCases() {
    // Create applications with edge cases
    Application app1 = tempEntity.newApplicationWithParent("libraryapp", "Library Without Dash");
    Application app2 = tempEntity.newApplicationWithParent("lib", "Just Lib");
    Application app3 = tempEntity.newApplicationWithParent("library", "Just Library");
    Application app4 = tempEntity.newApplicationWithParent("app-libraryfile-123", "Contains Libraryfile");
    Application app5 = tempEntity.newApplicationWithParent("dockerapp", "Docker Without Dash");
    Application app6 = tempEntity.newApplicationWithParent("doc", "Just Doc");

    // When
    var applicationNames = applicationService.getApplicationNamesForEvaluateComponent();

    // Then - these should all be included as they don't match the -library- pattern
    assertThat(applicationNames).containsKeys(
        app1.getPublicId(),
        app2.getPublicId(),
        app3.getPublicId(),
        app4.getPublicId(),
        app5.getPublicId(),
        app6.getPublicId());
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_LibraryPatternCaseSensitive() {
    // Create applications with uppercase library patterns
    Application app1 = tempEntity.newApplicationWithParent("app-LIBRARY-123", "Uppercase Library");
    Application app2 = tempEntity.newApplicationWithParent("app-Library-456", "Mixed Case Library");
    Application app3 = tempEntity.newApplicationWithParent("app-library-789", "Lowercase Library");

    // When
    var applicationNames = applicationService.getApplicationNamesForEvaluateComponent();

    // Then - uppercase and mixed case should be included (case-sensitive matching)
    assertThat(applicationNames).containsKeys(
        app1.getPublicId(),
        app2.getPublicId());

    // Lowercase -library- should be excluded
    assertThat(applicationNames).doesNotContainKey(app3.getPublicId());
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_MultipleLibraryPatternsInId() {
    // Create application with -library- pattern along with other patterns
    Application app1 = tempEntity.newApplicationWithParent(
        "app-library-test-doc-123",
        "Library with Other Patterns");

    // Create application with -docker- and -doc- but no -library-
    Application app2 = tempEntity.newApplicationWithParent(
        "app-docker-test-doc-456",
        "Docker and Doc without Library");

    // When
    var applicationNames = applicationService.getApplicationNamesForEvaluateComponent();

    // Then - app1 should be excluded because it contains -library-
    assertThat(applicationNames).doesNotContainKey(app1.getPublicId());

    // app2 should be included because it doesn't contain -library-
    assertThat(applicationNames).containsKey(app2.getPublicId());
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_ReturnsCorrectNames() {
    // Create applications with specific names
    Application app1 = tempEntity.newApplicationWithParent("test-app-1", "Test Application One");
    Application app2 = tempEntity.newApplicationWithParent("test-app-2", "Test Application Two");

    // When
    var applicationNames = applicationService.getApplicationNamesForEvaluateComponent();

    // Then - verify the names are correctly mapped
    assertThat(applicationNames.get(app1.getPublicId())).isEqualTo("Test Application One");
    assertThat(applicationNames.get(app2.getPublicId())).isEqualTo("Test Application Two");
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_EmptyWhenNoApplications() {
    // Delete all existing applications
    List<Application> allApps = applicationDAO.getAll();
    for (Application app : allApps) {
      applicationDAO.delete(app);
    }

    // When
    var applicationNames = applicationService.getApplicationNamesForEvaluateComponent();

    // Then
    assertThat(applicationNames).isEmpty();
  }
}

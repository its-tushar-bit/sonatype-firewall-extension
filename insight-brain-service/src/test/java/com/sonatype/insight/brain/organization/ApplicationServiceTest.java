/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
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

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ApplicationServiceTest
    extends AbstractComponentTest
{
  private static final int RESULTS_PER_PAGE = 50;

  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private ApplicationService applicationService;

  private Organization org;

  private Application app1;

  private Application app2;

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

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
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
    testProductLicense.setMaxApplications(null);
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testAddApplication_LicenseWithoutApplicationLimit_LookerEnabled() {
    // Given
    SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
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

    assertThat(ownerMaintenanceTelemetryData.getApplicationId()).isEqualTo(app.getId());
    assertThat(ownerMaintenanceTelemetryData.getApplicationName()).isEqualTo(app.getName());
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
    List<MembershipMapping> mappings = new MembershipMappingDAO().getByContextIdAndRoleId(app.getId(),
        Role.OWNER_ROLE_ID);
    assertThat(mappings).hasSize(1);
    assertThat(mappings.get(0).getMemberName()).isEqualTo(USERNAME);
    assertThat(mappings.get(0).getMemberType()).isEqualTo(MemberType.USER);
  }

  @Test
  public void testAddUpdateAndDeleteApplicationPostEvents() throws Exception {
    TestEventHandler<WebhookEvent> handler = new TestEventHandler<>(new CountDownLatch(2));
    eventBus.register(handler);

    Organization org = tempEntity.newOrganization();
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    final String applicationId = app.getId();

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    OwnerEvent ownerEvent = (OwnerEvent) handler.getAllEvents().stream().filter(event -> event instanceof OwnerEvent)
        .findFirst().get();
    OrganizationApplicationManagementEvent orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents().stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent).findFirst().get();

    assertThat(ownerEvent.action).isEqualTo(CREATED);
    assertThat(ownerEvent.ownerId).isEqualTo(applicationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(applicationId);
    assertThat(orgAppSummaryEvent.organizations).hasSize(3);
    assertThat(orgAppSummaryEvent.applications).hasSize(3);

    handler.setLatch(new CountDownLatch(2));

    app.setName("new appId");
    applicationService.updateApplication(app);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    ownerEvent = (OwnerEvent) handler.getAllEvents().stream().filter(event -> event instanceof OwnerEvent)
        .findFirst().get();
    orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents().stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent).findFirst().get();

    assertThat(ownerEvent.action).isEqualTo(UPDATED);
    assertThat(ownerEvent.ownerId).isEqualTo(applicationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(applicationId);
    assertThat(orgAppSummaryEvent.organizations).hasSize(3);
    assertThat(orgAppSummaryEvent.applications).hasSize(3);

    handler.setLatch(new CountDownLatch(2));

    applicationService.deleteApplicationByPublicId(app.getPublicId());

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    ownerEvent = (OwnerEvent) handler.getAllEvents().stream().filter(event -> event instanceof OwnerEvent)
        .findFirst().get();
    orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents().stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent).findFirst().get();

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
    assertThat(new ApplicationDAO().getById(applicationId)).isNull();

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
        }).collect(Collectors.toList());

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
            "Application Z1"
        )
    );
  }

  @Test
  public void testGetApplicationByPublicIdForLegalReviewer() {
    Application application = tempEntity.newApplicationWithParent();
    Application result = applicationService.getApplicationByPublicIdForLegalReviewer(application.getPublicId());
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(application.getId());
    assertThat(result.getPublicId()).isEqualTo(application.getPublicId());
  }

  private void createAlphabeticalOrgsAndApps(List<Organization> orgs, List<Application> apps) {
    orgs.addAll(new OrganizationDAO().getAll().stream()
        .filter(org -> !org.getId().equals(Organization.ROOT_ORGANIZATION_ID)).collect(Collectors.toList()));
    apps.addAll(new ApplicationDAO().getAll());
    int currentSize = apps.size();
    for (int result = 0; result < RESULTS_PER_PAGE + 1 - currentSize; result++) {
      String orgSuffix = getAlphabeticalSequenceElement(result);
      String appSuffix = getAlphabeticalSequenceElement(result + 1);
      Organization org = tempEntity.newOrganization("orgName" + orgSuffix);
      orgs.add(org);
      apps.add(tempEntity.newApplication("appName" + appSuffix, "appPublicId" + appSuffix, org.getId()));
    }
  }

  private String getAlphabeticalSequenceElement(int i) {
    return i < 0 ? "" : getAlphabeticalSequenceElement((i / 26) - 1) + (char) (65 + i % 26);
  }
}

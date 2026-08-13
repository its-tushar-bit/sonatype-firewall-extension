/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

@ComponentH2Test
public class ApplicationSummaryServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Inject
  private ApplicationSummaryService service;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Test
  public void testGetApplications_SortedByCaseInsensitiveName_EVALUATE_APPLICATION() throws Exception {
    testGetApplications_SortedByCaseInsensitiveName(Goal.EVALUATE_APPLICATION);
  }

  @Test
  public void testGetApplications_SortedByCaseInsensitiveName_EVALUATE_COMPONENT() throws Exception {
    testGetApplications_SortedByCaseInsensitiveName(Goal.EVALUATE_COMPONENT);
  }

  @Test
  public void testGetApplications_SortedByCaseInsensitiveName_VIEW_CIP() throws Exception {
    testGetApplications_SortedByCaseInsensitiveName(Goal.VIEW_CIP);
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesExist() {
    Application app = tempEntity.newApplicationWithParent();

    boolean result = service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");

    assertThat(result).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticApplicationCreationDisabled() {
    String appPublicId = "NoSuchAppPublicID";

    automaticApplicationsConfigurationDAO.setEnabled(false);

    boolean result = service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isFalse();

    // and:
    Organization org = tempEntity.newOrganization();
    result = service.verifyOrCreateApplication(appPublicId, org.getId(), Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticAppsEnabled_noOrgIdProvided() {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    boolean result = service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isTrue();

    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(automaticApplicationsConfigurationDAO.getOrganizationId());
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticAppsEnabled_orgIdProvided() {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    Organization org2 = tempEntity.newOrganization();
    boolean result = service.verifyOrCreateApplication(appPublicId, org2.getId(), Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isTrue();

    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(org2.getId());
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticAppsEnabled_wrongOrgIdProvided() {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    boolean result = service.verifyOrCreateApplication(appPublicId, "NoSuchOrgID", Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationExists_AutomaticAppsEnabled_differentOrgIdProvided() {
    Organization org1 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("app1-in-org1", org1.getId());

    // AutomaticApplications enabled with org1 as default org
    automaticApplicationsConfigurationDAO.setOrganizationId(org1.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    Organization org2 = tempEntity.newOrganization();
    boolean result = service.verifyOrCreateApplication(app1.getPublicId(), org2.getId(), Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isFalse();
  }

  private void testGetApplications_SortedByCaseInsensitiveName(Goal goal) {
    Application app1 = tempEntity.newApplicationWithParent("y", "AA");
    Application app0 = tempEntity.newApplicationWithParent("z", "a b");
    Application app2 = tempEntity.newApplicationWithParent("x", "c");

    ApplicationSummaryList applicationListDTO = service.getApplications(goal, null, null);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId(), app1.getId(), app2.getId());
  }

  @Test
  public void testVerifyOrCreateApplication_TelemetryData_AutomaticApplicationCreationDisabled() {
    // If auto app creation is disabled, then no telemetry data should be sent.
    automaticApplicationsConfigurationDAO.setEnabled(false);

    String appPublicId = "NoSuchAppPublicID";

    service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    verifyNoInteractions(telemetrySenderMock);

    Application app = tempEntity.newApplicationWithParent();
    service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testVerifyOrCreateApplication_TelemetryData_AutomaticApplicationCreationEnabled() throws Exception {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(telemetrySenderMock)
        .send(any(TelemetryData.class),
            eq("test_client_user_agent"));

    Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    String appPublicId = "NoSuchAppPublicID";

    // The app does not exist, so it will be created. We expect telemetry data that says the app was created
    // automatically.
    Date before = new Date();
    service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    Date after = new Date();
    assertTelemetryData(invocation[0], before, after, true, appPublicId);
    clearInvocations(telemetrySenderMock);

    // Toggle advanced reporting to make sure values are being obfuscated accordingly
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    // The app exists, but it doesn't have any evaluations. We expect telemetry data that says the app was not created
    // automatically.
    before = new Date();
    service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    after = new Date();
    assertTelemetryData(invocation[0], before, after, false, appPublicId);
    clearInvocations(telemetrySenderMock);

    // The app exists and it has evaluations. We don't expect any telemetry data.
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId");
    service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testVerifyOrCreateApplication_License() {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    testProductLicense.setMaxApplications(0);

    assertThatExceptionOfType(PaymentRequiredException.class).isThrownBy(
        () -> service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
            "test_client_user_agent"));
    assertThat(applicationDAO.getByPublicId(appPublicId)).isNull();

    testProductLicense.setMaxApplications(1);

    boolean result = service
        .verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    assertThat(result).isTrue();
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(automaticApplicationsConfigurationDAO.getOrganizationId());
  }

  private void assertTelemetryData(
      InvocationOnMock invocation,
      Date before,
      Date after,
      boolean expected,
      final String appPublicId)
  {
    TelemetryData telemetryData = invocation.getArgument(0);

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION);
    assertThat(telemetryData.getAttributes()).hasSize(2)
        .containsEntry(ApplicationSummaryService.APP_CREATED_AUTOMATICALLY_TELEMETRY_ATTR, String.valueOf(expected));
    assertThat(telemetryData.getTimestamp()).isGreaterThanOrEqualTo(before.getTime())
        .isLessThanOrEqualTo(after.getTime());

    OwnerMaintenanceTelemetry ownerMaintenanceTelemetryData =
        (OwnerMaintenanceTelemetry) telemetryData.getAttributes()
            .get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY);
    assertThat(ownerMaintenanceTelemetryData).isNotNull();

    final Application application = applicationDAO.getByPublicId(appPublicId);
    String applicationId = application.getId();
    String applicationName = application.getName();
    String parentOwnerId = application.getParentOwnerId();
    if (!configuration.getAdvanceReportingInsightsEnabled()) {
      applicationId = telemetryUtils.obfuscate(applicationId);
      applicationName = telemetryUtils.obfuscate(applicationName);
      parentOwnerId = telemetryUtils.obfuscate(parentOwnerId);
    }
    assertThat(ownerMaintenanceTelemetryData.getOwnerId()).isEqualTo(applicationId);
    assertThat(ownerMaintenanceTelemetryData.getOwnerName()).isEqualTo(applicationName);
    assertThat(ownerMaintenanceTelemetryData.getParentOwnerId()).isEqualTo(parentOwnerId);
    assertThat(ownerMaintenanceTelemetryData.getOwnerMaintenanceType()).isEqualTo(OwnerMaintenanceTelemetry.TYPE_ADD);
  }

  @Test
  public void testGetApplications_NoEnforcementFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    Application app1 = tempEntity.newApplicationWithParent("y", "AA");
    Application app0 = tempEntity.newApplicationWithParent("z", "a b");
    Application app2 = tempEntity.newApplicationWithParent("x", "c");

    ApplicationSummaryList applicationListDTO = service.getApplications(Goal.EVALUATE_APPLICATION, null, null);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId(), app1.getId(), app2.getId());
  }

  @Test
  public void testGetApplications_excludeFirewallForDocker() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    // Create an app with both a related repository manager and repository
    Organization orgWithRelatedRepo = tempEntity.newOrganizationWithRepositoryManager("org-with-repo");
    tempEntity.newApplication(orgWithRelatedRepo.getId());

    ApplicationSummaryList applicationListDTO = service.getApplications(Goal.EVALUATE_APPLICATION, null, null);

    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.getApplicationSummaries())
        .extracting(ApplicationSummary::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }

  @Test
  public void testGetApplications_NoEnforcementFeature_FromIDE() {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> service.getApplications(Goal.EVALUATE_COMPONENT, null, null));
  }

  @Test
  public void testGetApplicationsByOrganization_SortedByCaseInsensitiveName_EVALUATE_APPLICATION() throws Exception {
    testGetApplicationsByOrganization_SortedByCaseInsensitiveName(Goal.EVALUATE_APPLICATION);
  }

  @Test
  public void testGetApplicationsByOrganization_SortedByCaseInsensitiveName_EVALUATE_COMPONENT() throws Exception {
    testGetApplicationsByOrganization_SortedByCaseInsensitiveName(Goal.EVALUATE_COMPONENT);
  }

  @Test
  public void testGetApplicationsByOrganization_SortedByCaseInsensitiveName_VIEW_CIP() throws Exception {
    testGetApplicationsByOrganization_SortedByCaseInsensitiveName(Goal.VIEW_CIP);
  }

  @Test
  public void testGetApplicationsByOrganization_NoEnforcementFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    testGetApplicationsByOrganization_SortedByCaseInsensitiveName(Goal.EVALUATE_APPLICATION);
  }

  @Test
  public void testGetApplicationsByOrganization_NoEnforcementFeature_FromIDE() {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    Organization org = tempEntity.newOrganization("A");

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> service.getApplications(Goal.EVALUATE_COMPONENT, org.getId(), null));
  }

  @Test
  public void testAddApplicationPostEvent() throws Exception {
    final TestEventHandler<OrganizationApplicationManagementEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), OrganizationApplicationManagementEvent.class);
    eventBus.register(handler);

    final Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setEnabled(true);

    service.verifyOrCreateApplication("appPublicId", org.getId(), Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    final OrganizationApplicationManagementEvent event = handler.getEvent();

    assertThat(event).isNotNull();
    assertThat(event.applications).isNotEmpty();
    assertThat(event.organizations).isNotEmpty();

    eventBus.unregister(handler);
  }

  private void testGetApplicationsByOrganization_SortedByCaseInsensitiveName(Goal goal) {
    Organization org0 = tempEntity.newOrganization("A");
    Organization org1 = tempEntity.newOrganization("B");
    Application app0 = tempEntity.newApplication("x", "x", org0.getId());
    Application app1 = tempEntity.newApplication("y", "y", org0.getId());
    tempEntity.newApplication("z", org1.getId());
    tempEntity.newApplication("z1", org1.getId());

    ApplicationSummaryList applicationListDTO = service.getApplications(goal, org0.getId(), null);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId(), app1.getId());
  }
}

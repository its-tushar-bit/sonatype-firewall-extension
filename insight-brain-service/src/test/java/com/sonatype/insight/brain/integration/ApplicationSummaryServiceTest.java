/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

public class ApplicationSummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationSummaryService service;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

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
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticApplicationCreationDisabled()
      throws Exception
  {
    String appPublicId = "NoSuchAppPublicID";

    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = 
        new AutomaticApplicationsConfigurationDAO();
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
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = 
        new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    boolean result = service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isTrue();

    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(automaticApplicationsConfigurationDAO.getOrganizationId());
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticAppsEnabled_orgIdProvided() {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    Organization org2 = tempEntity.newOrganization();
    boolean result = service.verifyOrCreateApplication(appPublicId, org2.getId(), Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isTrue();

    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(org2.getId());
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticAppsEnabled_wrongOrgIdProvided() {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();
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
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org1.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    Organization org2 = tempEntity.newOrganization();
    boolean result = service.verifyOrCreateApplication(app1.getPublicId(), org2.getId(), Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isFalse();
  }

  private void testGetApplications_SortedByCaseInsensitiveName(Goal goal) throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("y", "AA");
    Application app0 = tempEntity.newApplicationWithParent("z", "a b");
    Application app2 = tempEntity.newApplicationWithParent("x", "c");

    ApplicationSummaryList applicationListDTO = service.getApplications(goal);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId(), app1.getId(), app2.getId());
  }

  @Test
  public void testVerifyOrCreateApplication_TelemetryData_AutomaticApplicationCreationDisabled() throws Exception {
    // If auto app creation is disabled, then no telemetry data should be sent.
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = 
        new AutomaticApplicationsConfigurationDAO();
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
    doAnswer(x -> invocation[0] = x).when(telemetrySenderMock).send(any(TelemetryData.class),
        eq("test_client_user_agent"));

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = 
        new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    String appPublicId = "NoSuchAppPublicID";

    // The app does not exist, so it will be created. We expect telemetry data that says the app was created
    // automatically.
    Date before = new Date();
    service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    Date after = new Date();
    assertTelemetryData(invocation[0], before, after, true);
    clearInvocations(telemetrySenderMock);

    // The app exists, but it doesn't have any evaluations. We expect telemetry data that says the app was not created
    // automatically.
    before = new Date();
    service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    after = new Date();
    assertTelemetryData(invocation[0], before, after, false);
    clearInvocations(telemetrySenderMock);

    // The app exists and it has evaluations. We don't expect any telemetry data.
    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId");
    service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testVerifyOrCreateApplication_License() throws Exception {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = 
        new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    testProductLicense.setMaxApplications(0);

    assertThatExceptionOfType(PaymentRequiredException.class).isThrownBy(
        () -> service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
            "test_client_user_agent"));
    assertThat(new ApplicationDAO().getByPublicId(appPublicId)).isNull();

    testProductLicense.setMaxApplications(1);

    boolean result = service
        .verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    assertThat(result).isTrue();
    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(automaticApplicationsConfigurationDAO.getOrganizationId());
  }

  private void assertTelemetryData(InvocationOnMock invocation, Date before, Date after, boolean expected)
      throws Exception
  {
    TelemetryData telemetryData = (TelemetryData) invocation.getArgument(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION);
    assertThat(telemetryData.getAttributes()).hasSize(1)
        .containsEntry(ApplicationSummaryService.APP_CREATED_AUTOMATICALLY_TELEMETRY_ATTR, String.valueOf(expected));
    assertThat(telemetryData.getTimestamp()).isGreaterThanOrEqualTo(before.getTime())
        .isLessThanOrEqualTo(after.getTime());
  }

  @Test
  public void testGetApplications_NoEnforcementFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    Application app1 = tempEntity.newApplicationWithParent("y", "AA");
    Application app0 = tempEntity.newApplicationWithParent("z", "a b");
    Application app2 = tempEntity.newApplicationWithParent("x", "c");

    ApplicationSummaryList applicationListDTO = service.getApplications(Goal.EVALUATE_APPLICATION);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId(), app1.getId(), app2.getId());
  }

  @Test
  public void testGetApplications_NoEnforcementFeature_FromIDE() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> service.getApplications(Goal.EVALUATE_COMPONENT));
  }
}

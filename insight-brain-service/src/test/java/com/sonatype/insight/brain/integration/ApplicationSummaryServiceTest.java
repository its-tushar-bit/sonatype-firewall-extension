/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.http.HttpEntity;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ApplicationSummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationSummaryService service;

  private HdsClient mockHdsClient = mock(HdsClient.class);

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
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
  public void testVerifyOrCreateApplication_ApplicationDoesExist() {
    Application app = tempEntity.newApplicationWithParent();

    boolean result = service.verifyOrCreateApplication(app.getPublicId(), Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");

    assertThat(result).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticApplicationCreationDisabled()
      throws Exception
  {
    String appPublicId = "NoSuchAppPublicID";

    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setEnabled(false);

    boolean result = service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticApplicationCreationEnabled()
      throws Exception
  {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    boolean result = service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent");
    assertThat(result).isTrue();

    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(automaticApplicationsConfigurationDAO.getOrganizationId());
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
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setEnabled(false);

    String appPublicId = "NoSuchAppPublicID";

    service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    verifyZeroInteractions(mockHdsClient);

    Application app = tempEntity.newApplicationWithParent();
    service.verifyOrCreateApplication(app.getPublicId(), Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    verifyZeroInteractions(mockHdsClient);
  }

  @Test
  public void testVerifyOrCreateApplication_TelemetryData_AutomaticApplicationCreationEnabled() throws Exception {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class),
        eq("test_client_user_agent"));

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    String appPublicId = "NoSuchAppPublicID";

    // The app does not exist, so it will be created. We expect telemetry data that says the app was created
    // automatically.
    Date before = new Date();
    service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    Date after = new Date();
    assertTelemetryData(invocation[0], before, after, true);
    clearInvocations(mockHdsClient);

    // The app exists, but it doesn't have any evaluations. We expect telemetry data that says the app was not created
    // automatically.
    before = new Date();
    service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    after = new Date();
    assertTelemetryData(invocation[0], before, after, false);
    clearInvocations(mockHdsClient);

    // The app exists and it has evaluations. We don't expect any telemetry data.
    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId");
    service.verifyOrCreateApplication(app.getPublicId(), Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    verifyZeroInteractions(mockHdsClient);
  }

  @Test
  public void testVerifyOrCreateApplication_License() throws Exception
  {
    String appPublicId = "NoSuchAppPublicID";

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    productLicenseManager.setApplicationLimit(0);
    clmLicenseManager.installLicense(null);

    assertThatExceptionOfType(PaymentRequiredException.class).isThrownBy(() -> {
      service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    });
    assertThat(new ApplicationDAO().getByPublicId(appPublicId)).isNull();

    productLicenseManager.setApplicationLimit(1);
    clmLicenseManager.installLicense(null);

    boolean result = service
        .verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION, "test_client_user_agent");
    assertThat(result).isTrue();
    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId()).isEqualTo(automaticApplicationsConfigurationDAO.getOrganizationId());
  }

  private void assertTelemetryData(InvocationOnMock invocation, Date before, Date after, boolean expected)
      throws Exception
  {
    assertThat(TelemetrySender.RESOURCE_PATH).isEqualTo(invocation.getArguments()[0]);
    HttpEntity httpEntity = (HttpEntity) invocation.getArguments()[1];
    ByteArrayDataSource multipartDataSource = new ByteArrayDataSource(httpEntity.getContent(), "multipart/form-data");
    MimeMultipart multipart = new MimeMultipart(multipartDataSource);
    BodyPart bodyPart = multipart.getBodyPart(0);
    String filename = bodyPart.getFileName();
    assertThat(TelemetrySender.ZIP_FILENAME).isEqualTo(filename);

    try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
      byte[] buffer = new byte[1024];

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
      zipInputStream.read(buffer);
      TelemetryHeader telemetryHeader = JsonUtils.parse(buffer, TelemetryHeader.class);
      assertThat(telemetryHeader.getCreateTime()).isAfterOrEqualsTo(before).isBeforeOrEqualsTo(after);

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
      zipInputStream.read(buffer);
      TelemetryData telemetryData = JsonUtils.parse(buffer, TelemetryData.class);
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION);
      assertThat(telemetryData.getAttributes()).hasSize(1)
          .containsEntry(ApplicationSummaryService.APP_CREATED_AUTOMATICALLY_TELEMETRY_ATTR, String.valueOf(expected));
      assertThat(telemetryData.getTimestamp()).isGreaterThanOrEqualTo(before.getTime())
          .isLessThanOrEqualTo(after.getTime());
    }
  }

  @Test
  public void testGetApplications_Foundation() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    clmLicenseManager.installLicense(null);
    Application app1 = tempEntity.newApplicationWithParent("y", "AA");
    Application app0 = tempEntity.newApplicationWithParent("z", "a b");
    Application app2 = tempEntity.newApplicationWithParent("x", "c");

    ApplicationSummaryList applicationListDTO = service.getApplications(Goal.EVALUATE_APPLICATION);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId(), app1.getId(), app2.getId());
  }

  @Test
  public void testGetApplications_Foundation_FromIDE() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    clmLicenseManager.installLicense(null);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> service.getApplications(Goal.EVALUATE_COMPONENT));
  }
}

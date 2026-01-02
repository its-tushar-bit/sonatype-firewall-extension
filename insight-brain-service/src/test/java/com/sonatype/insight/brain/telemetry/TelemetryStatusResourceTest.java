/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetryStatusResource.TelemetryStatusDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class TelemetryStatusResourceTest
    extends AbstractResourceTest
{
  @Mock
  private ApplicationDAO mockApplicationDAO;

  @Mock
  private ApplicationService mockApplicationService;

  @Mock
  private Configuration mockConfiguration;

  @Mock
  private ProductLicense mockProductLicense;

  @Mock
  private TelemetryId mockTelemetryId;

  private TelemetryStatusResource testSubject;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    testSubject = new TelemetryStatusResource(
        mockApplicationDAO,
        mockApplicationService,
        mockConfiguration,
        mockProductLicense,
        mockTelemetryId
    );
  }

  @Test
  public void testGetTelemetryStatus_h2() {
    // given:
    final var telemetryId = "telId-12345";
    final String h2ClusterId = null;
    final var advancedReportingEnabled = true;
    final var enterpriseReportingFeatureExists = true;
    final var userAppCount = 0;
    final var totalAppCount = 5;

    setupMocks(telemetryId, h2ClusterId, advancedReportingEnabled, enterpriseReportingFeatureExists, userAppCount,
        totalAppCount);

    // when:
    final var status = testSubject.getTelemetryStatus();

    // then:
    assertThat(status.telemetryId()).isEqualTo(telemetryId);
    assertThat(status.clusterId()).isEqualTo(h2ClusterId);
    assertThat(status.advancedReportingEnabled()).isEqualTo(advancedReportingEnabled);
    assertThat(status.enterpriseReportingFeatureExists()).isEqualTo(enterpriseReportingFeatureExists);
    assertThat(status.userApplicationCount()).isEqualTo(userAppCount);
  }

  @Test
  public void testGetTelemetryStatus_clusterIdMatch() {
    // given:
    final var telemetryId = "telId-12345";
    final var clusterId = "telId456-1234-abcdefg123";
    final var advancedReportingEnabled = false;
    final var enterpriseReportingFeatureExists = false;
    final var userAppCount = 7;
    final var totalAppCount = 15;

    setupMocks(telemetryId, clusterId, advancedReportingEnabled, enterpriseReportingFeatureExists, userAppCount,
        totalAppCount);

    // when:
    final var status = testSubject.getTelemetryStatus();

    // then:
    assertThat(status.telemetryId()).isEqualTo(telemetryId);
    assertThat(status.clusterId()).isEqualTo(clusterId);
    assertThat(status.advancedReportingEnabled()).isEqualTo(advancedReportingEnabled);
    assertThat(status.enterpriseReportingFeatureExists()).isEqualTo(enterpriseReportingFeatureExists);
    assertThat(status.userApplicationCount()).isEqualTo(userAppCount);
  }

  @Test
  public void testGetTelemetryStatus_uniqueClusterId() {
    // given:
    final var telemetryId = "telId-12345";
    final var clusterId = "other789-1234-abcdefg123";
    final var advancedReportingEnabled = false;
    final var enterpriseReportingFeatureExists = true;
    final var userAppCount = 10;
    final var totalAppCount = 20;

    setupMocks(telemetryId, clusterId, advancedReportingEnabled, enterpriseReportingFeatureExists, userAppCount,
        totalAppCount);

    // when:
    final var status = testSubject.getTelemetryStatus();

    // then:
    assertThat(status.telemetryId()).isEqualTo(telemetryId);
    assertThat(status.clusterId()).isEqualTo(clusterId);
    assertThat(status.advancedReportingEnabled()).isEqualTo(advancedReportingEnabled);
    assertThat(status.enterpriseReportingFeatureExists()).isEqualTo(enterpriseReportingFeatureExists);
    assertThat(status.userApplicationCount()).isEqualTo(userAppCount);
  }

  @Test
  public void testHttpGet() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(HttpStatus.SC_OK, response);

    var telemetryStatus = response.getBody(TelemetryStatusDTO.class);
    assertThat(telemetryStatus).isNotNull();
    assertThat(telemetryStatus.telemetryId()).isNotBlank();
    assertThat(telemetryStatus.clusterId()).isNotBlank();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(TelemetryStatusResource.TELEMETRY_STATUS_PATH);
  }

  private void setupMocks(
      String telemetryId,
      String clusterId,
      boolean advancedReportingEnabled,
      boolean enterpriseReportingFeatureExists,
      int userAppCount,
      long totalAppCount)
  {
    when(mockTelemetryId.getId()).thenReturn(telemetryId);
    when(mockTelemetryId.getClusterId()).thenReturn(clusterId);
    when(mockConfiguration.getAdvanceReportingInsightsEnabled()).thenReturn(advancedReportingEnabled);
    when(mockProductLicense.hasFeature(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING))
        .thenReturn(enterpriseReportingFeatureExists);
    when(mockApplicationService.getApplications()).thenReturn(Collections.nCopies(userAppCount,
        new Application("application-public-id", "Application Name", "organization-id-1234567890")
    ));
    when(mockApplicationDAO.getCount()).thenReturn(totalAppCount);
  }
}

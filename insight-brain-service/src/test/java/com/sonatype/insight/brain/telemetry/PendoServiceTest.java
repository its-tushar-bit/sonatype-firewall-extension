/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.hds.GainsightTelemetryClient;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PendoService.PendoConfig;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.google.common.hash.Hashing;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PendoServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PendoService pendoService;

  @Inject
  private TelemetryId telemetryId;

  @Inject
  private VersionService versionService;

  @Mock
  private GainsightTelemetryClient gainsightTelemetryClient;

  private String hashedVisitorId;

  @Mock
  private ProductLicense productLicense;

  @Override
  public void configure(Binder binder) {
    binder.bind(GainsightTelemetryClient.class).toInstance(gainsightTelemetryClient);
    binder.bind(ProductLicense.class).toInstance(productLicense);
    super.configure(binder);
  }

  @Before
  public void setup() {
    hashedVisitorId = Hashing.sha256().hashUnencodedChars(telemetryId.getId() + USERNAME).toString();
  }

  @Test
  public void testGetConfig() throws Exception {
    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(false);
    segmentInfo.segmentAttributes = Collections.singletonMap("foo", "bar");
    when(gainsightTelemetryClient.getWithTimeoutNoRetry(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));
    when(productLicense.getContactCompany()).thenReturn(null);

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", telemetryId.getId()).containsEntry("foo", "bar")
        .containsEntry("iq-server-version", versionService.getVersion());

    assertThat(config.visitor).containsEntry("id", hashedVisitorId);
  }

  @Test
  public void testGetConfig_disabled() throws Exception {
    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(true);
    when(gainsightTelemetryClient.getWithTimeoutNoRetry(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).isEmpty();
    assertThat(config.visitor).isEmpty();
  }

  @Test
  public void testGetConfig_unauthenticated() throws Exception {
    when(subject.getPrincipal()).thenReturn(null);

    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(false);
    segmentInfo.segmentAttributes = Collections.singletonMap("foo", "bar");
    when(gainsightTelemetryClient.getWithTimeoutNoRetry(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", telemetryId.getId()).containsEntry("foo", "bar")
        .containsEntry("iq-server-version", versionService.getVersion());

    assertThat(config.visitor).isEmpty();
  }

  @Test
  public void testGetConfig_error() {
    when(gainsightTelemetryClient.getWithTimeoutNoRetry(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenThrow(
        new NotFoundException("failed"));

    PendoConfig config = pendoService.getConfig();

    assertThat(config.visitor).containsEntry("id", hashedVisitorId);
    assertThat(config.account).containsEntry("id", telemetryId.getId());
  }

  @Test
  public void testGetConfig_TelemetryId_LicenseWithoutSalesforceIdWithCompanyName() {
    when(productLicense.getContactCompany()).thenReturn("Company A");
    String hashedCompanyName =
        Hashing.sha256().hashString(productLicense.getContactCompany(), StandardCharsets.UTF_8).toString();

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", hashedCompanyName);
  }

  @Test
  public void testGetConfig_TelemetryId_LicenseUnknownSalesforceIdWithCompanyName() throws Exception {
    when(productLicense.getContactCompany()).thenReturn("Company A");
    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(false);
    segmentInfo.segmentAttributes = Collections.singletonMap("iq_accountId", "UNKNOWN-62ec3ededa4a5d9e453f990cac348a");
    when(gainsightTelemetryClient.getWithTimeoutNoRetry(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));

    String hashedCompanyName =
        Hashing.sha256().hashString(productLicense.getContactCompany(), StandardCharsets.UTF_8).toString();

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", hashedCompanyName);
  }

  @Test
  public void testGetConfig_TelemetryId_LicenseWithSalesforceId() throws Exception {
    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(false);
    segmentInfo.segmentAttributes = Collections.singletonMap("iq_accountId", "sfAccountIdTest");
    when(gainsightTelemetryClient.getWithTimeoutNoRetry(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", "sfAccountIdTest");
  }

  @Test
  public void testGetJavascript() {
    when(gainsightTelemetryClient.getWithTimeoutNoRetry(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    byte[] javascript = pendoService.getJavascript();
    assertThat(new String(javascript, StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void proxyWithTimeout() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    InputStream mockContent = mock(InputStream.class);
    RelayResponse<InputStream> result = new RelayResponse<>(mockContent);
    when(gainsightTelemetryClient.relayNoRetry(eq(request), eq(InputStream.class), eq(PendoService
        .HDS_TELEMETRY_PATH + "/foo/bar"))).thenReturn(result);

    RelayResponse<InputStream> actual = pendoService.proxyWithoutRetry(request, "foo/bar");
    verify(gainsightTelemetryClient).relayNoRetry(eq(request), eq(InputStream.class),
        eq(PendoService.HDS_TELEMETRY_PATH + "/foo/bar"));
    assertThat(actual.contentType).isEqualTo(result.contentType);
    assertThat(actual.content).isEqualTo(result.content);
  }

  @Test
  public void testProxy_error() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(gainsightTelemetryClient.relayNoRetry(eq(request), eq(InputStream.class),
        eq(PendoService.HDS_TELEMETRY_PATH + "/foo/bar")))
        .thenThrow(new IOException());

    RelayResponse<InputStream> in = pendoService.proxyWithoutRetry(request, "foo/bar");
    assertThat(in).isNull();
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.hash.Hashing;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.PendoService.PendoConfig;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class PendoServiceTest
    extends AbstractComponentH2Test
{
  private PendoService pendoService;

  @Inject
  private TelemetryId telemetryId;

  @Inject
  private VersionService versionService;

  @Inject
  private ObjectMapper objectMapper;

  @Mock
  private HdsClient hdsClient;

  private String hashedVisitorId;

  @Mock
  private ProductLicense productLicense;

  @BeforeEach
  public void setup() {
    PendoCache pendoCache = new PendoCache(objectMapper, hdsClient);
    pendoService =
        new PendoService(hdsClient, pendoCache, telemetryId, versionService, new CurrentUser(), productLicense);
    hashedVisitorId = Hashing.sha256().hashUnencodedChars(telemetryId.getId() + USERNAME).toString();
  }

  @Test
  public void testGetConfig() throws Exception {
    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(false);
    segmentInfo.segmentAttributes = Collections.singletonMap("foo", "bar");
    when(hdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));
    when(productLicense.getContactCompany()).thenReturn(null);

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", telemetryId.getId())
        .containsEntry("foo", "bar")
        .containsEntry("iq-server-version", versionService.getVersion());

    assertThat(config.visitor).containsEntry("id", hashedVisitorId);
  }

  @Test
  public void testGetConfig_disabled() throws Exception {
    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(true);
    when(hdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
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
    when(hdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", telemetryId.getId())
        .containsEntry("foo", "bar")
        .containsEntry("iq-server-version", versionService.getVersion());

    assertThat(config.visitor).isEmpty();
  }

  @Test
  public void testGetConfig_error() {
    when(hdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenThrow(new NotFoundException("failed"));

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
    when(hdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
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
    when(hdsClient.get(InputStream.class, TelemetrySender.RESOURCE_PATH)).thenReturn(
        new ByteArrayInputStream(JsonUtils.generate(segmentInfo)));

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", "sfAccountIdTest");
  }

  @Test
  public void testGetJavascript() {
    when(hdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    byte[] javascript = pendoService.getJavascript();
    assertThat(new String(javascript, StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testProxy() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    RelayResponse<InputStream> result = new RelayResponse<>(mock(InputStream.class));

    when(hdsClient.relay(eq(request), eq(InputStream.class), eq(PendoService.HDS_TELEMETRY_PATH + "/foo/bar")))
        .thenReturn(result);

    assertThat(pendoService.proxy(request, "foo/bar")).isEqualTo(result);
    verify(hdsClient).relay(eq(request), eq(InputStream.class), eq(PendoService.HDS_TELEMETRY_PATH + "/foo/bar"));
  }

  @Test
  public void testProxy_error() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(hdsClient.relay(eq(request), eq(InputStream.class), eq(PendoService.HDS_TELEMETRY_PATH + "/foo/bar")))
        .thenThrow(new IOException());

    try (InputStream in = pendoService.proxy(request, "foo/bar").content) {
      assertThat(in).hasContent("");
    }
  }
}

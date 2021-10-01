/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PendoService.PendoConfig;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.NotFoundException;
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
  private HdsClient hdsClient;

  private String hashedVisitorId;

  @Mock
  private ProductLicense productLicense;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
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
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH)).thenReturn(segmentInfo);
    when(productLicense.getContactCompany()).thenReturn(null);
    when(productLicense.getSalesforceAccountId()).thenReturn(null);

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", telemetryId.getId()).containsEntry("foo", "bar")
        .containsEntry("iq-server-version", versionService.getVersion());

    assertThat(config.visitor).containsEntry("id", hashedVisitorId);
  }

  @Test
  public void testGetConfig_disabled() throws Exception {
    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(true);
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH)).thenReturn(segmentInfo);

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).isEmpty();
    assertThat(config.visitor).isEmpty();
  }

  @Test
  public void testGetConfig_unauthenticated() throws Exception {
    when(subject.getPrincipal()).thenReturn(null);

    CustomerTelemetryProperties segmentInfo = new CustomerTelemetryProperties(false);
    segmentInfo.segmentAttributes = Collections.singletonMap("foo", "bar");
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH)).thenReturn(segmentInfo);

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", telemetryId.getId()).containsEntry("foo", "bar")
        .containsEntry("iq-server-version", versionService.getVersion());

    assertThat(config.visitor).isEmpty();
  }

  @Test
  public void testGetConfig_error() throws Exception {
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH))
        .thenThrow(new NotFoundException("failed"));

    PendoConfig config = pendoService.getConfig();

    assertThat(config.visitor).containsEntry("id", hashedVisitorId);
    assertThat(config.account).containsEntry("id", telemetryId.getId());
  }

  @Test
  public void testGetConfig_TelemetryId_LicenseWithoutSalesforceIdWithCompanyName() throws Exception {
    when(productLicense.getContactCompany()).thenReturn("Company A");
    when(productLicense.getSalesforceAccountId()).thenReturn(null);
    String hashedCompanyName =
        Hashing.sha256().hashString(productLicense.getContactCompany(), StandardCharsets.UTF_8).toString();

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", hashedCompanyName);
  }

  @Test
  public void testGetConfig_TelemetryId_LicenseWithSalesforceId() throws Exception {
    when(productLicense.getSalesforceAccountId()).thenReturn("sfAccountIdTest");

    PendoConfig config = pendoService.getConfig();
    assertThat(config.account).containsEntry("id", "sfAccountIdTest");
  }

  @Test
  public void testGetJavascript() throws Exception {
    when(hdsClient.get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    File javascript = pendoService.getJavascript();
    assertThat(javascript).isFile().hasContent("test");
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

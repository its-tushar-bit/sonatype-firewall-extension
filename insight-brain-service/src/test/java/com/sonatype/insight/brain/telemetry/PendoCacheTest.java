/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PendoCacheTest
{
  @Mock
  private HdsClient hdsClient;

  private PendoCache pendoCache;

  @Before
  public void setup() {
    pendoCache = new PendoCache(hdsClient);
  }

  @Test
  public void testGetJs() {
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH))
        .thenReturn(new CustomerTelemetryProperties(false));
    when(hdsClient.get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    File file = pendoCache.getJs();
    assertThat(file).hasContent("test");
  }

  @Test
  public void testGetJs_telemetryDisabled() {
    CustomerTelemetryProperties properties = new CustomerTelemetryProperties(true);
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH)).thenReturn(properties);

    File file = pendoCache.getJs();
    assertThat(file).isNull();
    verify(hdsClient, never()).get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH);
  }

  @Test
  public void testGetJs_FailToGetTelemetryProperties() {
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH))
        .thenThrow(new BadGatewayException(""));
    when(hdsClient.get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    assertThat(pendoCache.getJs()).hasContent("test");
  }

  @Test
  public void testGetJs_FailToGetJsFile() {
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH))
        .thenReturn(new CustomerTelemetryProperties(false));
    when(hdsClient.get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH)).thenThrow(new NotFoundException(""));

    assertThat(pendoCache.getJs()).isNull();
    verify(hdsClient).get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH);
  }

  @Test
  public void testGetCustomerTelemetryProperties() {
    CustomerTelemetryProperties properties = new CustomerTelemetryProperties(true);
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH)).thenReturn(properties);

    assertThat(properties).isEqualTo(pendoCache.getCustomerTelemetryProperties());
  }

  @Test
  public void testGetCustomerTelemetryProperties_error() {
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH))
        .thenThrow(new BadGatewayException(""));

    CustomerTelemetryProperties properties = pendoCache.getCustomerTelemetryProperties();
    assertThat(properties).isNotNull();
    assertThat(properties.disabled).isNull();
  }
}

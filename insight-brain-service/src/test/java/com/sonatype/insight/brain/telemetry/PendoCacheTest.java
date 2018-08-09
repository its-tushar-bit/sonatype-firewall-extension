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
import java.nio.charset.Charset;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import org.apache.directory.api.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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
  public void testJsCache() throws IOException {
    when(hdsClient.get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    File file = pendoCache.getJs();
    assertThat(FileUtils.readFileToString(file, Charset.defaultCharset()), is("test"));
  }

  @Test
  public void testJsCache_telemetryDisabled() {
    CustomerTelemetryProperties properties = new CustomerTelemetryProperties(true);
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH)).thenReturn(properties);
    pendoCache.getCustomerTelemetryProperties();

    File file = pendoCache.getJs();
    assertNull(file);
    verify(hdsClient, never()).get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH);
  }

  @Test
  public void testJsCache_error() {
    when(hdsClient.get(InputStream.class, PendoCache.HDS_PENDO_JS_PATH)).thenThrow(new NotFoundException(""));

    assertNull(pendoCache.getJs());
  }

  @Test
  public void testGetCustomerTelemetryProperties() {
    CustomerTelemetryProperties properties = new CustomerTelemetryProperties(true);
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH)).thenReturn(properties);

    assertThat(properties, is(pendoCache.getCustomerTelemetryProperties()));
  }

  @Test
  public void testGetCustomerTelemetryProperties_error() {
    when(hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH))
        .thenThrow(new BadGatewayException(""));

    CustomerTelemetryProperties properties = pendoCache.getCustomerTelemetryProperties();
    assertNotNull(properties);
    assertNull(properties.disabled);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PendoCacheTest
    extends AbstractComponentTest
{
  private static final byte[] DISABLED_TELEMETRY_RESPONSE =
      "{ \"disabled\": true, \"segmentAttributes\": {} }".getBytes();

  private static final byte[] BASIC_ENABLED_TELEMETRY_RESPONSE =
      "{ \"segmentAttributes\": { \"foo\": \"bar\"} }".getBytes();

  @Mock
  private HdsClient mockHdsClient;

  @Inject
  private PendoCache pendoCache;

  @Inject
  private ObjectMapper objectMapper;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Before
  public void reset() {
    pendoCache.invalidateAll();
  }

  @Test
  public void testGetJs() throws Exception {
    when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    byte[] fileContent = pendoCache.getJs();
    assertThat(new String(fileContent, StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testGetJs_telemetryDisabled() throws Exception {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats")).thenReturn(
        new ByteArrayInputStream(DISABLED_TELEMETRY_RESPONSE));

    byte[] fileContent = pendoCache.getJs();
    assertThat(fileContent).isNull();
    verify(mockHdsClient, never()).get(InputStream.class, "user-telemetry.js");
  }

  @Test
  public void testGetJs_FailToGetTelemetryProperties() {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenThrow(new BadGatewayException(""));
    when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testGetJs_FailToGetJsFile() throws Exception {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));
    when(mockHdsClient.get(InputStream.class, "user-telemetry.js")).thenThrow(new NotFoundException(""));

    assertThat(pendoCache.getJs()).isNull();
    verify(mockHdsClient).get(InputStream.class, "user-telemetry.js");
  }

  @Test
  public void testGetJs_MultiTenant() throws Exception {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));

    when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    Tenant tenant1 = testAsNewTenant(testName, (Tenant t1) -> {
      pendoCache.getJs();
    });

    lenient().when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("never".getBytes()));

    Tenant tenant2 = testAsNewTenant(testName, (Tenant t2) -> {
      pendoCache.getJs();
    });

    testAsTenant(tenant1, (Tenant t1) -> {
      assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("test");
    });

    testAsTenant(tenant2, (Tenant t2) -> {
      // tenant 2 sees the "test" value cached when tenant1 did their query, and doesn't fetch the "never" value
      assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("test");
    });
  }

  @Test
  @Category(SlowTest.class)
  public void testGetJs_MultiTenant_Expiration() throws Exception {
    PendoCache pendoCache = new PendoCache(objectMapper, mockHdsClient, Duration.ofSeconds(2));

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));

    when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    Tenant tenant1 = testAsNewTenant(testName, (Tenant t1) -> {
      pendoCache.getJs();
    });

    when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("later".getBytes()));

    Tenant tenant2 = testAsNewTenant(testName, (Tenant t2) -> {
      pendoCache.getJs();
    });

    testAsTenant(tenant1, (Tenant t1) -> {
      assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("test");
    });

    testAsTenant(tenant2, (Tenant t2) -> {
      // tenant 2 sees the "test" value cached when tenant1 did their query, and doesn't fetch the "never" value
      assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("test");
    });

    Thread.sleep(2100);
    testAsTenant(tenant2, (Tenant t2) -> {
      assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("later");
    });

    testAsTenant(tenant1, (Tenant t1) -> {
      assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("later");
    });
  }

  @Test
  public void testGetCustomerTelemetryProperties() throws Exception {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats")).thenReturn(
        new ByteArrayInputStream(DISABLED_TELEMETRY_RESPONSE));

    var result = pendoCache.getCustomerTelemetryProperties();
    assertThat(result.disabled).isTrue();
    assertThat(result.segmentAttributes).isEmpty();
  }

  @Test
  public void testGetCustomerTelemetryProperties_error() {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenThrow(new BadGatewayException(""));

    CustomerTelemetryProperties properties = pendoCache.getCustomerTelemetryProperties();
    assertThat(properties).isNotNull();
    assertThat(properties.disabled).isNull();
    assertThat(properties.segmentAttributes).isEmpty();
  }

  @Test
  public void testGetCustomerTelemetryProperties_MultiTenant() throws Exception {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));

    Tenant tenant1 = testAsNewTenant(testName, (Tenant t1) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
    });

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(DISABLED_TELEMETRY_RESPONSE));

    Tenant tenant2 = testAsNewTenant(testName, (Tenant t2) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().disabled).isTrue();
    });

    testAsTenant(tenant1, (Tenant t1) -> {
      // original value is cached
      assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
    });

    // no effect - old value is cached
    lenient().when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));

    testAsTenant(tenant2, (Tenant t2) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().disabled).isTrue();
    });
  }

  @Test
  @Category(SlowTest.class)
  public void testGetCustomerTelemetryProperties_MultiTenant_Expiration() throws Exception {
    PendoCache pendoCache = new PendoCache(objectMapper, mockHdsClient, Duration.ofSeconds(2));

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));

    Tenant tenant1 = testAsNewTenant(testName, (Tenant t1) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
    });

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(DISABLED_TELEMETRY_RESPONSE));

    Tenant tenant2 = testAsNewTenant(testName, (Tenant t2) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().disabled).isTrue();
    });

    testAsTenant(tenant1, (Tenant t1) -> {
      // original value is cached
      assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
    });

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));

    testAsTenant(tenant2, (Tenant t2) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().disabled).isTrue();
    });

    Thread.sleep(2100);

    // after cache times out, each tenant should independently fetch a new value
    testAsTenant(tenant2, (Tenant t2) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
    });

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(DISABLED_TELEMETRY_RESPONSE));

    testAsTenant(tenant1, (Tenant t1) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().disabled).isTrue();
    });

    testAsTenant(tenant2, (Tenant t2) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
    });
  }

  @Test
  public void testProductLicenseChanged() throws Exception {
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(DISABLED_TELEMETRY_RESPONSE));
    assertThat(pendoCache.getJs()).isNull();
    pendoCache.productLicenseChanged();

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));
    when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
    byte[] fileContent = pendoCache.getJs();
    assertThat(new String(fileContent, StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testProductLicenseChanged_MultiTenant() throws Exception {
    // Initial values cached prior to the first `productLicenseChanged` call. The JS file is cached
    // globally and does not get invalidated when the product license changes. The telemetry properties
    // are cached per-tenant and do get invalidated when the product license changes.
    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));
    when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));

    assertThat(pendoCache.getJs()).isNotNull();

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(DISABLED_TELEMETRY_RESPONSE));
    lenient().when(mockHdsClient.get(InputStream.class, "user-telemetry.js"))
        .thenReturn(new ByteArrayInputStream("never".getBytes()));

    // trigger cache invalidation in tenant1 and then refresh cached values (JS file doesn't actually refresh)
    Tenant tenant1 = testAsNewTenant(testName, (Tenant t1) -> {
      pendoCache.productLicenseChanged();
      pendoCache.getCustomerTelemetryProperties();
      pendoCache.getJs();
    });

    when(mockHdsClient.get(InputStream.class, "rest/environment/stats"))
        .thenReturn(new ByteArrayInputStream(BASIC_ENABLED_TELEMETRY_RESPONSE));

    // trigger cache invalidation in tenant2 and then refresh cached values (JS file doesn't actually refresh)
    Tenant tenant2 = testAsNewTenant(testName, (Tenant t2) -> {
      pendoCache.productLicenseChanged();
      pendoCache.getCustomerTelemetryProperties();
      pendoCache.getJs();
    });

    // tenant 1 should see the original JS and the segment properties that were fetched after their product
    // license was refreshed. They should not see the segment properties that were fetched after tenant 2's product
    // license was refreshed.
    testAsTenant(tenant1, (Tenant t1) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().disabled).isTrue();
      assertThat(pendoCache.getJs()).isNull();
    });

    // tenant 2 should see the original JS and the segment properties that were fetched after their product
    // license was refreshed.
    testAsTenant(tenant2, (Tenant t2) -> {
      assertThat(pendoCache.getCustomerTelemetryProperties().segmentAttributes.get("foo")).isEqualTo("bar");
      assertThat(new String(pendoCache.getJs(), StandardCharsets.UTF_8)).isEqualTo("test");
    });
  }
}

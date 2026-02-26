/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.client.utils.ApiMetricsRecorder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmApiMetricsTest
{
  private MeterRegistry meterRegistry;

  private ScmApiMetrics underTest;

  @Before
  public void setup() throws Exception {
    TenantTestHelper.resetAfterTest();
    ApiMetricsRecorder.registerMetricsListener(null);
    meterRegistry = new SimpleMeterRegistry();
    underTest = new ScmApiMetrics(meterRegistry);
  }

  @After
  public void cleanup() throws Exception {
    underTest.stop();
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void testStart_singleTenantMode_doesNotRegisterListener() {
    underTest.start();

    ApiMetricsRecorder.recordApiCall("github", "user1");

    Counter counter = meterRegistry.find("scm.api.calls").counter();
    assertThat(counter).isNull();
  }

  @Test
  public void testOnApiCall_includesTenantIdTag() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onApiCall("github", "bot");
    });

    Counter counter = meterRegistry.find("scm.api.calls")
        .tag("tenant_id", "acme-corp")
        .tag("client_id", "github")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onApiCall("github", "bot");
    });

    assertThat(counter.count()).isEqualTo(2.0);
  }

  @Test
  public void testOnApiAuthFailure_includesTenantIdTag() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("widgets-inc", tenant -> {
      underTest.onApiAuthFailure("github", "bot");
    });

    Counter counter = meterRegistry.find("scm.api.auth.failures")
        .tag("tenant_id", "widgets-inc")
        .tag("client_id", "github")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);

    TenantTestHelper.testAsNewTenant("widgets-inc", tenant -> {
      underTest.onApiAuthFailure("github", "bot");
    });

    assertThat(counter.count()).isEqualTo(2.0);
  }

  @Test
  public void testOnApiCall_multipleTenants_trackedIndependently() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("tenant-a", tenant -> {
      underTest.onApiCall("github", "bot");
      underTest.onApiCall("github", "bot");
      underTest.onApiCall("github", "bot");
    });

    TenantTestHelper.testAsNewTenant("tenant-b", tenant -> {
      underTest.onApiCall("gitlab", "admin");
      underTest.onApiCall("gitlab", "admin");
    });

    Counter counterA = meterRegistry.find("scm.api.calls")
        .tag("tenant_id", "tenant-a")
        .tag("client_id", "github")
        .counter();
    assertThat(counterA).isNotNull();
    assertThat(counterA.count()).isEqualTo(3.0);

    Counter counterB = meterRegistry.find("scm.api.calls")
        .tag("tenant_id", "tenant-b")
        .tag("client_id", "gitlab")
        .counter();
    assertThat(counterB).isNotNull();
    assertThat(counterB.count()).isEqualTo(2.0);
  }

  @Test
  public void testOnApiCall_nullUserId_doesNotThrow() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onApiCall("github", null);
    });

    Counter counter = meterRegistry.find("scm.api.calls")
        .tag("tenant_id", "acme-corp")
        .tag("client_id", "github")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
    assertThat(counter.getId().getTag("user_id")).isNull();
  }

  @Test
  public void testOnApiAuthFailure_nullUserId_doesNotThrow() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onApiAuthFailure("github", null);
    });

    Counter counter = meterRegistry.find("scm.api.auth.failures")
        .tag("tenant_id", "acme-corp")
        .tag("client_id", "github")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
    assertThat(counter.getId().getTag("user_id")).isNull();
  }

  @Test
  public void testWithNullMeterRegistry_doesNotThrow() {
    ScmApiMetrics nullMetrics = new ScmApiMetrics(null);
    nullMetrics.onApiCall("github", "user1");
    nullMetrics.onApiAuthFailure("github", "user1");
  }

  @Test
  public void testStart_registersAsListener() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("test-tenant", tenant -> {
      ApiMetricsRecorder.recordApiCall("github", "user1");
    });

    Counter counter = meterRegistry.find("scm.api.calls")
        .tag("client_id", "github")
        .tag("tenant_id", "test-tenant")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  public void testStop_unregistersListener() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();
    underTest.stop();

    TenantTestHelper.testAsNewTenant("test-tenant", tenant -> {
      ApiMetricsRecorder.recordApiCall("github", "user1");
    });

    Counter counter = meterRegistry.find("scm.api.calls").counter();
    assertThat(counter).isNull();
  }
}

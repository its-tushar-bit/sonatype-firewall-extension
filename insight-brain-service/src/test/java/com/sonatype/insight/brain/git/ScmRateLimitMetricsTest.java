/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.client.utils.RateLimitRecorder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmRateLimitMetricsTest
{
  private MeterRegistry meterRegistry;

  private ScmRateLimitMetrics underTest;

  @BeforeEach
  public void setup() throws Exception {
    TenantTestHelper.resetAfterTest();
    RateLimitRecorder.fetchAndResetDailyRateLimitData();
    meterRegistry = new SimpleMeterRegistry();
    underTest = new ScmRateLimitMetrics(meterRegistry);
  }

  @AfterEach
  public void cleanup() throws Exception {
    underTest.stop();
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void testStart_singleTenantMode_doesNotRegisterListener() {
    underTest.start();

    RateLimitRecorder.recordApiRateLimitRemaining("github", "user1", 500);

    Counter counter = meterRegistry.find("scm.rate.limit.calls").counter();
    assertThat(counter).isNull();
  }

  @Test
  public void testOnRateLimitRemaining_includesTenantIdTag() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onRateLimitRemaining("github", "bot", 100);
    });

    Counter counter = meterRegistry.find("scm.rate.limit.calls")
        .tag("tenant_id", "acme-corp")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);

    Gauge gauge = meterRegistry.find("scm.rate.limit.remaining")
        .tag("tenant_id", "acme-corp")
        .gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(100.0);

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onRateLimitRemaining("github", "bot", 50);
    });

    assertThat(counter.count()).isEqualTo(2.0);
    assertThat(gauge.value()).isEqualTo(50.0);
  }

  @Test
  public void testOnRateLimitExceeded_includesTenantIdTag() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("widgets-inc", tenant -> {
      underTest.onRateLimitExceeded("github", "bot");
    });

    Counter counter = meterRegistry.find("scm.rate.limit.exceeded")
        .tag("tenant_id", "widgets-inc")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);

    TenantTestHelper.testAsNewTenant("widgets-inc", tenant -> {
      underTest.onRateLimitExceeded("github", "bot");
    });

    assertThat(counter.count()).isEqualTo(2.0);
  }

  @Test
  public void testOnRateLimitRemaining_multipleTenants_trackedIndependently() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("tenant-a", tenant -> {
      underTest.onRateLimitRemaining("github", "bot", 500);
      underTest.onRateLimitRemaining("github", "bot", 400);
      underTest.onRateLimitRemaining("github", "bot", 300);
    });

    TenantTestHelper.testAsNewTenant("tenant-b", tenant -> {
      underTest.onRateLimitRemaining("gitlab", "admin", 1000);
      underTest.onRateLimitRemaining("gitlab", "admin", 900);
    });

    Counter counterA = meterRegistry.find("scm.rate.limit.calls")
        .tag("tenant_id", "tenant-a")
        .tag("client_id", "github")
        .counter();
    assertThat(counterA).isNotNull();
    assertThat(counterA.count()).isEqualTo(3.0);

    Gauge gaugeA = meterRegistry.find("scm.rate.limit.remaining")
        .tag("tenant_id", "tenant-a")
        .tag("client_id", "github")
        .gauge();
    assertThat(gaugeA).isNotNull();
    assertThat(gaugeA.value()).isEqualTo(300.0);

    Counter counterB = meterRegistry.find("scm.rate.limit.calls")
        .tag("tenant_id", "tenant-b")
        .tag("client_id", "gitlab")
        .counter();
    assertThat(counterB).isNotNull();
    assertThat(counterB.count()).isEqualTo(2.0);

    Gauge gaugeB = meterRegistry.find("scm.rate.limit.remaining")
        .tag("tenant_id", "tenant-b")
        .tag("client_id", "gitlab")
        .gauge();
    assertThat(gaugeB).isNotNull();
    assertThat(gaugeB.value()).isEqualTo(900.0);
  }

  @Test
  public void testOnRateLimitRemaining_nullUserId_doesNotThrow() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onRateLimitRemaining("github", null, 100);
    });

    Counter counter = meterRegistry.find("scm.rate.limit.calls")
        .tag("tenant_id", "acme-corp")
        .tag("client_id", "github")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
    assertThat(counter.getId().getTag("user_id")).isNull();
  }

  @Test
  public void testOnRateLimitExceeded_nullUserId_doesNotThrow() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      underTest.onRateLimitExceeded("github", null);
    });

    Counter counter = meterRegistry.find("scm.rate.limit.exceeded")
        .tag("tenant_id", "acme-corp")
        .tag("client_id", "github")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
    assertThat(counter.getId().getTag("user_id")).isNull();
  }

  @Test
  public void testOnRateLimitRemaining_withNullMeterRegistry_doesNotThrow() {
    ScmRateLimitMetrics nullMetrics = new ScmRateLimitMetrics(null);
    nullMetrics.onRateLimitRemaining("github", "user1", 500);
    nullMetrics.onRateLimitExceeded("github", "user1");
  }

  @Test
  public void testStart_registersAsListener() {
    TenantTestHelper.initMultiTenantMode();
    underTest.start();

    TenantTestHelper.testAsNewTenant("test-tenant", tenant -> {
      RateLimitRecorder.recordApiRateLimitRemaining("github", "user1", 500);
    });

    Counter counter = meterRegistry.find("scm.rate.limit.calls")
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
      RateLimitRecorder.recordApiRateLimitRemaining("github", "user1", 500);
    });

    Counter counter = meterRegistry.find("scm.rate.limit.calls").counter();
    assertThat(counter).isNull();
  }
}

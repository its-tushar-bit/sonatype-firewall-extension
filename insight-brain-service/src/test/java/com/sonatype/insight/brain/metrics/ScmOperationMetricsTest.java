/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.AbstractLongAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.ALREADY_REMEDIATED;
import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.NOT_ELIGIBLE;
import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.NO_REMEDIATION;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmOperationMetricsTest
{
  private ScmOperationMetrics underTest;

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Before
  public void setup() {
    underTest = new ScmOperationMetrics(meterRegistry);
    TenantTestHelper.initMultiTenantMode();
  }

  @After
  public void cleanup() {
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void testRecordPrCommentCompleted_recordsTimerAndCounter() {
    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      ScmTimerContext context = underTest.startPrCommentTimer(ScmCommentOperation.CREATE, "GITHUB");
      underTest.recordPrCommentCompleted(context);

      assertTimerCount("scm.pr.comment.duration",
          "kind", "scm_operation", "operation", "create", "provider", "GITHUB", "tenant_id", "acme-corp")
              .isEqualTo(1);

      assertCounter("scm.pr.comment.completed",
          "kind", "scm_operation", "operation", "create", "provider", "GITHUB", "tenant_id", "acme-corp")
              .isEqualTo(1);
    });
  }

  @Test
  public void testRecordPrCommentFailed_recordsCounter() {
    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      ScmTimerContext context = underTest.startPrCommentTimer(ScmCommentOperation.UPDATE, "GITLAB");
      underTest.recordPrCommentFailed(context);

      assertCounter("scm.pr.comment.failed",
          "kind", "scm_operation", "operation", "update", "provider", "GITLAB", "tenant_id", "acme-corp")
              .isEqualTo(1);
    });
  }

  @Test
  public void testRecordPrCreationCompleted_recordsTimerAndCounter() {
    TenantTestHelper.testAsNewTenant("widgets-inc", tenant -> {
      ScmTimerContext context = underTest.startPrCreationTimer("BITBUCKET");
      underTest.recordPrCreationCompleted(context);

      assertTimerCount("scm.pr.create.duration",
          "kind", "scm_operation", "provider", "BITBUCKET", "tenant_id", "widgets-inc")
              .isEqualTo(1);

      assertCounter("scm.pr.create.completed",
          "kind", "scm_operation", "provider", "BITBUCKET", "tenant_id", "widgets-inc")
              .isEqualTo(1);
    });
  }

  @Test
  public void testRecordPrCreationFailed_recordsCounter() {
    TenantTestHelper.testAsNewTenant("acme-corp", tenant -> {
      ScmTimerContext context = underTest.startPrCreationTimer("GITHUB");
      underTest.recordPrCreationFailed(context);

      assertCounter("scm.pr.create.failed",
          "kind", "scm_operation", "provider", "GITHUB", "tenant_id", "acme-corp")
              .isEqualTo(1);
    });
  }

  @Test
  public void testRecordPrCreationIneligible_multipleTenants_trackedIndependently() {
    TenantTestHelper.testAsNewTenant("tenant-a", tenant -> {
      underTest.recordPrCreationIneligible(NOT_ELIGIBLE);
      underTest.recordPrCreationIneligible(NOT_ELIGIBLE);
      underTest.recordPrCreationIneligible(NO_REMEDIATION);
    });

    TenantTestHelper.testAsNewTenant("tenant-b", tenant -> {
      underTest.recordPrCreationIneligible(ALREADY_REMEDIATED);
    });

    assertCounter("scm.pr.create.ineligible",
        "kind", "scm_operation", "reason", "not_eligible", "tenant_id", "tenant-a")
            .isEqualTo(2);

    assertCounter("scm.pr.create.ineligible",
        "kind", "scm_operation", "reason", "no_remediation", "tenant_id", "tenant-a")
            .isEqualTo(1);

    assertCounter("scm.pr.create.ineligible",
        "kind", "scm_operation", "reason", "already_remediated", "tenant_id", "tenant-b")
            .isEqualTo(1);
  }

  @Test
  public void testNullMeterRegistry_doesNotThrow() {
    underTest = new ScmOperationMetrics(null);

    ScmTimerContext commentContext = underTest.startPrCommentTimer(ScmCommentOperation.CREATE, "GITHUB");
    underTest.recordPrCommentCompleted(commentContext);
    underTest.recordPrCommentFailed(commentContext);

    ScmTimerContext creationContext = underTest.startPrCreationTimer("GITHUB");
    underTest.recordPrCreationCompleted(creationContext);
    underTest.recordPrCreationFailed(creationContext);
    underTest.recordPrCreationIneligible(NOT_ELIGIBLE);
  }

  @Test
  public void testSingleTenantMode_doesNotRecordMetrics() {
    TenantTestHelper.resetAfterTest();
    underTest = new ScmOperationMetrics(meterRegistry);

    ScmTimerContext commentContext = underTest.startPrCommentTimer(ScmCommentOperation.CREATE, "GITHUB");
    underTest.recordPrCommentCompleted(commentContext);
    underTest.recordPrCommentFailed(commentContext);

    ScmTimerContext creationContext = underTest.startPrCreationTimer("GITHUB");
    underTest.recordPrCreationCompleted(creationContext);
    underTest.recordPrCreationFailed(creationContext);
    underTest.recordPrCreationIneligible(NOT_ELIGIBLE);

    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  private AbstractDoubleAssert<?> assertCounter(final String name, final String... tags) {
    Counter counter = meterRegistry.find(name).tags(tags).counter();
    if (counter == null) {
      counter = meterRegistry.find(name).counter();
      assertThat(counter).as("Counter '%s' not found", name).isNotNull();
      assertThat(counter.getId().getTags()).containsAll(Tags.of(tags));
    }
    return assertThat(counter.count());
  }

  private AbstractLongAssert<?> assertTimerCount(final String name, final String... tags) {
    Timer timer = meterRegistry.find(name).tags(tags).timer();
    if (timer == null) {
      timer = meterRegistry.find(name).timer();
      assertThat(timer).as("Timer '%s' not found", name).isNotNull();
      assertThat(timer.getId().getTags()).containsAll(Tags.of(tags));
    }
    return assertThat(timer.count());
  }
}

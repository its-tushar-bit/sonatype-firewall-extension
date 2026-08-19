/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PullRequestPollingSchedulerTest
    extends AbstractMultiTenantDatabaseTest
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestPollingSchedulerTest.class);

  @Mock
  private PullRequestPollingService pullRequestPollingService;

  @Mock
  private IqForScmLicenseChecker licenseChecker;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ScmNodeProcessor scmNodeProcessor;

  private PullRequestPollingScheduler scheduler;

  private final int delaySeconds = 1;

  private final int intervalSeconds = 1;

  @BeforeEach
  public void before() {
    lenient().when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);
    lenient().when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    scheduler = new PullRequestPollingScheduler(pullRequestPollingService, licenseChecker, mockApiConfigFeaturesService,
        delaySeconds, intervalSeconds, mockShutdownHandler, scmNodeProcessor);
  }

  @Test
  public void testPullRequestPollingScheduler_handlesMultipleTenants() throws InterruptedException {
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    // given: scheduler instance with single tenant
    Tenant tenant1 = testAsNewTenant(t1 -> scheduler.register());

    Mockito.doAnswer(invocation -> {
      log.debug("invocation 1");
      // then: polling service invoked only for tenant1
      assertThat(new TenantUtil().getTenantSlugForSynchronization()).isEqualTo(tenant1.tenantSlug);
      return null;
    }).when(pullRequestPollingService).fetchAndSendPullRequestsForCommenting();

    // when: wait 1 polling cycle, after initial delay
    Thread.sleep(1500);

    // given: scheduler instance with two tenants
    Tenant tenant2 = testAsNewTenant(t2 -> scheduler.register());

    AtomicBoolean calledWithTenant1 = new AtomicBoolean(false);
    AtomicBoolean calledWithTenant2 = new AtomicBoolean(false);
    Mockito.doAnswer(invocation -> {
      String tenant = new TenantUtil().getTenantSlugForSynchronization();
      if (tenant.equals(tenant1.tenantSlug)) {
        calledWithTenant1.set(true);
      }
      else if (tenant.equals(tenant2.tenantSlug)) {
        calledWithTenant2.set(true);
      }
      else {
        fail("Process called with unexpected tenant");
      }
      return null;
    }).when(pullRequestPollingService).fetchAndSendPullRequestsForCommenting();

    // when: wait 1 polling cycle, after initial delay for the new tenant
    Thread.sleep(1500);

    // then: polling service invoked for both tenants
    assertThat(calledWithTenant1.get()).isEqualTo(true);
    assertThat(calledWithTenant2.get()).isEqualTo(true);

    TenantTestHelper.testAsTenantAndInvalidate(tenant1.tenantSlug, t1 -> {
      scheduler.deregister();
    });

    Mockito.doAnswer(invocation -> {
      // then: polling service invoked only for tenant2
      assertThat(new TenantUtil().getTenantSlugForSynchronization()).isEqualTo(tenant2.tenantSlug);
      return null;
    }).when(pullRequestPollingService).fetchAndSendPullRequestsForCommenting();

    Thread.sleep(1500);

    TenantTestHelper.testAsTenantAndInvalidate(tenant2.tenantSlug, t1 -> {
      scheduler.deregister();
    });
  }

  @Test
  public void setSuppressed_isPerTenant() {
    Tenant tenant1 = testAsNewTenant(t1 -> {
      assertThat(scheduler.isSuppressed()).isFalse();
      assertThat(scheduler.setSuppressed(false)).isFalse();
    });
    Tenant tenant2 = testAsNewTenant(t2 -> {
      assertThat(scheduler.isSuppressed()).isFalse();
    });

    // Suppress on tenant1 only
    TenantTestHelper.testAsTenantAndInvalidate(tenant1.tenantSlug, t1 -> {
      assertThat(scheduler.setSuppressed(true)).isTrue();
      assertThat(scheduler.isSuppressed()).isTrue();
    });

    // Tenant2's view must be untouched
    TenantTestHelper.testAsTenantAndInvalidate(tenant2.tenantSlug, t2 -> {
      assertThat(scheduler.isSuppressed()).isFalse();
      assertThat(scheduler.setSuppressed(true)).isTrue();
    });

    // Reverting tenant1 must not affect tenant2
    TenantTestHelper.testAsTenantAndInvalidate(tenant1.tenantSlug, t1 -> {
      assertThat(scheduler.setSuppressed(false)).isTrue();
      assertThat(scheduler.isSuppressed()).isFalse();
    });
    TenantTestHelper.testAsTenantAndInvalidate(tenant2.tenantSlug, t2 -> {
      assertThat(scheduler.isSuppressed()).isTrue();
    });
  }
}

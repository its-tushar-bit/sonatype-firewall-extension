/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlEventOrchestratorMtiqTest
    extends MultiTenantTestSupport
{
  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private SourceControlEventProcessor mockSourceControlEventProcessor;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Mock
  private SourceControlLoadBalancer mockSourceControlLoadBalancer;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private IqForScmLicenseChecker mockIqForScmLicenseChecker;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ScmNodeProcessor scmNodeProcessor;

  private SourceControlEventOrchestrator underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    MockitoAnnotations.openMocks(this);

    when(mockIqForScmLicenseChecker.isIqForScmSupported()).thenReturn(true);
    when(mockSourceControlLoadBalancer.reserveEvent(any())).thenReturn(true);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);

    SourceControlEventOrchestrator orchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);

    underTest = Mockito.spy(orchestrator);
  }

  @Test
  public void testOrchestrator_register_deregister() {
    when(scmNodeProcessor.shouldRun()).thenReturn(true);
    testAsNewTenant(tenant -> {
      underTest.register();
      underTest.deregister();

      verify(underTest, times(1)).startEventProcessingExecutorService();
      verify(underTest, times(1)).notifyExecutorShutdown();
    });
  }

  @Test
  public void testOrchestrator_multiple_tenants_register_deregister() {
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    Tenant tenant1 = testAsNewTenant(t1 -> underTest.register());
    Tenant tenant2 = testAsNewTenant(t2 -> underTest.register());

    verify(underTest, times(2)).startEventProcessingExecutorService();
    verify(underTest, never()).notifyExecutorShutdown();

    testAsTenant(tenant1, t -> underTest.deregister());

    verify(underTest, times(1)).notifyExecutorShutdown();

    testAsTenant(tenant2, t -> underTest.deregister());

    verify(underTest, times(2)).notifyExecutorShutdown();
  }
}

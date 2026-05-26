/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

public class PullRequestMonitorTaskTest
    extends AbstractComponentTest
{
  @Inject
  private PullRequestMonitor underTest;

  @Mock
  private IqForScmLicenseChecker mockLicenseChecker;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @Before
  public void setupBeanOverrides() {
    applyBeanFieldOverride(PullRequestMonitor.class, "licenseChecker", mockLicenseChecker);
    applyBeanFieldOverride(PullRequestMonitor.class, "apiConfigFeaturesService", mockApiConfigFeaturesService);
  }

  @Test
  public void testExecute_shouldUpdatePullRequestDetails() {
    when(mockLicenseChecker.isIqForScmSupported()).thenReturn(true);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);

    PullRequestMonitor underTestSpy = spy(underTest);
    doAnswer(invocation -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(underTestSpy).updatePullRequestDetails();

    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    try (MDCUsernameScope ignored = MDCUsernameScope.forUser("username")) {
      underTestSpy.execute(mockContext);
    }

    verify(underTestSpy).updatePullRequestDetails();
  }
}

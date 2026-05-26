/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.configuration.FirewallIgnorePatternsDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

public class FirewallIgnorePatternUpdaterTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallIgnorePatternUpdater firewallIgnorePatternUpdater;

  @Inject
  private FirewallIgnorePatternsDAO firewallIgnorePatternsDAO;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private HdsClient hdsClientMock;

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(FirewallIgnorePatternUpdater.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testRegister() {
    firewallIgnorePatternUpdater.register();

    verify(taskSchedulerMock).schedulePeriodicTask(firewallIgnorePatternUpdater, Duration.ofHours(6));
  }

  @Test
  public void testExecute() {
    FirewallIgnorePatternUpdater firewallIgnorePatternUpdaterSpy = spy(firewallIgnorePatternUpdater);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(firewallIgnorePatternUpdaterSpy).updateFirewallIgnorePatterns();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      firewallIgnorePatternUpdaterSpy.execute(mock(JobExecutionContext.class));
    }

    verify(firewallIgnorePatternUpdaterSpy).updateFirewallIgnorePatterns();
  }

  @Test
  public void testUpdateFirewallIgnorePatterns() {
    com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns expectedFirewallIgnorePatterns =
        createFirewallIgnorePatterns();
    when(hdsClientMock.get(com.sonatype.clm.dto.model.component.FirewallIgnorePatterns.class,
        FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))
            .thenReturn(expectedFirewallIgnorePatterns.getFirewallIgnorePatterns());
    assertFirewallIgnorePatterns(firewallIgnorePatternsDAO.get(),
        new com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns());

    firewallIgnorePatternUpdater.updateFirewallIgnorePatterns();

    assertFirewallIgnorePatterns(firewallIgnorePatternsDAO.get(), expectedFirewallIgnorePatterns);
  }

  private com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns createFirewallIgnorePatterns() {
    FirewallIgnorePatterns ignorePatterns = new FirewallIgnorePatterns();
    ignorePatterns.regexpsByRepositoryFormat.put("format1", Arrays.asList("a", "b"));
    ignorePatterns.regexpsByRepositoryFormat.put("format2", Collections.singletonList("c"));
    return new com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns(ignorePatterns);
  }

  private void assertFirewallIgnorePatterns(
      com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns actual,
      com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns expected)
  {
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(FirewallIgnorePatternsDAO.SINGLETON_ENTITY_ID);
    assertThat(actual.getFirewallIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(expected.getFirewallIgnorePatterns());
  }
}

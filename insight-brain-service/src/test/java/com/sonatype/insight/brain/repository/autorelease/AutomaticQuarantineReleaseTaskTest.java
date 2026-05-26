/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

public class AutomaticQuarantineReleaseTaskTest
    extends AbstractComponentTest
{
  @Mock
  private AutomaticQuarantineRelease automaticQuarantineReleaseMock;

  @Test
  public void testExecute_QuartzJob() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(automaticQuarantineReleaseMock).run();

    AutomaticQuarantineReleaseTask underTest = new AutomaticQuarantineReleaseTask(() -> automaticQuarantineReleaseMock);

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      underTest.execute(mock(JobExecutionContext.class));
    }

    verify(automaticQuarantineReleaseMock).run();
  }

  @Test
  public void testExecute_AdminTask() throws Exception {
    AutomaticQuarantineReleaseTask underTest = new AutomaticQuarantineReleaseTask(() -> automaticQuarantineReleaseMock);

    underTest.execute((Map<String, List<String>>) null, new PrintWriter(OutputStream.nullOutputStream()));

    verify(automaticQuarantineReleaseMock).run();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(AutomaticQuarantineReleaseTask.class).build().isConcurrentExectionDisallowed())
        .isTrue();
  }
}

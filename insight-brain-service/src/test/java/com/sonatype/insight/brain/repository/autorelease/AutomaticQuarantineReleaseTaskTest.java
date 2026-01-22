/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import java.io.PrintWriter;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AutomaticQuarantineReleaseTaskTest
    extends AbstractComponentTest
{
  @Inject
  private AutomaticQuarantineReleaseTask automaticQuarantineReleaseTask;

  @Mock
  private AutomaticQuarantineRelease automaticQuarantineReleaseMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(AutomaticQuarantineRelease.class).toInstance(automaticQuarantineReleaseMock);
    super.configure(binder);
  }

  @Test
  public void testExecute_DropwizardTask() {
    PrintWriter printWriterMock = mock(PrintWriter.class);

    automaticQuarantineReleaseTask.execute(null, printWriterMock);

    verify(automaticQuarantineReleaseMock).run();
    verify(printWriterMock).write("Completed manual Automatic Quarantine Release execution\n");
  }

  @Test
  public void testExecute_QuartzJob() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(automaticQuarantineReleaseMock).run();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      automaticQuarantineReleaseTask.execute(mock(JobExecutionContext.class));
    }

    verify(automaticQuarantineReleaseMock).run();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(AutomaticQuarantineReleaseTask.class).build().isConcurrentExectionDisallowed())
        .isTrue();
  }
}

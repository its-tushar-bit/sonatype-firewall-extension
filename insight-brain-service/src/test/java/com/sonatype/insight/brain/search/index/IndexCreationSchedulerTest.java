/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import jakarta.inject.Inject;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class IndexCreationSchedulerTest
    extends AbstractComponentTest
{
  @Inject
  private IndexCreationScheduler indexCreationScheduler;

  @Mock
  private IndexService mockIndexService;

  @Override
  public void configure(Binder binder) {
    binder.bind(IndexService.class).toInstance(mockIndexService);
    super.configure(binder);
  }

  @Test
  public void testExecute() throws Exception {
    IndexCreationScheduler indexCreationSchedulerSpy = spy(indexCreationScheduler);

    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(mockIndexService).createSearchIndex();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      indexCreationSchedulerSpy.execute(null);
    }

    verify(mockIndexService).createSearchIndex();
  }
}

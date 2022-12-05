/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultQuartzJobInitializerTest
{
  @Test
  public void shouldCallRegisterOnStart() throws Exception {
    InsightJob job1 = mock(InsightJob.class);
    InsightJob job2 = mock(InsightJob.class);

    DefaultQuartzJobInitializer initializer = new DefaultQuartzJobInitializer(ImmutableList.of(job1, job2));

    initializer.start();

    verify(job1).register();
    verify(job2).register();
  }

  @Test
  public void shouldCallDeregisterOnStop() throws Exception {
    InsightJob job1 = mock(InsightJob.class);
    InsightJob job2 = mock(InsightJob.class);

    DefaultQuartzJobInitializer initializer = new DefaultQuartzJobInitializer(ImmutableList.of(job1, job2));

    initializer.stop();

    verify(job1).deregister();
    verify(job2).deregister();
  }
}

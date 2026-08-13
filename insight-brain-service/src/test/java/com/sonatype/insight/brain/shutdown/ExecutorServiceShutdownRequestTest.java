/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExecutorServiceShutdownRequestTest
{
  @Mock
  private ExecutorService mockExecutorService;

  private ExecutorServiceShutdownRequest executorServiceShutdownRequest;

  @BeforeEach
  public void before() {
    executorServiceShutdownRequest = new ExecutorServiceShutdownRequest(mockExecutorService, 0, null);
  }

  @Test
  public void testExecute() throws Exception {
    Future<?> shutdown = executorServiceShutdownRequest.execute(mockExecutorService);

    verify(mockExecutorService).shutdown();
    verify(mockExecutorService, never()).awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    shutdown.get();
    verify(mockExecutorService).awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }
}

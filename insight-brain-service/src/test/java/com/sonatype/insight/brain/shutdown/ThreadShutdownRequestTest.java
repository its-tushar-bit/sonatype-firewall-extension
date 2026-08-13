/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ThreadShutdownRequestTest
{
  @Test
  public void testExecute() throws Exception {
    Thread mockThread = mock(Thread.class);
    ThreadShutdownRequest threadShutdownRequest = new ThreadShutdownRequest(mockThread, 0, null);

    Future<?> shutdown = threadShutdownRequest.execute(null);

    verifyNoInteractions(mockThread);
    shutdown.get();
    verify(mockThread).join();
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ThreadShutdownRequestTest
{
  @Test
  public void testExecute() throws Exception {
    Thread mockThread = mock(Thread.class);
    ThreadShutdownRequest threadShutdownRequest = new ThreadShutdownRequest(new WeakReference<>(mockThread), 0);

    Future<?> shutdown = threadShutdownRequest.execute(null);

    verifyNoInteractions(mockThread);
    shutdown.get();
    verify(mockThread).join();
  }
}

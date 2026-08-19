/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WeakReferenceShutdownRequestTest
{
  @Mock
  private ExecutorService mockExecutorService;

  private final Object object = new Object();

  private final WeakReferenceShutdownRequest<Object> spyWeakReferenceShutdownRequestNonNullReferent =
      spy(new WeakReferenceShutdownRequest<>(object, 0, null)
      {
        @Override
        public Future<?> execute(final ExecutorService executorService, final Object item) {
          return CompletableFuture.completedFuture(null);
        }
      });

  private final WeakReferenceShutdownRequest<Object> spyWeakReferenceShutdownRequestNullReferent =
      spy(new WeakReferenceShutdownRequest<>(null, 0, null)
      {
        @Override
        public Future<?> execute(final ExecutorService executorService, final Object item) {
          return CompletableFuture.completedFuture(null);
        }
      });

  @Test
  public void testExecute_NonNullReferent() throws Exception {
    Future<?> future = spyWeakReferenceShutdownRequestNonNullReferent.execute(mockExecutorService);

    verify(spyWeakReferenceShutdownRequestNonNullReferent).execute(mockExecutorService, object);
    assertThat(future.isDone()).isTrue();
    assertThat(future.get()).isNull();
  }

  @Test
  public void testExecute_NullReferent() throws Exception {
    Future<?> future = spyWeakReferenceShutdownRequestNullReferent.execute(mockExecutorService);

    verify(spyWeakReferenceShutdownRequestNullReferent, never()).execute(any(), any());
    assertThat(future.isDone()).isTrue();
    assertThat(future.get()).isNull();
  }

  @Test
  public void testIsValid_NonNullReferent() {
    assertThat(spyWeakReferenceShutdownRequestNonNullReferent.isValid()).isTrue();
  }

  @Test
  public void testIsValid_NullReferent() {
    assertThat(spyWeakReferenceShutdownRequestNullReferent.isValid()).isFalse();
  }
}

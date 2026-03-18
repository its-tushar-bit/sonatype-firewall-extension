/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CompleteOnGetFutureTest
{
  @Test
  public void testGet_CancelTrue() {
    CompleteOnGetFuture<Void> completeOnGetFuture = new CompleteOnGetFuture<>(null);
    completeOnGetFuture.cancel(true);

    assertThatExceptionOfType(CancellationException.class).isThrownBy(completeOnGetFuture::get);
    assertThat(completeOnGetFuture.isCompletedExceptionally()).isTrue();
    assertThat(completeOnGetFuture.isCancelled()).isTrue();
    assertThat(completeOnGetFuture.isDone()).isTrue();
  }

  @Test
  public void testGet_CancelFalse() {
    CompleteOnGetFuture<Void> completeOnGetFuture = new CompleteOnGetFuture<>(null);
    completeOnGetFuture.cancel(false);

    assertThatExceptionOfType(CancellationException.class).isThrownBy(completeOnGetFuture::get);
    assertThat(completeOnGetFuture.isCompletedExceptionally()).isTrue();
    assertThat(completeOnGetFuture.isCancelled()).isTrue();
    assertThat(completeOnGetFuture.isDone()).isTrue();
  }

  @Test
  public void testGet_AlreadyDone() throws Exception {
    CompleteOnGetFuture<Void> completeOnGetFuture = new CompleteOnGetFuture<>(null);
    completeOnGetFuture.complete(null);

    assertThat(completeOnGetFuture.get()).isNull();
    assertThat(completeOnGetFuture.isCompletedExceptionally()).isFalse();
    assertThat(completeOnGetFuture.isCancelled()).isFalse();
    assertThat(completeOnGetFuture.isDone()).isTrue();
  }

  @Test
  public void testGet() throws Exception {
    AtomicBoolean ran = new AtomicBoolean(false);
    CompleteOnGetFuture<Void> completeOnGetFuture = new CompleteOnGetFuture<>(() -> ran.set(true));

    assertThat(completeOnGetFuture.get()).isNull();
    assertThat(ran).isTrue();
    assertThat(completeOnGetFuture.isCompletedExceptionally()).isFalse();
    assertThat(completeOnGetFuture.isCancelled()).isFalse();
    assertThat(completeOnGetFuture.isDone()).isTrue();
  }

  @Test
  public void testGet_Exception() {
    CompleteOnGetFuture<Void> completeOnGetFuture = new CompleteOnGetFuture<>(() -> {
      throw new RuntimeException("some exception");
    });

    assertThatExceptionOfType(ExecutionException.class)
        .isThrownBy(completeOnGetFuture::get)
        .withMessageContaining("some exception");
    assertThat(completeOnGetFuture.isCompletedExceptionally()).isTrue();
    assertThat(completeOnGetFuture.isCancelled()).isFalse();
    assertThat(completeOnGetFuture.isDone()).isTrue();
  }
}

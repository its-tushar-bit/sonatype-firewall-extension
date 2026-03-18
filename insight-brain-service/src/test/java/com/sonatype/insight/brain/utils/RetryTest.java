/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.sonatype.insight.brain.utils.Retry.RetryableCallable;
import com.sonatype.insight.brain.utils.Retry.RetryableRunnable;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RetryTest
{
  @Test
  public void testExecuteCallable_NoRetriesNeeded() {
    @SuppressWarnings("unchecked")
    RetryableCallable<Object, RuntimeException> callable = mock(RetryableCallable.class);
    Object result = new Object();
    doReturn(result).when(callable).call();

    Retry retry = new Retry("test", 1, null, e -> true, attempt -> Duration.ZERO);
    // Type arguments <Object, RuntimeException> are required and can't be removed to avoid JDK bug - JDK-8066974.
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8066974
    assertThat(retry.<Object, RuntimeException>executeCallable(callable)).isEqualTo(result);
    verify(callable, times(1)).call();
  }

  @Test
  public void testExecuteSupplier_NoRetriesNeeded() {
    @SuppressWarnings("unchecked")
    Supplier<Object> supplier = mock(Supplier.class);
    Object result = new Object();
    doReturn(result).when(supplier).get();

    Retry retry = new Retry("test", 1, null, e -> true, attempt -> Duration.ZERO);
    assertThat(retry.executeSupplier(supplier)).isEqualTo(result);
    verify(supplier, times(1)).get();
  }

  @Test
  public void testExecuteRunnable_NoRetriesNeeded() {
    @SuppressWarnings("unchecked")
    RetryableRunnable<RuntimeException> runnable = mock(RetryableRunnable.class);

    Retry retry = new Retry("test", 1, null, e -> true, attempt -> Duration.ZERO);
    retry.executeRunnable(runnable);
    verify(runnable, times(1)).run();
  }

  @Test
  public void testExecuteCallable_RetryDisabled() {
    @SuppressWarnings("unchecked")
    RetryableCallable<Object, RuntimeException> callable = mock(RetryableCallable.class);
    RuntimeException error = new RuntimeException();
    doThrow(error).doReturn(new Object()).when(callable).call();

    Retry retry = new Retry("test", 0, null, e -> true, attempt -> Duration.ZERO);
    assertThatThrownBy(() -> retry.executeCallable(callable)).isEqualTo(error);
    verify(callable, times(1)).call();
  }

  @Test
  public void testExecuteCallable_RetryableException() {
    @SuppressWarnings("unchecked")
    RetryableCallable<Object, RuntimeException> callable = mock(RetryableCallable.class);
    Object result = new Object();
    RuntimeException error = new RuntimeException();
    doThrow(error).doReturn(result).when(callable).call();

    Retry retry = new Retry("test", 1, null, e -> e == error, attempt -> Duration.ZERO);
    // Type arguments <Object, RuntimeException> are required and can't be removed to avoid JDK bug - JDK-8066974.
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8066974
    assertThat(retry.<Object, RuntimeException>executeCallable(callable)).isEqualTo(result);
    verify(callable, times(2)).call();
  }

  @Test
  public void testExecuteCallable_NonRetryableException() {
    @SuppressWarnings("unchecked")
    RetryableCallable<Object, RuntimeException> callable = mock(RetryableCallable.class);
    Object result = new Object();
    RuntimeException error = new RuntimeException();
    doThrow(error).doReturn(result).when(callable).call();

    Retry retry = new Retry("test", 1, null, e -> false, attempt -> Duration.ZERO);
    assertThatThrownBy(() -> retry.executeCallable(callable)).isEqualTo(error);
    verify(callable, times(1)).call();
  }

  @Test
  public void testExecuteCallable_MaxRetryCount() {
    @SuppressWarnings("unchecked")
    RetryableCallable<Object, RuntimeException> callable = mock(RetryableCallable.class);
    RuntimeException error = new RuntimeException();
    doThrow(error).when(callable).call();

    Retry retry = new Retry("test", 5, null, e -> true, attempt -> Duration.ZERO);
    assertThatThrownBy(() -> retry.executeCallable(callable)).isEqualTo(error);
    verify(callable, times(6)).call();
  }

  @Test
  public void testExecuteCallable_MaxRetryDuration() {
    @SuppressWarnings("unchecked")
    RetryableCallable<Object, RuntimeException> callable = mock(RetryableCallable.class);
    RuntimeException error = new RuntimeException();
    doThrow(error).when(callable).call();

    Retry retry = new Retry("test", -1, Duration.ofSeconds(2), e -> true, attempt -> Duration.ofMillis(20));
    long start = System.currentTimeMillis();
    assertThatThrownBy(() -> retry.executeCallable(callable)).isEqualTo(error);
    assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(2_000);
    verify(callable, atLeast(50)).call();
  }

  @Test
  public void testExecuteCallable_IgnorableException() {
    RetryableCallable<Object, RuntimeException> callable = mock(RetryableCallable.class);
    RuntimeException exception1 = new RuntimeException("Exception 1");
    RuntimeException exception2 = new RuntimeException("Exception 2");
    AtomicInteger invocationCount = new AtomicInteger();
    doAnswer(invocationOnMock -> {
      if (invocationCount.getAndIncrement() % 2 == 0) {
        throw exception1;
      }
      throw exception2;
    }).when(callable).call();

    Retry retry = new Retry("test", 1, null, e -> true, e -> e == exception1, attempt -> Duration.ZERO);
    assertThatThrownBy(() -> retry.executeCallable(callable)).isEqualTo(exception2);
    verify(callable, times(4)).call();
  }
}

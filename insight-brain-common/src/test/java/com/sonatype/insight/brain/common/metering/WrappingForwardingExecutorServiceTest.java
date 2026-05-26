/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.metering;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WrappingForwardingExecutorServiceTest
{
  private TestWrappingExecutor executor;

  @After
  public void after() throws Exception {
    if (executor != null) {
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  public void testExecuteWrapsRunnable() throws Exception {
    executor = new TestWrappingExecutor();

    CountDownLatch done = new CountDownLatch(1);
    executor.execute(done::countDown);

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executor.runnableWrapCount.get()).isEqualTo(1);
  }

  @Test
  public void testSubmitRunnableWrapsRunnable() throws Exception {
    executor = new TestWrappingExecutor();

    CountDownLatch done = new CountDownLatch(1);
    executor.submit((Runnable) done::countDown);

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executor.runnableWrapCount.get()).isEqualTo(1);
  }

  @Test
  public void testSubmitRunnableWithResultWrapsRunnable() throws Exception {
    executor = new TestWrappingExecutor();

    Future<String> future = executor.submit(() -> {
    }, "result");

    assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("result");
    assertThat(executor.runnableWrapCount.get()).isEqualTo(1);
  }

  @Test
  public void testSubmitCallableWrapsCallable() throws Exception {
    executor = new TestWrappingExecutor();

    Future<String> future = executor.submit(() -> "hello");

    assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("hello");
    assertThat(executor.callableWrapCount.get()).isEqualTo(1);
  }

  @Test
  public void testInvokeAllWrapsEachCallable() throws Exception {
    executor = new TestWrappingExecutor();

    List<Callable<Integer>> tasks = List.of(() -> 1, () -> 2, () -> 3);
    List<Future<Integer>> futures = executor.invokeAll(tasks);

    assertThat(futures).hasSize(3);
    assertThat(futures.get(0).get()).isEqualTo(1);
    assertThat(futures.get(1).get()).isEqualTo(2);
    assertThat(futures.get(2).get()).isEqualTo(3);
    assertThat(executor.callableWrapCount.get()).isEqualTo(3);
  }

  @Test
  public void testInvokeAllWithTimeoutWrapsEachCallable() throws Exception {
    executor = new TestWrappingExecutor();

    List<Callable<Integer>> tasks = List.of(() -> 1, () -> 2);
    List<Future<Integer>> futures = executor.invokeAll(tasks, 5, TimeUnit.SECONDS);

    assertThat(futures).hasSize(2);
    assertThat(futures.get(0).get()).isEqualTo(1);
    assertThat(futures.get(1).get()).isEqualTo(2);
    assertThat(executor.callableWrapCount.get()).isEqualTo(2);
  }

  @Test
  public void testInvokeAnyWrapsEachCallable() throws Exception {
    executor = new TestWrappingExecutor();

    List<Callable<String>> tasks = List.of(() -> "a", () -> "b");
    String result = executor.invokeAny(tasks);

    assertThat(result).isIn("a", "b");
    assertThat(executor.callableWrapCount.get()).isEqualTo(2);
  }

  @Test
  public void testInvokeAnyWithTimeoutWrapsEachCallable() throws Exception {
    executor = new TestWrappingExecutor();

    List<Callable<String>> tasks = List.of(() -> "x", () -> "y");
    String result = executor.invokeAny(tasks, 5, TimeUnit.SECONDS);

    assertThat(result).isIn("x", "y");
    assertThat(executor.callableWrapCount.get()).isEqualTo(2);
  }

  /**
   * Minimal implementation that counts wrap calls and delegates to a real executor.
   */
  private static class TestWrappingExecutor
      extends WrappingForwardingExecutorService
  {
    final AtomicInteger runnableWrapCount = new AtomicInteger(0);

    final AtomicInteger callableWrapCount = new AtomicInteger(0);

    private final ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    protected ExecutorService delegate() {
      return delegate;
    }

    @Override
    protected Runnable wrapTask(Runnable task) {
      runnableWrapCount.incrementAndGet();
      return task;
    }

    @Override
    protected <T> Callable<T> wrapTask(Callable<T> task) {
      callableWrapCount.incrementAndGet();
      return task;
    }
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShutdownHandlerTest
{
  private AtomicInteger executeCounter;

  private AtomicInteger getCounter;

  private ThreadFactory spyThreadFactory;

  private ExecutorService spyExecutorService;

  @Captor
  private ArgumentCaptor<Runnable> runnableArgumentCaptor;

  private ShutdownHandler spyShutdownHandler;

  @BeforeEach
  public void before() {
    spyThreadFactory =
        spy(new ThreadFactoryBuilder().setNameFormat(ShutdownHandlerTest.class.getSimpleName() + "-%d").build());
    spyExecutorService = spy(Executors.newCachedThreadPool(spyThreadFactory));
    spyShutdownHandler = spy(new ShutdownHandler(spyThreadFactory, spyExecutorService));
    lenient().doNothing().when(spyShutdownHandler).exit(anyInt());
    executeCounter = new AtomicInteger(0);
    getCounter = new AtomicInteger(0);
  }

  @AfterEach
  public void after() {
    spyExecutorService.shutdownNow();
  }

  @Test
  public void testTrigger_AlreadyTriggered() {
    when(spyShutdownHandler.isTriggered()).thenReturn(true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> spyShutdownHandler.trigger(null, false))
        .withMessage("Graceful shutdown already triggered.");
  }

  @Test
  public void testTrigger() throws Exception {
    Duration timeout = Duration.ofSeconds(10);

    // Create the shutdown requests
    TestShutdownRequest<String> shutdownRequest1 = new TestShutdownRequest<>("sr1", 0);
    TestShutdownRequest<String> shutdownRequest2 = new TestShutdownRequest<>("sr2", 0);
    TestShutdownRequest<String> shutdownRequest3 = new TestShutdownRequest<>("sr3", 1);
    TestShutdownRequest<String> shutdownRequest4 = new TestShutdownRequest<>("sr4", 1);
    TestShutdownRequest<String> shutdownRequest5 = new TestShutdownRequest<>("sr5", 2);
    TestShutdownRequest<String> shutdownRequest6 = new TestShutdownRequest<>("sr6", 2);

    // Add the shutdown requests out of order
    spyShutdownHandler.addAndClean(shutdownRequest6);
    spyShutdownHandler.addAndClean(shutdownRequest1);
    spyShutdownHandler.addAndClean(shutdownRequest3);
    spyShutdownHandler.addAndClean(shutdownRequest2);
    spyShutdownHandler.addAndClean(shutdownRequest5);
    spyShutdownHandler.addAndClean(shutdownRequest4);

    // Trigger in a new thread to not block
    Thread thread = new Thread(() -> spyShutdownHandler.trigger(timeout, false));
    thread.start();

    await().atMost(timeout).until(() -> getCounter.get() > 0);

    // Only group 0 should be executing initially
    assertThat(shutdownRequest1.getExecuteCount()).isEqualTo(0);
    assertThat(shutdownRequest2.getExecuteCount()).isEqualTo(1);
    assertThat(shutdownRequest3.getExecuteCount()).isNull();
    assertThat(shutdownRequest4.getExecuteCount()).isNull();
    assertThat(shutdownRequest5.getExecuteCount()).isNull();
    assertThat(shutdownRequest6.getExecuteCount()).isNull();

    // Let group 0 finish
    shutdownRequest1.getShutdownBlock().countDown();
    shutdownRequest2.getShutdownBlock().countDown();

    await().atMost(timeout).until(() -> getCounter.get() > 2);

    // Next group 1 should be executing
    assertThat(shutdownRequest1.getExecuteCount()).isEqualTo(0);
    assertThat(shutdownRequest2.getExecuteCount()).isEqualTo(1);
    assertThat(shutdownRequest3.getExecuteCount()).isEqualTo(2);
    assertThat(shutdownRequest4.getExecuteCount()).isEqualTo(3);
    assertThat(shutdownRequest5.getExecuteCount()).isNull();
    assertThat(shutdownRequest6.getExecuteCount()).isNull();

    // Let group 1 finish, out of order as well
    shutdownRequest4.getShutdownBlock().countDown();
    shutdownRequest3.getShutdownBlock().countDown();

    await().atMost(timeout).until(() -> getCounter.get() > 4);

    // Next group 2 should be executing
    // Note that since 6 was added before 5, and they have the same order, then 6 gets executed earlier
    assertThat(shutdownRequest1.getExecuteCount()).isEqualTo(0);
    assertThat(shutdownRequest2.getExecuteCount()).isEqualTo(1);
    assertThat(shutdownRequest3.getExecuteCount()).isEqualTo(2);
    assertThat(shutdownRequest4.getExecuteCount()).isEqualTo(3);
    assertThat(shutdownRequest5.getExecuteCount()).isEqualTo(5);
    assertThat(shutdownRequest6.getExecuteCount()).isEqualTo(4);

    // Check we haven't tried to shut down yet
    verify(spyExecutorService, never()).shutdownNow();
    verify(spyShutdownHandler, never()).exitInNewThread(anyInt());

    // Let group 2 finish
    shutdownRequest5.getShutdownBlock().countDown();
    shutdownRequest6.getShutdownBlock().countDown();

    thread.join(timeout.toMillis());

    // We should shut down by the end of the request
    verify(spyExecutorService).shutdownNow();
    verify(spyShutdownHandler).exitInNewThread(0);
  }

  @Test
  public void testTrigger_TimeoutException() {
    TestShutdownRequest<String> shutdownRequest = new TestShutdownRequest<>("sr", 0);
    spyShutdownHandler.addAndClean(shutdownRequest);

    spyShutdownHandler.trigger(Duration.ofMillis(10), false);

    verify(spyShutdownHandler).exitInNewThread(1);
  }

  @Test
  public void testTrigger_InterruptedException() {
    TestShutdownRequest<String> shutdownRequest = new TestShutdownRequest<>("sr", 0);
    spyShutdownHandler.addAndClean(shutdownRequest);

    Thread thread = new Thread(() -> spyShutdownHandler.trigger(Duration.ofSeconds(10), false));
    thread.start();
    await().atMost(10, TimeUnit.SECONDS).until(() -> getCounter.get() > 0);
    thread.interrupt();

    verify(spyShutdownHandler, timeout(10000)).exitInNewThread(2);
  }

  @Test
  public void testTrigger_Exception() {
    ShutdownRequest<String> shutdownRequest = new TestShutdownRequest<>("sr", 0)
    {
      @Override
      public Future<?> execute(final ExecutorService executorService) {
        throw new RuntimeException();
      }
    };
    spyShutdownHandler.addAndClean(shutdownRequest);

    spyShutdownHandler.trigger(Duration.ofSeconds(10), false);

    verify(spyShutdownHandler).exitInNewThread(3);
  }

  @Test
  public void testExitInNewThread() {
    Thread mockThread = mock(Thread.class);
    when(spyThreadFactory.newThread(any())).thenReturn(mockThread);

    spyShutdownHandler.exitInNewThread(10);

    verify(spyThreadFactory).newThread(runnableArgumentCaptor.capture());
    verify(mockThread).start();
    Runnable runnable = runnableArgumentCaptor.getValue();
    assertThat(runnable).isNotNull();
    runnable.run();
    verify(spyShutdownHandler).exit(10);
  }

  @Test
  public void testSkipExit() {
    spyShutdownHandler.trigger(Duration.ofSeconds(10), true);

    verify(spyShutdownHandler, never()).exit(anyInt());
  }

  private class TestShutdownRequest<T>
      extends AbstractShutdownRequest<T>
  {
    private final CountDownLatch shutdownBlock = new CountDownLatch(1);

    private Integer executeCount;

    public TestShutdownRequest(final T item, final int order) {
      super(item, order, null);
    }

    @Override
    public Future<?> execute(final ExecutorService executorService) {
      executeCount = executeCounter.getAndIncrement();
      return new CompleteOnGetFuture<>(() -> {
        getCounter.getAndIncrement();
        shutdownBlock.await();
      });
    }

    public Integer getExecuteCount() {
      return executeCount;
    }

    public CountDownLatch getShutdownBlock() {
      return shutdownBlock;
    }
  }
}

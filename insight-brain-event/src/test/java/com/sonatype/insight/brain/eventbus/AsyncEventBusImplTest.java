/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.test.LogOutput;

import com.google.common.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncEventBusImplTest
{
  private AsyncEventBusImpl underTest;

  private Handler handler1;

  private Handler handler2;

  private HandlerWithException handlerWithException;

  @Rule
  public LogOutput logOutput = new LogOutput(AsyncEventBusDiscardPolicy.class);

  @Before
  public void setUp() {
    underTest = new AsyncEventBusImpl(AsyncEventBus.DEFAULT_MAX_POOL_SIZE);
    handler1 = new Handler(underTest, new CountDownLatch(1));
    handler2 = new Handler(underTest, new CountDownLatch(1));
    handlerWithException = new HandlerWithException(underTest, new CountDownLatch(1));
  }

  @Test
  public void testRegister_EventsDispatched() throws Exception {
    underTest.post("foo");

    assertThat(handler1.getLatch().await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(handler2.getLatch().await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testRegister_HandlerException() throws Exception {
    underTest.post("foo");

    assertThat(handler1.getLatch().await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(handler2.getLatch().await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(handlerWithException.getLatch().await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testRegister_LogsDiscarded() throws InterruptedException {
    underTest = new AsyncEventBusImpl(1);

    HandlerWithLongExecution longHandler = new HandlerWithLongExecution(underTest, new CountDownLatch(2), 200);

    underTest.post("foo");
    underTest.post("bar");

    assertThat(logOutput).atErrorLevel()
        .contains("Discarding event because the thread bounds and queue capacities are reached");
    assertThat(longHandler.getLatch().await(2, TimeUnit.SECONDS)).isFalse();
    assertThat(longHandler.getLatch().getCount()).isEqualTo(1);
  }

  private class Handler
  {
    private CountDownLatch latch;

    public Handler(final AsyncEventBusImpl asyncEventBus, final CountDownLatch latch) {
      this.latch = latch;
      asyncEventBus.register(this);
    }

    public CountDownLatch getLatch() {
      return latch;
    }

    @Subscribe
    public void handleEvent(@SuppressWarnings("unused") final String message) {
      LoggerFactory.getLogger(getClass()).info("Handling {}", message);
      latch.countDown();
    }
  }

  private class HandlerWithException
  {
    private CountDownLatch latch;

    public HandlerWithException(final AsyncEventBusImpl asyncEventBus, final CountDownLatch latch) {
      this.latch = latch;
      asyncEventBus.register(this);
    }

    public CountDownLatch getLatch() {
      return latch;
    }

    @Subscribe
    public void handle(@SuppressWarnings("unused") final String message) {
      LoggerFactory.getLogger(getClass()).info("Handling {}", message);
      latch.countDown();
      throw new RuntimeException("something bad happened");
    }
  }

  private class HandlerWithLongExecution
  {
    private CountDownLatch latch;

    private int sleepTime;

    public HandlerWithLongExecution(
        final AsyncEventBusImpl asyncEventBus,
        final CountDownLatch latch,
        final int sleepTime)
    {
      this.latch = latch;
      this.sleepTime = sleepTime;
      asyncEventBus.register(this);
    }

    public CountDownLatch getLatch() {
      return latch;
    }

    @Subscribe
    public void handle(@SuppressWarnings("unused") final String message) throws InterruptedException {
      LoggerFactory.getLogger(getClass()).info("Handling {}", message);
      latch.countDown();
      Thread.sleep(sleepTime);
    }
  }
}

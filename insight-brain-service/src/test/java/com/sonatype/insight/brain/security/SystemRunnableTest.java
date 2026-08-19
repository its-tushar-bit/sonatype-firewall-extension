/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemRunnableTest
{
  private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(1));

  /**
   * JUnit reuses threads which may have MDC username set as {@link MDCUsernameScope.SYSTEM }
   */
  @BeforeEach
  public void setup() {
    MDC.remove(MDCUsernameScope.USERNAME);
    assertThat(MDC.get(MDCUsernameScope.USERNAME)).isNull();
  }

  @AfterEach
  public void cleanup() {
    MDC.remove(MDCUsernameScope.USERNAME);
    assertThat(MDC.get(MDCUsernameScope.USERNAME)).isNull();
  }

  @Test
  public void addsSystemUsernameToMDC() throws InterruptedException {
    testSystemUsernameToMDC();
    assertThat(MDC.get(MDCUsernameScope.USERNAME)).isNull();
  }

  @Test
  public void replacesMDCUsernameWithSystem() throws InterruptedException {
    final String username = "foo";
    MDC.put(MDCUsernameScope.USERNAME, username);
    testSystemUsernameToMDC();
    assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(username);
  }

  private void testSystemUsernameToMDC() throws InterruptedException {
    RunnableStub runnableStub = new RunnableStub();
    threadPoolExecutor.execute(new SystemRunnable(runnableStub));
    threadPoolExecutor.shutdown();
    boolean terminated = threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS);

    assertThat(terminated).isTrue();
    assertThat(runnableStub.username).isEqualTo(MDCUsernameScope.SYSTEM);
  }

  private static class RunnableStub
      implements Runnable
  {
    private String username;

    @Override
    public void run() {
      username = MDC.get(MDCUsernameScope.USERNAME);
    }
  }
}

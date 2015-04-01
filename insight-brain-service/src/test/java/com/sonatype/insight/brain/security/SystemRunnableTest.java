/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SystemRunnableTest
{
  private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
      new ArrayBlockingQueue<Runnable>(1));

  /**
   * JUnit reuses threads which may have MDC username set as {@link MDCUsernameScope.SYSTEM }
   */
  @Before
  public void setup() {
    MDC.remove(MDCUsernameScope.USERNAME);
    assertThat(MDC.get(MDCUsernameScope.USERNAME), is(nullValue()));
  }

  @After
  public void cleanup() {
    MDC.remove(MDCUsernameScope.USERNAME);
    assertThat(MDC.get(MDCUsernameScope.USERNAME), is(nullValue()));
  }

  @Test
  public void addsSystemUsernameToMDC() throws InterruptedException {
    testSystemUsernameToMDC();
    assertThat(MDC.get(MDCUsernameScope.USERNAME), is(nullValue()));
  }

  @Test
  public void replacesMDCUsernameWithSystem() throws InterruptedException {
    final String username = "foo";
    MDC.put(MDCUsernameScope.USERNAME, username);
    testSystemUsernameToMDC();
    assertThat(MDC.get(MDCUsernameScope.USERNAME), is(username));
  }

  private void testSystemUsernameToMDC() throws InterruptedException {
    RunnableStub runnableStub = new RunnableStub();
    threadPoolExecutor.execute(new SystemRunnable(runnableStub));
    threadPoolExecutor.shutdown();
    boolean terminated = threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS);

    assertThat(terminated, is(true));
    assertThat(runnableStub.username, is(MDCUsernameScope.SYSTEM));
  }

  private class RunnableStub
      implements Runnable
  {
    private String username;

    @Override
    public void run() {
      username = MDC.get(MDCUsernameScope.USERNAME);
    }
  }
}

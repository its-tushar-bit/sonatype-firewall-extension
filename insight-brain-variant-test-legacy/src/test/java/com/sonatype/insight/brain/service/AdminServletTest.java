/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.TestShutdownHandler;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.brain.utils.CheckedRunnable;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class AdminServletTest
    extends AbstractBrainServiceIntegrationTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(ShutdownHandler.class);

  /**
   * Override to provide TestShutdownHandler instead of the mock from BaseIntegrationTestConfiguration.
   */
  @Override
  protected List<Class<?>> getTestConfigurationClasses() {
    List<Class<?>> configs = new ArrayList<>(super.getTestConfigurationClasses());
    configs.add(AdminServletTestConfiguration.class);
    return configs;
  }

  /**
   * Test configuration that provides a real TestShutdownHandler instead of a mock,
   * and registers test-only JAX-RS resources and jobs.
   */
  @TestConfiguration
  static class AdminServletTestConfiguration
  {
    @Bean
    @Primary
    public ShutdownHandler shutdownHandler() {
      return spy(new TestShutdownHandler());
    }
  }

  /**
   * Reconfigure LogOutput appender after server startup to ensure it captures logs.
   * Spring Boot's logging initialization during test context startup can remove
   * appenders added by rules that run earlier.
   */
  @Before
  public void reconfigureLogOutput() throws Exception {
    // Force reconfigure LogOutput after Spring Boot test context has initialized
    // The configureLoggers() method is private, so we use reflection
    java.lang.reflect.Method configureLoggers = LogOutput.class.getDeclaredMethod("configureLoggers");
    configureLoggers.setAccessible(true);
    configureLoggers.invoke(logOutput);
  }

  @Test(timeout = 60_000)
  public void testCpuProfiling_NoEndlessBusyLoopOnNegativeFrequency_CLM_16983() throws Exception {
    assertThat(adminRequest().path("pprof").query("frequency", -1).query("duration", "1").get()).isNotNull();
  }

  @Test
  public void testTasksShutdown_WaitsForActiveRequests() throws Exception {
    TestBlockResource testBlockResource = getCLMServer().getInstance(TestBlockResource.class);
    AtomicReference<HttpResponse> blockResponse = new AtomicReference<>();
    testTasksShutdown_WaitsFor(
        () -> tryCheckedRunnable(() -> blockResponse.set(restRequest().path("test", "block").post())),
        testBlockResource.blocker,
        Duration.ofMinutes(1),
        "[^']*ActiveRequestCounterFilter[^']*",
        () -> assertThat(restRequest().path("rest", "product", "version").get().getStatusCode()).isEqualTo(503));
    assertThat(blockResponse.get().getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testTasksShutdown_WaitsForScheduler() throws Exception {
    TaskScheduler taskScheduler = getCLMServer().getInstance(TaskScheduler.class);
    try {
      taskScheduler.disableForTesting = false;
      taskScheduler.start();
      TestBlockJob testBlockJob = getCLMServer().getInstance(TestBlockJob.class);
      testTasksShutdown_WaitsFor(
          () -> taskScheduler.scheduleOneTimeTask(testBlockJob),
          testBlockJob.blocker,
          Duration.ofMinutes(1),
          "[^']*StdScheduler[^']*");
      assertThat(taskScheduler.getScheduler()).isNull();
    }
    finally {
      taskScheduler.disableForTesting = true;
    }
  }

  @Test
  public void testTasksShutdown_WaitsForThread() throws Exception {
    ShutdownHandler shutdownHandler = getCLMServer().getInstance(ShutdownHandler.class);
    Blocker blocker = new Blocker();
    Thread thread = new Thread(() -> tryCheckedRunnable(blocker::block));
    shutdownHandler.add(thread);
    testTasksShutdown_WaitsFor(
        thread::start,
        blocker,
        Duration.ofMinutes(1),
        "[^']*" + Integer.toHexString(thread.hashCode()) + "[^']*");
    assertThat(thread.isAlive()).isFalse();
  }

  @Test
  public void testTasksShutdown_WaitsForExecutorService() throws Exception {
    ShutdownHandler shutdownHandler = getCLMServer().getInstance(ShutdownHandler.class);
    Blocker blocker = new Blocker();
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    shutdownHandler.add(executorService);
    testTasksShutdown_WaitsFor(
        () -> executorService.submit(() -> tryCheckedRunnable(blocker::block)),
        blocker,
        Duration.ofMinutes(1),
        "[^']*" + Integer.toHexString(executorService.hashCode()) + "[^']*");
  }

  private void testTasksShutdown_WaitsFor(
      final Runnable blockerTrigger,
      final Blocker blocker,
      final Duration timeout,
      final String itemDescriptionRegex,
      final CheckedRunnable... assertions) throws Exception
  {
    long extraMillisToWait = 1000;
    long end = System.currentTimeMillis() + timeout.toMillis() + extraMillisToWait;
    try {
      // Start whatever should block shutdown until we decide to unblock it
      new Thread(blockerTrigger).start();
      await().atMost(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS).until(blocker::isBlocking);

      // Start shutdown
      AtomicReference<HttpResponse> shutdownResponse = new AtomicReference<>();
      Thread shutdownTaskThread = new Thread(
          () -> tryCheckedRunnable(() -> shutdownResponse.set(adminRequest().path("tasks", "shutdown").post())));
      shutdownTaskThread.start();
      await().atMost(5, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(logOutput)
              .atDebugLevel()
              .containsPattern(
                  String.format("Initiating shutdown for order '[^']*', origin '[^']*', item '%s'.",
                      itemDescriptionRegex))
              .containsPattern(
                  String.format("Waiting for shutdown for order '[^']*', origin '[^']*', item '%s'.",
                      itemDescriptionRegex)));

      // Wait some time
      Thread.sleep(extraMillisToWait);

      // Check shutdown is still blocked
      assertThat(blocker.isBlocking()).isTrue();
      assertThat(shutdownTaskThread.isAlive()).isTrue();

      // Unblock shutdown
      blocker.unblock();
      // Wait for shutdown to finish
      await().atMost(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertThat(blocker.isBlocking()).isFalse());
      await().atMost(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertThat(shutdownResponse.get()).isNotNull());
      assertThat(shutdownResponse.get().getStatusCode()).isEqualTo(200);
      TestShutdownHandler spyTestShutdownHandler =
          (TestShutdownHandler) getCLMServer().getInstance(ShutdownHandler.class);
      verify(spyTestShutdownHandler, timeout(end - System.currentTimeMillis())).exit(0);
      for (CheckedRunnable checkedRunnable : assertions) {
        checkedRunnable.run();
      }
    }
    finally {
      stopClmServer();
    }
  }

  public static final class Blocker
  {
    private final Semaphore semaphore = new Semaphore(0);

    public void block() throws InterruptedException {
      semaphore.acquire();
    }

    public void unblock() {
      semaphore.release();
    }

    public boolean isBlocking() {
      return semaphore.hasQueuedThreads();
    }
  }

  @Named
  @Singleton
  @Path("test/block")
  public static final class TestBlockResource
  {
    private final Blocker blocker = new Blocker();

    @POST
    @UnlicensedPath
    public void block() throws InterruptedException {
      blocker.block();
    }
  }

  @Named
  @Singleton
  public static final class TestBlockJob
      implements InsightJob
  {
    private final Blocker blocker = new Blocker();

    @Override
    public String getJobName() {
      return "TestBlockJob";
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
      tryCheckedRunnable(blocker::block);
    }
  }

  private static void tryCheckedRunnable(final CheckedRunnable checkedRunnable) {
    try {
      checkedRunnable.run();
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}

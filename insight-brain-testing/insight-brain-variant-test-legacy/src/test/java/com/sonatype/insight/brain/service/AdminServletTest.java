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
import com.sonatype.insight.brain.variant.LegacyServerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@LegacyServerTest
public class AdminServletTest
    extends AbstractBrainServiceIntegrationTest
{
  @RegisterExtension
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
  @BeforeEach
  public void reconfigureLogOutput() throws Exception {
    // Force reconfigure LogOutput after Spring Boot test context has initialized
    // The configureLoggers() method is private, so we use reflection
    java.lang.reflect.Method configureLoggers = LogOutput.class.getDeclaredMethod("configureLoggers");
    configureLoggers.setAccessible(true);
    configureLoggers.invoke(logOutput);
  }

  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  public void testCpuProfiling_NoEndlessBusyLoopOnNegativeFrequency_CLM_16983() throws Exception {
    assertThat(adminRequest().path("pprof").query("frequency", -1).query("duration", "1").get()).isNotNull();
  }

  /**
   * Verifies that a graceful shutdown waits for every category of registered blocking item before exiting, and that
   * items are drained in {@link com.sonatype.insight.brain.shutdown.ShutdownPriority} order: ACTIVE_REQUESTS (an
   * in-flight REST request), then QUARTZ_SCHEDULERS (a running scheduler job), then DEFAULT (a bare thread and an
   * executor service).
   *
   * <p>
   * All four item types are registered against a single server and drained by a single {@code tasks/shutdown}, so
   * this pays for one server boot instead of one boot per item type.
   */
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  public void testTasksShutdown_WaitsForRegisteredItemsInPriorityOrder() throws Exception {
    Duration timeout = Duration.ofMinutes(1);
    long stillBlockedCheckMillis = 300;
    // The test performs one sustained-block sleep per registered item (4 items). Budget the shared deadline for all
    // of them on top of the shutdown-wait timeout, so the later await()/Mockito-timeout calls that use
    // (end - now) never see a near-zero or negative remaining budget on a loaded CI box.
    int sustainedBlockChecks = 4;
    long end =
        System.currentTimeMillis() + timeout.toMillis() + (sustainedBlockChecks * stillBlockedCheckMillis);

    TaskScheduler taskScheduler = getCLMServer().getInstance(TaskScheduler.class);
    ShutdownHandler shutdownHandler = getCLMServer().getInstance(ShutdownHandler.class);
    try {
      // ACTIVE_REQUESTS (order 0): an in-flight REST request tracked by ActiveRequestCounterFilter
      TestBlockResource testBlockResource = getCLMServer().getInstance(TestBlockResource.class);
      AtomicReference<HttpResponse> blockResponse = new AtomicReference<>();
      new Thread(() -> tryCheckedRunnable(() -> blockResponse.set(restRequest().path("test", "block").post())))
          .start();

      // QUARTZ_SCHEDULERS (order 1): a running scheduler job
      taskScheduler.disableForTesting = false;
      taskScheduler.start();
      TestBlockJob testBlockJob = getCLMServer().getInstance(TestBlockJob.class);
      new Thread(() -> taskScheduler.scheduleOneTimeTask(testBlockJob)).start();

      // DEFAULT (order 2): a bare thread and an executor service
      Blocker threadBlocker = new Blocker();
      Thread thread = new Thread(() -> tryCheckedRunnable(threadBlocker::block));
      shutdownHandler.add(thread);
      thread.start();

      Blocker executorBlocker = new Blocker();
      ExecutorService executorService = Executors.newSingleThreadExecutor();
      shutdownHandler.add(executorService);
      executorService.submit(() -> tryCheckedRunnable(executorBlocker::block));

      // Every item must be actively blocking before shutdown is initiated
      await().atMost(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
          .until(() -> testBlockResource.blocker.isBlocking()
              && testBlockJob.blocker.isBlocking()
              && threadBlocker.isBlocking()
              && executorBlocker.isBlocking());

      // Initiate shutdown
      AtomicReference<HttpResponse> shutdownResponse = new AtomicReference<>();
      Thread shutdownTaskThread = new Thread(
          () -> tryCheckedRunnable(() -> shutdownResponse.set(adminRequest().path("tasks", "shutdown").post())));
      shutdownTaskThread.start();

      // ACTIVE_REQUESTS is drained first; shutdown blocks here until the in-flight request finishes
      awaitShutdownWaitingFor("[^']*ActiveRequestCounterFilter[^']*", end);
      // New requests are rejected with 503 while shutdown is in progress
      assertThat(restRequest().path("rest", "product", "version").get().getStatusCode()).isEqualTo(503);
      // Shutdown must still be blocked after a beat (it must wait for the item, not race through)
      Thread.sleep(stillBlockedCheckMillis);
      assertThat(testBlockResource.blocker.isBlocking()).isTrue();
      assertThat(shutdownTaskThread.isAlive()).isTrue();
      testBlockResource.blocker.unblock();
      await().atMost(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertThat(blockResponse.get()).isNotNull());
      assertThat(blockResponse.get().getStatusCode()).isEqualTo(204);

      // QUARTZ_SCHEDULERS is drained next
      awaitShutdownWaitingFor("[^']*StdScheduler[^']*", end);
      // Shutdown must still be blocked on the running scheduler job — proves it actually waits, not just that
      // ShutdownHandler logged the (unconditional) "Waiting for" line before calling future.get().
      Thread.sleep(stillBlockedCheckMillis);
      assertThat(testBlockJob.blocker.isBlocking()).isTrue();
      assertThat(shutdownTaskThread.isAlive()).isTrue();
      testBlockJob.blocker.unblock();

      // DEFAULT items (thread + executor service) are drained last. Items sharing an order are initiated together but
      // waited on sequentially, so unblock the thread before expecting the executor service's wait.
      awaitShutdownWaitingFor("[^']*" + Integer.toHexString(thread.hashCode()) + "[^']*", end);
      // Shutdown must still be blocked on the DEFAULT thread item.
      Thread.sleep(stillBlockedCheckMillis);
      assertThat(threadBlocker.isBlocking()).isTrue();
      assertThat(shutdownTaskThread.isAlive()).isTrue();
      threadBlocker.unblock();
      awaitShutdownWaitingFor("[^']*" + Integer.toHexString(executorService.hashCode()) + "[^']*", end);
      // Shutdown must still be blocked on the DEFAULT executor-service item.
      Thread.sleep(stillBlockedCheckMillis);
      assertThat(executorBlocker.isBlocking()).isTrue();
      assertThat(shutdownTaskThread.isAlive()).isTrue();
      executorBlocker.unblock();

      // Shutdown completes only once every item has drained
      await().atMost(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertThat(shutdownResponse.get()).isNotNull());
      assertThat(shutdownResponse.get().getStatusCode()).isEqualTo(200);
      TestShutdownHandler spyTestShutdownHandler =
          (TestShutdownHandler) getCLMServer().getInstance(ShutdownHandler.class);
      verify(spyTestShutdownHandler, timeout(end - System.currentTimeMillis())).exit(0);
      assertThat(thread.isAlive()).isFalse();
      assertThat(taskScheduler.getScheduler()).isNull();
    }
    finally {
      taskScheduler.disableForTesting = true;
      stopClmServer();
    }
  }

  private void awaitShutdownWaitingFor(final String itemDescriptionRegex, final long end) {
    await().atMost(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        .untilAsserted(() -> assertThat(logOutput)
            .atDebugLevel()
            .containsPattern(
                String.format("Initiating shutdown for order '[^']*', origin '[^']*', item '%s'.",
                    itemDescriptionRegex))
            .containsPattern(
                String.format("Waiting for shutdown for order '[^']*', origin '[^']*', item '%s'.",
                    itemDescriptionRegex)));
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

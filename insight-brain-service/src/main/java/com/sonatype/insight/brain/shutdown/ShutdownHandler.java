/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.concurrent.LazyInitThreadPoolExecutor;
import com.sonatype.insight.brain.eventbus.AsyncEventBusImpl;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.repository.RepositoryPolicyAlertEmailer;
import com.sonatype.insight.brain.utils.CheckedRunnable;
import com.sonatype.insight.brain.utils.FIFOEntry;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class helps orchestrate an orderly shutdown.
 * <br /><br />
 * {@link ShutdownRequest}s can be added via one of the {@link ShutdownHandler#add} methods for various supported items.
 * <br /><br />
 * Each {@link ShutdownRequest} has an {@code order}.
 * <br /><br />
 * When {@link ShutdownHandler#trigger} is called, {@link ShutdownRequest}s will begin to be processed according to
 * their natural ordering (i.e. in ascending {@link ShutdownRequest#getOrder()}). Those with the same
 * {@link ShutdownRequest#getOrder()} will be processed in parallel.
 * <br /><br />
 * {@link ShutdownRequest}s can be added at any point, even whilst shutdown is happening. However, to guarantee a 
 * {@link ShutdownRequest} is processed, it should be added by something which is also blocking shutdown. Additionally,
 * if item1 invokes item2 asynchronously, then a {@link ShutdownRequest} for item1 should have a lower {@code order}
 * than a {@link ShutdownRequest} for item2.
 * <br /><br />
 * The current ordering has
 * <ol start="-2">
 *   <li>Active requests</li>
 *   <li>Quartz jobs (i.e. {@link Scheduler}s)</li>
 *   <li>Default (everything else)</li>
 *   <li>{@link LazyInitThreadPoolExecutor#getThreadPoolExecutor()}</li>
 *   <li>{@link PolicyEvaluateService#getExecutor()}</li>
 *   <li>Application notification {@link Thread}s (JIRA, email, SCM alerts) and repository policy emails
 *   {@link RepositoryPolicyAlertEmailer#getExecutor()}</li>
 *   <li>{@link AsyncEventBusImpl#getThreadPoolExecutor()}</li>
 * </ol>
 */
@Named
@Singleton
public class ShutdownHandler
{
  private static final Logger log = LoggerFactory.getLogger(ShutdownHandler.class);

  private static final int DEFAULT_ORDER = 0;

  private final ThreadFactory threadFactory;

  private final ThreadPoolExecutor threadPoolExecutor;

  private final PriorityBlockingQueue<FIFOEntry<ShutdownRequest<?>>> shutdownRequests;

  private volatile boolean triggered;

  public ShutdownHandler() {
    this(new ThreadFactoryBuilder().setNameFormat(ShutdownHandler.class.getSimpleName() + "-%d").build());
  }

  private ShutdownHandler(final ThreadFactory threadFactory) {
    this(threadFactory, new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0,
        TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory));
  }

  // Visible for testing
  ShutdownHandler(final ThreadFactory threadFactory, final ThreadPoolExecutor threadPoolExecutor) {
    this.threadFactory = threadFactory;
    this.threadPoolExecutor = threadPoolExecutor;
    shutdownRequests = new PriorityBlockingQueue<>();
  }

  public boolean isTriggered() {
    return triggered;
  }

  // Visible for testing
  void addAndClean(final ShutdownRequest<?> shutdownRequest) {
    shutdownRequests.add(new FIFOEntry<>(shutdownRequest));
    clean();
  }

  private void clean() {
    shutdownRequests.removeIf(item -> !item.getEntry().isValid());
  }

  public void add(final ExecutorService executorService) {
    add(executorService, DEFAULT_ORDER);
  }

  public void add(final ExecutorService executorService, final int order) {
    addAndClean(new ExecutorServiceShutdownRequest(new WeakReference<>(executorService), order));
  }

  public void add(final Thread thread) {
    add(thread, DEFAULT_ORDER);
  }

  public void add(final Thread thread, final int order) {
    addAndClean(new ThreadShutdownRequest(new WeakReference<>(thread), order));
  }

  public void add(final Scheduler scheduler) {
    add(scheduler, DEFAULT_ORDER);
  }

  public void add(final Scheduler scheduler, final int order) {
    addAndClean(new SchedulerShutdownRequest(new WeakReference<>(scheduler), order));
  }

  public void add(final BooleanSupplier booleanSupplier) {
    add(booleanSupplier, DEFAULT_ORDER);
  }

  public void add(final BooleanSupplier booleanSupplier, final int order) {
    addAndClean(new BooleanSupplierShutdownRequest(booleanSupplier, order));
  }

  synchronized void trigger(final Duration timeout) {
    // Prevent multiple shutdown requests
    if (isTriggered()) {
      throw new BadRequestException("Graceful shutdown already initiated.");
    }
    triggered = true;
    int statusCode = 0;
    long start = System.currentTimeMillis();
    long end = start + timeout.toMillis();
    try {
      log.info("Initiating graceful shutdown.");

      // While there are shutdown requests to process in the ordered queue
      while (!shutdownRequests.isEmpty()) {

        // For each shutdown request with the same order as the one at the head of the queue
        // initiate the shutdown, possibly in a new thread
        int order = shutdownRequests.peek().getEntry().getOrder();
        List<Future<?>> futures = new ArrayList<>();
        while (shutdownRequests.peek() != null && shutdownRequests.peek().getEntry().getOrder() == order) {
          futures.add(shutdownRequests.poll().getEntry().execute(threadPoolExecutor));
        }

        // Wait for the shutdowns for this group to complete, but do not wait beyond the timeout
        // use a separate thread to enforce a timeout in case Future#get(long, TimeUnit) is unsupported
        Future<?> groupFuture = threadPoolExecutor.submit(() -> tryCheckedRunnable(() -> {
          for (Future<?> future : futures) {
            future.get();
          }
        }));
        groupFuture.get(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
      }

      threadPoolExecutor.shutdownNow();

      log.info("Completed graceful shutdown in {} ms.", System.currentTimeMillis() - start);
    }
    catch (TimeoutException timeoutException) {
      log.error("Timed out waiting for graceful shutdown in {} ms.", System.currentTimeMillis() - start);
      statusCode = 1;
    }
    catch (InterruptedException e) {
      log.error("Interrupted graceful shutdown in {} ms.", System.currentTimeMillis() - start, e);
      statusCode = 2;
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      log.error("Failed graceful shutdown in {} ms.", System.currentTimeMillis() - start, e);
      statusCode = 3;
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error("Failed graceful shutdown in {} ms.", System.currentTimeMillis() - start, t);
      statusCode = 4;
    }
    finally {
      // Need to trigger shutdown in a different thread otherwise it can deadlock
      // System.exit makes this thread, dw.admin-X, wait for ApplicationShutdownHooks to finish
      // ApplicationShutdownHooks includes org.eclipse.jetty.util.thread.ShutdownThread
      // ShutdownThread waits for all requests to finish including this one with a timeout
      // i.e. dw.admin-X waits for ShutdownThread and then ShutdownThread waits for dw.admin-X
      exitInNewThread(statusCode);
    }
  }

  // Visible for testing
  void exitInNewThread(final int status) {
    threadFactory.newThread(() -> exit(status)).start();
  }

  // Visible for testing
  void exit(final int status) {
    System.exit(status);
  }

  static void tryCheckedRunnable(final CheckedRunnable checkedRunnable) {
    try {
      checkedRunnable.run();
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      // This is not gracefully shutting down but an Error has occurred, so it's safer to exit now
      System.exit(3);
    }
  }
}

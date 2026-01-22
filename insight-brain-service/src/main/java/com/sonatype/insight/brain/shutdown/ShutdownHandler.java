/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
 * The current ordering can be seen in {@link ShutdownPriority}.
 */
@Named
@Singleton
public class ShutdownHandler
{
  private static final Logger log = LoggerFactory.getLogger(ShutdownHandler.class);

  private final ThreadFactory threadFactory;

  private final ExecutorService executorService;

  private final PriorityBlockingQueue<FIFOEntry<ShutdownRequest<?>>> shutdownRequests;

  private final int shutdownDelayMillis;

  private volatile boolean triggered;

  private volatile boolean afterGracePeriod;

  public ShutdownHandler() {
    this(new ThreadFactoryBuilder().setNameFormat(ShutdownHandler.class.getSimpleName() + "-%d").build());
  }

  private ShutdownHandler(final ThreadFactory threadFactory) {
    this(threadFactory, Executors.newCachedThreadPool(threadFactory));
  }

  // Visible for testing
  ShutdownHandler(final ThreadFactory threadFactory, final ExecutorService executorService) {
    this.threadFactory = threadFactory;
    this.executorService = executorService;
    shutdownRequests = new PriorityBlockingQueue<>();
    shutdownDelayMillis = getShutdownDelayMillis();
  }

  private int getShutdownDelayMillis() {
    try {
      String shutdownDelayMillis = System.getenv("SHUTDOWN_DELAY_MILLIS");
      return shutdownDelayMillis == null ? 0 : Integer.parseInt(shutdownDelayMillis);
    }
    catch (NumberFormatException e) {
      log.warn("Invalid SHUTDOWN_DELAY_MILLIS value, using default of 0.", e);
      return 0;
    }
  }

  public boolean isTriggered() {
    return triggered;
  }

  public boolean isAfterGracePeriod() {
    return afterGracePeriod;
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
    add(executorService, ShutdownPriority.DEFAULT);
  }

  public void add(final ExecutorService executorService, final ShutdownPriority shutdownPriority) {
    addAndClean(new ExecutorServiceShutdownRequest(executorService, shutdownPriority.ordinal(), getOrigin()));
  }

  public void remove(final ExecutorService executorService) {
    shutdownRequests
        .removeIf(shutdownRequestEntry -> ((shutdownRequestEntry.getEntry() instanceof ExecutorServiceShutdownRequest)
            && ((ExecutorServiceShutdownRequest) shutdownRequestEntry.getEntry()).getItem().refersTo(executorService)));
  }

  public void add(final Thread thread) {
    add(thread, ShutdownPriority.DEFAULT);
  }

  public void add(final Thread thread, final ShutdownPriority shutdownPriority) {
    addAndClean(new ThreadShutdownRequest(thread, shutdownPriority.ordinal(), getOrigin()));
  }

  public void add(final Scheduler scheduler) {
    add(scheduler, ShutdownPriority.DEFAULT);
  }

  public void add(final Scheduler scheduler, final ShutdownPriority shutdownPriority) {
    addAndClean(new SchedulerShutdownRequest(scheduler, shutdownPriority.ordinal(), getOrigin()));
  }

  public void add(final BooleanSupplier booleanSupplier) {
    add(booleanSupplier, ShutdownPriority.DEFAULT);
  }

  public void add(final BooleanSupplier booleanSupplier, final ShutdownPriority shutdownPriority) {
    addAndClean(new BooleanSupplierShutdownRequest(booleanSupplier, shutdownPriority.ordinal(), getOrigin()));
  }

  private String getOrigin() {
    StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    for (StackTraceElement stackTraceElement : stackTrace) {
      if (!getClass().getName().equals(stackTraceElement.getClassName())) {
        return stackTraceElement.getClassName();
      }
    }
    return null;
  }

  synchronized void trigger(final Duration timeout, final boolean skipSystemExit) {
    // Prevent multiple shutdown requests
    if (isTriggered()) {
      throw new BadRequestException("Graceful shutdown already triggered.");
    }
    triggered = true;
    int statusCode = 0;
    long start = System.currentTimeMillis();
    long end = start + timeout.toMillis();
    try {
      log.info("Initiating graceful shutdown.");
      if (shutdownDelayMillis > 0) {
        try {
          log.info("Delaying shutdown by {} ms.", shutdownDelayMillis);
          Thread.sleep(shutdownDelayMillis);
        }
        catch (InterruptedException e) {
          log.warn("Interrupted during shutdown delay.", e);
        }
        log.info("Shutdown delay complete.");
      }
      // If no shutdown delay, consider grace period over immediately
      afterGracePeriod = true;
      // While there are shutdown requests to process in the ordered queue
      while (!shutdownRequests.isEmpty()) {

        // For each shutdown request with the same order as the one at the head of the queue
        // initiate the shutdown, possibly in a new thread
        int order = shutdownRequests.peek().getEntry().getOrder();
        List<Future<?>> futures = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        while (shutdownRequests.peek() != null && shutdownRequests.peek().getEntry().getOrder() == order) {
          ShutdownRequest<?> shutdownRequest = shutdownRequests.poll().getEntry();
          String description =
              String.format("shutdown for order '%s', origin '%s', item '%s'", shutdownRequest.getOrder(),
                  shutdownRequest.getOrigin(), shutdownRequest.getItemToString());
          log.debug("Initiating {}.", description);
          futures.add(shutdownRequest.execute(executorService));
          descriptions.add(description);
        }

        // Wait for the shutdowns for this group to complete, but do not wait beyond the timeout
        // use a separate thread to enforce a timeout in case Future#get(long, TimeUnit) is unsupported
        Future<?> groupFuture = executorService.submit(() -> tryCheckedRunnable(() -> {
          for (int i = 0; i < futures.size(); i++) {
            log.debug("Waiting for {}.", descriptions.get(i));
            futures.get(i).get();
          }
        }));
        groupFuture.get(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
      }

      executorService.shutdownNow();

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
      if (!skipSystemExit) {
        exitInNewThread(statusCode);
      }
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

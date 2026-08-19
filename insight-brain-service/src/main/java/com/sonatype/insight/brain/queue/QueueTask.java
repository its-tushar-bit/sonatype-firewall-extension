/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.queue;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Wrapper {@link Runnable} used by {@link AbstractPollDispatchQueueConsumer}.
 * <p>
 * The job ID is added to {@code queuedItemIds} in the caller before {@code submit()} is called,
 * and removed via the {@code onStart} callback when a worker thread actually picks up the task.
 * This ensures graceful shutdown only unacquires jobs that were queued but never started.
 */
public class QueueTask
    implements Runnable
{
  private final String jobId;

  private final Runnable delegate;

  private final Consumer<String> onStart;

  QueueTask(final String jobId, final Runnable delegate, final Consumer<String> onStart) {
    // jobId must be non-null because downstream sites (queuedItemIds.add, Set.of(jobId) for
    // unacquire) cannot accept null — Java immutable collections reject null elements (CLM-37961
    // / AT-024). Failing here pinpoints the offending getJobId() implementation rather than
    // surfacing as an opaque NPE deep in dispatch().
    this.jobId = Objects.requireNonNull(jobId, "jobId must not be null");
    this.delegate = delegate;
    this.onStart = onStart;
  }

  @Override
  public void run() {
    onStart.accept(jobId);
    delegate.run();
  }

  public String getJobId() {
    return jobId;
  }
}

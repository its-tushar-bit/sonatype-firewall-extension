/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;

/**
 * Consumer-side per-flow bridge that turns an acquired
 * {@link ContinuousMonitoringQueueItem} into the actual flow-specific work
 * (re-evaluate a hosted repository component, re-evaluate an SBOM, re-evaluate an application).
 * <p>
 * Per design Section 4.3 ("ContinuousMonitoringFlowProcessor is synchronous — no
 * CompletableFuture, no fire-and-forget"): {@link #process} must complete (success, failure, or
 * thrown) before returning. This is required so the caller's {@code semaphore.release()} in its
 * {@code finally} block reflects actual in-flight work and the in-flight cap is enforced.
 * <p>
 * Tenancy: the processor runs inside a {@code TenantAwareOneTimeRunnable} on the shared executor.
 * Implementations that act on tenant-owned entities (Hosted Repo: a {@code repository_id}) MUST
 * re-assert that the entity belongs to the current tenant before acting (defense in depth on the
 * shared-executor surface — Section 4.3 / STRIDE Section 9).
 */
public interface ContinuousMonitoringFlowProcessor
{
  /** The flow this processor handles; used for Guice multibinding lookup. */
  ContinuousMonitoringFlowType getFlowType();

  /**
   * Processes one acquired queue item. Must be synchronous. Throws on failure so the consumer
   * framework can apply {@link RetryPolicy}.
   */
  void process(ContinuousMonitoringQueueItem item) throws Exception;
}

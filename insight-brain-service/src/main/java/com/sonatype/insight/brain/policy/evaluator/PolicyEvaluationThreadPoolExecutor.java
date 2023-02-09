/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PolicyEvaluationThreadPoolExecutor
    extends TenantThreadPoolExecutor
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationThreadPoolExecutor.class);

  // Visible for tests
  static final int THREAD_POOL_SIZE = 200;

  public PolicyEvaluationThreadPoolExecutor() {
    super(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 5L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("PolicyEvaluateService-%d").build());
    allowCoreThreadTimeOut(true);
  }

  @Override
  public void execute(Runnable command) {
    log.debug("Policy evaluation executor state before submit: queueSize={}, activeThreads={}, totalThreads={}",
        getQueue().size(), getActiveCount(), getPoolSize());

    super.execute(command);

    int queueSize = getQueue().size();
    int activeThreadCount = getActiveCount();

    log.debug("Policy evaluation executor state after submit: queueSize={}, activeThreads={}, totalThreads={}",
        queueSize, activeThreadCount, getPoolSize());

    if (queueSize > 0 && activeThreadCount == getMaximumPoolSize()) {
      log.warn("All policy evaluation threads are busy and there are {} tasks waiting in the queue.", queueSize);
    }
  }
}

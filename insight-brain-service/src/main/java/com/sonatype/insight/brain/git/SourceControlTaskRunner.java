/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SourceControlTaskRunner
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlTaskRunner.class);

  private final Provider<PullRequestTask> pullRequestTaskProvider;

  private final PullRequestExecutor pullRequestExecutor;

  private final ThreadPoolExecutor executor;

  @Inject
  public SourceControlTaskRunner(
      final Provider<PullRequestTask> pullRequestTaskProvider,
      final PullRequestExecutor pullRequestExecutor)
  {
    this.pullRequestTaskProvider = pullRequestTaskProvider;
    this.pullRequestExecutor = pullRequestExecutor;

    this.executor = new ThreadPoolExecutor(1, 1, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ScmWorker-%s").build());
  }

  public void doPullRequestRemediation(final PullRequestRemediationDetails pullRequestRemediationDetails) {
    PullRequestTask pullRequestTask = pullRequestTaskProvider.get();
    pullRequestTask.init(pullRequestRemediationDetails, pullRequestExecutor);

    executor.execute(pullRequestTask);
    log.info("Sent for execution: pull request task for [{}] on application with id [{}]. {} tasks in the queue" +
            " and {} total tasks since startup", pullRequestRemediationDetails.getToBeRemediated(),
        pullRequestRemediationDetails.getApp().getId(), executor.getQueue().size(),
        executor.getTaskCount()
    );
  }

  public boolean isFormatSupportedForPullRequestRemediation(final ComponentIdentifier componentIdentifier) {
    return pullRequestExecutor.isSupportedFormat(componentIdentifier.getFormat());
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
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

  private final Provider<PullRequestLocationDiscoveryTask> locationDiscoveryTaskProvider;

  private final LocationDiscoveryExecutor locationDiscoveryExecutor;

  private final ThreadPoolExecutor executor;

  @Inject
  public SourceControlTaskRunner(
      final Provider<PullRequestTask> pullRequestTaskProvider,
      final PullRequestExecutor pullRequestExecutor,
      final Provider<PullRequestLocationDiscoveryTask> locationDiscoveryTaskProvider,
      final LocationDiscoveryExecutor locationDiscoveryExecutor)
  {
    this.pullRequestTaskProvider = pullRequestTaskProvider;
    this.pullRequestExecutor = pullRequestExecutor;
    this.locationDiscoveryTaskProvider = locationDiscoveryTaskProvider;
    this.locationDiscoveryExecutor = locationDiscoveryExecutor;

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

  public LocationDiscoveryResult doPullRequestLocationDiscovery(
      final List<ComponentIdentifier> componentIdentifiers,
      final GitRepositoryInfo gitRepositoryInfo,
      final String branch,
      final String applicationId) throws ExecutionException, InterruptedException
  {
    PullRequestLocationDiscoveryTask locationDiscoveryTask = locationDiscoveryTaskProvider.get();
    locationDiscoveryTask.init(
        locationDiscoveryExecutor, componentIdentifiers, gitRepositoryInfo, branch, applicationId);

    Future<LocationDiscoveryResult> future = executor.submit(locationDiscoveryTask);
    log.info(
        "Sent for execution: location discovery task for {} component(s) on application with id [{}]. {} tasks in " +
            "the queue and {} total tasks since startup", componentIdentifiers.size(),
            applicationId, executor.getQueue().size(), executor.getTaskCount());
    return future.get();
  }

  public boolean isFormatSupportedForPullRequestRemediation(final ComponentIdentifier componentIdentifier) {
    return pullRequestExecutor.isSupportedFormat(componentIdentifier.getFormat());
  }
}

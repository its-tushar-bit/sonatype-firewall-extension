/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.dropwizard.servlets.tasks.Task;

/**
 * Admin task for manually triggering the PR state update job.
 * Normally runs once daily via Quartz ({@link PullRequestStateUpdateJob}). Available at admin port 8071.
 */
@Named
@Singleton
public class TriggerPullRequestStateUpdateTask
    extends Task
{
  private final PullRequestStateService pullRequestStateService;

  @Inject
  public TriggerPullRequestStateUpdateTask(final PullRequestStateService pullRequestStateService) {
    super("triggerPullRequestStateUpdate");
    this.pullRequestStateService = pullRequestStateService;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();
    output.println("Pull request state update events dispatched.");
  }
}

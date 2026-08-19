/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the scheduled GitHub App cleanup task.
 * Deletes all inactive GitHub App records.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class GitHubAppCleanupTask
    extends AdminTask
    implements InsightJob
{
  public static final String PATH = "triggerGitHubAppCleanup";

  private static final Logger log = LoggerFactory.getLogger(GitHubAppCleanupTask.class);

  private final Provider<GitHubAppCleanupService> gitHubAppCleanupServiceProvider;

  @Inject
  public GitHubAppCleanupTask(final Provider<GitHubAppCleanupService> gitHubAppCleanupServiceProvider) {
    super(PATH);
    this.gitHubAppCleanupServiceProvider = gitHubAppCleanupServiceProvider;
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Automatic request to run GitHub App cleanup for tenant {}", TenantThreadLocal.getTenant());
    execute(gitHubAppCleanupServiceProvider.get(), log, "GitHub App cleanup error");
    log.info("Next GitHub App cleanup execution scheduled for {}", jobExecutionContext.getNextFireTime());
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    log.info("Manual request to run GitHub App cleanup");
    gitHubAppCleanupServiceProvider.get().run();
    output.write("Completed manual GitHub App cleanup execution\n");
  }

  @Override
  public String getJobName() {
    return GitHubAppCleanupTask.class.getSimpleName();
  }
}

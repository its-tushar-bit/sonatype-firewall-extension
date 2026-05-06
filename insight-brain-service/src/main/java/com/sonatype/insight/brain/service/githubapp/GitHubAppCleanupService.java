/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes all inactive GitHub App records.
 */
@Named
public class GitHubAppCleanupService
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(GitHubAppCleanupService.class);

  private final GitHubAppDAO gitHubAppDAO;

  private final GitHubAppDeletionService gitHubAppDeletionService;

  @Inject
  public GitHubAppCleanupService(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppDeletionService gitHubAppDeletionService)
  {
    this.gitHubAppDAO = gitHubAppDAO;
    this.gitHubAppDeletionService = gitHubAppDeletionService;
  }

  @Override
  public void run() {
    log.info("Starting GitHub App cleanup for tenant {}", TenantThreadLocal.getTenant());
    long start = System.currentTimeMillis();

    List<GitHubApp> candidates = gitHubAppDAO.findInactive();

    if (candidates.isEmpty()) {
      log.info("No inactive GitHub Apps eligible for cleanup");
      return;
    }

    log.info("Found {} inactive GitHub Apps eligible for cleanup", candidates.size());

    int deleted = 0;
    int failed = 0;
    for (GitHubApp app : candidates) {
      try {
        gitHubAppDeletionService.delete(app);
        deleted++;
      }
      catch (Exception e) {
        log.error("Failed to delete GitHub App for owner {}", app.getOwnerId(), e);
        failed++;
      }
    }

    log.info("GitHub App cleanup complete: candidates={}, deleted={}, failed={}, duration_ms={}",
        candidates.size(), deleted, failed, System.currentTimeMillis() - start);
  }
}

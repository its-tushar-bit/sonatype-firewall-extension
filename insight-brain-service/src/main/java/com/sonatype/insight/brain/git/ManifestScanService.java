/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;

import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.RepositorySyncCommand;
import com.sonatype.nexus.iq.manager.RepositorySyncExecutor;
import com.sonatype.nexus.iq.manager.RepositorySyncResult;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * service for doing internal scans of manifest files of a project.
 */
@Singleton
public class ManifestScanService
{
  private static final Logger log = LoggerFactory.getLogger(ManifestScanService.class);

  private final GitApiFactory gitApiFactory;

  private final InsightConfig insightConfig;

  private final SourceControlUtils sourceControlUtils;

  private final ApplicationDAO applicationDAO;

  /**
   * constructor for the manifast scan service
   * @param insightConfig an insight config
   * @param gitApiFactory a factory for the GitApi
   * @param sourceControlUtils utils for source control
   * @param applicationDAO DAO for retrieving application info
   */
  @Inject
  public ManifestScanService(
      final InsightConfig insightConfig,
      final GitApiFactory gitApiFactory,
      final SourceControlUtils sourceControlUtils,
      final ApplicationDAO applicationDAO)
  {
    this.gitApiFactory = gitApiFactory;
    this.insightConfig = insightConfig;
    this.sourceControlUtils = sourceControlUtils;
    this.applicationDAO = applicationDAO;
  }

  /**
   * process a SourceControlEvent to do a manifest scan inside the server.
   * @param event a SourceControlEvent providing application id, branch, and stage
   * @throws GitException when a git operation fails
   */
  public void onManifestScan(final SourceControlEvent event) throws GitException {
    final GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());

    if (gitRepositoryInfo == null) {
      return;
    }

    final String branch = (event.getBranchName() != null)
        ? event.getBranchName()
        : gitRepositoryInfo.getBaseBranch();

    log.trace("Manifest scan initiated for application '{}' on branch '{}'",
        event.getApplicationId(), branch);
    final File repositoryDirectory = GitRepositoryTask.getCheckoutDirectory(
        insightConfig,
        applicationDAO.getById(event.getApplicationId()).getPublicId(),
        event.getApplicationId(),
        branch);

    final GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    final RepositorySyncResult result = new RepositorySyncExecutor().execute(
        new RepositorySyncCommand(gitApi, branch, repositoryDirectory));

    log.trace("Manifest scan completed for application '{}': {}", event.getApplicationId(), result);
  }
}

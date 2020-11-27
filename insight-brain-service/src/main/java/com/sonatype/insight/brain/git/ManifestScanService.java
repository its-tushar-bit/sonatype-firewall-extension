/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.RepositorySyncCommand;
import com.sonatype.nexus.iq.manager.RepositorySyncExecutor;
import com.sonatype.nexus.iq.manager.RepositorySyncResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * service for doing internal scans of manifest files of a project.
 */
@Named
@Singleton
public class ManifestScanService
{
  private static final Logger log = LoggerFactory.getLogger(ManifestScanService.class);

  private final GitApiFactory gitApiFactory;

  private final InsightConfig insightConfig;

  private final SourceControlUtils sourceControlUtils;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluateService policyEvaluateService;

  private ProprietaryConfigService proprietaryConfigService;

  private final InsightWork work;

  private final Scanner scanner;

  private final FileCleaner fileCleaner;

  /**
   * constructor for the manifest scan service
   */
  @Inject
  public ManifestScanService(
      final InsightConfig insightConfig,
      final GitApiFactory gitApiFactory,
      final SourceControlUtils sourceControlUtils,
      final ApplicationDAO applicationDAO,
      final ProprietaryConfigService proprietaryConfigService,
      final PolicyEvaluateService policyEvaluateService,
      final InsightWork work,
      final Scanner scanner,
      final FileCleaner fileCleaner)
  {
    this.gitApiFactory = gitApiFactory;
    this.insightConfig = insightConfig;
    this.sourceControlUtils = sourceControlUtils;
    this.applicationDAO = applicationDAO;
    this.proprietaryConfigService = proprietaryConfigService;
    this.policyEvaluateService = policyEvaluateService;
    this.work = work;
    this.scanner = scanner;
    this.fileCleaner = fileCleaner;
  }

  /**
   * process a SourceControlEvent to do a manifest scan inside the server.
   *
   * @param event a SourceControlEvent providing application id, branch, and stage
   * @throws GitException when a git operation fails
   */
  public void onManifestScan(final SourceControlEvent event) throws GitException, IOException {
    log.trace("Manifest scan initiated for application '{}' on branch '{}'", event.getApplicationId(),
        event.getBranchName());

    final GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());

    if (gitRepositoryInfo == null) {
      return;
    }

    final Application application = applicationDAO.getByIdNotNull(event.getApplicationId());

    final File repositoryDirectory = GitRepositoryTask.getCheckoutDirectory(
        insightConfig,
        application.getPublicId(),
        event.getApplicationId(),
        gitRepositoryInfo.getBaseBranch());

    RepositorySyncResult repoSyncResult = checkout(gitRepositoryInfo, event.getBranchName(), repositoryDirectory);
    ScanResult scanResult = scan(application, repositoryDirectory);
    evaluate(event, application, scanResult);

    log.trace("Manifest scan completed for application '{}': {}", event.getApplicationId(), repoSyncResult);
  }

  private RepositorySyncResult checkout(GitRepositoryInfo gitRepositoryInfo, String branch, File repositoryDirectory)
      throws GitException
  {
    final GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);

    try {
      return new RepositorySyncExecutor().execute(new RepositorySyncCommand(gitApi, branch, repositoryDirectory));
    }
    catch (GitException e) {
      // clean up the local repo directory on exception; it will start fresh next time
      GitRepositoryTask.cleanDirectory(fileCleaner, repositoryDirectory);
      throw e;
    }
  }

  private ScanResult scan(Application application, File repositoryDirectory) throws IOException {
    ProprietaryConfig proprietaryConfig = proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION,
        application.getPublicId());
    return scanner.scan(repositoryDirectory, null, work.getScanDir(application.getId()), proprietaryConfig);
  }

  private void evaluate(SourceControlEvent event, Application application, ScanResult scanResult) {
    policyEvaluateService.evaluateWithPolling(event.getStatusId(), application, ClientScanType.SONATYPE,
        new Stage(event.getStageTypeId()), scanResult.getScanFile(), "api",
        event.getUserAgent());
  }
}

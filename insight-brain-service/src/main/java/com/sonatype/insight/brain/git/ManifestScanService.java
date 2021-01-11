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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
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

  private final SourceControlUtils sourceControlUtils;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluateService policyEvaluateService;

  private ProprietaryConfigService proprietaryConfigService;

  private final InsightWork work;

  private final Scanner scanner;

  @Inject
  public ManifestScanService(
      final GitApiFactory gitApiFactory,
      final SourceControlUtils sourceControlUtils,
      final ApplicationDAO applicationDAO,
      final ProprietaryConfigService proprietaryConfigService,
      final PolicyEvaluateService policyEvaluateService,
      final InsightWork work,
      final Scanner scanner)
  {
    this.gitApiFactory = gitApiFactory;
    this.sourceControlUtils = sourceControlUtils;
    this.applicationDAO = applicationDAO;
    this.proprietaryConfigService = proprietaryConfigService;
    this.policyEvaluateService = policyEvaluateService;
    this.work = work;
    this.scanner = scanner;
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

    RepositorySyncResult repoSyncResult = checkout(application, gitRepositoryInfo, event.getBranchName());
    ScanResult scanResult = scan(application);
    evaluate(event, application, scanResult);

    log.trace("Manifest scan completed for application '{}': {}", event.getApplicationId(), repoSyncResult);
  }

  private RepositorySyncResult checkout(Application application, GitRepositoryInfo gitRepositoryInfo, String branch)
      throws GitException
  {
    final GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);
    final File repositoryDirectory = sourceControlUtils.getCheckoutDirectory(application);

    try {
      return new RepositorySyncExecutor().execute(new RepositorySyncCommand(gitApi, branch, repositoryDirectory));
    }
    catch (GitException e) {
      // clean up the local repo directory on exception; it will start fresh next time
      sourceControlUtils.deleteCheckoutDirectory(application);
      throw e;
    }
  }

  private ScanResult scan(Application application) throws IOException {
    File repositoryDirectory = sourceControlUtils.getCheckoutDirectory(application);
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

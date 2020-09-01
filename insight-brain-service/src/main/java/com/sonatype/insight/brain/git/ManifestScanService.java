/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventListener;
import com.sonatype.insight.brain.git.event.SourceControlEventService;
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

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * service for doing internal scans of manifest files of a project.
 */
@Singleton
public class ManifestScanService implements SourceControlEventListener
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

  /**
   * constructor for the manifest scan service
   */
  @Inject
  public ManifestScanService(
      final InsightConfig insightConfig,
      final GitApiFactory gitApiFactory,
      final SourceControlUtils sourceControlUtils,
      final ApplicationDAO applicationDAO,
      final SourceControlEventService sourceControlEventService,
      final ProprietaryConfigService proprietaryConfigService,
      final PolicyEvaluateService policyEvaluateService,
      final InsightWork work,
      final Scanner scanner)
  {
    this.gitApiFactory = gitApiFactory;
    this.insightConfig = insightConfig;
    this.sourceControlUtils = sourceControlUtils;
    this.applicationDAO = applicationDAO;
    this.proprietaryConfigService = proprietaryConfigService;
    this.policyEvaluateService = policyEvaluateService;
    this.work = work;
    this.scanner = scanner;
    sourceControlEventService.registerEventListener(this);
  }

  @Override
  public boolean executeEvent(final SourceControlEvent event) throws GitException, IOException {
    if (SourceControlEvent.MANIFEST_SCAN_EVENT.equals(event.getEventType())) {
      onManifestScan(event);
      return true;
    }
    return false;
  }

  /**
   * process a SourceControlEvent to do a manifest scan inside the server.
   * @param event a SourceControlEvent providing application id, branch, and stage
   * @throws GitException when a git operation fails
   */
  public void onManifestScan(final SourceControlEvent event) throws GitException, IOException {
    String applicationId = event.getApplicationId();
    String statusId = event.getStatusId();
    final GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    if (gitRepositoryInfo == null) {
      return;
    }

    final String branch = event.getBranchName();

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

    Application application = applicationDAO.getByIdNotNull(applicationId);

    ProprietaryConfig proprietaryConfig = proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION,
        application.getPublicId());
    ScanResult scanResult = scanner.scan(repositoryDirectory, null, work.getScanDir(applicationId), proprietaryConfig);

    policyEvaluateService.evaluateWithPolling(statusId, application, ClientScanType.SONATYPE,
        new Stage(event.getStageTypeId()), scanResult.getScanFile(), "api",
        event.getUserAgent());

    log.trace("Manifest scan completed for application '{}': {}", event.getApplicationId(), result);
  }
}

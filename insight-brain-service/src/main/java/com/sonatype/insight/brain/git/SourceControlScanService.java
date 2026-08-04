/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultUtils;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.RepositorySyncCommand;
import com.sonatype.nexus.iq.manager.RepositorySyncExecutor;
import com.sonatype.nexus.iq.manager.RepositorySyncResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.InvalidExitValueException;

/**
 * service for doing internal scans of source control files of a project.
 */
@Named
@Singleton
public class SourceControlScanService
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlScanService.class);

  private final GitApiFactory gitApiFactory;

  private final SourceControlUtils sourceControlUtils;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluateService policyEvaluateService;

  private final PolicyEvaluationPollingResultUtils policyEvaluationPollingResultUtils;

  private final IqForScmLicenseChecker licenseChecker;

  private ProprietaryConfigService proprietaryConfigService;

  private final Scanner scanner;

  private final AuditRecorder auditRecorder;

  private final SourceControlSshService sourceControlSshService;

  @Inject
  public SourceControlScanService(
      final GitApiFactory gitApiFactory,
      final SourceControlUtils sourceControlUtils,
      final ApplicationDAO applicationDAO,
      final IqForScmLicenseChecker licenseChecker,
      final ProprietaryConfigService proprietaryConfigService,
      final PolicyEvaluateService policyEvaluateService,
      final PolicyEvaluationPollingResultUtils policyEvaluationPollingResultUtils,
      final Scanner scanner,
      final AuditRecorder auditRecorder,
      final SourceControlSshService sourceControlSshService)
  {
    this.gitApiFactory = gitApiFactory;
    this.sourceControlUtils = sourceControlUtils;
    this.applicationDAO = applicationDAO;
    this.licenseChecker = licenseChecker;
    this.proprietaryConfigService = proprietaryConfigService;
    this.policyEvaluateService = policyEvaluateService;
    this.policyEvaluationPollingResultUtils = policyEvaluationPollingResultUtils;
    this.scanner = scanner;
    this.auditRecorder = auditRecorder;
    this.sourceControlSshService = sourceControlSshService;
  }

  /**
   * process a SourceControlEvent to do a source control scan inside the server.
   *
   * @param event a SourceControlEvent providing application id, branch, and stage
   * @throws GitException when a git operation fails
   */
  public void onSourceControlScan(final SourceControlEvent event) throws GitException, IOException {
    log.trace("Source control scan initiated for application '{}' on branch '{}'", event.getApplicationId(),
        event.getBranchName());

    final Application application = applicationDAO.getByIdNotNull(event.getApplicationId());

    try {
      GitRepositoryInfo gitRepositoryInfo =
          sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
      if (gitRepositoryInfo == null) {
        log.trace("onSourceControlScan: No gitRepositoryInfo for application ID '{}'.", event.getApplicationId());
        return;
      }

      try (AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.EVALUATE_APPLICATION)) {
        try {
          AuditData.get().setApplication(application);
          AuditData.get().setStageId(event.getStageTypeId());

          RepositorySyncResult repoSyncResult = checkout(application, gitRepositoryInfo, event.getBranchName());
          ScanResult scanResult = scan(application, event.getScanTargets(), repoSyncResult.getHeadRef());
          evaluate(event, application, scanResult);

          log.trace("Source control scan completed for application '{}': {}", event.getApplicationId(), repoSyncResult);
        }
        catch (Exception e) {
          AuditData.get().setException(e);
          throw e;
        }
      }
    }
    catch (Exception e) {
      policyEvaluationPollingResultUtils.handleException(event.getApplicationId(), event.getStatusId(), e);
      throw e;
    }
  }

  public PolicyEvaluation doSynchronousSourceControlScan(
      String applicationId,
      Stage stage,
      String branchName) throws GitException, IOException
  {
    return doSynchronousSourceControlScan(applicationId, stage, branchName, null /* commitHash */);
  }

  public PolicyEvaluation doSynchronousSourceControlScan(
      String applicationId,
      Stage stage,
      String branchName,
      String commitHash) throws GitException, IOException
  {
    if (!licenseChecker.isIqForScmSupported()) {
      log.debug("License does not support source control notification or automation features");
      return null;
    }

    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    if (gitRepositoryInfo == null) {
      log.trace("doSynchronousSourceControlScan: No gitRepositoryInfo for application ID '{}'.", applicationId);
      return null;
    }

    Boolean sourceControlEvaluationsEnabled = gitRepositoryInfo.getSourceControlEvaluationsEnabled();
    if (sourceControlEvaluationsEnabled == null || !sourceControlEvaluationsEnabled.booleanValue()) {
      return null;
    }

    final Application application = applicationDAO.getByIdNotNull(applicationId);

    try (AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.EVALUATE_APPLICATION)) {
      try {
        AuditData.get().setApplication(application);
        AuditData.get().setStageId(stage.getStageTypeId());

        RepositorySyncResult repoSyncResult = checkout(application, gitRepositoryInfo, branchName, commitHash);
        ScanResult scanResult = scan(application, null /* scanTarget */, repoSyncResult.getHeadRef());
        PolicyEvaluation policyEvaluation = policyEvaluateService.evaluateSynchronousNoAuth(application,
            scanResult.getClientScanType(), scanResult.getScanEntity(), stage,
            ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST, null /* clientUserAgent */);

        log.trace("Source control scan completed for application '{}': {}", applicationId, repoSyncResult);

        return policyEvaluation;
      }
      catch (Exception e) {
        AuditData.get().setException(e);
        throw e;
      }
    }
  }

  private RepositorySyncResult checkout(
      Application application,
      GitRepositoryInfo gitRepositoryInfo,
      String branchName) throws GitException
  {
    return checkout(application, gitRepositoryInfo, branchName, null);
  }

  private RepositorySyncResult checkout(
      Application application,
      GitRepositoryInfo gitRepositoryInfo,
      String branchName,
      String commitHash) throws GitException
  {
    final GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);
    final File repositoryDirectory = sourceControlUtils.getCheckoutDirectory(application);

    sourceControlSshService.verifySshUrlAndUpdateIfNeeded(application.getId());

    try {
      return new RepositorySyncExecutor().execute(
          new RepositorySyncCommand(gitApi, branchName, commitHash, repositoryDirectory));
    }
    catch (GitException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof InvalidExitValueException && cause.getMessage() != null &&
          cause.getMessage().contains("Sparse checkout leaves no entry on working directory"))
      {
        log.debug("{} for application '{}': {}", cause.getMessage(), application.getPublicId(),
            gitRepositoryInfo.normalizedRepositoryUrl);
        return new RepositorySyncResult();
      }
      else {
        // clean up the local repo directory on exception; it will start fresh next time
        sourceControlUtils.deleteCheckoutDirectory(application);
        throw e;
      }
    }
  }

  private ScanResult scan(Application application, List<String> scanTargets, String commitHash) throws IOException {
    File repositoryDirectory = sourceControlUtils.getCheckoutDirectory(application);
    ProprietaryConfig proprietaryConfig = proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION,
        application.getPublicId());

    ScanConfiguration scanConfiguration = new ScanConfiguration();
    scanConfiguration.setProperty("dirExcludes", "**/src/test");

    ScanMetadata scanMetadata = new ScanMetadata().withCommitHash(commitHash);
    List<File> absoluteScanTargets = new ArrayList<>();
    if (scanTargets == null || scanTargets.isEmpty()) {
      absoluteScanTargets.add(repositoryDirectory);
    }
    else {
      for (String scanTarget : scanTargets) {
        absoluteScanTargets.add(new File(repositoryDirectory, scanTarget));
      }
    }
    return scanner.scan(absoluteScanTargets, application.getId(), proprietaryConfig, scanConfiguration, scanMetadata);
  }

  private void evaluate(SourceControlEvent event, Application application, ScanResult scanResult) {
    policyEvaluateService.evaluateWithPolling(event.getStatusId(), application, scanResult.getClientScanType(),
        new Stage(event.getStageTypeId()), event.getScanTriggerType(), scanResult.getScanEntity(), "api",
        event.getUserAgent(), null);
  }
}

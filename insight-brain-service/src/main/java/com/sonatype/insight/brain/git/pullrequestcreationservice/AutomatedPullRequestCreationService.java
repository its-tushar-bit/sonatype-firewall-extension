/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.pullrequestcreationservice;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.git.RemediationPullRequestEligibilityService;
import com.sonatype.insight.brain.metrics.ScmOperationMetrics;

import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.ALREADY_REMEDIATED;
import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.NOT_ELIGIBLE;
import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.NOT_GOLDEN_VERSION;
import static com.sonatype.insight.brain.metrics.ScmPrIneligibleReason.NO_REMEDIATION;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.ScmReducedSecurityService;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.innersource.InnerSourceService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class AutomatedPullRequestCreationService
    extends BasePullRequestCreationService
{
  private static final Logger log = LoggerFactory.getLogger(AutomatedPullRequestCreationService.class);

  private final ScmOperationMetrics scmOperationMetrics;

  @Inject
  public AutomatedPullRequestCreationService(
      final RemediationPullRequestEligibilityService eligibilityService,
      final BaseUrl baseUrl,
      final SourceControlUtils sourceControlUtils,
      final SourceControlEventPublisher eventPublisher,
      final OrganizationDAO organizationDAO,
      final PullRequestBranchNameGenerator pullRequestBranchNameGenerator,
      final ScmReducedSecurityService scmReducedSecurityService,
      final InnerSourceService innerSourceService,
      final ScmOperationMetrics scmOperationMetrics)
  {
    super(baseUrl,
        sourceControlUtils,
        eventPublisher,
        organizationDAO,
        pullRequestBranchNameGenerator,
        eligibilityService,
        scmReducedSecurityService,
        innerSourceService);
    this.scmOperationMetrics = scmOperationMetrics;
  }

  public void createAutomatedRemediationPullRequest(
      final Application app,
      final String scanId,
      final Stage stage,
      final ComponentIdentifier componentIdentifier,
      final Supplier<Optional<RemediationVersionDTO>> remediationVersionDTOSupplier,
      final List<PolicyNotification> notifications,
      final boolean isDirectDependency,
      final String scannedBranchName) throws IOException
  {
    boolean isInnerSourceComponent = innerSourceService.isInnerSourceComponent(componentIdentifier);

    if (!eligibilityService.isEligibleForAutoPullRequest(app, stage, componentIdentifier, isInnerSourceComponent,
        isDirectDependency, scannedBranchName))
    {
      log.debug("Component '{}' in application '{}' is not eligible for automated PR", componentIdentifier,
          app.getPublicId());
      scmOperationMetrics.recordPrCreationIneligible(NOT_ELIGIBLE);
      return;
    }

    // Only fetch the remediation version after checking basic eligibility
    Optional<RemediationVersionDTO> remediationVersionOpt = remediationVersionDTOSupplier.get();
    if (remediationVersionOpt.isEmpty()) {
      log.debug("No remediation options found for component [{}]", componentIdentifier);
      scmOperationMetrics.recordPrCreationIneligible(NO_REMEDIATION);
      return;
    }
    RemediationVersionDTO remediationVersionDTO = remediationVersionOpt.get();

    String branchName =
        pullRequestBranchNameGenerator.getBranchName(app, componentIdentifier, remediationVersionDTO.getVersion());
    if (eligibilityService.isRemediationWaitingOrDone(app.getId(), branchName)) {
      scmOperationMetrics.recordPrCreationIneligible(ALREADY_REMEDIATED);
      return;
    }

    // Non-golden PRs are suppressed unless nonGoldenPullRequestsEnabled=true. InnerSource bypasses this.
    boolean isGoldenVersion = SourceControlUtils.isGolden(remediationVersionDTO.getRemediationType());
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());

    if (isInnerSourceComponent) {
      log.debug(
          "InnerSource component detected. Attempt to create automated PR for application '{}' and component '{}'",
          app.getPublicId(), componentIdentifier);
    }
    else if (isGoldenVersion) {
      log.debug("Creating golden automated PR for application '{}' component '{}'",
          app.getPublicId(), componentIdentifier);
    }
    else if (isNonGoldenPrEnabled(gitRepositoryInfo)) {
      log.debug(
          "Creating non-golden automated PR for application '{}' component '{}' (non-golden PRs explicitly enabled)",
          app.getPublicId(), componentIdentifier);
    }
    else {
      log.debug("Suppressing non-golden automated PR for application '{}' component '{}' (golden-only default)",
          app.getPublicId(), componentIdentifier);
      scmOperationMetrics.recordPrCreationIneligible(NOT_GOLDEN_VERSION);
      return;
    }
    boolean reducedSecurityData = scmReducedSecurityService.isReducedSecurityData(app.getId());
    PullRequestRemediationDetails prDetails = new PullRequestRemediationDetails(
        componentIdentifier,
        remediationVersionDTO,
        branchName,
        notifications,
        app,
        scanId,
        stage.getStageTypeId(),
        baseUrl.get(),
        gitRepositoryInfo.provider,
        gitRepositoryInfo.normalizedRepositoryUrl,
        organizationDAO, reducedSecurityData, isInnerSourceComponent);

    SourceControlEvent sourceControlEvent = createPullRequestEvent(prDetails, false, isGoldenVersion);

    eventPublisher.publishEvent(sourceControlEvent);

    log.info("Sent automated pull request event for application '{}' component '{}'",
        app.getId(), ComponentDisplayNameUtil.fromIdentifier(componentIdentifier));
  }

  /**
   * Returns true only if non-golden automated PRs are explicitly opted in via SCM configuration.
   * NULL (the default for existing and new records) is treated as false — golden-only is the default.
   */
  private boolean isNonGoldenPrEnabled(final GitRepositoryInfo gitRepositoryInfo) {
    if (gitRepositoryInfo == null) {
      return false;
    }
    Boolean val = gitRepositoryInfo.getNonGoldenPullRequestsEnabled();
    return val != null && val;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.pullrequestcreationservice;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.git.RemediationPullRequestEligibilityService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.ScmReducedSecurityService;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.innersource.InnerSourceService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyNotificationUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ManualPullRequestCreationService
    extends BasePullRequestCreationService
{
  private static final Logger log = LoggerFactory.getLogger(ManualPullRequestCreationService.class);

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ComponentInfoService componentInfoService;

  private final ComponentRemediationService componentRemediationService;

  private final PolicyNotificationUtil policyNotificationUtil;

  private final PolicyViolationDAO policyViolationDAO;

  private final CurrentUser currentUser;

  @Inject
  public ManualPullRequestCreationService(
      final RemediationPullRequestEligibilityService eligibilityService,
      final BaseUrl baseUrl,
      final SourceControlUtils sourceControlUtils,
      final SourceControlEventPublisher eventPublisher,
      final OrganizationDAO organizationDAO,
      final PullRequestBranchNameGenerator pullRequestBranchNameGenerator,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyNotificationUtil policyNotificationUtil,
      final PolicyViolationDAO policyViolationDAO,
      final ApplicationDAO applicationDAO,
      final ComponentInfoService componentInfoService,
      final ComponentRemediationService componentRemediationService,
      final InnerSourceService innerSourceService,
      final CurrentUser currentUser,
      final ScmReducedSecurityService scmReducedSecurityService)
  {
    super(baseUrl,
        sourceControlUtils,
        eventPublisher,
        organizationDAO,
        pullRequestBranchNameGenerator,
        eligibilityService,
        scmReducedSecurityService,
        innerSourceService);
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.componentInfoService = componentInfoService;
    this.componentRemediationService = componentRemediationService;
    this.policyNotificationUtil = policyNotificationUtil;
    this.policyViolationDAO = policyViolationDAO;
    this.currentUser = currentUser;
    componentInfoService.setToolName("ci");
  }

  /**
   * Create a manual remediation pull request
   */
  public PullRequestSubmissionResultDTO createManualRemediationPullRequest(
      final String applicationId,
      final String scanId,
      final ComponentIdentifier componentIdentifier,
      final String targetVersion,
      final String identificationSource,
      final boolean isDirectDependency) throws IOException
  {
    if (targetVersion.equals(componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION))) {
      throw new BadRequestException("Target version must be different from the current version");
    }

    Application app = applicationDAO.getByIdNotNull(applicationId);
    String branchName = pullRequestBranchNameGenerator.getBranchName(app, componentIdentifier, targetVersion);
    boolean isRemediationWaitingOrDone = eligibilityService.isRemediationWaitingOrDone(app.getId(), branchName);
    if (isRemediationWaitingOrDone) {
      throw new BadRequestException(
          "A remediation event for branch name '" + branchName + "' already exists for application '" +
              app.getPublicId() + "'. Please choose a different branch name.");
    }

    PolicyEvaluation policyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanIdNotNull(app.getId(), scanId);
    PolicyEvaluation latestEvaluationByStage =
        policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), policyEvaluation.getStageTypeId());
    Stage stage = new Stage(policyEvaluation.getStageTypeId());

    if (!latestEvaluationByStage.getScanId().equals(scanId)) {
      throw new BadRequestException("The provided scan ID does not match the latest evaluation for the stage.");
    }

    if (!eligibilityService.isEligibleForManualPullRequest(app, stage, componentIdentifier, isDirectDependency)) {
      throw new BadRequestException(
          "Manual pull request creation is not eligible for application " + app.getPublicId() +
              " component " + ComponentDisplayNameUtil.fromIdentifier(componentIdentifier) +
              " in stage " + stage.getStageTypeId());
    }

    log.debug("Attempt to create manual PR for application '{}' component '{}'",
        app.getPublicId(), componentIdentifier);

    //given component version info
    ComponentVersionInfoDTO componentVersionInfoDTO = componentInfoService.getComponentVersionInfoNoAuth(
        app.getType(),
        app.getPublicId(),
        componentIdentifier,
        stage.getStageTypeId(),
        identificationSource,
        scanId,
        DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true
    );

    Optional<RemediationVersionDTO> applicableVersionChange = getApplicableVersionChange(componentVersionInfoDTO);

    if (applicableVersionChange.isEmpty()) {
      throw new BadRequestException("No applicable version change found for component " +
          ComponentDisplayNameUtil.fromIdentifier(componentIdentifier));
    }

    if (!applicableVersionChange.get().getVersion().equals(targetVersion)) {
      throw new BadRequestException(
          "Target version " + targetVersion + " does not match the applicable version change "
              + applicableVersionChange.get().getVersion() + " for component "
              + ComponentDisplayNameUtil.fromIdentifier(componentIdentifier));
    }

    boolean isInnerSourceComponent = innerSourceService.isInnerSourceComponent(componentIdentifier);
    List<PolicyNotification> notifications = Collections.emptyList();

    if (!isInnerSourceComponent) {
      // Get policy violations for remediated component
      List<PolicyViolation> remediationPolicyViolations = getRemediationPolicyViolations(
          componentVersionInfoDTO,
          targetVersion,
          componentIdentifier,
          policyEvaluation
      );

      // Get policy violations for current component
      List<PolicyViolation> policyViolations = policyViolationDAO.getByApplicationId(app.getId());
      List<PolicyViolation> componentPolicyViolations = policyViolations.stream()
          .filter(v -> v.getComponentIdentifier() != null && v.getComponentIdentifier().equals(componentIdentifier))
          .toList();
      policyViolationDAO.loadConstraintFacts(componentPolicyViolations);

      //get the diff of policy violations
      PolicyViolationDiff<PolicyViolation> policyViolationDiff =
          PolicyViolationDigester.digestPolicyViolations(componentPolicyViolations, remediationPolicyViolations);

      notifications = policyNotificationUtil.createPolicyNotifications(
          app,
          policyViolationDiff.getCleared(),
          stage.getStageTypeId(),
          policyEvaluation.isForMonitoring()
      );
    }
    else {
      log.debug("InnerSource component detected, skipping policy violations for component '{}'", componentIdentifier);
    }

    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());
    boolean reducedSecurityData = scmReducedSecurityService.isReducedSecurityData(applicationId);
    PullRequestRemediationDetails prDetails = new PullRequestRemediationDetails(
        componentIdentifier,
        applicableVersionChange.get(),
        branchName,
        notifications,
        app,
        scanId,
        stage.getStageTypeId(),
        baseUrl.get(),
        gitRepositoryInfo.provider,
        gitRepositoryInfo.normalizedRepositoryUrl,
        organizationDAO,
        true,
        currentUser.getDisplayNameOrUsername(),
        reducedSecurityData,
        isInnerSourceComponent);

    SourceControlEvent event = createPullRequestEvent(prDetails, true,
        SourceControlUtils.isGolden(applicableVersionChange.get().getRemediationType()));
    eventPublisher.publishEvent(event);

    log.info("Sent manual pull request event for application '{}' component '{}'",
        app.getId(), ComponentDisplayNameUtil.fromIdentifier(componentIdentifier));

    return new PullRequestSubmissionResultDTO(event.getId());
  }

  private Optional<RemediationVersionDTO> getApplicableVersionChange(
      final ComponentVersionInfoDTO componentVersionInfoDTO
  )
  {
    Optional<ApiVersionChangeOptionDTO> versionChange =
        componentRemediationService.getApplicableVersionChangeFromAllType(
            componentVersionInfoDTO.remediation.suggestedVersionChange,
            componentVersionInfoDTO.remediation.versionChanges
        );

    return versionChange.map(change -> new RemediationVersionDTO(
        change.getData().getComponent().componentIdentifier.getCoordinates()
            .get(ComponentIdentifier.VERSION),
        change.getType(),
        change.getData().getComponent().breakingChangesCount
    ));
  }

  /**
   * Gets the policy violations for a remediated component
   */
  private List<PolicyViolation> getRemediationPolicyViolations(
      final ComponentVersionInfoDTO componentVersionInfoDTO,
      final String targetVersion,
      final ComponentIdentifier componentIdentifier,
      final PolicyEvaluation policyEvaluation)
  {
    ComponentIdentifier remediationComponentIdentifier = componentIdentifier.createAlternativeVersion(targetVersion);
    ComponentDetailsDTO remediatedComponentDetailsDTO = componentVersionInfoDTO.allVersions.stream()
        .filter(v -> v.componentIdentifier.compareTo(remediationComponentIdentifier) == 0)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Expected remediated component version not found: " + remediationComponentIdentifier));
    return PolicyAlertUtil.getPolicyViolationsFromAlertsAndEvaluation(
        policyEvaluation,
        remediatedComponentDetailsDTO.policyAlerts
    );
  }
}

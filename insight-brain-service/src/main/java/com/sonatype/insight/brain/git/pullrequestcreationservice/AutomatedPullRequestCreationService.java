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
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
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
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
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
      final boolean isDirectDependency) throws IOException
  {
    boolean isInnerSourceComponent = innerSourceService.isInnerSourceComponent(componentIdentifier);

    if (!eligibilityService.isEligibleForAutoPullRequest(app, stage, componentIdentifier, isInnerSourceComponent,
        isDirectDependency)) {
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

    boolean isGoldenVersion = SourceControlUtils.isGolden(remediationVersionDTO.getRemediationType());

    if (!isInnerSourceComponent) {
      /*
       * A 'non-breaking with dependencies versions PR' (aka 'Golden PR') is a PR made with remediation versions of type
       * RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES only.
       * A 'regular' PR is a PR made with remediation versions of all other types.
       * A Golden PR is created for Maven components if the 'developerSuggestNonBreakingVersion' feature flag is enabled
       * and if a non-breaking with dependencies version (aka 'Golden version') is available.
       * If the component is a Maven component and the feature flag is enabled, but there is no Golden version
       * available, no PR is created.
       * A regular automated remediation PR is created for non-Maven components or when the feature flag is not enabled.
       */
      if (shouldCreateNonBreakingVersionsPR(componentIdentifier)) {
        ApiVersionChangeOptionType remediationType = remediationVersionDTO.getRemediationType();
        if (!isGoldenVersion) {
          log.debug("Remediation type for component '{}' is not golden: {}",
              componentIdentifier, remediationType);
          scmOperationMetrics.recordPrCreationIneligible(NOT_GOLDEN_VERSION);
          return;
        }
        log.debug("Attempt to create golden PR for application '{}' component '{}'",
            app.getPublicId(), componentIdentifier);
      }
      else {
        log.debug("Attempt to create automated PR for application '{}' component '{}'",
            app.getPublicId(), componentIdentifier);
      }
    }
    else {
      log.debug(
          "InnerSource component detected. Attempt to create automated PR for application '{}' and component '{}'",
          app.getPublicId(), componentIdentifier);
    }

    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());
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
   * Determines if a non-breaking version PR should be created for a component. This is specific to Automated PRs and
   * implements the "Golden PR" feature.
   */
  private boolean shouldCreateNonBreakingVersionsPR(final ComponentIdentifier componentIdentifier) {
    return componentIdentifier.isMaven() &&
        SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.isEnabled();
  }
}

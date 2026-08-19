/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.pullrequestcreationservice;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.git.RemediationPullRequestEligibilityService;
import com.sonatype.insight.brain.git.ScmReducedSecurityService;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.innersource.InnerSourceService;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

public abstract class BasePullRequestCreationService
{
  private static final String POLICY_ALERT = "policy alert";

  private static final String MANUAL_REQUEST = "manual request";

  protected final RemediationPullRequestEligibilityService eligibilityService;

  protected final BaseUrl baseUrl;

  protected final SourceControlUtils sourceControlUtils;

  protected final SourceControlEventPublisher eventPublisher;

  protected final OrganizationDAO organizationDAO;

  protected final PullRequestBranchNameGenerator pullRequestBranchNameGenerator;

  protected final ScmReducedSecurityService scmReducedSecurityService;

  protected final InnerSourceService innerSourceService;

  protected BasePullRequestCreationService(
      final BaseUrl baseUrl,
      final SourceControlUtils sourceControlUtils,
      final SourceControlEventPublisher eventPublisher,
      final OrganizationDAO organizationDAO,
      final PullRequestBranchNameGenerator pullRequestBranchNameGenerator,
      final RemediationPullRequestEligibilityService eligibilityService,
      final ScmReducedSecurityService scmReducedSecurityService,
      final InnerSourceService innerSourceService)
  {
    this.baseUrl = baseUrl;
    this.sourceControlUtils = sourceControlUtils;
    this.eventPublisher = eventPublisher;
    this.organizationDAO = organizationDAO;
    this.pullRequestBranchNameGenerator = pullRequestBranchNameGenerator;
    this.eligibilityService = eligibilityService;
    this.scmReducedSecurityService = scmReducedSecurityService;
    this.innerSourceService = innerSourceService;
  }

  /**
   * Create source control event for PR creation (both automated and manual)
   */
  protected SourceControlEvent createPullRequestEvent(
      final PullRequestRemediationDetails prDetails,
      boolean isManual,
      boolean isGolden)
  {
    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(prDetails.getToBeRemediated())
        .setApplicationId(prDetails.getApp().getId())
        .setRemediationVersion(prDetails.getRemediatedVersion())
        .setScanId(prDetails.getScanId())
        .setStageTypeId(prDetails.getStage())
        .setBranchName(prDetails.getPullRequestBranchName())
        .setPullRequestContents(prDetails.getContents())
        .setInitiator(isManual ? MANUAL_REQUEST : POLICY_ALERT)
        .setIsGoldenPullRequest(isGolden);

    return isManual ? event.forManualRemediationPullRequest() : event.forRemediationPullRequest();
  }
}

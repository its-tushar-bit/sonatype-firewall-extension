/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApplicationTotalRiskDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.error.exception.BadRequestException;

@Named
public class ApplicationSourceControlService
{
  private final SourceControlUtils sourceControlUtils;

  private final ApiSourceControlService apiSourceControlService;

  private final H2ApplicationRiskService applicationRiskService;

  @Inject
  public ApplicationSourceControlService(
      final SourceControlUtils sourceControlUtils,
      final ApiSourceControlService apiSourceControlService,
      final H2ApplicationRiskService applicationRiskService)
  {
    this.sourceControlUtils = sourceControlUtils;
    this.apiSourceControlService = apiSourceControlService;
    this.applicationRiskService = applicationRiskService;
  }

  public List<ApplicationTotalRiskDTO> getApplicationsWithAutomatedSourceControlFeedbackDisabled(
      final int limit
  )
  {
    checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    if (limit <= 0) {
      throw new BadRequestException("Limit size must be greater than 0");
    }

    return applicationRiskService.getRiskForApplicationsWithReadPermissions()
        .stream()
        .sorted((app1, app2) -> app2.totalApplicationRisk.totalRisk - app1.totalApplicationRisk.totalRisk)
        .filter(entry -> isAutomatedSourceControlFeedbackDisabledForApp(entry.id))
        .limit(limit)
        .map(entry -> new ApplicationTotalRiskDTO(entry.applicationId,
            entry.applicationName, entry.totalApplicationRisk.totalRisk))
        .collect(Collectors.toList());
  }

  public boolean isAutomatedSourceControlFeedbackEnabledForApp(final String appId) {
    return !isAutomatedSourceControlFeedbackDisabledForApp(appId);
  }

  public boolean isAutomatedSourceControlFeedbackDisabledForApp(final String appId) {
    final SourceControl sourceControl = apiSourceControlService.getCompositeSourceControlByApplicationId(appId);
    final GitRepositoryInfo gitRepositoryInfo =
        SourceControlUtils.getGitRepositoryInfoForApplicationStatic(sourceControl, appId);

    final boolean isScmEnabled = sourceControlUtils.isScmEnabled(gitRepositoryInfo);

    if (!isScmEnabled) {
      return true;
    }
    else {
      return isAutomatedSourceControlFeedbackDisabled(sourceControl);
    }
  }

  // Automated source control feedback is considered disabled when either pull request commenting or commit statuses are
  // disabled
  private boolean isAutomatedSourceControlFeedbackDisabled(final SourceControl sourceControl) {
    if (sourceControl.getPullRequestCommentingEnabled() == null) {
      return true;
    }
    // According to the GitCommitStatusService.onApplicationEvaluation() logic, null commitStatusEnabled behaves enabled
    boolean commitStatusLogicallyDisabled = Boolean.FALSE.equals(sourceControl.getCommitStatusEnabled());

    return !sourceControl.getPullRequestCommentingEnabled() || commitStatusLogicallyDisabled;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
    // The @Authorize annotation provides the implementation for this function
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

@Named
public class ApplicationSourceControlService
{
  private final SourceControlUtils sourceControlUtils;

  private final ApiSourceControlService apiSourceControlService;

  @Inject
  public ApplicationSourceControlService(
      final SourceControlUtils sourceControlUtils,
      final ApiSourceControlService apiSourceControlService)
  {
    this.sourceControlUtils = sourceControlUtils;
    this.apiSourceControlService = apiSourceControlService;
  }

  public boolean isAutomatedSourceControlFeedbackEnabledForApp(final String appId) {
    final SourceControl sourceControl = apiSourceControlService.getCompositeSourceControlByApplicationId(appId);
    final GitRepositoryInfo gitRepositoryInfo =
        SourceControlUtils.getGitRepositoryInfoForApplicationStatic(sourceControl, appId);

    final boolean isScmEnabled = sourceControlUtils.isScmEnabled(gitRepositoryInfo);

    if (!isScmEnabled) {
      return false;
    }
    else {
      return !isAutomatedSourceControlFeedbackDisabled(sourceControl);
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
}

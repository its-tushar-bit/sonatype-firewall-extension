/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Named
@Singleton
public class PullRequestRemediationService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestRemediationService.class);

  private final PullRequestExecutor pullRequestExecutor;

  private final GitClientFactory gitClientFactory;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final SourceControlUtils sourceControlUtils;

  private final Provider<PullRequestTask> pullRequestTaskProvider;

  private final SourceControlSshService sourceControlSshService;

  private final SourceControlEventDAO sourceControlEventDAO;

  @Inject
  public PullRequestRemediationService(
      PullRequestExecutor pullRequestExecutor,
      GitClientFactory gitClientFactory,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      SourceControlUtils sourceControlUtils,
      Provider<PullRequestTask> pullRequestTaskProvider,
      SourceControlSshService sourceControlSshService,
      SourceControlEventDAO sourceControlEventDAO)
  {
    this.pullRequestExecutor = pullRequestExecutor;
    this.gitClientFactory = gitClientFactory;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.sourceControlUtils = sourceControlUtils;
    this.pullRequestTaskProvider = pullRequestTaskProvider;
    this.sourceControlSshService = sourceControlSshService;
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  /**
   * Handles the source control event associated with automated/manual remediation pull requests.
   *
   * @param event contains the details needed for pull request generation
   */
  public void onRemediateComponent(SourceControlEvent event) throws IOException {
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
    if (isBranchOnServer(gitRepositoryInfo, event.getBranchName())) {
      log.info("Branch already exists on remote server for remediation [{}]", event.getBranchName());

      throw new SourceControlException(
          "Branch already exists on remote server for remediation: " + event.getBranchName()
      );
    }
    else {
      sourceControlSshService.verifySshUrlAndUpdateIfNeeded(event.getApplicationId());

      Application application = applicationDAO.getById(event.getApplicationId());
      PullRequestRemediationDetails pullRequestRemediationDetails = new PullRequestRemediationDetails(
          event.getComponentIdentifier(),
          event.getRemediationVersion(),
          event.getBranchName(),
          application,
          event.getScanId(),
          event.getStageTypeId(),
          event.getPullRequestContents(),
          organizationDAO,
          SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT.equals(event.getEventType())
      );

      PullRequestTask pullRequestTask = pullRequestTaskProvider.get();
      PullRequestResult pullRequestResult =
          pullRequestTask.run(pullRequestRemediationDetails, pullRequestExecutor);
      if (pullRequestResult.isSuccessful()) {
        event.setEventStatusDetails(pullRequestResult.getPullRequestUrl());
        sourceControlEventDAO.update(event);
      }
    }
  }

  /**
   * Determines whether or not the given component identifier represents a format that is supported for automated
   * pull requests.
   *
   * @return true if the given component identifier is for a format that is supported for automated remediation pull
   * requests; false otherwise
   */
  public boolean isFormatSupportedForPullRequestRemediation(final String format) {
    return isNotBlank(format) && pullRequestExecutor.isSupportedFormat(format);
  }

  /**
   * Uses the SCM API to determine whether or not the given branch currently exists in the git repo.
   *
   * @param gitRepositoryInfo describes the repository to check
   * @param branchName the name of the branch to check
   * @return true if the branch already exists; false otherwise
   */
  private boolean isBranchOnServer(
      final GitRepositoryInfo gitRepositoryInfo,
      final String branchName)
      throws IOException
  {
    return gitClientFactory.createApiClient(gitRepositoryInfo).isBranchOnServer(branchName);
  }
}

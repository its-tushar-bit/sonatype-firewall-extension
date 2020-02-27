/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.api.model.PullRequest;

import org.apache.commons.collections4.CollectionUtils;

class PullRequestPollingTracker
{
  private final SourceControlDAO sourceControlDAO;

  // keep track of which repositories we've polled and which ones we haven't
  private final Set<String> repositoriesPolled = new HashSet<>();

  // the org/api key (token) combo is really what drives our SCM pull request checks;  once we've checked a given pair
  // for this polling cycle there is no need to check it again until the next polling cycle
  private Set<String> orgTokensChecked = new HashSet<>();

  PullRequestPollingTracker(SourceControlDAO sourceControlDAO) {
    this.sourceControlDAO = sourceControlDAO;
  }

  /**
   * once this tracker has visited a given app repository (as identified by the source control entry) we won't visit
   * it again with this tracker
   *
   * @return next source control entry to poll or null if there are none left to poll
   */
  SourceControl getNextRepositoryToPoll() {
    SourceControl sourceControl = sourceControlDAO.getNextRepositoryToPoll();
    if (null == sourceControl || repositoriesPolled.contains(sourceControl.getId())) {
      return null;
    }

    repositoriesPolled.add(sourceControl.getId());
    return sourceControl;
  }

  void updateSourceControlPollTime(String sourceControlId, Date pullRequestPollTime) {
    sourceControlDAO.updatePullRequestPollTime(sourceControlId, pullRequestPollTime);
  }

  void updateSourceControlPollTimeForApplication(String applicationId, Date pullRequestPollTime) {
    sourceControlDAO.updatePullRequestPollTimeForApplication(applicationId, pullRequestPollTime);
  }

  void updatePullRequestPollTimes() {
    sourceControlDAO.updatePullRequestPollTimes();
  }

  boolean updateSourceControlPollTimeFromPullRequest(PullRequest pullRequest) {
    List<SourceControl> sourceControlList = sourceControlDAO.getByRepositoryOwnerAndName(pullRequest.getRepository());
    if (CollectionUtils.isNotEmpty(sourceControlList)) {
      sourceControlList.forEach(
          sourceControl -> sourceControlDAO.updatePullRequestPollTime(sourceControl.getId(), pullRequest.getCreated()));
      return true;
    }
    return false;
  }

  /**
   * (1) checks whether the given org/token pair has already been used and (2) marks it as having been used
   *
   * @return true if this org/token pair has already been used; false otherwise
   */
  boolean visitAndCheckOrganizationWithToken(String org, String token) {
    String orgAndToken = String.format("%s::%s", org, token);
    if (orgTokensChecked.contains(orgAndToken)) {
      return true;
    }

    orgTokensChecked.add(orgAndToken);
    return false;
  }
}

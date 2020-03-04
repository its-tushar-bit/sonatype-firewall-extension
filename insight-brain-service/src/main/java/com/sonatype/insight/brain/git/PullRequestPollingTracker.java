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

import static java.lang.System.currentTimeMillis;

class PullRequestPollingTracker
{
  private static final long MS_PER_MINUTE = 1000L * 60;

  private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

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

  /**
   * In the event of an error processing pull requests we need to:
   *   1 - update the error count
   *   2 - update the poll time based on the error count
   *   3 - NOT update the cutoff time
   *
   * @param sourceControlId ID of source control entry for which the error occurred
   * @return String representing the delay being applied to the next poll time
   */
  String onErrorProcessingPullRequests(String sourceControlId) {
    String result = "";
    SourceControl sourceControl = sourceControlDAO.getById(sourceControlId);
    if (null != sourceControl) {
      Date pollTime;
      int errorCount = sourceControl.getPullRequestErrorCount();
      if (errorCount < Integer.MAX_VALUE) {
        errorCount++;
      }
      switch (errorCount) {
        case 1:
          pollTime = new Date(currentTimeMillis() + (5 * MS_PER_MINUTE));
          result = "5 minutes";
          break;
        case 2:
          pollTime = new Date(currentTimeMillis() + (10 * MS_PER_MINUTE));
          result = "10 minutes";
          break;
        case 3:
          pollTime = new Date(currentTimeMillis() + (15 * MS_PER_MINUTE));
          result = "15 minutes";
          break;
        case 4:
          pollTime = new Date(currentTimeMillis() + (30 * MS_PER_MINUTE));
          result = "30 minutes";
          break;
        case 5:
          pollTime = new Date(currentTimeMillis() + MS_PER_HOUR);
          result = "1 hour";
          break;
        case 6:
          pollTime = new Date(currentTimeMillis() + (6 * MS_PER_HOUR));
          result = "6 hours";
          break;
        case 7:
          pollTime = new Date(currentTimeMillis() + (12 * MS_PER_HOUR));
          result = "12 hours";
          break;
        default:
          pollTime = new Date(currentTimeMillis() + (24 * MS_PER_HOUR));
          result = "24 hours";
          break;
      }
      updateSourceControl(sourceControl, pollTime, errorCount);
    }
    return result;
  }

  void onPullRequestProcessed(String sourceControlId, Date time) {
    updateSourceControl(sourceControlDAO.getById(sourceControlId), time, time, 0);
  }

  void onPullRequestProcessedForApplication(String applicationId, Date time) {
    updateSourceControl(sourceControlDAO.getByOwnerId(applicationId), time, time, 0);
  }

  /**
   * For the given pull request object find and update any source control entries that match on the repo owner/name
   * specified in the pull request
   *
   * @param pullRequest PullRequest object
   * @return true if there were any source control entries matching the given pull request, false otherwise
   */
  boolean onPullRequestProcessed(PullRequest pullRequest) {
    List<SourceControl> sourceControlList = sourceControlDAO.getByRepositoryOwnerAndName(pullRequest.getRepository());
    if (CollectionUtils.isNotEmpty(sourceControlList)) {
      Date created = pullRequest.getCreated();
      sourceControlList.forEach(sourceControl -> updateSourceControl(sourceControl, created, created, 0));
      return true;
    }
    return false;
  }

  private void updateSourceControl(SourceControl sourceControl, Date pollTime, int errors) {
    updateSourceControl(sourceControl, pollTime, null, errors);
  }

  // cutoffTime is optional and is only updated if provided
  private void updateSourceControl(SourceControl sourceControl, Date pollTime, Date cutoffTime, int errors) {
    if (null != sourceControl) {
      sourceControl.setPullRequestPollTime(pollTime);
      if (null != cutoffTime) {
        sourceControl.setPullRequestCutoffTime(cutoffTime);
      }
      sourceControl.setPullRequestErrorCount(errors);
      sourceControlDAO.update(sourceControl);
    }
  }

  void initializePullRequestPollTimes() {
    sourceControlDAO.initializePullRequestPollTimes();
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

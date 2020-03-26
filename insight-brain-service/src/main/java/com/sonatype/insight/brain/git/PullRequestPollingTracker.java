/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  // keep track of cutoff times at the org-token level as multiple repos can share a token and we need to make sure
  // the cutoff time keeps advancing
  private static final Map<String, Date> orgTokenCutoffTimes = new HashMap<>();

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
      updateSourceControl(sourceControl.getId(), pollTime, errorCount);
    }
    return result;
  }

  void onPullRequestProcessed(String sourceControlId, String org, String token, Date time) {
    updateSourceControl(sourceControlId, time, 0);
    setCachedCutoffTime(org, token, time);
  }

  void onPullRequestProcessedForApplication(String applicationId, Date time) {
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(applicationId);
    if (sourceControl != null) {
      updateSourceControl(sourceControl.getId(), time, 0);
    }
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
      sourceControlList.forEach(sourceControl -> updateSourceControl(sourceControl.getId(), created, 0));
      return true;
    }
    return false;
  }

  private void updateSourceControl(String sourceControlId, Date pollTime, int errors) {
    sourceControlDAO.updatePollTimeAndErrorCounts(sourceControlId, pollTime, errors);
  }

  void initializePullRequestPollTimes() {
    sourceControlDAO.initializePullRequestPollTimes();
  }

  Date getCachedCutoffTime(String org, String token, Date defaultCutoffTime) {
    String orgAndToken = makeKey(org, token);
    return orgTokenCutoffTimes.computeIfAbsent(orgAndToken, k -> defaultCutoffTime);
  }

  private void setCachedCutoffTime(String org, String token, Date cutoffTime) {
    String orgAndToken = makeKey(org, token);
    orgTokenCutoffTimes.put(orgAndToken, cutoffTime);
  }

  /**
   * (1) checks whether the given org/token pair has already been used and (2) marks it as having been used
   *
   * @return true if this org/token pair has already been used; false otherwise
   */
  boolean visitAndCheckOrganizationWithToken(String org, String token) {
    String orgAndToken = makeKey(org, token);
    if (orgTokensChecked.contains(orgAndToken)) {
      return true;
    }

    orgTokensChecked.add(orgAndToken);
    return false;
  }

  private String makeKey(String org, String token) {
    return String.format("%s::%s", org, token);
  }
}

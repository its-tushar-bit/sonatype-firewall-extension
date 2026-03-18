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
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.base.Joiner;

import static java.lang.System.currentTimeMillis;

class PullRequestPollingTracker
{
  private static final long MS_PER_MINUTE = 1000L * 60;

  private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

  // keep track of cutoff times at the appropriate level as multiple repos can share a token and we need to make sure
  // the cutoff time keeps advancing
  private static final TenantReference<Map<String, Date>> keyCutoffTimes = new TenantReference<>(HashMap::new);

  private static final Joiner KEY_JOINER = Joiner.on("::").skipNulls();

  private final SourceControlDAO sourceControlDAO;

  // keep track of which repositories (by internal id) we've polled and which ones we haven't
  private final TenantReference<Set<String>> repositoriesPolled = new TenantReference<>(HashSet::new);

  // the org/repo/api key (token) combo is really what drives our SCM pull request checks; once we've checked a given
  // combo for this polling cycle there is no need to check it again until the next polling cycle
  private final TenantReference<Set<String>> alreadyCheckedKeys = new TenantReference<>(HashSet::new);

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
    if (null == sourceControl || repositoriesPolled.get().contains(sourceControl.getId())) {
      return null;
    }

    repositoriesPolled.get().add(sourceControl.getId());
    return sourceControl;
  }

  /**
   * In the event of an error processing PRs we need to do the following for all records with the same repository URL:
   * 1 - update the error count
   * 2 - update the poll time based on the error count
   * 3 - NOT update the cutoff time
   *
   * @param sourceControl source control entry for which the error occurred
   * @return String representing the delay being applied to the next poll time
   */
  String onErrorProcessingPullRequests(SourceControl sourceControl) {
    String result = "";
    List<SourceControl> sourceControlList = sourceControlDAO.getByRepositoryUrl(sourceControl.getRepositoryUrl());
    for (SourceControl sourceControlRecord : sourceControlList) {
      Date pollTime;
      int errorCount = sourceControlRecord.getPullRequestErrorCount();
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
      updateSourceControl(sourceControlRecord, pollTime, errorCount);
    }
    return result;
  }

  /**
   * All records with the same repository URL have to be updated; otherwise the PR polling cycle finishes prematurely.
   *
   * @param sourceControl the sourceControl we are processing PRs for
   * @param org organization processing PRs
   * @param repo repo processing PRs, may be null if polling can take place organization wide
   * @param token token used for processing PRs
   * @param cutoffTime next time to use for polling cutoff
   */
  void onPullRequestProcessed(SourceControl sourceControl, String org, String repo, String token, Date cutoffTime) {
    List<SourceControl> sourceControlList = sourceControlDAO.getByRepositoryUrl(sourceControl.getRepositoryUrl());
    for (SourceControl sourceControlRecord : sourceControlList) {
      updateSourceControl(sourceControlRecord, cutoffTime, 0);
    }
    setCachedCutoffTime(org, repo, token, cutoffTime);
  }

  void onPullRequestProcessedForApplication(String applicationId, Date time) {
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(applicationId);
    if (sourceControl != null) {
      updateSourceControl(sourceControl, time, 0);
    }
  }

  private void updateSourceControl(SourceControl sourceControl, Date pollTime, int errors) {
    // Don't move the PR poll time back in time
    if (sourceControl.getPullRequestPollTime() != null && pollTime.before(sourceControl.getPullRequestPollTime())) {
      pollTime = sourceControl.getPullRequestPollTime();
    }
    sourceControlDAO.updatePollTimeAndErrorCounts(sourceControl.getId(), pollTime, errors);
  }

  void initializePullRequestPollTimes() {
    sourceControlDAO.initializePullRequestPollTimes();
  }

  Date getCachedCutoffTime(String org, String repo, String token, Date defaultCutoffTime) {
    String key = makeKey(org, repo, token);
    return keyCutoffTimes.get().computeIfAbsent(key, k -> defaultCutoffTime);
  }

  private void setCachedCutoffTime(String org, String repo, String token, Date cutoffTime) {
    String key = makeKey(org, repo, token);
    keyCutoffTimes.get().put(key, cutoffTime);
  }

  /**
   * (1) checks whether the given org/repo/token pair has already been used and (2) marks it as having been used
   *
   * @return true if this org/repo/token combination has already been used; false otherwise
   */
  boolean visitAndCheckKeyAlreadyUsed(String org, String repo, String token) {
    String key = makeKey(org, repo, token);
    if (alreadyCheckedKeys.get().contains(key)) {
      return true;
    }

    alreadyCheckedKeys.get().add(key);
    return false;
  }

  private String makeKey(String org, String repo, String token) {
    return KEY_JOINER.join(org, repo, token);
  }
}

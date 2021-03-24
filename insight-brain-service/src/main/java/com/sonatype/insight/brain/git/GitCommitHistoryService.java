/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.nexus.scm.api.model.Commit;

import com.google.common.base.Strings;
import org.apache.commons.collections4.CollectionUtils;

@Named
@Singleton
public class GitCommitHistoryService
{
  private final SourceControlDefaultBranchCommitHistoryDAO commitHistoryDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public GitCommitHistoryService(
      final SourceControlDefaultBranchCommitHistoryDAO commitHistoryDAO,
      final PolicyEvaluationDAO policyEvaluationDAO)
  {
    this.commitHistoryDAO = commitHistoryDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  public Optional<PolicyEvaluation> getLatestPolicyEvaluationForApplicationBaseBranch(final String applicationId) {
    if (Strings.isNullOrEmpty(applicationId)) {
      return Optional.empty();
    }

    final SourceControlDefaultBranchCommitHistory sourceControlDefaultBranchCommitHistory =
        commitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(applicationId);

    if (sourceControlDefaultBranchCommitHistory != null) {
      return Optional
          .ofNullable(policyEvaluationDAO.getById(sourceControlDefaultBranchCommitHistory.getPolicyEvaluationId()));
    }
    return Optional.empty();
  }

  /**
   * update the commit history associated with the given policy evaluation, if one exists; this satisfies the case
   * where the policy eval against the base branch occurs after we've recorded the base branch commit history we
   * received from the SCM system for a feature branch policy eval
   */
  public void updateCommitHistoryForPolicyEvaluation(final PolicyEvaluation policyEvaluation) {
    if (null == policyEvaluation || Strings.isNullOrEmpty(policyEvaluation.getCommitHash())) {
      return;
    }
    SourceControlDefaultBranchCommitHistory commitHistory = commitHistoryDAO
        .getByApplicationIdAndCommitHash(policyEvaluation.getApplicationId(), policyEvaluation.getCommitHash());
    if (null != commitHistory) {
      recordCommitHistoryUpdate(commitHistory, policyEvaluation.getId());
      purgeOldCommitHistory(policyEvaluation.getApplicationId(), commitHistory.getCommitTime());
    }
  }

  /**
   * update the commit history for the given application using the given list of commits
   */
  public void updateCommitHistoryForCommits(
      final String applicationId,
      final List<Commit> commits)
  {
    if (null == applicationId || CollectionUtils.isEmpty(commits)) {
      return;
    }

    // get most recent entry with a policy eval
    SourceControlDefaultBranchCommitHistory mostRecentHistoryWithPolicyEval =
        commitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(applicationId);

    for (Commit commit : commits) {
      // is the current commit more recent than the latest one we have with a policy eval?
      if (null == mostRecentHistoryWithPolicyEval ||
          commit.getCommittedDate().after(mostRecentHistoryWithPolicyEval.getCommitTime())) {

        SourceControlDefaultBranchCommitHistory moreRecentHistoryWithPolicyEval =
            updateCommitHistoryForCommit(applicationId, commit);

        if (null != moreRecentHistoryWithPolicyEval) {
          mostRecentHistoryWithPolicyEval = moreRecentHistoryWithPolicyEval;
        }
      }
    }

    // clean out old, unneeded commit history entries
    if (null != mostRecentHistoryWithPolicyEval) {
      purgeOldCommitHistory(applicationId, mostRecentHistoryWithPolicyEval.getCommitTime());
    }
  }

  /**
   * update the commit history for the given single commit
   *
   * @return the created or updated SourceControlDefaultBranchCommitHistory object, if there was one, AND iff the given
   *         commit has a related policy evaluation, null otherwise
   */
  private SourceControlDefaultBranchCommitHistory updateCommitHistoryForCommit(String applicationId, Commit commit) {
    SourceControlDefaultBranchCommitHistory moreRecentCommitHistory = null;

    PolicyEvaluation newestPolicyEvaluationForCommit =
        policyEvaluationDAO.getLastByApplicationAndCommitHash(applicationId, commit.getHash());

    String policyEvaluationId =
        null != newestPolicyEvaluationForCommit ? newestPolicyEvaluationForCommit.getId() : null;

    SourceControlDefaultBranchCommitHistory commitHistory =
        commitHistoryDAO.getByApplicationIdAndCommitHash(applicationId, commit.getHash());

    if (null == commitHistory) {
      // this is a new entry
      commitHistory = new SourceControlDefaultBranchCommitHistory(
          applicationId, commit.getHash(), commit.getCommittedDate(), policyEvaluationId);
      commitHistoryDAO.insert(commitHistory);
      if (null != newestPolicyEvaluationForCommit) {
        moreRecentCommitHistory = commitHistory;
      }
    }
    else if (null == policyEvaluationId || !policyEvaluationId.equals(commitHistory.getPolicyEvaluationId())) {
      // update the existing entry
      recordCommitHistoryUpdate(commitHistory, policyEvaluationId);
      if (null != newestPolicyEvaluationForCommit) {
        moreRecentCommitHistory = commitHistory;
      }
    }

    if (null != newestPolicyEvaluationForCommit) {
      moreRecentCommitHistory = commitHistory;
    }

    return moreRecentCommitHistory;
  }

  private void purgeOldCommitHistory(String applicationId, Date cutoffDate) {
    commitHistoryDAO.deleteByApplicationIdBeforeCommitTime(applicationId, cutoffDate);
  }

  private void recordCommitHistoryUpdate(
      SourceControlDefaultBranchCommitHistory commitHistory,
      String policyEvaluationId)
  {
    commitHistory.setPolicyEvaluationId(policyEvaluationId);
    commitHistory.setUpdateTime(new Date());
    commitHistoryDAO.update(commitHistory);
  }

  public SourceControlDefaultBranchCommitHistory getLatestCommitForApplication(String applicationId) {
    return commitHistoryDAO.getLatestCommitForApplicationId(applicationId);
  }
}

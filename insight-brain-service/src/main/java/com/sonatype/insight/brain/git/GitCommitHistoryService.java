/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.nexus.scm.api.model.Commit;

import com.google.common.base.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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

  public Optional<PolicyEvaluation> getLatestPolicyEvaluationForApplicationBaseBranch(
      final String applicationId,
      final boolean externallyTriggered)
  {
    if (Strings.isNullOrEmpty(applicationId)) {
      return Optional.empty();
    }

    final SourceControlDefaultBranchCommitHistory sourceControlDefaultBranchCommitHistory =
        commitHistoryDAO.getForLatestCommitWithPolicyEvaluation(applicationId, externallyTriggered);

    if (sourceControlDefaultBranchCommitHistory != null) {
      return Optional
          .ofNullable(policyEvaluationDAO.getById(sourceControlDefaultBranchCommitHistory.getPolicyEvaluationId()));
    }
    return Optional.empty();
  }

  public void updateCommitHistoryForPolicyEvaluation(final String policyEvaluationId) {
    if (StringUtils.isNotEmpty(policyEvaluationId)) {
      PolicyEvaluation policyEvaluation = policyEvaluationDAO.getById(policyEvaluationId);
      updateCommitHistoryForPolicyEvaluation(policyEvaluation);
    }
  }

  /**
   * update the commit history associated with the given policy evaluation, if one exists; this satisfies the case
   * where the policy eval against the base branch occurs after we've recorded the base branch commit history we
   * received from the SCM system for a feature branch policy eval
   * <br/>
   * Externally triggered policy evaluations overwrite any existing values; internally triggered ones overwrite
   * only {@code null} or other internally triggered IDs
   */
  public void updateCommitHistoryForPolicyEvaluation(final PolicyEvaluation policyEvaluation) {
    if (null == policyEvaluation || Strings.isNullOrEmpty(policyEvaluation.getCommitHash())) {
      return;
    }
    SourceControlDefaultBranchCommitHistory commitHistory = commitHistoryDAO
        .getByApplicationIdAndCommitHash(policyEvaluation.getApplicationId(), policyEvaluation.getCommitHash());
    if (null != commitHistory) {
      if (commitHistory.getPolicyEvaluationId() == null) { // no policy eval. associated with the commit
        recordCommitHistoryUpdate(commitHistory, policyEvaluation.getId());
      }
      else { // existing policy eval. associated with the commit
        if (policyEvaluation.wasInternallyTriggered()) {
          // update association only if the previous eval. was internally triggered
          PolicyEvaluation associatedPolicyEvaluation =
              policyEvaluationDAO.getById(commitHistory.getPolicyEvaluationId());
          if (associatedPolicyEvaluation.wasInternallyTriggered()) {
            recordCommitHistoryUpdate(commitHistory, policyEvaluation.getId());
          }
        }
        else { // externally triggered policy evaluation
          recordCommitHistoryUpdate(commitHistory, policyEvaluation.getId());
        }
      }
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
  }

  /**
   * update the commit history for the given single commit
   *
   * @return the created or updated SourceControlDefaultBranchCommitHistory object, if there was one, AND iff the given
   * commit has a related policy evaluation, null otherwise
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

  private void recordCommitHistoryUpdate(
      SourceControlDefaultBranchCommitHistory commitHistory,
      String policyEvaluationId)
  {
    commitHistory.setPolicyEvaluationId(policyEvaluationId);
    commitHistory.setUpdateTime(new Date());
    commitHistoryDAO.update(commitHistory);
  }

  public String getLatestCommitForApplication(String applicationId) {
    SourceControlDefaultBranchCommitHistory commitHistory =
        commitHistoryDAO.getLatestCommitForApplicationId(applicationId);
    return null != commitHistory ? commitHistory.getCommitHash() : null;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityExistsException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class SourceControlDefaultBranchCommitHistoryDAOTest
    extends AbstractDbDAOTest
{
  private final SourceControlDefaultBranchCommitHistoryDAO defaultBranchCommitHistoryDAO =
      new SourceControlDefaultBranchCommitHistoryDAO();

  @Test
  public void testCRUD() throws InterruptedException {
    // given : a commit history entry not linked to a policy evaluation entry
    String commitHash = "commit";
    Date commitTime = new Date();
    SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory = tempEntity
        .newSourceControlDefaultBranchCommitHistory(application.getId(), commitHash, commitTime, null);

    // when : fetch by ID
    SourceControlDefaultBranchCommitHistory fetchedCommitHistory =
        defaultBranchCommitHistoryDAO.getById(defaultBranchCommitHistory.getId());

    // then : entry exists
    assertThat(fetchedCommitHistory).isNotNull();
    assertThat(fetchedCommitHistory.getApplicationId()).isEqualTo(application.getId());
    assertThat(fetchedCommitHistory.getCommitHash()).isEqualTo(commitHash);
    assertThat(fetchedCommitHistory.getPolicyEvaluationId()).isNull();
    assertThat(fetchedCommitHistory.getCommitTime()).isEqualTo(commitTime);
    assertThat(fetchedCommitHistory.getCreateTime()).isEqualTo(defaultBranchCommitHistory.getCreateTime());
    assertThat(fetchedCommitHistory.getUpdateTime()).isNull();

    // when : update the entry to add in the policy evaluation
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "scan", commitHash);
    fetchedCommitHistory.setPolicyEvaluationId(policyEvaluation.getId());
    // make sure update time should be after create time
    Thread.sleep(10);
    defaultBranchCommitHistoryDAO.update(fetchedCommitHistory);

    // when : fetch by app id and policy eval ID
    fetchedCommitHistory = defaultBranchCommitHistoryDAO.getByApplicationIdAndPolicyEvaluationId(
        application.getId(), policyEvaluation.getId());

    // then : we get the same entry, only updated
    assertThat(fetchedCommitHistory).isNotNull();
    assertThat(fetchedCommitHistory.getApplicationId()).isEqualTo(application.getId());
    assertThat(fetchedCommitHistory.getCommitHash()).isEqualTo(commitHash);
    assertThat(fetchedCommitHistory.getPolicyEvaluationId()).isEqualTo(policyEvaluation.getId());
    assertThat(fetchedCommitHistory.getCommitTime()).isEqualTo(commitTime);
    assertThat(fetchedCommitHistory.getCreateTime()).isEqualTo(defaultBranchCommitHistory.getCreateTime());
    assertThat(fetchedCommitHistory.getUpdateTime()).isAfter(fetchedCommitHistory.getCreateTime());

    // when : fetch by app id and commit hash
    fetchedCommitHistory =
        defaultBranchCommitHistoryDAO.getByApplicationIdAndCommitHash(application.getId(), commitHash);

    // then : again, got same entry
    assertThat(fetchedCommitHistory).isNotNull();
    assertThat(fetchedCommitHistory.getApplicationId()).isEqualTo(application.getId());
    assertThat(fetchedCommitHistory.getCommitHash()).isEqualTo(commitHash);
    assertThat(fetchedCommitHistory.getPolicyEvaluationId()).isEqualTo(policyEvaluation.getId());
    assertThat(fetchedCommitHistory.getCommitTime()).isEqualTo(commitTime);
    assertThat(fetchedCommitHistory.getCreateTime()).isEqualTo(defaultBranchCommitHistory.getCreateTime());
    assertThat(fetchedCommitHistory.getUpdateTime()).isAfter(fetchedCommitHistory.getCreateTime());

    // when : delete entry and re-fetch
    defaultBranchCommitHistoryDAO.delete(fetchedCommitHistory);
    fetchedCommitHistory = defaultBranchCommitHistoryDAO.getById(fetchedCommitHistory.getId());

    // then : entry no longer exists
    assertThat(fetchedCommitHistory).isNull();
  }

  @Test
  public void testGetByApplicationIdForLatestCommitWithPolicyEvaluation() {
    // given : several commit history entries with and without associated policy violations
    String policyEvaluationId1 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1", "commit1")
        .getId();
    SourceControlDefaultBranchCommitHistory oldestCommitWithEval =
        tempEntity.newSourceControlDefaultBranchCommitHistory(
            application.getId(), "commit1", createTime(-60), policyEvaluationId1);

    String policyEvaluationId2 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan2", "commit2")
        .getId();
    SourceControlDefaultBranchCommitHistory newestCommitWithEval =
        tempEntity.newSourceControlDefaultBranchCommitHistory(
            application.getId(), "commit2", createTime(-30), policyEvaluationId2);

    SourceControlDefaultBranchCommitHistory middleCommitNoEval = tempEntity.newSourceControlDefaultBranchCommitHistory(
        application.getId(), "commit3", createTime(-45), null);

    // when : fetch latest commit with eval
    SourceControlDefaultBranchCommitHistory fetchedCommitHistory =
        defaultBranchCommitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(application.getId());

    // then : should be the newest entry
    assertThat(fetchedCommitHistory.getId()).isEqualTo(newestCommitWithEval.getId());

    // when : remove newest entry and refetch
    defaultBranchCommitHistoryDAO.delete(fetchedCommitHistory);
    fetchedCommitHistory = defaultBranchCommitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(
        application.getId());

    // then : should skip middle entry (has no policy eval) and fetch the oldest one
    assertThat(fetchedCommitHistory.getId()).isEqualTo(oldestCommitWithEval.getId());

    // when : update the middle entry to have a policy eval and refetch
    String policyEvaluationId3 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan3", "commit3")
        .getId();
    middleCommitNoEval.setPolicyEvaluationId(policyEvaluationId3);
    defaultBranchCommitHistoryDAO.update(middleCommitNoEval);
    fetchedCommitHistory = defaultBranchCommitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(
        application.getId());

    // then : middle entry should be the one returned as it's now the newest with a policy eval
    assertThat(fetchedCommitHistory.getId()).isEqualTo(middleCommitNoEval.getId());
  }

  @Test
  public void testDeleteByPolicyEvaluationId() {
    // given : commit history linked to policy evaluations
    String policyEvaluationId =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan", "commit1")
        .getId();
    String policyEvaluationId2 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan", "commit2")
        .getId();
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit1", new Date(),
        policyEvaluationId);
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit2", new Date(),
        policyEvaluationId2);

    // when : delete using the policy evaluation ID
    defaultBranchCommitHistoryDAO.deleteByPolicyEvaluationId(policyEvaluationId);

    // then : entry doesn't exist in DB
    assertThat(defaultBranchCommitHistoryDAO.getByPolicyEvaluationId(policyEvaluationId)).isEmpty();

    // and : entry for policy eval 2 still exists
    assertThat(defaultBranchCommitHistoryDAO.getByPolicyEvaluationId(policyEvaluationId2)).isNotEmpty();
  }

  @Test
  public void testDeleteByApplicationIdBeforeCommitTime() {
    // given : a set of commit history entries, some linked to a policy evaluation, some not, and some for a different
    //         application
    String policyEvaluationId =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan", "commit1")
        .getId();
    tempEntity.newSourceControlDefaultBranchCommitHistory(
        application.getId(), "commit1", createTime(-75), policyEvaluationId);
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit2", createTime(-70), null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit3", createTime(-90), null);
    Date cutoffDateTime = createTime(-30);
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit4", cutoffDateTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit5", createTime(-20), null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit6", createTime(-220), null);
    Application app2 = tempEntity.newApplication("app2", organization.getId());
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit7", createTime(-180), null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit8", createTime(-120), null);

    // when : fetch all entries for first application
    List<SourceControlDefaultBranchCommitHistory> commitHistoryList =
        defaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then : all entries for given app and count is correct
    assertThat(commitHistoryList.size()).isEqualTo(6);
    commitHistoryList.forEach(entry -> assertThat(entry.getApplicationId()).isEqualTo(application.getId()));

    // when : delete entries for first application older than cutoff date
    defaultBranchCommitHistoryDAO.deleteByApplicationIdBeforeCommitTime(application.getId(), cutoffDateTime);

    // then : history older than cutoff time not in results for first application
    commitHistoryList = defaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    assertThat(commitHistoryList.size()).isEqualTo(2);
    commitHistoryList.forEach(entry -> {
      assertThat(entry.getApplicationId()).isEqualTo(application.getId());
      assertThat(entry.getCommitTime()).isAfterOrEqualTo(cutoffDateTime);
    });

    // and : history for app2 not affected
    commitHistoryList = defaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(app2.getId());
    assertThat(commitHistoryList.size()).isEqualTo(2);
    commitHistoryList.forEach(entry -> assertThat(entry.getApplicationId()).isEqualTo(app2.getId()));
  }

  @Test
  public void testInsert_invalidInputs() {
    // when : try to insert with invalid app
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlDefaultBranchCommitHistory(
        "bogusAppId", "commit", new Date(), null));

    // then : expecting exception
    assertThat(thrown).hasStackTraceContaining(
        "Referential integrity constraint violation: \"source_control_default_branch_commit_history_application_fk");

    // when : try to insert with invalid policy eval ID
    thrown = catchThrowable(() -> tempEntity.newSourceControlDefaultBranchCommitHistory(
        application.getId(), "commit", new Date(), "bogusPolicyEvaluationId"));

    // then : expecting exception
    assertThat(thrown).hasStackTraceContaining(
        "Referential integrity constraint violation: \"source_control_default_branch_commit_history_policy_eval_fk");

    // when : try to insert with missing commit
    thrown = catchThrowable(() -> tempEntity.newSourceControlDefaultBranchCommitHistory(
        application.getId(), null, new Date(), null));

    // then : expecting exception
    assertThat(thrown).hasStackTraceContaining("NULL not allowed for column \"commit_hash\"");
  }

  @Test
  public void testInsert_duplicateApplicationIdAndCommit() {
    // given : a commit history entry
    String commitHash = "commit";
    Date commitTime = new Date();
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), commitHash, commitTime, null);

    // when : insert a duplicate commit history (same app and commit)
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlDefaultBranchCommitHistory(
        application.getId(), commitHash, commitTime, null));

    // then : exception thrown due to duplicate
    assertThat(thrown).hasCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testUpdate_invalidPolicyEvaluationReference() {
    // given : a commit history entry without a policy eval reference
    SourceControlDefaultBranchCommitHistory commitHistory = tempEntity.newSourceControlDefaultBranchCommitHistory(
        application.getId(), "commit", new Date(), null);

    // when : try to update with an invalid policy eval reference
    commitHistory.setPolicyEvaluationId("bogusPolicyEvaluationId");
    Throwable thrown = catchThrowable(() -> defaultBranchCommitHistoryDAO.update(commitHistory));

    // then : expecting exception
    assertThat(thrown).hasStackTraceContaining(
        "Referential integrity constraint violation: \"source_control_default_branch_commit_history_policy_eval_fk");
  }

  @Test
  public void testDeleteByApplicationId() {
    // given : a set of commit history entries, some linked to a policy evaluation, some not, and some for a different
    //         application
    String policyEvaluationId =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan", "commit1")
            .getId();
    Date commitTime = new Date();
    // app 1
    tempEntity.newSourceControlDefaultBranchCommitHistory(
        application.getId(), "commit1", commitTime, policyEvaluationId);
    tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit2", commitTime, null);
    // app 2
    Application app2 = tempEntity.newApplication("app2", organization.getId());
    String policyEvaluationId2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scan2", "commit7").getId();
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit7", commitTime, policyEvaluationId2);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit8", commitTime, null);

    // when : fetch all entries for first application
    List<SourceControlDefaultBranchCommitHistory> commitHistoryList =
        defaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then : all entries for given app and count is correct
    assertThat(commitHistoryList.size()).isEqualTo(2);
    commitHistoryList.forEach(entry -> assertThat(entry.getApplicationId()).isEqualTo(application.getId()));

    // when : delete entries for first application
    try (TransactionContext tx = defaultBranchCommitHistoryDAO.createTransactionContext()) {
      tx.begin();
      defaultBranchCommitHistoryDAO.deleteByApplicationId(tx, application.getId());
      tx.commit();
    }

    // then : entries were removed for first application
    commitHistoryList = defaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    assertThat(commitHistoryList.isEmpty());

    // and : history for app2 not affected
    commitHistoryList = defaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(app2.getId());
    assertThat(commitHistoryList.size()).isEqualTo(2);
    commitHistoryList.forEach(entry -> assertThat(entry.getApplicationId()).isEqualTo(app2.getId()));
  }

  private Date createTime(int offsetInMinutes) {
    LocalDateTime dateTime = LocalDateTime.now();
    return toDate(offsetInMinutes < 0
        ? dateTime.minusMinutes(Math.abs(offsetInMinutes))
        : dateTime.plusMinutes(offsetInMinutes)
    );
  }

  private Date toDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}

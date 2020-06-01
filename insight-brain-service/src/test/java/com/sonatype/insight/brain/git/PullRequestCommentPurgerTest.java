/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.SourceControlConfig;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestCommentPurgerTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO;

  @Inject
  private SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO;

  @Test
  public void testPurgeObsoleteRecords_purgePullRequestComments() {
    // given:
    PullRequestCommentPurger pullRequestCommentPurger = getTestablePullRequestCommentPurger(null);

    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    // when: no obsolete comments exist
    SourceControlPullRequestComment comment1 = new SourceControlPullRequestComment(
        application.getId(), 1, 101, "contentHash", sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());
    sourceControlPullRequestCommentDAO.insert(comment1);
    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: nothing is purged
    List<SourceControlPullRequestComment> pullRequestComments =
        sourceControlPullRequestCommentDAO.getByApplicationId(application.getId());
    assertThat(pullRequestComments).hasSize(1);

    // when: one obsolete comment exists (and one not obsolete)
    SourceControlPullRequestComment comment2 = new SourceControlPullRequestComment(
        application.getId(), 2, 102, "contentHash", sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());
    Date updateTime = Date.from(ZonedDateTime.now().minusMonths(12).toInstant());
    comment2.setUpdateTime(updateTime);
    sourceControlPullRequestCommentDAO.insert(comment2);

    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: the obsolete comment is purged
    pullRequestComments = sourceControlPullRequestCommentDAO.getByApplicationId(application.getId());
    assertThat(pullRequestComments).hasSize(1);
  }

  @Test
  public void testPurgeObsoleteRecords_purgeDefaultBranchCommitHistory() {
    // given:
    PullRequestCommentPurger pullRequestCommentPurger = getTestablePullRequestCommentPurger(null);
    
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    // when: no obsolete commit history records exist
    SourceControlDefaultBranchCommitHistory record1 = new SourceControlDefaultBranchCommitHistory(
        application.getId(),
        "hash-1",
        new Date(),
        policyEvaluation.getId());
    sourceControlDefaultBranchCommitHistoryDAO.insert(record1);

    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: nothing is purged
    List<SourceControlDefaultBranchCommitHistory> defaultBranchCommitHistory =
        sourceControlDefaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    assertThat(defaultBranchCommitHistory).hasSize(1);

    // when: one obsolete record exists (and one not obsolete)
    SourceControlDefaultBranchCommitHistory record2 = new SourceControlDefaultBranchCommitHistory(
        application.getId(),
        "hash-2",
        new Date(),
        policyEvaluation.getId());
    Date updateTime = Date.from(ZonedDateTime.now().minusMonths(10).toInstant());
    record2.setUpdateTime(updateTime);
    sourceControlDefaultBranchCommitHistoryDAO.insert(record2);

    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: the obsolete record is purged
    defaultBranchCommitHistory =
        sourceControlDefaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    assertThat(defaultBranchCommitHistory).hasSize(1);
  }

  @Test
  public void testPurgeObsoleteRecords_purgeWindowOverride() {
    // given:
    PullRequestCommentPurger pullRequestCommentPurger = getTestablePullRequestCommentPurger(30);
    
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    // when: no obsolete commit history records exist
    SourceControlDefaultBranchCommitHistory record1 = new SourceControlDefaultBranchCommitHistory(
        application.getId(),
        "hash-1",
        new Date(),
        policyEvaluation.getId());
    sourceControlDefaultBranchCommitHistoryDAO.insert(record1);

    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: nothing is purged
    List<SourceControlDefaultBranchCommitHistory> defaultBranchCommitHistory =
        sourceControlDefaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    assertThat(defaultBranchCommitHistory).hasSize(1);

    // when: one 2-month old obsolete record exists (and one not obsolete)
    SourceControlDefaultBranchCommitHistory record2 = new SourceControlDefaultBranchCommitHistory(
        application.getId(),
        "hash-2",
        new Date(),
        policyEvaluation.getId());
    Date updateTime = Date.from(ZonedDateTime.now().minusMonths(2).toInstant());
    record2.setUpdateTime(updateTime);
    sourceControlDefaultBranchCommitHistoryDAO.insert(record2);

    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: the obsolete record is purged
    defaultBranchCommitHistory =
        sourceControlDefaultBranchCommitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    assertThat(defaultBranchCommitHistory).hasSize(1);
  }

  private PullRequestCommentPurger getTestablePullRequestCommentPurger(final Integer purgeWindowsInDays) {
    InsightConfig insightConfig = new InsightConfig();
    if (purgeWindowsInDays != null) {
      SourceControlConfig sourceControlConfig = new SourceControlConfig();
      sourceControlConfig.setPrCommentPurgeWindow(purgeWindowsInDays);
      insightConfig.setSourceControl(sourceControlConfig);
    }
    return new PullRequestCommentPurger(sourceControlPullRequestCommentDAO, sourceControlDefaultBranchCommitHistoryDAO,
        insightConfig);
  }
}

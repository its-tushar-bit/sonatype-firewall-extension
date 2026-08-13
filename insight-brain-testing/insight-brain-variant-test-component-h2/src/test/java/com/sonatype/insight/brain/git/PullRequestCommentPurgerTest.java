/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

@ComponentH2Test
public class PullRequestCommentPurgerTest
    extends AbstractComponentH2Test
{
  @Inject
  private SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO;

  @Inject
  private SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Inject
  private PullRequestCommentPurger pullRequestCommentPurger;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Test
  public void testStart() {
    pullRequestCommentPurger.register();

    verify(mockTaskScheduler).scheduleDailyTask(pullRequestCommentPurger, LocalTime.of(2, 0));
  }

  @Test
  public void testPurgeObsoleteRecords_purgePullRequestComments() {
    // given:
    setSourceControlConfiguration(null, null);

    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    // when: no obsolete comments exist
    SourceControlPullRequestComment comment1 = new SourceControlPullRequestComment(
        application.getId(), 1, 101, 3, "contentHash", sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());
    tempEntity.newSourceControlPullRequestComment(comment1);
    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: nothing is purged
    List<SourceControlPullRequestComment> pullRequestComments =
        sourceControlPullRequestCommentDAO.getByApplicationId(application.getId());
    assertThat(pullRequestComments).hasSize(1);

    // when: one obsolete comment exists (and one not obsolete)
    SourceControlPullRequestComment comment2 = new SourceControlPullRequestComment(
        application.getId(), 2, 102, 3, "contentHash", sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());
    Date updateTime = Date.from(ZonedDateTime.now().minusMonths(12).toInstant());
    comment2.setUpdateTime(updateTime);
    tempEntity.newSourceControlPullRequestComment(comment2);

    pullRequestCommentPurger.purgeObsoleteRecords();

    // then: the obsolete comment is purged
    pullRequestComments = sourceControlPullRequestCommentDAO.getByApplicationId(application.getId());
    assertThat(pullRequestComments).hasSize(1);
  }

  @Test
  public void testPurgeObsoleteRecords_purgeDefaultBranchCommitHistory() {
    // given:
    setSourceControlConfiguration(null, null);

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
    setSourceControlConfiguration(30, 5);

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

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(
        JobBuilder.newJob(PullRequestCommentPurger.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() throws Exception {
    PullRequestCommentPurger spyPullRequestCommentPurger = spy(pullRequestCommentPurger);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spyPullRequestCommentPurger).purgeObsoleteRecords();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyPullRequestCommentPurger.execute(mock(JobExecutionContext.class));
    }

    verify(spyPullRequestCommentPurger).purgeObsoleteRecords();
  }

  private void setSourceControlConfiguration(
      final Integer purgeWindowInDays,
      final Integer shortPurgeWindowInDays)
  {
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setPrCommentPurgeWindow(purgeWindowInDays);
    sourceControlConfiguration.setPrEventPurgeWindow(shortPurgeWindowInDays);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
  }
}

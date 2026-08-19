/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.nexus.scm.api.model.Commit;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.github.dto.GithubPullRequest;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;

@ComponentH2Test
public class GitCommitHistoryServiceTest
    extends AbstractComponentH2Test
{
  private static final String COMMIT_HASH = "COMMIT_HASH";

  private static final String HISTORY_COMMIT_HASH = "HISTORY_COMMIT_HASH";

  private static final String APP_ID = "APP_ID";

  private static final String SCAN_ID = "SCAN_ID";

  private Application application;

  private GitCommitHistoryService gitCommitHistoryService;

  @Inject
  private SourceControlDefaultBranchCommitHistoryDAO commitHistoryDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyEvaluation policyEvaluation;

  @BeforeEach
  public void before() {
    application = tempEntity.newApplicationWithParent(APP_ID);
    gitCommitHistoryService = new GitCommitHistoryService(commitHistoryDAO, policyEvaluationDAO);
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_SingleHistoryExistsWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    setupHistoryItemWithEvaluation(HISTORY_COMMIT_HASH);

    // when
    Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isNotEmpty();
    assertThat(optionalEvaluation.get().getCommitHash()).isEqualTo(HISTORY_COMMIT_HASH);

    // when
    optionalEvaluation =
        gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), false);

    // then
    assertThat(optionalEvaluation).isEmpty();
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_MultipleHistoryExistsWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    final Date date = new Date();
    setupHistoryItemWithEvaluation(HISTORY_COMMIT_HASH, date);
    setupHistoryItemWithEvaluation("OLDER_COMMIT", new Date(date.getTime() - 1000));
    setupHistoryItemWithEvaluation("OLDEST_COMMIT", new Date(date.getTime() - 2000));

    // when
    final Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService
            .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isNotEmpty();
    assertThat(optionalEvaluation.get().getCommitHash()).isEqualTo(HISTORY_COMMIT_HASH);
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_MultipleHistoryExistsWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation(HISTORY_COMMIT_HASH, date);
    setupHistoryItemWithoutEvaluation("OLDER_COMMIT", new Date(date.getTime() - 1000));
    setupHistoryItemWithoutEvaluation("OLDEST_COMMIT", new Date(date.getTime() - 2000));

    // when
    final Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService
            .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isEmpty();
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_SingleHistoryExistsWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    setupHistoryItemWithoutEvaluation(HISTORY_COMMIT_HASH, new Date());

    // when
    final Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService
            .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isEmpty();
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_NoHistoryExists() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");

    // when
    final Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService
            .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isEmpty();
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_OldestHasPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation(HISTORY_COMMIT_HASH, date);
    setupHistoryItemWithoutEvaluation("OLDER_COMMIT", new Date(date.getTime() - 1000));
    setupHistoryItemWithEvaluation("OLDEST_COMMIT", new Date(date.getTime() - 2000));

    // when
    final Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService
            .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isNotEmpty();
    assertThat(optionalEvaluation.get().getCommitHash()).isEqualTo("OLDEST_COMMIT");
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_NewestHasPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    final Date date = new Date();
    setupHistoryItemWithEvaluation(HISTORY_COMMIT_HASH, date);
    setupHistoryItemWithoutEvaluation("OLDER_COMMIT", new Date(date.getTime() - 1000));
    setupHistoryItemWithoutEvaluation("OLDEST_COMMIT", new Date(date.getTime() - 2000));

    // when
    final Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService
            .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isNotEmpty();
    assertThat(optionalEvaluation.get().getCommitHash()).isEqualTo("HISTORY_COMMIT_HASH");
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_MiddleHasPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation(HISTORY_COMMIT_HASH, date);
    setupHistoryItemWithEvaluation("OLDER_COMMIT", new Date(date.getTime() - 1000));
    setupHistoryItemWithoutEvaluation("OLDEST_COMMIT", new Date(date.getTime() - 2000));

    // when
    final Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isNotEmpty();
    assertThat(optionalEvaluation.get().getCommitHash()).isEqualTo("OLDER_COMMIT");
  }

  @Test
  public void testGetTargetPolicyEvaluationForPullRequest_InternalAndExternalPolicyEvals() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    final Date date = new Date();
    setupHistoryItemWithEvaluation("COMMIT_WITH_EXTERNAL_PE", date);
    setupHistoryItemWithoutEvaluation("OLDER_COMMIT", new Date(date.getTime() - 1000));
    setupHistoryItemWithInternalEvaluation("COMMIT_WITH_INTERNAL_PE", new Date(date.getTime() - 2000));

    // when
    Optional<PolicyEvaluation> optionalEvaluation =
        gitCommitHistoryService
            .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), true);

    // then
    assertThat(optionalEvaluation).isNotEmpty();
    assertThat(optionalEvaluation.get().getCommitHash()).isEqualTo("COMMIT_WITH_EXTERNAL_PE");

    // when
    optionalEvaluation = gitCommitHistoryService
        .getLatestPolicyEvaluationForApplicationBaseBranch(application.getId(), false);

    // then
    assertThat(optionalEvaluation).isNotEmpty();
    assertThat(optionalEvaluation.get().getCommitHash()).isEqualTo("COMMIT_WITH_INTERNAL_PE");
  }

  @Test
  public void testUpdateCommitHistory_NoneExistsSingleNewWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date()));

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(1);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
  }

  @Test
  public void testUpdateCommitHistory_NoneExistsMultipleNewWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", date));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 2000)));

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(3);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("OLDER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getCommitHash()).isEqualTo("OLDEST_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getPolicyEvaluationId()).isNull();
  }

  @Test
  public void testUpdateCommitHistory_NoneExistsSingleNewWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date()));
    setupPolicyEvaluation("ANOTHER_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(1);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_NoneExistsMultipleNewWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", date));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 2000)));
    setupPolicyEvaluation("OLDER_COMMIT_HASH");
    setupPolicyEvaluation("OLDEST_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(2);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("OLDER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithoutEvalSingleNewWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 1000));
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 2000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 3000));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 4000));

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(5);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(4).getCommitHash()).isEqualTo("EXISTING_HASH_4");
    MatcherAssert.assertThat(sourceControlDefaultBranchCommitHistories,
        everyItem(hasProperty("policyEvaluationId", Matchers.nullValue())));
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithoutEvalMultipleNewWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 2000));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 3000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 4000));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 5000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 6000));

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(7);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(6).getCommitHash()).isEqualTo("EXISTING_HASH_4");
    MatcherAssert.assertThat(sourceControlDefaultBranchCommitHistories,
        everyItem(hasProperty("policyEvaluationId", Matchers.nullValue())));
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithoutEvalSingleNewWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 1000));
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 2000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 3000));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 4000));
    setupPolicyEvaluation("ANOTHER_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(5);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("EXISTING_HASH_2");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithoutEvalMultipleNewWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 2000));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 3000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 4000));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 5000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 6000));
    setupPolicyEvaluation("OLDER_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(6);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getCommitHash()).isEqualTo("OLDER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithEvalSingleNewWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 1000));
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 2000)));
    setupHistoryItemWithEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 3000));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 4000));

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(5);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getCommitHash()).isEqualTo("EXISTING_HASH_3");
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithEvalMultipleNewWithoutPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 2000));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 3000)));
    setupHistoryItemWithEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 4000));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 5000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 6000));

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(6);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getCommitHash()).isEqualTo("OLDER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(4).getCommitHash()).isEqualTo("EXISTING_HASH_3");
    assertThat(sourceControlDefaultBranchCommitHistories.get(4).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithEvalSingleNewWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 1000));
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 2000)));
    setupHistoryItemWithEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 3000));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 4000));
    setupPolicyEvaluation("ANOTHER_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(5);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("EXISTING_HASH_2");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(2).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithEvalMultipleNewWithPolicyEval() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 2000));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 3000)));
    setupHistoryItemWithEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 4000));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 5000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 6000));
    setupPolicyEvaluation("OLDER_COMMIT_HASH");
    setupPolicyEvaluation("OLDEST_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(6);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getCommitHash()).isEqualTo("OLDER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithEvalSingleNewWithPolicyEvalOlderThanExists() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    setupHistoryItemWithEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 1000));
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 2000)));
    setupHistoryItemWithEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 3000));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 4000));
    setupPolicyEvaluation("ANOTHER_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(4);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("EXISTING_HASH_2");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_SomeExistsWithEvalMultipleNewWithPolicyEvalOlderThanExists() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 2000));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 3000)));
    setupHistoryItemWithEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 4000));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 5000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 6000));
    setupPolicyEvaluation("OLDEST_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(6);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getCommitHash()).isEqualTo("OLDER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(4).getCommitHash()).isEqualTo("EXISTING_HASH_3");
    assertThat(sourceControlDefaultBranchCommitHistories.get(4).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistoryForPolicyEvaluation_LinkEvalToExistingCommitHistory() {
    // given: a set of ordered commits; some with policy evals; some with commit history entries
    Map<String, Commit> orderedCommits =
        createOrderedCommits("policyEvalCommit1", "commit2", "commit3");
    PolicyEvaluation policyEvalForCommit1 = setupPolicyEvaluation("policyEvalCommit1");

    // existing commit history
    createHistory(orderedCommits.get("policyEvalCommit1"), policyEvalForCommit1);
    createHistory(orderedCommits.get("commit2"));
    createHistory(orderedCommits.get("commit3"));

    // when: verify setup
    List<SourceControlDefaultBranchCommitHistory> historyList =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    SourceControlDefaultBranchCommitHistory newestCommitHistoryWithPolicyEval =
        commitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(application.getId());

    // then: as expected
    assertThat(historyList).isNotNull();
    assertThat(historyList).hasSize(3);
    assertHistoryIsForCommit(historyList.get(0), orderedCommits.get("commit3"));
    assertHistoryIsForCommit(historyList.get(1), orderedCommits.get("commit2"));
    assertHistoryIsForCommit(historyList.get(2), orderedCommits.get("policyEvalCommit1"), policyEvalForCommit1);
    assertHistoryIsForCommit(newestCommitHistoryWithPolicyEval, orderedCommits.get("policyEvalCommit1"),
        policyEvalForCommit1);

    // when: add a policy eval for commit 3 and recheck
    PolicyEvaluation policyEvalForCommit3 = setupPolicyEvaluation("commit3");
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(policyEvalForCommit3);
    historyList = commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    newestCommitHistoryWithPolicyEval =
        commitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(application.getId());

    // then: commit 3 is latest with policy eval
    assertThat(historyList).isNotNull();
    assertThat(historyList).hasSize(3);
    assertHistoryIsForCommit(historyList.get(0), orderedCommits.get("commit3"), policyEvalForCommit3);
    assertHistoryIsForCommit(newestCommitHistoryWithPolicyEval, orderedCommits.get("commit3"),
        policyEvalForCommit3);
  }

  @Test
  public void testUpdateCommitHistoryForPolicyEvaluation_UpdateCases() {
    // given: a set of ordered commits; some with policy evals; some with commit history entries
    Map<String, Commit> orderedCommits =
        createOrderedCommits("commit1", "commit2", "commit3", "commit4");
    PolicyEvaluation externalEvalForCommit1 = setupPolicyEvaluation("commit1");
    PolicyEvaluation internalEvalForCommit2 = setupInternalPolicyEvaluation("commit2");

    // existing commit history
    createHistory(orderedCommits.get("commit1"), externalEvalForCommit1);
    createHistory(orderedCommits.get("commit2"), internalEvalForCommit2);
    createHistory(orderedCommits.get("commit3"));
    createHistory(orderedCommits.get("commit4"));

    // when: verify setup
    List<SourceControlDefaultBranchCommitHistory> historyList =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then: as expected
    assertThat(historyList).isNotNull();
    assertThat(historyList).hasSize(4);
    assertHistoryIsForCommit(historyList.get(0), orderedCommits.get("commit4"));
    assertHistoryIsForCommit(historyList.get(1), orderedCommits.get("commit3"));
    assertHistoryIsForCommit(historyList.get(2), orderedCommits.get("commit2"), internalEvalForCommit2);
    assertHistoryIsForCommit(historyList.get(3), orderedCommits.get("commit1"), externalEvalForCommit1);

    // when: new internal policy evaluation is executed for commit 3, and we try to update the commit history
    PolicyEvaluation internalEvalForCommit3 = setupInternalPolicyEvaluation("commit3");
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(internalEvalForCommit3);

    // then: commit 3 is associated with the new policy evaluation ID - internal overwrites null value
    assertUpdateCommitHistoryForPolicyEvaluation(orderedCommits.get("commit3"), 1, internalEvalForCommit3);

    // when: new external policy evaluation is executed for commit 4, and we try to update the commit history
    PolicyEvaluation externalEvalForCommit4 = setupPolicyEvaluation("commit4");
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(externalEvalForCommit4);

    // then: commit 4 is associated with the new policy evaluation ID - external overwrites null value
    assertUpdateCommitHistoryForPolicyEvaluation(orderedCommits.get("commit4"), 0, externalEvalForCommit4);

    // when: new internal policy evaluation is executed for commit 2, and we try to update the commit history
    internalEvalForCommit2 = setupInternalPolicyEvaluation("commit2");
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(internalEvalForCommit2);

    // then: commit 2 is associated with the new policy evaluation ID - internal overwrites internal
    assertUpdateCommitHistoryForPolicyEvaluation(orderedCommits.get("commit2"), 2, internalEvalForCommit2);

    // when: new external policy evaluation is executed for commit 2, and we try to update the commit history
    PolicyEvaluation externalEvalForCommit2 = setupPolicyEvaluation("commit2");
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(externalEvalForCommit2);

    // then: commit 2 is associated with the new policy evaluation ID - external overwrites internal
    assertUpdateCommitHistoryForPolicyEvaluation(orderedCommits.get("commit2"), 2, externalEvalForCommit2);

    // when: new internal policy evaluation is executed for commit 1, and we try to update the commit history
    PolicyEvaluation internalEvalForCommit1 = setupInternalPolicyEvaluation("commit1");
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(internalEvalForCommit1);

    // then: commit 1 is associated with the old policy evaluation ID - internal doesn't overwrite external
    assertUpdateCommitHistoryForPolicyEvaluation(orderedCommits.get("commit1"), 3, externalEvalForCommit1);

    // when: new external policy evaluation is executed for commit 1, and we try to update the commit history
    externalEvalForCommit1 = setupPolicyEvaluation("commit1");
    gitCommitHistoryService.updateCommitHistoryForPolicyEvaluation(externalEvalForCommit1);

    // then: commit 1 is associated with the new policy evaluation ID - external overwrites external
    assertUpdateCommitHistoryForPolicyEvaluation(orderedCommits.get("commit1"), 3, externalEvalForCommit1);
  }

  private void assertUpdateCommitHistoryForPolicyEvaluation(
      Commit commit,
      int historyIndex,
      PolicyEvaluation expectedEvalForCommit)
  {
    List<SourceControlDefaultBranchCommitHistory> historyList =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // assert that the commit is associated with the expected policy evaluation ID
    assertHistoryIsForCommit(historyList.get(historyIndex), commit, expectedEvalForCommit);
  }

  @Test
  public void testUpdateCommitHistory_LinkEvalToExistingCommitHistoryOlderThanExists() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();
    final Date date = new Date();
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_1", date);
    commits.add(setupCommit("ANOTHER_COMMIT_HASH", new Date(date.getTime() - 1000)));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_2", new Date(date.getTime() - 2000));
    commits.add(setupCommit("OLDER_COMMIT_HASH", new Date(date.getTime() - 3000)));
    setupHistoryItemWithEvaluation("EXISTING_HASH_3", new Date(date.getTime() - 4000));
    commits.add(setupCommit("OLDEST_COMMIT_HASH", new Date(date.getTime() - 5000)));
    setupHistoryItemWithoutEvaluation(COMMIT_HASH, new Date(date.getTime() - 6000));
    setupHistoryItemWithoutEvaluation("EXISTING_HASH_4", new Date(date.getTime() - 7000));
    setupPolicyEvaluation("OLDEST_COMMIT_HASH");

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(7);
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getCommitHash()).isEqualTo("EXISTING_HASH_1");
    assertThat(sourceControlDefaultBranchCommitHistories.get(0).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getCommitHash()).isEqualTo("ANOTHER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(1).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getCommitHash()).isEqualTo("OLDER_COMMIT_HASH");
    assertThat(sourceControlDefaultBranchCommitHistories.get(3).getPolicyEvaluationId()).isNull();
    assertThat(sourceControlDefaultBranchCommitHistories.get(4).getCommitHash()).isEqualTo("EXISTING_HASH_3");
    assertThat(sourceControlDefaultBranchCommitHistories.get(4).getPolicyEvaluationId()).isNotNull();
  }

  @Test
  public void testUpdateCommitHistory_scmCommitsHaveNewerPolicyEval() {
    // given: a set of ordered commits; some with policy evals; some with commit history entries
    Map<String, Commit> orderedCommits =
        createOrderedCommits("policyEvalCommit1", "commit2", "commit3", "commit4", "policyEvalCommit5", "commit6");

    PolicyEvaluation policyEvalForCommit1 = setupPolicyEvaluation("policyEvalCommit1");
    PolicyEvaluation policyEvalForCommit5 = setupPolicyEvaluation("policyEvalCommit5");

    // existing commit history
    createHistory(orderedCommits.get("policyEvalCommit1"), policyEvalForCommit1);
    createHistory(orderedCommits.get("commit2"));
    createHistory(orderedCommits.get("commit3"));

    final List<Commit> commitsFromSCM = new ArrayList<>();
    commitsFromSCM.add(orderedCommits.get("commit3")); // overlap with existing
    commitsFromSCM.add(orderedCommits.get("commit4"));
    commitsFromSCM.add(orderedCommits.get("policyEvalCommit5"));
    commitsFromSCM.add(orderedCommits.get("commit6"));

    // when: get current most recent
    SourceControlDefaultBranchCommitHistory newestCommitHistoryWithPolicyEval =
        commitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(application.getId());

    // then: expecting commit 1 to be most recent
    assertHistoryIsForCommit(newestCommitHistoryWithPolicyEval, orderedCommits.get("policyEvalCommit1"),
        policyEvalForCommit1);

    // when: update commit history and fetch current list
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvalForCommit5.getOwnerId(), commitsFromSCM);
    List<SourceControlDefaultBranchCommitHistory> historyList =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());
    newestCommitHistoryWithPolicyEval =
        commitHistoryDAO.getByApplicationIdForLatestCommitWithPolicyEvaluation(application.getId());

    // then: commit history older than commit 5 is gone and commit 5 is our most recent with a policy eval
    assertThat(historyList).isNotEmpty();
    assertThat(historyList).hasSize(6);
    assertHistoryIsForCommit(historyList.get(0), orderedCommits.get("commit6"));
    assertHistoryIsForCommit(historyList.get(1), orderedCommits.get("policyEvalCommit5"), policyEvalForCommit5);
    assertHistoryIsForCommit(newestCommitHistoryWithPolicyEval, orderedCommits.get("policyEvalCommit5"),
        policyEvalForCommit5);
  }

  private void assertHistoryIsForCommit(
      SourceControlDefaultBranchCommitHistory commitHistory,
      Commit commit)
  {
    assertHistoryIsForCommit(commitHistory, commit, null);
  }

  private void assertHistoryIsForCommit(
      SourceControlDefaultBranchCommitHistory commitHistory,
      Commit commit,
      PolicyEvaluation policyEvaluation)
  {
    assertThat(commitHistory.getCommitHash()).isEqualTo(commit.getHash());
    assertThat(commitHistory.getCommitTime()).isEqualTo(commit.getCommittedDate());
    assertThat(commitHistory.getPolicyEvaluationId())
        .isEqualTo(null != policyEvaluation ? policyEvaluation.getId() : null);
  }

  private void createHistory(Commit commit, PolicyEvaluation policyEvaluation) {
    tempEntity.createSourceControlDefaultBranchCommitHistory(
        application.getId(), commit.getHash(), commit.getCommittedDate(), policyEvaluation.getId());
  }

  private void createHistory(Commit commit) {
    tempEntity.createSourceControlDefaultBranchCommitHistory(
        application.getId(), commit.getHash(), commit.getCommittedDate(), null);
  }

  private Map<String, Commit> createOrderedCommits(String... commitHashes) {
    Map<String, Commit> result = new HashMap<>();

    Date date = new Date(System.currentTimeMillis() - 100_000);
    long offset = 0;
    for (String commitHash : commitHashes) {
      Commit commit = new Commit();
      commit.setCommittedDate(new Date(date.getTime() + offset));
      commit.setHash(commitHash);
      result.put(commitHash, commit);
      offset += 1_000;
    }

    return result;
  }

  @Test
  public void testUpdateCommitHistory_LinkEvalNoExistingHistory() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final List<Commit> commits = new ArrayList<>();

    // when
    gitCommitHistoryService.updateCommitHistoryForCommits(policyEvaluation.getOwnerId(), commits);
    final List<SourceControlDefaultBranchCommitHistory> sourceControlDefaultBranchCommitHistories =
        commitHistoryDAO.getByApplicationIdSortedByDateDesc(application.getId());

    // then
    assertThat(sourceControlDefaultBranchCommitHistories).isNotNull();
    assertThat(sourceControlDefaultBranchCommitHistories).hasSize(0);
  }

  @Test
  public void testGetLatestCommitForApplication() {
    // given
    policyEvaluation = setupPolicyEvaluation(COMMIT_HASH);
    final PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setBase("ANOTHER_BRANCH");
    pullRequest.setBaseCommitHash("ANOTHER_COMMIT");
    final Date date = new Date();
    setupHistoryItemWithEvaluation("OLD_COMMIT", new Date(date.getTime() - 1000));
    setupHistoryItemWithEvaluation("HISTORY_COMMIT_HASH", date);
    setupHistoryItemWithoutEvaluation("OLDER_COMMIT", new Date(date.getTime() - 2000));
    setupHistoryItemWithoutEvaluation("OLDEST_COMMIT", new Date(date.getTime() - 3000));

    // when
    final String latestCommitForApplication =
        gitCommitHistoryService.getLatestCommitForApplication(application.getId());

    assertThat(latestCommitForApplication).isNotNull();
    assertThat(latestCommitForApplication).isEqualTo("HISTORY_COMMIT_HASH");
  }

  private void setupHistoryItemWithEvaluation(final String commitHash) {
    setupHistoryItemWithEvaluation(commitHash, new Date());
  }

  private void setupHistoryItemWithEvaluation(final String commitHash, final Date commitTime) {
    final PolicyEvaluation localEvaluation = setupPolicyEvaluation(commitHash);
    tempEntity
        .createSourceControlDefaultBranchCommitHistory(application.getId(), commitHash, commitTime,
            localEvaluation.getId());
  }

  private void setupHistoryItemWithInternalEvaluation(final String commitHash, final Date commitTime) {
    final PolicyEvaluation localEvaluation = setupInternalPolicyEvaluation(commitHash);
    tempEntity
        .createSourceControlDefaultBranchCommitHistory(application.getId(), commitHash, commitTime,
            localEvaluation.getId());
  }

  private void setupHistoryItemWithoutEvaluation(final String commitHash, final Date commitTime) {
    tempEntity
        .createSourceControlDefaultBranchCommitHistory(application.getId(), commitHash, commitTime, null);
  }

  private PolicyEvaluation setupPolicyEvaluation(final String commitHash) {
    return tempEntity
        .newPolicyEvaluation(application.getId(), StageTypes.BUILD.getId(), SCAN_ID, false, false, false, new Date(),
            commitHash);
  }

  private PolicyEvaluation setupInternalPolicyEvaluation(final String commitHash) {
    return tempEntity
        .newPolicyEvaluation(application.getId(), StageTypes.BUILD.getId(), SCAN_ID, false, false, false, new Date(),
            commitHash, ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);
  }

  private Commit setupCommit(final String commitHash, final Date commitTime) {
    final Commit commit = new Commit();
    commit.setCommittedDate(commitTime);
    commit.setHash(commitHash);
    return commit;
  }
}

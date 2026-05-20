/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SourceControlPullRequestDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlPullRequestDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSourceControlPullRequestDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    String repositoryUrl = "testRepositoryUrl";
    int pullRequestId = 1234;
    String headCommitHash = "testHeadCommitHash";
    String baseCommitHash = "testBaseCommitHash";
    String branchName = "testBranchName";
    String baseBranchName = "testBaseBranchName";
    Date createTime = new Date(System.currentTimeMillis() - 1000);
    Date lastCheckTime = new Date(System.currentTimeMillis());
    Date lastDetectedUpdateTime = new Date(System.currentTimeMillis() + 1000);
    SourceControlPullRequest sourceControlPullRequest = tempEntity.newSourceControlPullRequest(repositoryUrl,
        pullRequestId, headCommitHash, baseCommitHash, branchName, baseBranchName,
        createTime, lastCheckTime, lastDetectedUpdateTime);
    assertThat(sourceControlPullRequest.getId()).isNotNull();

    // Read
    String id = sourceControlPullRequest.getId();
    sourceControlPullRequest = dao.getById(id);
    assertThat(sourceControlPullRequest.getId()).isEqualTo(id);
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(sourceControlPullRequest.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(sourceControlPullRequest.getHeadCommitHash()).isEqualTo(headCommitHash);
    assertThat(sourceControlPullRequest.getBranchName()).isEqualTo(branchName);
    assertThat(sourceControlPullRequest.getBaseBranchName()).isEqualTo(baseBranchName);
    assertThat(sourceControlPullRequest.getCreateTime()).isEqualTo(createTime);
    assertThat(sourceControlPullRequest.getLastCheckTime()).isEqualTo(lastCheckTime);
    assertThat(sourceControlPullRequest.getLastDetectedUpdateTime()).isEqualTo(lastDetectedUpdateTime);

    // Update
    String newHeadCommitHash = "testNewHeadCommitHash";
    Date newLastCheckTime = new Date(lastCheckTime.getTime() + 1000);
    Date newLastDetectedUpdateTime = new Date(lastDetectedUpdateTime.getTime() + 1000);
    sourceControlPullRequest.setHeadCommitHash(newHeadCommitHash);
    sourceControlPullRequest.setLastCheckTime(newLastCheckTime);
    sourceControlPullRequest.setLastDetectedUpdateTime(newLastDetectedUpdateTime);
    dao.update(sourceControlPullRequest);
    sourceControlPullRequest = dao.getById(id);
    assertThat(sourceControlPullRequest.getHeadCommitHash()).isEqualTo(newHeadCommitHash);
    assertThat(sourceControlPullRequest.getCreateTime()).isEqualTo(createTime);
    assertThat(sourceControlPullRequest.getLastCheckTime()).isEqualTo(newLastCheckTime);
    assertThat(sourceControlPullRequest.getLastDetectedUpdateTime()).isEqualTo(newLastDetectedUpdateTime);

    // Delete
    dao.delete(sourceControlPullRequest);
    sourceControlPullRequest = dao.getById(id);
    assertThat(sourceControlPullRequest).isNull();
  }

  @Test
  public void testUpdatePersistsTraceFields() {
    SourceControlPullRequest pr = tempEntity.newSourceControlPullRequest();

    pr.setSourceControlEventId("event-42");
    pr.setAuthenticationType("GITHUB_APP");
    pr.setAuthOwnerId("owner-A");
    pr.setGithubAppId("app-1");
    pr.setInstallationId("install-9");
    dao.update(pr);

    SourceControlPullRequest reloaded = dao.getById(pr.getId());
    assertThat(reloaded.getSourceControlEventId()).isEqualTo("event-42");
    assertThat(reloaded.getAuthenticationType()).isEqualTo("GITHUB_APP");
    assertThat(reloaded.getAuthOwnerId()).isEqualTo("owner-A");
    assertThat(reloaded.getGithubAppId()).isEqualTo("app-1");
    assertThat(reloaded.getInstallationId()).isEqualTo("install-9");
  }

  @Test
  public void testGetAll() {
    assertThat(dao.getAll()).hasSize(0);

    tempEntity.newSourceControlPullRequest();
    assertThat(dao.getAll()).hasSize(1);

    tempEntity.newSourceControlPullRequest();
    assertThat(dao.getAll()).hasSize(2);
  }

  @Test
  public void testDeleteByRepositoryUrl() {
    // Given 2 pull requests
    SourceControlPullRequest sourceControlPullRequest = tempEntity.newSourceControlPullRequest("testRepositoryUrl1", 1,
        "testHeadCommitHash1", "testBaseCommitHash1", "testBranchName1", "testBaseBranchName");
    tempEntity.newSourceControlPullRequest("testRepositoryUrl2", 1,
        "testHeadCommitHash2", "testBaseCommitHash2", "testBranchName2", "testBaseBranchName");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryUrl(tx, "testRepositoryUrl2");
      tx.commit();
    }

    List<SourceControlPullRequest> sourceControlPullRequests = dao.getAll();
    assertThat(sourceControlPullRequests).hasSize(1);
    assertThat(sourceControlPullRequests.get(0).getId()).isEqualTo(sourceControlPullRequest.getId());
  }

  @Test
  public void testGetExternalCountByUpdateTimeRange() {
    // Given several pull requests:
    // - 1 PR last updated now
    tempEntity.newSourceControlPullRequest("repoUrl", 1, "sha", "b-sha", "b-1", "bb");

    // - 2 PRs last updated between 1 and 2 weeks ago
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -10);
    Date updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 2, "sha", "b-sha", "b-2", "bb",
        new Date(), new Date(), updateTime);
    tempEntity.newSourceControlPullRequest("repoUrl", 3, "sha", "b-sha", "b-3", "bb",
        new Date(), new Date(), updateTime);

    // - 1 PR last updated more than a months ago
    calendar.add(Calendar.MONTH, -1);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 4, "sha", "b-sha", "b-4", "bb",
        new Date(), new Date(), updateTime);

    // when check how many PRs were updated between 1 and 2 weeks ago
    calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -7);
    Date oneWeekAgo = calendar.getTime();
    calendar.add(Calendar.DATE, -7);
    Date twoWeeksAgo = calendar.getTime();
    int countByUpdateTimeRange = dao.getExternalCountByUpdateTimeRange(twoWeeksAgo, oneWeekAgo);

    // then 2 records are found
    assertThat(countByUpdateTimeRange).isEqualTo(2);

    // when check how many PRs were updated 1 week ago or earlier
    countByUpdateTimeRange = dao.getExternalCountByUpdateTimeRange(null, oneWeekAgo);

    // then 3 records are found
    assertThat(countByUpdateTimeRange).isEqualTo(3);

    // when check how many PRs were updated in the last week
    countByUpdateTimeRange = dao.getExternalCountByUpdateTimeRange(oneWeekAgo, null);

    // then 1 record is found
    assertThat(countByUpdateTimeRange).isEqualTo(1);

    // and expect IllegalArgumentException when called with null arguments
    assertThatThrownBy(() -> dao.getExternalCountByUpdateTimeRange(null, null)).isInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  public void testGetExternalCountByUpdateTimeRange_IgnoresIQPRs() {
    createSourceControlPullRequest(1, PullRequestSource.AUTOMATIC);
    createSourceControlPullRequest(2, PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createSourceControlPullRequest(3, PullRequestSource.MANUAL);
    createSourceControlPullRequest(4, PullRequestSource.MANUAL_INNER_SOURCE);

    assertThat(dao.getExternalCountByUpdateTimeRange(new Date(0), null)).isZero();

    createSourceControlPullRequest(5, PullRequestSource.EXTERNAL);
    createSourceControlPullRequest(6, null);

    assertThat(dao.getExternalCountByUpdateTimeRange(new Date(0), null)).isEqualTo(2);
  }

  @Test
  public void testInsert_InvalidBaseBranchName() {
    assertThatThrownBy(() -> {
      tempEntity.newSourceControlPullRequest("repoUrl", 1, "sha", "b-sha", "testBranchName", "/testBaseBranchName");
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testInsert_InvalidBranchName() {
    assertThatThrownBy(() -> {
      tempEntity.newSourceControlPullRequest("repoUrl", 1, "sha", "b-sha", "/testBranchName", "testBaseBranchName");
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testUpdate_InvalidBaseBranchName() {
    SourceControlPullRequest sourceControlPullRequest =
        tempEntity.newSourceControlPullRequest("repoUrl", 1, "sha", "b-sha", "testBranchName", "testBaseBranchName");
    sourceControlPullRequest.setBaseBranchName("/testBaseBranch");
    assertThatThrownBy(() -> {
      dao.update(sourceControlPullRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testUpdate_InvalidBranchName() {
    SourceControlPullRequest sourceControlPullRequest =
        tempEntity.newSourceControlPullRequest("repoUrl", 1, "sha", "b-sha", "testBranchName", "testBaseBranchName");
    sourceControlPullRequest.setBranchName("/testBranch");
    assertThatThrownBy(() -> {
      dao.update(sourceControlPullRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testGetBySources() {
    // None
    assertThat(dao.getBySources()).isEmpty();
    // All
    assertThat(dao.getBySources(
        PullRequestSource.EXTERNAL,
        PullRequestSource.AUTOMATIC,
        PullRequestSource.AUTOMATIC_INNER_SOURCE,
        PullRequestSource.MANUAL,
        PullRequestSource.MANUAL_INNER_SOURCE)).isEmpty();

    SourceControlPullRequest s1 = createSourceControlPullRequest(1, PullRequestSource.AUTOMATIC);
    SourceControlPullRequest s2 = createSourceControlPullRequest(2, PullRequestSource.AUTOMATIC);
    SourceControlPullRequest s3 = createSourceControlPullRequest(3, PullRequestSource.AUTOMATIC_INNER_SOURCE);
    SourceControlPullRequest s4 = createSourceControlPullRequest(4, PullRequestSource.AUTOMATIC_INNER_SOURCE);
    SourceControlPullRequest s5 = createSourceControlPullRequest(5, PullRequestSource.EXTERNAL);
    SourceControlPullRequest s6 = createSourceControlPullRequest(6, PullRequestSource.EXTERNAL);
    SourceControlPullRequest s7 = createSourceControlPullRequest(7, PullRequestSource.MANUAL);
    SourceControlPullRequest s8 = createSourceControlPullRequest(8, PullRequestSource.MANUAL);
    SourceControlPullRequest s9 = createSourceControlPullRequest(9, PullRequestSource.MANUAL_INNER_SOURCE);
    SourceControlPullRequest s10 = createSourceControlPullRequest(10, PullRequestSource.MANUAL_INNER_SOURCE);

    // None
    assertThat(dao.getBySources()).isEmpty();

    // Single
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s1, s2);
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s3, s4);
    assertThat(dao.getBySources(PullRequestSource.EXTERNAL))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s5, s6);
    assertThat(dao.getBySources(PullRequestSource.MANUAL))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s7, s8);
    assertThat(dao.getBySources(PullRequestSource.MANUAL_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s9, s10);

    // Multiple and same
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC, PullRequestSource.AUTOMATIC))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s1, s2);
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC_INNER_SOURCE, PullRequestSource.AUTOMATIC_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s3, s4);
    assertThat(dao.getBySources(PullRequestSource.EXTERNAL, PullRequestSource.EXTERNAL))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s5, s6);
    assertThat(dao.getBySources(PullRequestSource.MANUAL, PullRequestSource.MANUAL))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s7, s8);
    assertThat(dao.getBySources(PullRequestSource.MANUAL_INNER_SOURCE, PullRequestSource.MANUAL_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s9, s10);

    // Multiple and different
    assertThat(dao.getBySources(PullRequestSource.EXTERNAL, PullRequestSource.AUTOMATIC))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s1, s2, s5, s6);
    assertThat(dao.getBySources(PullRequestSource.EXTERNAL, PullRequestSource.AUTOMATIC_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s3, s4, s5, s6);
    assertThat(dao.getBySources(PullRequestSource.EXTERNAL, PullRequestSource.MANUAL))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s5, s6, s7, s8);
    assertThat(dao.getBySources(PullRequestSource.EXTERNAL, PullRequestSource.MANUAL_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s5, s6, s9, s10);
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC, PullRequestSource.AUTOMATIC_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s1, s2, s3, s4);
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC, PullRequestSource.MANUAL))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s1, s2, s7, s8);
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC, PullRequestSource.MANUAL_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s1, s2, s9, s10);
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC_INNER_SOURCE, PullRequestSource.MANUAL))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s3, s4, s7, s8);
    assertThat(dao.getBySources(PullRequestSource.AUTOMATIC_INNER_SOURCE, PullRequestSource.MANUAL_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s3, s4, s9, s10);
    assertThat(dao.getBySources(PullRequestSource.MANUAL, PullRequestSource.MANUAL_INNER_SOURCE))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(s7, s8, s9, s10);

    // All
    assertThat(dao.getBySources(
        PullRequestSource.EXTERNAL,
        PullRequestSource.AUTOMATIC,
        PullRequestSource.AUTOMATIC_INNER_SOURCE,
        PullRequestSource.MANUAL,
        PullRequestSource.MANUAL_INNER_SOURCE))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10);
  }

  @Test
  public void testGetByStatesAndSources() {
    // Create pull requests with different states and sources
    // OPEN PRs with different sources
    SourceControlPullRequest openPr1 =
        createSourceControlPullRequestWithStateAndSource(1, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    SourceControlPullRequest openPr2 =
        createSourceControlPullRequestWithStateAndSource(2, PullRequestState.OPEN, PullRequestSource.MANUAL);
    SourceControlPullRequest openPr3 =
        createSourceControlPullRequestWithStateAndSource(3, PullRequestState.OPEN, PullRequestSource.EXTERNAL);
    SourceControlPullRequest openPr4 =
        createSourceControlPullRequestWithStateAndSource(4, PullRequestState.OPEN,
            PullRequestSource.AUTOMATIC_INNER_SOURCE);
    SourceControlPullRequest openPr5 =
        createSourceControlPullRequestWithStateAndSource(5, PullRequestState.OPEN,
            PullRequestSource.MANUAL_INNER_SOURCE);

    // Other PRs
    SourceControlPullRequest closedPr =
        createSourceControlPullRequestWithStateAndSource(6, PullRequestState.CLOSED, PullRequestSource.AUTOMATIC);
    SourceControlPullRequest mergedPr =
        createSourceControlPullRequestWithStateAndSource(7, PullRequestState.MERGED, PullRequestSource.AUTOMATIC);
    SourceControlPullRequest lockedPr =
        createSourceControlPullRequestWithStateAndSource(8, PullRequestState.LOCKED, PullRequestSource.MANUAL);

    // Test getByStatesAndSources with OPEN state and all sources
    Set<PullRequestSource> allSources = EnumSet.allOf(PullRequestSource.class);
    List<SourceControlPullRequest> openPullRequests =
        dao.getByStatesAndSources(Set.of(PullRequestState.OPEN), allSources);

    assertThat(openPullRequests).hasSize(5);
    assertThat(openPullRequests)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(openPr1, openPr2, openPr3, openPr4, openPr5);

    // Test getByStatesAndSources with OPEN state and only AUTOMATIC source
    List<SourceControlPullRequest> openAutomaticPullRequests = dao.getByStatesAndSources(
        Set.of(PullRequestState.OPEN),
        Set.of(PullRequestSource.AUTOMATIC));

    assertThat(openAutomaticPullRequests).hasSize(1);
    assertThat(openAutomaticPullRequests)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactly(openPr1);

    // Test getByStatesAndSources with CLOSED state and AUTOMATIC source
    List<SourceControlPullRequest> closedPullRequests = dao.getByStatesAndSources(
        Set.of(PullRequestState.CLOSED),
        Set.of(PullRequestSource.AUTOMATIC));

    assertThat(closedPullRequests).hasSize(1);
    assertThat(closedPullRequests)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactly(closedPr);

    // Test getByStatesAndSources with MERGED state and AUTOMATIC source
    List<SourceControlPullRequest> mergedPullRequests = dao.getByStatesAndSources(
        Set.of(PullRequestState.MERGED),
        Set.of(PullRequestSource.AUTOMATIC));

    assertThat(mergedPullRequests).hasSize(1);
    assertThat(mergedPullRequests)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactly(mergedPr);

    // Test getByStatesAndSources with LOCKED state and MANUAL source
    List<SourceControlPullRequest> lockedPullRequests = dao.getByStatesAndSources(
        Set.of(PullRequestState.LOCKED),
        Set.of(PullRequestSource.MANUAL));

    assertThat(lockedPullRequests).hasSize(1);
    assertThat(lockedPullRequests)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactly(lockedPr);

    // Test getByStatesAndSources with multiple states (OPEN and CLOSED) and AUTOMATIC source
    List<SourceControlPullRequest> openAndClosedPullRequests = dao.getByStatesAndSources(
        Set.of(PullRequestState.OPEN, PullRequestState.CLOSED),
        Set.of(PullRequestSource.AUTOMATIC));
    assertThat(openAndClosedPullRequests).hasSize(2);
    assertThat(openAndClosedPullRequests)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(openPr1, closedPr);
  }

  @Test
  public void testGetByStatesAndSources_NoMatches() {
    // Create pull request with OPEN state and AUTOMATIC source
    createSourceControlPullRequestWithStateAndSource(1, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);

    // Query for CLOSED state should return empty list
    List<SourceControlPullRequest> closedPullRequests = dao.getByStatesAndSources(
        Set.of(PullRequestState.CLOSED),
        Set.of(PullRequestSource.AUTOMATIC));

    assertThat(closedPullRequests).isEmpty();

    // Query for OPEN state but wrong source should return empty list
    List<SourceControlPullRequest> wrongSourcePullRequests = dao.getByStatesAndSources(
        Set.of(PullRequestState.OPEN),
        Set.of(PullRequestSource.MANUAL));

    assertThat(wrongSourcePullRequests).isEmpty();
  }

  @Test
  public void testGetByStatesAndSources_MultipleStatesAndSources() {
    // Create pull requests with different states and sources
    SourceControlPullRequest openAutoPr =
        createSourceControlPullRequestWithStateAndSource(1, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    SourceControlPullRequest openManualPr =
        createSourceControlPullRequestWithStateAndSource(2, PullRequestState.OPEN, PullRequestSource.MANUAL);
    SourceControlPullRequest openExternalPr =
        createSourceControlPullRequestWithStateAndSource(3, PullRequestState.OPEN, PullRequestSource.EXTERNAL);
    SourceControlPullRequest openAutoInnerPr =
        createSourceControlPullRequestWithStateAndSource(4, PullRequestState.OPEN,
            PullRequestSource.AUTOMATIC_INNER_SOURCE);
    SourceControlPullRequest openManualInnerPr =
        createSourceControlPullRequestWithStateAndSource(5, PullRequestState.OPEN,
            PullRequestSource.MANUAL_INNER_SOURCE);
    SourceControlPullRequest closedAutoPr =
        createSourceControlPullRequestWithStateAndSource(6, PullRequestState.CLOSED, PullRequestSource.AUTOMATIC);
    SourceControlPullRequest closedManualPr =
        createSourceControlPullRequestWithStateAndSource(7, PullRequestState.CLOSED, PullRequestSource.MANUAL);
    createSourceControlPullRequestWithStateAndSource(8, PullRequestState.CLOSED, PullRequestSource.EXTERNAL);
    SourceControlPullRequest closedAutoInnerPr =
        createSourceControlPullRequestWithStateAndSource(9, PullRequestState.CLOSED,
            PullRequestSource.AUTOMATIC_INNER_SOURCE);
    SourceControlPullRequest closedManualInnerPr =
        createSourceControlPullRequestWithStateAndSource(10, PullRequestState.CLOSED,
            PullRequestSource.MANUAL_INNER_SOURCE);
    SourceControlPullRequest mergedAutoPr =
        createSourceControlPullRequestWithStateAndSource(11, PullRequestState.MERGED, PullRequestSource.AUTOMATIC);
    createSourceControlPullRequestWithStateAndSource(12, PullRequestState.MERGED, PullRequestSource.MANUAL);
    SourceControlPullRequest mergedExternalPr =
        createSourceControlPullRequestWithStateAndSource(13, PullRequestState.MERGED, PullRequestSource.EXTERNAL);
    SourceControlPullRequest mergedAutoInnerPr =
        createSourceControlPullRequestWithStateAndSource(14, PullRequestState.MERGED,
            PullRequestSource.AUTOMATIC_INNER_SOURCE);
    SourceControlPullRequest mergedManualInnerPr =
        createSourceControlPullRequestWithStateAndSource(15, PullRequestState.MERGED,
            PullRequestSource.MANUAL_INNER_SOURCE);

    // Test multiple states and multiple sources
    List<SourceControlPullRequest> openAndClosedAutoAndManualPRs = dao.getByStatesAndSources(
        Set.of(PullRequestState.OPEN, PullRequestState.CLOSED),
        Set.of(PullRequestSource.AUTOMATIC, PullRequestSource.MANUAL));
    assertThat(openAndClosedAutoAndManualPRs).hasSize(4);
    assertThat(openAndClosedAutoAndManualPRs)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(openAutoPr, openManualPr, closedAutoPr, closedManualPr);

    // Test another combination: OPEN and MERGED states with AUTOMATIC and EXTERNAL sources
    List<SourceControlPullRequest> openAndMergedAutoAndExternalPRs = dao.getByStatesAndSources(
        Set.of(PullRequestState.OPEN, PullRequestState.MERGED),
        Set.of(PullRequestSource.AUTOMATIC, PullRequestSource.EXTERNAL));
    assertThat(openAndMergedAutoAndExternalPRs).hasSize(4);
    assertThat(openAndMergedAutoAndExternalPRs)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(openAutoPr, mergedAutoPr, openExternalPr, mergedExternalPr);

    // Test another combination, all InnerSource
    List<SourceControlPullRequest> openAndClosedAndMergedInnerSourcePRs = dao.getByStatesAndSources(
        Set.of(PullRequestState.OPEN, PullRequestState.CLOSED, PullRequestState.MERGED),
        Set.of(PullRequestSource.AUTOMATIC_INNER_SOURCE, PullRequestSource.MANUAL_INNER_SOURCE));
    assertThat(openAndClosedAndMergedInnerSourcePRs).hasSize(6);
    assertThat(openAndClosedAndMergedInnerSourcePRs)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(openAutoInnerPr, openManualInnerPr, closedAutoInnerPr, closedManualInnerPr,
            mergedAutoInnerPr, mergedManualInnerPr);
  }

  @Test
  public void testGetByApplicationIdAndPullRequestId() {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, "token", SourceControlProvider.GITHUB);

    // Create an application
    String applicationId = tempEntity.newApplicationWithParent().getId();

    // Create a SourceControl entity linked to the application
    String repositoryUrl = "https://github.com/test/repo";
    SourceControl sourceControl = tempEntity.newSourceControl(applicationId, repositoryUrl);
    String normalizedRepositoryUrl = sourceControl.getNormalizedRepositoryUrl();

    // Create pull requests for the repository
    int pullRequestId = 123;
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        normalizedRepositoryUrl, pullRequestId, "headCommit", "baseCommit",
        "feature-branch", "main");

    // Create additional pull requests with different IDs for the same repo
    tempEntity.newSourceControlPullRequest(
        normalizedRepositoryUrl, 456, "headCommit2", "baseCommit",
        "another-branch", "main");

    // Create pull requests for a different repository
    String otherRepositoryUrl = "https://github.com/other/repo";
    String otherApplicationId = tempEntity.newApplicationWithParent().getId();
    SourceControl otherSourceControl = tempEntity.newSourceControl(otherApplicationId, otherRepositoryUrl);
    tempEntity.newSourceControlPullRequest(
        otherSourceControl.getNormalizedRepositoryUrl(), pullRequestId, "headCommit3", "baseCommit",
        "different-repo-branch", "main");

    // Test getting by application ID and pull request ID
    SourceControlPullRequest result = dao.getByApplicationIdAndPullRequestId(applicationId, pullRequestId);

    // Verify the correct pull request is returned
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(pullRequest.getId());
    assertThat(result.getRepositoryUrl()).isEqualTo(normalizedRepositoryUrl);
    assertThat(result.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(result.getHeadCommitHash()).isEqualTo("headCommit");
    assertThat(result.getBranchName()).isEqualTo("feature-branch");
  }

  @Test
  public void testGetByApplicationIdAndPullRequestId_NotFound() {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, "token", SourceControlProvider.GITHUB);

    // Create an application and repository
    String applicationId = tempEntity.newApplicationWithParent().getId();
    String repositoryUrl = "https://github.com/test/repo";
    SourceControl sourceControl = tempEntity.newSourceControl(applicationId, repositoryUrl);

    // Create a pull request
    int existingPrId = 123;
    tempEntity.newSourceControlPullRequest(
        sourceControl.getNormalizedRepositoryUrl(), existingPrId, "headCommit", "baseCommit",
        "feature-branch", "main");

    // Test with non-existent pull request ID
    SourceControlPullRequest nonExistentResult = dao.getByApplicationIdAndPullRequestId(applicationId, 999);
    assertThat(nonExistentResult).isNull();

    // Test with non-existent application ID
    SourceControlPullRequest nonExistentAppResult = dao.getByApplicationIdAndPullRequestId(
        "non-existent-app-id", existingPrId);
    assertThat(nonExistentAppResult).isNull();
  }

  @Test
  public void testGetInternalCreatedSince() {
    // Create time stamps for testing
    Date now = new Date();
    Date tenDaysAgo = dateOffset(now, -10, ChronoUnit.DAYS);
    Date twentyDaysAgo = dateOffset(now, -20, ChronoUnit.DAYS);
    Date thirtyDaysAgo = dateOffset(now, -30, ChronoUnit.DAYS);

    // Create PRs with different sources and create times
    // 1. PRs created by IQ (MANUAL and AUTOMATIC) at different times
    SourceControlPullRequest recentManualPR = createSourceControlPullRequestWithSourceAndCreateTime(
        1, PullRequestSource.MANUAL, now);
    SourceControlPullRequest olderManualPR = createSourceControlPullRequestWithSourceAndCreateTime(
        2, PullRequestSource.MANUAL, twentyDaysAgo);
    SourceControlPullRequest recentAutoPR = createSourceControlPullRequestWithSourceAndCreateTime(
        3, PullRequestSource.AUTOMATIC, tenDaysAgo);
    SourceControlPullRequest olderAutoPR = createSourceControlPullRequestWithSourceAndCreateTime(
        4, PullRequestSource.AUTOMATIC, thirtyDaysAgo);

    // 2. PRs created externally at different times (should not be included in results)
    createSourceControlPullRequestWithSourceAndCreateTime(5, PullRequestSource.EXTERNAL, now);
    createSourceControlPullRequestWithSourceAndCreateTime(6, PullRequestSource.EXTERNAL, twentyDaysAgo);
    createSourceControlPullRequestWithSourceAndCreateTime(7, null, now); // Legacy external PR

    // Test 1: Get all internal PRs (null start date)
    List<SourceControlPullRequest> allInternalPRs = dao.getInternalCreatedSince(null);
    assertThat(allInternalPRs).hasSize(4);
    assertThat(allInternalPRs)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(recentManualPR, olderManualPR, recentAutoPR, olderAutoPR);

    // Test 2: Get internal PRs created in the last 15 days
    Date fifteenDaysAgo = dateOffset(now, -15, ChronoUnit.DAYS);
    List<SourceControlPullRequest> recentInternalPRs = dao.getInternalCreatedSince(fifteenDaysAgo);
    assertThat(recentInternalPRs).hasSize(2);
    assertThat(recentInternalPRs)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(recentManualPR, recentAutoPR);

    // Test 3: Get internal PRs created in the last 25 days (should include those from 20 days ago)
    Date twentyFiveDaysAgo = dateOffset(now, -25, ChronoUnit.DAYS);
    List<SourceControlPullRequest> twentyFiveDayPRs = dao.getInternalCreatedSince(twentyFiveDaysAgo);
    assertThat(twentyFiveDayPRs).hasSize(3);
    assertThat(twentyFiveDayPRs)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(recentManualPR, olderManualPR, recentAutoPR);

    // Test 4: Get internal PRs created in the last 5 days (should only include the most recent Manual PR)
    Date fiveDaysAgo = dateOffset(now, -5, ChronoUnit.DAYS);
    List<SourceControlPullRequest> veryRecentPRs = dao.getInternalCreatedSince(fiveDaysAgo);
    assertThat(veryRecentPRs).hasSize(1);
    assertThat(veryRecentPRs)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactly(recentManualPR);

    // Test 5: Get internal PRs created in the future (should return empty list)
    Date tomorrow = dateOffset(now, 1, ChronoUnit.DAYS);
    List<SourceControlPullRequest> futurePRs = dao.getInternalCreatedSince(tomorrow);
    assertThat(futurePRs).hasSize(0);
  }

  /**
   * Creates a date relative to the reference date by adding or subtracting the specified amount of time.
   *
   * @param referenceDate The starting date
   * @param amount The amount to add (negative values subtract time)
   * @param unit The temporal unit to use for the adjustment (e.g., ChronoUnit.DAYS, ChronoUnit.HOURS)
   * @return A new Date adjusted by the specified amount
   */
  private Date dateOffset(Date referenceDate, int amount, TemporalUnit unit) {
    return Date.from(referenceDate.toInstant().plus(amount, unit));
  }

  private SourceControlPullRequest createSourceControlPullRequestWithSourceAndCreateTime(
      final int pullRequestId,
      final PullRequestSource source,
      final Date createTime)
  {
    return tempEntity.newSourceControlPullRequest(
        "repoUrl",
        pullRequestId,
        "sha",
        "b-sha",
        "testBranchName",
        "testBaseBranchName",
        createTime,
        new Date(),
        new Date(),
        source);
  }

  private SourceControlPullRequest createSourceControlPullRequest(
      final int pullRequestId,
      final PullRequestSource source)
  {
    return tempEntity.newSourceControlPullRequest(
        "repoUrl",
        pullRequestId,
        "sha",
        "b-sha",
        "testBranchName",
        "testBaseBranchName",
        source);
  }

  private SourceControlPullRequest createSourceControlPullRequestWithStateAndSource(
      final int pullRequestId,
      final PullRequestState state,
      final PullRequestSource source)
  {
    return tempEntity.newSourceControlPullRequest(
        "repoUrl",
        pullRequestId,
        "sha",
        "b-sha",
        "testBranchName",
        "testBaseBranchName",
        new Date(),
        new Date(),
        new Date(),
        state,
        source);
  }
}

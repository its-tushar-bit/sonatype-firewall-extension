/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SourceControlPullRequestDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlPullRequestDAO dao = new SourceControlPullRequestDAO();

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
  public void testGetCountByUpdateTimeRange() {
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
    int countByUpdateTimeRange = dao.getCountByUpdateTimeRange(twoWeeksAgo, oneWeekAgo);

    // then 2 records are found
    assertThat(countByUpdateTimeRange).isEqualTo(2);

    // when check how many PRs were updated 1 week ago or earlier
    countByUpdateTimeRange = dao.getCountByUpdateTimeRange(null, oneWeekAgo);

    // then 3 records are found
    assertThat(countByUpdateTimeRange).isEqualTo(3);

    // when check how many PRs were updated in the last week
    countByUpdateTimeRange = dao.getCountByUpdateTimeRange(oneWeekAgo, null);

    // then 1 record is found
    assertThat(countByUpdateTimeRange).isEqualTo(1);

    // and expect IllegalArgumentException when called with null arguments
    assertThatThrownBy(() -> dao.getCountByUpdateTimeRange(null, null)).isInstanceOf(IllegalArgumentException.class);
  }
}

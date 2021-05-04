/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlPullRequestDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlPullRequestDAO dao = new SourceControlPullRequestDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    String repositoryUrl = "testRepositoryUrl";
    String repositoryUrlLowercase = "testrepositoryurl";
    int pullRequestId = 1234;
    String headCommitHash = "testHeadCommitHash";
    String branchName = "testBranchName";
    Date createTime = new Date(System.currentTimeMillis() - 1000);
    Date lastCheckTime = new Date(System.currentTimeMillis());
    Date lastDetectedUpdateTime = new Date(System.currentTimeMillis() + 1000);
    SourceControlPullRequest sourceControlPullRequest = tempEntity.newSourceControlPullRequest(repositoryUrl,
        pullRequestId, headCommitHash, branchName, createTime, lastCheckTime, lastDetectedUpdateTime);
    assertThat(sourceControlPullRequest.getId()).isNotNull();

    // Read
    String id = sourceControlPullRequest.getId();
    sourceControlPullRequest = dao.getById(id);
    assertThat(sourceControlPullRequest.getId()).isEqualTo(id);
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo(repositoryUrlLowercase);
    assertThat(sourceControlPullRequest.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(sourceControlPullRequest.getHeadCommitHash()).isEqualTo(headCommitHash);
    assertThat(sourceControlPullRequest.getBranchName()).isEqualTo(branchName);
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
    // Given 3 pull requests, of which two have the same repository URL (case insensitive)
    SourceControlPullRequest sourceControlPullRequest = tempEntity.newSourceControlPullRequest("testRepositoryUrl1", 1,
        "testHeadCommitHash1", "testBranchName1", new Date(), new Date(), new Date());
    tempEntity.newSourceControlPullRequest("testRepositoryUrl2", 1, "testHeadCommitHash2", "testBranchName2",
        new Date(), new Date(), new Date());
    tempEntity.newSourceControlPullRequest("TESTRepositoryUrl2", 2, "testHeadCommitHash3", "testBranchName3",
        new Date(), new Date(), new Date());

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryUrl(tx, "testRepositoryUrl2");
      tx.commit();
    }

    List<SourceControlPullRequest> sourceControlPullRequests = dao.getAll();
    assertThat(sourceControlPullRequests).hasSize(1);
    assertThat(sourceControlPullRequests.get(0).getId()).isEqualTo(sourceControlPullRequest.getId());
  }
}

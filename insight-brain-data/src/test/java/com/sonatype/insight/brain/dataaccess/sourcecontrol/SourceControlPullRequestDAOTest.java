/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;

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
    assertThat(sourceControlPullRequest.getRepositoryUrlLowercase()).isEqualTo(repositoryUrlLowercase);
    assertThat(sourceControlPullRequest.getRepositoryUrlLowercase())
        .isEqualTo(repositoryUrl.toLowerCase(Locale.ENGLISH));
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
}

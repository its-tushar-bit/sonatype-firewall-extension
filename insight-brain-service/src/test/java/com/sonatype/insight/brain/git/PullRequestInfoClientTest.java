/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.Commit;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.gitlab.dto.GitlabMergeRequestResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class PullRequestInfoClientTest
{
  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private PullRequestInfoProvider mockPullRequestInfoProvider;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testGetCommitInfoFromScm_ok() throws IOException {
    // given: a PullRequestInfoProvider setup to return the specified commit info
    doReturn(mockPullRequestInfoProvider).when(mockGitClientFactory).createPullRequestInfoClient(any());

    CommitInformation commitInfo = new CommitInformation();
    commitInfo.addCommit(new Commit()); // hash, date
    commitInfo.addPullRequest(new GitlabMergeRequestResponse());

    doReturn(commitInfo).when(mockPullRequestInfoProvider)
        .getCommitInformationForCommit(any(), any(), any(), any(), anyInt(), anyInt());

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("http://gitlab.com/test/project", null, "user", "token",
        SourceControlProvider.GITLAB, "master", true, true, true, true, true, true, false, null);

    // when: get pull request info
    PullRequestInfoClient pullRequestInfoClient = new PullRequestInfoClient(mockGitClientFactory);
    CommitInformation fetchedCommitInfo = pullRequestInfoClient.getCommitInfoFromScm(gitRepositoryInfo, "commit123");

    // then: the expected commit info is returned
    assertThat(fetchedCommitInfo).isEqualTo(commitInfo);
    assertThat(fetchedCommitInfo.getCommits().size()).isEqualTo(1);
    assertThat(fetchedCommitInfo.getPullRequests().size()).isEqualTo(1);
  }

  @Test
  public void testGetCommitInfoFromScm_providerThrowsException() throws IOException {
    // given: a PullRequestInfoProvider setup to throw an IO exception
    doReturn(mockPullRequestInfoProvider).when(mockGitClientFactory).createPullRequestInfoClient(any());

    doThrow(new IOException("Test generated")).when(mockPullRequestInfoProvider)
        .getCommitInformationForCommit(any(), any(), any(), any(), anyInt(), anyInt());

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("http://gitlab.com/test/project", null, "user", "token",
        SourceControlProvider.GITLAB, "master", true, true, true, true, true, true, false, null);

    PullRequestInfoClient pullRequestInfoClient = new PullRequestInfoClient(mockGitClientFactory);

    // expect: when we try to retrieve commit info
    assertThatExceptionOfType(SourceControlException.class)
        .isThrownBy(() -> pullRequestInfoClient.getCommitInfoFromScm(gitRepositoryInfo, "commit123"))
        .withMessage(
            "Failed to obtain CommitInfo from SCM for project http://gitlab.com/test/project/, " +
                "commit commit123 - reason: Test generated");
  }
}

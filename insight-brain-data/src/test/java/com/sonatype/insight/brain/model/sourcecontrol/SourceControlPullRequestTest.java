/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlPullRequestTest
{
  @Test
  public void testConstructor_SetsRepositoryUrlLowercase() {
    SourceControlPullRequest sourceControlPullRequest =
        new SourceControlPullRequest(" testRepositoryUrl ", 1, "testCommitHash", "baseCommitHash", "testBranchName",
            "baseBranchName", new Date(), new Date(), new Date(), null, null);

    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo("testRepositoryUrl");
  }

  @Test
  public void testSetRepositoryUrl() {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest();
    sourceControlPullRequest.setRepositoryUrl(" testRepositoryUrl ");

    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo("testRepositoryUrl");
  }

  @Test
  public void testSetRepositoryUrl_Null() {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest();
    sourceControlPullRequest.setRepositoryUrl("testRepositoryUrl");

    sourceControlPullRequest.setRepositoryUrl(null);
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isNull();
  }

  @Test
  public void testSetRepositoryUrl_WhiteSpace() {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest();
    sourceControlPullRequest.setRepositoryUrl("testRepositoryUrl");

    sourceControlPullRequest.setRepositoryUrl("     ");
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isNull();
  }
}

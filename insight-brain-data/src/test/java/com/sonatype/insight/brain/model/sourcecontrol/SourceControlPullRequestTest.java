/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlPullRequestTest
{
  @Test
  public void testConstructor_SetsRepositoryUrlLowercase() {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest(" testRepositoryUrl ", 1,
        "testCommitHash", "testVranchName", new Date(), new Date(), new Date());

    assertThat(sourceControlPullRequest.getRepositoryUrlLowercase()).isEqualTo("testrepositoryurl");
  }

  @Test
  public void testSetRepositoryUrlLowercase() {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest();
    sourceControlPullRequest.setRepositoryUrlLowercase(" testRepositoryUrl ");

    assertThat(sourceControlPullRequest.getRepositoryUrlLowercase()).isEqualTo("testrepositoryurl");
  }

  @Test
  public void testSetRepositoryUrl_Null() {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest();
    sourceControlPullRequest.setRepositoryUrlLowercase("testRepositoryUrl");

    sourceControlPullRequest.setRepositoryUrlLowercase(null);
    assertThat(sourceControlPullRequest.getRepositoryUrlLowercase()).isNull();
  }

  @Test
  public void testSetRepositoryUrl_WhiteSpace() {
    SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest();
    sourceControlPullRequest.setRepositoryUrlLowercase("testRepositoryUrl");

    sourceControlPullRequest.setRepositoryUrlLowercase("     ");
    assertThat(sourceControlPullRequest.getRepositoryUrlLowercase()).isNull();
  }
}

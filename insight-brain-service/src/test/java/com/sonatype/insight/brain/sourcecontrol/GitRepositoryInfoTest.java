/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import org.junit.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class GitRepositoryInfoTest
{
  @Test
  public void testEquals() {
    GitRepositoryInfo gitRepositoryInfo1 = new GitRepositoryInfo("https://github.com/org/proj", null, "user",
        "token", GITHUB, "main", true, true, true, true, true, true, false, null);
    GitRepositoryInfo gitRepositoryInfo2 = new GitRepositoryInfo("https://github.com/org/proj", null, "user",
        "token", GITHUB, "main", true, true, true, true, true, true, false, null);
    assertThat(gitRepositoryInfo1).isEqualTo(gitRepositoryInfo2);
    assertThat(gitRepositoryInfo1).hasSameHashCodeAs(gitRepositoryInfo2);
    assertThat(gitRepositoryInfo2).isEqualTo(gitRepositoryInfo1);
  }

  @Test
  public void testEquals_DifferentUrl() {
    GitRepositoryInfo gitRepositoryInfo1 = new GitRepositoryInfo("https://github.com/org/proj1", null, "user",
        "token", GITHUB, "main", true, true, true, true, true, true, false, null);
    GitRepositoryInfo gitRepositoryInfo2 = new GitRepositoryInfo("https://github.com/org/proj2", null, "user",
        "token", GITHUB, "main", true, true, true, true, true, true, false, null);
    assertThat(gitRepositoryInfo1).isNotEqualTo(gitRepositoryInfo2);
    assertThat(gitRepositoryInfo1).doesNotHaveSameHashCodeAs(gitRepositoryInfo2);
  }

  @Test
  public void testEquals_SameUrl_DifferentBranch() {
    // test one field other than the URL which will cause the equals check to fail but hashcode to match
    GitRepositoryInfo gitRepositoryInfo1 = new GitRepositoryInfo("https://github.com/org/proj", null, "user",
        "token", GITHUB, "main", true, true, true, true, true, true, false, null);
    GitRepositoryInfo gitRepositoryInfo2 = new GitRepositoryInfo("https://github.com/org/proj", null, "user",
        "token", GITHUB, "develop", true, true, true, true, true, true, false, null);
    assertThat(gitRepositoryInfo1).isNotEqualTo(gitRepositoryInfo2);
    assertThat(gitRepositoryInfo1).hasSameHashCodeAs(gitRepositoryInfo2);
  }

  @Test
  public void testEquals_Null() {
    GitRepositoryInfo gitRepositoryInfo1 =
        new GitRepositoryInfo("https://github.com/org/proj", null, "user", "token", GITHUB, "main", true, true, true,
            true, true, true, false, null);
    assertThat(gitRepositoryInfo1).isNotEqualTo(null);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test.gitlab;

import com.sonatype.insight.brain.common.test.SlowTest;

import org.gitlab4j.api.models.Project;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
// Ensure that the tests run in a specific order to test initial state, persistent state, and cleanup
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GitLabServerRuleTest
{
  public static GitLabServerRule gitLabServerRule;

  @AfterClass
  public static void afterClass() {
    GitLabServer gitLabServer = gitLabServerRule.getGitLabServer();
    if (gitLabServer != null) {
      gitLabServer.close();
    }
  }

  @Test
  public void testGitLabServerRule_A_Before() throws Exception {
    Network network = Network.newNetwork();
    gitLabServerRule = new GitLabServerRule(network, "some-repo");
    assertThat(gitLabServerRule.getGitLabServer()).isNull();

    gitLabServerRule.before();

    GitLabServer gitLabServer = gitLabServerRule.getGitLabServer();
    assertThat(gitLabServer).isNotNull();
    assertThat(gitLabServer.getImage()).isEqualTo(
        GitLabServer.DEFAULT_IMAGE_NAME + ":" + GitLabServer.DEFAULT_IMAGE_VERSION);
    assertThat(gitLabServer.getContainer().getNetwork()).isEqualTo(network);
    assertThat(gitLabServer.getGitLabApi().getProjectApi().getProjects())
        .extracting(Project::getName)
        .containsExactly("some-repo");
  }

  @Test
  public void testGitLabServerRule_B_After() throws Exception {
    GitLabServer gitLabServer = gitLabServerRule.getGitLabServer();
    assertThat(gitLabServer.getGitLabApi().getProjectApi().getProjects())
        .extracting(Project::getName)
        .containsExactly("some-repo");
    GenericContainer<?> container = gitLabServer.getContainer();
    assertThat(container).isNotNull();
    assertThat(container.isRunning()).isTrue();

    gitLabServerRule.after();

    assertThat(container.isRunning()).isFalse();
    assertThat(gitLabServerRule.getGitLabServer()).isNull();
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test.gitlab;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.Network;

/**
 * A Rule for creating a GitLab server (see also {@link GitLabServer}).
 */
public class GitLabServerRule
    extends ExternalResource
{
  private Network network;

  private String repositoryName;

  private GitLabServer gitLabServer;

  public GitLabServerRule() {
    this(null, null);
  }

  public GitLabServerRule(final String repositoryName) {
    this(null, repositoryName);
  }

  public GitLabServerRule(final Network network) {
    this(network, null);
  }

  public GitLabServerRule(final Network network, final String repositoryName) {
    this.network = network;
    this.repositoryName = repositoryName;
  }

  public GitLabServer getGitLabServer() {
    return gitLabServer;
  }

  @Override
  public void before() {
    gitLabServer =
        new GitLabServer(network, GitLabServer.DEFAULT_IMAGE_NAME, GitLabServer.DEFAULT_IMAGE_VERSION, repositoryName);
  }

  @Override
  public void after() {
    if (gitLabServer != null) {
      gitLabServer.close();
      gitLabServer = null;
    }
    network = null;
    repositoryName = null;
  }
}

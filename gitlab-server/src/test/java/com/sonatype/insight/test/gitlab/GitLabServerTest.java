/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test.gitlab;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sonatype.insight.brain.common.test.SlowTest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.models.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class GitLabServerTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testGitLabServer() throws Exception {
    GenericContainer<?> container;
    try (GitLabServer gitLabServer = new GitLabServer("repo")) {
      container = gitLabServer.getContainer();
      assertThat(container).isNotNull();
      assertThat(container.isRunning()).isTrue();
      assertThat(gitLabServer.getHostname()).isNotNull().isNotEqualToIgnoringCase("localhost");
      assertThat(gitLabServer.getHttpPort()).isNotNull();
      assertThat(gitLabServer.getProject().getName()).isEqualTo("repo");
      assertThat(gitLabServer.getAdminToken()).isNotNull();

      assertThat(gitLabServer.getGitLabApi().getProjectApi().getProjects())
          .extracting(Project::getName)
          .containsExactly(gitLabServer.getProject().getName());

      CredentialsProvider credentialsProvider =
          new UsernamePasswordCredentialsProvider(gitLabServer.getAdminUsername(), gitLabServer.getAdminPassword());

      // Push some file to GitLab
      File repoDir = temporaryFolder.newFolder();
      Path file = Paths.get(repoDir.getAbsolutePath(), "file");
      Files.write(file, "some file content".getBytes());
      try (Git git = Git.init().setDirectory(repoDir).call()) {
        git.add().addFilepattern(file.getFileName().toString()).call();
        git.commit().setMessage("Added file").call();
        git.remoteAdd()
            .setName("origin")
            .setUri(new URIish(gitLabServer.getProject().getHttpUrlToRepo()))
            .call();
        git.push()
            .setRemote("origin")
            .setCredentialsProvider(credentialsProvider)
            .add("main")
            .call();
      }

      // Checkout the repo and check the file is there
      File checkoutDir = temporaryFolder.newFolder();
      try (Git git = Git.cloneRepository()
          .setURI(gitLabServer.getProject().getHttpUrlToRepo())
          .setDirectory(checkoutDir)
          .setCredentialsProvider(credentialsProvider)
          .call())
      {
        Path checkoutFile = Paths.get(checkoutDir.getAbsolutePath(), file.getFileName().toString());
        assertThat(Files.exists(checkoutFile)).isTrue();
        assertThat(Files.readString(checkoutFile)).isEqualTo("some file content");
      }
    }
    assertThat(container.isRunning()).isFalse();
  }
}

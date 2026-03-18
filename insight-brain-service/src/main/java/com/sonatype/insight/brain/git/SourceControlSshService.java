/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Service to ensure that a source control entry has a SSH URL if it is supposed to. The SSH URL 'tags along' with the
 * primary repository URL. Since an SSH URL cannot be computed from a repository URL we retrieve it from the SCM via the
 * API. If a repository URL is changed then the SSH URL is removed and this class can be used to re-populate it. This
 * should occur before any SCM code that may invoke a clone/fetch/push.
 *
 * @since 1.125
 */
@Named
@Singleton
public class SourceControlSshService
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlSshService.class);

  private final SourceControlDAO sourceControlDAO;

  private final GitClientFactory gitClientFactory;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public SourceControlSshService(
      final SourceControlDAO sourceControlDAO,
      final GitClientFactory gitClientFactory,
      final SourceControlUtils sourceControlUtils)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.gitClientFactory = gitClientFactory;
    this.sourceControlUtils = sourceControlUtils;
  }

  public void verifySshUrlAndUpdateIfNeeded(final String applicationId) {
    // check if the source control entry has ssh enabled, yet is missing the SSH URL
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    if (gitRepositoryInfo != null
        && Boolean.TRUE.equals(gitRepositoryInfo.getSshEnabled())
        && isEmpty(gitRepositoryInfo.getSshRepositoryUrl()))
    {
      GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
      try {
        String sshUrl = gitApiClient.getSshUrl();
        if (isEmpty(sshUrl)) {
          log.warn(format("No SSH URL was found for repository '%s'", gitRepositoryInfo.getRepositoryUrl()));
        }
        else {
          log.info(format("Setting SSH URL '%s' for repository '%s'", sshUrl, gitRepositoryInfo.getRepositoryUrl()));
          SourceControl sourceControl = sourceControlDAO.getByOwnerId(applicationId);
          sourceControl.setRepositorySshUrl(sshUrl);
          sourceControlDAO.update(sourceControl);
        }
      }
      catch (IOException e) {
        log.error(format("Error determining SSH URL for repository '%s'", gitRepositoryInfo.getRepositoryUrl()), e);
      }
    }
  }
}

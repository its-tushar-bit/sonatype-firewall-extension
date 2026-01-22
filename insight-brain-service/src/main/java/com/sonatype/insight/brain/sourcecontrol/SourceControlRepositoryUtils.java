/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.git.utils.api.GitApi;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods for working with repository ssh and http urls.
 */
@Named
public class SourceControlRepositoryUtils
{
  private static final Pattern SSH_URL_PATTERN = Pattern.compile("git@(.+):(.+)", Pattern.CASE_INSENSITIVE);

  private final SourceControlDAO sourceControlDAO;

  private final GitApiFactory gitApiFactory;

  @Inject
  public SourceControlRepositoryUtils(SourceControlDAO sourceControlDAO, GitApiFactory gitApiFactory) {
    this.sourceControlDAO = sourceControlDAO;
    this.gitApiFactory = gitApiFactory;
  }

  /**
   * Supports only cloud providers - not supposed to work for on-premises hosts.
   */
  public String getRepositoryHttpUrlFromSshUrl(String sshUrl) {
    if (sshUrl == null) {
      return null;
    }

    Matcher matcher = SSH_URL_PATTERN.matcher(sshUrl);
    if (!matcher.matches()) {
      return null;
    }

    // git@[sshHost]:[sshTarget]
    String sshHost = matcher.group(1);
    String sshTarget = matcher.group(2);

    String derivedUrl;
    if (StringUtils.containsAny(sshHost, "github.com", "gitlab.com", "bitbucket.org")) {
      derivedUrl = sshHost.concat("/").concat(sshTarget);
    }
    else if (sshHost.contains("azure.com")) {
      // remove ssh. from host (ssh.dev.azure.com)
      sshHost = sshHost.substring("ssh.".length());
      // remove api version from target (v3/username/project/repository)
      sshTarget = sshTarget.substring(sshTarget.indexOf("/") + 1);

      String organization = sshTarget.substring(0, sshTarget.lastIndexOf("/"));
      String repository = sshTarget.substring(sshTarget.lastIndexOf("/"));
      derivedUrl = sshHost.concat("/").concat(organization).concat("/_git").concat(repository);
    }
    else {
      return null;
    }

    derivedUrl = "https://".concat(derivedUrl);
    return derivedUrl;
  }

  public boolean isRepositoryReachable(Application application, String repositoryUrl) {
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(application.getId());

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.repositoryUrl = repositoryUrl;
    gitRepositoryInfo.username = sourceControl.getUsername();
    gitRepositoryInfo.token = sourceControl.getToken();

    try {
      GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);
      if (!gitApi.getHeadCommitsForAllBranches(gitRepositoryInfo.repositoryUrl).isEmpty()) {
        return true;
      }
    }
    catch (Exception ignored) {
      // not reachable
    }
    return false;
  }
}

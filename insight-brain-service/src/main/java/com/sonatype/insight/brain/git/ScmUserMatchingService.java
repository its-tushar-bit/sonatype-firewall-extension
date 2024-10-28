/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ScmUserMatchingService
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlScanService.class);

  private final GitClientFactory gitClientFactory;

  private final MembershipMappingService membershipMappingService;

  private final RoleDAO roleDAO;

  private final UserDirectory userDirectory;

  private final ApplicationDAO applicationDAO;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  ScmUserMatchingService(
      GitClientFactory gitClientFactory,
      MembershipMappingService membershipMappingService,
      RoleDAO roleDAO,
      UserDirectory userDirectory,
      ApplicationDAO applicationDAO,
      SourceControlUtils sourceControlUtils)
  {
    this.gitClientFactory = gitClientFactory;
    this.membershipMappingService = membershipMappingService;
    this.roleDAO = roleDAO;
    this.userDirectory = userDirectory;
    this.applicationDAO = applicationDAO;
    this.sourceControlUtils = sourceControlUtils;
  }

  @Authorize(permission = Permission.EDIT_ACCESS_CONTROL)
  public Set<String> automaticRoleAssignment(
      final @AuthzContext(Key.APPLICATION_PUBLIC_ID) String publicId)
  {
    final Application application = this.applicationDAO.getByPublicIdNotNull(publicId);
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());
    if (gitRepositoryInfo == null) {
      throw new NotFoundException(String.format(
          "Cannot find GitRepositoryInfo for %s", publicId));
    }
    Set<String> githubUsernames = getGithubRepositoryContributorUsernames(gitRepositoryInfo);
    Set<String> systemAvailableUsernames = getAllAvailableUsernamesForMatching(githubUsernames);
    if (systemAvailableUsernames.isEmpty()) {
      return Collections.emptySet();
    }
    return autoCreateDeveloperRoleMatchingForSCMRepositoryUsers(application, systemAvailableUsernames);
  }

  private Set<String> autoCreateDeveloperRoleMatchingForSCMRepositoryUsers(
      final Application application,
      final Set<String> githubUsernames)
  {
    Role developerRole = roleDAO.getByName(RoleDAO.DEVELOPER);

    try {
      membershipMappingService.grantRoleMembershipsForNonGlobalContextNoAuthz(application.getType(),
          application.getId(), developerRole.getId(), MemberType.USER, githubUsernames);
      log.trace("{} access granted to users {} over application {}", developerRole.getDescription(), githubUsernames,
          application.getPublicId());
    }
    catch (SQLException e) {
      log.error("Failed to create membership mappings", e.getCause());
      throw new RuntimeException("There was an error creating membership mappings");
    }

    return githubUsernames;
  }

  private Set<String> getGithubRepositoryContributorUsernames(final GitRepositoryInfo githubRepositoryInfo) {
    try {
      final GitApiClient gitHubApiClient = gitClientFactory.createApiClient(githubRepositoryInfo);
      return gitHubApiClient.getRepositoryContributorsUsernames();
    }
    catch (IOException e) {
      throw new RuntimeException("There was an error communicating with SCM", e.getCause());
    }
    catch (RuntimeException e) {
      throw new BadRequestException("Error while creating scm client: " + e.getMessage(), e);
    }
  }

  private Set<String> getAllAvailableUsernamesForMatching(final Set<String> githubUsernames) {
    Set<String> result = new HashSet<>(githubUsernames);
    final Set<String> invalidUsers = userDirectory.validateUsers(githubUsernames);
    result.removeAll(invalidUsers);

    return result;
  }
}

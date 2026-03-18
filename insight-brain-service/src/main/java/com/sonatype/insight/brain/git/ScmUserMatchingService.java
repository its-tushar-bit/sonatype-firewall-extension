/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMatchingResultDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.UserMapping;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.api.ContributorInfoProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.Contributor;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.GITLOG_EMAIL;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.GITLOG_FULLNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_EMAIL;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_FULLNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_USERNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_EMAIL;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_FULLNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_USERNAME;
import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Named
@Singleton
public class ScmUserMatchingService
{
  private static final Logger log = LoggerFactory.getLogger(ScmUserMatchingService.class);

  private final GitClientFactory gitClientFactory;

  private final MembershipMappingService membershipMappingService;

  private final RoleDAO roleDAO;

  private final UserDirectory userDirectory;

  private final ApplicationDAO applicationDAO;

  private final SourceControlUtils sourceControlUtils;

  private final ScmUserMappingService scmUserMappingService;

  private final TelemetrySender telemetrySender;

  @Inject
  ScmUserMatchingService(
      GitClientFactory gitClientFactory,
      MembershipMappingService membershipMappingService,
      RoleDAO roleDAO,
      UserDirectory userDirectory,
      ApplicationDAO applicationDAO,
      SourceControlUtils sourceControlUtils,
      ScmUserMappingService scmUserMappingService,
      TelemetrySender telemetrySender)
  {
    this.gitClientFactory = gitClientFactory;
    this.membershipMappingService = membershipMappingService;
    this.roleDAO = roleDAO;
    this.userDirectory = userDirectory;
    this.applicationDAO = applicationDAO;
    this.sourceControlUtils = sourceControlUtils;
    this.scmUserMappingService = scmUserMappingService;
    this.telemetrySender = telemetrySender;
  }

  @Authorize(permission = Permission.EDIT_ACCESS_CONTROL)
  public SCMUserMatchingResultDTO automaticRoleAssignmentByMapping(
      final @AuthzContext(Key.APPLICATION_PUBLIC_ID) String publicId,
      SCMUserMappingsDTO scmUserMappingsDTO)
  {
    final Application application = applicationDAO.getByPublicId(publicId);
    return automaticRoleAssignmentByMappingNoAuthz(application, scmUserMappingsDTO);
  }

  SCMUserMatchingResultDTO automaticRoleAssignmentByMappingNoAuthz(
      Application application,
      SCMUserMappingsDTO scmUserMappingsDTO)
  {
    // will either return the supplied mappings or try to fetch from the db by app id when null
    scmUserMappingsDTO = provideConfiguredSCMUserMappingsWhenNull(scmUserMappingsDTO, application.getId());

    if (isNull(scmUserMappingsDTO)) {
      throw new BadRequestException(
          "An SCMUserMappingsDTO must be provided either with the request or at the organization level");
    }

    final List<UserMapping> userMappings = scmUserMappingsDTO.mappings();

    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());
    if (gitRepositoryInfo == null) {
      throw new NotFoundException(String.format("Cannot find GitRepositoryInfo for %s", application.getPublicId()));
    }

    final SCMUserMatchingResultDTO scmUserMatchingResultDTO = getMatchingUsers(gitRepositoryInfo, userMappings);

    autoCreateDeveloperRoleMatchingForSCMRepositoryUsers(
        application, scmUserMatchingResultDTO.matchedUsers(), scmUserMappingsDTO.role());

    return scmUserMatchingResultDTO;
  }

  private SCMUserMatchingResultDTO getMatchingUsers(
      final GitRepositoryInfo gitRepositoryInfo,
      List<UserMapping> userMappings)
  {
    // these will be fetched once, if needed to avoid fetching anything we don't use
    Set<String> githubUsernames = null;
    Set<Contributor> contributors = null;

    for (final UserMapping mapping : userMappings) {
      if (isNull(githubUsernames) && needsToFetchSCMUserNames(mapping)) {
        githubUsernames = getGithubRepositoryContributorUsernames(gitRepositoryInfo);
      }

      if (isNull(contributors) && needsToFetchContributors(mapping)) {
        contributors = getContributorsBasedOnLastHundredCommits(gitRepositoryInfo);
      }

      final Set<String> matchingUsers = getUserNamesForMapping(mapping, githubUsernames, contributors);

      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTOMATIC_ROLE_ASSIGNMENT_USER_MATCHING_ATTEMPT);
      telemetryData.put("matched_users_count", matchingUsers.size());
      telemetryData.put("mapping_strategy_used", JsonUtils.format(mapping.toSimpleEntry()));
      telemetrySender.send(telemetryData);

      // stop as soon as any strategy has any match
      if (!matchingUsers.isEmpty()) {
        return new SCMUserMatchingResultDTO(mapping, matchingUsers);
      }
    }

    return new SCMUserMatchingResultDTO(null, Sets.newHashSet());
  }

  private Set<String> getUserNamesForMapping(
      final UserMapping userMapping,
      final Set<String> githubUsernames,
      final Set<Contributor> contributors)
  {
    switch (userMapping.from()) {
      case SCM_USERNAME -> {
        return matchValueTo(userMapping.to(), githubUsernames);
      }
      case SCM_EMAIL -> {
        final Set<String> scmEmails = contributors
            .stream()
            .map(Contributor::getScmUserEmail)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return matchValueTo(userMapping.to(), scmEmails);
      }
      case SCM_FULLNAME -> {
        final Set<String> scmNames = contributors
            .stream()
            .map(Contributor::getScmName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return matchValueTo(userMapping.to(), scmNames);
      }
      case GITLOG_EMAIL -> {
        final Set<String> commitEmails = contributors
            .stream()
            .map(Contributor::getCommitAuthorEmail)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return matchValueTo(userMapping.to(), commitEmails);
      }
      case GITLOG_FULLNAME -> {
        final Set<String> commitNames = contributors
            .stream()
            .map(Contributor::getCommitAuthorName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return matchValueTo(userMapping.to(), commitNames);
      }
      default -> {
        return Sets.newHashSet();
      }
    }
  }

  private Set<String> matchValueTo(
      final ToMappingEnum toMappingEnum,
      final Set<String> valuesToMatch)
  {
    if (toMappingEnum.equals(IQ_USERNAME)) {
      return getAllAvailableUsernamesForMatching(valuesToMatch);
    }
    else if (toMappingEnum.equals(IQ_FULLNAME)) {
      return fetchRealNamesIfExistFromIQ(valuesToMatch);
    }
    else if (toMappingEnum.equals(IQ_EMAIL)) {
      return fetchEmailsIfExistsFromIQ(valuesToMatch);
    }
    else {
      // this should not happen unless the enum has been updated without updating this code, currently all cases
      // are handled.
      throw new RuntimeException("Unmatched user to mapping");
    }
  }

  private Set<String> fetchRealNamesIfExistFromIQ(final Set<String> namesToMatch) {
    return userDirectory.getUsersByRealNames(namesToMatch)
        .stream()
        .map(Member::getInternalName)
        .collect(Collectors.toSet());
  }

  private Set<String> fetchEmailsIfExistsFromIQ(final Set<String> emailsToMatch) {
    return userDirectory.getUsersByEmails(emailsToMatch)
        .stream()
        .map(Member::getInternalName)
        .collect(Collectors.toSet());
  }

  private boolean needsToFetchSCMUserNames(final UserMapping userMapping) {
    return userMapping.from().equals(SCM_USERNAME);
  }

  private boolean needsToFetchContributors(final UserMapping userMapping) {
    return userMapping.from().equals(SCM_EMAIL) ||
        userMapping.from().equals(SCM_FULLNAME) ||
        userMapping.from().equals(GITLOG_EMAIL) ||
        userMapping.from().equals(GITLOG_FULLNAME);
  }

  private void autoCreateDeveloperRoleMatchingForSCMRepositoryUsers(
      final Application application,
      final Set<String> githubUsernames,
      final String roleName)
  {
    final Role role = roleDAO.getByName(isNull(roleName) ? RoleDAO.DEVELOPER : roleName);

    try {
      membershipMappingService.grantRoleMembershipsForNonGlobalContextNoAuthz(application.getType(),
          application.getId(), role.getId(), MemberType.USER, githubUsernames);
      log.trace("{} access granted to {} users over application {}", role.getDescription(), githubUsernames.size(),
          application.getPublicId());
    }
    catch (SQLException e) {
      log.error("Failed to create membership mappings", e.getCause());
      throw new RuntimeException("There was an error creating membership mappings");
    }
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

  private Set<Contributor> getContributorsBasedOnLastHundredCommits(final GitRepositoryInfo githubRepositoryInfo) {
    try {
      final ContributorInfoProvider contributorInfoProvider = gitClientFactory
          .createContributorInfoProvider(githubRepositoryInfo);

      Entry<String, String> repositoryOwnerAndName = getRepositoryOwnerAndName(githubRepositoryInfo.repositoryUrl);

      final var paginatedResult = contributorInfoProvider.getContributorsFromGitLogs(
          repositoryOwnerAndName.getKey(),
          repositoryOwnerAndName.getValue(),
          100,
          null);

      return paginatedResult.getContributors();
    }
    catch (IOException e) {
      throw new RuntimeException("There was an error communicating with SCM: ", e.getCause());
    }
    catch (RuntimeException e) {
      throw new BadRequestException("Error while creating scm client: " + e.getMessage(), e);
    }
  }

  private Entry<String, String> getRepositoryOwnerAndName(final String repositoryUrl) {
    List<String> tempFields = Arrays.stream(repositoryUrl.split("/")).collect(Collectors.toList());
    Collections.reverse(tempFields);
    return new SimpleEntry<>(tempFields.get(1), tempFields.get(0));
  }

  private Set<String> getAllAvailableUsernamesForMatching(final Set<String> githubUsernames) {
    Set<String> result = new HashSet<>(githubUsernames);
    final Set<String> invalidUsers = userDirectory.validateUsers(githubUsernames);
    result.removeAll(invalidUsers);

    return result;
  }

  private SCMUserMappingsDTO provideConfiguredSCMUserMappingsWhenNull(
      final SCMUserMappingsDTO scmUserMappingsDTO,
      final String internalAppId)
  {
    if (!isNull(scmUserMappingsDTO)) {
      return scmUserMappingsDTO;
    }
    else {
      final SCMUserMappingsResponseDTO preConfiguredUserMappings =
          scmUserMappingService.getUserMappingsByOwnerNoAuthz(APPLICATION, internalAppId);
      return nonNull(preConfiguredUserMappings) ? preConfiguredUserMappings.userMapping() : null;
    }
  }
}

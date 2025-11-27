/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;

import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.ExpiredCredentialException;
import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.DirectoryEntity;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.builder.Combine;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.Property;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import org.apache.shiro.authc.UsernamePasswordToken;

public class CrowdClient
{
  private final com.atlassian.crowd.service.client.CrowdClient crowdClient;

  public CrowdClient(com.atlassian.crowd.service.client.CrowdClient crowdClient) {
    this.crowdClient = crowdClient;
  }

  public UserPrincipal authenticateUser(UsernamePasswordToken usernamePasswordToken)
      throws UserNotFoundException, OperationFailedException, ApplicationPermissionException,
             InvalidAuthenticationException, ExpiredCredentialException, InactiveAccountException
  {
    User crowdUser = crowdClient.authenticateUser(usernamePasswordToken.getUsername(),
        new String(usernamePasswordToken.getPassword()));
    Set<String> groupNames = getGroupNames(crowdUser.getName());
    return createUserPrincipal(crowdUser, CrowdRealm.ID, groupNames);
  }

  public UserPrincipal getUser(UserToken userToken)
      throws UserNotFoundException, OperationFailedException, ApplicationPermissionException,
             InvalidAuthenticationException, InactiveAccountException
  {
    User crowdUser = crowdClient.getUser(userToken.getUsername());
    if (!crowdUser.isActive()) {
      throw new InactiveAccountException(crowdUser.getName());
    }
    Set<String> groupNames = getGroupNames(crowdUser.getName());
    return createUserPrincipal(crowdUser, UserTokenRealm.ID, groupNames);
  }

  private Set<String> getGroupNames(String username)
      throws UserNotFoundException, OperationFailedException, ApplicationPermissionException,
             InvalidAuthenticationException
  {
    return crowdClient.getGroupsForNestedUser(username, 0, -1).stream()
        .filter(Group::isActive)
        .map(DirectoryEntity::getName)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private UserPrincipal createUserPrincipal(
      User crowdUser,
      String realmId,
      Set<String> groupNames)
  {
    return new UserPrincipal(crowdUser.getName(), crowdUser.getDisplayName(), realmId, groupNames);
  }

  public void testConnection()
      throws OperationFailedException, ApplicationPermissionException, InvalidAuthenticationException
  {
    crowdClient.testConnection();
  }

  public Set<Member> searchUsersByUsernames(Set<String> usernames)
      throws OperationFailedException, ApplicationPermissionException, InvalidAuthenticationException
  {
    return crowdClient.searchUsers(anyNameMatchesAndActive(usernames), 0, -1).stream()
        .map(user ->
            new Member(MemberType.USER, user.getName(), user.getDisplayName(), user.getEmailAddress(),
                    CrowdRealm.ID, user.getExternalId()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Set<Member> searchUsersByDisplayName(String displayName)
      throws OperationFailedException, ApplicationPermissionException, InvalidAuthenticationException
  {
    return crowdClient.searchUsers(displayNameMatchesAndActive(displayName), 0, -1).stream()
        .map(user ->
            new Member(MemberType.USER, user.getName(), user.getDisplayName(), user.getEmailAddress(),
                    CrowdRealm.ID, user.getExternalId()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Set<Member> searchGroupsByGroupNames(Set<String> groupNames)
      throws OperationFailedException, ApplicationPermissionException, InvalidAuthenticationException
  {
    return crowdClient.searchGroups(anyNameMatchesAndActive(groupNames), 0, -1).stream()
        .map(group -> new Member(MemberType.GROUP, group.getName(), group.getName(), null, CrowdRealm.ID))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  // Visible for testing
  SearchRestriction anyNameMatchesAndActive(Set<String> names) {
    return Combine.allOf(
        Combine.anyOf(names.stream()
            .map(name -> createStringPropertyRestriction(UserTermKeys.USERNAME, name))
            .collect(Collectors.toList())
        ),
        new TermRestriction<>(UserTermKeys.ACTIVE, MatchMode.EXACTLY_MATCHES, true)
    );
  }

  public Set<Member> getUsersByGroupName(String groupName)
      throws OperationFailedException, ApplicationPermissionException, GroupNotFoundException,
             InvalidAuthenticationException
  {
    return crowdClient.getNestedUsersOfGroup(groupName, 0, -1).stream()
        .filter(User::isActive)
        .map(user ->
            new Member(MemberType.USER, user.getName(), user.getDisplayName(), user.getEmailAddress(), CrowdRealm.ID))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  // Visible for testing
  SearchRestriction displayNameMatchesAndActive(String displayName) {
    return Combine.allOf(
        createStringPropertyRestriction(UserTermKeys.DISPLAY_NAME, displayName),
        new TermRestriction<>(UserTermKeys.ACTIVE, MatchMode.EXACTLY_MATCHES, true)
    );
  }

  private TermRestriction<String> createStringPropertyRestriction(Property<String> property, String value) {
    if (value.contains("*")) {
      return new TermRestriction<>(property, MatchMode.CONTAINS, value.replace("*", ""));
    }
    return new TermRestriction<>(property, MatchMode.EXACTLY_MATCHES, value);
  }
}

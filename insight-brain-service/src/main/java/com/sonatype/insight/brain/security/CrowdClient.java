/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.ExpiredCredentialException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.DirectoryEntity;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.User;
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
}

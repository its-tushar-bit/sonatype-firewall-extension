/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

/**
 * Firewall-specific permission gate that resolves the set of repository IDs
 * a user may access on the Firewall Dashboard.
 *
 * Extracted into its own bean to avoid a circular dependency between
 * ApiFirewallService and ApiFirewallMetricsService.
 */
@Named
public class FirewallPermissionGate
{
  private final AuthorizationChecker authorizationChecker;

  private final CurrentUser currentUser;

  private final OwnerDAO ownerDAO;

  @Inject
  public FirewallPermissionGate(
      final AuthorizationChecker authorizationChecker,
      final CurrentUser currentUser,
      final OwnerDAO ownerDAO)
  {
    this.authorizationChecker = authorizationChecker;
    this.currentUser = currentUser;
    this.ownerDAO = ownerDAO;
  }

  /**
   * Returns null for full-access users (container READ), or a non-empty Set of
   * permitted proxy repository IDs for scoped users.
   *
   * @throws UnauthenticatedException if the user is not authenticated
   * @throws UnauthorizedException if the user has no access to any proxy repository
   */
  public Set<String> resolvePermittedRepositoryIds() {
    if (currentUser.isAnonymous()) {
      throw new UnauthenticatedException("Authentication required");
    }

    Map<Key, Object> containerContext = Map.of(Key.OWNER, RepositoryContainer.SINGLETON);
    if (authorizationChecker.isPermitted(currentUser.getUserPrincipal(), Permission.READ, containerContext)) {
      return null;
    }

    Set<String> permitted = ownerDAO.getPermittedProxyRepositoryIds(
        Permission.READ,
        currentUser.getUserPrincipal().getUsername(),
        currentUser.getUserPrincipal().getMembership());

    if (permitted.isEmpty()) {
      throw new UnauthorizedException("No access to any proxy repository");
    }

    return permitted;
  }
}

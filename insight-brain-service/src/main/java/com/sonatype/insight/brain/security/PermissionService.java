/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.subject.Subject;

@Named
public class PermissionService
{
  private final AuthorizationChecker authzChecker;

  private final MembershipMappingDAO membershipMappingDAO;

  private final RolePermissionDAO rolePermissionDAO;

  @Inject
  public PermissionService(
      AuthorizationChecker authzChecker,
      MembershipMappingDAO membershipMappingDAO,
      RolePermissionDAO rolePermissionDAO)
  {
    this.authzChecker = authzChecker;
    this.membershipMappingDAO = membershipMappingDAO;
    this.rolePermissionDAO = rolePermissionDAO;
  }

  public Set<Permission> validatePermissionForPublicApplicationId(
      Subject subject,
      String publicAppId,
      Set<Permission> permissions)
  {
    checkPermissionsSet(permissions);

    if (!subject.isAuthenticated()) {
      return EnumSet.noneOf(Permission.class);
    }

    Map<AuthzContext.Key, Object> contextParameters = new EnumMap<>(AuthzContext.Key.class);

    contextParameters.put(Key.APPLICATION_PUBLIC_ID, publicAppId);

    return checkPermissions(subject, permissions, contextParameters);
  }

  public Set<Permission> validatePermission(
      Subject subject,
      OwnerType ownerType,
      String ownerId,
      Set<Permission> permissions)
  {
    if (!subject.isAuthenticated()) {
      return EnumSet.noneOf(Permission.class);
    }
    Map<AuthzContext.Key, Object> contextParameters = new EnumMap<>(AuthzContext.Key.class);
    switch (ownerType) {
      case APPLICATION:
        contextParameters.put(AuthzContext.Key.APPLICATION_ID, ownerId);
        break;
      case ORGANIZATION:
        contextParameters.put(AuthzContext.Key.ORGANIZATION_ID, ownerId);
        break;
      case REPOSITORY_CONTAINER:
        contextParameters.put(AuthzContext.Key.ID, ownerId);
        contextParameters.put(AuthzContext.Key.TYPE, OwnerType.REPOSITORY_CONTAINER);
        break;
      case REPOSITORY_MANAGER:
        contextParameters.put(AuthzContext.Key.REPOSITORY_MANAGER_ID, ownerId);
        break;
      case REPOSITORY:
        contextParameters.put(AuthzContext.Key.REPOSITORY_ID, ownerId);
        break;
      case GLOBAL:
        break;
      default:
        throw new IllegalArgumentException("Unknown owner type: " + ownerType);
    }

    return checkPermissions(subject, permissions, contextParameters);
  }

  private void checkPermissionsSet(Set<Permission> permissions) {
    if (permissions == null || permissions.isEmpty()) {
      throw new BadRequestException("Must specify permissions to check.");
    }
  }

  private EnumSet<Permission> checkPermissions(
      Subject subject,
      Set<Permission> permissions,
      Map<AuthzContext.Key, Object> contextParameters)
  {
    EnumSet<Permission> result = EnumSet.noneOf(Permission.class);

    UserPrincipal user = (UserPrincipal) subject.getPrincipal();
    for (Permission permission : permissions) {
      if (authzChecker.isPermitted(user, permission, contextParameters)) {
        result.add(permission);
      }
    }

    return result;
  }

  public Set<String> getContextIdsForUserWithPermission(UserPrincipal userPrincipal, Permission permission) {
    Collection<MembershipMapping> membershipMappings =
        membershipMappingDAO.getByUserCaseInsensitiveAndGroups(userPrincipal.getUsername(),
            userPrincipal.getMembership());

    Set<String> contextIds = new HashSet<>();

    for (MembershipMapping membershipMapping : membershipMappings) {
      if (rolePermissionDAO.getPermissionsForRole(membershipMapping.getRoleId()).contains(permission)) {
        contextIds.add(membershipMapping.getContextId());
      }
    }

    return contextIds;
  }
}

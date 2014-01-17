/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzFilter.Context;

/**
 * Evaluates authorization.
 * 
 * @since 1.7
 */
public class AuthorizationChecker
{
  private final MembershipMappingDAO membershipDAO;

  private final RolePermissionDAO rolePermissionDAO;

  private final ContextResolver contextResolver;

  public AuthorizationChecker() {
    this(new ContextResolver());
  }

  AuthorizationChecker(ContextResolver contextResolver) {
    this.rolePermissionDAO = new RolePermissionDAO();
    this.membershipDAO = new MembershipMappingDAO();
    this.contextResolver = contextResolver;
  }

  /**
   * Determines whether the given user has the specified permission in the supplied context or any of its ancestor
   * contexts.
   */
  public boolean isPermitted(UserPrincipal user, Permission permission, Map<AuthzContext.Key, Object> contextParameters) {
    Iterable<String> contextIds = contextResolver.resolveContextIds(contextParameters);
    return isPermitted(user, permission, contextIds);
  }

  /**
   * Determines whether the given user has the specified permission in any of the supplied contexts.
   */
  boolean isPermitted(UserPrincipal user, Permission permission, Iterable<String> contextIds) {
    if (user != null) {
      Set<String> roleIds = rolePermissionDAO.getRoleIdsByPermission(permission);
      for (String contextId : contextIds) {
        if (isUserHavingAnyRoleInContext(user, roleIds, contextId)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns a new collection that holds only those input entities for which the given user has the specified
   * permission.
   */
  @SuppressWarnings("unchecked")
  public Collection<?> filterByPermission(UserPrincipal user, Permission permission, Object entities, Context contextEntity)
  {
    Collection<Object> filtered = newCollection(entities);
    if (user != null) {
      switch (contextEntity) {
        case APPLICATION:
          filter(filtered, user, permission, (Iterable<Application>) entities, contextResolver.APPLICATION);
          break;
        case ORGANIZATION:
          filter(filtered, user, permission, (Iterable<Organization>) entities, contextResolver.ORGANIZATION);
          break;
        default:
          throw new IllegalStateException("Cannot check authorization in unknown context " + contextEntity);
      }
    }
    return filtered;
  }

  private static <T> Collection<T> newCollection(Object prototype) {
    if (prototype instanceof Set) {
      return new LinkedHashSet<T>();
    }
    else {
      return new ArrayList<T>();
    }
  }

  private <T> void filter(Collection<Object> filtered, UserPrincipal user, Permission permission,
      Iterable<? extends T> entities, ContextIdResolver<T> resolver)
  {
    Set<String> roleIds = rolePermissionDAO.getRoleIdsByPermission(permission);
    Map<String, Boolean> resultByContextId = new HashMap<String, Boolean>(256);
    for (T entity : entities) {
      Iterable<String> contextIds = resolver.resolveContextIds(entity);
      if (isUserHavingAnyRoleInAnyContext(user, roleIds, contextIds, resultByContextId)) {
        filtered.add(entity);
      }
    }
  }

  private boolean isUserHavingAnyRoleInAnyContext(UserPrincipal user, Set<String> roleIds, Iterable<String> contextIds,
      Map<String, Boolean> resultByContextId)
  {
    for (String contextId : contextIds) {
      Boolean result = resultByContextId.get(contextId);
      if (result == null) {
        result = isUserHavingAnyRoleInContext(user, roleIds, contextId);
        resultByContextId.put(contextId, result);
      }
      if (result) {
        return true;
      }
    }
    return false;
  }

  private boolean isUserHavingAnyRoleInContext(UserPrincipal user, Set<String> roleIds, String contextId) {
    Collection<MembershipMapping> memberships = membershipDAO.getByContextId(contextId);
    for (MembershipMapping membership : memberships) {
      if (roleIds.contains(membership.getRoleId()) && isUserIncluded(membership, user)) {
        return true;
      }
    }
    return false;
  }

  private boolean isUserIncluded(MembershipMapping membership, UserPrincipal user) {
    if (MemberType.USER.equals(membership.getMemberType())) {
      return membership.getMemberName().equalsIgnoreCase(user.username);
    }
    if (MemberType.GROUP.equals(membership.getMemberType())) {
      return user.membership.contains(membership.getMemberName());
    }
    return false;
  }
}

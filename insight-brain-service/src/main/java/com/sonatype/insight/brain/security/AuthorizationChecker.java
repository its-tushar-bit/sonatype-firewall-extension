/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzFilter.Context;

import com.google.common.annotations.VisibleForTesting;

/**
 * Evaluates authorization.
 *
 * @since 1.7
 */
public class AuthorizationChecker
{
  private MembershipMappingDAO membershipMappingDAO;

  private RolePermissionDAO rolePermissionDAO;

  private final ContextResolver contextResolver;

  @Inject
  AuthorizationChecker(ContextResolver contextResolver) {
    // additionally see javadoc on #injectDAOs
    this.contextResolver = contextResolver;
  }

  @VisibleForTesting
  AuthorizationChecker(
      final ContextResolver contextResolver,
      final RolePermissionDAO rolePermissionDAO,
      final MembershipMappingDAO membershipMappingDAO)
  {
    this.contextResolver = contextResolver;
    this.rolePermissionDAO = rolePermissionDAO;
    this.membershipMappingDAO = membershipMappingDAO;
  }

  /**
   * Injected using Guice <a href="https://github.com/google/guice/wiki/Injections#method-injection">method
   * injection</a> as this is a dependency of Shiro {@link org.apache.shiro.aop.MethodInterceptor} using AOP. See setup
   * in {@link SecurityAopModule} using `requestInjection`.
   */
  @Inject
  public void injectDAOs(final MembershipMappingDAO membershipMappingDAO, final RolePermissionDAO rolePermissionDAO) {
    this.membershipMappingDAO = membershipMappingDAO;
    this.rolePermissionDAO = rolePermissionDAO;
  }

  /**
   * Determines whether the given user has the specified permission in the supplied context or any of its ancestor
   * contexts.
   */
  public boolean isPermitted(UserPrincipal user,
                             Permission permission,
                             Map<AuthzContext.Key, Object> contextParameters)
  {
    Iterable<String> contextIds = contextResolver.resolveContextIds(contextParameters);
    return isPermitted(user, permission, contextIds);
  }

  /**
   * Determines whether the given user has the specified permission in any of the supplied contexts.
   */
  boolean isPermitted(UserPrincipal user, Permission permission, Iterable<String> contextIds) {
    if (user != null) {
      Set<String> roleIds = rolePermissionDAO.getRoleIdsByPermission(permission);
      if (permission.isGlobal()) {
        // The permission is global, so the contexts don't really matter.
        return isUserHavingAnyRoleInAnyContext(user, roleIds);
      }
      else {
        // The permission is non-global, so check it in the given contexts.
        for (String contextId : contextIds) {
          if (isUserHavingAnyRoleInContext(user, roleIds, contextId)) {
            return true;
          }
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
  public <T> Collection<T> filterByPermission(UserPrincipal user,
                                              Permission permission,
                                              Iterable<T> entities,
                                              Context contextEntity)
  {
    if (user == null) {
      return newCollection(entities);
    }
    switch (contextEntity) {
      case APPLICATION:
        return (Collection<T>) filter(user, permission, (Iterable<Application>) entities,
            contextResolver.resolverForApplication);
      case ORGANIZATION:
        return (Collection<T>) filter(user, permission, (Iterable<Organization>) entities,
            contextResolver.resolverForOrganization);
      case REPOSITORY:
        return (Collection<T>) filter(user, permission, (Iterable<Repository>) entities,
            contextResolver.resolverForRepository);
      case REPOSITORY_MANAGER:
        return (Collection<T>) filter(user, permission, (Iterable<RepositoryManager>) entities,
            contextResolver.resolverForRepositoryManager);
      default:
        throw new IllegalStateException("Cannot check authorization in unknown context " + contextEntity);
    }
  }

  private static <T> Collection<T> newCollection(Object prototype) {
    if (prototype instanceof Set) {
      return new LinkedHashSet<>();
    }
    else {
      return new ArrayList<>();
    }
  }

  private <T> Collection<T> filter(UserPrincipal user,
                                   Permission permission,
                                   Iterable<T> entities,
                                   ContextIdResolver<? super T> resolver)
  {
    Collection<T> filtered = newCollection(entities);
    Set<String> roleIds = rolePermissionDAO.getRoleIdsByPermission(permission);
    Map<String, Boolean> permitsByContextId = new HashMap<>(256);

    String username = user.getUsername();
    Set<String> groups = user.getMembership();

    Set<String> userContextIds =
        membershipMappingDAO.getByUserCaseInsensitiveAndGroupsAndRoles(username, groups, roleIds).stream()
        .map(MembershipMapping::getContextId)
        .collect(Collectors.toSet());

    for (T entity : entities) {
      Iterable<String> contextIds = resolver.resolveContextIds(entity);
      if (isUserHavingAnyRoleInAnyContext(userContextIds, contextIds, permitsByContextId)) {
        filtered.add(entity);
      }
    }
    return filtered;
  }

  private boolean isUserHavingAnyRoleInAnyContext(Set<String> userContextIds,
                                                  Iterable<String> contextIds,
                                                  Map<String, Boolean> permitsByContextId)
  {
    List<String> uncachedContextIds = new ArrayList<>();

    // consult the cache first (walking up the hierarchy)
    for (String contextId : contextIds) {
      Boolean permit = permitsByContextId.get(contextId);
      if (permit != null) {
        if (permit) {
          // due to inheritance, the permit also implies to all child contexts
          for (String childId : uncachedContextIds) {
            permitsByContextId.put(childId, true);
          }
          return true;
        }
        // this context and none of its ancestors permit access
        break;
      }
      uncachedContextIds.add(contextId);
    }

    // consult the database about the uncached contexts (walking down the hierarchy)
    for (int i = uncachedContextIds.size() - 1; i >= 0; i--) {
      String contextId = uncachedContextIds.get(i);
      boolean permit = userContextIds.contains(contextId);
      permitsByContextId.put(contextId, permit);
      if (permit) {
        // due to inheritance, the permit also implies to all child contexts
        for (i--; i >= 0; i--) {
          String childId = uncachedContextIds.get(i);
          permitsByContextId.put(childId, true);
        }
        return true;
      }
    }

    return false;
  }

  private boolean isUserHavingAnyRoleInContext(UserPrincipal user, Set<String> roleIds, String contextId) {
    Collection<MembershipMapping> memberships = membershipMappingDAO.getByContextId(contextId);
    for (MembershipMapping membership : memberships) {
      if (roleIds.contains(membership.getRoleId()) && membership.includes(user)) {
        return true;
      }
    }
    return false;
  }

  private boolean isUserHavingAnyRoleInAnyContext(UserPrincipal user, Set<String> roleIds) {
    return membershipMappingDAO.isUserHavingRolesInAnyContext(roleIds, user.getUsername(), user.getMembership());
  }
}

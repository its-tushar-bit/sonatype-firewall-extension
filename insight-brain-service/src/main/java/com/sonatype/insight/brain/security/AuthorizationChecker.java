/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzFilter.Context;

import com.google.common.annotations.VisibleForTesting;

import static com.sonatype.insight.brain.security.AuthorizationPermissionEntityFilter.newCollection;
import static java.util.stream.Collectors.toSet;

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

  private final AuthorizationPermissionEntityFilter entityPermissionFilter = new AuthorizationPermissionEntityFilter();

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
  public boolean isPermitted(
      UserPrincipal user,
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
  public <T> Collection<T> filterByPermission(
      UserPrincipal user,
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
      case APPLICATION_OR_ORGANIZATION:
        return (Collection<T>) filter(user, permission, (Iterable<? extends Owner>) entities,
            contextResolver.resolveForApplicationOrOrganization);
      default:
        throw new IllegalStateException("Cannot check authorization in unknown context " + contextEntity);
    }
  }

  private <T> Collection<T> filter(
      UserPrincipal user,
      Permission permission,
      Iterable<T> entities,
      ContextIdResolver<? super T> resolver)
  {
    Set<String> roleIds = rolePermissionDAO.getRoleIdsByPermission(permission);
    String username = user.getUsername();
    Set<String> groups = user.getMembership();
    Set<String> userContextIds = getContextIds(username, groups, roleIds);
    return entityPermissionFilter.filterWithPermissionCheck(entities, resolver, userContextIds);
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

  private Set<String> getContextIds(final String username, final Set<String> groups, final Set<String> roleIds) {
    switch (AuthorizationMembershipQueryStrategy.getStrategyFromEnv()) {
      // Direct context ID query - faster, less memory usage
      case DIRECT_CONTEXT_ID:
        return new HashSet<>(
            membershipMappingDAO
                .getContextIdsByUserCaseInsensitiveAndGroupsAndRoles(username, groups, roleIds));
      // Full MembershipMapping + mapping approach - traditional method
      case FULL_MEMBERSHIP_MAPPING_CONTEXT_ID:
      default:
        return membershipMappingDAO
            .getByUserCaseInsensitiveAndGroupsAndRoles(username, groups, roleIds)
            .stream()
            .map(MembershipMapping::getContextId)
            .collect(toSet());
    }
  }
}

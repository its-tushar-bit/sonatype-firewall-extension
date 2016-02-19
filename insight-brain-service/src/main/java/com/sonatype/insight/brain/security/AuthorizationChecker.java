/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzContext.Key;
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
  public boolean isPermitted(UserPrincipal user,
                             Permission permission,
                             Map<AuthzContext.Key, ContextParameter> contextParameters)
  {
    if (contextParameters.size() == 1 && contextParameters.values().iterator().next().multiple) {
      return checkIsPermittedMultiple(user, permission, contextParameters);
    }
    else {
      return checkIsPermittedSingle(user, permission, contextParameters);
    }
  }

  private boolean checkIsPermittedMultiple(final UserPrincipal user,
                                           final Permission permission,
                                           final Map<Key, ContextParameter> contextParameters)
  {
    ContextParameter parameter = contextParameters.values().iterator().next();
    if (parameter.object == null) {
      // anyone can see nothing
      return true;
    }
    else if (parameter.object instanceof Collection<?>) {
      Collection<?> paramObjects = (Collection<?>) parameter.object;
      for (Object o : paramObjects) {
        Map<Key, Object> contextParamMap = new EnumMap<>(Key.class);
        contextParamMap.put(parameter.key, o);
        Iterable<String> contextIds = contextResolver.resolveContextIds(contextParamMap);
        boolean permitted = isPermitted(user, permission, contextIds);
        if (!permitted) {
          return false;
        }
      }
      return true;
    }
    else {
      throw new IllegalStateException(parameter.toString() + " is not a Collection, which is required with multiple");
    }
  }

  private boolean checkIsPermittedSingle(final UserPrincipal user,
                                         final Permission permission,
                                         final Map<Key, ContextParameter> contextParameters)
  {
    Map<Key, Object> contextParamMap = new EnumMap<>(Key.class);
    for (ContextParameter cp : contextParameters.values()) {
      contextParamMap.put(cp.key, cp.object);
    }
    Iterable<String> contextIds = contextResolver.resolveContextIds(contextParamMap);
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
  public Collection<?> filterByPermission(UserPrincipal user,
                                          Permission permission,
                                          Object entities,
                                          Context contextEntity)
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
        case REPOSITORY:
          filter(filtered, user, permission, (Iterable<Repository>) entities, contextResolver.REPOSITORY);
          break;
        default:
          throw new IllegalStateException("Cannot check authorization in unknown context " + contextEntity);
      }
    }
    return filtered;
  }

  private static <T> Collection<T> newCollection(Object prototype) {
    if (prototype instanceof Set) {
      return new LinkedHashSet<>();
    }
    else {
      return new ArrayList<>();
    }
  }

  private <T> void filter(Collection<Object> filtered,
                          UserPrincipal user,
                          Permission permission,
                          Iterable<? extends T> entities,
                          ContextIdResolver<T> resolver)
  {
    Set<String> roleIds = rolePermissionDAO.getRoleIdsByPermission(permission);
    Map<String, Boolean> resultByContextId = new HashMap<>(256);
    for (T entity : entities) {
      Iterable<String> contextIds = resolver.resolveContextIds(entity);
      if (isUserHavingAnyRoleInAnyContext(user, roleIds, contextIds, resultByContextId)) {
        filtered.add(entity);
      }
    }
  }

  private boolean isUserHavingAnyRoleInAnyContext(UserPrincipal user,
                                                  Set<String> roleIds,
                                                  Iterable<String> contextIds,
                                                  Map<String, Boolean> resultByContextId)
  {
    List<String> uncachedContextIds = new ArrayList<>();

    // consult the cache first (walking up the hierarchy)
    for (String contextId : contextIds) {
      Boolean permit = resultByContextId.get(contextId);
      if (permit != null) {
        if (permit) {
          // due to inheritance, the permit also implies to all child contexts
          for (String childId : uncachedContextIds) {
            resultByContextId.put(childId, true);
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
      boolean permit = isUserHavingAnyRoleInContext(user, roleIds, contextId);
      resultByContextId.put(contextId, permit);
      if (permit) {
        // due to inheritance, the permit also implies to all child contexts
        for (i--; i >= 0; i--) {
          String childId = uncachedContextIds.get(i);
          resultByContextId.put(childId, true);
        }
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

  private boolean isUserHavingAnyRoleInAnyContext(UserPrincipal user, Set<String> roleIds) {
    Collection<MembershipMapping> memberships = membershipDAO.getByRoleIds(roleIds);
    for (MembershipMapping membership : memberships) {
      if (isUserIncluded(membership, user)) {
        return true;
      }
    }
    return false;
  }

  private boolean isUserIncluded(MembershipMapping membership, UserPrincipal user) {
    if (MemberType.USER.equals(membership.getMemberType())) {
      return membership.getMemberName().equalsIgnoreCase(user.getUsername());
    }
    if (MemberType.GROUP.equals(membership.getMemberType())) {
      return user.getMembership().contains(membership.getMemberName());
    }
    return false;
  }
}

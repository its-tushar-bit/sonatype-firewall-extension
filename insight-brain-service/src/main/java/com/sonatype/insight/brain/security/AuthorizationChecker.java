/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO.PermissionCheckResult;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.error.exception.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * Evaluates authorization by resolving context from method parameters and checking permissions.
 * <p>
 * This class combines the functionality of the former {@code ContextResolver} and
 * {@code AuthorizationPermissionEntityFilter} classes, providing a unified authorization checking API.
 *
 * @since 1.7
 */
public class AuthorizationChecker
{
  private volatile ApplicationDAO appDAO;

  private volatile OwnerDAO ownerDAO;

  private volatile ListableBeanFactory beanFactory;

  public AuthorizationChecker() {
    // no-arg constructor - for AOP pattern
  }

  /**
   * Constructor for testing.
   */
  @VisibleForTesting
  AuthorizationChecker(final ApplicationDAO appDAO, final OwnerDAO ownerDAO) {
    this.appDAO = appDAO;
    this.ownerDAO = ownerDAO;
  }

  /**
   * Injected after construction so the Shiro AOP interceptors can lazily resolve
   * DAO dependencies once the wider Spring context has registered them.
   */
  public void injectBeanFactory(final ListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  /**
   * Eagerly resolves DAO dependencies to fail fast at startup rather than at runtime.
   * Called via SmartInitializingSingleton after all singletons are instantiated.
   */
  public void validateDaoDependencies() {
    getApplicationDAO();
    getOwnerDAO();
  }

  /**
   * Determines whether the given user has the specified permission in the supplied context or any of its ancestor
   * contexts.
   */
  public boolean isPermitted(
      final UserPrincipal user,
      final Permission permission,
      final Map<Key, Object> contextParameters)
  {
    if (user == null) {
      return false;
    }
    return isPermitted(contextParameters, permission, user.getUsername(), user.getMembership());
  }

  /**
   * Determines whether the given user has the specified permission in the context resolved from the parameters.
   * Entity existence and permission are checked in a single DB query. If the entity doesn't exist, throws
   * NotFoundException (404) rather than returning false (403).
   */
  private boolean isPermitted(
      final Map<Key, Object> contextParameters,
      final Permission permission,
      final String username,
      final Set<String> groupNames)
  {
    ResolvedContext resolved = resolveOwnerContext(contextParameters);

    if (resolved == null) {
      // Empty params or TYPE=GLOBAL — check from root org (covers root org + global context via ancestors)
      resolved = new ResolvedContext(Organization.ROOT_ORGANIZATION_ID, OwnerType.ORGANIZATION);
    }

    PermissionCheckResult result = getOwnerDAO().checkPermissionForOwner(
        resolved.ownerId, resolved.ownerType, permission, username, groupNames);

    if (!result.entityExists() && resolved.notFoundMessage != null) {
      throw new NotFoundException(resolved.notFoundMessage);
    }

    return result.permitted();
  }

  /**
   * Returns a new collection that holds only those input entities for which the given user has the specified
   * permission.
   */
  public <T extends Owner> Collection<T> filterByPermission(
      final UserPrincipal user,
      final Permission permission,
      final Iterable<T> entities)
  {
    return filterByPermission(user, permission, entities, null);
  }

  /**
   * Returns a new collection that holds only those input entities for which the given user has the specified
   * permission. Uses the owner type to select the appropriate ancestor view for performance optimization.
   */
  public <T extends Owner> Collection<T> filterByPermission(
      final UserPrincipal user,
      final Permission permission,
      final Iterable<T> entities,
      final Context context)
  {
    if (user == null) {
      return newCollection(entities);
    }

    OwnerType ownerType = contextToOwnerType(context);
    List<String> ids = new ArrayList<>();
    for (T entity : entities) {
      ids.add(entity.getId());
    }

    Set<String> permittedIds = getOwnerDAO().getPermittedOwnerIds(
        ids, ownerType, permission, user.getUsername(), user.getMembership());

    Collection<T> filtered = newCollection(entities);
    for (T entity : entities) {
      if (permittedIds.contains(entity.getId())) {
        filtered.add(entity);
      }
    }
    return filtered;
  }

  /**
   * Converts an AuthzFilter Context to an OwnerType.
   * Returns null for APPLICATION_OR_ORGANIZATION to fall back to the generic owner_ancestor view.
   */
  private static OwnerType contextToOwnerType(final Context context) {
    if (context == null) {
      return null;
    }
    switch (context) {
      case APPLICATION:
        return OwnerType.APPLICATION;
      case ORGANIZATION:
        return OwnerType.ORGANIZATION;
      case REPOSITORY:
        return OwnerType.REPOSITORY;
      case REPOSITORY_MANAGER:
        return OwnerType.REPOSITORY_MANAGER;
      case APPLICATION_OR_ORGANIZATION:
        return null;
      default:
        throw new IllegalStateException("Unknown authorization context: " + context);
    }
  }

  /**
   * Resolves the owner ID and type from authorization context parameters.
   * Returns null for global/empty context.
   */
  private ResolvedContext resolveOwnerContext(final Map<Key, Object> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return null; // GLOBAL context
    }

    if (parameters.size() == 1) {
      return resolveFromSingleParameter(parameters);
    }

    if (parameters.size() == 2) {
      return resolveFromTypeAndId(parameters);
    }

    throw new IllegalArgumentException("Cannot resolve context from " + parameters);
  }

  /**
   * Resolves context from a single annotated parameter — either an entity or a string ID.
   */
  private ResolvedContext resolveFromSingleParameter(final Map<Key, Object> parameters) {
    Map.Entry<Key, Object> entry = parameters.entrySet().iterator().next();
    Key key = entry.getKey();
    Object value = entry.getValue();

    if (value == null) {
      throw new IllegalArgumentException("Null value for parameter " + key);
    }

    switch (key) {
      case APPLICATION:
        // Only the ID is used — ancestry is resolved via DB views, so mutable fields on the
        // passed-in entity (e.g. organizationId) do not affect the authorization check.
        Owner appOwner = (Owner) value;
        return new ResolvedContext(appOwner.getId(), OwnerType.APPLICATION);

      case ORGANIZATION:
        Owner orgOwner = (Owner) value;
        return new ResolvedContext(orgOwner.getId(), OwnerType.ORGANIZATION);

      case OWNER:
        Owner owner = (Owner) value;
        // OWNER entity resolves to its own ID with null ownerType (uses generic ancestor view)
        return new ResolvedContext(owner.getId(), null);

      case REPOSITORY_MANAGER:
        RepositoryManager rm = (RepositoryManager) value;
        if (rm.getId() != null) {
          return new ResolvedContext(rm.getId(), OwnerType.REPOSITORY_MANAGER);
        }
        return new ResolvedContext(RepositoryContainer.REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER);

      case REPOSITORY:
        Repository repo = (Repository) value;
        if (repo.getId() != null) {
          return new ResolvedContext(repo.getId(), OwnerType.REPOSITORY);
        }
        if (repo.getRepositoryManagerId() != null) {
          return new ResolvedContext(repo.getRepositoryManagerId(), OwnerType.REPOSITORY_MANAGER);
        }
        return new ResolvedContext(RepositoryContainer.REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER);

      case APPLICATION_OWNER:
        Owner appOwnerForCreate = (Owner) value;
        String appParentId = appOwnerForCreate.getParentOwnerId();
        if (appParentId == null) {
          appParentId = Organization.ROOT_ORGANIZATION_ID;
        }
        return new ResolvedContext(appParentId, OwnerType.ORGANIZATION);

      case ORGANIZATION_OWNER:
        Owner orgOwnerForCreate = (Owner) value;
        String parentId = orgOwnerForCreate.getParentOwnerId();
        if (parentId == null) {
          parentId = Organization.ROOT_ORGANIZATION_ID;
        }
        return new ResolvedContext(parentId, OwnerType.ORGANIZATION);

      case APPLICATION_ID:
      case ORGANIZATION_ID:
      case REPOSITORY_MANAGER_ID:
      case REPOSITORY_ID:
        String stringId = (String) value;
        OwnerType type = typeFromIdKey(key);
        return new ResolvedContext(stringId, type,
            entityNameForIdKey(key) + " with ID " + stringId + " does not exist.");

      case APPLICATION_PUBLIC_ID:
        String publicId = (String) value;
        List<String> ancestors = getApplicationDAO().getAncestorIdsByPublicId(publicId);
        if (ancestors.isEmpty()) {
          // App doesn't exist — use publicId as ownerId (won't match any ancestor, so permission check
          // will fail for non-global users, then isPermitted will check ownerExists and throw 404)
          return new ResolvedContext(publicId, OwnerType.APPLICATION,
              "Could not find an application with public ID " + publicId + ".");
        }
        return new ResolvedContext(ancestors.get(0), OwnerType.APPLICATION);

      default:
        throw new IllegalArgumentException("Cannot resolve context from " + key);
    }
  }

  /**
   * Derives OwnerType from an ID key.
   */
  private OwnerType typeFromIdKey(final Key key) {
    switch (key) {
      case APPLICATION_ID:
        return OwnerType.APPLICATION;
      case ORGANIZATION_ID:
        return OwnerType.ORGANIZATION;
      case REPOSITORY_MANAGER_ID:
        return OwnerType.REPOSITORY_MANAGER;
      case REPOSITORY_ID:
        return OwnerType.REPOSITORY;
      default:
        throw new IllegalArgumentException("Unexpected ID key: " + key);
    }
  }

  private String entityNameForIdKey(final Key key) {
    switch (key) {
      case APPLICATION_ID:
        return "Application";
      case ORGANIZATION_ID:
        return "Organization";
      case REPOSITORY_MANAGER_ID:
        return "Repository Manager";
      case REPOSITORY_ID:
        return "Repository";
      default:
        return key.name();
    }
  }

  private String entityNameForType(final OwnerType type) {
    switch (type) {
      case APPLICATION:
        return "Application";
      case ORGANIZATION:
        return "Organization";
      case REPOSITORY_MANAGER:
        return "Repository Manager";
      case REPOSITORY:
        return "Repository";
      default:
        return type.name();
    }
  }

  /**
   * Resolves context from a TYPE + ID/INTERNAL_ID pair.
   */
  private ResolvedContext resolveFromTypeAndId(final Map<Key, Object> parameters) {
    OwnerType type = (OwnerType) parameters.get(Key.TYPE);
    if (type == null) {
      throw new IllegalArgumentException("Expected TYPE parameter in " + parameters);
    }

    if (type == OwnerType.GLOBAL) {
      return null; // GLOBAL context
    }

    if (type == OwnerType.REPOSITORY_CONTAINER) {
      return new ResolvedContext(RepositoryContainer.REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER);
    }

    String id = (String) parameters.get(Key.ID);
    String internalId = (String) parameters.get(Key.INTERNAL_ID);

    if (type == OwnerType.APPLICATION && id != null) {
      List<String> ancestors = getApplicationDAO().getAncestorIdsByPublicId(id);
      if (ancestors.isEmpty()) {
        return new ResolvedContext(id, OwnerType.APPLICATION,
            "Could not find an application with public ID " + id + ".");
      }
      return new ResolvedContext(ancestors.get(0), OwnerType.APPLICATION);
    }

    String ownerId = internalId != null ? internalId : id;
    if (ownerId == null) {
      throw new IllegalArgumentException("Expected ID or INTERNAL_ID parameter for type " + type);
    }

    return new ResolvedContext(ownerId, type,
        entityNameForType(type) + " with ID " + ownerId + " does not exist.");
  }

  /**
   * Record representing a resolved authorization context.
   *
   * @param notFoundMessage if non-null, the error message to use in a NotFoundException when the permission check
   *          fails and the entity doesn't exist. Null for entity-object-based lookups where the entity
   *          is known to exist.
   */
  record ResolvedContext(String ownerId, OwnerType ownerType, String notFoundMessage)
  {
    ResolvedContext(String ownerId, OwnerType ownerType) {
      this(ownerId, ownerType, null);
    }
  }

  private ApplicationDAO getApplicationDAO() {
    if (appDAO == null && beanFactory != null) {
      appDAO = beanFactory.getBeanProvider(ApplicationDAO.class).getIfAvailable();
    }
    if (appDAO == null) {
      throw new IllegalStateException("ApplicationDAO is not available for authorization checks");
    }
    return appDAO;
  }

  private OwnerDAO getOwnerDAO() {
    if (ownerDAO == null && beanFactory != null) {
      ownerDAO = beanFactory.getBeanProvider(OwnerDAO.class).getIfAvailable();
    }
    if (ownerDAO == null) {
      throw new IllegalStateException("OwnerDAO is not available for authorization checks");
    }
    return ownerDAO;
  }

  /**
   * Creates a new collection of the same type as the prototype.
   */
  private static <T> Collection<T> newCollection(final Object prototype) {
    if (prototype instanceof Set) {
      return new LinkedHashSet<>();
    }
    else {
      return new ArrayList<>();
    }
  }
}

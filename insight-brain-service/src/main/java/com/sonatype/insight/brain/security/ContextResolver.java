/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Resolves authorization contexts from method parameters/results.
 *
 * @since 1.7
 */
class ContextResolver
{
  private static final Iterable<String> GLOBAL_CONTEXT = Collections.singleton(MembershipMapping.GLOBAL_CONTEXT_ID);

  private ApplicationDAO appDAO;

  private OrganizationDAO orgDAO;

  private RepositoryManagerDAO repoManagerDAO;

  private RepositoryDAO repoDAO;

  private OwnerDAO ownerDAO;

  public ContextResolver() {
    // no-arg constructor - see javadoc on #injectDAOs
  }

  /**
   * Injected using Guice <a href="https://github.com/google/guice/wiki/Injections#method-injection">method
   * injection</a> as this is a dependency of Shiro {@link org.apache.shiro.aop.MethodInterceptor} using AOP. See setup
   * in {@link SecurityAopModule} using `requestInjection`.
   */
  @Inject
  public void injectDAOs(
      final ApplicationDAO appDAO,
      final OrganizationDAO orgDAO,
      final RepositoryManagerDAO repoManagerDAO,
      final RepositoryDAO repoDAO,
      final OwnerDAO ownerDAO)
  {
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.repoManagerDAO = repoManagerDAO;
    this.repoDAO = repoDAO;
    this.ownerDAO = ownerDAO;
  }

  @VisibleForTesting
  ContextResolver(
      final ApplicationDAO appDAO,
      final OrganizationDAO orgDAO,
      final RepositoryManagerDAO repoManagerDAO,
      final RepositoryDAO repoDAO,
      final OwnerDAO ownerDAO)
  {
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.repoManagerDAO = repoManagerDAO;
    this.repoDAO = repoDAO;
    this.ownerDAO = ownerDAO;
  }

  private final ContextIdResolver<Application> resolverForInboundApplication = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      // load current entity state and disregard any potential updates expressed in user-supplied data
      app = appDAO.getByIdNotNull(app.getId());
      return resolverForApplication.resolveContextIds(app);
    }
  };

  final ContextIdResolver<Application> resolverForApplication = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      return resolveContextIdsForOwner(app);
    }
  };

  private final ContextIdResolver<String> resolverForApplicationId = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(String appId) {
      Application app = appDAO.getByIdNotNull(appId);
      return resolverForApplication.resolveContextIds(app);
    }
  };

  private final ContextIdResolver<String> resolverForApplicationPublicId = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(String appPublicId) {
      Application app = appDAO.getByPublicIdNotNull(appPublicId);
      return resolverForApplication.resolveContextIds(app);
    }
  };

  private final ContextIdResolver<Application> resolverForApplicationOwner = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      return resolveContextIdsForOwner(app.getOrganizationId(), OwnerType.ORGANIZATION);
    }
  };

  private final ContextIdResolver<Organization> resolverForInboundOrganization = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      // load current entity state and disregard any potential updates expressed in user-supplied data
      org = orgDAO.getByIdNotNull(org.getId());
      return resolverForOrganization.resolveContextIds(org);
    }
  };

  final ContextIdResolver<Organization> resolverForOrganization = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      return resolveContextIdsForOwner(org);
    }
  };

  final ContextIdResolver<Owner> resolveForApplicationOrOrganization = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(final Owner owner) {
      return resolveContextIdsForOwner(owner);
    }
  };

  private final ContextIdResolver<String> resolverForOrganizationId = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(String orgId) {
      Organization org = orgDAO.getByIdNotNull(orgId);
      return resolverForOrganization.resolveContextIds(org);
    }
  };

  private final ContextIdResolver<Organization> resolverForOrganizationOwner = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      String parentId = org.getParentOrganizationId();
      if (parentId == null) {
        parentId = Organization.ROOT_ORGANIZATION_ID;
      }
      return resolveContextIdsForOwner(parentId, OwnerType.ORGANIZATION);
    }
  };

  final ContextIdResolver<Repository> resolverForRepository = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(Repository repository) {
      if (repository.getId() != null) {
        // Existing repository
        return resolveContextIdsForOwner(repository);
      }
      else {
        // New repository
        if (repository.getRepositoryManagerId() != null) {
          // Existing repository manager
          return resolveContextIdsForOwner(repository.getRepositoryManagerId(), OwnerType.REPOSITORY_MANAGER);
        }
        else {
          // New repository manager
          return resolveContextIdsForOwner(RepositoryContainer.SINGLETON);
        }
      }
    }
  };

  private final ContextIdResolver<String> resolverForRepositoryId = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(String repositoryId) {
      Repository repository = repoDAO.getByIdNotNull(repositoryId);
      return resolverForRepository.resolveContextIds(repository);
    }
  };

  final ContextIdResolver<RepositoryManager> resolverForRepositoryManager = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(RepositoryManager repositoryManager) {
      if (repositoryManager.getId() != null) {
        // Existing repositoryManager
        return resolveContextIdsForOwner(repositoryManager);
      }
      else {
        // New repositoryManager
        return resolveContextIdsForOwner(RepositoryContainer.SINGLETON);
      }
    }
  };

  private final ContextIdResolver<String> resolverForRepositoryManagerId = new ContextIdResolver<>()
  {
    @Override
    public Iterable<String> resolveContextIds(String repositoryManagerId) {
      RepositoryManager repositoryManager = repoManagerDAO.getByIdNotNull(repositoryManagerId);
      return resolverForRepositoryManager.resolveContextIds(repositoryManager);
    }
  };

  private static Function<Owner, String> PARENT_ID_FUNCTION = new Function<>()
  {
    @Override
    public String apply(Owner owner) {
      String parentId = owner.getParentOwnerId();
      if (parentId == null) {
        parentId = MembershipMapping.GLOBAL_CONTEXT_ID;
      }
      return parentId;
    }
  };

  /**
   * More efficient variant of {@link #resolveContextIdsForOwner(String)} that avoids superfluous entity lookups when we
   * already have the entity in memory. Note that avoiding unnecessary entity lookups from the DB is the very reason
   * {@link ContextIdResolver#resolveContextIds(Object)} returns just an {@code Iterable} as opposed to
   * {@code Collection}, it enables lazy-loading and is critical to the performance.
   */
  private Iterable<String> resolveContextIdsForOwner(final Owner owner) {
    if (owner.getParentOwnerId() == null) {
      return Arrays.asList(owner.getId(), MembershipMapping.GLOBAL_CONTEXT_ID);
    }
    return Iterables.concat(
        Arrays.asList(owner.getId(), owner.getParentOwnerId()),
        Iterables.transform(
            ownerDAO.walkHierarchy(owner.getParentOwnerId(), owner.getType().getParentType()),
            PARENT_ID_FUNCTION));
  }

  private Iterable<String> resolveContextIdsForOwner(final String ownerId, OwnerType ownerType) {
    // Get an Iterable<Owner> and transform it to Iterable<owner Id>
    Iterable<String> ownerIdIterable = Iterables.transform(ownerDAO.walkHierarchy(ownerId, ownerType),
        new Function<Owner, String>()
        {
          @Override
          public String apply(Owner owner) {
            return owner.getId();
          }
        });
    return Iterables.concat(ownerIdIterable, GLOBAL_CONTEXT);
  }

  /**
   * Resolves the internal ids of the contexts to check for authorization from the given method parameters (keyed by
   * {@link AuthzContext#value()}).
   */
  public Iterable<String> resolveContextIds(Map<AuthzContext.Key, Object> parameters) {
    int count = parameters.size();
    if (parameters.isEmpty()) {
      return GLOBAL_CONTEXT;
    }
    if (count == 1) {
      switch (parameters.keySet().iterator().next()) {
        case APPLICATION:
          Application app = get(parameters, AuthzContext.Key.APPLICATION, Application.class);
          return resolverForInboundApplication.resolveContextIds(app);
        case APPLICATION_OWNER:
          app = get(parameters, AuthzContext.Key.APPLICATION_OWNER, Application.class);
          return resolverForApplicationOwner.resolveContextIds(app);
        case APPLICATION_ID:
          String appId = get(parameters, AuthzContext.Key.APPLICATION_ID, String.class);
          return resolverForApplicationId.resolveContextIds(appId);
        case APPLICATION_PUBLIC_ID:
          String appPublicId = get(parameters, AuthzContext.Key.APPLICATION_PUBLIC_ID, String.class);
          return resolverForApplicationPublicId.resolveContextIds(appPublicId);
        case ORGANIZATION:
          Organization org = get(parameters, AuthzContext.Key.ORGANIZATION, Organization.class);
          return resolverForInboundOrganization.resolveContextIds(org);
        case ORGANIZATION_OWNER:
          org = get(parameters, AuthzContext.Key.ORGANIZATION_OWNER, Organization.class);
          return resolverForOrganizationOwner.resolveContextIds(org);
        case ORGANIZATION_ID:
          String orgId = get(parameters, AuthzContext.Key.ORGANIZATION_ID, String.class);
          return resolverForOrganizationId.resolveContextIds(orgId);
        case REPOSITORY_MANAGER_ID:
          String repositoryManagerId = get(parameters, AuthzContext.Key.REPOSITORY_MANAGER_ID, String.class);
          return resolverForRepositoryManagerId.resolveContextIds(repositoryManagerId);
        case REPOSITORY_MANAGER:
          RepositoryManager repositoryManager =
              get(parameters, AuthzContext.Key.REPOSITORY_MANAGER, RepositoryManager.class);
          return resolverForRepositoryManager.resolveContextIds(repositoryManager);
        case REPOSITORY_ID:
          String repositoryId = get(parameters, AuthzContext.Key.REPOSITORY_ID, String.class);
          return resolverForRepositoryId.resolveContextIds(repositoryId);
        case REPOSITORY:
          Repository repository = get(parameters, AuthzContext.Key.REPOSITORY, Repository.class);
          return resolverForRepository.resolveContextIds(repository);
        case OWNER:
          Owner owner = get(parameters, AuthzContext.Key.OWNER, Owner.class);
          return resolveContextIdsForOwner(owner);
        default:
          throw new IllegalArgumentException("Cannot resolve context from " + parameters);
      }
    }
    else if (count == 2) {
      OwnerType type = get(parameters, AuthzContext.Key.TYPE, OwnerType.class);
      switch (type) {
        case APPLICATION:
          if (parameters.get(Key.ID) != null) {
            String id = get(parameters, Key.ID, String.class);
            return resolverForApplicationPublicId.resolveContextIds(id);
          }
          else {
            String id = get(parameters, Key.INTERNAL_ID, String.class);
            return resolverForApplicationId.resolveContextIds(id);
          }
        case ORGANIZATION:
          String id;
          if (parameters.get(Key.ID) != null) {
            id = get(parameters, Key.ID, String.class);
          }
          else {
            id = get(parameters, Key.INTERNAL_ID, String.class);
          }
          return resolverForOrganizationId.resolveContextIds(id);
        case REPOSITORY_MANAGER:
          String repositoryManagerId;
          if (parameters.get(Key.INTERNAL_ID) != null) {
            repositoryManagerId = get(parameters, Key.INTERNAL_ID, String.class);
          }
          else {
            repositoryManagerId = get(parameters, Key.ID, String.class);
          }
          return resolverForRepositoryManagerId.resolveContextIds(repositoryManagerId);
        case REPOSITORY:
          String repositoryId;
          if (parameters.get(Key.INTERNAL_ID) != null) {
            repositoryId = get(parameters, Key.INTERNAL_ID, String.class);
          }
          else {
            repositoryId = get(parameters, Key.ID, String.class);
          }
          return resolverForRepositoryId.resolveContextIds(repositoryId);
        case GLOBAL:
          return GLOBAL_CONTEXT;
        case REPOSITORY_CONTAINER:
          return resolveContextIdsForOwner(RepositoryContainer.SINGLETON);
        default:
          throw new IllegalArgumentException("Unknown owner type " + type);
      }
    }
    throw new IllegalArgumentException("Cannot resolve context from " + parameters);
  }

  private <T> T get(Map<AuthzContext.Key, Object> parameters, AuthzContext.Key key, Class<T> type) {
    Object value = parameters.get(key);
    try {
      Objects.requireNonNull(value, "Expected parameter " + key);
      return type.cast(value);
    }
    catch (RuntimeException e) {
      throw new IllegalArgumentException("Cannot resolve context from " + parameters, e);
    }
  }
}

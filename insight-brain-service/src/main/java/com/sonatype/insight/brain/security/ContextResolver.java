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

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.AuthzContext.Key;

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

  private static final ApplicationDAO appDAO = new ApplicationDAO();

  private static final OrganizationDAO orgDAO = new OrganizationDAO();

  private static final RepositoryDAO repoDAO = new RepositoryDAO();

  private static final OwnerDAO ownerDAO = new OwnerDAO();

  private final ContextIdResolver<Application> inboundApplication = new ContextIdResolver<Application>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      // load current entity state and disregard any potential updates expressed in user-supplied data
      app = appDAO.getByIdNotNull(app.getId());
      return application.resolveContextIds(app);
    }
  };

  final ContextIdResolver<Application> application = new ContextIdResolver<Application>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      return resolveContextIdsForOwner(app);
    }
  };

  private final ContextIdResolver<String> applicationId = new ContextIdResolver<String>()
  {
    @Override
    public Iterable<String> resolveContextIds(String appId) {
      Application app = appDAO.getByIdNotNull(appId);
      return application.resolveContextIds(app);
    }
  };

  private final ContextIdResolver<String> applicationPublicId = new ContextIdResolver<String>()
  {
    @Override
    public Iterable<String> resolveContextIds(String appPublicId) {
      Application app = appDAO.getByPublicIdNotNull(appPublicId);
      return application.resolveContextIds(app);
    }
  };

  private final ContextIdResolver<Application> applicationOwner = new ContextIdResolver<Application>()
  {
    @Override
    public Iterable<String> resolveContextIds(Application app) {
      return resolveContextIdsForOwner(app.getOrganizationId());
    }
  };

  private final ContextIdResolver<Organization> inboundOrganization = new ContextIdResolver<Organization>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      // load current entity state and disregard any potential updates expressed in user-supplied data
      org = orgDAO.getByIdNotNull(org.getId());
      return organization.resolveContextIds(org);
    }
  };

  final ContextIdResolver<Organization> organization = new ContextIdResolver<Organization>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      return resolveContextIdsForOwner(org);
    }
  };

  private final ContextIdResolver<String> organizationId = new ContextIdResolver<String>()
  {
    @Override
    public Iterable<String> resolveContextIds(String orgId) {
      Organization org = orgDAO.getByIdNotNull(orgId);
      return organization.resolveContextIds(org);
    }
  };

  private final ContextIdResolver<Organization> organizationOwner = new ContextIdResolver<Organization>()
  {
    @Override
    public Iterable<String> resolveContextIds(Organization org) {
      String parentId = org.getParentOrganizationId();
      if (parentId == null) {
        parentId = Organization.ROOT_ORGANIZATION_ID;
      }
      return resolveContextIdsForOwner(parentId);
    }
  };

  final ContextIdResolver<Repository> repository = new ContextIdResolver<Repository>()
  {
    @Override
    public Iterable<String> resolveContextIds(Repository repository) {
      if (repository.getId() != null) {
        // Existing repository
        return resolveContextIdsForOwner(repository);
      }
      else {
        // New repository
        return resolveContextIdsForOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      }
    }
  };

  private final ContextIdResolver<String> repositoryId = new ContextIdResolver<String>()
  {
    @Override
    public Iterable<String> resolveContextIds(String repositoryId) {
      Repository repository = repoDAO.getByIdNotNull(repositoryId);
      return ContextResolver.this.repository.resolveContextIds(repository);
    }
  };

  private static Function<Owner, String> PARENT_ID_FUNCTION = new Function<Owner, String>()
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
    return Iterables.concat(Arrays.asList(owner.getId(), owner.getParentOwnerId()),
        Iterables.transform(ownerDAO.walkHierarchy(owner.getParentOwnerId()), PARENT_ID_FUNCTION));
  }

  private Iterable<String> resolveContextIdsForOwner(final String ownerId) {
    // Get an Iterable<Owner> and transform it to Iterable<owner Id>
    Iterable<String> ownerIdIterable = Iterables.transform(ownerDAO.walkHierarchy(ownerId),
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
          return inboundApplication.resolveContextIds(app);
        case APPLICATION_OWNER:
          app = get(parameters, AuthzContext.Key.APPLICATION_OWNER, Application.class);
          return applicationOwner.resolveContextIds(app);
        case APPLICATION_ID:
          String appId = get(parameters, AuthzContext.Key.APPLICATION_ID, String.class);
          return applicationId.resolveContextIds(appId);
        case APPLICATION_PUBLIC_ID:
          String appPublicId = get(parameters, AuthzContext.Key.APPLICATION_PUBLIC_ID, String.class);
          return applicationPublicId.resolveContextIds(appPublicId);
        case ORGANIZATION:
          Organization org = get(parameters, AuthzContext.Key.ORGANIZATION, Organization.class);
          return inboundOrganization.resolveContextIds(org);
        case ORGANIZATION_OWNER:
          org = get(parameters, AuthzContext.Key.ORGANIZATION_OWNER, Organization.class);
          return organizationOwner.resolveContextIds(org);
        case ORGANIZATION_ID:
          String orgId = get(parameters, AuthzContext.Key.ORGANIZATION_ID, String.class);
          return organizationId.resolveContextIds(orgId);
        case REPOSITORY_ID:
          String repositoryId = get(parameters, AuthzContext.Key.REPOSITORY_ID, String.class);
          return this.repositoryId.resolveContextIds(repositoryId);
        case REPOSITORY:
          Repository repository = get(parameters, AuthzContext.Key.REPOSITORY, Repository.class);
          return this.repository.resolveContextIds(repository);
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
            return applicationPublicId.resolveContextIds(id);
          }
          else {
            String id = get(parameters, Key.INTERNAL_ID, String.class);
            return applicationId.resolveContextIds(id);
          }
        case ORGANIZATION:
          String id;
          if (parameters.get(Key.ID) != null) {
            id = get(parameters, Key.ID, String.class);
          }
          else {
            id = get(parameters, Key.INTERNAL_ID, String.class);
          }
          return organizationId.resolveContextIds(id);
        case REPOSITORY:
          return repositoryId.resolveContextIds(get(parameters, Key.ID, String.class));
        case GLOBAL:
          return GLOBAL_CONTEXT;
        case REPOSITORY_CONTAINER:
          return resolveContextIdsForOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID);
        default:
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

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.audit.AuditContainerRequestFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import jakarta.annotation.Priority;
import jakarta.inject.Provider;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;

/**
 * Spring-managed subclass of {@link AuditContainerRequestFilter} that bridges
 * the JAX-RS {@code @Context}-injected {@link ResourceInfo} into the filter's
 * constructor-injected provider.
 *
 * <p>
 * Shared between single-tenant {@link JerseyConfiguration} and
 * multi-tenant {@code MtiqJerseyConfiguration} to avoid duplication.
 */
@Priority(AuditContainerRequestFilter.PRIORITY)
public final class SpringManagedAuditContainerRequestFilter
    extends AuditContainerRequestFilter
{
  private final ResourceInfoProviderHolder resourceInfoProviderHolder;

  public SpringManagedAuditContainerRequestFilter(
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      RepositoryDAO repositoryDAO,
      RepositoryManagerDAO repositoryManagerDAO)
  {
    this(applicationDAO, organizationDAO, repositoryDAO, repositoryManagerDAO, new ResourceInfoProviderHolder());
  }

  SpringManagedAuditContainerRequestFilter(
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      RepositoryDAO repositoryDAO,
      RepositoryManagerDAO repositoryManagerDAO,
      ResourceInfoProviderHolder resourceInfoProviderHolder)
  {
    super(applicationDAO, organizationDAO, repositoryDAO, repositoryManagerDAO, resourceInfoProviderHolder);
    this.resourceInfoProviderHolder = resourceInfoProviderHolder;
  }

  @Context
  public void setResourceInfoProvider(Provider<ResourceInfo> resourceInfoProvider) {
    resourceInfoProviderHolder.setDelegate(resourceInfoProvider);
  }
}

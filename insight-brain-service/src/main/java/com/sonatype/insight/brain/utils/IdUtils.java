/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;

@Named
@Singleton
public class IdUtils
{
  static final String MSG_PREFIX_NO_OWNER_INSTANCE = "There is no Owner instance for OwnerType: ";

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public IdUtils(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  public Owner getOwnerNotNull(final OwnerType ownerType, final String ownerId) {
    switch (ownerType) {
      case APPLICATION:
        Application application = applicationDAO.getByPublicId(ownerId);
        if (application != null) {
          return application;
        }
        return applicationDAO.getByIdNotNull(ownerId);
      case ORGANIZATION:
        return organizationDAO.getByIdNotNull(ownerId);
      case REPOSITORY:
        return repositoryDAO.getByIdNotNull(ownerId);
      case REPOSITORY_MANAGER:
        return repositoryManagerDAO.getByIdNotNull(ownerId);
      case REPOSITORY_CONTAINER:
        return RepositoryContainer.SINGLETON;
      case HOSTED_REPOSITORY_COMPONENT:
        return hostedRepositoryComponentDAO.getByIdNotNull(ownerId);
      case GLOBAL:
        throw new IllegalArgumentException(MSG_PREFIX_NO_OWNER_INSTANCE + ownerType);
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }

  public String getInternalOwnerId(OwnerType ownerType, String ownerId) {
    switch (ownerType) {
      case APPLICATION:
        Application application = applicationDAO.getByPublicId(ownerId);
        if (application != null) {
          return application.getId();
        }
        return applicationDAO.getByIdNotNull(ownerId).getId();
      case ORGANIZATION:
        return organizationDAO.getByIdNotNull(ownerId).getId();
      case REPOSITORY:
        return repositoryDAO.getByIdNotNull(ownerId).getId();
      case REPOSITORY_MANAGER:
        return repositoryManagerDAO.getByIdNotNull(ownerId).getId();
      case REPOSITORY_CONTAINER:
        return RepositoryContainer.REPOSITORY_CONTAINER_ID;
      case HOSTED_REPOSITORY_COMPONENT:
        return hostedRepositoryComponentDAO.getByIdNotNull(ownerId).getId();
      case GLOBAL:
        return MembershipMapping.GLOBAL_CONTEXT_ID;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }

  public String getPublicOwnerId(OwnerType ownerType, String ownerId) {
    switch (ownerType) {
      case APPLICATION:
        Application application = applicationDAO.getByPublicId(ownerId);
        if (application != null) {
          return application.getPublicId();
        }
        return applicationDAO.getByIdNotNull(ownerId).getPublicId();
      case ORGANIZATION:
        return organizationDAO.getByIdNotNull(ownerId).getPublicId();
      case REPOSITORY:
        return repositoryDAO.getByIdNotNull(ownerId).getPublicId();
      case REPOSITORY_MANAGER:
        return repositoryManagerDAO.getByIdNotNull(ownerId).getPublicId();
      case REPOSITORY_CONTAINER:
        return RepositoryContainer.REPOSITORY_CONTAINER_ID;
      case HOSTED_REPOSITORY_COMPONENT:
        return hostedRepositoryComponentDAO.getByIdNotNull(ownerId).getPublicId();
      case GLOBAL:
        return MembershipMapping.GLOBAL_CONTEXT_ID;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }
}

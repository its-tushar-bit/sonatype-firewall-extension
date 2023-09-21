/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;

public class IdUtils
{
  static final String MSG_PREFIX_NO_OWNER_INSTANCE = "There is no Owner instance for OwnerType: ";

  private IdUtils() {
    // Utility class
  }

  public static Owner getOwnerNotNull(final OwnerType ownerType, final String ownerId) {
    switch (ownerType) {
      case APPLICATION:
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByPublicId(ownerId);
        if (application != null) {
          return application;
        }
        return applicationDAO.getByIdNotNull(ownerId);
      case ORGANIZATION:
        return new OrganizationDAO().getByIdNotNull(ownerId);
      case REPOSITORY:
        return new RepositoryDAO().getByIdNotNull(ownerId);
      case REPOSITORY_MANAGER:
        return new RepositoryManagerDAO().getByIdNotNull(ownerId);
      case REPOSITORY_CONTAINER:
        return RepositoryContainer.SINGLETON;
      case GLOBAL:
        throw new IllegalArgumentException(MSG_PREFIX_NO_OWNER_INSTANCE + ownerType);
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }

  public static String getInternalOwnerId(OwnerType ownerType, String ownerId) {
    switch (ownerType) {
      case APPLICATION:
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByPublicId(ownerId);
        if (application != null) {
          return application.getId();
        }
        return applicationDAO.getByIdNotNull(ownerId).getId();
      case ORGANIZATION:
        return new OrganizationDAO().getByIdNotNull(ownerId).getId();
      case REPOSITORY:
        return new RepositoryDAO().getByIdNotNull(ownerId).getId();
      case REPOSITORY_MANAGER:
        return new RepositoryManagerDAO().getByIdNotNull(ownerId).getId();
      case REPOSITORY_CONTAINER:
        return RepositoryContainer.REPOSITORY_CONTAINER_ID;
      case GLOBAL:
        return MembershipMapping.GLOBAL_CONTEXT_ID;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }

  public static String getPublicOwnerId(OwnerType ownerType, String ownerId) {
    switch (ownerType) {
      case APPLICATION:
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByPublicId(ownerId);
        if (application != null) {
          return application.getPublicId();
        }
        return applicationDAO.getByIdNotNull(ownerId).getPublicId();
      case ORGANIZATION:
        return new OrganizationDAO().getByIdNotNull(ownerId).getPublicId();
      case REPOSITORY:
        return new RepositoryDAO().getByIdNotNull(ownerId).getPublicId();
      case REPOSITORY_MANAGER:
        return new RepositoryManagerDAO().getByIdNotNull(ownerId).getPublicId();
      case REPOSITORY_CONTAINER:
        return RepositoryContainer.REPOSITORY_CONTAINER_ID;
      case GLOBAL:
        return MembershipMapping.GLOBAL_CONTEXT_ID;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }
}

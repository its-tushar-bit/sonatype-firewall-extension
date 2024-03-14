/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

public final class ScopeOwnerUtils
{
  public static final String SCOPE_OWNER_TYPE_ROOT_ORGANIZATION = "root_organization";

  public static final String SCOPE_OWNER_TYPE_REPOSITORY_CONTAINER = "all_repositories";

  public static String getScopeOwnerType(OwnerType ownerType, String ownerId) {
    String scopeOwnerType;

    switch (ownerId) {
      case Organization.ROOT_ORGANIZATION_ID:
        scopeOwnerType = SCOPE_OWNER_TYPE_ROOT_ORGANIZATION;
        break;
      case RepositoryContainer.REPOSITORY_CONTAINER_ID:
        scopeOwnerType = SCOPE_OWNER_TYPE_REPOSITORY_CONTAINER;
        break;
      default:
        scopeOwnerType = ownerType.toString();
        break;
    }

    return scopeOwnerType;
  }
}

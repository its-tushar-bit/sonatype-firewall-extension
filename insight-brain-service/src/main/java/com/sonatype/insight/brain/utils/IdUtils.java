/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;

public class IdUtils
{

  public static final String TYPE_ORGANIZATION = "organization";

  public static final String TYPE_APPLICATION = "application";

  public static String getInternalOwnerId(String ownerType, String ownerId) {
    if (TYPE_APPLICATION.equals(ownerType)) {
      return new ApplicationDAO().getByPublicIdNotNull(ownerId).getId();
    }
    else if (TYPE_ORGANIZATION.equals(ownerType)) {
      return new OrganizationDAO().getByIdNotNull(ownerId).getId();
    }

    throw new IllegalStateException("Unknown owner type: " + ownerType);
  }

}

/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;

public class IdUtils
{
  public static final String TYPE_GLOBAL = "global";

  public static final String TYPE_ORGANIZATION = "organization";

  public static final String TYPE_APPLICATION = "application";

  public static String getInternalOwnerId(String ownerType, String ownerId) {
    if (TYPE_APPLICATION.equals(ownerType)) {
      return new ApplicationDAO().getByPublicIdNotNull(ownerId).getId();
    }
    else if (TYPE_ORGANIZATION.equals(ownerType)) {
      return new OrganizationDAO().getByIdNotNull(ownerId).getId();
    }
    else if (TYPE_GLOBAL.equals(ownerType)) {
      return MembershipMapping.GLOBAL_CONTEXT_ID;
    }

    throw new IllegalStateException("Unknown owner type: " + ownerType);
  }
}

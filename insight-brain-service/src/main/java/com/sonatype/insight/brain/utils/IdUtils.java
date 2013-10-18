/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
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

  /**
   * Gets the internal owner/context id for the given application/organization and all its parents.
   * 
   * @since 1.7
   */
  public static List<String> getInternalOwnerIds(String ownerType, String ownerId) {
    List<String> ids = new ArrayList<String>();
    if (TYPE_APPLICATION.equals(ownerType)) {
      Application app = new ApplicationDAO().getByPublicIdNotNull(ownerId);
      ids.add(app.getId());
      if (app.getOrganizationId() != null) {
        ids.add(app.getOrganizationId());
      }
    }
    else if (TYPE_ORGANIZATION.equals(ownerType)) {
      Organization org = new OrganizationDAO().getByIdNotNull(ownerId);
      ids.add(org.getId());
    }
    else if (TYPE_GLOBAL.equals(ownerType)) {
      ids.add(MembershipMapping.GLOBAL_CONTEXT_ID);
    }
    else {
      throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
    return ids;
  }
}

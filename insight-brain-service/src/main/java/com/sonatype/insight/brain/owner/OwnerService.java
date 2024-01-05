/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dto.OwnerHierarchyDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;

import com.google.common.annotations.VisibleForTesting;

/**
 * @since 1.105
 */
@Named
public class OwnerService
{
  private final OwnerDAO ownerDAO;

  private final IdUtils idUtils;

  @Inject
  public OwnerService(OwnerDAO ownerDAO, final IdUtils idUtils) {
    this.ownerDAO = ownerDAO;
    this.idUtils = idUtils;
  }

  @Authorize(permission = Permission.READ)
  public OwnerHierarchyDTO getHierarchyForRead(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    return getHierarchyNoAuth(ownerType, ownerId);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public OwnerHierarchyDTO getHierarchyForLegalReviewer(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    return getHierarchyNoAuth(ownerType, ownerId);
  }

  @VisibleForTesting
  OwnerHierarchyDTO getHierarchyNoAuth(OwnerType ownerType, String ownerId) {
    Owner currentOwner = idUtils.getOwnerNotNull(ownerType, ownerId);
    OwnerHierarchyDTO currentHierarchy = null;
    for (Owner owner : ownerDAO.walkHierarchy(currentOwner)) {
      OwnerHierarchyDTO hierarchy =
          new OwnerHierarchyDTO(owner.getId(), owner.getPublicId(), owner.getName(), owner.getType(), null);
      if (currentHierarchy != null) {
        hierarchy.setChildren(new ArrayList<>());
        hierarchy.getChildren().add(currentHierarchy);
      }
      currentHierarchy = hierarchy;
    }
    return currentHierarchy;
  }
}

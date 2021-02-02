/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.105
 */
@Named
public class OwnerService
{
  private final OwnerDAO ownerDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public OwnerService(OwnerDAO ownerDAO, ApplicationDAO applicationDAO) {
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
  }

  public ApplicableContext getHierarchy(String ownerId) {
    return getHierarchy(getOwnerByIdOrPublicIdNotNull(ownerId));
  }

  private Owner getOwnerByIdOrPublicIdNotNull(String ownerId) {
    Owner owner = ownerDAO.getById(ownerId);
    if (owner == null) {
      owner = applicationDAO.getByPublicId(ownerId);
    }
    if (owner == null) {
      throw new NotFoundException("Could not find an owner with ID " + ownerId + ".");
    }
    return owner;
  }

  @Authorize(permission = Permission.READ)
  ApplicableContext getHierarchy(@AuthzContext(Key.OWNER) Owner currentOwner) {
    ApplicableContext currentApplicableContext = null;
    for (Owner owner : ownerDAO.walkHierarchy(currentOwner)) {
      ApplicableContext applicableContext =
          new ApplicableContext(owner.getPublicId(), owner.getName(), owner.getType());
      if (currentApplicableContext != null) {
        applicableContext.setChildren(new ArrayList<>());
        applicableContext.getChildren().add(currentApplicableContext);
      }
      currentApplicableContext = applicableContext;
    }
    return currentApplicableContext;
  }
}

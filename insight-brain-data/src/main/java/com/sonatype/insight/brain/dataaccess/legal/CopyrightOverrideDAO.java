/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.105
 */
public class CopyrightOverrideDAO
    extends AbstractOperationalSqlDAO<CopyrightOverride>
{
  public List<CopyrightOverride> getByComponentCopyrightId(TransactionContext tx, String componentCopyrightId) {
    String sQuery = "SELECT entity FROM CopyrightOverride entity" + //
        " WHERE entity.componentCopyrightId=?1";
    return getList(tx, sQuery, componentCopyrightId);
  }

  public List<CopyrightOverride> getByComponentCopyrightId(String componentCopyrightId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentCopyrightId(tx, componentCopyrightId);
    }
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    ComponentCopyrightDAO componentCopyrightDAO = new ComponentCopyrightDAO();
    ComponentCopyright componentCopyright =
        componentCopyrightDAO.getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    if (componentCopyright == null) {
      return Collections.emptyList();
    }
    return getByComponentCopyrightId(tx, componentCopyright.getId());
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    OwnerDAO ownerDAO = new OwnerDAO();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<CopyrightOverride> copyrightOverrides =
          getByOwnerIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
      if (!copyrightOverrides.isEmpty()) {
        return copyrightOverrides;
      }
    }
    return Collections.emptyList();
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier);
    }
  }

  @Override
  public void update(TransactionContext tx, CopyrightOverride copyrightOverride) {
    if (getById(tx, copyrightOverride.getId()) == null) {
      throw new BadRequestException(
          "Cannot update copyright override with id " + copyrightOverride.getId() + " because it does not exist.");
    }
    super.update(tx, copyrightOverride);
  }
}

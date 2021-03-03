/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.105
 */
public class ComponentLegalFileDAO
    extends AbstractOperationalSqlDAO<ComponentLegalFile>
{
  @Override
  public ComponentLegalFile getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ComponentLegalFile entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<ComponentLegalFile> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM ComponentLegalFile entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<ComponentLegalFile> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public ComponentLegalFile getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM ComponentLegalFile entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3";
    return get(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public ComponentLegalFile getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public ComponentLegalFile getByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    OwnerDAO ownerDAO = new OwnerDAO();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      ComponentLegalFile componentLegalFile =
          getByOwnerIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
      if (componentLegalFile != null) {
        return componentLegalFile;
      }
    }
    return null;
  }

  public ComponentLegalFile getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier);
    }
  }

  @Override
  public void insert(TransactionContext tx, ComponentLegalFile componentLegalFile) {
    if (getByOwnerIdAndComponentIdentifier(tx, componentLegalFile.getOwnerId(),
        componentLegalFile.getComponentIdentifier()) != null) {
      throw new BadRequestException(
          "Component legal file already exists for owner with id " + componentLegalFile.getOwnerId() +
              " and component " + componentLegalFile.getComponentIdentifier() + ".");
    }
    if (componentLegalFile.getLastUpdatedAt() == null) {
      componentLegalFile.setLastUpdatedAt(new Date());
    }
    super.insert(tx, componentLegalFile);
  }

  @Override
  public void update(TransactionContext tx, ComponentLegalFile componentLegalFile) {
    if (getById(tx, componentLegalFile.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component legal file with id " + componentLegalFile.getId() + " because it does not exist.");
    }
    componentLegalFile.setLastUpdatedAt(new Date());
    super.update(tx, componentLegalFile);
  }

  @Override
  public void delete(TransactionContext tx, ComponentLegalFile componentLegalFile) {
    // Cascade to legal file overrides
    LegalFileOverrideDAO legalFileOverrideDAO = new LegalFileOverrideDAO();
    for (LegalFileOverride legalFileOverride : legalFileOverrideDAO
        .getByComponentLegalFileId(tx, componentLegalFile.getId())) {
      legalFileOverrideDAO.delete(tx, legalFileOverride);
    }
    super.delete(tx, componentLegalFile);
  }
}

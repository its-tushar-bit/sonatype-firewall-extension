/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.105
 */
@Named
@Singleton
public class ComponentLegalFileDAO
    extends AbstractOperationalSqlDAO<ComponentLegalFile>
{
  private final OwnerDAO ownerDAO;

  private final Provider<LegalFileOverrideDAO> legalFileOverrideDAOProvider;

  @Inject
  public ComponentLegalFileDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO,
      final Provider<LegalFileOverrideDAO> legalFileOverrideDAOProvider)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
    this.legalFileOverrideDAOProvider = legalFileOverrideDAOProvider;
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

  public ComponentLegalFile getByOwnerIdAndComponentIdentifierAndType(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType legalFileType)
  {
    String sQuery = "SELECT entity FROM ComponentLegalFile entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3" + //
        " AND entity.type=?4";
    return get(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()), legalFileType);
  }

  public ComponentLegalFile getByOwnerIdAndComponentIdentifierAndType(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType legalFileType)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndType(tx, ownerId, componentIdentifier, legalFileType);
    }
  }

  public ComponentLegalFile getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType legalFileType)
  {
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      ComponentLegalFile componentLegalFile =
          getByOwnerIdAndComponentIdentifierAndType(tx, owner.getId(), componentIdentifier, legalFileType);
      if (componentLegalFile != null) {
        return componentLegalFile;
      }
    }
    return null;
  }

  public ComponentLegalFile getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType legalFileType)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(tx, ownerId, componentIdentifier, legalFileType);
    }
  }

  @Override
  public void insert(TransactionContext tx, ComponentLegalFile componentLegalFile) {
    if (getByOwnerIdAndComponentIdentifierAndType(tx, componentLegalFile.getOwnerId(),
        componentLegalFile.getComponentIdentifier(), componentLegalFile.getType()) != null)
    {
      throw new BadRequestException(
          "Component legal file already exists for owner with id " + componentLegalFile.getOwnerId() +
              " and component " + componentLegalFile.getComponentIdentifier() +
              " and type " + componentLegalFile.getType() + ".");
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
    LegalFileOverrideDAO legalFileOverrideDAO = legalFileOverrideDAOProvider.get();
    for (LegalFileOverride legalFileOverride : legalFileOverrideDAO
        .getByComponentLegalFileId(tx, componentLegalFile.getId()))
    {
      legalFileOverrideDAO.delete(tx, legalFileOverride);
    }
    super.delete(tx, componentLegalFile);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
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
public class LegalFileOverrideDAO
    extends AbstractOperationalSqlDAO<LegalFileOverride>
{
  private final ComponentLegalFileDAO componentLegalFileDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public LegalFileOverrideDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO,
      final ComponentLegalFileDAO componentLegalFileDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
    this.componentLegalFileDAO = componentLegalFileDAO;
  }

  public List<LegalFileOverride> getByComponentLegalFileId(TransactionContext tx, String componentLegalFileId) {
    String sQuery = "SELECT entity FROM LegalFileOverride entity" + //
        " WHERE entity.componentLegalFileId=?1";
    return getList(tx, sQuery, componentLegalFileId);
  }

  public List<LegalFileOverride> getByComponentLegalFileId(String componentLegalFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentLegalFileId(tx, componentLegalFileId);
    }
  }

  public List<LegalFileOverride> getByOwnerIdAndComponentIdentifierAndType(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType type)
  {
    ComponentLegalFile componentLegalFile =
        componentLegalFileDAO.getByOwnerIdAndComponentIdentifierAndType(tx, ownerId, componentIdentifier, type);
    if (componentLegalFile == null) {
      return Collections.emptyList();
    }
    return getByComponentLegalFileId(tx, componentLegalFile.getId());
  }

  public List<LegalFileOverride> getByOwnerIdAndComponentIdentifierAndType(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType type)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndType(tx, ownerId, componentIdentifier, type);
    }
  }

  public List<LegalFileOverride> getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType type)
  {
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<LegalFileOverride> legalFileOverrides =
          getByOwnerIdAndComponentIdentifierAndType(tx, owner.getId(), componentIdentifier, type);
      if (!legalFileOverrides.isEmpty()) {
        return legalFileOverrides;
      }
    }
    return Collections.emptyList();
  }

  public List<LegalFileOverride> getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType type)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(tx, ownerId, componentIdentifier, type);
    }
  }

  @Override
  public void update(TransactionContext tx, LegalFileOverride legalFileOverride) {
    if (getById(tx, legalFileOverride.getId()) == null) {
      throw new BadRequestException(
          "Cannot update legal file override with id " + legalFileOverride.getId() + " because it does not exist.");
    }
    super.update(tx, legalFileOverride);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentLegalFile.COMPONENT_LEGAL_FILE;

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
    return tx.dsl()
        .selectFrom(COMPONENT_LEGAL_FILE)
        .where(COMPONENT_LEGAL_FILE.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
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
    var query = tx.dsl()
        .selectFrom(COMPONENT_LEGAL_FILE)
        .where(COMPONENT_LEGAL_FILE.OWNER_ID.eq(ownerId))
        .and(COMPONENT_LEGAL_FILE.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_LEGAL_FILE.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())));
    if (legalFileType != null) {
      query = query.and(COMPONENT_LEGAL_FILE.TYPE.eq(legalFileType.name()));
    }
    return query.fetchOne(this::toEntity);
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
  public int insert(TransactionContext tx, ComponentLegalFile componentLegalFile) {
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
    if (componentLegalFile.getId() == null) {
      componentLegalFile.setId(UUID.randomUUID().toString());
    }
    return super.insert(tx, componentLegalFile);
  }

  @Override
  public int update(TransactionContext tx, ComponentLegalFile componentLegalFile) {
    if (getById(tx, componentLegalFile.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component legal file with id " + componentLegalFile.getId() + " because it does not exist.");
    }
    componentLegalFile.setLastUpdatedAt(new Date());
    return super.update(tx, componentLegalFile);
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

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_LEGAL_FILE;
  }

  @Override
  public Class<ComponentLegalFile> getEntityClass() {
    return ComponentLegalFile.class;
  }
}

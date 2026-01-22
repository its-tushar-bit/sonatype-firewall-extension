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
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.105
 */
@Named
@Singleton
public class ComponentCopyrightDAO
    extends AbstractOperationalSqlDAO<ComponentCopyright>
{
  private final OwnerDAO ownerDAO;

  private final Provider<CopyrightOverrideDAO> copyrightOverrideDAOProvider;

  @Inject
  public ComponentCopyrightDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO,
      final Provider<CopyrightOverrideDAO> copyrightOverrideDAOProvider)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
    this.copyrightOverrideDAOProvider = copyrightOverrideDAOProvider;
  }

  public List<ComponentCopyright> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM ComponentCopyright entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<ComponentCopyright> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public ComponentCopyright getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM ComponentCopyright entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3";
    return get(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public ComponentCopyright getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public ComponentCopyright getByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      ComponentCopyright componentCopyright =
          getByOwnerIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
      if (componentCopyright != null) {
        return componentCopyright;
      }
    }
    return null;
  }

  public ComponentCopyright getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier);
    }
  }

  @Override
  public void insert(TransactionContext tx, ComponentCopyright componentCopyright) {
    if (getByOwnerIdAndComponentIdentifier(tx, componentCopyright.getOwnerId(),
        componentCopyright.getComponentIdentifier()) != null) {
      throw new BadRequestException(
          "Component copyright already exists for owner with id " + componentCopyright.getOwnerId() +
              " and component " + componentCopyright.getComponentIdentifier() + ".");
    }
    if (componentCopyright.getLastUpdatedAt() == null) {
      componentCopyright.setLastUpdatedAt(new Date());
    }
    super.insert(tx, componentCopyright);
  }

  @Override
  public void update(TransactionContext tx, ComponentCopyright componentCopyright) {
    if (getById(tx, componentCopyright.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component copyright with id " + componentCopyright.getId() + " because it does not exist.");
    }
    componentCopyright.setLastUpdatedAt(new Date());
    super.update(tx, componentCopyright);
  }

  @Override
  public void delete(TransactionContext tx, ComponentCopyright componentCopyright) {
    // Cascade to copyright overrides
    CopyrightOverrideDAO copyrightOverrideDAO = copyrightOverrideDAOProvider.get();
    for (CopyrightOverride copyrightOverride : copyrightOverrideDAO
        .getByComponentCopyrightId(tx, componentCopyright.getId())) {
      copyrightOverrideDAO.delete(tx, copyrightOverride);
    }
    super.delete(tx, componentCopyright);
  }
}

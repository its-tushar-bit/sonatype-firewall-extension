/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.133
 */
@Named
@Singleton
public class ComponentSourceLinkDAO
    extends AbstractOperationalSqlDAO<ComponentSourceLink>
{
  private final Provider<SourceLinkOverrideDAO> sourceLinkOverrideDAOProvider;

  @Inject
  public ComponentSourceLinkDAO(
      final OperationalDataStore operationalDataStore,
      final Provider<SourceLinkOverrideDAO> sourceLinkOverrideDAOProvider)
  {
    super(operationalDataStore);
    this.sourceLinkOverrideDAOProvider = sourceLinkOverrideDAOProvider;
  }

  public ComponentSourceLink getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM ComponentSourceLink entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3";
    return get(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public ComponentSourceLink getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  @Override
  public void insert(TransactionContext tx, ComponentSourceLink componentSourceLink) {
    if (getByOwnerIdAndComponentIdentifier(tx, componentSourceLink.getOwnerId(),
        componentSourceLink.getComponentIdentifier()) != null) {
      throw new BadRequestException(
          "Component source link already exists for owner with id " + componentSourceLink.getOwnerId()
              + " and component " + componentSourceLink.getComponentIdentifier() + ".");
    }
    if (componentSourceLink.getLastUpdatedAt() == null) {
      componentSourceLink.setLastUpdatedAt(new Date());
    }
    super.insert(tx, componentSourceLink);
  }

  @Override
  public void update(TransactionContext tx, ComponentSourceLink componentSourceLink) {
    if (getById(tx, componentSourceLink.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component source link with id " + componentSourceLink.getId() + " because it does not exist.");
    }
    componentSourceLink.setLastUpdatedAt(new Date());
    super.update(tx, componentSourceLink);
  }

  @Override
  public void delete(TransactionContext tx, ComponentSourceLink componentSourceLink) {
    // Cascade to Source Link overrides
    SourceLinkOverrideDAO sourceLinkOverrideDAO = sourceLinkOverrideDAOProvider.get();
    for (SourceLinkOverride sourceLinkOverride : sourceLinkOverrideDAO
        .getByComponentSourceLinkId(tx, componentSourceLink.getId())) {
      sourceLinkOverrideDAO.delete(tx, sourceLinkOverride);
    }
    super.delete(tx, componentSourceLink);
  }
}

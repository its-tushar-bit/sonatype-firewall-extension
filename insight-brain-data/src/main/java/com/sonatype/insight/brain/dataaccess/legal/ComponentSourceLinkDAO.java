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

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentSourceLink.COMPONENT_SOURCE_LINK;

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
    return super.toEntity(tx.dsl()
        .selectFrom(COMPONENT_SOURCE_LINK)
        .where(COMPONENT_SOURCE_LINK.OWNER_ID.eq(ownerId))
        .and(COMPONENT_SOURCE_LINK.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_SOURCE_LINK.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetchOne());
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
        componentSourceLink.getComponentIdentifier()) != null)
    {
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
        .getByComponentSourceLinkId(tx, componentSourceLink.getId()))
    {
      sourceLinkOverrideDAO.delete(tx, sourceLinkOverride);
    }
    tx.dsl()
        .deleteFrom(COMPONENT_SOURCE_LINK)
        .where(COMPONENT_SOURCE_LINK.COMPONENT_SOURCE_LINK_ID.eq(componentSourceLink.getId()))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_SOURCE_LINK;
  }

  @Override
  public Class<ComponentSourceLink> getEntityClass() {
    return ComponentSourceLink.class;
  }
}

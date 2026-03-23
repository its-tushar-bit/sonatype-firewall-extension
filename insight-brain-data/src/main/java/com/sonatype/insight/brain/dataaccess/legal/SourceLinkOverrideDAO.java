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
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceLinkOverride.SOURCE_LINK_OVERRIDE;

/**
 * @since 1.133
 */
@Named
@Singleton
public class SourceLinkOverrideDAO
    extends AbstractOperationalSqlDAO<SourceLinkOverride>
{
  private final ComponentSourceLinkDAO componentSourceLinkDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public SourceLinkOverrideDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO,
      final ComponentSourceLinkDAO componentSourceLinkDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
    this.componentSourceLinkDAO = componentSourceLinkDAO;
  }

  public List<SourceLinkOverride> getByComponentSourceLinkId(TransactionContext tx, String componentSourceLinkId) {
    return tx.dsl()
        .selectFrom(SOURCE_LINK_OVERRIDE)
        .where(SOURCE_LINK_OVERRIDE.COMPONENT_SOURCE_LINK_ID.eq(componentSourceLinkId))
        .fetch(this::toEntity);
  }

  public List<SourceLinkOverride> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    ComponentSourceLink componentSourceLink =
        componentSourceLinkDAO.getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    if (componentSourceLink == null) {
      return Collections.emptyList();
    }
    return getByComponentSourceLinkId(tx, componentSourceLink.getId());
  }

  public List<SourceLinkOverride> getByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<SourceLinkOverride> sourceLinkOverrides =
          getByOwnerIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
      if (!sourceLinkOverrides.isEmpty()) {
        return sourceLinkOverrides;
      }
    }
    return Collections.emptyList();
  }

  public List<SourceLinkOverride> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier);
    }
  }

  @Override
  public void update(TransactionContext tx, SourceLinkOverride entity) {
    if (getById(tx, entity.getId()) == null) {
      throw new BadRequestException(
          "Cannot update source link override with id " + entity.getId() + " because it does not exist.");
    }
    super.update(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_LINK_OVERRIDE;
  }

  @Override
  public Class<SourceLinkOverride> getEntityClass() {
    return SourceLinkOverride.class;
  }
}

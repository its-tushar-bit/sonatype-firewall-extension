/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import org.jooq.Row2;
import org.jooq.Table;
import org.jooq.impl.DSL;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentCopyright.COMPONENT_COPYRIGHT;

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
    return tx.dsl()
        .selectFrom(COMPONENT_COPYRIGHT)
        .where(COMPONENT_COPYRIGHT.OWNER_ID.eq(ownerId))
        .fetchInto(ComponentCopyright.class);
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
    return tx.dsl()
        .selectFrom(COMPONENT_COPYRIGHT)
        .where(COMPONENT_COPYRIGHT.OWNER_ID.eq(ownerId))
        .and(COMPONENT_COPYRIGHT.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_COPYRIGHT.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetchOneInto(ComponentCopyright.class);
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

  /**
   * Batch fetches component copyright records for multiple components at the specified ownerId
   * (no hierarchy resolution).
   *
   * @param ownerId the owner ID to look up directly
   * @param componentIdentifiers the components to fetch copyrights for
   * @return map from ComponentIdentifier to ComponentCopyright; components with no record are not included
   */
  public Map<ComponentIdentifier, ComponentCopyright> batchGetByOwnerIdAndComponentIdentifiers(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      return Collections.emptyMap();
    }

    List<ComponentCopyright> copyrights = getListWithSqlInClause(componentIdentifiers, chunk -> {
      List<Row2<String, String>> componentRows = chunk.stream()
          .map(ComponentIdentifierAdapter::toComponentRow)
          .toList();
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .selectFrom(COMPONENT_COPYRIGHT)
            .where(COMPONENT_COPYRIGHT.OWNER_ID.eq(ownerId))
            .and(DSL.row(COMPONENT_COPYRIGHT.COMPONENT_ID_FORMAT, COMPONENT_COPYRIGHT.COMPONENT_ID_COORDINATES_JSON)
                .in(componentRows))
            .fetchInto(ComponentCopyright.class);
      }
    }, 2, 1);

    Map<ComponentIdentifier, ComponentCopyright> result = new HashMap<>();
    for (ComponentCopyright copyright : copyrights) {
      result.put(copyright.getComponentIdentifier(), copyright);
    }
    return result;
  }

  @Override
  public int insert(TransactionContext tx, ComponentCopyright componentCopyright) {
    if (getByOwnerIdAndComponentIdentifier(tx, componentCopyright.getOwnerId(),
        componentCopyright.getComponentIdentifier()) != null)
    {
      throw new BadRequestException(
          "Component copyright already exists for owner with id " + componentCopyright.getOwnerId() +
              " and component " + componentCopyright.getComponentIdentifier() + ".");
    }
    if (componentCopyright.getLastUpdatedAt() == null) {
      componentCopyright.setLastUpdatedAt(new Date());
    }
    return super.insert(tx, componentCopyright);
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
        .getByComponentCopyrightId(tx, componentCopyright.getId()))
    {
      copyrightOverrideDAO.delete(tx, copyrightOverride);
    }
    tx.dsl()
        .deleteFrom(COMPONENT_COPYRIGHT)
        .where(COMPONENT_COPYRIGHT.COMPONENT_COPYRIGHT_ID.eq(componentCopyright.getId()))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_COPYRIGHT;
  }

  @Override
  public Class<ComponentCopyright> getEntityClass() {
    return ComponentCopyright.class;
  }
}

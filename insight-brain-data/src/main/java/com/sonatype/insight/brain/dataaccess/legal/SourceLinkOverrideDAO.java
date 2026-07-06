/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentSourceLink.COMPONENT_SOURCE_LINK;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
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

  /**
   * Batch fetches source link overrides for multiple components with hierarchy resolution.
   * Returns the overrides from the closest ancestor for each component.
   *
   * @param ownerId the owner (application) ID to resolve hierarchy from
   * @param componentIdentifiers the components to fetch overrides for
   * @return map from ComponentIdentifier to list of SourceLinkOverride; components with no overrides are not included
   */
  public Map<ComponentIdentifier, List<SourceLinkOverride>> batchGetWithHierarchy(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      return Collections.emptyMap();
    }

    var csl = COMPONENT_SOURCE_LINK;
    var slo = SOURCE_LINK_OVERRIDE;
    var oa = OWNER_ANCESTOR;

    List<Record> rows = getListWithSqlInClause(componentIdentifiers, chunk -> {
      List<Row2<String, String>> componentRows = chunk.stream()
          .map(ComponentIdentifierAdapter::toComponentRow)
          .toList();
      try (TransactionContext tx = createTransactionContext()) {
        return new ArrayList<>(tx.dsl()
            .select(
                csl.COMPONENT_ID_FORMAT,
                csl.COMPONENT_ID_COORDINATES_JSON,
                oa.ANCESTOR_DISTANCE,
                slo.SOURCE_LINK_OVERRIDE_ID,
                slo.CONTENT,
                slo.ORIGINAL_CONTENT,
                slo.STATUS,
                slo.COMPONENT_SOURCE_LINK_ID)
            .from(slo)
            .join(csl)
            .on(slo.COMPONENT_SOURCE_LINK_ID.eq(csl.COMPONENT_SOURCE_LINK_ID))
            .join(oa)
            .on(csl.OWNER_ID.eq(oa.ANCESTOR_ID))
            .where(oa.OWNER_ID.eq(ownerId))
            .and(DSL.row(csl.COMPONENT_ID_FORMAT, csl.COMPONENT_ID_COORDINATES_JSON).in(componentRows))
            .fetch());
      }
    }, 2, 1);

    return buildBatchSourceLinkResultMap(rows);
  }

  private Map<ComponentIdentifier, List<SourceLinkOverride>> buildBatchSourceLinkResultMap(
      List<Record> rows)
  {
    Map<ComponentIdentifier, ClosestAncestorAccumulator> closestByComponent = new HashMap<>();

    for (Record row : rows) {
      ComponentIdentifier ci = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(
          row.get(COMPONENT_SOURCE_LINK.COMPONENT_ID_FORMAT),
          row.get(COMPONENT_SOURCE_LINK.COMPONENT_ID_COORDINATES_JSON));
      int distance = row.get(OWNER_ANCESTOR.ANCESTOR_DISTANCE);

      closestByComponent.merge(ci, new ClosestAncestorAccumulator(distance, row),
          (existing, candidate) -> existing.merge(candidate.distance, candidate.rows.getFirst()));
    }

    Map<ComponentIdentifier, List<SourceLinkOverride>> result = new HashMap<>();
    closestByComponent
        .forEach((ci, closest) -> result.put(ci, closest.rows.stream()
            .map(row -> row.into(SourceLinkOverride.class))
            .toList()));
    return result;
  }

  @Override
  public int update(TransactionContext tx, SourceLinkOverride entity) {
    if (getById(tx, entity.getId()) == null) {
      throw new BadRequestException(
          "Cannot update source link override with id " + entity.getId() + " because it does not exist.");
    }
    return super.update(tx, entity);
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

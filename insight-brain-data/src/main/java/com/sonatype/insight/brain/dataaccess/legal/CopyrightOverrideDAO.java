/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentCopyright.COMPONENT_COPYRIGHT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.CopyrightOverride.COPYRIGHT_OVERRIDE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;

/**
 * @since 1.105
 */
@Named
@Singleton
public class CopyrightOverrideDAO
    extends AbstractOperationalSqlDAO<CopyrightOverride>
{
  private final ComponentCopyrightDAO componentCopyrightDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public CopyrightOverrideDAO(
      final OperationalDataStore operationalDataStore,
      final ComponentCopyrightDAO componentCopyrightDAO,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.componentCopyrightDAO = componentCopyrightDAO;
    this.ownerDAO = ownerDAO;
  }

  public List<CopyrightOverride> getByComponentCopyrightId(TransactionContext tx, String componentCopyrightId) {
    return tx.dsl()
        .selectFrom(COPYRIGHT_OVERRIDE)
        .where(COPYRIGHT_OVERRIDE.COMPONENT_COPYRIGHT_ID.eq(componentCopyrightId))
        .fetch(this::toEntity);
  }

  public List<CopyrightOverride> getByComponentCopyrightId(String componentCopyrightId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentCopyrightId(tx, componentCopyrightId);
    }
  }

  public void deleteByComponentCopyrightIds(TransactionContext tx, Collection<String> componentCopyrightIds) {
    if (CollectionUtils.isEmpty(componentCopyrightIds)) {
      return;
    }
    getListWithSqlInClause(componentCopyrightIds, idChunk -> List.of(tx.dsl()
        .deleteFrom(COPYRIGHT_OVERRIDE)
        .where(COPYRIGHT_OVERRIDE.COMPONENT_COPYRIGHT_ID.in(idChunk))
        .execute()), getDataStore());
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    ComponentCopyright componentCopyright =
        componentCopyrightDAO.getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    if (componentCopyright == null) {
      return Collections.emptyList();
    }
    return getByComponentCopyrightId(tx, componentCopyright.getId());
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    var cc = COMPONENT_COPYRIGHT;
    var co = COPYRIGHT_OVERRIDE;
    var oa = OWNER_ANCESTOR;

    List<Field<?>> selectFields = new ArrayList<>(Arrays.asList(co.fields()));
    selectFields.add(oa.ANCESTOR_DISTANCE);

    List<Record> rows = new ArrayList<>(tx.dsl()
        .select(selectFields)
        .from(co)
        .join(cc)
        .on(co.COMPONENT_COPYRIGHT_ID.eq(cc.COMPONENT_COPYRIGHT_ID))
        .join(oa)
        .on(cc.OWNER_ID.eq(oa.ANCESTOR_ID))
        .where(oa.OWNER_ID.eq(ownerId))
        .and(DSL.row(cc.COMPONENT_ID_FORMAT, cc.COMPONENT_ID_COORDINATES_JSON)
            .eq(ComponentIdentifierAdapter.toComponentRow(componentIdentifier)))
        .orderBy(oa.ANCESTOR_DISTANCE)
        .fetch());

    return ClosestAncestorAccumulator.closest(rows, oa.ANCESTOR_DISTANCE)
        .stream()
        .map(row -> row.into(CopyrightOverride.class))
        .toList();
  }

  public List<CopyrightOverride> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier);
    }
  }

  /**
   * Batch fetches copyright overrides for multiple components with hierarchy resolution.
   * Returns the overrides from the closest ancestor for each component.
   *
   * @param ownerId the owner (application) ID to resolve hierarchy from
   * @param componentIdentifiers the components to fetch overrides for
   * @return map from ComponentIdentifier to list of CopyrightOverride; components with no overrides are not included
   */
  public Map<ComponentIdentifier, List<CopyrightOverride>> batchGetWithHierarchy(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      return Collections.emptyMap();
    }

    var cc = COMPONENT_COPYRIGHT;
    var co = COPYRIGHT_OVERRIDE;
    var oa = OWNER_ANCESTOR;

    List<Record> rows = getListWithSqlInClause(componentIdentifiers, chunk -> {
      List<Row2<String, String>> componentRows = chunk.stream()
          .map(ComponentIdentifierAdapter::toComponentRow)
          .toList();
      try (TransactionContext tx = createTransactionContext()) {
        return new ArrayList<>(tx.dsl()
            .select(
                cc.COMPONENT_ID_FORMAT,
                cc.COMPONENT_ID_COORDINATES_JSON,
                oa.ANCESTOR_DISTANCE,
                co.COPYRIGHT_OVERRIDE_ID,
                co.ORIGINAL_CONTENT_HASH,
                co.CONTENT_HASH,
                co.CONTENT,
                co.STATUS,
                co.COMPONENT_COPYRIGHT_ID)
            .from(co)
            .join(cc)
            .on(co.COMPONENT_COPYRIGHT_ID.eq(cc.COMPONENT_COPYRIGHT_ID))
            .join(oa)
            .on(cc.OWNER_ID.eq(oa.ANCESTOR_ID))
            .where(oa.OWNER_ID.eq(ownerId))
            .and(DSL.row(cc.COMPONENT_ID_FORMAT, cc.COMPONENT_ID_COORDINATES_JSON).in(componentRows))
            .fetch());
      }
    }, 2, 1);

    return buildBatchOverrideResultMap(rows);
  }

  private Map<ComponentIdentifier, List<CopyrightOverride>> buildBatchOverrideResultMap(
      List<Record> rows)
  {
    // Single pass: for each component, keep only the rows at the minimum ancestor distance
    Map<ComponentIdentifier, ClosestAncestorAccumulator> closestByComponent = new HashMap<>();

    for (Record row : rows) {
      ComponentIdentifier ci = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(
          row.get(COMPONENT_COPYRIGHT.COMPONENT_ID_FORMAT),
          row.get(COMPONENT_COPYRIGHT.COMPONENT_ID_COORDINATES_JSON));
      int distance = row.get(OWNER_ANCESTOR.ANCESTOR_DISTANCE);

      closestByComponent.merge(ci, new ClosestAncestorAccumulator(distance, row),
          (existing, candidate) -> existing.merge(candidate.distance, candidate.rows.getFirst()));
    }

    Map<ComponentIdentifier, List<CopyrightOverride>> result = new HashMap<>();
    closestByComponent
        .forEach((ci, closest) -> result.put(ci, closest.rows.stream()
            .map(row -> row.into(CopyrightOverride.class))
            .toList()));
    return result;
  }

  @Override
  public int update(TransactionContext tx, CopyrightOverride copyrightOverride) {
    if (getById(tx, copyrightOverride.getId()) == null) {
      throw new BadRequestException(
          "Cannot update copyright override with id " + copyrightOverride.getId() + " because it does not exist.");
    }
    return super.update(tx, copyrightOverride);
  }

  @Override
  public Table<?> getJooqTable() {
    return COPYRIGHT_OVERRIDE;
  }

  @Override
  public Class<CopyrightOverride> getEntityClass() {
    return CopyrightOverride.class;
  }
}

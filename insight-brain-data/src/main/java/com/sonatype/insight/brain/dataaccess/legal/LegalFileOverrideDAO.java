/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;

import org.jooq.Table;

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
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Record;
import org.jooq.Row2;

import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentLegalFile.COMPONENT_LEGAL_FILE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LegalFileOverride.LEGAL_FILE_OVERRIDE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;

/**
 * @since 1.105
 */
@Named
@Singleton
public class LegalFileOverrideDAO
    extends AbstractOperationalSqlDAO<LegalFileOverride>
{
  public record BatchResult(ComponentLegalFile componentLegalFile, List<LegalFileOverride> overrides)
  {
  }

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

  @Override
  public int update(TransactionContext tx, LegalFileOverride legalFileOverride) {
    if (getById(tx, legalFileOverride.getId()) == null) {
      throw new BadRequestException(
          "Cannot update legal file override with id " + legalFileOverride.getId() + " because it does not exist.");
    }
    return super.update(tx, legalFileOverride);
  }

  public List<LegalFileOverride> getByComponentLegalFileId(TransactionContext tx, String componentLegalFileId) {
    return tx.dsl()
        .selectFrom(LEGAL_FILE_OVERRIDE)
        .where(LEGAL_FILE_OVERRIDE.COMPONENT_LEGAL_FILE_ID.eq(componentLegalFileId))
        .fetch(this::toEntity);
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

  /**
   * Fetches overrides for all legal file types in a single query, returning results partitioned by type.
   * Each type is independently resolved to the closest ancestor per component.
   */
  public ImmutableTable<LegalFileType, ComponentIdentifier, BatchResult> batchGetWithHierarchyAllTypes(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      return ImmutableTable.of();
    }

    var clf = COMPONENT_LEGAL_FILE;
    var lfo = LEGAL_FILE_OVERRIDE;
    var oa = OWNER_ANCESTOR;

    List<Record> rows = getListWithSqlInClause(componentIdentifiers, chunk -> {
      List<Row2<String, String>> componentRows = chunk.stream()
          .map(ComponentIdentifierAdapter::toComponentRow)
          .toList();
      try (TransactionContext tx = createTransactionContext()) {
        return new ArrayList<>(tx.dsl()
            .select(
                clf.COMPONENT_LEGAL_FILE_ID,
                clf.COMPONENT_ID_FORMAT,
                clf.COMPONENT_ID_COORDINATES_JSON,
                clf.OWNER_ID,
                clf.TYPE,
                clf.LEGAL_CONTENT_HASH,
                clf.LAST_UPDATED_BY_USERNAME,
                clf.LAST_UPDATED_AT,
                oa.ANCESTOR_DISTANCE,
                lfo.LEGAL_FILE_OVERRIDE_ID,
                lfo.ORIGINAL_CONTENT_HASH,
                lfo.CONTENT_HASH,
                lfo.CONTENT,
                lfo.STATUS,
                lfo.COMPONENT_LEGAL_FILE_ID)
            .from(lfo)
            .join(clf)
            .on(lfo.COMPONENT_LEGAL_FILE_ID.eq(clf.COMPONENT_LEGAL_FILE_ID))
            .join(oa)
            .on(clf.OWNER_ID.eq(oa.ANCESTOR_ID))
            .where(oa.OWNER_ID.eq(ownerId))
            .and(DSL.row(clf.COMPONENT_ID_FORMAT, clf.COMPONENT_ID_COORDINATES_JSON).in(componentRows))
            .fetch());
      }
    }, 2, 1);

    // Resolve closest ancestor per (type, component) pair
    var accumulators =
        HashBasedTable.<LegalFileType, ComponentIdentifier, ClosestAncestorAccumulator>create();
    for (Record row : rows) {
      LegalFileType type = LegalFileType.valueOf(row.get(clf.TYPE));
      ComponentIdentifier ci = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(
          row.get(clf.COMPONENT_ID_FORMAT), row.get(clf.COMPONENT_ID_COORDINATES_JSON));
      int distance = row.get(oa.ANCESTOR_DISTANCE);

      ClosestAncestorAccumulator existing = accumulators.get(type, ci);
      if (existing == null) {
        accumulators.put(type, ci, new ClosestAncestorAccumulator(distance, row));
      }
      else {
        existing.merge(distance, row);
      }
    }

    // Convert accumulators to BatchResults, keyed by type
    ImmutableTable.Builder<LegalFileType, ComponentIdentifier, BatchResult> resultBuilder = ImmutableTable.builder();
    accumulators.cellSet().forEach(cell -> {
      ClosestAncestorAccumulator closest = cell.getValue();
      ComponentLegalFile parent = closest.rows.getFirst().into(ComponentLegalFile.class);
      List<LegalFileOverride> overrides = closest.rows.stream()
          .map(r -> r.into(LegalFileOverride.class))
          .toList();
      resultBuilder.put(cell.getRowKey(), cell.getColumnKey(), new BatchResult(parent, overrides));
    });

    return resultBuilder.build();
  }

  @Override
  public Table<?> getJooqTable() {
    return LEGAL_FILE_OVERRIDE;
  }

  @Override
  public Class<LegalFileOverride> getEntityClass() {
    return LegalFileOverride.class;
  }
}

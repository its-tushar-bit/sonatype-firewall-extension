/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Table;
import org.jooq.impl.DSL;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligation.COMPONENT_OBLIGATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.105
 */
@Named
@Singleton
public class ComponentObligationDAO
    extends AbstractOperationalSqlDAO<ComponentObligation>
{
  /**
   * Bind parameters consumed per (format, coordinates-json) pair when chunking
   * IN-clause queries over component identifiers.
   */
  private static final int COMPONENT_IDENTIFIER_PARAMS_PER_ELEMENT = 2;

  /**
   * Additional bind parameters beyond the chunked rows (the owner id filter).
   */
  private static final int COMPONENT_IDENTIFIER_EXTRA_PARAMS = 1;

  private final OwnerDAO ownerDAO;

  @Inject
  public ComponentObligationDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
  }

  public List<ComponentObligation> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.eq(ownerId))
        .fetch(super::toEntity);
  }

  public List<ComponentObligation> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public ComponentObligation getByOwnerIdAndComponentIdentifierAndObligationName(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String obligationName)
  {
    return super.toEntity(tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .and(COMPONENT_OBLIGATION.OBLIGATION_NAME.eq(obligationName))
        .fetchOne());
  }

  public ComponentObligation getByOwnerIdAndComponentIdentifierAndObligationName(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String obligationName)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationName(tx, ownerId, componentIdentifier, obligationName);
    }
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetch(super::toEntity);
  }

  public List<ComponentObligation> getByOwnerIdsAndComponentIdentifierAndObligationNames(
      TransactionContext tx,
      List<String> ownerIds,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    if (CollectionUtils.isEmpty(obligationNames)) {
      return Collections.emptyList();
    }

    List<ComponentObligation> componentObligationsFromDb = tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.in(ownerIds))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .and(COMPONENT_OBLIGATION.OBLIGATION_NAME.in(obligationNames))
        .fetch(super::toEntity);

    return resolveObligationsForOwnerOrder(ownerIds, componentObligationsFromDb, obligationNames);
  }

  /**
   * Picks the effective obligation row per name using {@code ownerIds} precedence (first owner wins). Callers must pass
   * {@code ownerIds} in the same most-specific-first order as
   * {@link #getByOwnerIdsAndComponentIdentifierAndObligationNames}; a misordered list silently returns wrong results.
   */
  public static List<ComponentObligation> resolveObligationsForOwnerOrder(
      final List<String> ownerIds,
      final List<ComponentObligation> componentObligationsFromDb,
      final Set<String> obligationNames)
  {
    if (CollectionUtils.isEmpty(ownerIds) || CollectionUtils.isEmpty(obligationNames)
        || CollectionUtils.isEmpty(componentObligationsFromDb))
    {
      return Collections.emptyList();
    }
    Map<String, List<ComponentObligation>> ownerIdAndComponentObligation =
        componentObligationsFromDb.stream().collect(groupingBy(ComponentObligation::getOwnerId, toList()));

    Set<String> missingObligations = new HashSet<>(obligationNames);
    List<ComponentObligation> results = new ArrayList<>();
    for (String ownerId : ownerIds) {
      if (!ownerIdAndComponentObligation.containsKey(ownerId)) {
        continue;
      }

      List<ComponentObligation> obligationsFromOwner = ownerIdAndComponentObligation.get(ownerId);
      for (ComponentObligation componentObligation : obligationsFromOwner) {
        if (missingObligations.contains(componentObligation.getObligationName())) {
          results.add(componentObligation);

          missingObligations.remove(componentObligation.getObligationName());
          if (missingObligations.isEmpty()) {
            return results;
          }
        }
      }
    }

    return results;
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    return batchGetWithHierarchy(ownerId, List.of(componentIdentifier))
        .getOrDefault(componentIdentifier, Collections.emptyList());
  }

  public Map<ComponentIdentifier, Set<String>> getAddressedObligationsByOwnerIdWithHierarchy(String ownerId) {
    var co = COMPONENT_OBLIGATION;
    var oa = OWNER_ANCESTOR;

    List<Record> rows;
    try (TransactionContext tx = createTransactionContext()) {
      rows = new ArrayList<>(tx.dsl()
          .select(co.COMPONENT_ID_FORMAT, co.COMPONENT_ID_COORDINATES_JSON, co.OBLIGATION_NAME, co.STATUS,
              oa.ANCESTOR_DISTANCE)
          .from(co)
          .join(oa)
          .on(co.OWNER_ID.eq(oa.ANCESTOR_ID))
          .where(oa.OWNER_ID.eq(ownerId))
          .orderBy(oa.ANCESTOR_DISTANCE)
          .fetch());
    }

    // Rows are ordered nearest-ancestor-first, so the first occurrence of each (component, obligation) is the closest;
    // an obligation counts as addressed only when that closest occurrence is FULFILLED or IGNORED.
    Map<ComponentIdentifier, Set<String>> componentObligationsFound = new HashMap<>();
    Map<ComponentIdentifier, Set<String>> componentObligationsAddressed = new HashMap<>();
    for (Record row : rows) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(
          row.get(co.COMPONENT_ID_FORMAT), row.get(co.COMPONENT_ID_COORDINATES_JSON));
      String obligationName = row.get(co.OBLIGATION_NAME);
      String status = row.get(co.STATUS);
      if (componentObligationsFound.computeIfAbsent(componentIdentifier, key -> new HashSet<>()).add(obligationName)
          && (ObligationStatus.FULFILLED.name().equals(status) || ObligationStatus.IGNORED.name().equals(status)))
      {
        componentObligationsAddressed.computeIfAbsent(componentIdentifier, key -> new HashSet<>())
            .add(obligationName);
      }
    }

    return componentObligationsAddressed;
  }

  /**
   * Batch fetches obligations for multiple components with hierarchy resolution.
   * Obligations accumulate from all hierarchy levels, with the closest ancestor winning per obligation_name.
   *
   * @param ownerId the owner (application) ID to resolve hierarchy from
   * @param componentIdentifiers the components to fetch obligations for
   * @return map from ComponentIdentifier to list of ComponentObligation; components with no obligations are not
   *         included
   */
  public Map<ComponentIdentifier, List<ComponentObligation>> batchGetWithHierarchy(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      return Collections.emptyMap();
    }

    var co = COMPONENT_OBLIGATION;
    var oa = OWNER_ANCESTOR;

    // Fetch all obligations at all hierarchy levels, then keep closest per obligation_name in Java.
    // obligation_name is NOT NULL on this table.
    List<Record> rows = getListWithSqlInClause(componentIdentifiers, chunk -> {
      List<Row2<String, String>> componentRows = chunk.stream()
          .map(ComponentIdentifierAdapter::toComponentRow)
          .toList();
      try (TransactionContext tx = createTransactionContext()) {
        return new ArrayList<>(tx.dsl()
            .select(
                co.COMPONENT_OBLIGATION_ID,
                co.COMPONENT_ID_FORMAT,
                co.COMPONENT_ID_COORDINATES_JSON,
                co.OWNER_ID,
                co.OBLIGATION_NAME,
                co.COMMENT,
                co.STATUS,
                co.LEGAL_CONTENT_HASH,
                co.LAST_UPDATED_BY_USERNAME,
                co.LAST_UPDATED_AT,
                oa.ANCESTOR_DISTANCE)
            .from(co)
            .join(oa)
            .on(co.OWNER_ID.eq(oa.ANCESTOR_ID))
            .where(oa.OWNER_ID.eq(ownerId))
            .and(DSL.row(co.COMPONENT_ID_FORMAT, co.COMPONENT_ID_COORDINATES_JSON).in(componentRows))
            .fetch());
      }
    }, COMPONENT_IDENTIFIER_PARAMS_PER_ELEMENT, COMPONENT_IDENTIFIER_EXTRA_PARAMS);

    // Group by component, then deduplicate by obligation_name keeping the closest ancestor.
    // Unlike LegalFileOverrideDAO (which accumulates all rows at the closest level), obligations are
    // unique per (owner, component, name), so at most one row exists per distance — no multi-row
    // accumulation is needed.
    Map<ComponentIdentifier, Map<String, Record>> byComponentAndName = new HashMap<>();
    for (Record row : rows) {
      ComponentIdentifier ci = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(
          row.get(co.COMPONENT_ID_FORMAT), row.get(co.COMPONENT_ID_COORDINATES_JSON));
      String name = row.get(co.OBLIGATION_NAME);

      byComponentAndName.computeIfAbsent(ci, k -> new HashMap<>())
          .merge(name, row,
              (existing, candidate) -> candidate.get(oa.ANCESTOR_DISTANCE) < existing.get(oa.ANCESTOR_DISTANCE)
                  ? candidate
                  : existing);
    }

    Map<ComponentIdentifier, List<ComponentObligation>> result = new HashMap<>();
    byComponentAndName.forEach(
        (ci, nameMap) -> result.put(ci, nameMap.values()
            .stream()
            .map(row -> row.into(ComponentObligation.class))
            .toList()));
    return result;
  }

  @Override
  public int insert(TransactionContext tx, ComponentObligation componentObligation) {
    if (getByOwnerIdAndComponentIdentifierAndObligationName(tx, componentObligation.getOwnerId(),
        componentObligation.getComponentIdentifier(), componentObligation.getObligationName()) != null)
    {
      throw new BadRequestException(
          "Component obligation already exists for owner with id " + componentObligation.getOwnerId() +
              " and component " + componentObligation.getComponentIdentifier() + " and obligation name " +
              componentObligation.getObligationName() + ".");
    }
    if (componentObligation.getLastUpdatedAt() == null) {
      componentObligation.setLastUpdatedAt(new Date());
    }
    return super.insert(tx, componentObligation);
  }

  @Override
  public int update(TransactionContext tx, ComponentObligation componentObligation) {
    if (getById(tx, componentObligation.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component obligation with id " + componentObligation.getId() + " because it does not exist.");
    }
    componentObligation.setLastUpdatedAt(new Date());
    return super.update(tx, componentObligation);
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_OBLIGATION;
  }

  @Override
  public Class<ComponentObligation> getEntityClass() {
    return ComponentObligation.class;
  }
}

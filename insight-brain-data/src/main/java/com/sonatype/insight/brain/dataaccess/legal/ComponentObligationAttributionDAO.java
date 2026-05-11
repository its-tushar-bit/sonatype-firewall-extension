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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligationAttribution.COMPONENT_OBLIGATION_ATTRIBUTION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;

/**
 * @since 1.105
 */
@Named
@Singleton
public class ComponentObligationAttributionDAO
    extends AbstractOperationalSqlDAO<ComponentObligationAttribution>
{
  private final OwnerDAO ownerDAO;

  @Inject
  public ComponentObligationAttributionDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
  }

  public List<ComponentObligationAttribution> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION_ATTRIBUTION)
        .where(COMPONENT_OBLIGATION_ATTRIBUTION.OWNER_ID.eq(ownerId))
        .fetch(super::toEntity);
  }

  public List<ComponentObligationAttribution> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNames(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    Set<String> nonNullObligationNames = obligationNames.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    org.jooq.Condition obligationCondition;
    if (obligationNames.contains(null)) {
      obligationCondition = COMPONENT_OBLIGATION_ATTRIBUTION.OBLIGATION_NAME.in(nonNullObligationNames)
          .or(COMPONENT_OBLIGATION_ATTRIBUTION.OBLIGATION_NAME.isNull());
    }
    else {
      obligationCondition = COMPONENT_OBLIGATION_ATTRIBUTION.OBLIGATION_NAME.in(nonNullObligationNames);
    }

    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION_ATTRIBUTION)
        .where(COMPONENT_OBLIGATION_ATTRIBUTION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .and(obligationCondition)
        .fetch(super::toEntity);
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION_ATTRIBUTION)
        .where(COMPONENT_OBLIGATION_ATTRIBUTION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetch(super::toEntity);
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNames(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationNames(tx, ownerId, componentIdentifier, obligationNames);
    }
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    Map<String, ComponentObligationAttribution> obligationNameToAttribution = new HashMap<>();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<ComponentObligationAttribution> componentObligationAttributions =
          getByOwnerIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
      for (ComponentObligationAttribution attribution : componentObligationAttributions) {
        obligationNameToAttribution.putIfAbsent(attribution.getObligationName(), attribution);
      }
    }
    return new ArrayList<>(obligationNameToAttribution.values());
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier);
    }
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    List<ComponentObligationAttribution> results = new ArrayList<>();
    Set<String> missingObligations = new HashSet<>(obligationNames);
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<ComponentObligationAttribution> componentObligationAttributions =
          getByOwnerIdAndComponentIdentifierAndObligationNames(tx, owner.getId(), componentIdentifier,
              missingObligations);
      results.addAll(componentObligationAttributions);
      missingObligations.removeAll(componentObligationAttributions.stream()
          .map(ComponentObligationAttribution::getObligationName)
          .collect(Collectors.toSet()));
      if (missingObligations.isEmpty()) {
        break;
      }
    }
    return results;
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(tx, ownerId, componentIdentifier,
          obligationNames);
    }
  }

  /**
   * Batch fetches obligation attributions for multiple components with hierarchy resolution.
   * Attributions accumulate from all hierarchy levels, with the closest ancestor winning per obligation_name.
   *
   * @param ownerId the owner (application) ID to resolve hierarchy from
   * @param componentIdentifiers the components to fetch attributions for
   * @return map from ComponentIdentifier to list of ComponentObligationAttribution; components with no attributions
   *         are not included
   */
  public Map<ComponentIdentifier, List<ComponentObligationAttribution>> batchGetWithHierarchy(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      return Collections.emptyMap();
    }

    var coa = COMPONENT_OBLIGATION_ATTRIBUTION;
    var oa = OWNER_ANCESTOR;

    // Fetch all attributions at all hierarchy levels, then keep closest per obligation_name in Java.
    // obligation_name is nullable; null-named attributions are deduped together.
    List<Record> rows = getListWithSqlInClause(componentIdentifiers, chunk -> {
      List<Row2<String, String>> componentRows = chunk.stream()
          .map(ComponentIdentifierAdapter::toComponentRow)
          .toList();
      try (TransactionContext tx = createTransactionContext()) {
        return new ArrayList<>(tx.dsl()
            .select(
                coa.COMPONENT_OBLIGATION_ATTRIBUTION_ID,
                coa.COMPONENT_ID_FORMAT,
                coa.COMPONENT_ID_COORDINATES_JSON,
                coa.OWNER_ID,
                coa.OBLIGATION_NAME,
                coa.CONTENT,
                coa.LEGAL_CONTENT_HASH,
                coa.LAST_UPDATED_BY_USERNAME,
                coa.LAST_UPDATED_AT,
                oa.ANCESTOR_DISTANCE)
            .from(coa)
            .join(oa)
            .on(coa.OWNER_ID.eq(oa.ANCESTOR_ID))
            .where(oa.OWNER_ID.eq(ownerId))
            .and(DSL.row(coa.COMPONENT_ID_FORMAT, coa.COMPONENT_ID_COORDINATES_JSON).in(componentRows))
            .fetch());
      }
    }, 2, 1);

    // Group by component, then deduplicate by obligation_name keeping the closest ancestor
    Map<ComponentIdentifier, Map<String, Record>> byComponentAndName = new HashMap<>();
    for (Record row : rows) {
      ComponentIdentifier ci = ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(
          row.get(coa.COMPONENT_ID_FORMAT), row.get(coa.COMPONENT_ID_COORDINATES_JSON));
      String name = row.get(coa.OBLIGATION_NAME);

      byComponentAndName.computeIfAbsent(ci, k -> new HashMap<>())
          .merge(name, row,
              (existing, candidate) -> candidate.get(oa.ANCESTOR_DISTANCE) < existing.get(oa.ANCESTOR_DISTANCE)
                  ? candidate
                  : existing);
    }

    Map<ComponentIdentifier, List<ComponentObligationAttribution>> result = new HashMap<>();
    byComponentAndName.forEach((ci, nameMap) -> result.put(ci,
        nameMap.values()
            .stream()
            .map(row -> row.into(ComponentObligationAttribution.class))
            .toList()));
    return result;
  }

  @Override
  public void insert(TransactionContext tx, ComponentObligationAttribution componentObligationAttribution) {
    if (componentObligationAttribution.getLastUpdatedAt() == null) {
      componentObligationAttribution.setLastUpdatedAt(new Date());
    }
    super.insert(tx, componentObligationAttribution);
  }

  @Override
  public void update(TransactionContext tx, ComponentObligationAttribution componentObligationAttribution) {
    if (getById(tx, componentObligationAttribution.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component obligation attribution with id " + componentObligationAttribution.getId() +
              " because it does not exist.");
    }
    componentObligationAttribution.setLastUpdatedAt(new Date());
    super.update(tx, componentObligationAttribution);
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_OBLIGATION_ATTRIBUTION;
  }

  @Override
  public Class<ComponentObligationAttribution> getEntityClass() {
    return ComponentObligationAttribution.class;
  }
}

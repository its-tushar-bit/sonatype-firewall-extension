/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.ApplicationComponentRisk;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerComponent.OWNER_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligation.COMPONENT_OBLIGATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION;

/**
 * @since 1.11
 */
@Named
@Singleton
public class OwnerComponentDAO
    extends AbstractOperationalSqlDAO<OwnerComponent>
{
  /**
   * Compound key for looking up OwnerComponent by owner, stage, and hash.
   */
  public record OwnerComponentKey(String ownerId, String stageTypeId, String hash)
  {
  }

  private static final int H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY = 350;

  private final TemporaryTableHelper temporaryTableHelper;

  @Inject
  public OwnerComponentDAO(
      final OperationalDataStore operationalDataStore,
      final TemporaryTableHelper temporaryTableHelper)
  {
    super(operationalDataStore);
    this.temporaryTableHelper = temporaryTableHelper;
  }

  @Override
  public int update(TransactionContext tx, OwnerComponent entity) {
    throw new UnsupportedOperationException("OwnerComponent does not support update operations");
  }

  public List<OwnerComponent> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(OWNER_COMPONENT)
        .where(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
  }

  public List<OwnerComponent> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<OwnerComponent> getByOwnerIdAndStageTypeId(String ownerId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndStageTypeId(tx, ownerId, stageTypeId);
    }
  }

  public List<OwnerComponent> getByOwnerIdAndStageTypeId(
      TransactionContext tx,
      String ownerId,
      String stageTypeId)
  {
    return tx.dsl()
        .selectFrom(OWNER_COMPONENT)
        .where(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
        .and(OWNER_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
        .fetch(this::toEntity);
  }

  /**
   * Batch variant of {@link #getByOwnerIdAndStageTypeId(String, String)} that fetches the components for many
   * owners at once, avoiding a per-owner query. Callers group the result by owner id.
   */
  public List<OwnerComponent> getByOwnerIdsAndStageTypeId(
      Set<String> ownerIds,
      String stageTypeId)
  {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return List.of();
    }
    return getListWithSqlInClause(
        ownerIds,
        ownerIdChunk -> {
          try (TransactionContext tx = createTransactionContext()) {
            return tx.dsl()
                .selectFrom(OWNER_COMPONENT)
                .where(OWNER_COMPONENT.OWNER_ID.in(ownerIdChunk))
                .and(OWNER_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
                .fetch(this::toEntity);
          }
        },
        getDataStore());
  }

  // Bypasses per-entity delete() for performance. This DAO does not use a SearchIndexManager,
  // so no search index side effects are lost.
  public void deleteByOwnerIdAndStageTypeId(TransactionContext tx, String ownerId, String stageTypeId) {
    tx.dsl()
        .deleteFrom(OWNER_COMPONENT)
        .where(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
        .and(OWNER_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
        .execute();
  }

  public void deleteByOwnerId(TransactionContext tx, String ownerId) {
    tx.dsl()
        .deleteFrom(OWNER_COMPONENT)
        .where(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
        .execute();
  }

  public OwnerComponent getByOwnerIdAndStageTypeIdAndHash(String ownerId, String stageTypeId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(OWNER_COMPONENT)
          .where(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
          .and(OWNER_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
          .and(OWNER_COMPONENT.HASH.eq(hash))
          .fetchOne());
    }
  }

  public Map<OwnerComponentKey, OwnerComponent> getMapByOwnerIdsAndStageTypeIdsAndHashes(
      Set<String> ownerIds,
      Set<String> stageTypeIds,
      Set<String> hashes)
  {
    if (CollectionUtils.isEmpty(ownerIds) || CollectionUtils.isEmpty(stageTypeIds)
        || CollectionUtils.isEmpty(hashes))
    {
      return Map.of();
    }
    return getStreamWithSqlInClause(
        ownerIds,
        ownerIdChunk -> getStreamWithSqlInClause(
            hashes,
            hashChunk -> {
              try (TransactionContext tx = createTransactionContext()) {
                return tx.dsl()
                    .selectFrom(OWNER_COMPONENT)
                    .where(OWNER_COMPONENT.OWNER_ID.in(ownerIdChunk))
                    .and(OWNER_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
                    .and(OWNER_COMPONENT.HASH.in(hashChunk))
                    .fetch(this::toEntity)
                    .stream();
              }
            },
            getDataStore(),
            1,
            ownerIdChunk.size() + stageTypeIds.size()),
        getDataStore(),
        1,
        stageTypeIds.size() + 1) // +1 for minimum 1 hash in each inner IN-clause
            .collect(Collectors.toMap(
                component -> new OwnerComponentKey(
                    component.getOwnerId(),
                    component.getStageTypeId(),
                    component.getHash()),
                component -> component));
  }

  public List<OwnerComponent> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndHash(tx, ownerId, hash);
    }
  }

  public List<OwnerComponent> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    return tx.dsl()
        .selectFrom(OWNER_COMPONENT)
        .where(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
        .and(OWNER_COMPONENT.HASH.eq(hash))
        .fetch(this::toEntity);
  }

  public List<OwnerComponent> getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public List<OwnerComponent> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    return tx.dsl()
        .selectFrom(OWNER_COMPONENT)
        .where(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
        .and(OWNER_COMPONENT.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(OWNER_COMPONENT.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetch(this::toEntity);
  }

  public OwnerComponent getLastByHash(String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(OWNER_COMPONENT)
          .where(OWNER_COMPONENT.HASH.eq(hash))
          .orderBy(OWNER_COMPONENT.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public OwnerComponent getLastByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(OWNER_COMPONENT)
          .where(OWNER_COMPONENT.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
          .and(OWNER_COMPONENT.COMPONENT_ID_COORDINATES_JSON.eq(
              ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
          .orderBy(OWNER_COMPONENT.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public List<OwnerComponent> getByOwnerIdsAndStageTypeIdsSince(
      Set<String> ownerIds,
      Set<String> stageTypeIds,
      Date date)
  {
    try (TransactionContext tx = createTransactionContext()) {
      if (ownerIds != null && ownerIds.size() >= getInOperatorThreshold()) {
        List<OwnerComponent> ownerComponents = tx.dsl()
            .selectFrom(OWNER_COMPONENT)
            .where(OWNER_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .and(OWNER_COMPONENT.TIME.greaterOrEqual(date))
            .orderBy(OWNER_COMPONENT.TIME.asc())
            .fetch(this::toEntity);

        List<OwnerComponent> retval = new ArrayList<>();
        for (OwnerComponent ownerComponent : ownerComponents) {
          if (ownerIds.contains(ownerComponent.getOwnerId())) {
            retval.add(ownerComponent);
          }
        }
        return retval;
      }
      else {
        return tx.dsl()
            .selectFrom(OWNER_COMPONENT)
            .where(OWNER_COMPONENT.OWNER_ID.in(ownerIds))
            .and(OWNER_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .and(OWNER_COMPONENT.TIME.greaterOrEqual(date))
            .orderBy(OWNER_COMPONENT.TIME.asc())
            .fetch(this::toEntity);
      }
    }
  }

  public List<OwnerComponent> getByOwnerIdsAndStageTypeIds(
      Set<String> ownerIds,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      if (ownerIds != null && ownerIds.size() >= getInOperatorThreshold()) {
        List<OwnerComponent> ownerComponents = tx.dsl()
            .selectFrom(OWNER_COMPONENT)
            .where(OWNER_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .fetch(this::toEntity);

        List<OwnerComponent> retval = new ArrayList<>();
        for (OwnerComponent ownerComponent : ownerComponents) {
          if (ownerIds.contains(ownerComponent.getOwnerId())) {
            retval.add(ownerComponent);
          }
        }
        return retval;
      }
      else {
        return tx.dsl()
            .selectFrom(OWNER_COMPONENT)
            .where(OWNER_COMPONENT.OWNER_ID.in(ownerIds))
            .and(OWNER_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .fetch(this::toEntity);
      }
    }
  }

  /**
   * Queries the combination of owner IDs and stage type IDs where the components found in the last evaluation
   * have a review of the license legal obligations already started or not.
   * <p>
   * A license legal obligations review is considered started for an owner and stage type when there is at least one
   * entry for a component in {@ComponentObligation} whether at the application, organization or root organization scope
   * while a not started review is when there is not a single entry.
   *
   * @param ownerIds Owner IDs where the query can be made.
   * @param stageTypeIds Stage type IDs where the query can be made.
   * @param isReviewStarted {@code true} to query the owners and stage types where the review already started,
   *          {@code false} to query the ones where the review hasn't started.
   * @return A list of Object arrays with 2 positions: the owner ID and the stage type ID.
   */
  public List<Object[]> getOwnerIdsAndStageTypeIdsByReviewStatus(
      Set<String> ownerIds,
      Set<String> stageTypeIds,
      boolean isReviewStarted)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var oc = OWNER_COMPONENT.as("oc");
      var a = APPLICATION.as("a");

      // Build EXISTS condition for application level
      Condition appExists = DSL.exists(
          DSL.selectOne()
              .from(COMPONENT_OBLIGATION)
              .where(COMPONENT_OBLIGATION.OWNER_ID.eq(oc.OWNER_ID))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(oc.COMPONENT_ID_FORMAT))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(oc.COMPONENT_ID_COORDINATES_JSON)));

      // Build EXISTS condition for organization level
      Condition orgExists = DSL.exists(
          DSL.selectOne()
              .from(COMPONENT_OBLIGATION)
              .where(COMPONENT_OBLIGATION.OWNER_ID.eq(a.ORGANIZATION_ID))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(oc.COMPONENT_ID_FORMAT))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(oc.COMPONENT_ID_COORDINATES_JSON)));

      // Build EXISTS condition for root organization level
      Condition rootExists = DSL.exists(
          DSL.selectOne()
              .from(COMPONENT_OBLIGATION)
              .where(COMPONENT_OBLIGATION.OWNER_ID.eq(Organization.ROOT_ORGANIZATION_ID))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(oc.COMPONENT_ID_FORMAT))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(oc.COMPONENT_ID_COORDINATES_JSON)));

      // Combine conditions based on isReviewStarted
      Condition reviewCondition;
      if (isReviewStarted) {
        reviewCondition = appExists.or(orgExists).or(rootExists);
      }
      else {
        reviewCondition = DSL.not(appExists).and(DSL.not(orgExists)).and(DSL.not(rootExists));
      }

      boolean requiresManualFilter = requiresManualFilter(ownerIds);

      Condition baseCondition = a.APPLICATION_ID.eq(oc.OWNER_ID)
          .and(oc.STAGE_TYPE_ID.in(stageTypeIds))
          .and(reviewCondition);

      if (!requiresManualFilter) {
        baseCondition = baseCondition.and(oc.OWNER_ID.in(ownerIds));
      }

      List<Object[]> results = tx.dsl()
          .selectDistinct(oc.OWNER_ID, oc.STAGE_TYPE_ID)
          .from(oc, a)
          .where(baseCondition)
          .fetch(r -> new Object[]{r.value1(), r.value2()});

      if (requiresManualFilter) {
        return results.stream()
            .filter(array -> ownerIds.contains(array[0].toString()))
            .collect(Collectors.toList());
      }

      return results;
    }
  }

  public List<ApplicationComponentRisk> getComponentsRiskFiltered(
      Set<String> ownerIds,
      Set<String> stageTypes,
      Set<String> policyThreatCategoryFilter,
      Entry<Integer, Integer> policyThreatLevelFilter,
      Set<String> policyViolationStateFilter,
      String orderBy,
      int page,
      int pageSize)
  {
    return getComponentsRiskFiltered(
        ownerIds,
        stageTypes,
        policyThreatCategoryFilter,
        policyThreatLevelFilter == null ? null : List.of(policyThreatLevelFilter),
        policyViolationStateFilter,
        orderBy,
        page,
        pageSize,
        null);
  }

  /**
   * Threat levels may be a single contiguous range or several OR'd ranges (Martha multi-bucket
   * selection). Multiple ranges are applied as {@code (BETWEEN … OR BETWEEN …)} so gap levels are
   * not included in score aggregation.
   * <p>
   * When {@code componentHashes} is non-empty, only those hashes are aggregated — used by Martha
   * page-card enrichment so SQL cost stays proportional to the visible page (≤100 hashes).
   */
  public List<ApplicationComponentRisk> getComponentsRiskFiltered(
      Set<String> ownerIds,
      Set<String> stageTypes,
      Set<String> policyThreatCategoryFilter,
      List<Entry<Integer, Integer>> policyThreatLevelRanges,
      Set<String> policyViolationStateFilter,
      String orderBy,
      int page,
      int pageSize,
      Set<String> componentHashes)
  {
    if (!isDatabasePostgresql()) {
      throw new UnsupportedOperationException("This operation is only supported for PostgreSQL databases");
    }

    if (ownerIds.isEmpty()) {
      return Collections.emptyList();
    }
    if (componentHashes != null && componentHashes.isEmpty()) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      boolean useTemporaryTable =
          temporaryTableHelper.maybeCreateTemporaryTableWithIds(tx, ownerIds);

      var pv = POLICY_VIOLATION.as("pv");

      // Build WHERE conditions using jOOQ
      Condition whereCondition = pv.FIX_TIME.isNull();

      if (!useTemporaryTable) {
        whereCondition = whereCondition.and(pv.OWNER_ID.in(ownerIds));
      }
      if (componentHashes != null && !componentHashes.isEmpty()) {
        whereCondition = whereCondition.and(pv.HASH.in(componentHashes));
      }
      if (!stageTypes.isEmpty()) {
        whereCondition = whereCondition.and(pv.STAGE_TYPE_ID.in(stageTypes));
      }
      if (!policyThreatCategoryFilter.isEmpty()) {
        whereCondition = whereCondition.and(pv.THREAT_CATEGORY.in(policyThreatCategoryFilter));
      }
      if (policyThreatLevelRanges != null && !policyThreatLevelRanges.isEmpty()) {
        Condition threatCondition = null;
        for (Entry<Integer, Integer> range : policyThreatLevelRanges) {
          if (range == null || range.getKey() == null || range.getValue() == null) {
            continue;
          }
          Condition between = pv.THREAT_LEVEL.between(
              clampThreatLevel(range.getKey()),
              clampThreatLevel(range.getValue()));
          threatCondition = threatCondition == null ? between : threatCondition.or(between);
        }
        if (threatCondition != null) {
          whereCondition = whereCondition.and(threatCondition);
        }
      }
      if (!policyViolationStateFilter.isEmpty()
          && !policyViolationStateFilter.containsAll(List.of("WAIVED", "LEGACY_VIOLATION", "OPEN")))
      {
        Condition stateCondition = DSL.noCondition();
        for (String state : policyViolationStateFilter) {
          Condition stateClause = switch (state) {
            case "WAIVED" -> pv.WAIVE_TIME.isNotNull();
            case "LEGACY_VIOLATION" -> pv.LEGACY_VIOLATION_TIME.isNotNull();
            case "OPEN" -> pv.WAIVE_TIME.isNull().and(pv.LEGACY_VIOLATION_TIME.isNull());
            default -> throw new IllegalArgumentException("Invalid policy violation state: " + state);
          };
          stateCondition = stateCondition.or(stateClause);
        }
        whereCondition = whereCondition.and(stateCondition);
      }

      // Build the inner subquery using ROW_NUMBER() to simulate DISTINCT ON (PostgreSQL-specific pattern)
      var rowNum = DSL.rowNumber()
          .over(
              DSL.partitionBy(pv.OWNER_ID, pv.POLICY_ID, pv.CONSTRAINT_FACTS_ID,
                  pv.HASH, pv.COMPONENT_ID_FORMAT, pv.COMPONENT_ID_COORDINATES_JSON)
                  .orderBy(pv.OPEN_TIME.desc()))
          .as("rn");

      var subquery = useTemporaryTable
          ? tx.dsl()
              .select(pv.OWNER_ID, pv.POLICY_ID, pv.CONSTRAINT_FACTS_ID, pv.OPEN_TIME,
                  pv.THREAT_LEVEL, pv.HASH, pv.FILENAME, pv.COMPONENT_ID_FORMAT, pv.COMPONENT_ID_COORDINATES_JSON,
                  rowNum)
              .from(pv)
              .join(DSL.table("temporary_ids").as("ti"))
              .on(pv.OWNER_ID.eq(DSL.field("ti.id", String.class)))
              .where(whereCondition)
              .asTable("sub")
          : tx.dsl()
              .select(pv.OWNER_ID, pv.POLICY_ID, pv.CONSTRAINT_FACTS_ID, pv.OPEN_TIME,
                  pv.THREAT_LEVEL, pv.HASH, pv.FILENAME, pv.COMPONENT_ID_FORMAT, pv.COMPONENT_ID_COORDINATES_JSON,
                  rowNum)
              .from(pv)
              .where(whereCondition)
              .asTable("sub");

      var innerQuery = tx.dsl()
          .select(
              DSL.field("sub.owner_id", String.class).as("owner_id"),
              DSL.field("sub.policy_id", String.class).as("policy_id"),
              DSL.field("sub.constraint_facts_id", String.class).as("constraint_facts_id"),
              DSL.field("sub.open_time").as("open_time"),
              DSL.field("sub.threat_level", Short.class).as("threat_level"),
              DSL.field("sub.hash", String.class).as("hash"),
              DSL.field("sub.filename", String.class).as("filename"),
              DSL.field("sub.component_id_format", String.class).as("component_id_format"),
              DSL.field("sub.component_id_coordinates_json", String.class).as("component_id_coordinates_json"))
          .from(subquery)
          .where(DSL.field("sub.rn", Integer.class).eq(1))
          .asTable("x");

      var hash = DSL.field("hash", String.class);
      var filename = DSL.field("filename", String.class);
      var componentIdFormat = DSL.field("component_id_format", String.class);
      var componentIdCoordinatesJson = DSL.field("component_id_coordinates_json", String.class);
      var ownerId = DSL.field("owner_id", String.class);
      var threatLevel = DSL.field("threat_level", Short.class);

      // Parse orderBy string (e.g., "score DESC") into field and direction
      String[] orderByParts = orderBy.split(" ");
      String orderColumn = orderByParts[0];
      boolean orderDesc = orderByParts.length > 1 && "DESC".equalsIgnoreCase(orderByParts[1]);
      // Use DSL.name() to ensure the column name is properly quoted in PostgreSQL. Without quoting,
      // PostgreSQL lowercases identifiers, which won't match quoted aliases like "affectedApplications"
      var orderField = orderDesc ? DSL.field(DSL.name(orderColumn)).desc() : DSL.field(DSL.name(orderColumn)).asc();

      var query = tx.dsl()
          .select(
              hash,
              filename,
              componentIdFormat,
              componentIdCoordinatesJson,
              DSL.countDistinct(ownerId).as("affectedApplications"),
              DSL.sum(threatLevel).as("score"),
              DSL.sum(DSL.when(threatLevel.ge((short) 8), threatLevel).otherwise((short) 0)).as("scoreCritical"),
              DSL.sum(DSL.when(threatLevel.ge((short) 4).and(threatLevel.lt((short) 8)), threatLevel)
                  .otherwise((short) 0)).as("scoreSevere"),
              DSL.sum(DSL.when(threatLevel.ge((short) 2).and(threatLevel.lt((short) 4)), threatLevel)
                  .otherwise((short) 0)).as("scoreModerate"),
              DSL.sum(DSL.when(threatLevel.lt((short) 2), threatLevel).otherwise((short) 0)).as("scoreLow"))
          .from(innerQuery)
          .groupBy(hash, filename, componentIdFormat, componentIdCoordinatesJson)
          .orderBy(orderField, hash.asc());

      var finalQuery = pageSize < Integer.MAX_VALUE
          ? query.limit(pageSize + 1).offset(pageSize * page)
          : query;

      return finalQuery.fetch(r -> new ApplicationComponentRisk(
          r.get(hash),
          r.get(filename),
          r.get(componentIdFormat),
          r.get(componentIdCoordinatesJson),
          r.get("affectedApplications", Integer.class),
          r.get("score", Integer.class),
          r.get("scoreCritical", Integer.class),
          r.get("scoreSevere", Integer.class),
          r.get("scoreModerate", Integer.class),
          r.get("scoreLow", Integer.class)));
    }
  }

  /**
   * Policy threat levels are 0–10. Callers may pass {@link Integer#MAX_VALUE} as an unbounded max;
   * casting that to {@code short} wraps to {@code -1} and breaks {@code BETWEEN}.
   */
  public static short clampThreatLevel(final int level) {
    return (short) Math.max(0, Math.min(level, 10));
  }

  private boolean requiresManualFilter(Collection<?> items) {
    return isDatabaseEmbedded() && items.size() >= H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY;
  }

  @Override
  public Table<?> getJooqTable() {
    return OWNER_COMPONENT;
  }

  @Override
  public Class<OwnerComponent> getEntityClass() {
    return OwnerComponent.class;
  }
}

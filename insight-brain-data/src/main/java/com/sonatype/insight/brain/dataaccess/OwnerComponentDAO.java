/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.ApplicationComponentRisk;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LastPolicyEvaluation.LAST_POLICY_EVALUATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerComponent.OWNER_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligation.COMPONENT_OBLIGATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION;
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

  public void deleteByOwnerIds(TransactionContext tx, Collection<String> ownerIds) {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return;
    }
    getListWithSqlInClause(ownerIds, idChunk -> List.of(tx.dsl()
        .deleteFrom(OWNER_COMPONENT)
        .where(OWNER_COMPONENT.OWNER_ID.in(idChunk))
        .execute()), getDataStore());
  }

  /**
   * Counts owner_component rows per {@code (owner_id, stage_type_id)} pair, keyed by
   * {@code ownerId + "|" + stageTypeId}.
   */
  public Map<String, Integer> getCountsByOwnerIdsAndStageTypeIds(
      Collection<String> ownerIds,
      Collection<String> stageTypeIds)
  {
    if (CollectionUtils.isEmpty(ownerIds) || CollectionUtils.isEmpty(stageTypeIds)) {
      return Map.of();
    }
    Map<String, Integer> counts = new HashMap<>();
    getListWithSqlInClause(ownerIds, chunk -> {
      try (TransactionContext tx = createTransactionContext()) {
        tx.dsl()
            .select(OWNER_COMPONENT.OWNER_ID, OWNER_COMPONENT.STAGE_TYPE_ID, DSL.count())
            .from(OWNER_COMPONENT)
            .where(OWNER_COMPONENT.OWNER_ID.in(chunk))
            .and(OWNER_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .groupBy(OWNER_COMPONENT.OWNER_ID, OWNER_COMPONENT.STAGE_TYPE_ID)
            .fetch()
            .forEach(r -> counts.put(r.value1() + '|' + r.value2(), r.value3()));
      }
      return List.of();
    }, getDataStore());
    return counts;
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
   * Distinct application ({@code owner_id}) that contains {@code hash}, with latest component time.
   * Used by Nexus One component where-used (CLM-43959).
   */
  public record ComponentOwnerUsageRow(String ownerId, Date lastSeenTime)
  {
  }

  /**
   * Distinct organization that contains {@code hash} via at least one readable application, with
   * distinct application count and latest component time.
   */
  public record ComponentOrganizationUsageRow(String organizationId, long applicationCount, Date lastSeenTime)
  {
  }

  /**
   * Latest policy evaluation report for an application stage that contains a component hash.
   */
  public record ComponentReportUsageRow(String reportId, String stageTypeId, Date evaluationTime)
  {
  }

  /**
   * Count + page of distinct applications for a hash in one transaction (one RBAC temp table).
   * Inner-joins {@code application} so orphan {@code owner_component} rows never inflate total/pages.
   * <ul>
   * <li>{@code ownerIds == null} — unrestricted (hash-only; no owner IN clause)</li>
   * <li>{@code ownerIds} empty — fail-closed → empty</li>
   * <li>non-empty — filter to those owners (temp table when large)</li>
   * </ul>
   */
  public record PagedOwnersByHash(long total, List<ComponentOwnerUsageRow> rows)
  {
  }

  /**
   * Count + page of distinct organizations for a hash in one transaction (one RBAC temp table).
   */
  public record PagedOrganizationsByHash(long total, List<ComponentOrganizationUsageRow> rows)
  {
  }

  /**
   * Count + page of latest report rows for one application and component hash.
   */
  public record PagedReportsByHashAndOwner(long total, List<ComponentReportUsageRow> rows)
  {
  }

  /**
   * Paged distinct application owners for a component hash (latest-seen descending), with total.
   */
  public PagedOwnersByHash findDistinctOwnersByHashPaged(
      final String hash,
      final Set<String> ownerIds,
      final int offset,
      final int limit)
  {
    return findDistinctOwnersByHashPaged(hash, ownerIds, offset, limit, null, null);
  }

  /**
   * Paged distinct application owners with optional name / organization filters (CLM-44667).
   */
  public PagedOwnersByHash findDistinctOwnersByHashPaged(
      final String hash,
      final Set<String> ownerIds,
      final int offset,
      final int limit,
      final String nameSearch,
      final String organizationId)
  {
    if (StringUtils.isBlank(hash) || isFailClosedOwnerScope(ownerIds) || limit < 1) {
      return new PagedOwnersByHash(0L, List.of());
    }
    String normalizedName = normalizedNameSearch(nameSearch);
    // Embedded H2: avoid uncapped IN (ownerIds) cost; seek by hash then filter/page in memory.
    if (ownerIds != null && requiresManualFilter(ownerIds)) {
      return findDistinctOwnersByHashPagedEmbedded(hash, ownerIds, offset, limit, normalizedName, organizationId);
    }
    try (TransactionContext tx = createTransactionContext()) {
      boolean useTemporaryTable = prepareOwnerTempTable(tx, ownerIds);
      Condition where = hashAndOwnerCondition(hash, ownerIds, useTemporaryTable)
          .and(applicationUsageFilters(normalizedName, organizationId));

      var countFrom = tx.dsl()
          .select(DSL.countDistinct(OWNER_COMPONENT.OWNER_ID))
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID));
      if (useTemporaryTable) {
        countFrom = countFrom
            .join(DSL.table("temporary_ids").as("ti"))
            .on(OWNER_COMPONENT.OWNER_ID.eq(DSL.field("ti.id", String.class)));
      }
      Long count = countFrom.where(where).fetchOne(0, Long.class);
      long total = count == null ? 0L : count;

      var lastSeen = DSL.max(OWNER_COMPONENT.TIME).as("last_seen");
      var select = tx.dsl()
          .select(OWNER_COMPONENT.OWNER_ID, lastSeen)
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID));
      if (useTemporaryTable) {
        select = select
            .join(DSL.table("temporary_ids").as("ti"))
            .on(OWNER_COMPONENT.OWNER_ID.eq(DSL.field("ti.id", String.class)));
      }
      List<ComponentOwnerUsageRow> rows = select
          .where(where)
          .groupBy(OWNER_COMPONENT.OWNER_ID)
          .orderBy(lastSeen.desc(), OWNER_COMPONENT.OWNER_ID.asc())
          .limit(limit)
          .offset(offset)
          .fetch(r -> new ComponentOwnerUsageRow(r.get(OWNER_COMPONENT.OWNER_ID), r.get(lastSeen)));
      return new PagedOwnersByHash(total, rows);
    }
  }

  /**
   * Resolve specific application owners for a hash (URL / selected Path ids).
   * When {@code nameSearch} is null/blank, name is not filtered (used to pin Path selections).
   */
  public List<ComponentOwnerUsageRow> findDistinctOwnersByHashAndIds(
      final String hash,
      final Set<String> ownerIds,
      final Set<String> includeIds,
      final String organizationId)
  {
    return findDistinctOwnersByHashAndIds(hash, ownerIds, includeIds, organizationId, null);
  }

  public List<ComponentOwnerUsageRow> findDistinctOwnersByHashAndIds(
      final String hash,
      final Set<String> ownerIds,
      final Set<String> includeIds,
      final String organizationId,
      final String nameSearch)
  {
    if (StringUtils.isBlank(hash) || isFailClosedOwnerScope(ownerIds) || CollectionUtils.isEmpty(includeIds)) {
      return List.of();
    }
    Set<String> scopedIncludeIds = ownerIds == null
        ? includeIds
        : includeIds.stream().filter(ownerIds::contains).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    if (scopedIncludeIds.isEmpty()) {
      return List.of();
    }
    String normalizedName = normalizedNameSearch(nameSearch);
    try (TransactionContext tx = createTransactionContext()) {
      Condition where = OWNER_COMPONENT.HASH.eq(hash)
          .and(OWNER_COMPONENT.OWNER_ID.in(scopedIncludeIds))
          .and(applicationUsageFilters(normalizedName, organizationId));
      var lastSeen = DSL.max(OWNER_COMPONENT.TIME).as("last_seen");
      return tx.dsl()
          .select(OWNER_COMPONENT.OWNER_ID, lastSeen)
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID))
          .where(where)
          .groupBy(OWNER_COMPONENT.OWNER_ID)
          .orderBy(lastSeen.desc(), OWNER_COMPONENT.OWNER_ID.asc())
          .fetch(r -> new ComponentOwnerUsageRow(r.get(OWNER_COMPONENT.OWNER_ID), r.get(lastSeen)));
    }
  }

  /**
   * H2-safe owners where-used: hash index seek + join application, then RBAC filter/page in memory.
   */
  private PagedOwnersByHash findDistinctOwnersByHashPagedEmbedded(
      final String hash,
      final Set<String> ownerIds,
      final int offset,
      final int limit,
      final String normalizedName,
      final String organizationId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var lastSeen = DSL.max(OWNER_COMPONENT.TIME).as("last_seen");
      Condition where = OWNER_COMPONENT.HASH.eq(hash).and(applicationUsageFilters(normalizedName, organizationId));
      List<ComponentOwnerUsageRow> filtered = tx.dsl()
          .select(OWNER_COMPONENT.OWNER_ID, lastSeen)
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID))
          .where(where)
          .groupBy(OWNER_COMPONENT.OWNER_ID)
          .fetch(r -> new ComponentOwnerUsageRow(r.get(OWNER_COMPONENT.OWNER_ID), r.get(lastSeen)))
          .stream()
          .filter(row -> ownerIds.contains(row.ownerId()))
          .sorted(Comparator
              .comparing(ComponentOwnerUsageRow::lastSeenTime, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(ComponentOwnerUsageRow::ownerId, Comparator.nullsLast(String::compareTo)))
          .toList();
      return pageOwners(filtered, offset, limit);
    }
  }

  /**
   * Stage type ids per owner for a hash, for the current applications page only.
   */
  public Map<String, List<String>> getStageTypeIdsByOwnerIdForHash(
      final String hash,
      final Collection<String> ownerIds)
  {
    if (StringUtils.isBlank(hash) || CollectionUtils.isEmpty(ownerIds)) {
      return Map.of();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(OWNER_COMPONENT.OWNER_ID, OWNER_COMPONENT.STAGE_TYPE_ID)
          .from(OWNER_COMPONENT)
          .where(OWNER_COMPONENT.HASH.eq(hash))
          .and(OWNER_COMPONENT.OWNER_ID.in(ownerIds))
          .orderBy(OWNER_COMPONENT.OWNER_ID.asc(), OWNER_COMPONENT.STAGE_TYPE_ID.asc())
          .fetchGroups(OWNER_COMPONENT.OWNER_ID, OWNER_COMPONENT.STAGE_TYPE_ID);
    }
  }

  /**
   * Paged distinct organizations for a component hash (latest-seen descending), with total.
   */
  public PagedOrganizationsByHash findDistinctOrganizationsByHashPaged(
      final String hash,
      final Set<String> ownerIds,
      final int offset,
      final int limit)
  {
    return findDistinctOrganizationsByHashPaged(hash, ownerIds, offset, limit, null);
  }

  /**
   * Paged distinct organizations with optional name filter (CLM-44667).
   */
  public PagedOrganizationsByHash findDistinctOrganizationsByHashPaged(
      final String hash,
      final Set<String> ownerIds,
      final int offset,
      final int limit,
      final String nameSearch)
  {
    if (StringUtils.isBlank(hash) || isFailClosedOwnerScope(ownerIds) || limit < 1) {
      return new PagedOrganizationsByHash(0L, List.of());
    }
    String normalizedName = normalizedNameSearch(nameSearch);
    if (ownerIds != null && requiresManualFilter(ownerIds)) {
      return findDistinctOrganizationsByHashPagedEmbedded(hash, ownerIds, offset, limit, normalizedName);
    }
    try (TransactionContext tx = createTransactionContext()) {
      boolean useTemporaryTable = prepareOwnerTempTable(tx, ownerIds);
      // organization_id is NOT NULL in schema; keep explicit for GROUP BY / COUNT Distinct parity.
      Condition where = hashAndOwnerCondition(hash, ownerIds, useTemporaryTable)
          .and(APPLICATION.ORGANIZATION_ID.isNotNull())
          .and(organizationUsageFilters(normalizedName));
      boolean joinOrganization = requiresOrganizationJoin(normalizedName);

      var countFrom = tx.dsl()
          .select(DSL.countDistinct(APPLICATION.ORGANIZATION_ID))
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID));
      if (joinOrganization) {
        countFrom = countFrom
            .join(ORGANIZATION)
            .on(ORGANIZATION.ORGANIZATION_ID.eq(APPLICATION.ORGANIZATION_ID));
      }
      if (useTemporaryTable) {
        countFrom = countFrom
            .join(DSL.table("temporary_ids").as("ti"))
            .on(OWNER_COMPONENT.OWNER_ID.eq(DSL.field("ti.id", String.class)));
      }
      Long count = countFrom.where(where).fetchOne(0, Long.class);
      long total = count == null ? 0L : count;

      var lastSeen = DSL.max(OWNER_COMPONENT.TIME).as("last_seen");
      var appCount = DSL.countDistinct(OWNER_COMPONENT.OWNER_ID).as("application_count");
      var select = tx.dsl()
          .select(APPLICATION.ORGANIZATION_ID, appCount, lastSeen)
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID));
      if (joinOrganization) {
        select = select
            .join(ORGANIZATION)
            .on(ORGANIZATION.ORGANIZATION_ID.eq(APPLICATION.ORGANIZATION_ID));
      }
      if (useTemporaryTable) {
        select = select
            .join(DSL.table("temporary_ids").as("ti"))
            .on(OWNER_COMPONENT.OWNER_ID.eq(DSL.field("ti.id", String.class)));
      }
      List<ComponentOrganizationUsageRow> rows = select
          .where(where)
          .groupBy(APPLICATION.ORGANIZATION_ID)
          .orderBy(lastSeen.desc(), APPLICATION.ORGANIZATION_ID.asc())
          .limit(limit)
          .offset(offset)
          .fetch(r -> {
            Long applicationCount = r.get(appCount, Long.class);
            return new ComponentOrganizationUsageRow(
                r.get(APPLICATION.ORGANIZATION_ID),
                applicationCount == null ? 0L : applicationCount,
                r.get(lastSeen));
          });
      return new PagedOrganizationsByHash(total, rows);
    }
  }

  /**
   * Resolve specific organizations for a hash (URL / selected Path ids).
   * When {@code nameSearch} is null/blank, name is not filtered (used to pin Path selections).
   */
  public List<ComponentOrganizationUsageRow> findDistinctOrganizationsByHashAndIds(
      final String hash,
      final Set<String> ownerIds,
      final Set<String> includeOrganizationIds)
  {
    return findDistinctOrganizationsByHashAndIds(hash, ownerIds, includeOrganizationIds, null);
  }

  public List<ComponentOrganizationUsageRow> findDistinctOrganizationsByHashAndIds(
      final String hash,
      final Set<String> ownerIds,
      final Set<String> includeOrganizationIds,
      final String nameSearch)
  {
    if (StringUtils.isBlank(hash)
        || isFailClosedOwnerScope(ownerIds)
        || CollectionUtils.isEmpty(includeOrganizationIds))
    {
      return List.of();
    }
    String normalizedName = normalizedNameSearch(nameSearch);
    try (TransactionContext tx = createTransactionContext()) {
      boolean useTemporaryTable = prepareOwnerTempTable(tx, ownerIds);
      Condition where = hashAndOwnerCondition(hash, ownerIds, useTemporaryTable)
          .and(APPLICATION.ORGANIZATION_ID.in(includeOrganizationIds))
          .and(organizationUsageFilters(normalizedName));
      var lastSeen = DSL.max(OWNER_COMPONENT.TIME).as("last_seen");
      var appCount = DSL.countDistinct(OWNER_COMPONENT.OWNER_ID).as("application_count");
      var select = tx.dsl()
          .select(APPLICATION.ORGANIZATION_ID, appCount, lastSeen)
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID));
      if (requiresOrganizationJoin(normalizedName)) {
        select = select
            .join(ORGANIZATION)
            .on(ORGANIZATION.ORGANIZATION_ID.eq(APPLICATION.ORGANIZATION_ID));
      }
      if (useTemporaryTable) {
        select = select
            .join(DSL.table("temporary_ids").as("ti"))
            .on(OWNER_COMPONENT.OWNER_ID.eq(DSL.field("ti.id", String.class)));
      }
      return select
          .where(where)
          .groupBy(APPLICATION.ORGANIZATION_ID)
          .orderBy(lastSeen.desc(), APPLICATION.ORGANIZATION_ID.asc())
          .fetch(r -> {
            Long applicationCount = r.get(appCount, Long.class);
            return new ComponentOrganizationUsageRow(
                r.get(APPLICATION.ORGANIZATION_ID),
                applicationCount == null ? 0L : applicationCount,
                r.get(lastSeen));
          });
    }
  }

  /**
   * Paged latest report ids per application stage for a component hash.
   * Does not join {@code application}: the caller supplies a single owner id after RBAC validation,
   * so this is a targeted application lookup rather than a bulk estate scan.
   */
  public PagedReportsByHashAndOwner findReportsByHashAndOwnerPaged(
      final String hash,
      final String ownerId,
      final int offset,
      final int limit)
  {
    if (StringUtils.isBlank(hash) || StringUtils.isBlank(ownerId) || limit < 1) {
      return new PagedReportsByHashAndOwner(0L, List.of());
    }
    try (TransactionContext tx = createTransactionContext()) {
      Condition where = OWNER_COMPONENT.HASH.eq(hash)
          .and(OWNER_COMPONENT.OWNER_ID.eq(ownerId))
          .and(POLICY_EVALUATION.SCAN_ID.isNotNull())
          .and(DSL.trim(POLICY_EVALUATION.SCAN_ID).ne(""));

      Long count = tx.dsl()
          .select(DSL.countDistinct(OWNER_COMPONENT.STAGE_TYPE_ID))
          .from(OWNER_COMPONENT)
          .join(LAST_POLICY_EVALUATION)
          .on(LAST_POLICY_EVALUATION.OWNER_ID.eq(OWNER_COMPONENT.OWNER_ID))
          .and(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.eq(OWNER_COMPONENT.STAGE_TYPE_ID))
          .join(POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .where(where)
          .fetchOne(0, Long.class);
      long total = count == null ? 0L : count;

      List<ComponentReportUsageRow> rows = tx.dsl()
          .selectDistinct(
              POLICY_EVALUATION.SCAN_ID,
              OWNER_COMPONENT.STAGE_TYPE_ID,
              POLICY_EVALUATION.TIME)
          .from(OWNER_COMPONENT)
          .join(LAST_POLICY_EVALUATION)
          .on(LAST_POLICY_EVALUATION.OWNER_ID.eq(OWNER_COMPONENT.OWNER_ID))
          .and(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.eq(OWNER_COMPONENT.STAGE_TYPE_ID))
          .join(POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .where(where)
          .orderBy(POLICY_EVALUATION.TIME.desc().nullsLast(), OWNER_COMPONENT.STAGE_TYPE_ID.asc())
          .limit(limit)
          .offset(offset)
          .fetch(r -> new ComponentReportUsageRow(
              r.get(POLICY_EVALUATION.SCAN_ID),
              r.get(OWNER_COMPONENT.STAGE_TYPE_ID),
              r.get(POLICY_EVALUATION.TIME)));
      return new PagedReportsByHashAndOwner(total, rows);
    }
  }

  /**
   * H2-safe orgs where-used: hash seek + join application, RBAC filter and org rollup in memory.
   */
  private PagedOrganizationsByHash findDistinctOrganizationsByHashPagedEmbedded(
      final String hash,
      final Set<String> ownerIds,
      final int offset,
      final int limit,
      final String normalizedName)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var lastSeen = DSL.max(OWNER_COMPONENT.TIME).as("last_seen");
      record OwnerOrg(String ownerId, String organizationId, Date lastSeenTime)
      {
      }
      Condition where = OWNER_COMPONENT.HASH.eq(hash)
          .and(APPLICATION.ORGANIZATION_ID.isNotNull())
          .and(organizationUsageFilters(normalizedName));
      var select = tx.dsl()
          .select(OWNER_COMPONENT.OWNER_ID, APPLICATION.ORGANIZATION_ID, lastSeen)
          .from(OWNER_COMPONENT)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(OWNER_COMPONENT.OWNER_ID));
      if (requiresOrganizationJoin(normalizedName)) {
        select = select
            .join(ORGANIZATION)
            .on(ORGANIZATION.ORGANIZATION_ID.eq(APPLICATION.ORGANIZATION_ID));
      }
      List<OwnerOrg> owners = select
          .where(where)
          .groupBy(OWNER_COMPONENT.OWNER_ID, APPLICATION.ORGANIZATION_ID)
          .fetch(r -> new OwnerOrg(
              r.get(OWNER_COMPONENT.OWNER_ID),
              r.get(APPLICATION.ORGANIZATION_ID),
              r.get(lastSeen)))
          .stream()
          .filter(row -> ownerIds.contains(row.ownerId()))
          .toList();

      Map<String, ComponentOrganizationUsageRow> byOrg = new HashMap<>();
      for (OwnerOrg owner : owners) {
        ComponentOrganizationUsageRow existing = byOrg.get(owner.organizationId());
        if (existing == null) {
          byOrg.put(owner.organizationId(), new ComponentOrganizationUsageRow(
              owner.organizationId(), 1L, owner.lastSeenTime()));
          continue;
        }
        Date mergedLastSeen = existing.lastSeenTime();
        if (owner.lastSeenTime() != null
            && (mergedLastSeen == null || owner.lastSeenTime().after(mergedLastSeen)))
        {
          mergedLastSeen = owner.lastSeenTime();
        }
        byOrg.put(owner.organizationId(), new ComponentOrganizationUsageRow(
            owner.organizationId(), existing.applicationCount() + 1L, mergedLastSeen));
      }

      List<ComponentOrganizationUsageRow> filtered = byOrg.values()
          .stream()
          .sorted(Comparator
              .comparing(ComponentOrganizationUsageRow::lastSeenTime,
                  Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(ComponentOrganizationUsageRow::organizationId,
                  Comparator.nullsLast(String::compareTo)))
          .toList();
      return pageOrganizations(filtered, offset, limit);
    }
  }

  private static PagedOwnersByHash pageOwners(
      final List<ComponentOwnerUsageRow> filtered,
      final int offset,
      final int limit)
  {
    if (offset >= filtered.size()) {
      return new PagedOwnersByHash(filtered.size(), List.of());
    }
    int to = Math.min(offset + limit, filtered.size());
    return new PagedOwnersByHash(filtered.size(), filtered.subList(offset, to));
  }

  private static PagedOrganizationsByHash pageOrganizations(
      final List<ComponentOrganizationUsageRow> filtered,
      final int offset,
      final int limit)
  {
    if (offset >= filtered.size()) {
      return new PagedOrganizationsByHash(filtered.size(), List.of());
    }
    int to = Math.min(offset + limit, filtered.size());
    return new PagedOrganizationsByHash(filtered.size(), filtered.subList(offset, to));
  }

  /** Empty set is fail-closed; {@code null} is unrestricted (no owner filter). */
  private static boolean isFailClosedOwnerScope(final Set<String> ownerIds) {
    return ownerIds != null && ownerIds.isEmpty();
  }

  /**
   * Shared RBAC owner-scope: create {@code temporary_ids} when the IN list exceeds the PG parameter
   * threshold. Callers join that table when this returns {@code true}, otherwise AND
   * {@link #hashAndOwnerCondition}.
   */
  private boolean prepareOwnerTempTable(final TransactionContext tx, final Set<String> ownerIds) {
    return ownerIds != null && temporaryTableHelper.maybeCreateTemporaryTableWithIds(tx, ownerIds);
  }

  private static Condition hashAndOwnerCondition(
      final String hash,
      final Set<String> ownerIds,
      final boolean useTemporaryTable)
  {
    Condition where = OWNER_COMPONENT.HASH.eq(hash);
    if (ownerIds != null && !useTemporaryTable) {
      where = where.and(OWNER_COMPONENT.OWNER_ID.in(ownerIds));
    }
    return where;
  }

  private static String normalizedNameSearch(final String nameSearch) {
    if (StringUtils.isBlank(nameSearch)) {
      return null;
    }
    String normalized = NameHelper.normalize(nameSearch.trim());
    return StringUtils.isBlank(normalized) ? null : normalized;
  }

  private static Condition applicationUsageFilters(final String normalizedName, final String organizationId) {
    Condition condition = DSL.noCondition();
    if (StringUtils.isNotBlank(organizationId)) {
      condition = condition.and(APPLICATION.ORGANIZATION_ID.eq(organizationId.trim()));
    }
    if (StringUtils.isNotBlank(normalizedName)) {
      condition = condition.and(APPLICATION.NAME_LOWERCASE_NO_WHITESPACE.contains(normalizedName));
    }
    return condition;
  }

  private static Condition organizationUsageFilters(final String normalizedName) {
    if (!requiresOrganizationJoin(normalizedName)) {
      return DSL.noCondition();
    }
    return ORGANIZATION.NAME_LOWERCASE_NO_WHITESPACE.contains(normalizedName);
  }

  /** True when org-name filters require joining {@code organization}. */
  private static boolean requiresOrganizationJoin(final String normalizedName) {
    return StringUtils.isNotBlank(normalizedName);
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

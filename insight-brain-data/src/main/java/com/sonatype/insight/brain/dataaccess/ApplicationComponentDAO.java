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
import com.sonatype.insight.brain.model.ApplicationComponent;
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
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationComponent.APPLICATION_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligation.COMPONENT_OBLIGATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION;

/**
 * @since 1.11
 */
@Named
@Singleton
public class ApplicationComponentDAO
    extends AbstractOperationalSqlDAO<ApplicationComponent>
{
  /**
   * Compound key for looking up ApplicationComponent by application, stage, and hash.
   */
  public record ApplicationComponentKey(String applicationId, String stageTypeId, String hash)
  {
  }

  private static final int H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY = 350;

  private final TemporaryTableHelper temporaryTableHelper;

  @Inject
  public ApplicationComponentDAO(
      final OperationalDataStore operationalDataStore,
      final TemporaryTableHelper temporaryTableHelper)
  {
    super(operationalDataStore);
    this.temporaryTableHelper = temporaryTableHelper;
  }

  @Override
  public void update(TransactionContext tx, ApplicationComponent entity) {
    throw new UnsupportedOperationException("ApplicationComponent does not support update operations");
  }

  public List<ApplicationComponent> getByApplicationId(TransactionContext tx, String appId) {
    return tx.dsl()
        .selectFrom(APPLICATION_COMPONENT)
        .where(APPLICATION_COMPONENT.APPLICATION_ID.eq(appId))
        .fetch(this::toEntity);
  }

  public List<ApplicationComponent> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndStageTypeId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndStageTypeId(tx, appId, stageTypeId);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndStageTypeId(
      TransactionContext tx,
      String appId,
      String stageTypeId)
  {
    return tx.dsl()
        .selectFrom(APPLICATION_COMPONENT)
        .where(APPLICATION_COMPONENT.APPLICATION_ID.eq(appId))
        .and(APPLICATION_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
        .fetch(this::toEntity);
  }

  /**
   * Batch variant of {@link #getByApplicationIdAndStageTypeId(String, String)} that fetches the components for many
   * applications at once, avoiding a per-application query. Callers group the result by application id.
   */
  public List<ApplicationComponent> getByApplicationIdsAndStageTypeId(
      Set<String> applicationIds,
      String stageTypeId)
  {
    if (CollectionUtils.isEmpty(applicationIds)) {
      return List.of();
    }
    return getListWithSqlInClause(
        applicationIds,
        appIdChunk -> {
          try (TransactionContext tx = createTransactionContext()) {
            return tx.dsl()
                .selectFrom(APPLICATION_COMPONENT)
                .where(APPLICATION_COMPONENT.APPLICATION_ID.in(appIdChunk))
                .and(APPLICATION_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
                .fetch(this::toEntity);
          }
        },
        getDataStore());
  }

  // Bypasses per-entity delete() for performance. This DAO does not use a SearchIndexManager,
  // so no search index side effects are lost.
  public void deleteByApplicationIdAndStageTypeId(TransactionContext tx, String appId, String stageTypeId) {
    tx.dsl()
        .deleteFrom(APPLICATION_COMPONENT)
        .where(APPLICATION_COMPONENT.APPLICATION_ID.eq(appId))
        .and(APPLICATION_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
        .execute();
  }

  public ApplicationComponent getByApplicationIdAndStageTypeIdAndHash(String appId, String stageTypeId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(APPLICATION_COMPONENT)
          .where(APPLICATION_COMPONENT.APPLICATION_ID.eq(appId))
          .and(APPLICATION_COMPONENT.STAGE_TYPE_ID.eq(stageTypeId))
          .and(APPLICATION_COMPONENT.HASH.eq(hash))
          .fetchOne());
    }
  }

  public Map<ApplicationComponentKey, ApplicationComponent> getMapByApplicationIdsAndStageTypeIdsAndHashes(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      Set<String> hashes)
  {
    if (CollectionUtils.isEmpty(applicationIds) || CollectionUtils.isEmpty(stageTypeIds)
        || CollectionUtils.isEmpty(hashes))
    {
      return Map.of();
    }
    return getStreamWithSqlInClause(
        applicationIds,
        appIdChunk -> getStreamWithSqlInClause(
            hashes,
            hashChunk -> {
              try (TransactionContext tx = createTransactionContext()) {
                return tx.dsl()
                    .selectFrom(APPLICATION_COMPONENT)
                    .where(APPLICATION_COMPONENT.APPLICATION_ID.in(appIdChunk))
                    .and(APPLICATION_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
                    .and(APPLICATION_COMPONENT.HASH.in(hashChunk))
                    .fetch(this::toEntity)
                    .stream();
              }
            },
            getDataStore(),
            1,
            appIdChunk.size() + stageTypeIds.size()),
        getDataStore(),
        1,
        stageTypeIds.size() + 1) // +1 for minimum 1 hash in each inner IN-clause
            .collect(Collectors.toMap(
                component -> new ApplicationComponentKey(
                    component.getApplicationId(),
                    component.getStageTypeId(),
                    component.getHash()),
                component -> component));
  }

  public List<ApplicationComponent> getByApplicationIdAndHash(String appId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndHash(tx, appId, hash);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndHash(TransactionContext tx, String appId, String hash) {
    return tx.dsl()
        .selectFrom(APPLICATION_COMPONENT)
        .where(APPLICATION_COMPONENT.APPLICATION_ID.eq(appId))
        .and(APPLICATION_COMPONENT.HASH.eq(hash))
        .fetch(this::toEntity);
  }

  public List<ApplicationComponent> getByApplicationIdAndComponentIdentifier(
      String appId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndComponentIdentifier(tx, appId, componentIdentifier);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndComponentIdentifier(
      TransactionContext tx,
      String appId,
      ComponentIdentifier componentIdentifier)
  {
    return tx.dsl()
        .selectFrom(APPLICATION_COMPONENT)
        .where(APPLICATION_COMPONENT.APPLICATION_ID.eq(appId))
        .and(APPLICATION_COMPONENT.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetch(this::toEntity);
  }

  public ApplicationComponent getLastByHash(String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(APPLICATION_COMPONENT)
          .where(APPLICATION_COMPONENT.HASH.eq(hash))
          .orderBy(APPLICATION_COMPONENT.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public ApplicationComponent getLastByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(APPLICATION_COMPONENT)
          .where(APPLICATION_COMPONENT.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
          .and(APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON.eq(
              ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
          .orderBy(APPLICATION_COMPONENT.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public List<ApplicationComponent> getByApplicationIdsAndStageTypeIdsSince(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      Date date)
  {
    try (TransactionContext tx = createTransactionContext()) {
      if (applicationIds != null && applicationIds.size() >= getInOperatorThreshold()) {
        List<ApplicationComponent> applicationComponents = tx.dsl()
            .selectFrom(APPLICATION_COMPONENT)
            .where(APPLICATION_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .and(APPLICATION_COMPONENT.TIME.greaterOrEqual(date))
            .orderBy(APPLICATION_COMPONENT.TIME.asc())
            .fetch(this::toEntity);

        List<ApplicationComponent> retval = new ArrayList<>();
        for (ApplicationComponent applicationComponent : applicationComponents) {
          if (applicationIds.contains(applicationComponent.getApplicationId())) {
            retval.add(applicationComponent);
          }
        }
        return retval;
      }
      else {
        return tx.dsl()
            .selectFrom(APPLICATION_COMPONENT)
            .where(APPLICATION_COMPONENT.APPLICATION_ID.in(applicationIds))
            .and(APPLICATION_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .and(APPLICATION_COMPONENT.TIME.greaterOrEqual(date))
            .orderBy(APPLICATION_COMPONENT.TIME.asc())
            .fetch(this::toEntity);
      }
    }
  }

  public List<ApplicationComponent> getByApplicationIdsAndStageTypeIds(
      Set<String> applicationIds,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      if (applicationIds != null && applicationIds.size() >= getInOperatorThreshold()) {
        List<ApplicationComponent> applicationComponents = tx.dsl()
            .selectFrom(APPLICATION_COMPONENT)
            .where(APPLICATION_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .fetch(this::toEntity);

        List<ApplicationComponent> retval = new ArrayList<>();
        for (ApplicationComponent applicationComponent : applicationComponents) {
          if (applicationIds.contains(applicationComponent.getApplicationId())) {
            retval.add(applicationComponent);
          }
        }
        return retval;
      }
      else {
        return tx.dsl()
            .selectFrom(APPLICATION_COMPONENT)
            .where(APPLICATION_COMPONENT.APPLICATION_ID.in(applicationIds))
            .and(APPLICATION_COMPONENT.STAGE_TYPE_ID.in(stageTypeIds))
            .fetch(this::toEntity);
      }
    }
  }

  /**
   * Queries the combination of applications IDs and stage type IDs where the components found in the last evaluation
   * have a review of the license legal obligations already started or not.
   * <p>
   * A license legal obligations review is considered started in an application and stage type when there is a least one
   * entry for a component in {@ComponentObligation} whether at the application, organization or root organization scope
   * while a not started review is when there is not a single entry.
   *
   * @param applicationIds Applications IDs where the query can be made.
   * @param stageTypeIds Stage type IDs where the query can be made.
   * @param isReviewStarted {@code true} to query the applications and stage types where the review already started,
   *          {@code false} to query the ones where the review hasn't started.
   * @return A list of Object arrays with 2 positions: the application ID and the stage type ID.
   */
  public List<Object[]> getApplicationIdsAndStageTypeIdsByReviewStatus(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      boolean isReviewStarted)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var ac = APPLICATION_COMPONENT.as("ac");
      var a = APPLICATION.as("a");

      // Build EXISTS condition for application level
      Condition appExists = DSL.exists(
          DSL.selectOne()
              .from(COMPONENT_OBLIGATION)
              .where(COMPONENT_OBLIGATION.OWNER_ID.eq(ac.APPLICATION_ID))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(ac.COMPONENT_ID_FORMAT))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(ac.COMPONENT_ID_COORDINATES_JSON)));

      // Build EXISTS condition for organization level
      Condition orgExists = DSL.exists(
          DSL.selectOne()
              .from(COMPONENT_OBLIGATION)
              .where(COMPONENT_OBLIGATION.OWNER_ID.eq(a.ORGANIZATION_ID))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(ac.COMPONENT_ID_FORMAT))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(ac.COMPONENT_ID_COORDINATES_JSON)));

      // Build EXISTS condition for root organization level
      Condition rootExists = DSL.exists(
          DSL.selectOne()
              .from(COMPONENT_OBLIGATION)
              .where(COMPONENT_OBLIGATION.OWNER_ID.eq(Organization.ROOT_ORGANIZATION_ID))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(ac.COMPONENT_ID_FORMAT))
              .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(ac.COMPONENT_ID_COORDINATES_JSON)));

      // Combine conditions based on isReviewStarted
      Condition reviewCondition;
      if (isReviewStarted) {
        reviewCondition = appExists.or(orgExists).or(rootExists);
      }
      else {
        reviewCondition = DSL.not(appExists).and(DSL.not(orgExists)).and(DSL.not(rootExists));
      }

      boolean requiresManualFilter = requiresManualFilter(applicationIds);

      Condition baseCondition = a.APPLICATION_ID.eq(ac.APPLICATION_ID)
          .and(ac.STAGE_TYPE_ID.in(stageTypeIds))
          .and(reviewCondition);

      if (!requiresManualFilter) {
        baseCondition = baseCondition.and(ac.APPLICATION_ID.in(applicationIds));
      }

      List<Object[]> results = tx.dsl()
          .selectDistinct(ac.APPLICATION_ID, ac.STAGE_TYPE_ID)
          .from(ac, a)
          .where(baseCondition)
          .fetch(r -> new Object[]{r.value1(), r.value2()});

      if (requiresManualFilter) {
        return results.stream()
            .filter(array -> applicationIds.contains(array[0].toString()))
            .collect(Collectors.toList());
      }

      return results;
    }
  }

  public List<ApplicationComponentRisk> getComponentsRiskFiltered(
      Set<String> applicationIds,
      Set<String> stageTypes,
      Set<String> policyThreatCategoryFilter,
      Entry<Integer, Integer> policyThreatLevelFilter,
      Set<String> policyViolationStateFilter,
      String orderBy,
      int page,
      int pageSize)
  {
    if (!isDatabasePostgresql()) {
      throw new UnsupportedOperationException("This operation is only supported for PostgreSQL databases");
    }

    if (applicationIds.isEmpty()) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      boolean useTemporaryTable =
          temporaryTableHelper.maybeCreateTemporaryTableWithIds(tx, applicationIds);

      var pv = POLICY_VIOLATION.as("pv");

      // Build WHERE conditions using jOOQ
      Condition whereCondition = pv.FIX_TIME.isNull();

      if (!useTemporaryTable) {
        whereCondition = whereCondition.and(pv.APPLICATION_ID.in(applicationIds));
      }
      if (!stageTypes.isEmpty()) {
        whereCondition = whereCondition.and(pv.STAGE_TYPE_ID.in(stageTypes));
      }
      if (!policyThreatCategoryFilter.isEmpty()) {
        whereCondition = whereCondition.and(pv.THREAT_CATEGORY.in(policyThreatCategoryFilter));
      }
      if (policyThreatLevelFilter != null) {
        whereCondition = whereCondition.and(pv.THREAT_LEVEL.between(
            policyThreatLevelFilter.getKey().shortValue(),
            policyThreatLevelFilter.getValue().shortValue()));
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
              DSL.partitionBy(pv.APPLICATION_ID, pv.POLICY_ID, pv.CONSTRAINT_FACTS_ID,
                  pv.HASH, pv.COMPONENT_ID_FORMAT, pv.COMPONENT_ID_COORDINATES_JSON)
                  .orderBy(pv.OPEN_TIME.desc()))
          .as("rn");

      var subquery = useTemporaryTable
          ? tx.dsl()
              .select(pv.APPLICATION_ID, pv.POLICY_ID, pv.CONSTRAINT_FACTS_ID, pv.OPEN_TIME,
                  pv.THREAT_LEVEL, pv.HASH, pv.FILENAME, pv.COMPONENT_ID_FORMAT, pv.COMPONENT_ID_COORDINATES_JSON,
                  rowNum)
              .from(pv)
              .join(DSL.table("temporary_ids").as("ti"))
              .on(pv.APPLICATION_ID.eq(DSL.field("ti.id", String.class)))
              .where(whereCondition)
              .asTable("sub")
          : tx.dsl()
              .select(pv.APPLICATION_ID, pv.POLICY_ID, pv.CONSTRAINT_FACTS_ID, pv.OPEN_TIME,
                  pv.THREAT_LEVEL, pv.HASH, pv.FILENAME, pv.COMPONENT_ID_FORMAT, pv.COMPONENT_ID_COORDINATES_JSON,
                  rowNum)
              .from(pv)
              .where(whereCondition)
              .asTable("sub");

      var innerQuery = tx.dsl()
          .select(
              DSL.field("sub.application_id", String.class).as("application_id"),
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
      var applicationId = DSL.field("application_id", String.class);
      var threatLevel = DSL.field("threat_level", Short.class);

      // Parse orderBy string (e.g., "score DESC") into field and direction
      String[] orderByParts = orderBy.split(" ");
      String orderColumn = orderByParts[0];
      boolean orderDesc = orderByParts.length > 1 && "DESC".equalsIgnoreCase(orderByParts[1]);
      // Use DSL.name() to ensure the column name is properly quoted in PostgreSQL. Without quoting,
      // PostgreSQL lowercases identifiers, which won't match quoted aliases like "affectedApplications"
      var orderField = orderDesc ? DSL.field(DSL.name(orderColumn)).desc() : DSL.field(DSL.name(orderColumn)).asc();

      // Build outer query with aggregations
      var query = tx.dsl()
          .select(
              hash,
              filename,
              componentIdFormat,
              componentIdCoordinatesJson,
              DSL.countDistinct(applicationId).as("affectedApplications"),
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

  private boolean requiresManualFilter(Collection<?> items) {
    return isDatabaseEmbedded() && items.size() >= H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY;
  }

  @Override
  public Table<?> getJooqTable() {
    return APPLICATION_COMPONENT;
  }

  @Override
  public Class<ApplicationComponent> getEntityClass() {
    return ApplicationComponent.class;
  }
}

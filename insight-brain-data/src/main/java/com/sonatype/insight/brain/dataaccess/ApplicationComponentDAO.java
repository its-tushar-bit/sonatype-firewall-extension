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
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentRisk;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.11
 */
@Named
@Singleton
public class ApplicationComponentDAO
    extends AbstractOperationalSqlDAO<ApplicationComponent>
{
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
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
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
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2";
    return getList(tx, sQuery, appId, stageTypeId);
  }

  public ApplicationComponent getByApplicationIdAndStageTypeIdAndHash(String appId, String stageTypeId, String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 and entity.hash=?3";
    return get(sQuery, appId, stageTypeId, hash);
  }

  public List<ApplicationComponent> getByApplicationIdAndHash(String appId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndHash(tx, appId, hash);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndHash(TransactionContext tx, String appId, String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.hash=?2";
    return getList(tx, sQuery, appId, hash);
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
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.componentIdFormat=?2 AND entity.componentIdCoordinatesJson=?3";
    return getList(tx, sQuery, appId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public ApplicationComponent getLastByHash(String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.hash=?1" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, hash).forceSingleResult().get();
  }

  public ApplicationComponent getLastByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.componentIdFormat=?1 AND entity.componentIdCoordinatesJson=?2" +
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())).forceSingleResult().get();
  }

  public List<ApplicationComponent> getByApplicationIdsAndStageTypeIdsSince(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      Date date)
  {
    if (applicationIds != null && applicationIds.size() >= getInOperatorThreshold()) {
      String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
          " WHERE entity.stageTypeId IN (?1) AND entity.time >= ?2" + //
          " ORDER BY entity.time ASC";

      List<ApplicationComponent> applicationComponents = getList(sQuery, stageTypeIds, date);
      List<ApplicationComponent> retval = new ArrayList<>();

      for (ApplicationComponent applicationComponent : applicationComponents) {
        if (applicationIds.contains(applicationComponent.getApplicationId())) {
          retval.add(applicationComponent);
        }
      }

      return retval;
    }
    else {
      String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
          " WHERE entity.applicationId IN (?1) AND entity.stageTypeId IN (?2) AND entity.time >= ?3" + //
          " ORDER BY entity.time ASC";
      return getList(sQuery, applicationIds, stageTypeIds, date);
    }
  }

  public List<ApplicationComponent> getByApplicationIdsAndStageTypeIds(
      Set<String> applicationIds,
      Set<String> stageTypeIds)
  {
    if (applicationIds != null && applicationIds.size() >= getInOperatorThreshold()) {
      String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
          " WHERE entity.stageTypeId IN (?1)";

      List<ApplicationComponent> applicationComponents = getList(sQuery, stageTypeIds);
      List<ApplicationComponent> retval = new ArrayList<>();

      for (ApplicationComponent applicationComponent : applicationComponents) {
        if (applicationIds.contains(applicationComponent.getApplicationId())) {
          retval.add(applicationComponent);
        }
      }

      return retval;
    }
    else {
      String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
          " WHERE entity.applicationId IN (?1) AND entity.stageTypeId IN (?2)";
      return getList(sQuery, applicationIds, stageTypeIds);
    }
  }

  /**
   * Queries the combination of applications IDs and stage type IDs where the components found in the last evaluation
   * have a review of the license legal obligations already started or not.
   *
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
  @SuppressWarnings("unchecked")
  public List<Object[]> getApplicationIdsAndStageTypeIdsByReviewStatus(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      boolean isReviewStarted)
  {
    String reviewStartedCondition = isReviewStarted ? "" : " NOT ";
    String reviewStartedOperator = (isReviewStarted ? " OR " : " AND ") + reviewStartedCondition;

    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT DISTINCT ac.applicationId, ac.stageTypeId" + //
          " FROM ApplicationComponent ac, Application a" + //
          " WHERE a.id = ac.applicationId" + //
          " AND ac.stageTypeId IN (?1)" + //
          " AND (" + reviewStartedCondition + " EXISTS (SELECT 1" + //
          "                  FROM ComponentObligation co" + //
          "                  WHERE co.ownerId = ac.applicationId" + //
          "                  AND co.componentIdFormat = ac.componentIdFormat" + //
          "                  AND co.componentIdCoordinatesJson = ac.componentIdCoordinatesJson)" + //
          reviewStartedOperator + " EXISTS (SELECT 1" + //
          "                  FROM ComponentObligation co" + //
          "                  WHERE co.ownerId = a.organizationId" + //
          "                  AND co.componentIdFormat = ac.componentIdFormat" + //
          "                  AND co.componentIdCoordinatesJson = ac.componentIdCoordinatesJson)" + //
          reviewStartedOperator + " EXISTS (SELECT 1" + //
          "                  FROM ComponentObligation co" + //
          "                  WHERE co.ownerId = ?2" + //
          "                  AND co.componentIdFormat = ac.componentIdFormat" + //
          "                  AND co.componentIdCoordinatesJson = ac.componentIdCoordinatesJson))";

      boolean requiresManualFilter = requiresManualFilter(applicationIds);

      if (!requiresManualFilter) {
        sQuery += " AND ac.applicationId IN (?3)";
      }

      jakarta.persistence.Query query = tx.createQuery(sQuery);
      query.setParameter(1, stageTypeIds);
      query.setParameter(2, Organization.ROOT_ORGANIZATION_ID);

      if (!requiresManualFilter) {
        query.setParameter(3, applicationIds);
        return query.getResultList();
      }

      return ((Stream<Object[]>) query.getResultStream()).parallel()
          .filter(array -> applicationIds.contains(array[0].toString()))
          .collect(Collectors.toList());
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

      String applicationWhereClause = useTemporaryTable
          ? ""
          : " pv.application_id IN (?" + StringUtils.repeat(",?", applicationIds.size() - 1) + ")";
      String stageTypeWhereClause = stageTypes.isEmpty()
          ? ""
          : "pv.stage_type_id IN (?" + StringUtils.repeat(",?", stageTypes.size() - 1) + ")";
      String threatCategoryWhereClause = policyThreatCategoryFilter.isEmpty()
          ? ""
          : "pv.threat_category IN (?" + StringUtils.repeat(",?", policyThreatCategoryFilter.size() - 1) + ")";
      String threatLevelWhereClause = policyThreatLevelFilter == null
          ? ""
          : "pv.threat_level BETWEEN ? AND ?";
      String violationStateWhereClause;
      if (policyViolationStateFilter.isEmpty()) {
        violationStateWhereClause = "";
      }
      else if (policyViolationStateFilter.containsAll(List.of("WAIVED", "LEGACY_VIOLATION", "OPEN"))) {
        violationStateWhereClause = "";
      }
      else {
        violationStateWhereClause = "(" + StringUtils.joinWith(" OR ",
            policyViolationStateFilter.stream().map(state -> switch (state)
            {
              case "WAIVED" -> "pv.waive_time IS NOT NULL";
              case "LEGACY_VIOLATION" -> "pv.legacy_violation_time IS NOT NULL";
              case "OPEN" -> "(pv.waive_time IS NULL AND pv.legacy_violation_time IS NULL)";
              default -> throw new IllegalArgumentException("Invalid policy violation state: " + state);
            }).filter(StringUtils::isNotBlank).toArray()) + ")";
      }

      String whereClause = StringUtils.joinWith(" AND ",
          Stream.of(applicationWhereClause, stageTypeWhereClause, threatCategoryWhereClause, threatLevelWhereClause,
              violationStateWhereClause).filter(StringUtils::isNotBlank).toArray());

      String sQuery = """
          SELECT  hash, filename,
                component_id_format AS componentIdFormat,
                component_id_coordinates_json AS componentIdCoordinatesJson,
                COUNT(DISTINCT application_id) AS affectedApplications,
                SUM(threat_level) AS score,
                SUM(CASE  WHEN threat_level >= 8 THEN threat_level ELSE 0 END) AS scoreCritical,
                SUM(CASE  WHEN threat_level >= 4 AND threat_level < 8 THEN threat_level ELSE 0 END) AS scoreSevere,
                SUM(CASE  WHEN threat_level >= 2 AND threat_level < 4 THEN threat_level ELSE 0 END) AS scoreModerate,
                SUM(CASE  WHEN threat_level < 2 THEN threat_level ELSE 0 END) AS scoreLow
           FROM  (
            SELECT  DISTINCT ON (application_id, policy_id, constraint_facts_id, hash, component_id_format,
                      component_id_coordinates_json)
                  pv.application_id, pv.policy_id, pv.constraint_facts_id, pv.open_time, pv.threat_level, pv.hash,
                  pv.filename, pv.component_id_format, pv.component_id_coordinates_json
            FROM %s.policy_violation pv
            %s
            WHERE pv.fix_time ISNULL AND %s
            ORDER BY pv.application_id, pv.policy_id, pv.constraint_facts_id, hash,
               component_id_format, component_id_coordinates_json, open_time DESC
          ) x
           GROUP BY hash, filename, component_id_format, component_id_coordinates_json
           ORDER BY %s, hash ASC"""
          .formatted(
              getDatabaseSchema(),
              useTemporaryTable ? "JOIN temporary_ids ti ON pv.application_id = ti.id" : "",
              (whereClause.isEmpty() ? "" : whereClause),
              orderBy);

      if (pageSize < Integer.MAX_VALUE) {
        sQuery += " LIMIT " + (pageSize + 1);
        sQuery += " OFFSET " + pageSize * page;
      }

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);

      int i = 1;
      if (!useTemporaryTable) {
        for (String appId : applicationIds) {
          query.setParameter(i++, appId);
        }
      }
      for (String stageType : stageTypes) {
        query.setParameter(i++, stageType);
      }
      for (String threatCategory : policyThreatCategoryFilter) {
        query.setParameter(i++, threatCategory);
      }
      if (policyThreatLevelFilter != null) {
        query.setParameter(i++, policyThreatLevelFilter.getKey());
        query.setParameter(i++, policyThreatLevelFilter.getValue());
      }

      List<Object[]> results = query.getResultList();

      if (results.isEmpty()) {
        return Collections.emptyList();
      }

      return (results).stream()
          .parallel()
          .map(array -> new ApplicationComponentRisk(
              (String) array[0],
              (String) array[1],
              (String) array[2],
              (String) array[3],
              Long.valueOf((long) array[4]).intValue(),
              Long.valueOf((long) array[5]).intValue(),
              Long.valueOf((long) array[6]).intValue(),
              Long.valueOf((long) array[7]).intValue(),
              Long.valueOf((long) array[8]).intValue(),
              Long.valueOf((long) array[9]).intValue()))
          .toList();
    }
  }

  private boolean requiresManualFilter(Collection<?> items) {
    return isDatabaseEmbedded() && items.size() >= H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY;
  }
}

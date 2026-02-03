/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryTableHelper;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainer;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField.SortableField;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.containerimages.ContainerImagePolicyViolationSummaryDTO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomPolicyViolationSummaryDTO;
import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.11
 */
@Named
@Singleton
public class PolicyViolationDAO
    extends AbstractPolicyViolationDAO<PolicyViolation>
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationDAO.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  static final int DELETE_BATCH_SIZE = 100;

  private final TemporaryTableHelper temporaryTableHelper;

  @Inject
  public PolicyViolationDAO(
      OperationalDataStore operationalDataStore,
      PolicyEvaluationDAO policyEvaluationDAO,
      TemporaryTableHelper temporaryTableHelper,
      PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO)
  {
    super(operationalDataStore, policyViolationConstraintFactsDAO);
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.temporaryTableHelper = temporaryTableHelper;
  }

  public List<PolicyViolation> getByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1";
    return getList(sQuery, applicationId);
  }

  public List<PolicyViolation> getByIds(Set<String> ids) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIds(tx, ids);
    }
  }

  public List<PolicyViolation> getByIds(TransactionContext tx, Set<String> ids) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.id IN (?1)";
    return getListWithSqlInClause(ids, c -> getList(tx, sQuery, c));
  }

  public List<PolicyViolation> getByApplicationIdAndPolicyIdAndHash(
      String applicationId,
      String policyId,
      String hash)
  {
    StringBuilder sQueryBuilder = new StringBuilder();
    sQueryBuilder.append("SELECT entity FROM PolicyViolation entity");
    sQueryBuilder.append(" WHERE entity.applicationId=?1 AND entity.policyId=?2");

    if (hash == null) {
      sQueryBuilder.append(" AND entity.hash IS NULL");
      return getList(sQueryBuilder.toString(), applicationId, policyId);
    }
    else {
      sQueryBuilder.append(" AND entity.hash=?3");
      return getList(sQueryBuilder.toString(), applicationId, policyId, hash);
    }
  }

  public List<PolicyViolation> getUnfixedByApplicationIdAndStageId(String applicationId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getUnfixedByApplicationIdAndStageId(tx, applicationId, stageTypeId);
    }
  }

  public List<PolicyViolation> getUnfixedByApplicationIdAndStageId(
      TransactionContext tx,
      String applicationId,
      String stageTypeId)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " AND entity.fixTime IS NULL";
    return getList(tx, sQuery, applicationId, stageTypeId);
  }

  /**
   * Returns violations to enforce based on stage-specific semantics.
   * Firewall (proxy): Ignores legacy violations completely (treats as active).
   * Lifecycle (build/release): Excludes all legacy violations.
   */
  public List<PolicyViolation> getActiveByApplicationIdAndStageId(String applicationId, String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL";

    if (!ProxyStageType.ID.equals(stageTypeId)) {
      // Lifecycle: exclude legacy violations
      sQuery += " AND entity.legacyViolationTime IS NULL";
    }

    return getList(sQuery, applicationId, stageTypeId);
  }

  /**
   * Returns violations to enforce based on stage-specific semantics.
   * Firewall (proxy): Ignores legacy violations completely (treats as active).
   * Lifecycle (build/release): Excludes all legacy violations.
   */
  public List<PolicyViolation> getActiveByApplicationIdAndStageIdAndActionId(
      String applicationId,
      String stageTypeId,
      String actionTypeId)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.actionTypeId=?3" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL";

    if (!ProxyStageType.ID.equals(stageTypeId)) {
      // Lifecycle: exclude legacy violations
      sQuery += " AND entity.legacyViolationTime IS NULL";
    }

    return getList(sQuery, applicationId, stageTypeId, actionTypeId);
  }

  /**
   * Returns violations to enforce based on stage-specific semantics.
   * Firewall (proxy): Ignores legacy violations completely (treats as active).
   * Lifecycle (build/release): Excludes all legacy violations.
   */
  public List<PolicyViolation> getActiveByApplicationIdAndStageIdAndHash(
      String applicationId,
      String stageTypeId,
      String hash)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.hash=?3" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL";

    if (!ProxyStageType.ID.equals(stageTypeId)) {
      // Lifecycle: exclude legacy violations
      sQuery += " AND entity.legacyViolationTime IS NULL";
    }

    return getList(sQuery, applicationId, stageTypeId, hash);
  }

  public List<PolicyViolation> getUnfixedByApplicationIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    return getUnfixedByApplicationIdsOpenedAfterDate(applicationIds, minDate, false, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  public List<PolicyViolation> getActiveByApplicationIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    return getUnfixedByApplicationIdsOpenedAfterDate(applicationIds, minDate, true, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  private List<PolicyViolation> getUnfixedByApplicationIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Date minDate,
      boolean onlyActiveViolations,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + //
        " AND entity.openTime >= ?2" + //
        " AND entity.threatLevel >= ?3" + //
        " AND entity.threatLevel <= ?4" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.threatCategory IN (?5)" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds, minDate, minThreatLevel, maxThreatLevel, policyThreatCategories);
  }

  private String applicationIdString() {
    return isDatabaseEmbedded() ? "entity.applicationId=?1" : "entity.applicationId IN (?1)";
  }

  public List<PolicyViolation> getUnfixedByApplicationIds(Collection<String> applicationIds) {
    return getUnfixedByApplicationIds(applicationIds, false);
  }

  public List<PolicyViolation> getActiveByApplicationIds(Collection<String> applicationIds) {
    return getUnfixedByApplicationIds(applicationIds, true);
  }

  private List<PolicyViolation> getUnfixedByApplicationIds(
      Collection<String> applicationIds,
      boolean onlyActiveViolations)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL " : "") + //
        (onlyActiveViolations ? " AND entity.legacyViolationTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds);
  }

  public List<PolicyViolation> getUnfixedByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.fixTime IS NULL";
    return getList(tx, sQuery, applicationId);
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndStageIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.stageTypeId IN (?2)" + //
        " AND entity.openTime >= ?3" + //
        " AND entity.threatLevel >= ?4" + //
        " AND entity.threatLevel <= ?5" + //
        " AND entity.threatCategory IN (?6)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL";
    return getUnfixed(sQuery, applicationIds, stageTypeIds, minDate, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  public List<RepositoryResultsForImageContainer> getRepositoryResultsForImageContainer(
      Collection<String> repositoryIds, Collection<String> applicationIds,
      RepositoryResultsForImageContainerFilter detailsFilter)
  {
    if (detailsFilter.aggregate) {
      return getRepositoryResultsForImageContainerAggregate(repositoryIds, applicationIds, detailsFilter);
    }
    else {
      return getRepositoryResultsForImageContainerNonAggregate(repositoryIds, applicationIds, detailsFilter);
    }
  }

  private List<RepositoryResultsForImageContainer> getRepositoryResultsForImageContainerNonAggregate(
      Collection<String> repositoryIds, Collection<String> applicationIds,
      RepositoryResultsForImageContainerFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      int threatLevelFiltersSize =
          detailsFilter.threatLevelFilters != null ? detailsFilter.threatLevelFilters.size() : 0;
      int repositoryIdsSize = repositoryIds.size();
      // POLICY_NAME, QUARANTINE_TIME, OBJECT_NAME
      // are the only possible search filters used in the Query (as of May 2025)
      int repositoryIdsParamStartPosition = 1;
      int threatLevelFiltersParamStartPosition = repositoryIdsSize + 1;
      int searchFiltersParamStartPosition = repositoryIdsSize + threatLevelFiltersSize + 1;

      List<PolicyEvaluation> policyEvalList =
          policyEvaluationDAO.getLastByApplicationIdsAndStageIds(applicationIds.stream().collect(Collectors.toSet()),
              Set.of(Stage.ID_PROXY));

      if (policyEvalList.isEmpty()) {
        return new ArrayList<>();
      }

      Map<String, String> applicationIdsToScanIdMap = policyEvalList.stream()
          .collect(Collectors.toMap(
              PolicyEvaluation::getApplicationId, // Key: applicationId
              PolicyEvaluation::getScanId        // Value: scanId
          ));

      String baseQuery = "SELECT threat_level," + //
          " policy_name as policy," + //
          " app.name as object," + //
          " CASE WHEN pv.waive_time IS NOT NULL THEN NULL" + //
          " WHEN pv.action_type_id = 'fail' THEN pv.open_time ELSE NULL END as quarantine_time," + //
          " app.application_id," + //
          " app.public_id" + //
          " FROM " + getDatabaseSchema() + ".organization org JOIN " + getDatabaseSchema() + ".application app" +
          " ON org.organization_id = app.organization_id" + //
          " INNER JOIN " + getDatabaseSchema() + ".last_policy_evaluation lpe" + //
          " ON lpe.application_id = app.application_id" + //
          " INNER JOIN " + getDatabaseSchema() + ".policy_evaluation pe" + //
          " ON lpe.policy_evaluation_id = pe.policy_evaluation_id" + //
          ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") +
          " " + getDatabaseSchema() + ".policy_violation pv" + //
          " ON app.application_id = pv.application_id AND pv.stage_type_id = 'proxy'" + //
          " WHERE related_repository_id IN "
          + buildPositionalParameters(repositoryIds, repositoryIdsParamStartPosition) + //
          " AND pv.fix_time IS NULL";

      StringBuilder sQuery = new StringBuilder(baseQuery);

      sQuery.append(addThreatLevelFilters(detailsFilter.threatLevelFilters, threatLevelFiltersParamStartPosition));

      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      sQuery.append(addSearchFilters(detailsFilter.searchFilters, searchFiltersParamStartPosition));

      sQuery.append(validateAndAddSortFields(detailsFilter.sortFields));

      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery.toString());
      addPositionalParameters(query, repositoryIds, repositoryIdsParamStartPosition);

      if (detailsFilter.threatLevelFilters != null && detailsFilter.threatLevelFilters.size() == 2) {
        query.setParameter(threatLevelFiltersParamStartPosition, detailsFilter.threatLevelFilters.get(0));
        query.setParameter(threatLevelFiltersParamStartPosition + 1, detailsFilter.threatLevelFilters.get(1));
      }
      query.setParameter(searchFiltersParamStartPosition, '%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      query.setParameter(searchFiltersParamStartPosition + 1,
          '%' + detailsFilter.searchFilters.get("QUARANTINE_TIME") + '%');
      query.setParameter(searchFiltersParamStartPosition + 2,
          '%' + detailsFilter.searchFilters.get("OBJECT_NAME") + '%');
      query.setFirstResult(offset).setMaxResults(detailsFilter.pageSize +
          1); // Incremented page size to help UI determine whether to enable / disable NextPage button

      List<RepositoryResultsForImageContainer> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new RepositoryResultsForImageContainer(getInteger(array[0]), (String) array[1], null,
              (String) array[2],
              array[3] == null ? null : new Date(((Timestamp) array[3]).getTime()),
              applicationIdsToScanIdMap.get(array[4]), (String) array[5]))
          .collect(Collectors.toList());

      return results;
    }
  }

  private boolean hasNonViolatingFilter(final Set<String> violationStateFilters) {
    return violationStateFilters.stream()
        .anyMatch(filter -> filter.equals("VIOLATION_STATE_ALL") || filter.equals("VIOLATION_STATE_NOT_VIOLATING"));
  }

  protected List<RepositoryResultsForImageContainer> getRepositoryResultsForImageContainerAggregate(
      Collection<String> repositoryIds, Collection<String> applicationIds,
      RepositoryResultsForImageContainerFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      int repositoryIdsSize = repositoryIds.size();
      int threatLevelFiltersSize =
          detailsFilter.threatLevelFilters != null ? detailsFilter.threatLevelFilters.size() : 0;
      // POLICY_NAME, QUARANTINE_TIME, OBJECT_NAME
      // are the only possible search filters used in the Query (as of May 2025)
      int repositoryIdsParamStartPosition = 1;
      int threatLevelFiltersParamStartPosition = repositoryIdsSize + 1;
      int searchFiltersParamStartPosition = repositoryIdsSize + threatLevelFiltersSize + 1;
      int pageSize = detailsFilter.pageSize + 1;
      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;

      List<PolicyEvaluation> policyEvalList =
          policyEvaluationDAO.getLastByApplicationIdsAndStageIds(applicationIds.stream().collect(Collectors.toSet()),
              Set.of(Stage.ID_PROXY));

      if (policyEvalList.isEmpty()) {
        return new ArrayList<>();
      }
      Map<String, String> applicationIdsToScanIdMap = policyEvalList.stream()
          .collect(Collectors.toMap(
              PolicyEvaluation::getApplicationId, // Key: applicationId
              PolicyEvaluation::getScanId        // Value: scanId
          ));

      String baseQuery = "SELECT max(threat_level) as threat_level," + //
              " COUNT(CASE WHEN (threat_level >= 2) THEN 1 END) as violation_count," + //
              " app.name as object," + //
              " max(CASE WHEN pv.waive_time IS NOT NULL THEN NULL" + //
              " WHEN pv.action_type_id = 'fail' THEN pv.open_time ELSE NULL END) as quarantine_time," + //
              " app.application_id," + //
              " app.public_id" + //
              " FROM " + getDatabaseSchema() + ".organization org JOIN " + getDatabaseSchema() + ".application app" +
              " ON org.organization_id = app.organization_id" + //
              ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") +
              " " + getDatabaseSchema() + ".policy_violation pv" + //
              " ON app.application_id = pv.application_id AND pv.stage_type_id = 'proxy'" + //
              " INNER JOIN " + getDatabaseSchema() + ".last_policy_evaluation lpe" + //
              " ON lpe.application_id = app.application_id" + //
              " INNER JOIN " + getDatabaseSchema() + ".policy_evaluation pe" + //
              " ON lpe.policy_evaluation_id = pe.policy_evaluation_id" + //
              " WHERE related_repository_id IN" + //
              buildPositionalParameters(repositoryIds, repositoryIdsParamStartPosition) +
              addThreatLevelFilters(detailsFilter.threatLevelFilters, threatLevelFiltersParamStartPosition) +
              addViolationStateFilters(detailsFilter.violationStateFilters) +
              addSearchFilters(detailsFilter.searchFilters, searchFiltersParamStartPosition) +
              " AND pv.fix_time IS NULL" + //
              " GROUP BY app.application_id, app.name" + //
              addPolicyViolationCountForHavingClause(detailsFilter.searchFilters, searchFiltersParamStartPosition) +
              validateAndAddSortFields(detailsFilter.sortFields) +
              " LIMIT " + pageSize +
              " OFFSET " + offset;

      jakarta.persistence.Query query = tx.createNativeQuery(baseQuery);
      addPositionalParameters(query, repositoryIds, repositoryIdsParamStartPosition);
      if (detailsFilter.threatLevelFilters != null && detailsFilter.threatLevelFilters.size() == 2) {
        query.setParameter(threatLevelFiltersParamStartPosition, detailsFilter.threatLevelFilters.get(0));
        query.setParameter(threatLevelFiltersParamStartPosition + 1, detailsFilter.threatLevelFilters.get(1));
      }
      query.setParameter(searchFiltersParamStartPosition, detailsFilter.searchFilters
          .get("VIOLATION_COUNT"));
      query.setParameter(searchFiltersParamStartPosition + 1,
          '%' + detailsFilter.searchFilters.get("QUARANTINE_TIME") + '%');
      query.setParameter(searchFiltersParamStartPosition + 2,
          '%' + detailsFilter.searchFilters.get("OBJECT_NAME") + '%');

      List<RepositoryResultsForImageContainer> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new RepositoryResultsForImageContainer(
              getInteger(array[0]),
              null,
              getInteger(array[1]),
              (String) array[2],
              array[3] == null ? null : new Date(((Timestamp) array[3]).getTime()),
              applicationIdsToScanIdMap.get(array[4]),
              (String) array[5]
          )).collect(Collectors.toList());
      return results;
    }
  }

  private static String addViolationStateFilters(Set<String> filters) {
    StringBuilder query = new StringBuilder();
    int filterCount = 0;
    if (!CollectionUtils.isEmpty(filters)) {
      for (String filter : filters) {
        filterCount++;
        switch (filter) {
          case "VIOLATION_STATE_ALL":
            return "";
          case "VIOLATION_STATE_NOT_VIOLATING":
            query.append(filterCount > 1 ? " OR" : " AND (");
            query.append(" pv.policy_name IS NULL");
            break;
          case "VIOLATION_STATE_OPEN":
            query.append(filterCount > 1 ? " OR" : " AND (");
            query.append(" pv.waive_time IS NULL");
            break;
          case "VIOLATION_STATE_QUARANTINED":
            query.append(filterCount > 1 ? " OR" : " AND (");
            query.append(
                " (pv.open_time IS NOT NULL AND pv.action_type_id = 'fail' AND pv.waive_time IS NULL)");
            break;
          case "VIOLATION_STATE_WAIVED":
            query.append(filterCount > 1 ? " OR" : " AND (");
            query.append(" pv.waive_time IS NOT NULL");
            break;
          default:
        }
      }
      query.append(" )");
    }

    return query.toString();
  }

  private static String addThreatLevelFilters(List<Integer> filters, int paramStartPosition) {
    if (filters != null && filters.size() == 2 && (filters.get(0) > 0 || filters.get(1) < 10)) {
      if (filters.get(0) == 0) {
        return " AND (pv.threat_level IS NULL OR" +
            " (pv.threat_level >= ?" + paramStartPosition + " AND pv.threat_level <= ?" +
            (++paramStartPosition) + "))";
      }
      return " AND pv.threat_level >= ?" + paramStartPosition + " AND pv.threat_level <= ?" +
          (++paramStartPosition);
    }
    return "";
  }

  private static String addSearchFilters(Map<String, String> filters, int paramStartPosition) {
    StringBuilder query = new StringBuilder();
    if (!MapUtils.isEmpty(filters)) {
      for (Entry<String, String> filter : filters.entrySet()) {
        if (filter.getKey().equals("POLICY_NAME")) {
          query.append(" AND LOWER(pv.policy_name) LIKE ?" + paramStartPosition);
        }
        if (filter.getKey().equals("QUARANTINE_TIME")) {
          query.append(" AND (pv.open_time IS NOT NULL AND TO_CHAR(pv.open_time, 'YYYY-MM-DD') LIKE ?" +
              (paramStartPosition + 1) + ")");
        }
        if (filter.getKey().equals("OBJECT_NAME")) {
          query.append(" AND LOWER(app.name) LIKE ?" + (paramStartPosition + 2));
        }
      }
    }

    return query.toString();
  }

  private static String addPolicyViolationCountForHavingClause(Map<String, String> filters, int paramStartPosition) {
    StringBuilder query = new StringBuilder();
    if (!MapUtils.isEmpty(filters)) {
      for (Entry<String, String> filter : filters.entrySet()) {
        if (filter.getKey().equals("VIOLATION_COUNT")) {
          query.append(" HAVING violation_count = ?" + (paramStartPosition));
        }
      }
    }

    return query.toString();
  }

  private String validateAndAddSortFields(final List<SortField> sortFields) {
    StringBuilder query = new StringBuilder();
    List<String> result = new ArrayList<>();
    if (!CollectionUtils.isEmpty(sortFields)) {
      sortFields.sort(Comparator.comparing(field -> field.sortPriority));
      Set<Integer> sortPriorities = new HashSet<>();
      for (SortField sortField : sortFields) {
        if (sortPriorities.contains(sortField.sortPriority)) {
          throw new BadRequestException("sort priority cannot be the same for different fields");
        }
        if (sortField.asc) {
          result.add(getSortField(sortField.sortableField) + " NULLS LAST");
        }
        else {
          result.add(getSortField(sortField.sortableField) + " DESC NULLS LAST");
        }
        sortPriorities.add(sortField.sortPriority);
      }
      query.append(" ORDER BY ").append(StringUtils.join(result, ", "));
    }

    return query.toString();
  }

  private String getSortField(SortableField field) {
    switch (field) {
      case POLICY_THREAT_LEVEL:
        return "threat_level";
      case POLICY_NAME:
        return "policy_name";
      case QUARANTINE_TIME:
        return "quarantine_time";
      case OBJECT_NAME:
        return "app.name";
      case VIOLATION_COUNT:
        return "violation_count";
      default:
        return "";
    }
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndStageIds(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.stageTypeId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.threatLevel >= ?3" + //
        " AND entity.threatLevel <= ?4" + //
        " AND entity.threatCategory IN (?5)" + //
        " AND entity.waiveTime IS NULL" + //
        " AND entity.legacyViolationTime IS NULL";

    return getUnfixed(sQuery, applicationIds, stageTypeIds, minThreatLevel, maxThreatLevel, policyThreatCategories);
  }

  public List<PolicyViolation> getUnfixedBy(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories,
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.stageTypeId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.threatLevel >= ?3" + //
        " AND entity.threatLevel <= ?4" + //
        " AND entity.threatCategory IN (?5)";

    sQuery += getPolicyStateFilter(violationStateOpen, violationStateWaived, violationStateLegacyViolation);

    return getUnfixed(sQuery, applicationIds, stageTypeIds, minThreatLevel, maxThreatLevel, policyThreatCategories);
  }

  public List<PolicyViolation> getUnfixedBy(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories,
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + "AND entity.stageTypeId IN (?2)" + //
        " AND entity.openTime >= ?3" + //
        " AND entity.threatLevel >= ?4" + //
        " AND entity.threatLevel <= ?5" + //
        " AND entity.threatCategory IN (?6)" + //
        " AND entity.fixTime IS NULL";

    sQuery += getPolicyStateFilter(violationStateOpen, violationStateWaived, violationStateLegacyViolation);

    return getUnfixed(sQuery, applicationIds, stageTypeIds, minDate, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  private String getPolicyStateFilter(
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    String policyStateFilter = "";

    violationStateOpen = violationStateOpen == null || violationStateOpen;
    violationStateWaived = violationStateWaived == null || violationStateWaived;
    violationStateLegacyViolation = violationStateLegacyViolation == null || violationStateLegacyViolation;

    if (!violationStateOpen && !violationStateWaived && !violationStateLegacyViolation) {
      return policyStateFilter;
    }

    if (!violationStateOpen || !violationStateWaived || !violationStateLegacyViolation) {
      policyStateFilter += " AND (";

      List<String> stateQuery = new ArrayList<>();
      if (violationStateOpen) {
        stateQuery.add("(entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL)");
      }
      if (violationStateWaived) {
        stateQuery.add("entity.waiveTime IS NOT NULL");
      }
      if (violationStateLegacyViolation) {
        stateQuery.add("entity.legacyViolationTime IS NOT NULL");
      }
      policyStateFilter += StringUtils.join(stateQuery.toArray(), " OR ");
      policyStateFilter += ")";
    }

    return policyStateFilter;
  }

  private String getPolicyStateFilterForNativeQuery(
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    String policyStateFilter = "";

    violationStateOpen = violationStateOpen == null || violationStateOpen;
    violationStateWaived = violationStateWaived == null || violationStateWaived;
    violationStateLegacyViolation = violationStateLegacyViolation == null || violationStateLegacyViolation;

    if (!violationStateOpen && !violationStateWaived && !violationStateLegacyViolation) {
      return policyStateFilter;
    }

    if (!violationStateOpen || !violationStateWaived || !violationStateLegacyViolation) {
      policyStateFilter += "    AND (";

      List<String> stateQuery = new ArrayList<>();
      if (violationStateOpen) {
        stateQuery.add("(waive_time IS NULL AND legacy_violation_time IS NULL)");
      }
      if (violationStateWaived) {
        stateQuery.add("waive_time IS NOT NULL");
      }
      if (violationStateLegacyViolation) {
        stateQuery.add("legacy_violation_time IS NOT NULL");
      }
      policyStateFilter += StringUtils.join(stateQuery.toArray(), " OR ");
      policyStateFilter += ")\n";
    }

    return policyStateFilter;
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndPolicyIds(
      Collection<String> applicationIds,
      Collection<String> policyIds,
      Date openTimeAfter,
      Date openTimeBefore)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.policyId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.waiveTime IS NULL" + //
        " AND entity.legacyViolationTime IS NULL";

    return loadPolicyViolationsWithDateFilters(applicationIds, policyIds, openTimeAfter, openTimeBefore, sQuery);
  }

  private List<PolicyViolation> getUnfixed(
      String sQuery,
      Collection<String> applicationIds,
      Object... otherParameters)
  {
    if (isDatabaseEmbedded()) {
      // H2 won't utilize the index for the application id when the query uses an IN operator with multiple values (and
      // has additional filter criteria like the fix_time), doing an expensive table scan instead.
      // So we make one query per app to ensure the index is used (and all the fixed violations aren't scanned).
      TenantAwareFunction<String, List<PolicyViolation>> tenantAwareFunction =
          new TenantAwareFunction<>(applicationId -> {
            Object[] parameters = new Object[otherParameters.length + 1];
            System.arraycopy(otherParameters, 0, parameters, 1, otherParameters.length);
            parameters[0] = applicationId;
            return getList(sQuery, parameters);
          });
      return CompletableFuture.supplyAsync(
          new TenantAwareSupplier<>(() -> applicationIds.stream()
              .parallel()
              .map(tenantAwareFunction)
              .flatMap(Collection::stream)
              .collect(toList())),
          ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.DAO)).join();
    }
    else if (!applicationIds.isEmpty()) {
      return getListWithSqlInClause(applicationIds, appIds -> {
        Object[] parameters = new Object[otherParameters.length + 1];
        System.arraycopy(otherParameters, 0, parameters, 1, otherParameters.length);
        parameters[0] = appIds;
        return getList(sQuery, parameters);
      });
    }
    else {
      return Collections.emptyList();
    }
  }

  public List<PolicyViolation> getActiveByApplicationIdAndStageIdsAndTimeRange(
      String appId,
      Collection<String> stageTypeIds,
      Date from,
      Date to)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId = ?1 AND entity.stageTypeId IN (?2)" + //
        " AND (" + //
        "   (entity.openTime >= ?3 AND entity.openTime < ?4" + // opened during time range
        "    AND (entity.waiveTime > entity.openTime OR entity.waiveTime IS NULL) " + // not immediately waived
        "    AND (entity.legacyViolationTime > entity.openTime OR entity.legacyViolationTime IS NULL)) " +
        "   OR " + //
        "   (entity.openTime < ?3 " + // opened before time range
        "    AND CASE WHEN entity.fixTime <= ?3 THEN false" + // not fixed before time range
        "             WHEN entity.waiveTime <= ?3 THEN false" + // not waived before time range
        "             WHEN entity.legacyViolationTime <= ?3 THEN false" + // not legacy status before time range
        "             ELSE true " + //
        "        END = true))";
    return getList(sQuery, appId, stageTypeIds, from, to);
  }

  public List<PolicyViolation> getUnfixedLegacyViolationByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.fixTime IS NULL AND entity.legacyViolationTime IS NOT NULL";
    return getList(tx, sQuery, appId);
  }

  public int replacePolicyId(String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?2" + //
        " WHERE entity.policyId=?1";
    return createQuery(sQuery, fromPolicyId, toPolicyId).executeUpdate();
  }

  public int replacePolicyId(TransactionContext tx, String applicationId, String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?3" + //
        " WHERE entity.applicationId=?1 AND entity.policyId=?2";
    return createQuery(sQuery, applicationId, fromPolicyId, toPolicyId).executeUpdate(tx);
  }

  public int deleteFixedByApplicationIdAndDate(String applicationId, Date fixedBefore) {
    // For performance reasons, we bypass the standard delete (per entity) method here.
    if (isDatabaseEmbedded()) {
      // Deleting a potentially huge number of records from H2 in one shot consumes a lot of heap and blocks any other
      // database operation for a long time. To avoid this, we split the entire delete up into smaller batches.
      // See https://issues.sonatype.org/browse/CLM-15723 for details
      String sQuery = "SELECT entity.id FROM PolicyViolation entity" + //
          " WHERE entity.applicationId = ?1 AND entity.fixTime < ?2";
      int deletedRows = 0;
      while (true) {
        List<String> ids =
            new Query<String>(sQuery, applicationId, fixedBefore).setMaxResults(DELETE_BATCH_SIZE).getList();
        if (ids.isEmpty()) {
          return deletedRows;
        }
        deletedRows += createQuery("DELETE FROM PolicyViolation entity WHERE entity.id IN (?1)", ids).executeUpdate();
      }
    }
    else {
      // We cannot do this for H2 until we upgrade to a multi-threaded H2 version.
      // See https://issues.sonatype.org/browse/CLM-15723 for details
      return createQuery("DELETE FROM PolicyViolation entity WHERE entity.applicationId = ?1 AND entity.fixTime < ?2",
          applicationId, fixedBefore).executeUpdate();
    }
  }

  @Override
  public final void delete(PolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for an application.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, PolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for an application.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(tx, entity);
  }

  public int getCountApplicationsWithPolicyActionFailures(final String stageTypeId) {
    final String sQuery = "SELECT COUNT(DISTINCT entity.applicationId)" +
        " FROM PolicyViolation entity" +
        " WHERE entity.stageTypeId = ?1" +
        " AND entity.fixTime IS NULL" +
        " AND entity.waiveTime IS NULL" +
        String.format(" AND entity.actionTypeId = '%s'", Action.ID_FAIL);
    return getSingle(Number.class, sQuery, stageTypeId).intValue();
  }

  public int getCountActiveWaivers() {
    final String sQuery = "SELECT COUNT(entity.id)" +
        " FROM PolicyViolation entity" +
        " WHERE entity.waiveTime IS NOT NULL" +
        " AND entity.fixTime IS NULL";
    return getSingle(Number.class, sQuery).intValue();
  }

  /**
   * This method streams the policy violations that were either still open at the cutoff date or were created, waived,
   * fixed or resolved as legacy since the cutoff date.  IOW, we ignore any policy violations that were resolved in some
   * fashion before the cutoff date.
   *
   * @param cutoffDate the cutoff date
   * @param batchSize  number of rows to process in a batch
   * @param consumer   the consumer to accept the policy violations
   */
  public void consumePolicyViolationsSinceDate(Date cutoffDate, int batchSize, Consumer<PolicyViolation> consumer)
      throws SQLException
  {
    log.debug("Starting to consume policy violations (cutoffDate={}, batchSize={}).", cutoffDate, batchSize);

    long start = System.currentTimeMillis();

    String databaseSchema = getDatabaseSchema();
    // Since (Repository)PolicyViolationConstraintFactsJsonAsyncDbMigration runs async, there is a time window where the
    // system has to be able to use migrated and unmigrated policy violations.
    // This means unmigrated policy violations contain their constraint facts in the policy_violation record and
    // migrated policy violations in the policy_violation_constraint_facts table.
    String sQuery = String.format("""
        SELECT
          pv.policy_violation_id,
          pv.application_id,
          pv.policy_id,
          pv.policy_name,
          pv.stage_type_id,
          pv.open_time,
          pv.fix_time,
          pv.waive_time,
          pv.legacy_violation_time,
          pv.threat_level,
          pv.threat_category,
          pv.component_id_format,
          pv.component_id_coordinates_json,
          pv.hash,
          pv.filename,
          pv.constraint_facts_id,
          coalesce(cf.constraint_facts_json, pv.constraint_facts_json) constraint_facts_json
        FROM %s.policy_violation pv
          LEFT JOIN %s.policy_violation_constraint_facts cf
          ON (pv.constraint_facts_id = cf.policy_violation_constraint_facts_id)
        WHERE
          pv.policy_violation_id > ?
          AND (
            (pv.fix_time IS NULL AND pv.waive_time IS NULL AND pv.legacy_violation_time IS NULL)
            OR pv.open_time > ?
            OR pv.fix_time > ?
            OR pv.waive_time > ?
            OR pv.legacy_violation_time > ?
          )
        ORDER BY pv.policy_violation_id ASC
        LIMIT ?
        """, databaseSchema, databaseSchema);

    // Empty string is smaller than any string
    String lastProcessedViolationId = "";
    int processedRecordCount = 0;
    boolean inProgress = true;
    while (inProgress) {
      inProgress = false;
      List<PolicyViolation> policyViolations = new ArrayList<>();
      try (Connection connection = getDataStore().getDataSource().getConnection();
           PreparedStatement statement = connection.prepareStatement(sQuery)) {
        statement.setString(1, lastProcessedViolationId);
        statement.setDate(2, new java.sql.Date(cutoffDate.getTime()));
        statement.setDate(3, new java.sql.Date(cutoffDate.getTime()));
        statement.setDate(4, new java.sql.Date(cutoffDate.getTime()));
        statement.setDate(5, new java.sql.Date(cutoffDate.getTime()));
        statement.setInt(6, batchSize);

        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            inProgress = true;
            PolicyViolation policyViolation = new PolicyViolation();
            lastProcessedViolationId = resultSet.getString("policy_violation_id");
            policyViolation.setId(lastProcessedViolationId);
            policyViolation.setApplicationId(resultSet.getString("application_id"));
            policyViolation.setPolicyId(resultSet.getString("policy_id"));
            policyViolation.setPolicyName(resultSet.getString("policy_name"));
            policyViolation.setStageTypeId(resultSet.getString("stage_type_id"));
            policyViolation.setOpenTime(resultSet.getTimestamp("open_time"));
            policyViolation.setFixTime(resultSet.getTimestamp("fix_time"));
            policyViolation.setWaiveTime(resultSet.getTimestamp("waive_time"));
            policyViolation.setLegacyViolationTime(resultSet.getTimestamp("legacy_violation_time"));
            policyViolation.setThreatLevel(resultSet.getInt("threat_level"));

            String threatCategory = resultSet.getString("threat_category");
            if (StringUtils.isNotBlank(threatCategory)) {
              policyViolation.setThreatCategory(
                  PolicyThreatCategory.getByName(resultSet.getString("threat_category").toLowerCase()));
            }

            String format = resultSet.getString("component_id_format");
            String coordinates = resultSet.getString("component_id_coordinates_json");
            if (!StringUtils.isAnyBlank(format, coordinates)) {
              policyViolation.setComponentIdentifier( //
                  ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier( //
                      format, //
                      coordinates));
            }
            policyViolation.setHash(resultSet.getString("hash"));
            policyViolation.setFilename(resultSet.getString("filename"));

            String constraintFactsId = resultSet.getString("constraint_facts_id");
            if (!StringUtils.isBlank(constraintFactsId)) {
              policyViolation.setConstraintFactsId(constraintFactsId);
            }
            policyViolation.setDeprecatedConstraintFactsJson(resultSet.getString("constraint_facts_json"));

            policyViolations.add(policyViolation);
          }
        }
      }
      policyViolations.forEach(consumer::accept);
      processedRecordCount += policyViolations.size();

      // Allow the system to pick up other work
      try {
        Thread.sleep(50);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    // tell the consumer we're done now
    consumer.accept(null);

    log.debug("Consumed {} policy violations (cutoffDate={}, batchSize={}) in {} ms.", processedRecordCount, cutoffDate,
        batchSize, System.currentTimeMillis() - start);
  }

  public List<PolicyViolation> getWaivedFixed() {
    final String sQuery = "SELECT entity" +
        " FROM PolicyViolation entity" +
        " WHERE entity.fixTime IS NOT NULL" +
        " OR entity.waiveTime IS NOT NULL";
    return getList(sQuery);
  }

  public Map<String, SbomPolicyViolationSummaryDTO> getSbomPolicyViolationSummaryForAnApplication(
      Collection<String> applicationIds)
  {
    String sQuery = "" + //
        "SELECT application_id," +
        " COUNT(CASE WHEN (threat_level >= 8) THEN 1 END) AS policyViolationCritical," + //
        " COUNT(CASE WHEN (threat_level >= 4 and threat_level < 8) THEN 1 END) AS policyViolationSevere," + //
        " COUNT(CASE WHEN (threat_level >= 2 and threat_level < 4) THEN 1 END) AS policyViolationModerate," + //
        " COUNT(CASE WHEN (threat_level < 2) THEN 1 END) AS policyViolationLow" + //
        " FROM " + getDatabaseSchema() + ".policy_violation" + //
        " WHERE fix_time is null" + //
        " AND waive_time is null" + //
        " AND stage_type_id = ?1" + //
        " AND application_id = ANY(array[?2])" + //
        " GROUP BY application_id";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery,
          ComplianceStageType.ID, createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));

      Map<String, SbomPolicyViolationSummaryDTO> applicationIdResultMap = new HashMap<>();

      List<Object[]> resultStreamList = (List<Object[]>) query.getResultStream().collect(Collectors.toList());
      for (Object[] result : resultStreamList) {
        applicationIdResultMap.put(String.valueOf(result[0]), new SbomPolicyViolationSummaryDTO(result));
      }

      return applicationIdResultMap;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ContainerImagePolicyViolationSummaryDTO getContainerImagePolicyViolationSummaryForRepository(
          String repositoryId)
  {
    String sQuery = "" + //
        "SELECT " + " COUNT(CASE WHEN (threat_level >= 8) THEN 1 END) AS policyViolationCritical," + //
        " COUNT(CASE WHEN (threat_level >= 4 and threat_level < 8) THEN 1 END) AS policyViolationSevere," + //
        " COUNT(CASE WHEN (threat_level >= 2 and threat_level < 4) THEN 1 END) AS policyViolationModerate," + //
        " COUNT(DISTINCT CASE WHEN pv.threat_level > 1 THEN pv.application_id END) AS affectedContainers," + //
        " COUNT(DISTINCT CASE WHEN pv.action_type_id = ?1 THEN pv.application_id END) AS containersInQuarantine" + //
        " FROM " + getDatabaseSchema() + ".organization org" + //
        " JOIN " + getDatabaseSchema() + ".application app" + //
        " ON org.related_repository_id = ?2" + //
        " AND org.organization_id = app.organization_id" + //
        " JOIN " + getDatabaseSchema() + ".policy_violation pv" + //
        " ON pv.application_id = app.application_id" + //
        " WHERE fix_time is null" + //
        " AND waive_time is null" + //
        " AND stage_type_id = ?3";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery,
              Action.ID_FAIL, repositoryId, ProxyStageType.ID);
      Object[] result = (Object[]) query.getSingleResult();
      return new ContainerImagePolicyViolationSummaryDTO(result);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getMeanTimeToRemediate(final int lookBackWindowDays) {
    final String sQuery;

    if (isDatabasePostgresql()) {
      sQuery = "SELECT EXTRACT(EPOCH FROM AVG(LEAST(fix_time, waive_time) - open_time)) * 1000" +
          "  FROM " + getDatabaseSchema() + ".policy_violation" +
          "  WHERE (fix_time IS NOT null OR waive_time IS NOT null)" +
          "  AND open_time > CURRENT_DATE - MAKE_INTERVAL(days => ?1)";
    }
    else {
      // We support h2 1.4. This version does not support EXTRACT (epoch, it also does not correctly subtract two
      // timestamps
      // It does, however have the function TIMESTAMPDIFF (postgres does not), which gets us the same result
      // Keep in mind
      // SELECT DATEADD(mm,-1, '2024-03-29 18:47:52.69') -> 2024-02-29 18:47:52.69, but
      // SELECT DATEADD(mm,-1, '2024-03-30 18:47:52.69') -> 2024-02-29 18:47:52.69
      // TO DO CONSIDER TAKING THIS NUMBER IN DAYS, WEEKS or MS, FOR OTHER ENDPOINTS WE TREAT 3 months as 12 weeks
      sQuery = "SELECT AVG(TIMESTAMPDIFF(MILLISECOND, open_time, LEAST(fix_time, waive_time)))" +
          "  FROM " + getDatabaseSchema() + ".policy_violation" +
          "  WHERE (fix_time IS NOT null OR waive_time IS NOT null)" +
          "  AND policy_violation.open_time > DATEADD(dd, -?1, CURRENT_TIMESTAMP)";
    }

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery, Double.class);

      query.setParameter(1, lookBackWindowDays);
      Double result = (Double) query.getSingleResult();

      if (result == null) {
        return 0L;
      }

      return Math.round(result);
    }
  }

  private Collection<PolicyThreatCategory> getPolicyThreatCategoriesFilter(
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    if (policyThreatCategories == null || policyThreatCategories.isEmpty()) {
      policyThreatCategories = Arrays.stream(PolicyThreatCategory.values()).collect(Collectors.toSet());
    }
    return policyThreatCategories;
  }

  public List<InternalDashboardViolationRiskDTO> getDashboardViolationRisk(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      Integer minPolicyThreatLevel,
      Integer maxPolicyThreatLevel,
      Date minDate,
      Set<String> policyThreatCategories,
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation,
      List<String> orderBys,
      int page,
      int pageSize)
  {
    if (applicationIds.isEmpty()) {
      return Collections.emptyList();
    }

    String databaseSchema = getDatabaseSchema();

    // The aggregationQuery extracts policy violations grouped by app+policy+threat_level+component across stages (i.e.
    // the columns in DISTINCT ON) and extracts the oldest policy violation for each group (because it orders by the
    // same columns it groups by and open_time).
    // It is important that the columns in DISTINCT ON are the first columns in ORDER BY.
    // Note that GROUP BY was tested with this query instead of DISTINCT ON and was found to be slower.
    String aggregationQuery;

    try (TransactionContext tx = createTransactionContext()) {
      boolean useTemporaryTable =
          temporaryTableHelper.maybeCreateTemporaryTableWithIds(tx, applicationIds);

      int appIdsParamStartPosition = 1;
      int stageIdsParamStartPosition = useTemporaryTable ? 1 : appIdsParamStartPosition + applicationIds.size();

      aggregationQuery = String.format("""
              SELECT
                DISTINCT ON (
                  pv.application_id,
                  pv.policy_name,
                  pv.threat_level,
                  pv.hash,
                  pv.component_id_format,
                  pv.component_id_coordinates_json,
                  pv.constraint_facts_id
                )
                pv.application_id,
                pv.policy_name,
                pv.threat_level,
                pv.hash,
                pv.component_id_format,
                pv.component_id_coordinates_json,
                pv.constraint_facts_id,
                pv.open_time,
                pv.filename,
                pv.policy_violation_id,
                pv.auto_policy_waiver_id
              FROM %s.policy_violation pv
              %s
              WHERE
                pv.stage_type_id IN %s
                AND pv.fix_time IS NULL
                %s
              """,
          databaseSchema,
          useTemporaryTable ? "JOIN temporary_ids ti ON pv.application_id = ti.id" : "",
          buildPositionalParameters(stageTypeIds, stageIdsParamStartPosition),
          useTemporaryTable ? "" :
              "AND pv.application_id IN " + buildPositionalParameters(applicationIds, appIdsParamStartPosition)
      );

      int nextParamPosition = stageIdsParamStartPosition + stageTypeIds.size();
      int minDateParamPosition = nextParamPosition;
      if (minDate != null) {
        aggregationQuery += "    AND pv.open_time >= ?" + minDateParamPosition + "\n";
        nextParamPosition++;
      }
      int minThreatLevelParamPosition = nextParamPosition;
      if (minPolicyThreatLevel != null) {
        aggregationQuery += "    AND pv.threat_level >= ?" + minThreatLevelParamPosition + "\n";
        nextParamPosition++;
      }
      int maxThreatLevelParamPosition = nextParamPosition;
      if (maxPolicyThreatLevel != null) {
        aggregationQuery += "    AND pv.threat_level <= ?" + maxThreatLevelParamPosition + "\n";
        nextParamPosition++;
      }
      int threatCategoriesParamPosition = nextParamPosition;
      if (policyThreatCategories != null) {
        aggregationQuery += "    AND pv.threat_category IN "
            + buildPositionalParameters(policyThreatCategories, threatCategoriesParamPosition) + "\n";
        nextParamPosition++;
      }
      aggregationQuery +=
          getPolicyStateFilterForNativeQuery(violationStateOpen, violationStateWaived, violationStateLegacyViolation);
      aggregationQuery += """
          ORDER BY
            pv.application_id,
            pv.policy_name,
            pv.threat_level,
            pv.hash,
            pv.component_id_format,
            pv.component_id_coordinates_json,
            pv.constraint_facts_id,
            pv.open_time""";

      // The final query uses the aggregation query above to extract the columns needed in the results.
      // We need this "extra" query because the desired order is not the order used in the aggregation query.
      String sQuery = String.format("""
          WITH aggregated_policy_violation AS (
          %s
          )
          SELECT
            application.application_id,
            application.name application_name,
            organization.name organization_name,
            apv.policy_violation_id,
            apv.policy_name,
            apv.threat_level,
            apv.hash,
            apv.filename,
            apv.component_id_format,
            apv.component_id_coordinates_json,
            apv.constraint_facts_id,
            apv.open_time,
            apv.auto_policy_waiver_id
          FROM aggregated_policy_violation apv
          JOIN %s.application application USING (application_id)
          JOIN %s.organization organization USING (organization_id)
          """, aggregationQuery, databaseSchema, databaseSchema);
      // Adds sorting by policy_violation_id to get repeatable results
      orderBys.add("policy_violation_id");
      sQuery += String.format("ORDER BY %s\n", String.join(", ", orderBys));
      // For CSV export, the pageSize is set to Integer.MAX_VALUE, which means unlimited.
      // We extract pageSize+1 records to be able to know if there are more records available, which tells the UI if
      // there is a next page or not.
      if (pageSize < Integer.MAX_VALUE) {
        sQuery += String.format("LIMIT %d OFFSET %d", (pageSize + 1), (page * pageSize));
      }

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery.toString());

      // Add parameters based on whether we're using a temporary table or not
      if (!useTemporaryTable) {
        addPositionalParameters(query, applicationIds, appIdsParamStartPosition);
      }

      addPositionalParameters(query, stageTypeIds, stageIdsParamStartPosition);

      if (minDate != null) {
        query.setParameter(minDateParamPosition, minDate);
      }
      if (minPolicyThreatLevel != null) {
        query.setParameter(minThreatLevelParamPosition, minPolicyThreatLevel);
      }
      if (maxPolicyThreatLevel != null) {
        query.setParameter(maxThreatLevelParamPosition, maxPolicyThreatLevel);
      }
      if (policyThreatCategories != null) {
        addPositionalParameters(query, policyThreatCategories, threatCategoriesParamPosition);
      }

      @SuppressWarnings("unchecked")
      List<InternalDashboardViolationRiskDTO> results =
          ((Stream<Object[]>) query.getResultStream())
              .map(array -> new InternalDashboardViolationRiskDTO( //
                  (String) array[0], // applicationId
                  (String) array[1], // applicationName
                  (String) array[2], // organizationName
                  (String) array[3], // policyViolationId
                  (String) array[4], // policyName
                  getInteger(array[5]), // threatLevel
                  (String) array[6], // hash
                  (String) array[7], // filename
                  (String) array[8], // componentIdFormat
                  (String) array[9], // componentIdCoordinatesJson
                  (String) array[10], // constraintFactsId
                  ((Timestamp) array[11]).getTime(), // firstOccurrenceTime
                  (String) array[12] // autoPolicyWaiverId
              )).toList();

      return results;
    }
  }

  public List<PolicyViolation> getAutoWaivedByApplicationIdAndStageId(final String appId, final String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NOT NULL AND entity.autoPolicyWaiverId IS NOT NULL";
    return getList(sQuery, appId, stageTypeId);
  }

  public Collection<PolicyViolation> getByApplicationIdsAndPolicyIdsAndTypes(
      Set<String> applicationIds,
      Set<String> policyIds,
      Date openTimeAfter,
      Date openTimeBefore,
      boolean includeActive,
      boolean includeWaived,
      boolean includeLegacy)
  {
    String baseQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.policyId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        getPolicyStateFilter(includeActive, includeWaived, includeLegacy);

    return loadPolicyViolationsWithDateFilters(applicationIds, policyIds, openTimeAfter, openTimeBefore, baseQuery);
  }

  public long getContainerImagesQuarantinedCount() {
    String sQuery = String.format("""
        SELECT COUNT(DISTINCT CONCAT(pv.application_id, r.repository_id)) AS total_failed_proxy_violations
        FROM %1$s.policy_violation pv
                 JOIN %1$s.application a ON pv.application_id = a.application_id
                 JOIN %1$s.organization o ON a.organization_id = o.organization_id
                 JOIN %1$s.repository r ON o.related_repository_id = r.repository_id
        WHERE r.format = 'docker'
          AND pv.stage_type_id = 'proxy'
          AND pv.action_type_id = 'fail'
          AND pv.waive_time IS NULL
          AND pv.fix_time IS NULL
        """, getDatabaseSchema());

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery);
      Object result = query.getSingleResult();
      return (long)result;
    }
  }

  public List<ContainerImageInQuarantineData> getContainerImagesInQuarantine(int page, int pageSize) {
    String sQuery = String.format("""
        WITH AggregatedPolicyViolation AS (
            SELECT pv.application_id,
                   MAX(pv.open_time) AS max_open_time,
                   MAX(pv.threat_level) AS max_threat_level,
                   COUNT(pv.application_id) AS policy_violation_count
            FROM %1$s.policy_violation pv
            WHERE pv.stage_type_id = 'proxy'
              AND pv.action_type_id = 'fail'
              AND pv.waive_time IS NULL
              AND pv.fix_time IS NULL
            GROUP BY pv.application_id
        )
        SELECT max_threat_level AS threat_level,
               max_open_time AS open_time,
               a.public_id AS application_public_id,
               a.application_id,
               a.name AS application_name,
               r.public_id AS repository_public_id,
               r.repository_id,
               apv.policy_violation_count,
               pe.scan_id
        FROM AggregatedPolicyViolation apv
                 JOIN %1$s.application a ON apv.application_id = a.application_id
                 JOIN %1$s.organization o ON a.organization_id = o.organization_id
                 JOIN %1$s.repository r ON o.related_repository_id = r.repository_id
                 JOIN %1$s.policy_evaluation pe ON apv.application_id = pe.application_id
                          AND apv.max_open_time = pe.time
        WHERE r.format = 'docker'
        ORDER BY  apv.max_open_time DESC, a.application_id ASC
        """, getDatabaseSchema());

    int offset = (page - 1) * pageSize;
    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativePaginationQuery(tx, sQuery, offset, pageSize);
      return ((Stream<Object[]>) query.getResultStream())
          .map(array -> new ContainerImageInQuarantineData(
              ((Number) array[0]).intValue(), // threatLevel
              (Date) array[1], // openTime
              (String) array[2], // applicationPublicId
              (String) array[3], // applicationId
              (String) array[4], // applicationName
              (String) array[5], // repositoryPublicId
              (String) array[6], // repositoryId
              ((Number) array[7]).longValue(), // policyViolationCount
              (String) array[8] // scanId
          )).toList();
    }
  }

  public static record ContainerImageInQuarantineData(
      int threatLevel,
      Date openTime,
      String applicationPublicId,
      String applicationId,
      String applicationName,
      String repositoryPublicId,
      String repositoryId,
      Long policyViolationCount,
      String scanId) { }

  private List<PolicyViolation> loadPolicyViolationsWithDateFilters(
      Collection<String> applicationIds,
      Collection<String> policyIds,
      Date openTimeAfter,
      Date openTimeBefore,
      String baseQuery)
  {
    StringBuilder queryBuilder = new StringBuilder(baseQuery);
    int nextIndex = 3;

    if (openTimeAfter != null) {
      queryBuilder.append(" AND entity.openTime >= ?").append(nextIndex++);
    }
    if (openTimeBefore != null) {
      queryBuilder.append(" AND entity.openTime <= ?").append(nextIndex);
    }

    if (openTimeAfter != null) {
      if (openTimeBefore != null) {
        return getUnfixed(queryBuilder.toString(), applicationIds, policyIds, openTimeAfter, openTimeBefore);
      }
      return getUnfixed(queryBuilder.toString(), applicationIds, policyIds, openTimeAfter);
    }
    else {
      if (openTimeBefore != null) {
        return getUnfixed(queryBuilder.toString(), applicationIds, policyIds, openTimeBefore);
      }
      return getUnfixed(queryBuilder.toString(), applicationIds, policyIds);
    }
  }
}

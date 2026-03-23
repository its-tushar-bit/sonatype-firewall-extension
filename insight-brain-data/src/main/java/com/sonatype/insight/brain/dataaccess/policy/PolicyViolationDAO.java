/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION;
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

  @Override
  public Table<?> getJooqTable() {
    return POLICY_VIOLATION;
  }

  @Override
  public List<PolicyViolation> getAll(TransactionContext tx) {
    return tx.dsl().selectFrom(POLICY_VIOLATION).fetchInto(PolicyViolation.class);
  }

  @Override
  public void insert(TransactionContext tx, PolicyViolation entity) {
    storeConstraints(entity);
    generateIdIfNeeded(entity);
    tx.dsl()
        .insertInto(POLICY_VIOLATION)
        .set(POLICY_VIOLATION.POLICY_VIOLATION_ID, entity.getId())
        .set(POLICY_VIOLATION.APPLICATION_ID, entity.getApplicationId())
        .set(POLICY_VIOLATION.STAGE_TYPE_ID, entity.getStageTypeId())
        .set(POLICY_VIOLATION.POLICY_ID, entity.getPolicyId())
        .set(POLICY_VIOLATION.POLICY_NAME, entity.getPolicyName())
        .set(POLICY_VIOLATION.THREAT_LEVEL, (short) entity.getThreatLevel())
        .set(POLICY_VIOLATION.THREAT_CATEGORY,
            entity.getThreatCategory() != null ? entity.getThreatCategory().name() : null)
        .set(POLICY_VIOLATION.HASH, entity.getHash())
        .set(POLICY_VIOLATION.COMPONENT_ID_FORMAT, entity.getComponentIdFormat())
        .set(POLICY_VIOLATION.COMPONENT_ID_COORDINATES_JSON, entity.getComponentIdCoordinatesJson())
        .set(POLICY_VIOLATION.FILENAME, entity.getFilename())
        .set(POLICY_VIOLATION.ACTION_TYPE_ID, entity.getActionTypeId())
        .set(POLICY_VIOLATION.OPEN_TIME, entity.getOpenTime())
        .set(POLICY_VIOLATION.WAIVE_TIME, entity.getWaiveTime())
        .set(POLICY_VIOLATION.LEGACY_VIOLATION_TIME, entity.getLegacyViolationTime())
        .set(POLICY_VIOLATION.FIX_TIME, entity.getFixTime())
        .set(POLICY_VIOLATION.POLICY_WAIVER_ID, entity.getPolicyWaiverId())
        .set(POLICY_VIOLATION.POLICY_WAIVER_COMMENT, entity.getPolicyWaiverComment())
        .set(POLICY_VIOLATION.SEEN_BY_PRIMARY_EVALUATION, entity.isSeenByPrimaryEvaluation())
        .set(POLICY_VIOLATION.SEEN_BY_MONITORING_EVALUATION, entity.isSeenByMonitoringEvaluation())
        .set(POLICY_VIOLATION.LEGACY_VIOLATION_APPLIED, entity.isLegacyViolationApplied())
        .set(POLICY_VIOLATION.REACHABILITY_STATUS,
            entity.getReachabilityStatus() != null ? entity.getReachabilityStatus().name() : null)
        .set(POLICY_VIOLATION.AUTO_POLICY_WAIVER_ID, entity.getAutoPolicyWaiverId())
        .set(POLICY_VIOLATION.CONSTRAINT_FACTS_ID, entity.getConstraintFactsId())
        .execute();
  }

  @Override
  public void update(TransactionContext tx, PolicyViolation entity) {
    storeConstraints(entity);
    tx.dsl()
        .update(POLICY_VIOLATION)
        .set(POLICY_VIOLATION.APPLICATION_ID, entity.getApplicationId())
        .set(POLICY_VIOLATION.STAGE_TYPE_ID, entity.getStageTypeId())
        .set(POLICY_VIOLATION.POLICY_ID, entity.getPolicyId())
        .set(POLICY_VIOLATION.POLICY_NAME, entity.getPolicyName())
        .set(POLICY_VIOLATION.THREAT_LEVEL, (short) entity.getThreatLevel())
        .set(POLICY_VIOLATION.THREAT_CATEGORY,
            entity.getThreatCategory() != null ? entity.getThreatCategory().name() : null)
        .set(POLICY_VIOLATION.HASH, entity.getHash())
        .set(POLICY_VIOLATION.COMPONENT_ID_FORMAT, entity.getComponentIdFormat())
        .set(POLICY_VIOLATION.COMPONENT_ID_COORDINATES_JSON, entity.getComponentIdCoordinatesJson())
        .set(POLICY_VIOLATION.FILENAME, entity.getFilename())
        .set(POLICY_VIOLATION.ACTION_TYPE_ID, entity.getActionTypeId())
        .set(POLICY_VIOLATION.OPEN_TIME, entity.getOpenTime())
        .set(POLICY_VIOLATION.WAIVE_TIME, entity.getWaiveTime())
        .set(POLICY_VIOLATION.LEGACY_VIOLATION_TIME, entity.getLegacyViolationTime())
        .set(POLICY_VIOLATION.FIX_TIME, entity.getFixTime())
        .set(POLICY_VIOLATION.POLICY_WAIVER_ID, entity.getPolicyWaiverId())
        .set(POLICY_VIOLATION.POLICY_WAIVER_COMMENT, entity.getPolicyWaiverComment())
        .set(POLICY_VIOLATION.SEEN_BY_PRIMARY_EVALUATION, entity.isSeenByPrimaryEvaluation())
        .set(POLICY_VIOLATION.SEEN_BY_MONITORING_EVALUATION, entity.isSeenByMonitoringEvaluation())
        .set(POLICY_VIOLATION.LEGACY_VIOLATION_APPLIED, entity.isLegacyViolationApplied())
        .set(POLICY_VIOLATION.REACHABILITY_STATUS,
            entity.getReachabilityStatus() != null ? entity.getReachabilityStatus().name() : null)
        .set(POLICY_VIOLATION.AUTO_POLICY_WAIVER_ID, entity.getAutoPolicyWaiverId())
        .set(POLICY_VIOLATION.CONSTRAINT_FACTS_ID, entity.getConstraintFactsId())
        // Clear deprecated column after migration to new constraint_facts_id
        .set(POLICY_VIOLATION.CONSTRAINT_FACTS_JSON, (String) null)
        .where(POLICY_VIOLATION.POLICY_VIOLATION_ID.eq(entity.getId()))
        .execute();
  }

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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
          .fetchInto(PolicyViolation.class);
    }
  }

  public List<PolicyViolation> getByIds(Set<String> ids) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIds(tx, ids);
    }
  }

  public List<PolicyViolation> getByIds(TransactionContext tx, Set<String> ids) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }
    return tx.dsl()
        .selectFrom(POLICY_VIOLATION)
        .where(POLICY_VIOLATION.POLICY_VIOLATION_ID.in(ids))
        .fetchInto(PolicyViolation.class);
  }

  public List<PolicyViolation> getByApplicationIdAndPolicyIdAndHash(
      String applicationId,
      String policyId,
      String hash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_VIOLATION.POLICY_ID.eq(policyId));

      if (hash == null) {
        return query.and(POLICY_VIOLATION.HASH.isNull())
            .fetchInto(PolicyViolation.class);
      }
      else {
        return query.and(POLICY_VIOLATION.HASH.eq(hash))
            .fetchInto(PolicyViolation.class);
      }
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
    return tx.dsl()
        .selectFrom(POLICY_VIOLATION)
        .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
        .and(POLICY_VIOLATION.STAGE_TYPE_ID.eq(stageTypeId))
        .and(POLICY_VIOLATION.FIX_TIME.isNull())
        .fetchInto(PolicyViolation.class);
  }

  /**
   * Returns violations to enforce based on stage-specific semantics.
   * Firewall (proxy): Ignores legacy violations completely (treats as active).
   * Lifecycle (build/release): Excludes all legacy violations.
   */
  public List<PolicyViolation> getActiveByApplicationIdAndStageId(String applicationId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_VIOLATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_VIOLATION.FIX_TIME.isNull())
          .and(POLICY_VIOLATION.WAIVE_TIME.isNull());

      if (!ProxyStageType.ID.equals(stageTypeId)) {
        // Lifecycle: exclude legacy violations
        query = query.and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull());
      }

      return query.fetchInto(PolicyViolation.class);
    }
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
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_VIOLATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_VIOLATION.ACTION_TYPE_ID.eq(actionTypeId))
          .and(POLICY_VIOLATION.FIX_TIME.isNull())
          .and(POLICY_VIOLATION.WAIVE_TIME.isNull());

      if (!ProxyStageType.ID.equals(stageTypeId)) {
        // Lifecycle: exclude legacy violations
        query = query.and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull());
      }

      return query.fetchInto(PolicyViolation.class);
    }
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
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_VIOLATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_VIOLATION.HASH.eq(hash))
          .and(POLICY_VIOLATION.FIX_TIME.isNull())
          .and(POLICY_VIOLATION.WAIVE_TIME.isNull());

      if (!ProxyStageType.ID.equals(stageTypeId)) {
        // Lifecycle: exclude legacy violations
        query = query.and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull());
      }

      return query.fetchInto(PolicyViolation.class);
    }
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
    final int finalMinThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    final int finalMaxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    final Collection<PolicyThreatCategory> finalCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);
    final List<String> categoryNames = finalCategories.stream()
        .map(PolicyThreatCategory::name)
        .collect(toList());

    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        var query = tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.OPEN_TIME.ge(minDate))
            .and(POLICY_VIOLATION.THREAT_LEVEL.ge((short) finalMinThreatLevel))
            .and(POLICY_VIOLATION.THREAT_LEVEL.le((short) finalMaxThreatLevel))
            .and(POLICY_VIOLATION.FIX_TIME.isNull())
            .and(POLICY_VIOLATION.THREAT_CATEGORY.in(categoryNames));

        if (onlyActiveViolations) {
          query = query.and(POLICY_VIOLATION.WAIVE_TIME.isNull())
              .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull());
        }

        return query.fetchInto(PolicyViolation.class);
      }
    });
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
    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        var query = tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.FIX_TIME.isNull());

        if (onlyActiveViolations) {
          query = query.and(POLICY_VIOLATION.WAIVE_TIME.isNull())
              .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull());
        }

        return query.fetchInto(PolicyViolation.class);
      }
    });
  }

  public List<PolicyViolation> getUnfixedByApplicationId(TransactionContext tx, String applicationId) {
    return tx.dsl()
        .selectFrom(POLICY_VIOLATION)
        .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
        .and(POLICY_VIOLATION.FIX_TIME.isNull())
        .fetchInto(PolicyViolation.class);
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndStageIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    final int finalMinThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    final int finalMaxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    final Collection<PolicyThreatCategory> finalCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);
    final List<String> categoryNames = finalCategories.stream()
        .map(PolicyThreatCategory::name)
        .collect(toList());

    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.STAGE_TYPE_ID.in(stageTypeIds))
            .and(POLICY_VIOLATION.OPEN_TIME.ge(minDate))
            .and(POLICY_VIOLATION.THREAT_LEVEL.ge((short) finalMinThreatLevel))
            .and(POLICY_VIOLATION.THREAT_LEVEL.le((short) finalMaxThreatLevel))
            .and(POLICY_VIOLATION.THREAT_CATEGORY.in(categoryNames))
            .and(POLICY_VIOLATION.FIX_TIME.isNull())
            .and(POLICY_VIOLATION.WAIVE_TIME.isNull())
            .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull())
            .fetchInto(PolicyViolation.class);
      }
    });
  }

  public List<RepositoryResultsForImageContainer> getRepositoryResultsForImageContainer(
      Collection<String> repositoryIds,
      Collection<String> applicationIds,
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
      Collection<String> repositoryIds,
      Collection<String> applicationIds,
      RepositoryResultsForImageContainerFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      // Only count threat level filters if they will actually produce a WHERE clause
      // The clause is only added when filters are outside the default [0, 10] range
      int threatLevelFiltersSize = detailsFilter.threatLevelFilters != null &&
          detailsFilter.threatLevelFilters.size() == 2 &&
          (detailsFilter.threatLevelFilters.get(0) > 0 || detailsFilter.threatLevelFilters.get(1) < 10) ? 2 : 0;
      int repositoryIdsSize = repositoryIds.size();
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
              PolicyEvaluation::getScanId // Value: scanId
          ));

      StringBuilder sQuery = new StringBuilder();
      sQuery.append("SELECT threat_level,");
      sQuery.append(" policy_name as policy,");
      sQuery.append(" app.name as object,");
      sQuery.append(" CASE WHEN pv.waive_time IS NOT NULL THEN NULL");
      sQuery.append(" WHEN pv.action_type_id = 'fail' THEN pv.open_time ELSE NULL END as quarantine_time,");
      sQuery.append(" app.application_id,");
      sQuery.append(" app.public_id");
      sQuery.append(" FROM ")
          .append(getDatabaseSchema())
          .append(".organization org JOIN ")
          .append(getDatabaseSchema())
          .append(".application app");
      sQuery.append(" ON org.organization_id = app.organization_id");
      sQuery.append(" INNER JOIN ").append(getDatabaseSchema()).append(".last_policy_evaluation lpe");
      sQuery.append(" ON lpe.application_id = app.application_id");
      sQuery.append(" INNER JOIN ").append(getDatabaseSchema()).append(".policy_evaluation pe");
      sQuery.append(" ON lpe.policy_evaluation_id = pe.policy_evaluation_id");
      sQuery.append((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN");
      sQuery.append(" ").append(getDatabaseSchema()).append(".policy_violation pv");
      sQuery.append(" ON app.application_id = pv.application_id AND pv.stage_type_id = 'proxy'");
      sQuery.append(" WHERE related_repository_id IN ");
      sQuery.append(buildPositionalParameters(repositoryIds, repositoryIdsParamStartPosition));
      sQuery.append(" AND pv.fix_time IS NULL");

      sQuery.append(addThreatLevelFilters(detailsFilter.threatLevelFilters, threatLevelFiltersParamStartPosition));

      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      // Add search filters using the new method signature
      addSearchFilters(sQuery, detailsFilter.searchFilters, searchFiltersParamStartPosition);

      sQuery.append(validateAndAddSortFields(detailsFilter.sortFields));

      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;
      int pageLimit = detailsFilter.pageSize +
          1; // Incremented page size to help UI determine whether to enable / disable NextPage button

      // Build bindings list - only add bindings for filters that exist
      List<Object> bindings = new ArrayList<>();
      bindings.addAll(repositoryIds);
      if (threatLevelFiltersSize == 2) {
        bindings.add(detailsFilter.threatLevelFilters.get(0));
        bindings.add(detailsFilter.threatLevelFilters.get(1));
      }
      // Add search filter bindings in the same order as addSearchFilters
      if (detailsFilter.searchFilters.containsKey("POLICY_NAME")) {
        bindings.add('%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("QUARANTINE_TIME")) {
        bindings.add('%' + detailsFilter.searchFilters.get("QUARANTINE_TIME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("OBJECT_NAME")) {
        bindings.add('%' + detailsFilter.searchFilters.get("OBJECT_NAME") + '%');
      }

      // Add LIMIT and OFFSET to the query
      sQuery.append(" LIMIT ").append(pageLimit).append(" OFFSET ").append(offset);

      // Convert positional parameters from ?N to ? format for jOOQ
      String convertedQuery = convertPositionalParams(sQuery.toString());

      List<RepositoryResultsForImageContainer> results = tx.dsl()
          .resultQuery(convertedQuery, bindings.toArray())
          .fetch()
          .stream()
          .map(record -> new RepositoryResultsForImageContainer(
              record.get(0, Integer.class),
              record.get(1, String.class),
              null,
              record.get(2, String.class),
              toDateFromTimestampOrLocalDateTime(record.get(3)),
              applicationIdsToScanIdMap.get(record.get(4, String.class)),
              record.get(5, String.class)))
          .collect(Collectors.toList());

      return results;
    }
  }

  private boolean hasNonViolatingFilter(final Set<String> violationStateFilters) {
    return violationStateFilters.stream()
        .anyMatch(filter -> filter.equals("VIOLATION_STATE_ALL") || filter.equals("VIOLATION_STATE_NOT_VIOLATING"));
  }

  protected List<RepositoryResultsForImageContainer> getRepositoryResultsForImageContainerAggregate(
      Collection<String> repositoryIds,
      Collection<String> applicationIds,
      RepositoryResultsForImageContainerFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      int repositoryIdsSize = repositoryIds.size();
      // Only count threat level filters if they will actually produce a WHERE clause
      // The clause is only added when filters are outside the default [0, 10] range
      int threatLevelFiltersSize = detailsFilter.threatLevelFilters != null &&
          detailsFilter.threatLevelFilters.size() == 2 &&
          (detailsFilter.threatLevelFilters.get(0) > 0 || detailsFilter.threatLevelFilters.get(1) < 10) ? 2 : 0;
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
              PolicyEvaluation::getScanId // Value: scanId
          ));

      // Build query with StringBuilder to track parameter positions
      StringBuilder sQuery = new StringBuilder();
      sQuery.append("SELECT max(threat_level) as threat_level,");
      sQuery.append(" COUNT(CASE WHEN (threat_level >= 2) THEN 1 END) as violation_count,");
      sQuery.append(" app.name as object,");
      sQuery.append(" max(CASE WHEN pv.waive_time IS NOT NULL THEN NULL");
      sQuery.append(" WHEN pv.action_type_id = 'fail' THEN pv.open_time ELSE NULL END) as quarantine_time,");
      sQuery.append(" app.application_id,");
      sQuery.append(" app.public_id");
      sQuery.append(" FROM ")
          .append(getDatabaseSchema())
          .append(".organization org JOIN ")
          .append(getDatabaseSchema())
          .append(".application app");
      sQuery.append(" ON org.organization_id = app.organization_id");
      sQuery.append((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN");
      sQuery.append(" ").append(getDatabaseSchema()).append(".policy_violation pv");
      sQuery.append(" ON app.application_id = pv.application_id AND pv.stage_type_id = 'proxy'");
      sQuery.append(" INNER JOIN ").append(getDatabaseSchema()).append(".last_policy_evaluation lpe");
      sQuery.append(" ON lpe.application_id = app.application_id");
      sQuery.append(" INNER JOIN ").append(getDatabaseSchema()).append(".policy_evaluation pe");
      sQuery.append(" ON lpe.policy_evaluation_id = pe.policy_evaluation_id");
      sQuery.append(" WHERE related_repository_id IN");
      sQuery.append(buildPositionalParameters(repositoryIds, repositoryIdsParamStartPosition));
      sQuery.append(addThreatLevelFilters(detailsFilter.threatLevelFilters, threatLevelFiltersParamStartPosition));
      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      // Add search filters and track how many parameters were added
      int searchParamsAdded = addSearchFilters(sQuery, detailsFilter.searchFilters, searchFiltersParamStartPosition);

      sQuery.append(" AND pv.fix_time IS NULL");
      sQuery.append(" GROUP BY app.application_id, app.name");

      // HAVING clause uses the next available position after search filters
      int havingParamPosition = searchFiltersParamStartPosition + searchParamsAdded;
      addPolicyViolationCountForHavingClause(sQuery, detailsFilter.searchFilters, havingParamPosition);

      // Use isAggregateQuery=true to avoid adding policy_name tiebreaker (not in GROUP BY)
      sQuery.append(validateAndAddSortFields(detailsFilter.sortFields, true));
      sQuery.append(" LIMIT ").append(pageSize);
      sQuery.append(" OFFSET ").append(offset);

      // Build bindings list - only add bindings for filters that exist
      List<Object> bindings = new ArrayList<>();
      bindings.addAll(repositoryIds);
      if (threatLevelFiltersSize == 2) {
        bindings.add(detailsFilter.threatLevelFilters.get(0));
        bindings.add(detailsFilter.threatLevelFilters.get(1));
      }
      // Add search filter bindings in the same order as addSearchFilters
      if (detailsFilter.searchFilters.containsKey("POLICY_NAME")) {
        bindings.add('%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("QUARANTINE_TIME")) {
        bindings.add('%' + detailsFilter.searchFilters.get("QUARANTINE_TIME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("OBJECT_NAME")) {
        bindings.add('%' + detailsFilter.searchFilters.get("OBJECT_NAME") + '%');
      }
      // HAVING clause binding comes after WHERE search filters
      if (detailsFilter.searchFilters.containsKey("VIOLATION_COUNT")) {
        bindings.add(detailsFilter.searchFilters.get("VIOLATION_COUNT"));
      }

      // Convert positional parameters from ?N to ? format for jOOQ
      String convertedQuery = convertPositionalParams(sQuery.toString());

      List<RepositoryResultsForImageContainer> results = tx.dsl()
          .resultQuery(convertedQuery, bindings.toArray())
          .fetch()
          .stream()
          .map(record -> new RepositoryResultsForImageContainer(
              record.get(0, Integer.class),
              null,
              record.get(1, Integer.class),
              record.get(2, String.class),
              toDateFromTimestampOrLocalDateTime(record.get(3)),
              applicationIdsToScanIdMap.get(record.get(4, String.class)),
              record.get(5, String.class)))
          .collect(Collectors.toList());
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

  /**
   * Adds search filter conditions to the query. Returns the number of parameters added. The filters are added in a
   * consistent order: POLICY_NAME, QUARANTINE_TIME, OBJECT_NAME.
   */
  private static int addSearchFilters(StringBuilder query, Map<String, String> filters, int paramStartPosition) {
    int paramCount = 0;
    if (!MapUtils.isEmpty(filters)) {
      // Process in consistent order
      if (filters.containsKey("POLICY_NAME")) {
        query.append(" AND LOWER(pv.policy_name) LIKE ?" + (paramStartPosition + paramCount));
        paramCount++;
      }
      if (filters.containsKey("QUARANTINE_TIME")) {
        query.append(" AND (pv.open_time IS NOT NULL AND TO_CHAR(pv.open_time, 'YYYY-MM-DD') LIKE ?" +
            (paramStartPosition + paramCount) + ")");
        paramCount++;
      }
      if (filters.containsKey("OBJECT_NAME")) {
        query.append(" AND LOWER(app.name) LIKE ?" + (paramStartPosition + paramCount));
        paramCount++;
      }
    }
    return paramCount;
  }

  /**
   * Adds HAVING clause for violation count filter. Returns 1 if added, 0 otherwise.
   */
  private static int addPolicyViolationCountForHavingClause(
      StringBuilder query,
      Map<String, String> filters,
      int paramStartPosition)
  {
    if (!MapUtils.isEmpty(filters) && filters.containsKey("VIOLATION_COUNT")) {
      query.append(" HAVING violation_count = ?" + paramStartPosition);
      return 1;
    }
    return 0;
  }

  private String validateAndAddSortFields(final List<SortField> sortFields) {
    return validateAndAddSortFields(sortFields, false);
  }

  private String validateAndAddSortFields(final List<SortField> sortFields, final boolean isAggregateQuery) {
    StringBuilder query = new StringBuilder();
    List<String> result = new ArrayList<>();
    if (!CollectionUtils.isEmpty(sortFields)) {
      sortFields.sort(Comparator.comparing(field -> field.sortPriority));
      Set<Integer> sortPriorities = new HashSet<>();
      Set<SortableField> addedFields = new HashSet<>();
      for (SortField sortField : sortFields) {
        if (sortPriorities.contains(sortField.sortPriority)) {
          throw new BadRequestException("sort priority cannot be the same for different fields");
        }
        // Skip POLICY_NAME for aggregate queries since it's not in the GROUP BY clause
        if (isAggregateQuery && sortField.sortableField == SortableField.POLICY_NAME) {
          continue;
        }
        if (sortField.asc) {
          result.add(getSortField(sortField.sortableField) + " NULLS LAST");
        }
        else {
          result.add(getSortField(sortField.sortableField) + " DESC NULLS LAST");
        }
        sortPriorities.add(sortField.sortPriority);
        addedFields.add(sortField.sortableField);
      }
      // Add policy_name as a tiebreaker for deterministic ordering when not already included
      // Skip for aggregate queries since policy_name is not in the GROUP BY clause
      if (!isAggregateQuery && !addedFields.contains(SortableField.POLICY_NAME)) {
        result.add(getSortField(SortableField.POLICY_NAME) + " NULLS LAST");
      }
      if (!result.isEmpty()) {
        query.append(" ORDER BY ").append(StringUtils.join(result, ", "));
      }
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
    final int finalMinThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    final int finalMaxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    final Collection<PolicyThreatCategory> finalCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);
    final List<String> categoryNames = finalCategories.stream()
        .map(PolicyThreatCategory::name)
        .collect(toList());

    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.STAGE_TYPE_ID.in(stageTypeIds))
            .and(POLICY_VIOLATION.FIX_TIME.isNull())
            .and(POLICY_VIOLATION.THREAT_LEVEL.ge((short) finalMinThreatLevel))
            .and(POLICY_VIOLATION.THREAT_LEVEL.le((short) finalMaxThreatLevel))
            .and(POLICY_VIOLATION.THREAT_CATEGORY.in(categoryNames))
            .and(POLICY_VIOLATION.WAIVE_TIME.isNull())
            .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull())
            .fetchInto(PolicyViolation.class);
      }
    });
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
    final int finalMinThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    final int finalMaxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    final Collection<PolicyThreatCategory> finalCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);
    final List<String> categoryNames = finalCategories.stream()
        .map(PolicyThreatCategory::name)
        .collect(toList());
    final org.jooq.Condition stateCondition = buildPolicyStateCondition(
        violationStateOpen, violationStateWaived, violationStateLegacyViolation);

    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        var query = tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.STAGE_TYPE_ID.in(stageTypeIds))
            .and(POLICY_VIOLATION.FIX_TIME.isNull())
            .and(POLICY_VIOLATION.THREAT_LEVEL.ge((short) finalMinThreatLevel))
            .and(POLICY_VIOLATION.THREAT_LEVEL.le((short) finalMaxThreatLevel))
            .and(POLICY_VIOLATION.THREAT_CATEGORY.in(categoryNames));

        if (stateCondition != null) {
          query = query.and(stateCondition);
        }

        return query.fetchInto(PolicyViolation.class);
      }
    });
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
    final int finalMinThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    final int finalMaxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    final Collection<PolicyThreatCategory> finalCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);
    final List<String> categoryNames = finalCategories.stream()
        .map(PolicyThreatCategory::name)
        .collect(toList());
    final org.jooq.Condition stateCondition = buildPolicyStateCondition(
        violationStateOpen, violationStateWaived, violationStateLegacyViolation);

    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        var query = tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.STAGE_TYPE_ID.in(stageTypeIds))
            .and(POLICY_VIOLATION.OPEN_TIME.ge(minDate))
            .and(POLICY_VIOLATION.THREAT_LEVEL.ge((short) finalMinThreatLevel))
            .and(POLICY_VIOLATION.THREAT_LEVEL.le((short) finalMaxThreatLevel))
            .and(POLICY_VIOLATION.THREAT_CATEGORY.in(categoryNames))
            .and(POLICY_VIOLATION.FIX_TIME.isNull());

        if (stateCondition != null) {
          query = query.and(stateCondition);
        }

        return query.fetchInto(PolicyViolation.class);
      }
    });
  }

  /**
   * Builds a jOOQ condition for filtering policy violations by state (open, waived, legacy).
   *
   * @param violationStateOpen include open violations (waiveTime IS NULL AND legacyViolationTime IS NULL)
   * @param violationStateWaived include waived violations (waiveTime IS NOT NULL)
   * @param violationStateLegacyViolation include legacy violations (legacyViolationTime IS NOT NULL)
   * @return the jOOQ condition, or null if all states are included
   */
  private org.jooq.Condition buildPolicyStateCondition(
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    boolean includeOpen = violationStateOpen == null || violationStateOpen;
    boolean includeWaived = violationStateWaived == null || violationStateWaived;
    boolean includeLegacy = violationStateLegacyViolation == null || violationStateLegacyViolation;

    // If none are selected, return null (no filter applied)
    if (!includeOpen && !includeWaived && !includeLegacy) {
      return null;
    }

    // If all are selected, no filter needed
    if (includeOpen && includeWaived && includeLegacy) {
      return null;
    }

    // Build OR conditions for the selected states
    List<org.jooq.Condition> conditions = new ArrayList<>();
    if (includeOpen) {
      conditions.add(POLICY_VIOLATION.WAIVE_TIME.isNull().and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull()));
    }
    if (includeWaived) {
      conditions.add(POLICY_VIOLATION.WAIVE_TIME.isNotNull());
    }
    if (includeLegacy) {
      conditions.add(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNotNull());
    }

    // Combine with OR
    org.jooq.Condition result = conditions.get(0);
    for (int i = 1; i < conditions.size(); i++) {
      result = result.or(conditions.get(i));
    }
    return result;
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
    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        var query = tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.POLICY_ID.in(policyIds))
            .and(POLICY_VIOLATION.FIX_TIME.isNull())
            .and(POLICY_VIOLATION.WAIVE_TIME.isNull())
            .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull());

        if (openTimeAfter != null) {
          query = query.and(POLICY_VIOLATION.OPEN_TIME.ge(openTimeAfter));
        }
        if (openTimeBefore != null) {
          query = query.and(POLICY_VIOLATION.OPEN_TIME.le(openTimeBefore));
        }

        return query.fetchInto(PolicyViolation.class);
      }
    });
  }

  /**
   * Executes a jOOQ query for unfixed policy violations with optimized handling for different databases.
   * <p>
   * For H2 (embedded): Runs one query per application ID in parallel to ensure index usage. For PostgreSQL: Uses IN
   * clause batching for efficiency.
   *
   * @param applicationIds the application IDs to query
   * @param queryFunction a function that takes a collection of application IDs and returns the query results
   * @return list of policy violations matching the criteria
   */
  private List<PolicyViolation> getUnfixed(
      Collection<String> applicationIds,
      java.util.function.Function<Collection<String>, List<PolicyViolation>> queryFunction)
  {
    if (isDatabaseEmbedded()) {
      // H2 won't utilize the index for the application id when the query uses an IN operator with multiple values (and
      // has additional filter criteria like the fix_time), doing an expensive table scan instead.
      // So we make one query per app to ensure the index is used (and all the fixed violations aren't scanned).
      TenantAwareFunction<String, List<PolicyViolation>> tenantAwareFunction =
          new TenantAwareFunction<>(applicationId -> queryFunction.apply(Collections.singletonList(applicationId)));
      return CompletableFuture.supplyAsync(
          new TenantAwareSupplier<>(() -> applicationIds.stream()
              .parallel()
              .map(tenantAwareFunction)
              .flatMap(Collection::stream)
              .collect(toList())),
          ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.DAO)).join();
    }
    else if (!applicationIds.isEmpty()) {
      return getListWithSqlInClause(applicationIds, queryFunction);
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
    try (TransactionContext tx = createTransactionContext()) {
      // Condition for violations opened during time range AND not immediately waived/legacy
      var openedDuringRange = POLICY_VIOLATION.OPEN_TIME.ge(from)
          .and(POLICY_VIOLATION.OPEN_TIME.lt(to))
          .and(POLICY_VIOLATION.WAIVE_TIME.gt(POLICY_VIOLATION.OPEN_TIME)
              .or(POLICY_VIOLATION.WAIVE_TIME.isNull()))
          .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.gt(POLICY_VIOLATION.OPEN_TIME)
              .or(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull()));

      // Condition for violations opened before time range AND not resolved before time range
      var openedBeforeRange = POLICY_VIOLATION.OPEN_TIME.lt(from)
          .and(POLICY_VIOLATION.FIX_TIME.isNull().or(POLICY_VIOLATION.FIX_TIME.gt(from)))
          .and(POLICY_VIOLATION.WAIVE_TIME.isNull().or(POLICY_VIOLATION.WAIVE_TIME.gt(from)))
          .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNull().or(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.gt(from)));

      return tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.APPLICATION_ID.eq(appId))
          .and(POLICY_VIOLATION.STAGE_TYPE_ID.in(stageTypeIds))
          .and(openedDuringRange.or(openedBeforeRange))
          .fetchInto(PolicyViolation.class);
    }
  }

  public List<PolicyViolation> getUnfixedLegacyViolationByApplicationId(TransactionContext tx, String appId) {
    return tx.dsl()
        .selectFrom(POLICY_VIOLATION)
        .where(POLICY_VIOLATION.APPLICATION_ID.eq(appId))
        .and(POLICY_VIOLATION.FIX_TIME.isNull())
        .and(POLICY_VIOLATION.LEGACY_VIOLATION_TIME.isNotNull())
        .fetchInto(PolicyViolation.class);
  }

  public int replacePolicyId(String fromPolicyId, String toPolicyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .update(POLICY_VIOLATION)
          .set(POLICY_VIOLATION.POLICY_ID, toPolicyId)
          .where(POLICY_VIOLATION.POLICY_ID.eq(fromPolicyId))
          .execute();
    }
  }

  public int replacePolicyId(TransactionContext tx, String applicationId, String fromPolicyId, String toPolicyId) {
    return tx.dsl()
        .update(POLICY_VIOLATION)
        .set(POLICY_VIOLATION.POLICY_ID, toPolicyId)
        .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
        .and(POLICY_VIOLATION.POLICY_ID.eq(fromPolicyId))
        .execute();
  }

  public int deleteFixedByApplicationIdAndDate(String applicationId, Date fixedBefore) {
    // For performance reasons, we bypass the standard delete (per entity) method here.

    if (isDatabaseEmbedded()) {
      // Deleting a potentially huge number of records from H2 in one shot consumes a lot of heap and blocks any other
      // database operation for a long time. To avoid this, we split the entire delete up into smaller batches.
      // See https://issues.sonatype.org/browse/CLM-15723 for details
      int deletedRows = 0;
      while (true) {
        List<String> ids;
        try (TransactionContext tx = createTransactionContext()) {
          ids = tx.dsl()
              .select(POLICY_VIOLATION.POLICY_VIOLATION_ID)
              .from(POLICY_VIOLATION)
              .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
              .and(POLICY_VIOLATION.FIX_TIME.lt(fixedBefore))
              .limit(DELETE_BATCH_SIZE)
              .fetchInto(String.class);
        }
        if (ids.isEmpty()) {
          return deletedRows;
        }
        try (TransactionContext tx = createTransactionContext()) {
          deletedRows += tx.dsl()
              .deleteFrom(POLICY_VIOLATION)
              .where(POLICY_VIOLATION.POLICY_VIOLATION_ID.in(ids))
              .execute();
        }
      }
    }
    else {
      // We cannot do this for H2 until we upgrade to a multi-threaded H2 version.
      // See https://issues.sonatype.org/browse/CLM-15723 for details
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .deleteFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.eq(applicationId))
            .and(POLICY_VIOLATION.FIX_TIME.lt(fixedBefore))
            .execute();
      }
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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.countDistinct(POLICY_VIOLATION.APPLICATION_ID))
          .from(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_VIOLATION.FIX_TIME.isNull())
          .and(POLICY_VIOLATION.WAIVE_TIME.isNull())
          .and(POLICY_VIOLATION.ACTION_TYPE_ID.eq(Action.ID_FAIL))
          .fetchOne(0, Integer.class);
    }
  }

  public int getCountActiveWaivers() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.WAIVE_TIME.isNotNull())
          .and(POLICY_VIOLATION.FIX_TIME.isNull())
          .fetchOne(0, Integer.class);
    }
  }

  /**
   * This method streams the policy violations that were either still open at the cutoff date or were created, waived,
   * fixed or resolved as legacy since the cutoff date. IOW, we ignore any policy violations that were resolved in some
   * fashion before the cutoff date.
   *
   * @param cutoffDate the cutoff date
   * @param batchSize number of rows to process in a batch
   * @param consumer the consumer to accept the policy violations
   */
  public void consumePolicyViolationsSinceDate(
      Date cutoffDate,
      int batchSize,
      Consumer<PolicyViolation> consumer) throws SQLException
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
          PreparedStatement statement = connection.prepareStatement(sQuery))
      {
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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.FIX_TIME.isNotNull()
              .or(POLICY_VIOLATION.WAIVE_TIME.isNotNull()))
          .fetchInto(PolicyViolation.class);
    }
  }

  public Map<String, SbomPolicyViolationSummaryDTO> getSbomPolicyViolationSummaryForAnApplication(
      Collection<String> applicationIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var pv = POLICY_VIOLATION;
      var criticalCount = DSL.count(DSL.case_().when(pv.THREAT_LEVEL.ge((short) 8), 1)).as("policyViolationCritical");
      var severeCount = DSL.count(DSL.case_().when(pv.THREAT_LEVEL.ge((short) 4).and(pv.THREAT_LEVEL.lt((short) 8)), 1))
          .as("policyViolationSevere");
      var moderateCount =
          DSL.count(DSL.case_().when(pv.THREAT_LEVEL.ge((short) 2).and(pv.THREAT_LEVEL.lt((short) 4)), 1))
              .as("policyViolationModerate");
      var lowCount = DSL.count(DSL.case_().when(pv.THREAT_LEVEL.lt((short) 2), 1)).as("policyViolationLow");

      var results = tx.dsl()
          .select(pv.APPLICATION_ID, criticalCount, severeCount, moderateCount, lowCount)
          .from(pv)
          .where(pv.FIX_TIME.isNull())
          .and(pv.WAIVE_TIME.isNull())
          .and(pv.STAGE_TYPE_ID.eq(ComplianceStageType.ID))
          .and(pv.APPLICATION_ID.in(applicationIds))
          .groupBy(pv.APPLICATION_ID)
          .fetch();

      Map<String, SbomPolicyViolationSummaryDTO> applicationIdResultMap = new HashMap<>();
      for (var record : results) {
        Object[] resultArray = new Object[]{
          record.get(pv.APPLICATION_ID),
          record.get(criticalCount),
          record.get(severeCount),
          record.get(moderateCount),
          record.get(lowCount)
        };
        applicationIdResultMap.put(record.get(pv.APPLICATION_ID), new SbomPolicyViolationSummaryDTO(resultArray));
      }

      return applicationIdResultMap;
    }
  }

  public ContainerImagePolicyViolationSummaryDTO getContainerImagePolicyViolationSummaryForRepository(
      String repositoryId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var org = ORGANIZATION;
      var app = APPLICATION;
      var pv = POLICY_VIOLATION;

      var criticalCount = DSL.count(DSL.case_().when(pv.THREAT_LEVEL.ge((short) 8), 1)).as("policyViolationCritical");
      var severeCount = DSL.count(DSL.case_().when(pv.THREAT_LEVEL.ge((short) 4).and(pv.THREAT_LEVEL.lt((short) 8)), 1))
          .as("policyViolationSevere");
      var moderateCount =
          DSL.count(DSL.case_().when(pv.THREAT_LEVEL.ge((short) 2).and(pv.THREAT_LEVEL.lt((short) 4)), 1))
              .as("policyViolationModerate");
      var affectedContainers = DSL.countDistinct(DSL.case_().when(pv.THREAT_LEVEL.gt((short) 1), pv.APPLICATION_ID))
          .as("affectedContainers");
      var containersInQuarantine =
          DSL.countDistinct(DSL.case_().when(pv.ACTION_TYPE_ID.eq(Action.ID_FAIL), pv.APPLICATION_ID))
              .as("containersInQuarantine");

      var record = tx.dsl()
          .select(criticalCount, severeCount, moderateCount, affectedContainers, containersInQuarantine)
          .from(org)
          .join(app)
          .on(org.RELATED_REPOSITORY_ID.eq(repositoryId).and(org.ORGANIZATION_ID.eq(app.ORGANIZATION_ID)))
          .join(pv)
          .on(pv.APPLICATION_ID.eq(app.APPLICATION_ID))
          .where(pv.FIX_TIME.isNull())
          .and(pv.WAIVE_TIME.isNull())
          .and(pv.STAGE_TYPE_ID.eq(ProxyStageType.ID))
          .fetchOne();

      if (record == null) {
        return new ContainerImagePolicyViolationSummaryDTO(new Object[]{0L, 0L, 0L, 0L, 0L});
      }

      // Convert counts to Long since jOOQ returns Integer for H2 but Long for PostgreSQL
      Object[] result = new Object[]{
        ((Number) record.get(criticalCount)).longValue(),
        ((Number) record.get(severeCount)).longValue(),
        ((Number) record.get(moderateCount)).longValue(),
        ((Number) record.get(affectedContainers)).longValue(),
        ((Number) record.get(containersInQuarantine)).longValue()
      };
      return new ContainerImagePolicyViolationSummaryDTO(result);
    }
  }

  public long getMeanTimeToRemediate(final int lookBackWindowDays) {
    // Using raw SQL here because of dialect-specific differences in timestamp arithmetic:
    // - PostgreSQL: EXTRACT(EPOCH FROM ...) and MAKE_INTERVAL
    // - H2: TIMESTAMPDIFF and DATEADD functions
    final String sQuery;

    if (isDatabasePostgresql()) {
      sQuery = "SELECT EXTRACT(EPOCH FROM AVG(LEAST(fix_time, waive_time) - open_time)) * 1000" +
          "  FROM " + getDatabaseSchema() + ".policy_violation" +
          "  WHERE (fix_time IS NOT null OR waive_time IS NOT null)" +
          "  AND open_time > CURRENT_DATE - MAKE_INTERVAL(days => ?)";
    }
    else {
      sQuery = "SELECT AVG(TIMESTAMPDIFF(MILLISECOND, open_time, LEAST(fix_time, waive_time)))" +
          "  FROM " + getDatabaseSchema() + ".policy_violation" +
          "  WHERE (fix_time IS NOT null OR waive_time IS NOT null)" +
          "  AND policy_violation.open_time > DATEADD(dd, -?, CURRENT_TIMESTAMP)";
    }

    try (TransactionContext tx = createTransactionContext()) {
      Double result = tx.dsl()
          .resultQuery(sQuery, lookBackWindowDays)
          .fetchOne(0, Double.class);

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
          useTemporaryTable
              ? ""
              : "AND pv.application_id IN " + buildPositionalParameters(applicationIds, appIdsParamStartPosition));

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
          JOIN %s.application application ON apv.application_id = application.application_id
          JOIN %s.organization organization ON application.organization_id = organization.organization_id
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

      // Build bindings list in the order parameters appear in the query after convertPositionalParams
      // The query has: stage_type_id IN (?), then application_id IN (?,?)
      List<Object> bindings = new ArrayList<>();
      bindings.addAll(stageTypeIds);
      if (!useTemporaryTable) {
        bindings.addAll(applicationIds);
      }
      if (minDate != null) {
        bindings.add(minDate);
      }
      if (minPolicyThreatLevel != null) {
        bindings.add(minPolicyThreatLevel);
      }
      if (maxPolicyThreatLevel != null) {
        bindings.add(maxPolicyThreatLevel);
      }
      if (policyThreatCategories != null) {
        bindings.addAll(policyThreatCategories);
      }

      // Convert positional parameters from ?N to ? format for jOOQ
      String convertedQuery = convertPositionalParams(sQuery);

      List<InternalDashboardViolationRiskDTO> results = tx.dsl()
          .resultQuery(convertedQuery, bindings.toArray())
          .fetch()
          .stream()
          .map(record -> new InternalDashboardViolationRiskDTO(
              record.get(0, String.class), // applicationId
              record.get(1, String.class), // applicationName
              record.get(2, String.class), // organizationName
              record.get(3, String.class), // policyViolationId
              record.get(4, String.class), // policyName
              record.get(5, Integer.class), // threatLevel
              record.get(6, String.class), // hash
              record.get(7, String.class), // filename
              record.get(8, String.class), // componentIdFormat
              record.get(9, String.class), // componentIdCoordinatesJson
              record.get(10, String.class), // constraintFactsId
              toEpochMillisFromTimestampOrLocalDateTime(record.get(11)), // firstOccurrenceTime
              record.get(12, String.class) // autoPolicyWaiverId
          ))
          .toList();

      return results;
    }
  }

  public List<PolicyViolation> getAutoWaivedByApplicationIdAndStageId(final String appId, final String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_VIOLATION)
          .where(POLICY_VIOLATION.APPLICATION_ID.eq(appId))
          .and(POLICY_VIOLATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_VIOLATION.FIX_TIME.isNull())
          .and(POLICY_VIOLATION.WAIVE_TIME.isNotNull())
          .and(POLICY_VIOLATION.AUTO_POLICY_WAIVER_ID.isNotNull())
          .fetchInto(PolicyViolation.class);
    }
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
    final org.jooq.Condition stateCondition = buildPolicyStateCondition(includeActive, includeWaived, includeLegacy);

    return getUnfixed(applicationIds, appIds -> {
      try (TransactionContext tx = createTransactionContext()) {
        var query = tx.dsl()
            .selectFrom(POLICY_VIOLATION)
            .where(POLICY_VIOLATION.APPLICATION_ID.in(appIds))
            .and(POLICY_VIOLATION.POLICY_ID.in(policyIds))
            .and(POLICY_VIOLATION.FIX_TIME.isNull());

        if (stateCondition != null) {
          query = query.and(stateCondition);
        }
        if (openTimeAfter != null) {
          query = query.and(POLICY_VIOLATION.OPEN_TIME.ge(openTimeAfter));
        }
        if (openTimeBefore != null) {
          query = query.and(POLICY_VIOLATION.OPEN_TIME.le(openTimeBefore));
        }

        return query.fetchInto(PolicyViolation.class);
      }
    });
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
      Long result = tx.dsl()
          .resultQuery(sQuery)
          .fetchOne(0, Long.class);
      return result != null ? result : 0L;
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
      return createNativePaginationQuery(tx, sQuery, offset, pageSize)
          .fetchStream()
          .map(record -> {
            Object[] array = record.intoArray();
            return new ContainerImageInQuarantineData(
                ((Number) array[0]).intValue(), // threatLevel
                toDateFromTimestampOrLocalDateTime(array[1]), // openTime
                (String) array[2], // applicationPublicId
                (String) array[3], // applicationId
                (String) array[4], // applicationName
                (String) array[5], // repositoryPublicId
                (String) array[6], // repositoryId
                ((Number) array[7]).longValue(), // policyViolationCount
                (String) array[8] // scanId
            );
          })
          .toList();
    }
  }

  /**
   * Converts a timestamp value (either java.sql.Timestamp or LocalDateTime) to a Date. Native SQL queries may return
   * different types depending on the database and driver.
   */
  private Date toDateFromTimestampOrLocalDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Timestamp ts) {
      return new Date(ts.getTime());
    }
    if (value instanceof LocalDateTime ldt) {
      return new Date(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
    throw new IllegalArgumentException("Unexpected type for timestamp conversion: " + value.getClass());
  }

  /**
   * Converts a timestamp value (either java.sql.Timestamp or LocalDateTime) to epoch milliseconds. Native SQL queries
   * may return different types depending on the database and driver.
   */
  private long toEpochMillisFromTimestampOrLocalDateTime(Object value) {
    if (value instanceof Timestamp ts) {
      return ts.getTime();
    }
    if (value instanceof LocalDateTime ldt) {
      return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    throw new IllegalArgumentException("Unexpected type for timestamp conversion: " + value.getClass());
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
      String scanId)
  {
  }

  /**
   * Converts numbered positional parameters (e.g., ?1, ?2) to simple ? placeholders for jOOQ. The bindings array must
   * be in the same order as the numbered parameters.
   */
  private static String convertPositionalParams(String query) {
    return query.replaceAll("\\?\\d+", "?");
  }

  @Override
  public Class<PolicyViolation> getEntityClass() {
    return PolicyViolation.class;
  }
}

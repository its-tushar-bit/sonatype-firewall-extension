/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField.SortableField;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyViolationSummary;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryPolicyViolation.REPOSITORY_POLICY_VIOLATION;
import static java.util.stream.Collectors.toMap;

/**
 * @since 1.17
 */
@Named
@Singleton
public class RepositoryPolicyViolationDAO
    extends AbstractPolicyViolationDAO<RepositoryPolicyViolation>
{
  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_POLICY_VIOLATION;
  }

  @Override
  public void insert(TransactionContext tx, RepositoryPolicyViolation entity) {
    storeConstraints(entity);
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, RepositoryPolicyViolation entity) {
    storeConstraints(entity);
    super.update(tx, entity);
  }

  @Inject
  public RepositoryPolicyViolationDAO(
      OperationalDataStore operationalDataStore,
      PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO)
  {
    super(operationalDataStore, policyViolationConstraintFactsDAO);
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getActiveByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathname(
      TransactionContext tx,
      String repositoryId,
      String pathname)
  {
    return tx.dsl()
        .selectFrom(REPOSITORY_POLICY_VIOLATION)
        .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
        .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
        .orderBy(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL.desc(), REPOSITORY_POLICY_VIOLATION.POLICY_ID)
        .fetch(this::toEntity);
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnameAndWaived(
      String repositoryId,
      String pathname,
      boolean isWaived)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
          .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(isWaived))
          .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
          .orderBy(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL.desc(), REPOSITORY_POLICY_VIOLATION.POLICY_ID)
          .fetch(this::toEntity);
    }
  }

  /**
   * @since 1.78
   */
  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathnameAndActionAndNotWaived(
      String repositoryId,
      String pathname,
      String actionTypeId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
          .and(REPOSITORY_POLICY_VIOLATION.ACTION_TYPE_ID.eq(actionTypeId))
          .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(false))
          .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
          .fetch(this::toEntity);
    }
  }

  public List<RepositoryPolicyViolation> getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .fetch(this::toEntity);
    }
  }

  /**
   * Gets a paginated list of policy violations for a repository, optionally filtering by time. Results are ordered by
   * ID for consistent pagination.
   *
   * @param tx the transaction context
   * @param repositoryId the repository ID to filter by
   * @param beforeDate optional date filter - if provided, only violations with time before this date are returned
   * @param offset the offset for pagination
   * @param pageSize the page size for pagination
   * @return list of repository policy violations
   */
  public List<RepositoryPolicyViolation> getByRepositoryIdPaginated(
      TransactionContext tx,
      String repositoryId,
      Date beforeDate,
      int offset,
      int pageSize)
  {
    var query = tx.dsl()
        .selectFrom(REPOSITORY_POLICY_VIOLATION)
        .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId));

    if (beforeDate != null) {
      query = query.and(REPOSITORY_POLICY_VIOLATION.TIME.lt(beforeDate));
    }

    return query.orderBy(REPOSITORY_POLICY_VIOLATION.REPOSITORY_POLICY_VIOLATION_ID)
        .offset(offset)
        .limit(pageSize)
        .fetch(this::toEntity);
  }

  public PolicyViolationSummary getPolicyViolationSummary(final String repositoryId) {
    String sQuery =
        " SELECT COUNT(CASE WHEN max_threat_level >= 8 THEN 1 END)                              AS criticalCount," +
            "        COUNT(CASE WHEN max_threat_level >= 4 AND max_threat_level < 8 THEN 1 END) AS severeCount," +
            "        COUNT(CASE WHEN max_threat_level >= 2 AND max_threat_level < 4 THEN 1 END) AS moderateCount" +
            " FROM (SELECT MAX(threat_level) AS max_threat_level" +
            "       FROM " + getDatabaseSchema() + ".repository_policy_violation" +
            "       WHERE repository_id=?" +
            "         AND active=true" +
            "         AND waived=false" +
            "       GROUP BY pathname) AS subquery";

    try (TransactionContext tx = createTransactionContext()) {
      Object[] result = tx.dsl()
          .resultQuery(sQuery, repositoryId)
          .fetchOne()
          .intoArray();
      return new PolicyViolationSummary(
          result[0] == null ? null : ((Number) result[0]).longValue(),
          result[1] == null ? null : ((Number) result[1]).longValue(),
          result[2] == null ? null : ((Number) result[2]).longValue());
    }
  }

  public List<RepositoryPolicyViolation> getActiveWaivedRepositoryPolicyViolations(
      final Collection<String> repositoryIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      List<RepositoryPolicyViolation> repositoryPolicyViolations = new ArrayList<>();
      for (String repositoryId : repositoryIds) {
        repositoryPolicyViolations.addAll(
            tx.dsl()
                .selectFrom(REPOSITORY_POLICY_VIOLATION)
                .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
                .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
                .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(true))
                .fetch(this::toEntity));
      }
      return repositoryPolicyViolations;
    }
  }

  public void deleteByRepositoryId(TransactionContext tx, String repositoryId) {
    if (isDatabaseEmbedded()) {
      // We do not enroll the deletions in the transaction on purpose.
      // This improves performance and keeps db operations (including commits) reasonably short, which means other
      // concurrent db operations are blocked for shorter periods of time (H2 is single threaded).
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      getByRepositoryId(repositoryId).forEach(this::delete);
    }
    else {
      // For performance reasons, we bypass the standard delete (per entity) method here.
      // We cannot do this for H2 until we upgrade to a multi-threaded H2 version.
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      tx.dsl()
          .deleteFrom(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .execute();
    }
  }

  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathname(
      TransactionContext tx,
      String repositoryId,
      String pathname)
  {
    return tx.dsl()
        .selectFrom(REPOSITORY_POLICY_VIOLATION)
        .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
        .fetch(this::toEntity);
  }

  /**
   * @since 1.126
   */
  public int getQuarantinedPolicyViolationsCountByRepositoryIdAndPathname(
      String repositoryId,
      String pathname)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
          .and(REPOSITORY_POLICY_VIOLATION.ACTION_TYPE_ID.eq("fail"))
          .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(false))
          .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
          .fetchOne(0, Integer.class);
    }
  }

  public List<RepositoryResultsDetails> getRepositoryResultsDetails(
      Set<String> repositoryIds,
      RepositoryResultsDetailsFilter detailsFilter)
  {
    if (detailsFilter.aggregate) {
      return getRepositoryResultsDetailsAggregate(repositoryIds, detailsFilter);
    }
    else {
      return getRepositoryResultsDetailsNonAggregate(repositoryIds, detailsFilter);
    }
  }

  /**
   * @since 1.140.0
   */
  private List<RepositoryResultsDetails> getRepositoryResultsDetailsNonAggregate(
      Set<String> repositoryIds,
      RepositoryResultsDetailsFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      List<Object> params = new ArrayList<>();

      String baseQuery = "SELECT violation.threat_level," + //
          " violation.policy_name," + //
          " repository.repository_manager_id," + //
          " component.repository_id," + //
          " component.component_id_format," + //
          " component.pathname," + //
          " component.component_id_coordinates_json," + //
          " component.display_name," + //
          " component.hash," + //
          " component.match_state_id," + //
          " CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL) THEN" + //
          " component.quarantine_time END AS quarantine_time," + //
          " violation.waived" + //
          " FROM " + getDatabaseSchema() + ".repository_component component" + //
          ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") + //
          " " + getDatabaseSchema() + ".repository_policy_violation violation" + //
          " ON component.repository_id = violation.repository_id AND component.pathname = violation.pathname" + //
          " INNER JOIN " + getDatabaseSchema() + ".repository ON component.repository_id = repository.repository_id" +
          " WHERE component.repository_id IN " +
          buildJooqPositionalParameters(repositoryIds);

      StringBuilder sQuery = new StringBuilder(baseQuery);
      params.addAll(repositoryIds);

      String threatLevelClause = addThreatLevelFiltersJooq(detailsFilter.threatLevelFilters);
      sQuery.append(threatLevelClause);
      // Only add params if the clause was actually added (not empty)
      if (!threatLevelClause.isEmpty() && detailsFilter.threatLevelFilters != null
          && detailsFilter.threatLevelFilters.size() == 2)
      {
        params.add(detailsFilter.threatLevelFilters.get(0));
        params.add(detailsFilter.threatLevelFilters.get(1));
      }

      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      sQuery.append(addSearchFiltersJooq(detailsFilter.searchFilters));
      if (detailsFilter.searchFilters.containsKey("POLICY_NAME")) {
        params.add('%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("QUARANTINE_TIME")) {
        params.add('%' + detailsFilter.searchFilters.get("QUARANTINE_TIME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("COMPONENT_COORDINATES")) {
        params.add('%' + detailsFilter.searchFilters.get("COMPONENT_COORDINATES") + '%');
      }

      if (!detailsFilter.matchStateFilter.isEmpty()) {
        sQuery.append(" AND component.match_state_id = ?");
        params.add(detailsFilter.matchStateFilter);
      }

      sQuery.append(addFormatExclusionFiltersJooq(detailsFilter.formatExclusionPatterns, params));

      sQuery.append(validateAndAddSortFields(detailsFilter.sortFields));

      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;
      // Incremented page size to help UI determine whether to enable / disable NextPage button
      int pageSize = detailsFilter.pageSize + 1;
      sQuery.append(" OFFSET ? LIMIT ?");
      params.add(offset);
      params.add(pageSize);

      List<RepositoryResultsDetails> results = tx.dsl()
          .resultQuery(sQuery.toString(), params.toArray())
          .fetchStream()
          .map(record -> {
            Object[] array = record.intoArray();
            return new RepositoryResultsDetails(getInteger(array[0]), (String) array[1],
                (String) array[2], (String) array[3], (String) array[4], (String) array[5], (String) array[6],
                (String) array[7], (String) array[8], (String) array[9],
                array[10] == null ? null : new Date(((Timestamp) array[10]).getTime()), (Boolean) array[11]);
          })
          .collect(Collectors.toList());

      return results;
    }
  }

  public List<RepositoryResultsDetails> getRepositoryResultsDetailsAggregate(
      Set<String> repositoryIds,
      RepositoryResultsDetailsFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      List<Object> params = new ArrayList<>();

      String part1 = "MAX(CONCAT(LPAD(CAST(violation.threat_level AS varchar), 2, '0'), violation.policy_name))";
      String part2 = "SUBSTRING(threat_level_and_policy_name, 1, 2)";
      String part3 = "SUBSTRING(threat_level_and_policy_name, 3)";
      String[] threatLevelPolicyNameParts = new String[]{part1, part2, part3};

      StringBuilder select1Builder = new StringBuilder();
      select1Builder.append("SELECT")
          .append(" repository.repository_manager_id,")
          .append(" component.repository_id,")
          .append(" component.pathname,")
          .append(" ")
          .append(threatLevelPolicyNameParts[0])
          .append(" AS threat_level_and_policy_name,")
          .append(" MAX(CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL)")
          .append(" THEN component.quarantine_time END) AS quarantine_time,")
          .append(" MAX(component.display_name) AS display_name")
          .append(" FROM ")
          .append(getDatabaseSchema())
          .append(".repository_component component")
          .append((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN")
          .append(" ")
          .append(getDatabaseSchema())
          .append(".repository_policy_violation violation")
          .append(" ON component.repository_id = violation.repository_id")
          .append(" AND component.pathname = violation.pathname")
          .append(" INNER JOIN ")
          .append(getDatabaseSchema())
          .append(".repository ON component.repository_id = repository.repository_id")
          .append(" WHERE component.repository_id IN ")
          .append(buildJooqPositionalParameters(repositoryIds));

      params.addAll(repositoryIds);

      String threatLevelClause = addThreatLevelFiltersJooq(detailsFilter.threatLevelFilters);
      select1Builder.append(threatLevelClause);
      // Only add params if the clause was actually added (not empty)
      if (!threatLevelClause.isEmpty() && detailsFilter.threatLevelFilters != null
          && detailsFilter.threatLevelFilters.size() == 2)
      {
        params.add(detailsFilter.threatLevelFilters.get(0));
        params.add(detailsFilter.threatLevelFilters.get(1));
      }

      select1Builder.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      select1Builder.append(addSearchFiltersJooq(detailsFilter.searchFilters));
      if (detailsFilter.searchFilters.containsKey("POLICY_NAME")) {
        params.add('%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("QUARANTINE_TIME")) {
        params.add('%' + detailsFilter.searchFilters.get("QUARANTINE_TIME") + '%');
      }
      if (detailsFilter.searchFilters.containsKey("COMPONENT_COORDINATES")) {
        params.add('%' + detailsFilter.searchFilters.get("COMPONENT_COORDINATES") + '%');
      }

      if (!detailsFilter.matchStateFilter.isEmpty()) {
        select1Builder.append(" AND component.match_state_id = ?");
        params.add(detailsFilter.matchStateFilter);
      }

      select1Builder.append(addFormatExclusionFiltersJooq(detailsFilter.formatExclusionPatterns, params));

      select1Builder.append(" GROUP BY repository.repository_manager_id, component.repository_id, component.pathname");

      String select1 = select1Builder.toString();

      // Incremented page size to help UI determine whether to enable / disable NextPage button
      int pageSize = detailsFilter.pageSize + 1;
      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;
      String select2 = "SELECT" +
          " repository_manager_id," +
          " repository_id," +
          " pathname," +
          " CASE WHEN(threat_level_and_policy_name <> '')" +
          " THEN CAST(" + threatLevelPolicyNameParts[1] + " AS integer) " +
          " ELSE NULL END AS threat_level," +
          " CASE WHEN(threat_level_and_policy_name <> '')" +
          " THEN " + threatLevelPolicyNameParts[2] +
          " ELSE NULL END AS policy_name," +
          " quarantine_time," +
          " display_name" +
          " FROM (" + select1 + ") AS t1";

      String select3 = "SELECT" +
          " threat_level," +
          " policy_name," +
          " repository_manager_id," +
          " component.repository_id," +
          " component.component_id_format," +
          " component.pathname," +
          " component.component_id_coordinates_json," +
          " component.display_name," +
          " component.hash," +
          " component.match_state_id," +
          " CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL)" +
          " THEN component.quarantine_time END AS quarantine_time" +
          " FROM " + getDatabaseSchema() + ".repository_component component" +
          " INNER JOIN (" + select2 + ") AS t2" +
          " ON t2.pathname = component.pathname AND t2.repository_id = component.repository_id" +
          validateAndAddSortFields(detailsFilter.sortFields) +
          " LIMIT " + pageSize +
          " OFFSET " + offset;

      List<RepositoryResultsDetails> results = tx.dsl()
          .resultQuery(select3, params.toArray())
          .fetchStream()
          .map(record -> {
            Object[] array = record.intoArray();
            return new RepositoryResultsDetails(
                getInteger(array[0]),
                (String) array[1],
                (String) array[2],
                (String) array[3],
                (String) array[4],
                (String) array[5],
                (String) array[6],
                (String) array[7],
                (String) array[8],
                (String) array[9],
                array[10] == null ? null : new Date(((Timestamp) array[10]).getTime()),
                null // waived doesn't make sense in an aggregation
            );
          })
          .collect(Collectors.toList());

      return results;
    }
  }

  public Map<Integer, Integer> getCountsByPolicyThreatLevel(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL, DSL.count())
          .from(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId)
              .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
              .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(false)))
          .groupBy(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL)
          .fetchStream()
          .collect(toMap(
              record -> record.get(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL).intValue(),
              record -> record.get(1, Integer.class)));
    }
  }

  private boolean hasNonViolatingFilter(final Set<String> violationStateFilters) {
    return violationStateFilters.stream()
        .anyMatch(filter -> filter.equals("VIOLATION_STATE_ALL") || filter.equals("VIOLATION_STATE_NOT_VIOLATING"));
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
            query.append(" violation.policy_name IS NULL");
            break;
          case "VIOLATION_STATE_OPEN":
            query.append(filterCount > 1 ? " OR" : " AND (");
            query.append(" violation.waived = false");
            break;
          case "VIOLATION_STATE_QUARANTINED":
            query.append(filterCount > 1 ? " OR" : " AND (");
            query.append(
                " (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL" + //
                    " AND violation.action_type_id = 'fail')");
            break;
          case "VIOLATION_STATE_WAIVED":
            query.append(filterCount > 1 ? " OR" : " AND (");
            query.append(" violation.waived = true");
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
        return " AND (violation.threat_level IS NULL OR" +
            " (violation.threat_level >= ?" + paramStartPosition + " AND violation.threat_level <= ?" +
            (++paramStartPosition) + "))";
      }
      return " AND violation.threat_level >= ?" + paramStartPosition + " AND violation.threat_level <= ?" +
          (++paramStartPosition);
    }
    return "";
  }

  private static String addThreatLevelFiltersJooq(List<Integer> filters) {
    if (filters != null && filters.size() == 2 && (filters.get(0) > 0 || filters.get(1) < 10)) {
      if (filters.get(0) == 0) {
        return " AND (violation.threat_level IS NULL OR" +
            " (violation.threat_level >= ? AND violation.threat_level <= ?))";
      }
      return " AND violation.threat_level >= ? AND violation.threat_level <= ?";
    }
    return "";
  }

  private static String addSearchFilters(Map<String, String> filters, int paramStartPosition) {
    StringBuilder query = new StringBuilder();
    if (!MapUtils.isEmpty(filters)) {
      for (Entry<String, String> filter : filters.entrySet()) {
        if (filter.getKey().equals("POLICY_NAME")) {
          query.append(" AND LOWER(violation.policy_name) LIKE ?" + paramStartPosition);
        }
        if (filter.getKey().equals("QUARANTINE_TIME")) {
          query.append(" AND (quarantine_time IS NOT NULL AND TO_CHAR(quarantine_time, 'YYYY-MM-DD') LIKE ?" +
              (paramStartPosition + 1) + ")");
        }
        if (filter.getKey().equals("COMPONENT_COORDINATES")) {
          query.append(" AND LOWER(component.display_name) LIKE ?" + (paramStartPosition + 2));
        }
      }
    }

    return query.toString();
  }

  private static String addSearchFiltersJooq(Map<String, String> filters) {
    StringBuilder query = new StringBuilder();
    if (!MapUtils.isEmpty(filters)) {
      // Ensure consistent order: POLICY_NAME, QUARANTINE_TIME, COMPONENT_COORDINATES
      // This order must match the order parameters are added in callers
      if (filters.containsKey("POLICY_NAME")) {
        query.append(" AND LOWER(violation.policy_name) LIKE ?");
      }
      if (filters.containsKey("QUARANTINE_TIME")) {
        query.append(" AND (quarantine_time IS NOT NULL AND TO_CHAR(quarantine_time, 'YYYY-MM-DD') LIKE ?)");
      }
      if (filters.containsKey("COMPONENT_COORDINATES")) {
        query.append(" AND LOWER(component.display_name) LIKE ?");
      }
    }

    return query.toString();
  }

  /**
   * Adds SQL clauses to exclude certain pathname patterns based on component format. For example, exclude .json files
   * for NuGet components.
   *
   * @param formatExclusionPatterns map of format -> list of pathname patterns to exclude (using SQL LIKE syntax)
   * @param params the parameter list to add values to
   * @return SQL clause string
   */
  private static String addFormatExclusionFiltersJooq(
      final Map<String, List<String>> formatExclusionPatterns,
      final List<Object> params)
  {
    if (MapUtils.isEmpty(formatExclusionPatterns)) {
      return "";
    }

    StringBuilder query = new StringBuilder();
    for (Entry<String, List<String>> entry : formatExclusionPatterns.entrySet()) {
      String format = entry.getKey();
      List<String> patterns = entry.getValue();
      if (patterns != null) {
        for (String pattern : patterns) {
          query.append(" AND NOT (component.component_id_format = ? AND component.pathname LIKE ?)");
          params.add(format);
          params.add(pattern);
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
      case COMPONENT_COORDINATES:
        return "display_name";
      case QUARANTINE_TIME:
        return "quarantine_time";
      default:
        return "";
    }
  }

  /**
   * Builds a jOOQ-compatible positional parameters string using ? placeholders.
   *
   * @param collection the collection of values
   * @return a string like "(?, ?, ?)" for use in jOOQ resultQuery
   */
  private String buildJooqPositionalParameters(Collection<?> collection) {
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < collection.size(); i++) {
      joiner.add("?");
    }
    return "(" + joiner.toString() + ")";
  }

  @Override
  public Class<RepositoryPolicyViolation> getEntityClass() {
    return RepositoryPolicyViolation.class;
  }
}

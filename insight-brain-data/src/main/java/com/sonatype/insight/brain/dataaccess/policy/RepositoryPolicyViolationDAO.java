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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField.SortableField;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyViolationSummary;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toMap;

/**
 * @since 1.17
 */
@Named
@Singleton
public class RepositoryPolicyViolationDAO
    extends AbstractPolicyViolationDAO<RepositoryPolicyViolation>
{
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
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.active=true" + //
        " ORDER BY entity.threatLevel DESC, entity.policyId";
    return getList(tx, sQuery, repositoryId, pathname);
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnameAndWaived(
      String repositoryId,
      String pathname,
      boolean isWaived)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.isWaived=?3" + //
        " AND entity.active=true" + //
        " ORDER BY entity.threatLevel DESC, entity.policyId";
    return getList(sQuery, repositoryId, pathname, isWaived);
  }

  /**
   * @since 1.78
   */
  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathnameAndActionAndNotWaived(
      String repositoryId,
      String pathname,
      String actionTypeId)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.actionTypeId=?3" + //
        " AND entity.isWaived=false" + //
        " AND entity.active=true";
    return getList(sQuery, repositoryId, pathname, actionTypeId);
  }

  public List<RepositoryPolicyViolation> getByRepositoryId(String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1";
    return getList(sQuery, repositoryId);
  }

  public PolicyViolationSummary getPolicyViolationSummary(final String repositoryId) {
    String sQuery =
        " SELECT COUNT(CASE WHEN max_threat_level >= 8 THEN 1 END)                              AS criticalCount," +
            "        COUNT(CASE WHEN max_threat_level >= 4 AND max_threat_level < 8 THEN 1 END) AS severeCount," +
            "        COUNT(CASE WHEN max_threat_level >= 2 AND max_threat_level < 4 THEN 1 END) AS moderateCount" +
            " FROM (SELECT MAX(threat_level) AS max_threat_level" +
            "       FROM " + getDatabaseSchema() + ".repository_policy_violation" +
            "       WHERE repository_id=?1" +
            "         AND active=true" +
            "         AND waived=false" +
            "       GROUP BY pathname) AS subquery";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, repositoryId);
      Object[] result = (Object[]) query.getSingleResult();
      return new PolicyViolationSummary((Long) result[0], (Long) result[1], (Long) result[2]);
    }
  }

  public List<RepositoryPolicyViolation> getActiveWaivedRepositoryPolicyViolations(
      final Collection<String> repositoryIds)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.active=true" + //
        " AND entity.isWaived=true";
    return getRepositoryPolicyViolations(sQuery, repositoryIds);
  }

  private List<RepositoryPolicyViolation> getRepositoryPolicyViolations(
      String sQuery,
      Collection<String> repositoryIds)
  {
    List<RepositoryPolicyViolation> repositoryPolicyViolations = new ArrayList<>();
    for (String repositoryId : repositoryIds) {
      Object[] parameters = {repositoryId};
      repositoryPolicyViolations.addAll(getList(sQuery, parameters));
    }

    return repositoryPolicyViolations;
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.active=true";
    return getList(tx, sQuery, repositoryId);
  }

  @Override
  public final void delete(RepositoryPolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, RepositoryPolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(tx, entity);
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
      String sQuery = "DELETE FROM RepositoryPolicyViolation entity WHERE entity.repositoryId=?1";
      createQuery(sQuery, repositoryId).executeUpdate(tx);
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
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2";
    return getList(tx, sQuery, repositoryId, pathname);
  }

  /**
   * @since 1.126
   */
  public int getQuarantinedPolicyViolationsCountByRepositoryIdAndPathname(
      String repositoryId,
      String pathname)
  {
    String sQuery = "SELECT COUNT(entity) FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.actionTypeId='fail'" + //
        " AND entity.isWaived=false" + //
        " AND entity.active=true";
    return getSingle(Number.class, sQuery, repositoryId, pathname).intValue();
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
      int repositoryIdsSize = repositoryIds.size();
      int threatLevelFiltersSize =
          detailsFilter.threatLevelFilters != null ? detailsFilter.threatLevelFilters.size() : 0;
      // POLICY_NAME, QUARANTINE_TIME, COMPONENT_COORDINATES
      // are the only possible search filters used in the Query (as of Mar 2024)
      int searchFiltersMaxSize = 3;
      int repositoryIdsParamStartPosition = 1;
      int threatLevelFiltersParamStartPosition = repositoryIdsSize + 1;
      int searchFiltersParamStartPosition = repositoryIdsSize + threatLevelFiltersSize + 1;
      int matchStateFilterParamPosition = repositoryIdsSize + threatLevelFiltersSize + searchFiltersMaxSize + 1;

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
          buildPositionalParameters(repositoryIds, repositoryIdsParamStartPosition);

      StringBuilder sQuery = new StringBuilder(baseQuery);

      sQuery.append(addThreatLevelFilters(detailsFilter.threatLevelFilters, threatLevelFiltersParamStartPosition));

      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      sQuery.append(addSearchFilters(detailsFilter.searchFilters, searchFiltersParamStartPosition));

      if (!detailsFilter.matchStateFilter.isEmpty()) {
        sQuery.append(" AND component.match_state_id = ?" + matchStateFilterParamPosition);
      }

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
          '%' + detailsFilter.searchFilters.get("COMPONENT_COORDINATES") + '%');
      query.setParameter(matchStateFilterParamPosition, detailsFilter.matchStateFilter);
      query.setFirstResult(offset)
          .setMaxResults(detailsFilter.pageSize +
              1); // Incremented page size to help UI determine whether to enable / disable NextPage button

      List<RepositoryResultsDetails> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new RepositoryResultsDetails(getInteger(array[0]), (String) array[1],
              (String) array[2], (String) array[3], (String) array[4], (String) array[5], (String) array[6],
              (String) array[7], (String) array[8], (String) array[9],
              array[10] == null ? null : new Date(((Timestamp) array[10]).getTime()), (Boolean) array[11]))
          .collect(Collectors.toList());

      return results;
    }
  }

  public List<RepositoryResultsDetails> getRepositoryResultsDetailsAggregate(
      Set<String> repositoryIds,
      RepositoryResultsDetailsFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      String part1 = "MAX(CONCAT(LPAD(CAST(violation.threat_level AS varchar), 2, '0'), violation.policy_name))";
      String part2 = "SUBSTRING(threat_level_and_policy_name, 1, 2)";
      String part3 = "SUBSTRING(threat_level_and_policy_name, 3)";
      String[] threatLevelPolicyNameParts = new String[]{part1, part2, part3};
      int repositoryIdsSize = repositoryIds.size();
      int threatLevelFiltersSize =
          detailsFilter.threatLevelFilters != null ? detailsFilter.threatLevelFilters.size() : 0;
      // POLICY_NAME, QUARANTINE_TIME, COMPONENT_COORDINATES
      // are the only possible search filters used in the Query (as of Mar 2024)
      int searchFiltersMaxSize = 3;
      int repositoryIdsParamStartPosition = 1;
      int threatLevelFiltersParamStartPosition = repositoryIdsSize + 1;
      int searchFiltersParamStartPosition = repositoryIdsSize + threatLevelFiltersSize + 1;
      int matchStateFilterParamPosition = repositoryIdsSize + threatLevelFiltersSize + searchFiltersMaxSize + 1;

      String select1 = "SELECT" +
          " repository.repository_manager_id," +
          " component.repository_id," +
          " component.pathname," +
          " " + threatLevelPolicyNameParts[0] +
          " AS threat_level_and_policy_name," +
          " MAX(CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL)" +
          " THEN component.quarantine_time END) AS quarantine_time," +
          " MAX(component.display_name) AS display_name" +
          " FROM " + getDatabaseSchema() + ".repository_component component" +
          ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") +
          " " + getDatabaseSchema() + ".repository_policy_violation violation" +
          " ON component.repository_id = violation.repository_id" +
          " AND component.pathname = violation.pathname" +
          " INNER JOIN " + getDatabaseSchema() + ".repository ON component.repository_id = repository.repository_id" +
          " WHERE component.repository_id IN " +
          buildPositionalParameters(repositoryIds, repositoryIdsParamStartPosition) +
          addThreatLevelFilters(detailsFilter.threatLevelFilters, threatLevelFiltersParamStartPosition) +
          addViolationStateFilters(detailsFilter.violationStateFilters) +
          addSearchFilters(detailsFilter.searchFilters, searchFiltersParamStartPosition) +
          (!detailsFilter.matchStateFilter.isEmpty()
              ? " AND component.match_state_id = ?" + matchStateFilterParamPosition
              : "")
          +
          " GROUP BY repository.repository_manager_id, component.repository_id, component.pathname";

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

      jakarta.persistence.Query query = tx.createNativeQuery(select3);
      addPositionalParameters(query, repositoryIds, repositoryIdsParamStartPosition);
      if (detailsFilter.threatLevelFilters != null && detailsFilter.threatLevelFilters.size() == 2) {
        query.setParameter(threatLevelFiltersParamStartPosition, detailsFilter.threatLevelFilters.get(0));
        query.setParameter(threatLevelFiltersParamStartPosition + 1, detailsFilter.threatLevelFilters.get(1));
      }
      query.setParameter(searchFiltersParamStartPosition, '%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      query.setParameter(searchFiltersParamStartPosition + 1,
          '%' + detailsFilter.searchFilters.get("QUARANTINE_TIME") + '%');
      query.setParameter(searchFiltersParamStartPosition + 2,
          '%' + detailsFilter.searchFilters.get("COMPONENT_COORDINATES") + '%');
      query.setParameter(matchStateFilterParamPosition, detailsFilter.matchStateFilter);

      List<RepositoryResultsDetails> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new RepositoryResultsDetails(
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
          ))
          .collect(Collectors.toList());

      return results;
    }
  }

  public Map<Integer, Integer> getCountsByPolicyThreatLevel(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT threat_level, COUNT(*) AS number_of_policy_violations" + //
          " FROM " + getDatabaseSchema() + ".repository_policy_violation violation" + //
          " WHERE repository_id = ?1 AND active = true AND waived = false" + //
          " GROUP BY threat_level";

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, repositoryId);

      return ((Stream<Object[]>) query.getResultStream()) //
          .collect(toMap(row -> getInteger(row[0]), row -> getInteger(row[1])));
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
}

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

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField.SortableField;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toMap;

/**
 * @since 1.17
 */
public class RepositoryPolicyViolationDAO
    extends AbstractOperationalSqlDAO<RepositoryPolicyViolation>
{
  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getActiveByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathname(TransactionContext tx,
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

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnameAndWaived(String repositoryId,
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

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndNotWaived(final String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.active=true" + //
        " AND entity.isWaived=false";
    return getList(sQuery, repositoryId);
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
      String repositoryId, RepositoryResultsDetailsFilter detailsFilter)
  {
    if (detailsFilter.aggregate) {
      return getRepositoryResultsDetailsAggregate(repositoryId, detailsFilter);
    }
    else {
      return getRepositoryResultsDetailsNonAggregate(repositoryId, detailsFilter);
    }
  }

  /**
   * @since 1.140.0
   */
  private List<RepositoryResultsDetails> getRepositoryResultsDetailsNonAggregate(
      String repositoryId, RepositoryResultsDetailsFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      String baseQuery = "SELECT violation.threat_level," + //
          " violation.policy_name," + //
          " component.component_id_format," + //
          " component.pathname," + //
          " component.component_id_coordinates_json," + //
          " component.display_name," + //
          " component.hash," + //
          " component.match_state_id," + //
          " CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL) THEN" + //
          " component.quarantine_time END AS quarantine_time," + //
          " violation.waived" + //
          " FROM " + OperationalDataStoreProvider.getDatabaseSchema() + ".repository_component component" + //
          ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") + //
          " " + OperationalDataStoreProvider.getDatabaseSchema() + ".repository_policy_violation violation" + //
          " ON component.repository_id = violation.repository_id AND component.pathname = violation.pathname" + //
          " WHERE component.repository_id = ?1";

      StringBuilder sQuery = new StringBuilder(baseQuery);

      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      sQuery.append(addSearchFilters(detailsFilter.searchFilters));

      if (!detailsFilter.matchStateFilter.isEmpty()) {
        sQuery.append(" AND component.match_state_id = ?4");
      }

      sQuery.append(validateAndAddSortFields(detailsFilter.sortFields));

      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;

      javax.persistence.Query query = tx.createNativeQuery(sQuery.toString());
      query.setParameter(1, repositoryId);
      query.setParameter(2, '%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      query.setParameter(3, '%' + detailsFilter.searchFilters.get("COMPONENT_COORDINATES") + '%');
      query.setParameter(4, detailsFilter.matchStateFilter);
      query.setFirstResult(offset).setMaxResults(detailsFilter.pageSize +
          1); // Incremented page size to help UI determine whether to enable / disable NextPage button

      List<RepositoryResultsDetails> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new RepositoryResultsDetails(getInteger(array[0]), (String) array[1],
              (String) array[2], (String) array[3], (String) array[4], (String) array[5], (String) array[6],
              (String) array[7],
              array[8] == null ? null : new Date(((Timestamp) array[8]).getTime()), (Boolean) array[9]))
          .collect(Collectors.toList());

      return results;
    }
  }

  public List<RepositoryResultsDetails> getRepositoryResultsDetailsAggregate(
      String repositoryId,
      RepositoryResultsDetailsFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      String[] threatLevelPolicyNameParts = getThreatLevelPolicyNameParts(detailsFilter);

      String select1 = "SELECT" +
          " component.pathname," +
          " " + threatLevelPolicyNameParts[0] +
          " AS threat_level_and_policy_name," +
          " MAX(CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL)" +
          " THEN component.quarantine_time END) AS quarantine_time," +
          " MAX(component.display_name) AS display_name" +
          " FROM " + OperationalDataStoreProvider.getDatabaseSchema() + ".repository_component component" +
          ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") +
          " " + OperationalDataStoreProvider.getDatabaseSchema() + ".repository_policy_violation violation" +
          " ON component.repository_id = violation.repository_id" +
          " AND component.pathname = violation.pathname" +
          " WHERE component.repository_id = ?1" +
          addViolationStateFilters(detailsFilter.violationStateFilters) +
          addSearchFilters(detailsFilter.searchFilters) +
          (!detailsFilter.matchStateFilter.isEmpty() ? " AND component.match_state_id = ?4" : "") +
          " GROUP BY component.pathname";

      // Incremented page size to help UI determine whether to enable / disable NextPage button
      int pageSize = detailsFilter.pageSize + 1;
      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;
      String select2 = "SELECT" +
          " pathname," +
          " CASE WHEN(threat_level_and_policy_name <> '')" +
          " THEN CAST(" + threatLevelPolicyNameParts[1] + " AS integer) " +
          " ELSE NULL END AS threat_level," +
          " CASE WHEN(threat_level_and_policy_name <> '')" +
          " THEN " + threatLevelPolicyNameParts[2] +
          " ELSE NULL END AS policy_name," +
          " quarantine_time," +
          " display_name" +
          " FROM (" + select1 + ") AS t1" +
          validateAndAddSortFields(detailsFilter.sortFields) +
          " LIMIT " + pageSize +
          " OFFSET " + offset;

      String select3 = "SELECT" +
          " threat_level," +
          " policy_name," +
          " component.component_id_format," +
          " component.pathname," +
          " component.component_id_coordinates_json," +
          " component.display_name," +
          " component.hash," +
          " component.match_state_id," +
          " CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL)" +
          " THEN component.quarantine_time END AS quarantine_time" +
          " FROM " + OperationalDataStoreProvider.getDatabaseSchema() + ".repository_component component" +
          " INNER JOIN (" + select2 + ") AS t2" +
          " ON t2.pathname = component.pathname" +
          " AND component.repository_id = ?1";

      javax.persistence.Query query = tx.createNativeQuery(select3);
      query.setParameter(1, repositoryId);
      query.setParameter(2, '%' + detailsFilter.searchFilters.get("POLICY_NAME") + '%');
      query.setParameter(3, '%' + detailsFilter.searchFilters.get("COMPONENT_COORDINATES") + '%');
      query.setParameter(4, detailsFilter.matchStateFilter);

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
              array[8] == null ? null : new Date(((Timestamp) array[8]).getTime()),
              null // waived doesn't make sense in an aggregation
          )).collect(Collectors.toList());

      return results;
    }
  }

  private String[] getThreatLevelPolicyNameParts(RepositoryResultsDetailsFilter detailsFilter) {
    String part1 = "MAX(CONCAT(LPAD(CAST(violation.threat_level AS varchar), 2, '0'), violation.policy_name))";
    String part2 = "SUBSTRING(threat_level_and_policy_name, 1, 2)";
    String part3 = "SUBSTRING(threat_level_and_policy_name, 3)";
    if (!CollectionUtils.isEmpty(detailsFilter.sortFields)) {
      detailsFilter.sortFields.sort(Comparator.comparing(field -> field.sortPriority));
      for (SortField sortField : detailsFilter.sortFields) {
        if (sortField.sortableField == SortableField.POLICY_THREAT_LEVEL) {
          if (sortField.asc) {
            part1 = "MIN(CONCAT(LPAD(CAST(violation.threat_level AS varchar), 2, '0'), violation.policy_name))";
          }
          else {
            part1 = "MAX(CONCAT(LPAD(CAST(violation.threat_level AS varchar), 2, '0'), violation.policy_name))";
          }
          part2 = "SUBSTRING(threat_level_and_policy_name, 1, 2)";
          part3 = "SUBSTRING(threat_level_and_policy_name, 3)";
          break;
        }
        if (sortField.sortableField == SortableField.POLICY_NAME) {
          if (sortField.asc) {
            part1 = "MIN(CONCAT(violation.policy_name, LPAD(CAST(violation.threat_level AS varchar), 2, '0')))";
          }
          else {
            part1 = "MAX(CONCAT(violation.policy_name, LPAD(CAST(violation.threat_level AS varchar), 2, '0')))";
          }
          part2 = "SUBSTRING(threat_level_and_policy_name, LENGTH(threat_level_and_policy_name) - 1)";
          part3 = "SUBSTRING(threat_level_and_policy_name, 1, LENGTH(threat_level_and_policy_name) - 2)";
          break;
        }
      }
    }
    return new String[]{part1, part2, part3};
  }

  public Map<Integer, Integer> getCountsByPolicyThreatLevel(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT threat_level, COUNT(*) AS number_of_policy_violations" + //
          " FROM " + OperationalDataStoreProvider.getDatabaseSchema() + ".repository_policy_violation violation" + //
          " WHERE repository_id = ?1 AND active = true AND waived = false" + //
          " GROUP BY threat_level";

      javax.persistence.Query query = tx.createNativeQuery(sQuery);
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

  private static String addSearchFilters(Map<String, String> filters) {
    StringBuilder query = new StringBuilder();
    if (!MapUtils.isEmpty(filters)) {
      for (Entry<String, String> filter : filters.entrySet()) {
        if (filter.getKey().equals("POLICY_NAME")) {
          query.append(" AND LOWER(violation.policy_name) LIKE ?2");
        }
        if (filter.getKey().equals("COMPONENT_COORDINATES")) {
          query.append(" AND LOWER(component.display_name) LIKE ?3");
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

  private static <T> Integer getInteger(T value) {
    if (value instanceof Short) {
      return Integer.valueOf((Short) value);
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof Long) {
      return ((Long) value).intValue();
    }

    return null;
  }
}

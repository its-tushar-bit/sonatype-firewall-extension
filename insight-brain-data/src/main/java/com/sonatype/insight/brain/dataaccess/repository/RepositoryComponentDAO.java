/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * @since 1.17
 */
@Named
@Singleton
public class RepositoryComponentDAO
    extends AbstractOperationalSqlDAO<RepositoryComponent>
{
  /*
    For queries on `quarantineTime` or `unquarantineTime`, if we query using `IS NOT NULL` the applicable indices
    are not used by H2. By changing this to `> {d EPOCH_START}` the queries return the same results but the applicable
    indices are also used.
  */
  private static final String EPOCH_START = new SimpleDateFormat("yyyy-MM-dd").format(Date.from(Instant.EPOCH));

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  public RepositoryComponentDAO(
      final OperationalDataStore operationalDataStore,
      final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO)
  {
    super(operationalDataStore);
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
  }

  public List<RepositoryComponent> getByRepositoryId(String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1";
    return getList(sQuery, repositoryId);
  }

  public RepositoryComponent getByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public RepositoryComponent getByRepositoryIdAndPathname(TransactionContext tx, String repositoryId, String pathname) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2";
    return get(tx, sQuery, repositoryId, pathname);
  }

  public List<RepositoryComponent> getByRepositoryIdAndPathnames(
      String repositoryId,
      List<String> pathnames)
  {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname IN (?2)";
    List<List<String>> partitions = Lists.partition(pathnames, getInOperatorThreshold());
    return partitions.stream()
        .map(partition -> getList(sQuery, repositoryId, partition))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public List<RepositoryComponent> getByRepositoryIdAndHash(String repositoryId, String hash) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.hash=?2";
    return getList(sQuery, repositoryId, hash);
  }

  @SuppressWarnings("unchecked")
  public Map<Repository, List<RepositoryComponent>> getRepositoryToComponentsByHash(
      TransactionContext tx,
      String hash)
  {
    String sQuery = """
        SELECT repository, component
          FROM Repository repository, RepositoryComponent component
         WHERE repository.id = component.repositoryId
           AND component.hash = ?1
        """;

    List<Object[]> results = (List<Object[]>) createQuery(tx, sQuery, hash).getResultList();
    Map<Repository, List<RepositoryComponent>> resultMap = new HashMap<>();

    for (Object[] row : results) {
      Repository repository = (Repository) row[0];
      RepositoryComponent component = (RepositoryComponent) row[1];

      resultMap.computeIfAbsent(repository, key -> new ArrayList<>()).add(component);
    }

    return resultMap;
  }

  public int getComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1";

    return getSingle(Number.class, sQuery, repositoryId).intValue();
  }

  public int getKnownComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1 AND component.matchStateId <> ?2";

    return getSingle(Number.class, sQuery, repositoryId, MatchState.UNKNOWN.getId()).intValue();
  }

  public int getQuarantinedComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" //
        + " WHERE component.repositoryId=?1"
        + " AND component.quarantineTime IS NOT NULL AND component.unquarantineTime IS NULL";

    return getSingle(Number.class, sQuery, repositoryId).intValue();
  }

  /**
   * @since 1.106
   */
  public long getQuarantinedComponentCount() {
    String sQuery = String.format("SELECT COUNT(component.id) FROM RepositoryComponent component" //
        + " WHERE component.quarantineTime > {d '%s'} AND component.unquarantineTime IS NULL", EPOCH_START);

    return getSingle(Long.class, sQuery);
  }

  public List<RepositoryComponent> getAllQuarantinedComponent() {
    return getList("SELECT qc FROM RepositoryComponent qc" +
        " WHERE qc.quarantineTime IS NOT NULL AND qc.unquarantineTime IS NULL");
  }

  public List<RepositoryComponent> getQuarantinedByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.quarantineTime IS NOT NULL AND entity.unquarantineTime IS NULL";

    return getList(tx, sQuery, repositoryId);
  }

  public List<RepositoryComponent> getQuarantinedByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getQuarantinedByRepositoryId(tx, repositoryId);
    }
  }

  /**
   * @since 1.104
   */
  public List<RepositoryComponent> getQuarantinedByRepositoryIdAndDate(String repositoryId, Date date) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.quarantineTime>=?2 AND entity.unquarantineTime IS NULL";

    return getList(sQuery, repositoryId, date);
  }

  /**
   * @since 1.170
   */
  public Map<LocalDate, Long> getConsolidatedQuarantinedComponentsMetricByDate(Date date) {
    String sQuery = "SELECT CAST(entity.quarantine_time AS DATE) as metrics_date, " +
        "COUNT(entity.repository_component_id) as metrics_value " +
        "FROM " + getDatabaseSchema() +
        ".repository_component entity " +
        "WHERE entity.quarantine_time > ?1 " +
        "GROUP BY metrics_date";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, date);
      query.setParameter(1, date);
      Stream<Object[]> result = query.getResultStream();
      return result.collect(Collectors.toMap(array -> ((java.sql.Date) array[0]).toLocalDate(),
          array -> (Long) array[1]));
    }
  }

  @SuppressWarnings("unchecked")
  public Map<LocalDate, Long> getQuarantinedCountByRepositoryIdAndDate(String repositoryId, Date date) {
    String sQuery = "SELECT CAST(rc.quarantine_time AS DATE), COUNT(1)" + //
        " FROM " + getDatabaseSchema() + ".repository_component rc" + //
        " WHERE rc.repository_id = ?1" + //
        " AND rc.quarantine_time >= ?2" + //
        " GROUP BY CAST(rc.quarantine_time AS DATE)";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, repositoryId);
      query.setParameter(2, date);

      Stream<Object[]> result = query.getResultStream();
      return result
          .collect(Collectors.toMap(array -> ((java.sql.Date) array[0]).toLocalDate(), array -> (Long) array[1]));
    }
  }

  public Date getOldestComponentEvaluationTimeByRepositoryId(String repositoryId) {
    String sQuery = "SELECT MIN(entity.lastEvaluationTime) FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1";

    Date oldest = getSingle(Date.class, sQuery, repositoryId);

    // converting from a Timestamp to a Date object for happy comparisons
    return oldest != null ? new Date(oldest.getTime()) : null;
  }

  public List<RepositoryComponent> getUnquarantinedByRepositoryId(String repositoryId, Date sinceUtcTimestamp) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.unquarantineTime IS NOT NULL AND entity.unquarantineTime>=?2";
    return getList(sQuery, repositoryId, sinceUtcTimestamp);
  }

  public long getAutoReleaseQuarantinedCountByDate(Date date) {
    String sQuery = String.format("SELECT COUNT(component.id) FROM RepositoryComponent component" //
        + " WHERE component.quarantineTime > {d '%s'} AND component.unquarantineTime >=?1"
        + " AND component.autoUnquarantined = true", EPOCH_START);

    return getSingle(Number.class, sQuery, date).longValue();
  }

  @SuppressWarnings("unchecked")
  public Map<LocalDate, Long> getAutoReleaseQuarantinedCountByRepositoryIdAndDate(
      String repositoryId, Date date, boolean exclusiveDate)
  {
    String sQuery = "SELECT CAST(rc.unquarantine_time AS DATE), COUNT(1)" + //
        " FROM " + getDatabaseSchema() + ".repository_component rc" + //
        " WHERE rc.repository_id = ?1" + //
        " AND rc.unquarantine_time " + //
        (exclusiveDate ? ">" : ">=") + " ?2" + //
        " AND rc.auto_unquarantined = ?3" + //
        " GROUP BY CAST(rc.unquarantine_time AS DATE)";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, repositoryId);
      query.setParameter(2, date);
      query.setParameter(3, true);

      Stream<Object[]> result = query.getResultStream();
      return result
          .collect(Collectors.toMap(array -> ((java.sql.Date) array[0]).toLocalDate(), array -> (Long) array[1]));
    }
  }

  public List<FirewallQuarantinedComponentDetails> getQuarantinedComponentsDetails(
      FirewallRepositoryComponentFilter filter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      // extracting the highest threat level and policy name combination
      String highestThreatLevelPolicyNamePart =
          "MAX(CONCAT(LPAD(CAST(violation.threat_level AS varchar), 2, '0'), violation.policy_name))";
      // extracting threat level from concatenated string
      String threatLevelPart = "SUBSTRING(threat_level_and_policy_name, 1, 2)";
      // extracting policy name from concatenated string
      String policyNamePart = "SUBSTRING(threat_level_and_policy_name, 3)";

      String select1 = "SELECT" +
          " " + highestThreatLevelPolicyNamePart + " AS threat_level_and_policy_name," +
          " true AS quarantined," + // data is already filtered to quarantined components in the where clause
          " component.component_id_format," +
          " component.component_id_coordinates_json," +
          " component.pathname," +
          " component.display_name," +
          " component.repository_id," +
          " repository.public_id," +
          " component.hash," +
          " component.match_state_id," +
          " component.quarantine_time" +
          " FROM " + getDatabaseSchema() + ".repository_component component" +
          " INNER JOIN " + getDatabaseSchema() + ".repository" +
          " ON repository.repository_id = component.repository_id" +
          " INNER JOIN " + getDatabaseSchema() + ".repository_policy_violation violation" +
          " ON component.repository_id = violation.repository_id" +
          " AND component.pathname = violation.pathname" +
          " WHERE (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL)" +
          " AND violation.action_type_id = 'fail'" +
          " AND violation.active = true" +
          " AND violation.waived = false";

      List<String> policyIds = getPolicyIdsFromFilter(filter);

      select1 += addFilterParameters(filter, policyIds);

      select1 += " GROUP BY component.quarantine_time, component.component_id_format," +
          " component.component_id_coordinates_json, component.pathname," +
          " component.repository_id, repository.public_id, component.display_name, component.hash," +
          " component.match_state_id";

      int offset = (filter.page - 1) * filter.pageSize;

      String select2 = "SELECT " +
          " CASE WHEN(threat_level_and_policy_name <> '')" +
          " THEN CAST(" + threatLevelPart + " AS integer) " +
          " ELSE NULL END AS threat_level," +
          " CASE WHEN(threat_level_and_policy_name <> '')" +
          " THEN " + policyNamePart +
          " ELSE NULL END AS policy_name," +
          " quarantined," +
          " component_id_format," +
          " component_id_coordinates_json," +
          " pathname," +
          " display_name," +
          " repository_id," +
          " public_id," +
          " hash," +
          " match_state_id," +
          " quarantine_time" +
          " FROM (" + select1 + ") AS t1";

      select2 += addSortFields(filter);

      select2 += " LIMIT " + filter.pageSize + " OFFSET " + offset;

      jakarta.persistence.Query query = tx.createNativeQuery(select2);

      setFilterParameters(query, filter, policyIds);

      List<FirewallQuarantinedComponentDetails> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new FirewallQuarantinedComponentDetails(
              getInteger(array[0]),
              (String) array[1],
              (Boolean) array[2],
              (String) array[3],
              (String) array[4],
              (String) array[5],
              (String) array[6],
              (String) array[7],
              (String) array[8],
              (String) array[9],
              (String) array[10],
              toDate(array[11])
          ))
          .collect(Collectors.toList());

      return results;
    }
  }

  private List<String> getPolicyIdsFromFilter(FirewallRepositoryComponentFilter filter) {
    return Optional.ofNullable(filter.getFilterFieldsMap().get(FirewallFilterableField.POLICY_ID))
        .map(policyIdsString -> policyIdsString.split(FirewallFilterField.MULTI_VALUE_SEPARATOR))
        .map(Arrays::asList)
        // or else, return an empty list
        .orElse(List.of());
  }

  private Date toDate(Object o) {
    return Optional.ofNullable(o).map(t -> ((Timestamp) t)).map(Timestamp::getTime).map(Date::new)
        // or else, return null
        .orElse(null);
  }

  private String addFilterParameters(FirewallRepositoryComponentFilter filter, List<String> policyIds) {
    String filterQuery = "";
    int parameterIndex = 1;

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
      filterQuery += " AND LOWER(component.display_name) LIKE ?" + parameterIndex++;
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
      filterQuery += " AND LOWER(repository.public_id) LIKE ?" + parameterIndex++;
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.QUARANTINE_TIME)) {
      filterQuery += " AND component.quarantine_time >= CAST(?" + (parameterIndex++) + " AS TIMESTAMP)";
    }

    // Filter check intentionally left last and not incrementing index, as it adds positional parameters.
    // e.g.: (?p1, ?p2, ..., ?pn)
    if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
      filterQuery += " AND violation.policy_id IN " + buildPositionalParameters(policyIds, parameterIndex);
    }

    return filterQuery;
  }

  private void setFilterParameters(
      jakarta.persistence.Query query,
      FirewallRepositoryComponentFilter filter,
      List<String> policyIds)
  {
    int parameterIndex = 1;

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
      query.setParameter(parameterIndex++,
          '%' + filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME) + '%');
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
      query.setParameter(parameterIndex++,
          '%' + filter.getFilterFieldsMap().get(FirewallFilterableField.REPOSITORY_PUBLIC_ID) + '%');
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.QUARANTINE_TIME)) {
      query.setParameter(parameterIndex++,
          filter.getFilterFieldsMap().get(FirewallFilterableField.QUARANTINE_TIME));
    }

    // Filter check intentionally left last and not incrementing index, as it adds positional parameters.
    // e.g.: (?p1, ?p2, ..., ?pn)
    if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
      addPositionalParameters(query, policyIds, parameterIndex);
    }
  }

  private String addSortFields(final FirewallRepositoryComponentFilter filter) {
    boolean isDisplayNameFilter = false;
    String select = " ORDER BY ";

    if (null != filter.sortableField && !filter.sortableField.equals(FirewallSortableField.QUARANTINE_TIME)) {
      isDisplayNameFilter = filter.sortableField.equals(FirewallSortableField.COMPONENT_DISPLAY_NAME);
      select += getSortField(filter.sortableField);
      if (filter.asc) {
        select += " NULLS LAST,";
      }
      else {
        select += " DESC NULLS LAST,";
      }
    }

    select += "quarantine_time";

    if (null != filter.sortableField && filter.sortableField.equals(FirewallSortableField.QUARANTINE_TIME) &&
        filter.asc) {
      select += " NULLS LAST,";
    }
    else {
      select += " DESC NULLS LAST,";
    }

    if (isDisplayNameFilter) {
      select += " threat_level DESC NULLS LAST";
    }
    else {
      select += " threat_level DESC NULLS LAST, display_name DESC NULLS LAST";
    }
    return select;
  }

  private String getSortField(FirewallSortableField field) {
    switch (field) {
      case REPOSITORY_PUBLIC_ID:
        return "public_id";
      case POLICY_NAME:
        return "policy_name";
      case COMPONENT_DISPLAY_NAME:
        return "display_name";
      default:
        return "";
    }
  }

  public List<RepositoryComponent> getFirewallRepositoryComponents(FirewallRepositoryComponentFilter filter) {
    String baseQuery = getBaseFirewallComponentsQueryAndViolations(filter, "SELECT DISTINCT component");

    StringBuilder sQuery = new StringBuilder(baseQuery);

    // SORTING
    if (null != filter.sortableField) {
      sQuery.append(" ORDER BY component.").append(filter.sortableField.getColumn());
    }
    else {
      sQuery.append(" ORDER BY component.time");
    }

    if (filter.asc) {
      sQuery.append(" ASC");
    }
    else {
      sQuery.append(" DESC");
    }

    // PAGINATION
    int offset = (filter.page - 1) * filter.pageSize;
    int parameterIndex = 1;
    try (TransactionContext tx = createTransactionContext()) {
      final jakarta.persistence.Query paginationQuery =
          createPaginationQuery(tx, sQuery.toString(), offset, filter.pageSize);

      if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
        paginationQuery.setParameter(parameterIndex++,
            filter.getFilterFieldsMap().get(FirewallFilterableField.POLICY_ID)
                .split(FirewallFilterField.MULTI_VALUE_SEPARATOR));
      }

      if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
        paginationQuery.setParameter(parameterIndex,
            "%" + StringUtils.lowerCase(filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME)) + "%");
      }

      return paginationQuery.getResultList();
    }
  }

  public long getTotalFirewallRepositoryComponents(FirewallRepositoryComponentFilter filter) {
    String sQuery = getBaseFirewallComponentsQueryAndViolations(filter, "SELECT COUNT(DISTINCT component)");
    List<Object> parameters = new ArrayList<>();

    // FILTER
    if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
      parameters.add(filter.getFilterFieldsMap().get(FirewallFilterableField.POLICY_ID)
          .split(FirewallFilterField.MULTI_VALUE_SEPARATOR));
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
      parameters.add(
          "%" + StringUtils.lowerCase(filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME)) + "%");
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
      parameters.add(
          "%" + StringUtils.lowerCase(filter.getFilterFieldsMap().get(FirewallFilterableField.REPOSITORY_PUBLIC_ID)) +
              "%");
    }

    return getSingle(Long.class, sQuery, parameters.toArray());
  }

  public int getCountWithPolicyViolationInPolicyThreatLevelRange(
      String repositoryId,
      int minPolicyThreatLevel,
      int maxPolicyThreatLevel)
  {
    // Jan 19, 2023:
    // I tried this JPA query:
    // String sQuery = "SELECT COUNT(DISTINCT policyViolation.pathname)" + //
    // " FROM RepositoryPolicyViolation policyViolation" + //
    // " WHERE policyViolation.repositoryId=?1" + //
    // " AND policyViolation.active = true AND policyViolation.isWaived = false" + //
    // " AND policyViolation.threatLevel >= ?2 AND policyViolation.threatLevel <= ?3";
    // The native query below is about 2 times faster than the JPA query.
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT COUNT(*) AS component_count FROM " + //
          "(SELECT DISTINCT pathname" + //
          " FROM " + getDatabaseSchema() + ".repository_policy_violation" + //
          " WHERE repository_id = ?1 AND active = true AND waived = false" + //
          " AND threat_level >= ?2 AND threat_level <= ?3) inner_select_alias";

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, repositoryId);
      query.setParameter(2, minPolicyThreatLevel);
      query.setParameter(3, maxPolicyThreatLevel);

      return ((Long) query.getResultList().get(0)).intValue();
    }
  }

  private static String getBaseFirewallComponentsQueryAndViolations(
      FirewallRepositoryComponentFilter filter,
      String selectStatement)
  {
    validateFirewallRepositoryComponentFilter(filter);
    StringBuilder sQuery = new StringBuilder(selectStatement + " FROM RepositoryComponent component");
    MutableBoolean sQueryContainsWhereClause = new MutableBoolean();
    int parameterIndex = 1;

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
      sQuery.append(" , Repository repo");
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
      sQuery.append(" , RepositoryPolicyViolation policyViolation")
          .append(" WHERE component.repositoryId = policyViolation.repositoryId")
          .append(" AND component.pathname = policyViolation.pathname")
          .append(" AND policyViolation.actionTypeId = 'fail'")
          .append(" AND policyViolation.active = true")
          .append(" AND policyViolation.policyId IN (?")
          .append(parameterIndex++)
          .append(") AND policyViolation.isWaived = false");
      sQueryContainsWhereClause.setTrue();
    }

    sQuery.append(getFirewallComponentStateClause(sQueryContainsWhereClause, filter));

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
      if (sQueryContainsWhereClause.getValue()) {
        sQuery.append(" AND");
      }
      else {
        sQuery.append(" WHERE");
        sQueryContainsWhereClause.setTrue();
      }
      sQuery.append(" LOWER(component.displayName) LIKE ?").append(parameterIndex++);
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
      if (sQueryContainsWhereClause.getValue()) {
        sQuery.append(" AND");
      }
      else {
        sQuery.append(" WHERE");
        sQueryContainsWhereClause.setTrue();
      }
      sQuery.append(" component.repositoryId = repo.id AND LOWER(repo.publicId) LIKE ?").append(parameterIndex);
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.QUARANTINE_TIME)) {
      if (sQueryContainsWhereClause.getValue()) {
        sQuery.append(" AND");
      }
      else {
        sQuery.append(" WHERE");
        sQueryContainsWhereClause.setTrue();
      }
      sQuery.append(String.format(" component.quarantineTime >= {ts '%s'}",
          filter.getFilterFieldsMap().get(FirewallFilterableField.QUARANTINE_TIME)));
    }

    return sQuery.toString();
  }

  private static boolean filterFieldsMapContainsField(
      FirewallRepositoryComponentFilter filter,
      FirewallFilterableField firewallFilterableField)
  {
    return filter.getFilterFieldsMap().containsKey(firewallFilterableField);
  }

  private static String getFirewallComponentStateClause(
      final MutableBoolean sQueryContainsWhereClause,
      final FirewallRepositoryComponentFilter filter)
  {
    String prefix = sQueryContainsWhereClause.getValue() ? "AND" : "WHERE";

    switch (filter.firewallComponentFilterState) {
      case AUDIT:
        sQueryContainsWhereClause.setTrue();
        return String.format(" %s (component.quarantineTime IS NULL)", prefix);
      case QUARANTINE:
        sQueryContainsWhereClause.setTrue();
        return String.format(" %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime IS NULL)", prefix,
            EPOCH_START);
      case UNQUARANTINE_AUTO:
        sQueryContainsWhereClause.setTrue();
        return String.format(
            " %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime > {d '%s'}" +
                " AND component.autoUnquarantined = true)", prefix, EPOCH_START, EPOCH_START);
      case UNQUARANTINE_MANUAL:
        sQueryContainsWhereClause.setTrue();
        return String.format(
            " %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime > {d '%s'}" +
                " AND (component.autoUnquarantined = false OR component.autoUnquarantined IS NULL))",
            prefix, EPOCH_START, EPOCH_START);
      case UNQUARANTINE_ALL:
        sQueryContainsWhereClause.setTrue();
        return String
            .format(" %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime > {d '%s'})", prefix,
                EPOCH_START, EPOCH_START);
      case ALL:
      default:
        return "";
    }
  }

  private static void validateFirewallRepositoryComponentFilter(final FirewallRepositoryComponentFilter filter) {
    if (filter.firewallComponentFilterState == null) {
      throw new BadRequestException("firewallComponentFilterState is required and cannot be null.");
    }

    if (filter.firewallComponentFilterState.equals(FirewallComponentFilterState.QUARANTINE) &&
        FirewallSortableField.RELEASE_QUARANTINE_TIME.equals(filter.sortableField)) {
      throw new BadRequestException(
          "Sortable field releaseQuarantineTime is not applicable to component state QUARANTINE");
    }

    if ((filter.firewallComponentFilterState.equals(FirewallComponentFilterState.AUDIT) ||
        filter.firewallComponentFilterState.equals(FirewallComponentFilterState.ALL)) &&
        filter.sortableField != null) {
      throw new BadRequestException(String
          .format("Sortable field cannot be specified for component state %s", filter.firewallComponentFilterState));
    }
  }

  public List<RepositoryComponent> getByRepositoryIdAndMatchStateId(String repositoryId, String matchStateId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.matchStateId=?2";
    return getList(sQuery, repositoryId, matchStateId);
  }

  @Override
  public final void delete(RepositoryComponent entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all components for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, RepositoryComponent entity) {
    // WARNING: Be careful adding business logic to this method because, for performance reasons,
    // we bypass this method when deleting all components for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    quarantinedComponentAccessDAO.deleteByRepositoryComponentId(tx, entity.getId());
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
      quarantinedComponentAccessDAO.deleteByRepositoryId(tx, repositoryId);
      String sQuery = "DELETE FROM RepositoryComponent entity WHERE entity.repositoryId=?1";
      createQuery(sQuery, repositoryId).executeUpdate(tx);
    }
  }

  public List<RepositoryComponent> getOtherVersionRepositoryComponentsByPathnameFilter(
      String repositoryId,
      String pathnamePrefix,
      String pathname)
  {
    // Returns components that are safe to suggest as "other allowed versions" for a quarantined component.
    // Includes components that are EITHER:
    // 1. Never quarantined AND have no active 'fail' policy violations (excludes pre-cached components with violations)
    // 2. Previously quarantined but later released (quarantineTime and unquarantineTime both set)
    // Excludes the currently quarantined component itself (pathname <> ?3)
    String sQuery = "SELECT component FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1" + //
        " AND component.pathname like ?2" + //
        " AND component.pathname <> ?3" + //
        " AND (component.quarantineTime IS NULL" + //
        " AND NOT EXISTS (SELECT 1 FROM RepositoryPolicyViolation v" +
        " WHERE v.repositoryId = component.repositoryId" +
        " AND v.pathname = component.pathname" +
        " AND v.actionTypeId = '" + Action.ID_FAIL + "'" +
        " AND v.isWaived = false" +
        " AND v.active = true) " +
        " OR (component.quarantineTime IS NOT NULL AND component.unquarantineTime IS NOT NULL))";

    return getList(sQuery, repositoryId, pathnamePrefix + "%", pathname);
  }

  public List<RepositoryComponent> getByRepositoryIdAndComponentIdentifier(
      String repositoryId,
      ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.componentIdFormat=?2 AND entity.componentIdCoordinatesJson=?3";
    return getList(sQuery, repositoryId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public List<RepositoryComponent> getByRepositoryIdAndDisplayName(String repositoryId, String displayName) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.displayName=?2";
    return getList(sQuery, repositoryId, displayName);
  }

  @Override
  public void insert(TransactionContext tx, RepositoryComponent entity) {
    fillDisplayName(entity);
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, RepositoryComponent entity) {
    fillDisplayName(entity);
    super.update(tx, entity);
  }

  private void fillDisplayName(RepositoryComponent entity) {
    if (entity.getComponentIdentifier() != null) {
      entity.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(entity.getComponentIdentifier()).toString());
      return;
    }

    String pathname = entity.getPathname();
    if (pathname == null) {
      return;
    }

    entity.setDisplayName(pathname.substring(pathname.lastIndexOf('/') + 1) + " (" + pathname + ")");
  }
}

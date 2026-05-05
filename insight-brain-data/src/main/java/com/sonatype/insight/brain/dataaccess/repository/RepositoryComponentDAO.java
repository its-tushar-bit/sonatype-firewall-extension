/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record2;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedComponentScanQueue.HOSTED_COMPONENT_SCAN_QUEUE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryComponent.REPOSITORY_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryPolicyViolation.REPOSITORY_POLICY_VIOLATION;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.selectOne;

/**
 * @since 1.17
 */
@Named
@Singleton
public class RepositoryComponentDAO
    extends AbstractOperationalSqlDAO<RepositoryComponent>
{
  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  public RepositoryComponentDAO(
      final OperationalDataStore operationalDataStore,
      final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO)
  {
    super(operationalDataStore);
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_COMPONENT;
  }

  public List<RepositoryComponent> getByRepositoryIdPaged(
      String repositoryId,
      String filter,
      int limit,
      int offset)
  {
    if (repositoryId == null || repositoryId.isEmpty()) {
      return List.of();
    }
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId));
      if (filter != null && !filter.isEmpty()) {
        String escaped = escapeLike(filter);
        query = query.and(
            REPOSITORY_COMPONENT.DISPLAY_NAME.containsIgnoreCase(escaped)
                .or(REPOSITORY_COMPONENT.PATHNAME.containsIgnoreCase(escaped)));
      }
      return query.orderBy(REPOSITORY_COMPONENT.DISPLAY_NAME)
          .limit(limit)
          .offset(offset)
          .fetch(this::toEntity);
    }
  }

  public int countByRepositoryIdWithFilter(String repositoryId, String filter) {
    if (repositoryId == null || repositoryId.isEmpty()) {
      return 0;
    }
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectCount()
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId));
      if (filter != null && !filter.isEmpty()) {
        String escaped = escapeLike(filter);
        query = query.and(
            REPOSITORY_COMPONENT.DISPLAY_NAME.containsIgnoreCase(escaped)
                .or(REPOSITORY_COMPONENT.PATHNAME.containsIgnoreCase(escaped)));
      }
      return query.fetchOne(0, Integer.class);
    }
  }

  private static String escapeLike(String value) {
    return value.replace("%", "\\%").replace("_", "\\_");
  }

  public List<RepositoryComponent> getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .fetch(this::toEntity);
    }
  }

  public List<RepositoryComponent> getByRepositoryId(
      final TransactionContext tx,
      final String repositoryId,
      final int limit,
      final int offset)
  {
    return tx.dsl()
        .selectFrom(REPOSITORY_COMPONENT)
        .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .orderBy(REPOSITORY_COMPONENT.REPOSITORY_COMPONENT_ID.asc())
        .limit(limit)
        .offset(offset)
        .fetch(this::toEntity);
  }

  public RepositoryComponent getByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public RepositoryComponent getByRepositoryIdAndPathname(TransactionContext tx, String repositoryId, String pathname) {
    return toEntity(tx.dsl()
        .selectFrom(REPOSITORY_COMPONENT)
        .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_COMPONENT.PATHNAME.eq(pathname))
        .fetchOne());
  }

  public RepositoryComponent getByRepositoryIdAndComponentId(String repositoryId, String componentId) {
    if (componentId == null) {
      return null;
    }
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.REPOSITORY_COMPONENT_ID.eq(componentId))
          .fetchOne());
    }
  }

  public List<RepositoryComponent> getByRepositoryIdAndPathnames(
      String repositoryId,
      List<String> pathnames)
  {
    List<List<String>> partitions = Lists.partition(pathnames, getInOperatorThreshold());
    return partitions.stream()
        .map(partition -> {
          try (TransactionContext tx = createTransactionContext()) {
            return tx.dsl()
                .selectFrom(REPOSITORY_COMPONENT)
                .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
                .and(REPOSITORY_COMPONENT.PATHNAME.in(partition))
                .fetch(this::toEntity);
          }
        })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public List<RepositoryComponent> getByRepositoryIdAndHash(String repositoryId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.HASH.eq(hash))
          .fetch(this::toEntity);
    }
  }

  public Map<Repository, List<RepositoryComponent>> getRepositoryToComponentsByHash(
      TransactionContext tx,
      String hash)
  {
    // Use repository ID as key first, then build final map with Repository objects
    // This is necessary because Repository doesn't have equals/hashCode based on ID,
    // and jOOQ creates new instances for each row
    Map<String, Repository> repositoryById = new HashMap<>();
    Map<String, List<RepositoryComponent>> componentsByRepositoryId = new HashMap<>();

    tx.dsl()
        .select(REPOSITORY.fields())
        .select(REPOSITORY_COMPONENT.fields())
        .from(REPOSITORY)
        .join(REPOSITORY_COMPONENT)
        .on(REPOSITORY.REPOSITORY_ID.eq(REPOSITORY_COMPONENT.REPOSITORY_ID))
        .where(REPOSITORY_COMPONENT.HASH.eq(hash))
        .fetch()
        .forEach(record -> {
          Repository repository = record.into(REPOSITORY.fields()).into(Repository.class);
          RepositoryComponent component = record.into(REPOSITORY_COMPONENT.fields()).into(RepositoryComponent.class);
          String repositoryId = repository.getId();
          repositoryById.putIfAbsent(repositoryId, repository);
          componentsByRepositoryId.computeIfAbsent(repositoryId, key -> new ArrayList<>()).add(component);
        });

    // Build final map with Repository objects as keys
    Map<Repository, List<RepositoryComponent>> resultMap = new HashMap<>();
    for (Map.Entry<String, List<RepositoryComponent>> entry : componentsByRepositoryId.entrySet()) {
      resultMap.put(repositoryById.get(entry.getKey()), entry.getValue());
    }
    return resultMap;
  }

  public int getComponentCountByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .fetchOne(0, Integer.class);
    }
  }

  public int getKnownComponentCountByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.MATCH_STATE_ID.ne(MatchState.UNKNOWN.getId()))
          .fetchOne(0, Integer.class);
    }
  }

  public int getQuarantinedComponentCountByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.QUARANTINE_TIME.isNotNull())
          .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNull())
          .fetchOne(0, Integer.class);
    }
  }

  /**
   * @since 1.106
   */
  public long getQuarantinedComponentCount() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.QUARANTINE_TIME.gt(Date.from(Instant.EPOCH)))
          .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNull())
          .fetchOne(0, Long.class);
    }
  }

  public List<RepositoryComponent> getAllQuarantinedComponent() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.QUARANTINE_TIME.isNotNull())
          .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNull())
          .fetch(this::toEntity);
    }
  }

  public List<RepositoryComponent> getQuarantinedByRepositoryId(TransactionContext tx, String repositoryId) {
    return tx.dsl()
        .selectFrom(REPOSITORY_COMPONENT)
        .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_COMPONENT.QUARANTINE_TIME.isNotNull())
        .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNull())
        .fetch(this::toEntity);
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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.QUARANTINE_TIME.ge(date))
          .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNull())
          .fetch(this::toEntity);
    }
  }

  /**
   * @since 1.170
   */
  public Map<LocalDate, Long> getConsolidatedQuarantinedComponentsMetricByDate(Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.cast(REPOSITORY_COMPONENT.QUARANTINE_TIME, java.sql.Date.class).as("metrics_date"),
              DSL.count(REPOSITORY_COMPONENT.REPOSITORY_COMPONENT_ID).as("metrics_value"))
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.QUARANTINE_TIME.gt(date))
          .groupBy(DSL.cast(REPOSITORY_COMPONENT.QUARANTINE_TIME, java.sql.Date.class))
          .fetchMap(
              record -> record.get("metrics_date", java.sql.Date.class).toLocalDate(),
              record -> record.get("metrics_value", Long.class));
    }
  }

  public Map<LocalDate, Long> getQuarantinedCountByRepositoryIdAndDate(String repositoryId, Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.cast(REPOSITORY_COMPONENT.QUARANTINE_TIME, java.sql.Date.class).as("quarantine_date"),
              DSL.count().as("count"))
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.QUARANTINE_TIME.ge(date))
          .groupBy(DSL.cast(REPOSITORY_COMPONENT.QUARANTINE_TIME, java.sql.Date.class))
          .fetchMap(
              record -> record.get("quarantine_date", java.sql.Date.class).toLocalDate(),
              record -> record.get("count", Long.class));
    }
  }

  public Date getOldestComponentEvaluationTimeByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.min(REPOSITORY_COMPONENT.LAST_EVALUATION_TIME))
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .fetchOne(0, Date.class);
    }
  }

  public List<RepositoryComponent> getUnquarantinedByRepositoryId(String repositoryId, Date sinceUtcTimestamp) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNotNull())
          .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.ge(sinceUtcTimestamp))
          .fetch(this::toEntity);
    }
  }

  public long getAutoReleaseQuarantinedCountByDate(Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      Date epochStart = Date.from(Instant.EPOCH);
      return tx.dsl()
          .selectCount()
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.QUARANTINE_TIME.gt(epochStart))
          .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.ge(date))
          .and(REPOSITORY_COMPONENT.AUTO_UNQUARANTINED.eq(true))
          .fetchOne(0, Long.class);
    }
  }

  public Map<LocalDate, Long> getAutoReleaseQuarantinedCountByRepositoryIdAndDate(
      String repositoryId,
      Date date,
      boolean exclusiveDate)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var dateCondition = exclusiveDate
          ? REPOSITORY_COMPONENT.UNQUARANTINE_TIME.gt(date)
          : REPOSITORY_COMPONENT.UNQUARANTINE_TIME.ge(date);
      return tx.dsl()
          .select(DSL.cast(REPOSITORY_COMPONENT.UNQUARANTINE_TIME, java.sql.Date.class).as("unquarantine_date"),
              DSL.count().as("count"))
          .from(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(dateCondition)
          .and(REPOSITORY_COMPONENT.AUTO_UNQUARANTINED.eq(true))
          .groupBy(DSL.cast(REPOSITORY_COMPONENT.UNQUARANTINE_TIME, java.sql.Date.class))
          .fetchMap(
              record -> record.get("unquarantine_date", java.sql.Date.class).toLocalDate(),
              record -> record.get("count", Long.class));
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
      List<Object> params = new ArrayList<>();

      select1 += addJooqFilterParameters(filter, policyIds, params);

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

      List<FirewallQuarantinedComponentDetails> results = tx.dsl()
          .resultQuery(select2, params.toArray())
          .fetchStream()
          .map(record -> new FirewallQuarantinedComponentDetails(
              record.get(0, Integer.class),
              record.get(1, String.class),
              record.get(2, Boolean.class),
              record.get(3, String.class),
              record.get(4, String.class),
              record.get(5, String.class),
              record.get(6, String.class),
              record.get(7, String.class),
              record.get(8, String.class),
              record.get(9, String.class),
              record.get(10, String.class),
              toDate(record.get(11))))
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
    return Optional.ofNullable(o)
        .map(t -> ((Timestamp) t))
        .map(Timestamp::getTime)
        .map(Date::new)
        // or else, return null
        .orElse(null);
  }

  /**
   * Builds filter parameters for jOOQ queries using simple ? placeholders and collects parameter values. This method is
   * used instead of addFilterParameters for jOOQ resultQuery calls.
   */
  private String addJooqFilterParameters(
      FirewallRepositoryComponentFilter filter,
      List<String> policyIds,
      List<Object> params)
  {
    StringBuilder filterQuery = new StringBuilder();

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
      filterQuery.append(" AND LOWER(component.display_name) LIKE ?");
      params.add('%' + filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME) + '%');
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
      filterQuery.append(" AND LOWER(repository.public_id) LIKE ?");
      params.add('%' + filter.getFilterFieldsMap().get(FirewallFilterableField.REPOSITORY_PUBLIC_ID) + '%');
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.QUARANTINE_TIME)) {
      filterQuery.append(" AND component.quarantine_time >= CAST(? AS TIMESTAMP)");
      params.add(filter.getFilterFieldsMap().get(FirewallFilterableField.QUARANTINE_TIME));
    }

    if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
      filterQuery.append(" AND violation.policy_id IN (");
      for (int i = 0; i < policyIds.size(); i++) {
        if (i > 0) {
          filterQuery.append(", ");
        }
        filterQuery.append("?");
        params.add(policyIds.get(i));
      }
      filterQuery.append(")");
    }

    return filterQuery.toString();
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
        filter.asc)
    {
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
    validateFirewallRepositoryComponentFilter(filter);

    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectDistinct(REPOSITORY_COMPONENT.fields())
          .from(REPOSITORY_COMPONENT);

      // Join with policy violation table if filtering by policy or sorting by policy name
      var conditions = new ArrayList<org.jooq.Condition>();
      boolean needsPolicyViolationJoin = filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)
          || (filter.sortableField == FirewallSortableField.POLICY_NAME);

      if (needsPolicyViolationJoin) {
        query = query.join(REPOSITORY_POLICY_VIOLATION)
            .on(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID))
            .and(REPOSITORY_COMPONENT.PATHNAME.eq(REPOSITORY_POLICY_VIOLATION.PATHNAME));

        conditions.add(REPOSITORY_POLICY_VIOLATION.ACTION_TYPE_ID.eq("fail"));
        conditions.add(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true));
        conditions.add(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(false));

        if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
          String[] policyIds = filter.getFilterFieldsMap()
              .get(FirewallFilterableField.POLICY_ID)
              .split(FirewallFilterField.MULTI_VALUE_SEPARATOR);
          conditions.add(REPOSITORY_POLICY_VIOLATION.POLICY_ID.in(policyIds));
        }
      }

      // Join with repository table if filtering by repository public id or sorting by it
      boolean needsRepositoryJoin = filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)
          || (filter.sortableField == FirewallSortableField.REPOSITORY_PUBLIC_ID);

      if (needsRepositoryJoin) {
        query = query.join(REPOSITORY)
            .on(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(REPOSITORY.REPOSITORY_ID));

        if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
          String repoPublicIdPattern = "%" + StringUtils.lowerCase(
              filter.getFilterFieldsMap().get(FirewallFilterableField.REPOSITORY_PUBLIC_ID)) + "%";
          conditions.add(DSL.lower(REPOSITORY.PUBLIC_ID).like(repoPublicIdPattern));
        }
      }

      // Add filter state conditions
      conditions.addAll(getFirewallComponentStateConditions(filter));

      // Add component name filter
      if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
        String componentNamePattern = "%" + StringUtils.lowerCase(
            filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME)) + "%";
        conditions.add(DSL.lower(REPOSITORY_COMPONENT.DISPLAY_NAME).like(componentNamePattern));
      }

      // Add quarantine time filter
      if (filterFieldsMapContainsField(filter, FirewallFilterableField.QUARANTINE_TIME)) {
        String quarantineTimeStr = filter.getFilterFieldsMap().get(FirewallFilterableField.QUARANTINE_TIME);
        Date quarantineTime = Timestamp.valueOf(quarantineTimeStr.replace("T", " "));
        conditions.add(REPOSITORY_COMPONENT.QUARANTINE_TIME.ge(quarantineTime));
      }

      var selectConditionStep = query.where(conditions);

      // Sorting
      org.jooq.Field<?> sortField;
      if (filter.sortableField != null) {
        sortField = getJooqSortField(filter.sortableField);
      }
      else {
        sortField = REPOSITORY_COMPONENT.TIME;
      }

      var orderedQuery = filter.asc
          ? selectConditionStep.orderBy(sortField.asc())
          : selectConditionStep.orderBy(sortField.desc());

      // Pagination
      int offset = (filter.page - 1) * filter.pageSize;

      return orderedQuery.limit(filter.pageSize)
          .offset(offset)
          .fetch(this::toEntity);
    }
  }

  private org.jooq.Field<?> getJooqSortField(FirewallSortableField sortableField) {
    switch (sortableField) {
      case QUARANTINE_TIME:
        return REPOSITORY_COMPONENT.QUARANTINE_TIME;
      case RELEASE_QUARANTINE_TIME:
        return REPOSITORY_COMPONENT.UNQUARANTINE_TIME;
      case COMPONENT_DISPLAY_NAME:
        return REPOSITORY_COMPONENT.DISPLAY_NAME;
      case REPOSITORY_PUBLIC_ID:
        return REPOSITORY.PUBLIC_ID;
      case POLICY_NAME:
        return REPOSITORY_POLICY_VIOLATION.POLICY_NAME;
      default:
        return REPOSITORY_COMPONENT.TIME;
    }
  }

  private List<org.jooq.Condition> getFirewallComponentStateConditions(FirewallRepositoryComponentFilter filter) {
    List<org.jooq.Condition> conditions = new ArrayList<>();
    Date epochStart = Date.from(Instant.EPOCH);

    switch (filter.firewallComponentFilterState) {
      case AUDIT:
        conditions.add(REPOSITORY_COMPONENT.QUARANTINE_TIME.isNull());
        break;
      case QUARANTINE:
        conditions.add(REPOSITORY_COMPONENT.QUARANTINE_TIME.gt(epochStart));
        conditions.add(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNull());
        break;
      case UNQUARANTINE_AUTO:
        conditions.add(REPOSITORY_COMPONENT.QUARANTINE_TIME.gt(epochStart));
        conditions.add(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.gt(epochStart));
        conditions.add(REPOSITORY_COMPONENT.AUTO_UNQUARANTINED.eq(true));
        break;
      case UNQUARANTINE_MANUAL:
        conditions.add(REPOSITORY_COMPONENT.QUARANTINE_TIME.gt(epochStart));
        conditions.add(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.gt(epochStart));
        conditions.add(REPOSITORY_COMPONENT.AUTO_UNQUARANTINED.eq(false)
            .or(REPOSITORY_COMPONENT.AUTO_UNQUARANTINED.isNull()));
        break;
      case UNQUARANTINE_ALL:
        conditions.add(REPOSITORY_COMPONENT.QUARANTINE_TIME.gt(epochStart));
        conditions.add(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.gt(epochStart));
        break;
      case ALL:
      default:
        // No additional conditions
        break;
    }
    return conditions;
  }

  public long getTotalFirewallRepositoryComponents(FirewallRepositoryComponentFilter filter) {
    validateFirewallRepositoryComponentFilter(filter);

    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .select(DSL.countDistinct(REPOSITORY_COMPONENT.REPOSITORY_COMPONENT_ID))
          .from(REPOSITORY_COMPONENT);

      // Join with policy violation table if filtering by policy
      var conditions = new ArrayList<org.jooq.Condition>();

      if (filterFieldsMapContainsField(filter, FirewallFilterableField.POLICY_ID)) {
        query = query.join(REPOSITORY_POLICY_VIOLATION)
            .on(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID))
            .and(REPOSITORY_COMPONENT.PATHNAME.eq(REPOSITORY_POLICY_VIOLATION.PATHNAME));

        conditions.add(REPOSITORY_POLICY_VIOLATION.ACTION_TYPE_ID.eq("fail"));
        conditions.add(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true));
        conditions.add(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(false));

        String[] policyIds = filter.getFilterFieldsMap()
            .get(FirewallFilterableField.POLICY_ID)
            .split(FirewallFilterField.MULTI_VALUE_SEPARATOR);
        conditions.add(REPOSITORY_POLICY_VIOLATION.POLICY_ID.in(policyIds));
      }

      // Join with repository table if filtering by repository public id
      if (filterFieldsMapContainsField(filter, FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
        query = query.join(REPOSITORY)
            .on(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(REPOSITORY.REPOSITORY_ID));

        String repoPublicIdPattern = "%" + StringUtils.lowerCase(
            filter.getFilterFieldsMap().get(FirewallFilterableField.REPOSITORY_PUBLIC_ID)) + "%";
        conditions.add(DSL.lower(REPOSITORY.PUBLIC_ID).like(repoPublicIdPattern));
      }

      // Add filter state conditions
      conditions.addAll(getFirewallComponentStateConditions(filter));

      // Add component name filter
      if (filterFieldsMapContainsField(filter, FirewallFilterableField.COMPONENT_NAME)) {
        String componentNamePattern = "%" + StringUtils.lowerCase(
            filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME)) + "%";
        conditions.add(DSL.lower(REPOSITORY_COMPONENT.DISPLAY_NAME).like(componentNamePattern));
      }

      // Add quarantine time filter
      if (filterFieldsMapContainsField(filter, FirewallFilterableField.QUARANTINE_TIME)) {
        String quarantineTimeStr = filter.getFilterFieldsMap().get(FirewallFilterableField.QUARANTINE_TIME);
        Date quarantineTime = Timestamp.valueOf(quarantineTimeStr.replace("T", " "));
        conditions.add(REPOSITORY_COMPONENT.QUARANTINE_TIME.ge(quarantineTime));
      }

      return query.where(conditions).fetchOne(0, Long.class);
    }
  }

  public int getCountWithPolicyViolationInPolicyThreatLevelRange(
      String repositoryId,
      int minPolicyThreatLevel,
      int maxPolicyThreatLevel)
  {
    // Jan 19, 2023: jOOQ query is used for type safety while maintaining performance.
    // Original JPA query was about 2x slower than native SQL.
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.countDistinct(REPOSITORY_POLICY_VIOLATION.PATHNAME))
          .from(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
          .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(false))
          .and(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL.ge((short) minPolicyThreatLevel))
          .and(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL.le((short) maxPolicyThreatLevel))
          .fetchOne(0, Integer.class);
    }
  }

  private static boolean filterFieldsMapContainsField(
      FirewallRepositoryComponentFilter filter,
      FirewallFilterableField firewallFilterableField)
  {
    return filter.getFilterFieldsMap().containsKey(firewallFilterableField);
  }

  private static void validateFirewallRepositoryComponentFilter(final FirewallRepositoryComponentFilter filter) {
    if (filter.firewallComponentFilterState == null) {
      throw new BadRequestException("firewallComponentFilterState is required and cannot be null.");
    }

    if (filter.firewallComponentFilterState.equals(FirewallComponentFilterState.QUARANTINE) &&
        FirewallSortableField.RELEASE_QUARANTINE_TIME.equals(filter.sortableField))
    {
      throw new BadRequestException(
          "Sortable field releaseQuarantineTime is not applicable to component state QUARANTINE");
    }

    if ((filter.firewallComponentFilterState.equals(FirewallComponentFilterState.AUDIT) ||
        filter.firewallComponentFilterState.equals(FirewallComponentFilterState.ALL)) &&
        filter.sortableField != null)
    {
      throw new BadRequestException(String
          .format("Sortable field cannot be specified for component state %s", filter.firewallComponentFilterState));
    }
  }

  public List<RepositoryComponent> getByRepositoryIdAndMatchStateId(String repositoryId, String matchStateId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.MATCH_STATE_ID.eq(matchStateId))
          .fetch(this::toEntity);
    }
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
      tx.dsl()
          .deleteFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .execute();
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
    // Excludes the currently quarantined component itself (pathname <> pathname parameter)
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.PATHNAME.like(pathnamePrefix + "%"))
          .and(REPOSITORY_COMPONENT.PATHNAME.ne(pathname))
          .and(
              REPOSITORY_COMPONENT.QUARANTINE_TIME.isNull()
                  .and(notExists(
                      selectOne()
                          .from(REPOSITORY_POLICY_VIOLATION)
                          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(REPOSITORY_COMPONENT.REPOSITORY_ID))
                          .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(REPOSITORY_COMPONENT.PATHNAME))
                          .and(REPOSITORY_POLICY_VIOLATION.ACTION_TYPE_ID.eq(Action.ID_FAIL))
                          .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(false))
                          .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))))
                  .or(REPOSITORY_COMPONENT.QUARANTINE_TIME.isNotNull()
                      .and(REPOSITORY_COMPONENT.UNQUARANTINE_TIME.isNotNull())))
          .fetchInto(RepositoryComponent.class);
    }
  }

  public List<RepositoryComponent> getByRepositoryIdAndComponentIdentifier(
      String repositoryId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_COMPONENT.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
          .and(REPOSITORY_COMPONENT.COMPONENT_ID_COORDINATES_JSON.eq(
              ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
          .fetch(this::toEntity);
    }
  }

  public List<RepositoryComponent> getByRepositoryIdAndDisplayName(String repositoryId, String displayName) {
    try (TransactionContext tx = createTransactionContext()) {
      var condition = REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId);
      if (displayName == null) {
        condition = condition.and(REPOSITORY_COMPONENT.DISPLAY_NAME.isNull());
      }
      else {
        condition = condition.and(REPOSITORY_COMPONENT.DISPLAY_NAME.eq(displayName));
      }
      return tx.dsl()
          .selectFrom(REPOSITORY_COMPONENT)
          .where(condition)
          .fetch(this::toEntity);
    }
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

  public Map<String, Date> getLastScanTimesByRepositoryIds(Collection<String> repositoryIds) {
    if (repositoryIds == null || repositoryIds.isEmpty()) {
      return Map.of();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return getLastScanTimesByRepositoryIds(tx, repositoryIds);
    }
  }

  public Map<String, Date> getLastScanTimesByRepositoryIds(TransactionContext tx, Collection<String> repositoryIds) {
    if (repositoryIds == null || repositoryIds.isEmpty()) {
      return Map.of();
    }
    return tx.dsl()
        .select(REPOSITORY_COMPONENT.REPOSITORY_ID, DSL.max(REPOSITORY_COMPONENT.LAST_EVALUATION_TIME))
        .from(REPOSITORY_COMPONENT)
        .where(REPOSITORY_COMPONENT.REPOSITORY_ID.in(repositoryIds))
        .groupBy(REPOSITORY_COMPONENT.REPOSITORY_ID)
        .fetch()
        .stream()
        .filter(record -> record.value2() != null)
        .collect(Collectors.toMap(
            Record2::value1,
            record -> new Date(record.value2().getTime())));
  }

  public Set<String> getRepositoryIdsWithQueuedScans(final Collection<String> repositoryIds) {
    if (repositoryIds == null || repositoryIds.isEmpty()) {
      return Collections.emptySet();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return getRepositoryIdsWithQueuedScans(tx, repositoryIds);
    }
  }

  public void stampComponentId(
      final TransactionContext tx,
      final String repositoryId,
      final String pathname,
      final String componentId)
  {
    tx.dsl()
        .update(REPOSITORY_COMPONENT)
        .set(REPOSITORY_COMPONENT.COMPONENT_ID, componentId)
        .where(REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_COMPONENT.PATHNAME.eq(pathname))
        .execute();
  }

  public Set<String> getRepositoryIdsWithQueuedScans(
      final TransactionContext tx,
      final Collection<String> repositoryIds)
  {
    if (repositoryIds == null || repositoryIds.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(tx.dsl()
        .selectDistinct(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID)
        .from(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID.in(repositoryIds)
            .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.in(
                HostedComponentScanQueueDAO.Status.PENDING.name(),
                HostedComponentScanQueueDAO.Status.IN_PROGRESS.name())))
        .fetch(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID));
  }

  @Override
  public Class<RepositoryComponent> getEntityClass() {
    return RepositoryComponent.class;
  }
}

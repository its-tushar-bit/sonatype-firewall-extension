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
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.google.common.collect.Lists;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsCountSummary;
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
import org.jooq.Condition;
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
  @Inject
  public RepositoryPolicyViolationDAO(
      OperationalDataStore operationalDataStore,
      PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO)
  {
    super(operationalDataStore, policyViolationConstraintFactsDAO);
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_POLICY_VIOLATION;
  }

  @Override
  public int insert(TransactionContext tx, RepositoryPolicyViolation entity) {
    storeConstraints(entity);
    return super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, RepositoryPolicyViolation entity) {
    storeConstraints(entity);
    super.update(tx, entity);
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

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnames(
      String repositoryId,
      List<String> pathnames)
  {
    if (repositoryId == null || pathnames == null || pathnames.isEmpty()) {
      return List.of();
    }
    List<List<String>> partitions = com.google.common.collect.Lists.partition(pathnames, getInOperatorThreshold());
    return partitions.stream()
        .flatMap(partition -> {
          try (TransactionContext tx = createTransactionContext()) {
            return tx.dsl()
                .selectFrom(REPOSITORY_POLICY_VIOLATION)
                .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
                .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.in(partition))
                .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
                .orderBy(REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL.desc(), REPOSITORY_POLICY_VIOLATION.POLICY_ID)
                .fetch(this::toEntity)
                .stream();
          }
        })
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Returns active violations whose pathname is either {@code outerPathname} itself, OR an inner
   * pathname of the form {@code outerPathname + "!/" + ...}. Used by the hosted-repo consumer to
   * synthesise a {@code policythreats.json} for an archive-of-archives upload — the outer
   * artifact has its own violations, plus the evaluator stamps each inner artifact's pathname as
   * {@code outer.zip!/inner.jar}; this method gathers both batches in one query.
   */
  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnameOrInnerPathnames(
      String repositoryId,
      String outerPathname)
  {
    if (repositoryId == null || outerPathname == null) {
      return List.of();
    }
    String innerPrefix = outerPathname + "!/";
    String escapedPrefix = innerPrefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(outerPathname)
              .or(REPOSITORY_POLICY_VIOLATION.PATHNAME.like(escapedPrefix + "%", '\\')))
          .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
          .orderBy(REPOSITORY_POLICY_VIOLATION.PATHNAME, REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL.desc(),
              REPOSITORY_POLICY_VIOLATION.POLICY_ID)
          .fetch(this::toEntity);
    }
  }

  /**
   * Batch variant of {@link #getActiveByRepositoryIdAndPathnameOrInnerPathnames}: returns active
   * violations whose pathname matches any of the supplied outer pathnames OR is an inner pathname
   * ({@code outer + "!/..."} ) under any of them. Used by the per-repository components-list page
   * so a single query covers a whole page of outer artifacts plus all of their inner-jar
   * violations, replacing the legacy N+1 pattern that fetched only the outer's own violations.
   * <p>
   * Partitions by {@code getInOperatorThreshold()} when the outer list is large; each chunk
   * issues one query with an {@code IN (...)} clause for exact matches plus a {@code LIKE OR ...}
   * tail for the inner-pathname prefixes. The {@code %} and {@code _} characters are escaped on
   * each outer pathname before being assembled into the LIKE pattern so user-supplied path
   * characters cannot widen the match.
   */
  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnamesOrInnerPathnames(
      String repositoryId,
      List<String> outerPathnames)
  {
    if (repositoryId == null || outerPathnames == null || outerPathnames.isEmpty()) {
      return List.of();
    }
    List<String> nonNullOuters = outerPathnames.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (nonNullOuters.isEmpty()) {
      return List.of();
    }
    List<List<String>> partitions = Lists.partition(nonNullOuters, getInOperatorThreshold());
    return partitions.stream()
        .flatMap(partition -> {
          try (TransactionContext tx = createTransactionContext()) {
            Condition exactMatch = REPOSITORY_POLICY_VIOLATION.PATHNAME.in(partition);
            Condition prefixMatch = null;
            for (String outer : partition) {
              String innerPrefix = outer + "!/";
              String escapedPrefix = innerPrefix
                  .replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_");
              Condition like = REPOSITORY_POLICY_VIOLATION.PATHNAME.like(escapedPrefix + "%", '\\');
              prefixMatch = prefixMatch == null ? like : prefixMatch.or(like);
            }
            Condition pathnameMatch = prefixMatch == null ? exactMatch : exactMatch.or(prefixMatch);
            return tx.dsl()
                .selectFrom(REPOSITORY_POLICY_VIOLATION)
                .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
                .and(pathnameMatch)
                .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
                .orderBy(REPOSITORY_POLICY_VIOLATION.PATHNAME,
                    REPOSITORY_POLICY_VIOLATION.THREAT_LEVEL.desc(),
                    REPOSITORY_POLICY_VIOLATION.POLICY_ID)
                .fetch(this::toEntity)
                .stream();
          }
        })
        .collect(Collectors.toList());
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnameAndWaived(
      TransactionContext tx,
      String repositoryId,
      String pathname,
      boolean isWaived)
  {
    return tx.dsl()
        .selectFrom(REPOSITORY_POLICY_VIOLATION)
        .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
        .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(isWaived))
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
      return getActiveByRepositoryIdAndPathnameAndWaived(tx, repositoryId, pathname, isWaived);
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
    // CLM-40943: roll inner-pathname rows (`outer.zip!/inner.jar`) into the outer artifact when
    // computing the per-repo summary. The hosted-repo archive-of-archives fan-out persists
    // per-inner-jar violations against synthetic inner pathnames; the Components page shows ONE
    // row per uploaded outer artifact and that row's threat tier must reflect the worst threat
    // anywhere inside the archive — outer's own violations OR any inner. Strip the "!/..." tail
    // before grouping so all inner rows fold under their outer, then take MAX threat-level per
    // outer and bucket outers by tier. Pure outers (no "!/") and inner-only outers both group
    // correctly under the same key.
    // Standard-SQL pathname normalizer: strip "!/..." tail. Compatible with both H2 and
    // PostgreSQL (POSITION + SUBSTRING are SQL:2003). The CASE handles pathnames without "!/"
    // by returning them unchanged.
    String stripExpr = "CASE WHEN POSITION('!/' IN pathname) > 0 "
        + "THEN SUBSTRING(pathname, 1, POSITION('!/' IN pathname) - 1) "
        + "ELSE pathname END";
    String sQuery =
        " SELECT COUNT(CASE WHEN max_threat_level >= 8 THEN 1 END)                              AS criticalCount," +
            "        COUNT(CASE WHEN max_threat_level >= 4 AND max_threat_level < 8 THEN 1 END) AS severeCount," +
            "        COUNT(CASE WHEN max_threat_level >= 2 AND max_threat_level < 4 THEN 1 END) AS moderateCount" +
            " FROM (SELECT MAX(threat_level) AS max_threat_level" +
            "       FROM " + getDatabaseSchema() + ".repository_policy_violation" +
            "       WHERE repository_id=?" +
            "         AND active=true" +
            "         AND waived=false" +
            "       GROUP BY " + stripExpr + ") AS subquery";

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

  public boolean hasActiveMalwareWaivedViolation(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
          .and(REPOSITORY_POLICY_VIOLATION.WAIVED.eq(true))
          .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
          .and(REPOSITORY_POLICY_VIOLATION.POLICY_NAME.eq("Security-Malicious"))
          .fetchOne(0, Integer.class) > 0;
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

  @Override
  public final void delete(RepositoryPolicyViolation entity) {
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, RepositoryPolicyViolation entity) {
    super.delete(tx, entity);
  }

  public void deleteByRepositoryId(TransactionContext tx, String repositoryId) {
    if (isDatabaseEmbedded()) {
      getByRepositoryId(repositoryId).forEach(this::delete);
    }
    else {
      tx.dsl()
          .deleteFrom(REPOSITORY_POLICY_VIOLATION)
          .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
          .execute();
    }
  }

  public void stampComponentId(
      final TransactionContext tx,
      final String repositoryId,
      final String pathname,
      final String componentId)
  {
    tx.dsl()
        .update(REPOSITORY_POLICY_VIOLATION)
        .set(REPOSITORY_POLICY_VIOLATION.COMPONENT_ID, componentId)
        .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(pathname))
        .execute();
  }

  /**
   * Stamps {@code component_id} on every active violation row whose pathname is either
   * {@code outerPathname} itself OR an inner-pathname under it ({@code outerPathname + "!/..."}).
   * Used by the hosted-repo archive-of-archives flow so a single NXRM componentId reaches BOTH
   * the outer artifact's violations AND the synthesized inner-pathname violations the evaluator
   * persists during fan-out. Without this, future code that joins on
   * {@code repository_policy_violation.component_id} (waiver-by-component, quarantine-by-component)
   * would silently miss inner findings.
   * <p>
   * The LIKE side of the predicate is escaped the same way as
   * {@link #getActiveByRepositoryIdAndPathnameOrInnerPathnames} so a real outer pathname
   * containing {@code %} or {@code _} doesn't turn the prefix into a wildcard.
   */
  public void stampComponentIdOnPathnameOrInnerPathnames(
      final TransactionContext tx,
      final String repositoryId,
      final String outerPathname,
      final String componentId)
  {
    if (repositoryId == null || outerPathname == null) {
      return;
    }
    String innerPrefix = outerPathname + "!/";
    String escapedPrefix = innerPrefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    // Limit the UPDATE to active rows only — symmetric with
    // getActiveByRepositoryIdAndPathnameOrInnerPathnames so the rows the SELECT can read are
    // exactly the rows this UPDATE writes. Stamping deactivated historical violation rows
    // would be harmless today (no current code path reads component_id on inactive rows), but
    // the asymmetry could mislead a future reader into thinking the filter on the SELECT side
    // wasn't required.
    tx.dsl()
        .update(REPOSITORY_POLICY_VIOLATION)
        .set(REPOSITORY_POLICY_VIOLATION.COMPONENT_ID, componentId)
        .where(REPOSITORY_POLICY_VIOLATION.REPOSITORY_ID.eq(repositoryId))
        .and(REPOSITORY_POLICY_VIOLATION.PATHNAME.eq(outerPathname)
            .or(REPOSITORY_POLICY_VIOLATION.PATHNAME.like(escapedPrefix + "%", '\\')))
        .and(REPOSITORY_POLICY_VIOLATION.ACTIVE.eq(true))
        .execute();
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

  private List<RepositoryResultsDetails> getRepositoryResultsDetailsNonAggregate(
      Set<String> repositoryIds,
      RepositoryResultsDetailsFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      List<Object> params = new ArrayList<>();

      String baseQuery = "SELECT violation.threat_level," +
          " violation.policy_name," +
          " repository.repository_manager_id," +
          " component.repository_id," +
          " component.component_id_format," +
          " component.pathname," +
          " component.component_id_coordinates_json," +
          " component.display_name," +
          " component.hash," +
          " component.match_state_id," +
          " component.last_evaluation_time," +
          " CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL) THEN" +
          " component.quarantine_time END AS quarantine_time," +
          " violation.waived," +
          " COALESCE(cf.constraint_facts_json, violation.constraint_facts_json) AS constraint_facts_json," +
          " violation.repository_policy_violation_id" +
          " FROM " + getDatabaseSchema() + ".repository_component component" +
          ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") +
          " " + getDatabaseSchema() + ".repository_policy_violation violation" +
          " ON component.repository_id = violation.repository_id AND component.pathname = violation.pathname" +
          " LEFT JOIN " + getDatabaseSchema() + ".policy_violation_constraint_facts cf" +
          " ON violation.constraint_facts_id = cf.policy_violation_constraint_facts_id" +
          " INNER JOIN " + getDatabaseSchema() + ".repository ON component.repository_id = repository.repository_id" +
          " WHERE component.repository_id IN " +
          buildJooqPositionalParameters(repositoryIds);

      StringBuilder sQuery = new StringBuilder(baseQuery);
      params.addAll(repositoryIds);

      String threatLevelClause =
          addThreatLevelFiltersJooq(detailsFilter.threatLevelFilters, detailsFilter.excludeThreatLevelZero);
      sQuery.append(threatLevelClause);
      if ((detailsFilter.excludeThreatLevelZero || !threatLevelClause.isEmpty())
          && detailsFilter.threatLevelFilters != null
          && detailsFilter.threatLevelFilters.size() == 2
          && (detailsFilter.threatLevelFilters.get(0) > 0 || detailsFilter.threatLevelFilters.get(1) < 10))
      {
        params.add(detailsFilter.threatLevelFilters.get(0));
        params.add(detailsFilter.threatLevelFilters.get(1));
      }

      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      sQuery.append(addSearchFiltersJooq(detailsFilter.searchFilters));
      if (!MapUtils.isEmpty(detailsFilter.searchFilters)) {
        if (detailsFilter.searchFilters.containsKey("POLICY_NAME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("POLICY_NAME").toLowerCase()) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("EVALUATION_TIME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("EVALUATION_TIME")) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("QUARANTINE_TIME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("QUARANTINE_TIME")) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("COMPONENT_COORDINATES")) {
          params.add(
              '%' + escapeLikePattern(detailsFilter.searchFilters.get("COMPONENT_COORDINATES").toLowerCase()) + '%');
        }
      }

      if (!detailsFilter.matchStateFilter.isEmpty()) {
        sQuery.append(" AND component.match_state_id = ?");
        params.add(detailsFilter.matchStateFilter);
      }

      sQuery.append(addFormatExclusionFiltersJooq(detailsFilter.formatExclusionPatterns, params));

      sQuery.append(validateAndAddSortFields(detailsFilter.sortFields));

      int offset = (detailsFilter.page - 1) * detailsFilter.pageSize;
      int pageSize = detailsFilter.pageSize + 1;
      sQuery.append(" OFFSET ? LIMIT ?");
      params.add(offset);
      params.add(pageSize);

      return tx.dsl()
          .resultQuery(sQuery.toString(), params.toArray())
          .fetchStream()
          .map(record -> {
            Object[] array = record.intoArray();
            String constraintFactsJson = extractClobAsString(array[13]);
            String policyViolationId = (String) array[14];
            return new RepositoryResultsDetails(getInteger(array[0]), (String) array[1],
                (String) array[2], (String) array[3], (String) array[4], (String) array[5], (String) array[6],
                (String) array[7], (String) array[8], (String) array[9],
                array[10] == null ? null : new Date(((Timestamp) array[10]).getTime()),
                array[11] == null ? null : new Date(((Timestamp) array[11]).getTime()), (Boolean) array[12],
                constraintFactsJson, policyViolationId);
          })
          .collect(Collectors.toList());
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
          .append(" MAX(component.last_evaluation_time) AS last_evaluation_time,")
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

      String threatLevelClause =
          addThreatLevelFiltersJooq(detailsFilter.threatLevelFilters, detailsFilter.excludeThreatLevelZero);
      select1Builder.append(threatLevelClause);
      if ((detailsFilter.excludeThreatLevelZero || !threatLevelClause.isEmpty())
          && detailsFilter.threatLevelFilters != null
          && detailsFilter.threatLevelFilters.size() == 2
          && (detailsFilter.threatLevelFilters.get(0) > 0 || detailsFilter.threatLevelFilters.get(1) < 10))
      {
        params.add(detailsFilter.threatLevelFilters.get(0));
        params.add(detailsFilter.threatLevelFilters.get(1));
      }

      select1Builder.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      select1Builder.append(addSearchFiltersJooq(detailsFilter.searchFilters));
      if (!MapUtils.isEmpty(detailsFilter.searchFilters)) {
        if (detailsFilter.searchFilters.containsKey("POLICY_NAME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("POLICY_NAME").toLowerCase()) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("EVALUATION_TIME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("EVALUATION_TIME")) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("QUARANTINE_TIME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("QUARANTINE_TIME")) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("COMPONENT_COORDINATES")) {
          params.add(
              '%' + escapeLikePattern(detailsFilter.searchFilters.get("COMPONENT_COORDINATES").toLowerCase()) + '%');
        }
      }

      if (!detailsFilter.matchStateFilter.isEmpty()) {
        select1Builder.append(" AND component.match_state_id = ?");
        params.add(detailsFilter.matchStateFilter);
      }

      select1Builder.append(addFormatExclusionFiltersJooq(detailsFilter.formatExclusionPatterns, params));

      select1Builder.append(" GROUP BY repository.repository_manager_id, component.repository_id, component.pathname");

      String select1 = select1Builder.toString();
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
          " last_evaluation_time," +
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
          " component.last_evaluation_time," +
          " CASE WHEN (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL)" +
          " THEN component.quarantine_time END AS quarantine_time" +
          " FROM " + getDatabaseSchema() + ".repository_component component" +
          " INNER JOIN (" + select2 + ") AS t2" +
          " ON t2.pathname = component.pathname AND t2.repository_id = component.repository_id" +
          validateAndAddSortFields(detailsFilter.sortFields, true) +
          " LIMIT " + pageSize +
          " OFFSET " + offset;

      return tx.dsl()
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
                array[11] == null ? null : new Date(((Timestamp) array[11]).getTime()),
                null,
                null,
                null);
          })
          .collect(Collectors.toList());
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

  public RepositoryResultsCountSummary countRepositoryResultsDetails(
      Set<String> repositoryIds,
      RepositoryResultsDetailsFilter detailsFilter)
  {
    try (TransactionContext tx = createTransactionContext()) {
      List<Object> params = new ArrayList<>();

      String baseQuery = "SELECT" +
          " COUNT(*) AS total_count," +
          " COUNT(CASE WHEN violation.waived = false THEN 1 END) AS open_count," +
          " COUNT(CASE WHEN violation.waived = true THEN 1 END) AS waived_count," +
          " COUNT(CASE WHEN component.quarantine_time IS NOT NULL" +
          " AND component.unquarantine_time IS NULL" +
          " AND violation.action_type_id = 'fail' THEN 1 END) AS quarantined_count" +
          " FROM " + getDatabaseSchema() + ".repository_component component" +
          ((hasNonViolatingFilter(detailsFilter.violationStateFilters)) ? " LEFT JOIN" : " INNER JOIN") +
          " " + getDatabaseSchema() + ".repository_policy_violation violation" +
          " ON component.repository_id = violation.repository_id AND component.pathname = violation.pathname" +
          " INNER JOIN " + getDatabaseSchema() + ".repository ON component.repository_id = repository.repository_id" +
          " WHERE component.repository_id IN " +
          buildJooqPositionalParameters(repositoryIds);

      StringBuilder sQuery = new StringBuilder(baseQuery);
      params.addAll(repositoryIds);

      String threatLevelClause =
          addThreatLevelFiltersJooq(detailsFilter.threatLevelFilters, detailsFilter.excludeThreatLevelZero);
      sQuery.append(threatLevelClause);
      if ((detailsFilter.excludeThreatLevelZero || !threatLevelClause.isEmpty())
          && detailsFilter.threatLevelFilters != null
          && detailsFilter.threatLevelFilters.size() == 2
          && (detailsFilter.threatLevelFilters.get(0) > 0 || detailsFilter.threatLevelFilters.get(1) < 10))
      {
        params.add(detailsFilter.threatLevelFilters.get(0));
        params.add(detailsFilter.threatLevelFilters.get(1));
      }

      sQuery.append(addViolationStateFilters(detailsFilter.violationStateFilters));

      sQuery.append(addSearchFiltersJooq(detailsFilter.searchFilters));
      if (!MapUtils.isEmpty(detailsFilter.searchFilters)) {
        if (detailsFilter.searchFilters.containsKey("POLICY_NAME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("POLICY_NAME").toLowerCase()) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("EVALUATION_TIME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("EVALUATION_TIME")) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("QUARANTINE_TIME")) {
          params.add('%' + escapeLikePattern(detailsFilter.searchFilters.get("QUARANTINE_TIME")) + '%');
        }
        if (detailsFilter.searchFilters.containsKey("COMPONENT_COORDINATES")) {
          params.add(
              '%' + escapeLikePattern(detailsFilter.searchFilters.get("COMPONENT_COORDINATES").toLowerCase()) + '%');
        }
      }

      if (!detailsFilter.matchStateFilter.isEmpty()) {
        sQuery.append(" AND component.match_state_id = ?");
        params.add(detailsFilter.matchStateFilter);
      }

      sQuery.append(addFormatExclusionFiltersJooq(detailsFilter.formatExclusionPatterns, params));

      var record = tx.dsl()
          .resultQuery(sQuery.toString(), params.toArray())
          .fetchOne();
      if (record == null) {
        return new RepositoryResultsCountSummary(0L, 0L, 0L, 0L);
      }

      Object[] result = record.intoArray();

      return new RepositoryResultsCountSummary(
          ((Number) result[0]).longValue(),
          ((Number) result[1]).longValue(),
          ((Number) result[2]).longValue(),
          ((Number) result[3]).longValue());
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
                " (component.quarantine_time IS NOT NULL AND component.unquarantine_time IS NULL" +
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

  private static String addFormatExclusionFiltersJooq(
      final Map<String, List<String>> formatExclusionPatterns,
      final List<Object> params)
  {
    if (MapUtils.isEmpty(formatExclusionPatterns)) {
      return "";
    }

    StringBuilder query = new StringBuilder();
    for (Map.Entry<String, List<String>> entry : formatExclusionPatterns.entrySet()) {
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

  private static String addThreatLevelFiltersJooq(List<Integer> filters, boolean excludeThreatLevelZero) {
    StringBuilder result = new StringBuilder();
    if (excludeThreatLevelZero) {
      result.append(" AND violation.threat_level > 0");
    }
    if (filters != null && filters.size() == 2 && (filters.get(0) > 0 || filters.get(1) < 10)) {
      if (filters.get(0) == 0) {
        result.append(
            " AND (violation.threat_level IS NULL OR (violation.threat_level >= ? AND violation.threat_level <= ?))");
      }
      else {
        result.append(" AND violation.threat_level >= ? AND violation.threat_level <= ?");
      }
    }
    return result.toString();
  }

  private static String addSearchFiltersJooq(Map<String, String> filters) {
    StringBuilder query = new StringBuilder();
    if (!MapUtils.isEmpty(filters)) {
      if (filters.containsKey("POLICY_NAME")) {
        query.append(" AND LOWER(violation.policy_name) LIKE ?");
      }
      if (filters.containsKey("EVALUATION_TIME")) {
        query.append(" AND (last_evaluation_time IS NOT NULL AND TO_CHAR(last_evaluation_time, 'YYYY-MM-DD') LIKE ?)");
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

  private String validateAndAddSortFields(final List<SortField> sortFields) {
    return validateAndAddSortFields(sortFields, false);
  }

  private String validateAndAddSortFields(final List<SortField> sortFields, boolean isAggregate) {
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
      // Add unique tiebreaker for stable pagination - prevents duplicate rows across pages
      if (isAggregate) {
        // In aggregate mode, violation alias not available - use component composite key
        result.add("component.pathname");
        result.add("component.repository_id");
      }
      else {
        result.add("violation.repository_policy_violation_id NULLS LAST");
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
      case EVALUATION_TIME:
        return "last_evaluation_time";
      case QUARANTINE_TIME:
        return "quarantine_time";
      default:
        return "";
    }
  }

  private String buildJooqPositionalParameters(Collection<?> collection) {
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < collection.size(); i++) {
      joiner.add("?");
    }
    return "(" + joiner.toString() + ")";
  }

  private String extractClobAsString(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String) {
      return (String) value;
    }
    if (value instanceof java.sql.Clob) {
      try {
        java.sql.Clob clob = (java.sql.Clob) value;
        return clob.getSubString(1, (int) clob.length());
      }
      catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(RepositoryPolicyViolationDAO.class)
            .debug("Failed to read Clob value", e);
        return null;
      }
    }
    return value.toString();
  }

  private static String escapeLikePattern(String input) {
    if (input == null) {
      return "";
    }
    return input
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  @Override
  public Class<RepositoryPolicyViolation> getEntityClass() {
    return RepositoryPolicyViolation.class;
  }
}

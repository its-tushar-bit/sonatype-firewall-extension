/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.development.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.prioritization.IntegrationStatusSummary;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;

/**
 * Data Access Object for retrieving integration status information for applications.
 * Provides optimized queries that replace multiple individual queries with single JOIN operations.
 *
 * @since 1.196
 */
@Named
@Singleton
public class IntegrationStatusDAO
    extends AbstractOperationalSqlDAO<Application>
{
  private static final String QUERY_TEMPLATE =
      "WITH latest_policy_eval AS (" +
          "  SELECT DISTINCT ON (pe1.owner_id) pe1.owner_id, pe1.time, pe1.scan_id " +
          "  FROM %1$s.policy_evaluation pe1 " +
          "  INNER JOIN %1$s.last_policy_evaluation lpe ON pe1.policy_evaluation_id = lpe.policy_evaluation_id " +
          "  WHERE lpe.stage_type_id = ? AND pe1.for_monitoring = false AND pe1.reevaluation = false " +
          "  ORDER BY pe1.owner_id, pe1.time DESC" +
          "), " +
          "latest_commit AS (" +
          "  SELECT DISTINCT ON (sc1.application_id) sc1.application_id, sc1.commit_time " +
          "  FROM %1$s.source_control_default_branch_commit_history sc1 " +
          "  ORDER BY sc1.application_id, sc1.commit_time DESC" +
          "), " +
          "ci_eval AS (" +
          "  SELECT DISTINCT pe3.owner_id " +
          "  FROM %1$s.policy_evaluation pe3 " +
          "  WHERE pe3.stage_type_id = ? AND pe3.reevaluation = false " +
          "    AND pe3.for_monitoring = false AND pe3.time >= ?" +
          ") " +
          "SELECT " +
          "  a.application_id, a.name, a.public_id, a.organization_id, " +
          "  pe.time, pe.scan_id, sc.commit_time, " +
          "  CASE WHEN ci.owner_id IS NOT NULL THEN true ELSE false END " +
          "FROM %1$s.application a " +
          "LEFT JOIN latest_policy_eval pe ON a.application_id = pe.owner_id " +
          "LEFT JOIN latest_commit sc      ON a.application_id = sc.application_id " +
          "LEFT JOIN ci_eval ci            ON a.application_id = ci.owner_id " +
          "WHERE a.application_id IN (%2$s) " +
          "ORDER BY a.application_id";

  @Inject
  public IntegrationStatusDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<IntegrationStatusSummary> getIntegrationStatusBulk(
      Collection<String> applicationIds,
      Date ciLookbackDate)
  {
    return applicationIds.isEmpty()
        ? new ArrayList<>()
        : getListWithSqlInClause(applicationIds, ids -> getIntegrationStatusForApplications(ids, ciLookbackDate));
  }

  private List<IntegrationStatusSummary> getIntegrationStatusForApplications(
      Collection<String> applicationIds,
      Date ciLookbackDate)
  {
    if (!isDatabasePostgresql()) {
      throw new UnsupportedOperationException("This operation is only supported for PostgreSQL databases");
    }

    try (TransactionContext tx = createTransactionContext()) {
      String placeholders = applicationIds.stream()
          .map(id -> "?")
          .collect(Collectors.joining(","));
      String query = String.format(QUERY_TEMPLATE, getDatabaseSchema(), placeholders);

      // Build parameters list: stage_id (2x for CTEs), ciLookbackDate, then applicationIds
      List<Object> params = new ArrayList<>();
      params.add(Stage.ID_BUILD);
      params.add(Stage.ID_BUILD);
      params.add(DSL.val(ciLookbackDate, SQLDataType.TIMESTAMP));
      params.addAll(applicationIds);

      try (var stream = tx.dsl()
          .resultQuery(query, params.toArray())
          .fetchStream()
          .map(record -> mapToIntegrationStatusSummary(record.intoArray())))
      {
        return stream.collect(Collectors.toList());
      }
    }
  }

  private IntegrationStatusSummary mapToIntegrationStatusSummary(Object[] row) {
    return new IntegrationStatusSummary(
        (String) row[0],
        (String) row[1],
        (String) row[2],
        (String) row[3],
        row[4] != null ? ((Date) row[4]).getTime() : 0L,
        (String) row[5],
        row[6] != null ? ((Date) row[6]).getTime() : 0L,
        (Boolean) row[7]);
  }

  @Override
  public int insert(TransactionContext tx, Application entity) {
    throw new UnsupportedOperationException(
        "IntegrationStatusDAO is a read-only DAO and does not support insert operations. " +
            "Use ApplicationDAO for Application entity management.");
  }

  @Override
  public int update(TransactionContext tx, Application entity) {
    throw new UnsupportedOperationException(
        "IntegrationStatusDAO is a read-only DAO and does not support update operations. " +
            "Use ApplicationDAO for Application entity management.");
  }

  @Override
  public Table<?> getJooqTable() {
    return APPLICATION;
  }

  @Override
  public Class<Application> getEntityClass() {
    return Application.class;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUserActivity;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class SourceControlUserActivityDAO
    extends AbstractOperationalSqlDAO<SourceControlUserActivity>
{
  @Inject
  public SourceControlUserActivityDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void insert(TransactionContext tx, final SourceControlUserActivity entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when inserting all entities.
    super.insert(tx, entity);
  }

  @Override
  public void insert(final SourceControlUserActivity entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when inserting all entities.
    super.insert(entity);
  }

  public List<SourceControlUserActivity> getBySourceControlUserId(TransactionContext tx, String sourceControlUserId) {
    String sQuery = "SELECT entity FROM SourceControlUserActivity entity WHERE entity.sourceControlUserId = ?1";
    return getList(tx, sQuery, sourceControlUserId);
  }

  @Override
  public final void delete(TransactionContext tx, SourceControlUserActivity entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
    super.delete(tx, entity);
  }

  @Override
  public final void delete(SourceControlUserActivity entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
    super.delete(entity);
  }

  /**
   * Inserts all activities massively. This operation requires the id field of all objects to be already filled with the
   * expected UUID that will go into the database. The way the inserts work can potentially cause existing records to
   * have their ids updated. This side effect is not desirable but currently is not impactful as it's not referenced
   * anywhere.
   */
  public void insertAllIfNew(TransactionContext tx, final List<SourceControlUserActivity> userActivities) {
    if (userActivities.isEmpty()) {
      return;
    }
    String dbSchema = getDatabaseSchema();
    SourceControlUserActivityDAOQueryBuilder queryBuilder =
        isDatabasePostgresql() ? new PostgresqlSourceControlUserActivityDAOQueryBuilder(dbSchema)
            : new DefaultSourceControlUserActivityDAOQueryBuilder(dbSchema);

    List<SourceControlUserActivity> batchActivities = new ArrayList<>(queryBuilder.getBatchLimit());
    for (final SourceControlUserActivity userActivity : userActivities) {
      batchActivities.add(userActivity);
      if (batchActivities.size() == queryBuilder.getBatchLimit()) {
        insertBatch(tx, queryBuilder, batchActivities);
        batchActivities = new ArrayList<>(queryBuilder.getBatchLimit());
      }
    }
    insertBatch(tx, queryBuilder, batchActivities);
  }

  private static void insertBatch(
      final TransactionContext tx,
      final SourceControlUserActivityDAOQueryBuilder queryBuilder,
      final List<SourceControlUserActivity> batchActivities)
  {
    if (batchActivities.size() > 0) {
      jakarta.persistence.Query query =
          tx.createNativeQuery(queryBuilder.getMassiveInsertNativeQuery(batchActivities.size()));
      int i = 0;
      for (SourceControlUserActivity userActivity : batchActivities) {
        query.setParameter(++i, StringUtils.isNotBlank(userActivity.getId())
                ? userActivity.getId() : UUID.randomUUID().toString().replace("-", ""))
            .setParameter(++i, userActivity.getSourceControlUserId())
            .setParameter(++i, userActivity.getCommitYearMonth());
      }
      query.executeUpdate();
    }
  }

  private interface SourceControlUserActivityDAOQueryBuilder
  {
    String getMassiveInsertNativeQuery(int activitiesToInsert);

    int getBatchLimit();

    int getUpdateBatchLimit();
  }

  private static class DefaultSourceControlUserActivityDAOQueryBuilder
      implements SourceControlUserActivityDAO.SourceControlUserActivityDAOQueryBuilder
  {
    // Batch limit is divided by 3 to account for the 32767 parameter upper limit of the PreparedStatement
    private static final int BATCH_LIMIT = AbstractOperationalSqlDAO.H2_IN_OPERATOR_THRESHOLD / 3;

    private static final int BATCH_UPDATE_LIMIT = AbstractOperationalSqlDAO.H2_IN_OPERATOR_THRESHOLD;

    private final String databaseSchema;

    public DefaultSourceControlUserActivityDAOQueryBuilder(final String databaseSchema) {
      this.databaseSchema = databaseSchema;
    }

    @Override
    public String getMassiveInsertNativeQuery(int activitiesToInsert) {
      return "MERGE INTO " + databaseSchema + ".source_control_user_activity" +
          " (source_control_user_activity_id, source_control_user_id, commit_year_month)" +
          " KEY (source_control_user_id, commit_year_month)" +
          " VALUES (?, ?, ?)" + StringUtils.repeat(", (?, ?, ?)", activitiesToInsert - 1);
    }

    @Override
    public int getBatchLimit() {
      return BATCH_LIMIT;
    }

    @Override
    public int getUpdateBatchLimit() {
      return BATCH_UPDATE_LIMIT;
    }
  }

  private static class PostgresqlSourceControlUserActivityDAOQueryBuilder
      implements SourceControlUserActivityDAO.SourceControlUserActivityDAOQueryBuilder
  {
    // Batch limit is divided by 3 to account for the 32767 parameter upper limit of the PreparedStatement
    private static final int BATCH_LIMIT = AbstractOperationalSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD / 3;

    private static final int BATCH_UPDATE_LIMIT = AbstractOperationalSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD;

    private final String databaseSchema;

    public PostgresqlSourceControlUserActivityDAOQueryBuilder(final String databaseSchema) {
      this.databaseSchema = databaseSchema;
    }

    @Override
    public String getMassiveInsertNativeQuery(int activitiesToInsert) {
      return "INSERT INTO " + databaseSchema + ".source_control_user_activity" +
          " (source_control_user_activity_id, source_control_user_id, commit_year_month)" +
          " VALUES (?, ?, ?)" + StringUtils.repeat(", (?, ?, ?)", activitiesToInsert - 1) +
          " ON CONFLICT (source_control_user_id, commit_year_month) DO NOTHING";
    }

    @Override
    public int getBatchLimit() {
      return BATCH_LIMIT;
    }

    @Override
    public int getUpdateBatchLimit() {
      return BATCH_UPDATE_LIMIT;
    }
  }

  public Stream<SourceControlUserActivityTelemetryDTO> getActivitiesNotSentToTelemetry() {
    String sQuery =
        "SELECT sourceControlUserActivity.id, " +
            "sourceControlUser.email," +
            "sourceControlUser.applicationId, " +
            "sourceControlUserActivity.commitYearMonth " +
            "FROM SourceControlUserActivity sourceControlUserActivity, SourceControlUser sourceControlUser " +
            "WHERE sourceControlUserActivity.sourceControlUserId = sourceControlUser.id " +
            "and sourceControlUserActivity.isSentToTelemetry = false ";
    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createQuery(sQuery);
      return ((Stream<Object[]>) query.getResultStream()).map(object -> {
        SourceControlUserActivityTelemetryDTO sourceControlUserActivityTelemetryDTO =
            new SourceControlUserActivityTelemetryDTO();
        sourceControlUserActivityTelemetryDTO.setSourceControlUserActivityId((String) object[0]);
        sourceControlUserActivityTelemetryDTO.setEmail((String) object[1]);
        sourceControlUserActivityTelemetryDTO.setApplicationId((String) object[2]);
        sourceControlUserActivityTelemetryDTO.setCommitYearMonth((LocalDate) object[3]);

        return sourceControlUserActivityTelemetryDTO;
      });
    }
  }

  public int updateActivitiesSentToTelemetry(final Set<String> sourceControlUserActivityIds) {
    int updatedIds = 0;
    if (sourceControlUserActivityIds.isEmpty()) {
      return updatedIds;
    }
    String dbSchema = getDatabaseSchema();
    SourceControlUserActivityDAOQueryBuilder queryBuilder =
        isDatabasePostgresql() ? new PostgresqlSourceControlUserActivityDAOQueryBuilder(dbSchema)
            : new DefaultSourceControlUserActivityDAOQueryBuilder(dbSchema);
    Set<String> batchActivityIds = new HashSet<>(queryBuilder.getUpdateBatchLimit());
    for (final String userActivityId : sourceControlUserActivityIds) {
      batchActivityIds.add(userActivityId);
      if (batchActivityIds.size() == queryBuilder.getUpdateBatchLimit()) {
        updatedIds += updateBatch(batchActivityIds);
        batchActivityIds = new HashSet<>(queryBuilder.getUpdateBatchLimit());
      }
    }
    updatedIds += updateBatch(batchActivityIds);
    return updatedIds;
  }

  private String getMassiveUpdateQuery() {
    return "UPDATE SourceControlUserActivity entity SET entity.isSentToTelemetry=TRUE " +
        "WHERE entity.id IN (?1) ";
  }

  private int updateBatch(
      final Set<String> sourceControlUserActivityIds)
  {
    if (!sourceControlUserActivityIds.isEmpty()) {
      return createQuery(getMassiveUpdateQuery(), sourceControlUserActivityIds).executeUpdate();
    }
    return 0;
  }
}

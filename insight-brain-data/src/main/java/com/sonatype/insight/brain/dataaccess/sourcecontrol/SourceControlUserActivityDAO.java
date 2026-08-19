/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUserActivity;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlUserActivity.SOURCE_CONTROL_USER_ACTIVITY;

@Named
@Singleton
public class SourceControlUserActivityDAO
    extends AbstractOperationalSqlDAO<SourceControlUserActivity>
{
  @Inject
  public SourceControlUserActivityDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
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
        isDatabasePostgresql()
            ? new PostgresqlSourceControlUserActivityDAOQueryBuilder(dbSchema)
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

  @SuppressWarnings("PMD.UnusedFormalParameter") // queryBuilder kept for API consistency with similar batch methods
  private void insertBatch(
      final TransactionContext tx,
      final SourceControlUserActivityDAOQueryBuilder queryBuilder,
      final List<SourceControlUserActivity> batchActivities)
  {
    if (batchActivities.isEmpty()) {
      return;
    }

    // Insert one row at a time, skipping if a record with same user_id+commit_year_month exists
    // jOOQ's onDuplicateKeyIgnore() generates PostgreSQL MERGE syntax which H2 doesn't support
    for (SourceControlUserActivity userActivity : batchActivities) {
      String id = StringUtils.isNotBlank(userActivity.getId())
          ? userActivity.getId()
          : UUID.randomUUID().toString().replace("-", "");
      Date commitYearMonth = userActivity.getCommitYearMonth() != null
          ? Date.from(userActivity.getCommitYearMonth().atStartOfDay(ZoneId.systemDefault()).toInstant())
          : null;

      // Check if record already exists (unique constraint on source_control_user_id + commit_year_month)
      boolean exists = tx.dsl()
          .fetchExists(
              tx.dsl()
                  .selectFrom(SOURCE_CONTROL_USER_ACTIVITY)
                  .where(SOURCE_CONTROL_USER_ACTIVITY.SOURCE_CONTROL_USER_ID.eq(userActivity.getSourceControlUserId()))
                  .and(SOURCE_CONTROL_USER_ACTIVITY.COMMIT_YEAR_MONTH.eq(commitYearMonth)));

      if (!exists) {
        tx.dsl()
            .insertInto(SOURCE_CONTROL_USER_ACTIVITY)
            .set(SOURCE_CONTROL_USER_ACTIVITY.SOURCE_CONTROL_USER_ACTIVITY_ID, id)
            .set(SOURCE_CONTROL_USER_ACTIVITY.SOURCE_CONTROL_USER_ID, userActivity.getSourceControlUserId())
            .set(SOURCE_CONTROL_USER_ACTIVITY.COMMIT_YEAR_MONTH, commitYearMonth)
            .execute();
      }
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

  public List<SourceControlUserActivityTelemetryDTO> getActivitiesNotSentToTelemetry() {
    try (TransactionContext tx = createTransactionContext()) {
      var sourceControlUser =
          com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlUser.SOURCE_CONTROL_USER;

      return tx.dsl()
          .select(
              SOURCE_CONTROL_USER_ACTIVITY.SOURCE_CONTROL_USER_ACTIVITY_ID,
              sourceControlUser.EMAIL,
              sourceControlUser.APPLICATION_ID,
              SOURCE_CONTROL_USER_ACTIVITY.COMMIT_YEAR_MONTH)
          .from(SOURCE_CONTROL_USER_ACTIVITY)
          .join(sourceControlUser)
          .on(SOURCE_CONTROL_USER_ACTIVITY.SOURCE_CONTROL_USER_ID.eq(sourceControlUser.SOURCE_CONTROL_USER_ID))
          .where(SOURCE_CONTROL_USER_ACTIVITY.IS_SENT_TO_TELEMETRY.eq(false))
          .fetch()
          .map(record -> {
            SourceControlUserActivityTelemetryDTO dto = new SourceControlUserActivityTelemetryDTO();
            dto.setSourceControlUserActivityId(
                record.get(SOURCE_CONTROL_USER_ACTIVITY.SOURCE_CONTROL_USER_ACTIVITY_ID));
            dto.setEmail(record.get(sourceControlUser.EMAIL));
            dto.setApplicationId(record.get(sourceControlUser.APPLICATION_ID));
            Date commitYearMonth = record.get(SOURCE_CONTROL_USER_ACTIVITY.COMMIT_YEAR_MONTH);
            dto.setCommitYearMonth(commitYearMonth != null
                ? commitYearMonth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null);
            return dto;
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
        isDatabasePostgresql()
            ? new PostgresqlSourceControlUserActivityDAOQueryBuilder(dbSchema)
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

  private int updateBatch(final Set<String> sourceControlUserActivityIds) {
    if (!sourceControlUserActivityIds.isEmpty()) {
      try (TransactionContext tx = createTransactionContext()) {
        tx.begin();
        int updated = tx.dsl()
            .update(SOURCE_CONTROL_USER_ACTIVITY)
            .set(SOURCE_CONTROL_USER_ACTIVITY.IS_SENT_TO_TELEMETRY, true)
            .where(SOURCE_CONTROL_USER_ACTIVITY.SOURCE_CONTROL_USER_ACTIVITY_ID.in(sourceControlUserActivityIds))
            .execute();
        tx.commit();
        return updated;
      }
    }
    return 0;
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_USER_ACTIVITY;
  }

  @Override
  public Class<SourceControlUserActivity> getEntityClass() {
    return SourceControlUserActivity.class;
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class SourceControlUserDAO
    extends AbstractOperationalSqlDAO<SourceControlUser>
{
  @Inject
  public SourceControlUserDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public final void delete(TransactionContext tx, SourceControlUser entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
    super.delete(tx, entity);
  }

  @Override
  public final void delete(SourceControlUser entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
    super.delete(entity);
  }

  public List<SourceControlUser> getByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM SourceControlUser entity WHERE entity.applicationId=?1";
    return getList(sQuery, applicationId);
  }

  public Map<String, String> getUserIdByEmailFilteringByApplicationId(String applicationId) {
    return getByApplicationId(applicationId).stream()
        .collect(Collectors.toMap(SourceControlUser::getEmail, SourceControlUser::getId));
  }

  /**
   * Inserts all activities massively. This operation requires the id field of all objects to be already filled with the
   * expected UUID that will go into the database. If a record already exists for a particular application-email pair,
   * it's insertion will be ignored
   */
  public void insertAllIfNew(TransactionContext tx, final List<SourceControlUser> usersToInsert) {
    if (usersToInsert.isEmpty()) {
      return;
    }

    String dbSchema = getDatabaseSchema();
    SourceControlUserDAOQueryBuilder queryBuilder =
        isDatabasePostgresql()
            ? new PostgresqlSourceControlUserDAOQueryBuilder(dbSchema)
            : new DefaultSourceControlUserDAOQueryBuilder(dbSchema);

    final jakarta.persistence.Query query =
        tx.createNativeQuery(queryBuilder.getMassiveInsertNativeQuery(usersToInsert));
    int i = 0;
    for (SourceControlUser user : usersToInsert) {
      query.setParameter(++i, user.getId())
          .setParameter(++i, user.getApplicationId())
          .setParameter(++i, user.getEmail());
    }
    query.executeUpdate();
  }

  private interface SourceControlUserDAOQueryBuilder
  {
    String getMassiveInsertNativeQuery(final List<SourceControlUser> usersToInsert);
  }

  private static class DefaultSourceControlUserDAOQueryBuilder
      implements SourceControlUserDAOQueryBuilder
  {
    private final String databaseSchema;

    public DefaultSourceControlUserDAOQueryBuilder(final String databaseSchema) {
      this.databaseSchema = databaseSchema;
    }

    @Override
    public String getMassiveInsertNativeQuery(final List<SourceControlUser> usersToInsert) {
      return "INSERT INTO " + databaseSchema + ".source_control_user (source_control_user_id, application_id, email)" +
          " SELECT sour.* FROM ( SELECT ? || '' as id,? || '' as app,? || '' as em" +
          StringUtils.repeat(" UNION SELECT ? || '' as id,? || '' as app,? || '' as em", usersToInsert.size() - 1) +
          ") sour WHERE NOT EXISTS (SELECT source_control_user_id FROM " + databaseSchema + ".source_control_user " +
          "WHERE application_id=sour.app AND email=sour.em)";
    }
  }

  private static class PostgresqlSourceControlUserDAOQueryBuilder
      implements SourceControlUserDAOQueryBuilder
  {
    private final String databaseSchema;

    public PostgresqlSourceControlUserDAOQueryBuilder(final String databaseSchema) {
      this.databaseSchema = databaseSchema;
    }

    @Override
    public String getMassiveInsertNativeQuery(final List<SourceControlUser> usersToInsert) {
      return "INSERT INTO " + databaseSchema + ".source_control_user (source_control_user_id, application_id, email) " +
          "VALUES (?, ?, ?)" + StringUtils.repeat(", (?, ?, ?)", usersToInsert.size() - 1) +
          " ON CONFLICT (application_id, email) DO NOTHING";
    }
  }
}

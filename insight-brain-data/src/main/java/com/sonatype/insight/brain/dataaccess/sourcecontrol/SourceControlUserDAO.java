/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

public class SourceControlUserDAO
    extends AbstractOperationalSqlDAO<SourceControlUser>
{
  @Override
  public void delete(final TransactionContext tx, final SourceControlUser entity) {
    // Cascade to source control user activity
    new SourceControlUserActivityDAO().deleteBySourceControlUserId(tx, entity.getId());
    super.delete(tx, entity);
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

    SourceControlUserDAOQueryBuilder queryBuilder =
        isDatabasePostgresql() ? new PostgresqlSourceControlUserDAOQueryBuilder()
            : new DefaultSourceControlUserDAOQueryBuilder();

    final javax.persistence.Query query =
        tx.createNativeQuery(queryBuilder.getMassiveInsertNativeQuery(usersToInsert));
    int i = 0;
    for (SourceControlUser user : usersToInsert) {
      query.setParameter(++i, user.getId())
          .setParameter(++i, user.getApplicationId())
          .setParameter(++i, user.getEmail());
    }
    query.executeUpdate();
  }

  private boolean isDatabasePostgresql() {
    return !OperationalDataStoreProvider.isDatabaseInMemory() && org.postgresql.Driver.class.getName()
        .equals(OperationalDataStoreProvider.getDatabaseConfig().getDriverClassName());
  }

  private interface SourceControlUserDAOQueryBuilder
  {
    String getMassiveInsertNativeQuery(final List<SourceControlUser> usersToInsert);
  }

  private static class DefaultSourceControlUserDAOQueryBuilder
      implements SourceControlUserDAOQueryBuilder
  {
    @Override
    public String getMassiveInsertNativeQuery(final List<SourceControlUser> usersToInsert) {
      final String dbSchema = OperationalDataStoreProvider.getDatabaseSchema();
      return "INSERT INTO " + dbSchema + ".source_control_user (source_control_user_id, application_id, email)" +
          " SELECT sour.* FROM ( SELECT ? || '' as id,? || '' as app,? || '' as em" +
          StringUtils.repeat(" UNION SELECT ? || '' as id,? || '' as app,? || '' as em", usersToInsert.size() - 1) +
          ") sour WHERE NOT EXISTS (SELECT source_control_user_id FROM " + dbSchema + ".source_control_user " +
          "WHERE application_id=sour.app AND email=sour.em)";
    }
  }

  private static class PostgresqlSourceControlUserDAOQueryBuilder
      implements SourceControlUserDAOQueryBuilder
  {
    @Override
    public String getMassiveInsertNativeQuery(final List<SourceControlUser> usersToInsert) {
      final String dbSchema = OperationalDataStoreProvider.getDatabaseSchema();
      return "INSERT INTO " + dbSchema + ".source_control_user (source_control_user_id, application_id, email) " +
          "VALUES (?, ?, ?)" + StringUtils.repeat(", (?, ?, ?)", usersToInsert.size() - 1) +
          " ON CONFLICT (application_id, email) DO NOTHING";
    }
  }
}

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
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlUser.SOURCE_CONTROL_USER;

@Named
@Singleton
public class SourceControlUserDAO
    extends AbstractOperationalSqlDAO<SourceControlUser>
{
  @Inject
  public SourceControlUserDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<SourceControlUser> getByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_USER)
          .where(SOURCE_CONTROL_USER.APPLICATION_ID.eq(applicationId))
          .fetch(this::toEntity);
    }
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

    // Insert one row at a time, skipping if a record with same application_id+email exists
    // jOOQ's onDuplicateKeyIgnore() generates PostgreSQL MERGE syntax which H2 doesn't support
    for (SourceControlUser user : usersToInsert) {
      // Check if record already exists (unique constraint on application_id + email)
      boolean exists = tx.dsl()
          .fetchExists(
              tx.dsl()
                  .selectFrom(SOURCE_CONTROL_USER)
                  .where(SOURCE_CONTROL_USER.APPLICATION_ID.eq(user.getApplicationId()))
                  .and(SOURCE_CONTROL_USER.EMAIL.eq(user.getEmail())));

      if (!exists) {
        tx.dsl()
            .insertInto(SOURCE_CONTROL_USER)
            .set(SOURCE_CONTROL_USER.SOURCE_CONTROL_USER_ID, user.getId())
            .set(SOURCE_CONTROL_USER.APPLICATION_ID, user.getApplicationId())
            .set(SOURCE_CONTROL_USER.EMAIL, user.getEmail())
            .execute();
      }
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_USER;
  }

  @Override
  public Class<SourceControlUser> getEntityClass() {
    return SourceControlUser.class;
  }
}

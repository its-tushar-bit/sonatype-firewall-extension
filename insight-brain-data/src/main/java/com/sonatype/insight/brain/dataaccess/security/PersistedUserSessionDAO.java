/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PersistedUserSession.PERSISTED_USER_SESSION;

@Named
@Singleton
public class PersistedUserSessionDAO
    extends AbstractOperationalSqlDAO<PersistedUserSession>
{
  @Inject
  public PersistedUserSessionDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public void deleteById(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(PERSISTED_USER_SESSION)
          .where(PERSISTED_USER_SESSION.PERSISTED_USER_SESSION_ID.eq(id))
          .execute();
      tx.commit();
    }
  }

  @Override
  protected boolean detectTestEntityLeaks() {
    // Functional tests login the user once per test class (logins are expensive),
    // so we cannot delete the PersistedUserSession after each test,
    // so we cannot detect PersistedUserSession leaks.
    return false;
  }

  @Override
  public Table<?> getJooqTable() {
    return PERSISTED_USER_SESSION;
  }

  @Override
  public Class<PersistedUserSession> getEntityClass() {
    return PersistedUserSession.class;
  }
}

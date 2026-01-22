/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityNotFoundException;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class PersistedUserSessionDAO
    extends AbstractOperationalSqlDAO<PersistedUserSession>
{
  @Inject
  public PersistedUserSessionDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void update(TransactionContext tx, PersistedUserSession persistedUserSession) {
    int rowsUpdated = createQuery("UPDATE PersistedUserSession entity SET entity.sessionJson=?2 WHERE entity.id=?1",
        persistedUserSession.getId(), persistedUserSession.getSessionJson()).executeUpdate(tx);
    if (rowsUpdated == 0) {
      throw new EntityNotFoundException();
    }
  }

  public void deleteById(String id) {
    String sQuery = "DELETE FROM PersistedUserSession entity WHERE entity.id=?1";
    createQuery(sQuery, id).executeUpdate();
  }

  @Override
  protected boolean detectTestEntityLeaks() {
    // Functional tests login the user once per test class (logins are expensive),
    // so we cannot delete the PersistedUserSession after each test,
    // so we cannot detect PersistedUserSession leaks.
    return false;
  }
}

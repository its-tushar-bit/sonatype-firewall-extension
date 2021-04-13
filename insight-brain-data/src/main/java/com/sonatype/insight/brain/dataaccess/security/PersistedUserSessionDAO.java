/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.dataaccess.TransactionContext;

public class PersistedUserSessionDAO
    extends AbstractOperationalSqlDAO<PersistedUserSession>
{
  @Override
  public PersistedUserSession getById(String id) {
    String sQuery = "SELECT entity FROM PersistedUserSession entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<PersistedUserSession> getAll() {
    String sQuery = "SELECT entity FROM PersistedUserSession entity";
    return getList(sQuery);
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
}

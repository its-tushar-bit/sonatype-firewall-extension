/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.ide;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.ide.UserIdePolicyEvaluation;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserIdePolicyEvaluationDAO extends AbstractOperationalSqlDAO<UserIdePolicyEvaluation>
{
  private static final Logger log = LoggerFactory.getLogger(UserIdePolicyEvaluationDAO.class);

  @Override
  public UserIdePolicyEvaluation getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM UserIdePolicyEvaluation entity WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM UserIdePolicyEvaluation entity";
    return getSingle(Long.class, sQuery);
  }

  public long getCountSince(Date sinceUtcDate) {
    String sQuery = "SELECT COUNT(entity) FROM UserIdePolicyEvaluation entity" +
        " WHERE entity.lastEvaluationTime >= ?1";
    return getSingle(Long.class, sQuery, sinceUtcDate);
  }

  public UserIdePolicyEvaluation getByUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      throw new DataAccessException("The username name cannot be null or empty.");
    }
    String sQuery = "SELECT entity FROM UserIdePolicyEvaluation entity WHERE entity.username=?1";
    return get(sQuery, username);
  }

  public UserIdePolicyEvaluation getByUsername(TransactionContext tx, String username) {
    if (username == null || username.trim().isEmpty()) {
      throw new DataAccessException("The username name cannot be null or empty.");
    }
    String sQuery = "SELECT entity FROM UserIdePolicyEvaluation entity WHERE entity.username=?1";
    return get(tx, sQuery, username);
  }

  public void upsert(String username) {
    UserIdePolicyEvaluation entity = getByUsername(username);
    if (entity != null) {
      entity.setLastEvaluationTime(new Date());
      update(entity);
    }
    else {
      insert(new UserIdePolicyEvaluation(username, new Date()));
    }
  }

  public void deleteByUsername(TransactionContext tx, String username) {
    UserIdePolicyEvaluation entity = getByUsername(tx, username);
    if (entity != null) {
      delete(tx, entity);
    }
  }

  public List<UserIdePolicyEvaluation> getAll() {
    String sQuery = "SELECT entity FROM UserIdePolicyEvaluation entity";
    return getList(sQuery);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.75
 */
public class UserTokenDAO
    extends AbstractOperationalSqlDAO<UserToken>
{
  @Override
  protected UserToken getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM UserToken entity WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void insert(TransactionContext tx, UserToken userToken) {
    if (userToken.getCreateTime() == null) {
      userToken.setCreateTime(new Date());
    }
    super.insert(tx, userToken);
  }

  public List<UserToken> getAllLdap() {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.realmId<>?1 AND userToken.realmId<>?2";
    return getList(sQuery, User.INTERNAL_REALM_ID, SamlUser.SAML_REALM_ID);
  }

  public UserToken getInternalByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.username=?1 AND userToken.realmId=?2";
    return get(tx, sQuery, username, User.INTERNAL_REALM_ID);
  }

  public UserToken getByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.username=?1 AND userToken.realmId=?2";
    return get(tx, sQuery, username, realmId);
  }

  public UserToken getByUsernameAndRealmId(String username, String realmId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndRealmId(tx, username, realmId);
    }
  }

  public UserToken getByUserCode(TransactionContext tx, String userCode) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.userCode=?1";
    return get(tx, sQuery, userCode);
  }

  public UserToken getByUserCode(String userCode) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUserCode(tx, userCode);
    }
  }

  private List<UserToken> getByRealmId(TransactionContext tx, String realmId) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.realmId=?1";
    return getList(tx, sQuery, realmId);
  }

  @Override
  public void update(TransactionContext tx, UserToken userToken) {
    throw new UnsupportedOperationException("The UserToken table does not support update operations.");
  }

  public void deleteByRealmId(TransactionContext tx, String realmId) {
    getByRealmId(tx, realmId).forEach(userToken -> delete(tx, userToken));
  }

  public boolean userTokenExists(String username, String realmId) {
    String sQuery = "SELECT COUNT(userToken) FROM UserToken" + //
        " userToken WHERE userToken.username=?1 AND userToken.realmId=?2";
    return getSingle(Long.class, sQuery, username, realmId) > 0;
  }

  /**
   * Both createdAfter and createdBefore can be null.
   *
   * @param createdAfter  :inclusive
   * @param createdBefore :inclusive
   */
  public List<UserToken> getByCreateDateBetween(Date createdAfter, Date createdBefore) {
    String sQuery = "SELECT userToken FROM UserToken userToken";

    if (createdAfter == null && createdBefore == null) {
      // Get all
      return getList(sQuery);
    }

    if (createdAfter != null && createdBefore != null) {
      sQuery += " WHERE userToken.createTime >= ?1 AND userToken.createTime <= ?2";
      return getList(sQuery, createdAfter, createdBefore);
    }

    if (createdBefore != null) {
      sQuery += " WHERE userToken.createTime <= ?1";
      return getList(sQuery, createdBefore);
    }

    sQuery += " WHERE userToken.createTime >= ?1";
    return getList(sQuery, createdAfter);
  }
}

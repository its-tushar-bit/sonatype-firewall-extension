/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
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

  public List<UserToken> getAllNotInternal() {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.realmId<>?1";
    return getList(sQuery, User.INTERNAL_REALM_ID);
  }

  public UserToken getInternalByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.username=?1 AND userToken.realmId=?2";
    return get(tx, sQuery, username, User.INTERNAL_REALM_ID);
  }

  public UserToken getByUsernameAndRealmId(String username, String realmId) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.username=?1 AND userToken.realmId=?2";
    return get(sQuery, username, realmId);
  }

  public UserToken getByUserCode(String userCode) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.userCode=?1";
    return get(sQuery, userCode);
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
}

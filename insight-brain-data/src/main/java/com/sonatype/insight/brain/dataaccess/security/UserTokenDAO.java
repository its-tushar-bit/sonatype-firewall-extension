/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
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

  public UserToken getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public UserToken getByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.usernameLowercase=?1";
    return get(tx, sQuery, UserToken.normalizeUsername(username));
  }

  @Override
  public void update(TransactionContext tx, UserToken userToken) {
    throw new UnsupportedOperationException("The UserToken table does not support update operations.");
  }
}

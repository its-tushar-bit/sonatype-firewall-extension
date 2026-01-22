/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.75
 */
@Named
@Singleton
public class UserTokenDAO
    extends AbstractOperationalSqlDAO<UserToken>
{
  @Inject
  public UserTokenDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
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
        " WHERE userToken.realmId<>?1 AND userToken.realmId<>?2 AND userToken.realmId<>?3";
    return getList(sQuery, User.INTERNAL_REALM_ID, SamlUser.SAML_REALM_ID, OAuth2User.OAUTH2_REALM_ID);
  }

  public UserToken getInternalByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.username=?1 AND userToken.realmId=?2";
    return get(tx, sQuery, username, User.INTERNAL_REALM_ID);
  }

  public UserToken getByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    String sQuery = "SELECT userToken FROM UserToken userToken" + //
        " WHERE userToken.realmId=?1";
    if (User.INTERNAL_REALM_ID.equals(realmId)) {
      username = User.normalizeUsername(username);
      sQuery += " AND LOWER(userToken.username)=?2";
    }
    else {
      sQuery += " AND userToken.username=?2";
    }
    return get(tx, sQuery, realmId, username);
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
    UserToken stored = getByIdNotNull(tx, userToken.getId());
    if (!userToken.getUsername().equals(stored.getUsername())
        || !userToken.getUserCode().equals(stored.getUserCode())
        || !userToken.getPassCode().equals(stored.getPassCode())
        || !userToken.getRealmId().equals(stored.getRealmId())
        || !userToken.getCreateTime().equals(stored.getCreateTime())) {
      throw new UnsupportedOperationException("Cannot update anything except last access time.");
    }
    super.update(tx, userToken);
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
   * @param realmId : realmId (e.g. {Internal|SAML|...}
   */
  public List<UserToken> getByCreateDateBetweenAndRealmId(Date createdAfter, Date createdBefore, String realmId) {
    String sQuery = "SELECT userToken FROM UserToken userToken";

    if (createdAfter == null && createdBefore == null) {
      if (realmId == null) {
        // Get all
        return getList(sQuery);
      }
      else {
        sQuery += " WHERE userToken.realmId=?1";
        return getList(sQuery, realmId);
      }
    }

    List<Object> params = constructParamsList(createdAfter, createdBefore, realmId);
    if (createdAfter != null && createdBefore != null) {
      sQuery += " WHERE userToken.createTime >= ?1 AND userToken.createTime <= ?2";
      sQuery = includeRealm(sQuery, realmId, 3);
      return getList(sQuery, params.toArray());
    }

    if (createdBefore != null) {
      sQuery += " WHERE userToken.createTime <= ?1";
      sQuery = includeRealm(sQuery, realmId, 2);
      return getList(sQuery, params.toArray());
    }

    sQuery += " WHERE userToken.createTime >= ?1";
    sQuery = includeRealm(sQuery, realmId, 2);
    return getList(sQuery, params.toArray());
  }

  private List<Object> constructParamsList(Date createdAfter, Date createdBefore, String realmId) {
    List<Object> params = new ArrayList<>();
    if (createdAfter != null) {
      params.add(createdAfter);
    }
    if (createdBefore != null) {
      params.add(createdBefore);
    }
    if (realmId != null) {
      params.add(realmId);
    }
    return params;
  }

  private String includeRealm(String sQuery, String realmId, int realmIndex) {
    if (realmId == null) {
      return sQuery;
    }
    return sQuery + String.format(" AND userToken.realmId=?%d", realmIndex);
  }
}

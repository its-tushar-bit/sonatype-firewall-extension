/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

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

import org.jooq.Condition;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.UserToken.USER_TOKEN;

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

  @Override
  public void update(TransactionContext tx, UserToken userToken) {
    UserToken stored = getByIdNotNull(tx, userToken.getId());
    if (!userToken.getUsername().equals(stored.getUsername())
        || !userToken.getUserCode().equals(stored.getUserCode())
        || !userToken.getPassCode().equals(stored.getPassCode())
        || !userToken.getRealmId().equals(stored.getRealmId())
        || !userToken.getCreateTime().equals(stored.getCreateTime()))
    {
      throw new UnsupportedOperationException("Cannot update anything except last access time.");
    }
    super.update(tx, userToken);
  }

  /**
   * Updates the passcode hash and last access time for a user token, bypassing the normal update guard.
   * Used for opportunistic rehashing of legacy token hashes to the more efficient SHA-256 format.
   */
  public void updatePassCodeAndLastAccessTime(UserToken userToken) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(USER_TOKEN)
          .set(USER_TOKEN.PASS_CODE, userToken.getPassCode())
          .set(USER_TOKEN.LAST_ACCESS_TIME, userToken.getLastAccessTime())
          .where(USER_TOKEN.USER_TOKEN_ID.eq(userToken.getId()))
          .execute();
      tx.commit();
    }
  }

  @Override
  public void delete(TransactionContext tx, UserToken userToken) {
    if (userToken == null) {
      return;
    }
    super.delete(tx, userToken);
  }

  public List<UserToken> getAllLdap() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(USER_TOKEN)
          .where(USER_TOKEN.REALM_ID.ne(User.INTERNAL_REALM_ID)
              .and(USER_TOKEN.REALM_ID.ne(SamlUser.SAML_REALM_ID))
              .and(USER_TOKEN.REALM_ID.ne(OAuth2User.OAUTH2_REALM_ID)))
          .fetchInto(UserToken.class);
    }
  }

  public UserToken getInternalByUsername(TransactionContext tx, String username) {
    return tx.dsl()
        .selectFrom(USER_TOKEN)
        .where(USER_TOKEN.USERNAME.eq(username)
            .and(USER_TOKEN.REALM_ID.eq(User.INTERNAL_REALM_ID)))
        .fetchOneInto(UserToken.class);
  }

  public UserToken getByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    Condition condition = USER_TOKEN.REALM_ID.eq(realmId);
    if (User.INTERNAL_REALM_ID.equals(realmId)) {
      username = User.normalizeUsername(username);
      condition = condition.and(DSL.lower(USER_TOKEN.USERNAME).eq(username));
    }
    else {
      condition = condition.and(USER_TOKEN.USERNAME.eq(username));
    }
    return tx.dsl()
        .selectFrom(USER_TOKEN)
        .where(condition)
        .fetchOneInto(UserToken.class);
  }

  public UserToken getByUsernameAndRealmId(String username, String realmId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndRealmId(tx, username, realmId);
    }
  }

  public UserToken getByUserCode(TransactionContext tx, String userCode) {
    return tx.dsl()
        .selectFrom(USER_TOKEN)
        .where(USER_TOKEN.USER_CODE.eq(userCode))
        .fetchOneInto(UserToken.class);
  }

  public UserToken getByUserCode(String userCode) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUserCode(tx, userCode);
    }
  }

  private List<UserToken> getByRealmId(TransactionContext tx, String realmId) {
    return tx.dsl()
        .selectFrom(USER_TOKEN)
        .where(USER_TOKEN.REALM_ID.eq(realmId))
        .fetchInto(UserToken.class);
  }

  public void deleteByRealmId(TransactionContext tx, String realmId) {
    getByRealmId(tx, realmId).forEach(userToken -> delete(tx, userToken));
  }

  public boolean userTokenExists(String username, String realmId) {
    try (TransactionContext tx = createTransactionContext()) {
      Long count = tx.dsl()
          .selectCount()
          .from(USER_TOKEN)
          .where(USER_TOKEN.USERNAME.eq(username)
              .and(USER_TOKEN.REALM_ID.eq(realmId)))
          .fetchOne(0, Long.class);
      return count != null && count > 0;
    }
  }

  /**
   * Both createdAfter and createdBefore can be null.
   *
   * @param createdAfter :inclusive
   * @param createdBefore :inclusive
   * @param realmId : realmId (e.g. {Internal|SAML|...}
   */
  public List<UserToken> getByCreateDateBetweenAndRealmId(Date createdAfter, Date createdBefore, String realmId) {
    try (TransactionContext tx = createTransactionContext()) {
      Condition condition = null;

      if (createdAfter != null) {
        condition = USER_TOKEN.CREATE_TIME.ge(createdAfter);
      }

      if (createdBefore != null) {
        Condition beforeCondition = USER_TOKEN.CREATE_TIME.le(createdBefore);
        condition = condition == null ? beforeCondition : condition.and(beforeCondition);
      }

      if (realmId != null) {
        Condition realmCondition = USER_TOKEN.REALM_ID.eq(realmId);
        condition = condition == null ? realmCondition : condition.and(realmCondition);
      }

      if (condition == null) {
        return tx.dsl()
            .selectFrom(USER_TOKEN)
            .fetchInto(UserToken.class);
      }
      else {
        return tx.dsl()
            .selectFrom(USER_TOKEN)
            .where(condition)
            .fetchInto(UserToken.class);
      }
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return USER_TOKEN;
  }

  @Override
  public Class<UserToken> getEntityClass() {
    return UserToken.class;
  }
}

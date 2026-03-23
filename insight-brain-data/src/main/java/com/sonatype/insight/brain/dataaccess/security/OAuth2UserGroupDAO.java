/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.OAuth2UserGroup;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2UserGroup.OAUTH2_USER_GROUP;

@Named
@Singleton
public class OAuth2UserGroupDAO
    extends AbstractOperationalSqlDAO<OAuth2UserGroup>
{
  public static final String SELECT_FROM_ENTITY = "SELECT entity FROM OAuth2UserGroup entity";

  public static final String DELETE_FROM_ENTITY = "DELETE FROM OAuth2UserGroup entity";

  @Inject
  public OAuth2UserGroupDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public OAuth2UserGroup getByOAuth2UserIdAndSamlGroupId(String oAuth2UserId, String oAuth2GroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOAuth2UserIdAndSamlGroupId(tx, oAuth2UserId, oAuth2GroupId);
    }
  }

  public OAuth2UserGroup getByOAuth2UserIdAndSamlGroupId(
      TransactionContext tx,
      String oAuth2UserId,
      String oAuth2GroupId)
  {
    return tx.dsl()
        .selectFrom(OAUTH2_USER_GROUP)
        .where(OAUTH2_USER_GROUP.OAUTH2_USER_ID.eq(oAuth2UserId))
        .and(OAUTH2_USER_GROUP.OAUTH2_GROUP_ID.eq(oAuth2GroupId))
        .fetchOneInto(OAuth2UserGroup.class);
  }

  public List<OAuth2UserGroup> getByOAuth2UserId(String oAuth2UserId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOAuth2UserId(tx, oAuth2UserId);
    }
  }

  public List<OAuth2UserGroup> getByOAuth2UserId(TransactionContext tx, String oAuth2UserId) {
    return tx.dsl()
        .selectFrom(OAUTH2_USER_GROUP)
        .where(OAUTH2_USER_GROUP.OAUTH2_USER_ID.eq(oAuth2UserId))
        .fetchInto(OAuth2UserGroup.class);
  }

  public List<OAuth2UserGroup> getByOAuth2GroupId(String oAuth2GroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOAuth2GroupId(tx, oAuth2GroupId);
    }
  }

  public List<OAuth2UserGroup> getByOAuth2GroupId(TransactionContext tx, String oAuth2GroupId) {
    return tx.dsl()
        .selectFrom(OAUTH2_USER_GROUP)
        .where(OAUTH2_USER_GROUP.OAUTH2_GROUP_ID.eq(oAuth2GroupId))
        .fetchInto(OAuth2UserGroup.class);
  }

  public void upsertByOAuth2UserIdAndOAuth2GroupId(OAuth2UserGroup oauthUserGroup) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      upsertByOAuth2UserIdAndOAuth2GroupId(tx, oauthUserGroup);
      tx.commit();
    }
  }

  public void upsertByOAuth2UserIdAndOAuth2GroupId(TransactionContext tx, OAuth2UserGroup oAuth2UserGroup) {
    OAuth2UserGroup stored =
        getByOAuth2UserIdAndSamlGroupId(tx, oAuth2UserGroup.getOAuth2UserId(), oAuth2UserGroup.getOAuth2GroupId());
    if (stored == null) {
      insert(tx, oAuth2UserGroup);
    }
    else {
      oAuth2UserGroup.setId(stored.getId());
      update(tx, oAuth2UserGroup);
    }
  }

  public void deleteByOAuth2UserId(String oAuth2UserId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByOAuth2UserId(tx, oAuth2UserId);
      tx.commit();
    }
  }

  public void deleteByOAuth2UserId(TransactionContext tx, String oAuth2UserId) {
    tx.dsl()
        .deleteFrom(OAUTH2_USER_GROUP)
        .where(OAUTH2_USER_GROUP.OAUTH2_USER_ID.eq(oAuth2UserId))
        .execute();
  }

  public void deleteByOAuth2GroupId(String oauthUserId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByOAuth2GroupId(tx, oauthUserId);
      tx.commit();
    }
  }

  public void deleteByOAuth2GroupId(TransactionContext tx, String oAuth2GroupId) {
    tx.dsl()
        .deleteFrom(OAUTH2_USER_GROUP)
        .where(OAUTH2_USER_GROUP.OAUTH2_GROUP_ID.eq(oAuth2GroupId))
        .execute();
  }

  public void deleteByOAuth2UserIdAndGroupIds(String oAuth2UserId, Set<String> groupIds) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByOAuth2UserIdAndGroupIds(tx, oAuth2UserId, groupIds);
      tx.commit();
    }
  }

  public void deleteByOAuth2UserIdAndGroupIds(TransactionContext tx, String oAuth2UserId, Set<String> groupIds) {
    if (CollectionUtils.isEmpty(groupIds)) {
      return;
    }
    tx.dsl()
        .deleteFrom(OAUTH2_USER_GROUP)
        .where(OAUTH2_USER_GROUP.OAUTH2_USER_ID.eq(oAuth2UserId))
        .and(OAUTH2_USER_GROUP.OAUTH2_GROUP_ID.in(groupIds))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return OAUTH2_USER_GROUP;
  }

  @Override
  public List<OAuth2UserGroup> getAll(TransactionContext tx) {
    return tx.dsl().selectFrom(OAUTH2_USER_GROUP).fetchInto(OAuth2UserGroup.class);
  }

  @Override
  public Class<OAuth2UserGroup> getEntityClass() {
    return OAuth2UserGroup.class;
  }
}

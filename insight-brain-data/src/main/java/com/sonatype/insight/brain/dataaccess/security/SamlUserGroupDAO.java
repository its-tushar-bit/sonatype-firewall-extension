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
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SamlUserGroup.SAML_USER_GROUP;

@Named
@Singleton
public class SamlUserGroupDAO
    extends AbstractOperationalSqlDAO<SamlUserGroup>
{
  @Inject
  public SamlUserGroupDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SamlUserGroup getBySamlUserIdAndSamlGroupId(String samlUserId, String samlGroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySamlUserIdAndSamlGroupId(tx, samlUserId, samlGroupId);
    }
  }

  public SamlUserGroup getBySamlUserIdAndSamlGroupId(TransactionContext tx, String samlUserId, String samlGroupId) {
    return tx.dsl()
        .selectFrom(SAML_USER_GROUP)
        .where(SAML_USER_GROUP.SAML_USER_ID.eq(samlUserId))
        .and(SAML_USER_GROUP.SAML_GROUP_ID.eq(samlGroupId))
        .fetchOneInto(SamlUserGroup.class);
  }

  public List<SamlUserGroup> getBySamlUserId(String samlUserId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySamlUserId(tx, samlUserId);
    }
  }

  public List<SamlUserGroup> getBySamlUserId(TransactionContext tx, String samlUserId) {
    return tx.dsl()
        .selectFrom(SAML_USER_GROUP)
        .where(SAML_USER_GROUP.SAML_USER_ID.eq(samlUserId))
        .fetchInto(SamlUserGroup.class);
  }

  public List<SamlUserGroup> getBySamlGroupId(String samlGroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySamlGroupId(tx, samlGroupId);
    }
  }

  public List<SamlUserGroup> getBySamlGroupId(TransactionContext tx, String samlGroupId) {
    return tx.dsl()
        .selectFrom(SAML_USER_GROUP)
        .where(SAML_USER_GROUP.SAML_GROUP_ID.eq(samlGroupId))
        .fetchInto(SamlUserGroup.class);
  }

  public void upsertBySamlUserIdAndSamlGroupId(SamlUserGroup samlUserGroup) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      upsertBySamlUserIdAndSamlGroupId(tx, samlUserGroup);
      tx.commit();
    }
  }

  public void upsertBySamlUserIdAndSamlGroupId(TransactionContext tx, SamlUserGroup samlUserGroup) {
    SamlUserGroup stored =
        getBySamlUserIdAndSamlGroupId(tx, samlUserGroup.getSamlUserId(), samlUserGroup.getSamlGroupId());
    if (stored == null) {
      insert(tx, samlUserGroup);
    }
    else {
      samlUserGroup.setId(stored.getId());
      update(tx, samlUserGroup);
    }
  }

  public void deleteBySamlUserId(String samlUserId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySamlUserId(tx, samlUserId);
      tx.commit();
    }
  }

  public void deleteBySamlUserId(TransactionContext tx, String samlUserId) {
    tx.dsl()
        .deleteFrom(SAML_USER_GROUP)
        .where(SAML_USER_GROUP.SAML_USER_ID.eq(samlUserId))
        .execute();
  }

  public void deleteBySamlGroupId(String samlGroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySamlGroupId(tx, samlGroupId);
      tx.commit();
    }
  }

  public void deleteBySamlGroupId(TransactionContext tx, String samlGroupId) {
    tx.dsl()
        .deleteFrom(SAML_USER_GROUP)
        .where(SAML_USER_GROUP.SAML_GROUP_ID.eq(samlGroupId))
        .execute();
  }

  public void deleteBySamlUserIdAndGroupIds(String samlUserId, Set<String> groupIds) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySamlUserIdAndGroupIds(tx, samlUserId, groupIds);
      tx.commit();
    }
  }

  public void deleteBySamlUserIdAndGroupIds(TransactionContext tx, String samlUserId, Set<String> groupIds) {
    if (CollectionUtils.isEmpty(groupIds)) {
      return;
    }
    tx.dsl()
        .deleteFrom(SAML_USER_GROUP)
        .where(SAML_USER_GROUP.SAML_USER_ID.eq(samlUserId))
        .and(SAML_USER_GROUP.SAML_GROUP_ID.in(groupIds))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return SAML_USER_GROUP;
  }

  @Override
  public Class<SamlUserGroup> getEntityClass() {
    return SamlUserGroup.class;
  }
}

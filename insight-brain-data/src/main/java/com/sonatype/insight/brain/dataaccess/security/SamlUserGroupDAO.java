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
    String sQuery = "SELECT entity FROM SamlUserGroup entity" + //
        " WHERE entity.samlUserId=?1" + //
        " AND entity.samlGroupId=?2";
    return get(tx, sQuery, samlUserId, samlGroupId);
  }

  public List<SamlUserGroup> getBySamlUserId(String samlUserId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySamlUserId(tx, samlUserId);
    }
  }

  public List<SamlUserGroup> getBySamlUserId(TransactionContext tx, String samlUserId) {
    String sQuery = "SELECT entity FROM SamlUserGroup entity" + //
        " WHERE entity.samlUserId=?1";
    return getList(tx, sQuery, samlUserId);
  }

  public List<SamlUserGroup> getBySamlGroupId(String samlGroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySamlGroupId(tx, samlGroupId);
    }
  }

  public List<SamlUserGroup> getBySamlGroupId(TransactionContext tx, String samlGroupId) {
    String sQuery = "SELECT entity FROM SamlUserGroup entity" + //
        " WHERE entity.samlGroupId=?1";
    return getList(tx, sQuery, samlGroupId);
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
    String sQuery = "DELETE FROM SamlUserGroup entity" + //
        " WHERE entity.samlUserId=?1";
    createQuery(sQuery, samlUserId).executeUpdate(tx);
  }

  public void deleteBySamlGroupId(String samlGroupId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySamlGroupId(tx, samlGroupId);
      tx.commit();
    }
  }

  public void deleteBySamlGroupId(TransactionContext tx, String samlGroupId) {
    String sQuery = "DELETE FROM SamlUserGroup entity" + //
        " WHERE entity.samlGroupId=?1";
    createQuery(sQuery, samlGroupId).executeUpdate(tx);
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
    String sQuery = "DELETE FROM SamlUserGroup entity" + //
        " WHERE entity.samlUserId=?1" + //
        " AND entity.samlGroupId IN ?2";
    createQuery(sQuery, samlUserId, groupIds).executeUpdate(tx);
  }
}

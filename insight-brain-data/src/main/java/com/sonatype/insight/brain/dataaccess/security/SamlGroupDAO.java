/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;

@Named
@Singleton
public class SamlGroupDAO
    extends AbstractOperationalSqlDAO<SamlGroup>
{
  private final SamlUserGroupDAO samlUserGroupDAO;

  @Inject
  public SamlGroupDAO(
      final OperationalDataStore operationalDataStore,
      final SamlUserGroupDAO samlUserGroupDAO)
  {
    super(operationalDataStore);
    this.samlUserGroupDAO = samlUserGroupDAO;
  }

  @Override
  public List<SamlGroup> getAll() {
    String sQuery = "SELECT entity FROM SamlGroup entity" + //
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  public List<SamlGroup> getByIds(Set<String> ids) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIds(tx, ids);
    }
  }

  public List<SamlGroup> getByIds(TransactionContext tx, Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    String sQuery = "SELECT entity from SamlGroup entity" + //
        " WHERE entity.id IN ?1" + //
        " ORDER BY entity.name";
    return getList(tx, sQuery, ids);
  }

  public SamlGroup getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public SamlGroup getByName(TransactionContext tx, String name) {
    String sQuery = "SELECT entity FROM SamlGroup entity" + //
        " WHERE entity.name=?1";
    return get(tx, sQuery, name);
  }

  public List<SamlGroup> getByNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Collections.emptyList();
    }
    String sQuery = "SELECT entity from SamlGroup entity" + //
        " WHERE entity.name IN ?1" + //
        " ORDER BY entity.name";
    return getList(sQuery, names);
  }

  public void upsertByName(SamlGroup samlGroup) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      upsertByName(tx, samlGroup);
      tx.commit();
    }
  }

  public void upsertByName(TransactionContext tx, SamlGroup samlGroup) {
    SamlGroup stored = getByName(tx, samlGroup.getName());
    if (stored == null) {
      insert(tx, samlGroup);
    }
    else {
      samlGroup.setId(stored.getId());
      update(tx, samlGroup);
    }
  }

  public List<SamlGroup> findGroupsByNameQuery(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = "SELECT entity FROM SamlGroup entity" + //
        " WHERE lower(entity.name) LIKE ?1" + //
        " ORDER BY entity.name";
    return getList(sQuery, nameQuery);
  }

  @Override
  public void delete(TransactionContext tx, SamlGroup entity) {
    // Cascade to saml user group mappings
    samlUserGroupDAO.deleteBySamlGroupId(tx, entity.getId());

    super.delete(tx, entity);
  }
}

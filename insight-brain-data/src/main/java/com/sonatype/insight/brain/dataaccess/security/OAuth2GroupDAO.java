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
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;

@Named
@Singleton
public class OAuth2GroupDAO
    extends AbstractOperationalSqlDAO<OAuth2Group>
{
  public static final String SELECT_FROM_ENTITY = "SELECT entity FROM OAuth2Group entity";

  public static final String ORDER_BY_NAME = " ORDER BY entity.name";

  private final OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Inject
  public OAuth2GroupDAO(
      final OperationalDataStore operationalDataStore,
      final OAuth2UserGroupDAO oAuth2UserGroupDAO)
  {
    super(operationalDataStore);
    this.oAuth2UserGroupDAO = oAuth2UserGroupDAO;
  }

  @Override
  public List<OAuth2Group> getAll() {
    String sQuery = SELECT_FROM_ENTITY + //
        ORDER_BY_NAME;
    return getList(sQuery);
  }

  public List<OAuth2Group> getByIds(Set<String> ids) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIds(tx, ids);
    }
  }

  public List<OAuth2Group> getByIds(TransactionContext tx, Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    String sQuery = SELECT_FROM_ENTITY + //
        " WHERE entity.id IN ?1" + //
        ORDER_BY_NAME;
    return getList(tx, sQuery, ids);
  }

  public OAuth2Group getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public OAuth2Group getByName(TransactionContext tx, String name) {
    String sQuery = SELECT_FROM_ENTITY + //
        " WHERE entity.name=?1";
    return get(tx, sQuery, name);
  }

  public List<OAuth2Group> getByNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Collections.emptyList();
    }
    String sQuery = SELECT_FROM_ENTITY + //
        " WHERE entity.name IN ?1" + //
        ORDER_BY_NAME;
    return getList(sQuery, names);
  }

  public void upsertByName(OAuth2Group oAuth2Group) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      upsertByName(tx, oAuth2Group);
      tx.commit();
    }
  }

  public void upsertByName(TransactionContext tx, OAuth2Group oAuth2Group) {
    OAuth2Group stored = getByName(tx, oAuth2Group.getName());
    if (stored == null) {
      insert(tx, oAuth2Group);
    }
    else {
      oAuth2Group.setId(stored.getId());
      update(tx, oAuth2Group);
    }
  }

  public List<OAuth2Group> findGroupsByNameQuery(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = SELECT_FROM_ENTITY + //
        " WHERE lower(entity.name) LIKE ?1" + //
        ORDER_BY_NAME;
    return getList(sQuery, nameQuery);
  }

  @Override
  public void delete(TransactionContext tx, OAuth2Group entity) {
    // Cascade to oauth2 user group mappings
    oAuth2UserGroupDAO.deleteByOAuth2GroupId(tx, entity.getId());

    super.delete(tx, entity);
  }
}

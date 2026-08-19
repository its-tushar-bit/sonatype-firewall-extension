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
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2Group.OAUTH2_GROUP;

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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_GROUP)
          .orderBy(OAUTH2_GROUP.NAME)
          .fetchInto(OAuth2Group.class);
    }
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
    return tx.dsl()
        .selectFrom(OAUTH2_GROUP)
        .where(OAUTH2_GROUP.OAUTH2_GROUP_ID.in(ids))
        .orderBy(OAUTH2_GROUP.NAME)
        .fetchInto(OAuth2Group.class);
  }

  public OAuth2Group getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public OAuth2Group getByName(TransactionContext tx, String name) {
    return tx.dsl()
        .selectFrom(OAUTH2_GROUP)
        .where(OAUTH2_GROUP.NAME.eq(name))
        .fetchOneInto(OAuth2Group.class);
  }

  public List<OAuth2Group> getByNames(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_GROUP)
          .where(OAUTH2_GROUP.NAME.in(names))
          .orderBy(OAUTH2_GROUP.NAME)
          .fetchInto(OAuth2Group.class);
    }
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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(OAUTH2_GROUP)
          .where(DSL.lower(OAUTH2_GROUP.NAME).like(nameQuery))
          .orderBy(OAUTH2_GROUP.NAME)
          .fetchInto(OAuth2Group.class);
    }
  }

  @Override
  public void delete(TransactionContext tx, OAuth2Group entity) {
    // Cascade to oauth2 user group mappings
    oAuth2UserGroupDAO.deleteByOAuth2GroupId(tx, entity.getId());

    super.delete(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return OAUTH2_GROUP;
  }

  @Override
  public List<OAuth2Group> getAll(TransactionContext tx) {
    return tx.dsl().selectFrom(OAUTH2_GROUP).fetchInto(OAuth2Group.class);
  }

  @Override
  public Class<OAuth2Group> getEntityClass() {
    return OAuth2Group.class;
  }
}

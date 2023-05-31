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

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections.CollectionUtils;

/**
 * @since 1.133
 */
public class SamlUserDAO
    extends AbstractOperationalSqlDAO<SamlUser>
{
  @Override
  protected SamlUser getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM SamlUser entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<SamlUser> getByIds(Set<String> ids) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIds(tx, ids);
    }
  }

  public List<SamlUser> getByIds(TransactionContext tx, Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    String sQuery = "SELECT entity from SamlUser entity" + //
        " WHERE entity.id IN ?1" + //
        " ORDER BY entity.username";
    return getList(tx, sQuery, ids);
  }

  public List<SamlUser> getByUsernames(Set<String> usernames) {
    if (CollectionUtils.isEmpty(usernames)) {
      return Collections.emptyList();
    }
    String sQuery = "SELECT entity from SamlUser entity" + //
        " WHERE entity.username IN ?1" + //
        " ORDER BY entity.username";
    return getList(sQuery, usernames);
  }

  public List<SamlUser> findUsersByNameOrUsernameQuery(String nameQuery) {
    nameQuery = nameQuery.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = "SELECT entity FROM SamlUser entity" +
        " WHERE lower(concat(coalesce(entity.firstName,''), ' ', coalesce(entity.lastName,''))) LIKE ?1" +
        " OR lower(entity.username) LIKE ?1" +
        " ORDER BY entity.username";
    return getList(sQuery, nameQuery);
  }

  public SamlUser getByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT entity FROM SamlUser entity" + //
        " WHERE entity.username=?1";
    return get(tx, sQuery, username);
  }

  public SamlUser getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public SamlUser getByUsernameNotNull(String username) {
    SamlUser samlUser = getByUsername(username);
    if (samlUser == null) {
      throw new NotFoundException("Cannot find a SAML user with username " + username + ".");
    }
    return samlUser;
  }

  public void upsertByUsername(TransactionContext tx, SamlUser samlUser) {
    SamlUser stored = getByUsername(tx, samlUser.getUsername());
    if (stored == null) {
      insert(tx, samlUser);
    }
    else {
      samlUser.setId(stored.getId());
      update(tx, samlUser);
    }
  }

  public void upsertByUsername(SamlUser samlUser) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      upsertByUsername(tx, samlUser);
      tx.commit();
    }
  }

  @Override
  public void delete(TransactionContext tx, SamlUser entity) {
    // Cascade to user token
    UserTokenDAO userTokenDAO = new UserTokenDAO();
    UserToken userToken = userTokenDAO.getByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);
    if (userToken != null) {
      userTokenDAO.delete(tx, userToken);
    }

    // Cascade to dashboard filters
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    dashboardFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);

    // Cascade to user filters
    UserFilterDAO userFilterDAO = new UserFilterDAO();
    userFilterDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);

    // Cascade to user viewed product notifications
    UserViewedProductNotificationDAO userViewedProductNotificationDAO = new UserViewedProductNotificationDAO();
    userViewedProductNotificationDAO.deleteByUsernameAndRealmId(tx, entity.getUsername(), SamlUser.SAML_REALM_ID);

    // Cascade to saml user group mappings
    SamlUserGroupDAO samlUserGroupDAO = new SamlUserGroupDAO();
    samlUserGroupDAO.deleteBySamlUserId(tx, entity.getId());

    super.delete(tx, entity);
  }

  public List<SamlUser> getAll() {
    String sQuery = "SELECT entity FROM SamlUser entity" + //
        " ORDER BY entity.username";
    return getList(sQuery);
  }
}

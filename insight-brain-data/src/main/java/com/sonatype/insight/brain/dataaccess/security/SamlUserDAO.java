/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

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

    super.delete(tx, entity);
  }

  public List<SamlUser> getAll() {
    String sQuery = "SELECT entity FROM SamlUser entity" + //
        " ORDER BY entity.username";
    return getList(sQuery);
  }
}

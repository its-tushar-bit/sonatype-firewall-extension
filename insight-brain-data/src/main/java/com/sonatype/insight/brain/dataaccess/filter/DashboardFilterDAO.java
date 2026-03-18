/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.11.0
 */
@Named
@Singleton
public class DashboardFilterDAO
    extends AbstractOperationalSqlDAO<DashboardFilter>
{
  private static final Logger log = LoggerFactory.getLogger(DashboardFilterDAO.class);

  @Inject
  public DashboardFilterDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<DashboardFilter> getByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    username = User.normalizeUsername(username);
    String sQuery = "SELECT entity FROM DashboardFilter entity" + //
        " WHERE entity.usernameLowercase=?1 AND entity.realmId=?2" + //
        " ORDER BY entity.name";
    return getList(tx, sQuery, username, realmId);
  }

  public List<DashboardFilter> getByUsernameAndRealmId(String username, String realmId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndRealmId(tx, username, realmId);
    }
  }

  public DashboardFilter getByUsernameAndRealmIdAndName(String username, String realmId, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndRealmIdAndName(tx, username, realmId, name);
    }
  }

  private DashboardFilter getByUsernameAndRealmIdAndName(
      TransactionContext tx,
      String username,
      String realmId,
      String name)
  {
    name = NameHelper.normalize(name);
    username = User.normalizeUsername(username);
    String sQuery = "SELECT entity FROM DashboardFilter entity" + //
        " WHERE entity.usernameLowercase=?1 AND entity.realmId=?2 AND entity.nameLowercaseNoWhitespace=?3";
    return get(tx, sQuery, username, realmId, name);
  }

  public DashboardFilter getLegacyByUsernameAndName(String username, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLegacyByUsernameAndName(tx, username, name);
    }
  }

  /**
   * Before Insight Brain 1.76, dashboard filters stored the username as the user entered it at login time,
   * not as it is stored in the authentication realm.
   * This means there may be multiple filters with the same name and same case insensitive username.
   *
   * This method tries first to find a match by username case sensitive, then by username case insensitive.
   * In both cases, if there are multiple filters, then this method will return only one of those filters.
   */
  public DashboardFilter getLegacyByUsernameAndName(TransactionContext tx, String username, String name) {
    name = NameHelper.normalize(name);
    // Try to find a filter that matches the username case sensitive.
    String sQuery = "SELECT entity FROM DashboardFilter entity" + //
        " WHERE entity.username=?1 AND entity.realmId IS NULL AND entity.nameLowercaseNoWhitespace=?2";
    List<DashboardFilter> dashboardFilters = getList(tx, sQuery, username, name);
    if (dashboardFilters.isEmpty()) {
      // No filter matches the username case sensitive. Try case-insensitive.
      username = User.normalizeUsername(username);
      sQuery = "SELECT entity FROM DashboardFilter entity" + //
          " WHERE entity.usernameLowercase=?1 AND entity.realmId IS NULL AND entity.nameLowercaseNoWhitespace=?2";
      dashboardFilters = getList(tx, sQuery, username, name);
    }
    if (dashboardFilters.isEmpty()) {
      return null;
    }
    return dashboardFilters.get(0);
  }

  public List<DashboardFilter> getNamedFiltersByUsernameAndRealmId(String username, String realmId) {
    username = User.normalizeUsername(username);
    String sQuery = "SELECT entity FROM DashboardFilter entity" + //
        " WHERE entity.usernameLowercase=?1 AND entity.realmId=?2 AND entity.nameLowercaseNoWhitespace <> ''" + //
        " ORDER BY entity.name";
    return getList(sQuery, username, realmId);
  }

  public List<DashboardFilter> getLegacyNamedFiltersByUsername(String username) {
    return getNamedFiltersByUsernameAndRealmId(username, null /* realmId */);
  }

  @Override
  public void insert(TransactionContext tx, DashboardFilter dashboardFilter) {
    validate(tx, dashboardFilter);
    DashboardFilter existingFilter = getByUsernameAndRealmIdAndName(tx, dashboardFilter.getUsername(),
        dashboardFilter.getRealmId(), dashboardFilter.getName());
    if (existingFilter == null) {
      existingFilter = getLegacyByUsernameAndName(tx, dashboardFilter.getUsername(), dashboardFilter.getName());
    }
    if (existingFilter != null) {
      throw new InvalidNameException(dashboardFilter.getName() + " is already used as a name.");
    }
    super.insert(tx, dashboardFilter);
  }

  @Override
  public void update(TransactionContext tx, DashboardFilter dashboardFilter) {
    validate(tx, dashboardFilter);
    DashboardFilter existingFilter = getByUsernameAndRealmIdAndName(tx, dashboardFilter.getUsername(),
        dashboardFilter.getRealmId(), dashboardFilter.getName());
    if (existingFilter == null) {
      existingFilter = getLegacyByUsernameAndName(tx, dashboardFilter.getUsername(), dashboardFilter.getName());
    }
    if (existingFilter != null && !existingFilter.getId().equals(dashboardFilter.getId())) {
      throw new InvalidNameException(dashboardFilter.getName() + " is already used as a name.");
    }
    super.update(tx, dashboardFilter);
  }

  private void validate(TransactionContext tx, DashboardFilter dashboardFilter) {
    if (StringUtils.isBlank(dashboardFilter.getRealmId())) {
      throw new BadRequestException("The realm ID is required.");
    }

    final String name = dashboardFilter.getName();
    if (!"".equals(name)) {
      NameHelper.validate(name);
      if (dashboardFilter.getBasedOnFilterName() != null) {
        throw new BadRequestException("Only the active filter can be based on another filter.");
      }
    }
    else if (dashboardFilter.getBasedOnFilterName() != null) {
      DashboardFilter basedOnFilter = getByUsernameAndRealmIdAndName(tx, dashboardFilter.getUsername(),
          dashboardFilter.getRealmId(), dashboardFilter.getBasedOnFilterName());
      if (basedOnFilter == null) {
        basedOnFilter =
            getLegacyByUsernameAndName(tx, dashboardFilter.getUsername(), dashboardFilter.getBasedOnFilterName());
        if (basedOnFilter != null) {
          basedOnFilter.setRealmId(dashboardFilter.getRealmId());
          update(tx, basedOnFilter);
        }
        else {
          log.debug("Attempted to persist active filter based on non-existing saved filter named {}.",
              dashboardFilter.getBasedOnFilterName());
          dashboardFilter.setBasedOnFilterName(null);
        }
      }
    }
  }

  public void deleteByUsernameAndRealmId(String username, String realmId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByUsernameAndRealmId(tx, username, realmId);
      tx.commit();
    }
  }

  public void deleteByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    List<DashboardFilter> dashboardFilters = getByUsernameAndRealmId(tx, username, realmId);
    for (DashboardFilter dashboardFilter : dashboardFilters) {
      delete(tx, dashboardFilter);
    }
  }

  public void deleteLegacyByUsername(TransactionContext tx, String username) {
    deleteByUsernameAndRealmId(tx, username, null /* realmId */);
  }

  private List<DashboardFilter> getByRealmId(TransactionContext tx, String realmId) {
    String sQuery = "SELECT entity FROM DashboardFilter entity" + //
        " WHERE entity.realmId=?1";
    return getList(tx, sQuery, realmId);
  }

  public void deleteByRealmId(TransactionContext tx, String realmId) {
    getByRealmId(tx, realmId).forEach(dashboardFilter -> delete(tx, dashboardFilter));
  }
}

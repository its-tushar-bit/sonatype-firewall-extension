/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.11.0
 */
public class DashboardFilterDAO
    extends AbstractOperationalSqlDAO<DashboardFilter>
{
  @Override
  public DashboardFilter getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM DashboardFilter entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<DashboardFilter> getByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT entity FROM DashboardFilter entity WHERE entity.username=?1" + //
        " ORDER BY entity.name";
    return getList(tx, sQuery, username);
  }

  public List<DashboardFilter> getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public DashboardFilter getByUsernameAndName(String username, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndName(tx, username, name);
    }
  }

  public DashboardFilter getByUsernameAndName(TransactionContext tx, String username, String name) {
    // Dashboard filter name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM DashboardFilter entity WHERE entity.username=?1 AND" +
        " entity.nameLowercaseNoWhitespace=?2";
    return get(tx, sQuery, username, name);
  }

  public List<DashboardFilter> getNamedFiltersByUsername(String username) {
    String sQuery = "SELECT entity FROM DashboardFilter entity WHERE entity.username=?1 AND" +
        " entity.nameLowercaseNoWhitespace <> ''" +
        " ORDER BY entity.name";
    return getList(sQuery, username);
  }

  public List<DashboardFilter> getAll() {
    String sQuery = "SELECT entity FROM DashboardFilter entity";
    return getList(sQuery);
  }

  @Override
  public void insert(TransactionContext tx, DashboardFilter dashboardFilter) {
    validateFilterName(dashboardFilter);
    if (getByUsernameAndName(tx, dashboardFilter.getUsername(), dashboardFilter.getName()) != null) {
      throw new BadRequestException(dashboardFilter.getName() + " is already used as a name.");
    }
    super.insert(tx, dashboardFilter);
  }

  @Override
  public void update(TransactionContext tx, DashboardFilter dashboardFilter) {
    validateFilterName(dashboardFilter);
    DashboardFilter existingFilter = getByUsernameAndName(tx, dashboardFilter.getUsername(), dashboardFilter.getName());
    if (existingFilter != null && !existingFilter.getId().equals(dashboardFilter.getId())) {
      throw new BadRequestException(dashboardFilter.getName() + " is already used as a name.");
    }
    super.update(tx, dashboardFilter);
  }

  private void validateFilterName(final DashboardFilter dashboardFilter) {
    final String name = dashboardFilter.getName();
    if (!"".equals(name)) {
      NameHelper.validate(name);
    }
  }
}

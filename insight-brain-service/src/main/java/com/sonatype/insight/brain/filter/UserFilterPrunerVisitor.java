/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.filter.UserFilterType.UserFilterVisitor;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.collections.CollectionUtils;

/**
 * This visitor allow for pruning of unauthorized or undesired values of the {@link
 * com.sonatype.insight.brain.model.filter.UserFilter}'s filter value.
 */
@Named
public class UserFilterPrunerVisitor
    implements UserFilterVisitor
{
  private ApplicationDAO applicationDAO;

  @Inject
  public UserFilterPrunerVisitor(ApplicationDAO applicationDAO) {
    this.applicationDAO = applicationDAO;
  }

  public String process(UserFilterType type, final String json) {
    return type.accept(this, json);
  }

  @Override
  public String filterAdvancedLegalPack(final String json) {
    if (json == null) {
      return null;
    }
    try {
      AdvancedLegalPackDashboardFilter filter = JsonUtils.parse(json, AdvancedLegalPackDashboardFilter.class);
      pruneUnauthorizedApplicationIds(filter);
      return JsonUtils.format(filter);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void pruneUnauthorizedApplicationIds(AdvancedLegalPackDashboardFilter filter) {
    if (filter == null || CollectionUtils.isEmpty(filter.getApplicationFilters())) {
      return;
    }
    List<Application> apps = getApplicationsByIds(filter.getApplicationFilters());
    filter.getApplicationFilters().clear();
    for (Application app : apps) {
      filter.getApplicationFilters().add(app.getId());
    }
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsByIds(final List<String> applicationIds) {
    return applicationDAO.getByIds(new LinkedHashSet<>(applicationIds));
  }
}

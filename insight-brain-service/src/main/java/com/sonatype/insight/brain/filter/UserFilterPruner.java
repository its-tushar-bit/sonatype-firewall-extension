/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.util.LinkedHashSet;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;

import org.apache.commons.collections4.CollectionUtils;

import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;

/**
 * Allow for pruning of unauthorized or undesired values of the {@link
 * com.sonatype.insight.brain.model.filter.UserFilter}'s filter value.
 */
@Named
public class UserFilterPruner
{
  private ApplicationDAO applicationDAO;

  @Inject
  public UserFilterPruner(ApplicationDAO applicationDAO) {
    this.applicationDAO = applicationDAO;
  }

  public void process(UserFilterDTO userFilterDTO) {
    if (userFilterDTO.getType().equals(ADVANCED_LEGAL_PACK_DASHBOARD)) {
      filterAdvancedLegalPack(userFilterDTO);
    }
  }

  public void filterAdvancedLegalPack(UserFilterDTO userFilterDTO) {
    pruneUnauthorizedApplicationIds((AdvancedLegalPackDashboardFilter) userFilterDTO.getFilter());
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

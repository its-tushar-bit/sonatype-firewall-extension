/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class DashboardFilterServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private DashboardFilterService dashboardFilterService;

  @Test
  public void testGetNamedDashboardFiltersForCurrentUser_UnauthorizedApps() throws Exception {
    Application app2 = tempEntity.newApplication(org.getId());
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO("Abcd", app2.getId(), app.getId());
    login();

    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(namedDashboardFilterDTO);

    grantReadPermission(app.getId());
    List<NamedDashboardFilterDTO> actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual.get(0).filter.applicationFilters, not(contains(app2.getId())));
    assertThat(actual.get(0).filter.applicationFilters, contains(app.getId()));

    grantReadPermission(app2.getId());
    actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual.get(0).filter.applicationFilters, containsInAnyOrder(app.getId(), app2.getId()));
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_UnauthorizedApps() throws Exception {
    Application app2 = tempEntity.newApplication(org.getId());
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO("", app2.getId(), app.getId());

    login();

    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(namedDashboardFilterDTO);

    grantReadPermission(app.getId());
    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual.filter.applicationFilters, contains(app.getId()));
    assertThat(actual.filter.applicationFilters, not(contains(app2.getId())));

    grantReadPermission(app2.getId());
    actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual.filter.applicationFilters, containsInAnyOrder(app.getId(), app2.getId()));
  }

  private NamedDashboardFilterDTO createNamedDashboardFilterDTO(final String filterName, String... applicationIDs) {
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    DashboardFilterDTO dto = new DashboardFilterDTO();
    dto.applicationFilters = new ArrayList<>();
    for (String applicationId : applicationIDs) {
      dto.applicationFilters.add(applicationId);
    }
    namedDashboardFilterDTO.filter = dto;
    namedDashboardFilterDTO.name = filterName;
    return namedDashboardFilterDTO;
  }
}

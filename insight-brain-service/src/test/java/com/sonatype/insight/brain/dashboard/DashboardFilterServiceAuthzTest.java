/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
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
    assertThat(actual.get(0).filter.applicationFilters).doesNotContain(app2.getId())
        .containsExactlyInAnyOrder(app.getId());

    grantReadPermission(app2.getId());
    actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual.get(0).filter.applicationFilters).containsExactlyInAnyOrder(app.getId(), app2.getId());
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_UnauthorizedApps() throws Exception {
    Application app2 = tempEntity.newApplication(org.getId());
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO("", app2.getId(), app.getId());

    login();

    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(namedDashboardFilterDTO);

    grantReadPermission(app.getId());
    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual.filter.applicationFilters).containsExactlyInAnyOrder(app.getId()).doesNotContain(app2.getId());

    grantReadPermission(app2.getId());
    actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual.filter.applicationFilters).containsExactlyInAnyOrder(app.getId(), app2.getId());
  }

  private NamedDashboardFilterDTO createNamedDashboardFilterDTO(final String filterName, String... applicationIDs) {
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    DashboardFilterDTO dto = new DashboardFilterDTO();
    dto.applicationFilters = new ArrayList<>();
    dto.applicationFilters.addAll(Arrays.asList(applicationIDs));
    namedDashboardFilterDTO.filter = dto;
    namedDashboardFilterDTO.name = filterName;
    return namedDashboardFilterDTO;
  }
}

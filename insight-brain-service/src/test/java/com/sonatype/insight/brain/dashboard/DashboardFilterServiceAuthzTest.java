/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class DashboardFilterServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private DashboardFilterService dashboardFilterService;

  @Test
  public void testGetFilters_UnauthorizedApps() throws Exception {
    Application app2 = tempEntity.newApplication(org.getId());

    DashboardFilterDTO dto = new DashboardFilterDTO();
    dto.applicationFilters = new ArrayList<>();
    dto.applicationFilters.add(app.getId());
    dto.applicationFilters.add(app2.getId());

    login();

    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto);

    grantReadPermission(app.getId());
    dto = dashboardFilterService.getDashboardFilterForCurrentUser();
    assertThat(dto.applicationFilters, not(contains(app2.getId())));

    grantReadPermission(app2.getId());
    dto = dashboardFilterService.getDashboardFilterForCurrentUser();
    assertThat(dto.applicationFilters, containsInAnyOrder(app.getId(), app2.getId()));
  }

  @Test
  public void testGetFilterSummary_ExplicitApplicationFilter_Unauthenticated() {
    assertThat(getFilterSummaryTotalApps(false), is(0));
  }

  @Test
  public void testGetFilterSummary_ExplicitApplicationFilter_Unauthorized() {
    login();
    assertThat(getFilterSummaryTotalApps(false), is(0));
  }

  @Test
  public void testGetFilterSummary_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    assertThat(getFilterSummaryTotalApps(false), is(1));
  }

  @Test
  public void testGetFilterSummary_ImplicitApplicationFilter_Unauthenticated() {
    assertThat(getFilterSummaryTotalApps(true), is(0));
  }

  @Test
  public void testGetFilterSummary_ImplicitApplicationFilter_Unauthorized() {
    login();
    assertThat(getFilterSummaryTotalApps(true), is(0));
  }

  @Test
  public void testGetFilterSummary_ImplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    assertThat(getFilterSummaryTotalApps(true), is(1));
  }

  private int getFilterSummaryTotalApps(boolean all) {
    return dashboardFilterService.getFilterSummary(all ? null : Collections.singleton(app.getId()), null, null, null,
        null).totalApplications;
  }
}

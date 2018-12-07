/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.DashboardFilterService.ACTIVE_FILTER_NAME;

public class DashboardResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testUpdateDashboardFilterForCurrentUser() throws Exception {
    NamedDashboardFilterDTO dashboardFilter = createNamedDashboardFilter("a-name");
    dashboardRequest().path(DashboardResource.FILTERS_PATH).body(dashboardFilter).put();

    assertDashboardFilterAudit(ACTIVE_FILTER_NAME);
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser() throws Exception {
    NamedDashboardFilterDTO dashboardFilter = createNamedDashboardFilter("filter-name");
    dashboardRequest().path(DashboardResource.NAMED_FILTERS_PATH).body(dashboardFilter).put();

    assertDashboardFilterAudit(dashboardFilter.name);
  }

  private void assertDashboardFilterAudit(final String filterName) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SAVE_DASHBOARD_FILTER, null);
    DashboardFilter persistedFilter = new DashboardFilterDAO().getByUsernameAndName("admin", filterName);
    assertCustomData(auditDTO, "filterId", persistedFilter.getId());
    assertCustomData(auditDTO, "filterName", filterName.equals(ACTIVE_FILTER_NAME) ? "(active)" : filterName);
  }

  private NamedDashboardFilterDTO createNamedDashboardFilter(String filterName) {
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = filterName;
    DashboardFilterDTO dashboardFilterDTO = new DashboardFilterDTO();
    dashboardFilterDTO.minPolicyThreatLevel = 1;
    dashboardFilterDTO.maxPolicyThreatLevel = 10;
    namedDashboardFilterDTO.filter = dashboardFilterDTO;
    return namedDashboardFilterDTO;
  }

  private HttpRequest dashboardRequest() {
    return restRequest().path(DashboardResource.RESOURCE_PATH);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.After;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.DashboardFilterService.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.security.User.ADMIN_USERNAME;
import static java.util.Arrays.asList;

public class DashboardResourceAuditTest
    extends AbstractAuditTest
{
  private DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

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

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName() throws Exception {
    String filterName = "some-filter";
    DashboardFilter filter = tempEntity
        .newDashboardFilter(ADMIN_USERNAME, filterName, JsonUtils.format(createNamedDashboardFilter(filterName)));
    DashboardFilter activeFilter = tempEntity.newDashboardFilter(ADMIN_USERNAME, ACTIVE_FILTER_NAME,
        JsonUtils.format(createNamedDashboardFilter(ACTIVE_FILTER_NAME)));

    dashboardRequest().path(DashboardResource.DELETE_NAMED_FILTERS_PATH).body(asList(filterName, ACTIVE_FILTER_NAME))
        .post();

    List<AuditDTO> auditDTOS = assertAuditLogs(AuditEvent.DELETE_DASHBOARD_FILTER, 2, null);
    assertDashboardFilterAuditData(filter, auditDTOS.get(0));
    assertDashboardFilterAuditData(activeFilter, auditDTOS.get(1));
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_NotFound() throws Exception {
    String filterName = "non-existent-filter";
    dashboardRequest().path(DashboardResource.DELETE_NAMED_FILTERS_PATH).body(asList(filterName))
        .post();

    assertAuditLog(AuditEvent.DELETE_DASHBOARD_FILTER, "not-found");
  }

  @After
  public void after() {
    //required in order to avoid clashes between create/delete tests
    dashboardFilterDAO.getByUsername(ADMIN_USERNAME).forEach(dashboardFilterDAO::delete);
  }

  private void assertDashboardFilterAudit(final String filterName) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SAVE_DASHBOARD_FILTER, null);
    DashboardFilter persistedFilter = dashboardFilterDAO.getByUsernameAndName("admin", filterName);
    assertDashboardFilterAuditData(persistedFilter, auditDTO);
  }

  private void assertDashboardFilterAuditData(final DashboardFilter filter, final AuditDTO auditDTO) {
    assertCustomData(auditDTO, "filterId", filter.getId());
    assertCustomData(auditDTO, "filterName",
        ACTIVE_FILTER_NAME.equals(filter.getName()) ? "(active)" : filter.getName());
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

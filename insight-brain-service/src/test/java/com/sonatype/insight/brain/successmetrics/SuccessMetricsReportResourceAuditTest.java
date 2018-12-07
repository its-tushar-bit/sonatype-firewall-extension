/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

public class SuccessMetricsReportResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testGetChartData() throws Exception {
    testViewSuccessMetricsData(SuccessMetricsReportResource.CHART_DATA_PATH);
  }

  @Test
  public void testGetChartData_Unauthorized() throws Exception {
    testViewSuccessMetricsData_Unauthorized(SuccessMetricsReportResource.CHART_DATA_PATH);
  }

  @Test
  public void testGetComponentCounts() throws Exception {
    testViewSuccessMetricsData(SuccessMetricsReportResource.COMPONENT_COUNTS_PATH);
  }

  @Test
  public void testGetComponentCounts_Unauthorized() throws Exception {
    testViewSuccessMetricsData_Unauthorized(SuccessMetricsReportResource.COMPONENT_COUNTS_PATH);
  }

  private void testViewSuccessMetricsData_Unauthorized(final String resourceSubpath) throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    SuccessMetricsReport report = createSuccessMetricsReport(null, Collections.singleton(app.getId()),
        unauthorizedUser.getUsername());

    successMetricsReportRequest().path(resourceSubpath).with(unauthorizedUser())
        .parameter(report.getId()).get();

    // user allowed to access resource but no apps included in report
    assertViewSuccessMetricsReport(report, 0, unauthorizedUser.getUsername());
  }

  private void testViewSuccessMetricsData(final String resourceSubpath) throws Exception {
    Organization org1 = tempEntity.newOrganization();
    tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplicationWithParent();
    SuccessMetricsReport report = createSuccessMetricsReport(Collections.singleton(org1.getId()),
        Collections.singleton(app2.getId()), null);

    successMetricsReportRequest().path(resourceSubpath).parameter(report.getId()).get();

    assertViewSuccessMetricsReport(report, 2, "admin");
  }

  private void assertViewSuccessMetricsReport(final SuccessMetricsReport report,
                                              final int includedApplicationCount,
                                              final String username)
  {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SUCCESS_METRICS_REPORT, null, username);
    assertCustomData(auditDTO, "reportId", report.getId());
    assertCustomData(auditDTO, "reportName", report.getName());
    assertCustomData(auditDTO, "inspectedApplicationCount", includedApplicationCount);
  }

  private HttpRequest successMetricsReportRequest() {
    return restRequest().path(SuccessMetricsReportResource.RESOURCE_PATH);
  }

  private SuccessMetricsReport createSuccessMetricsReport(final Set<String> organizationIds,
                                                          final Set<String> applicationIds,
                                                          final String username)
  {
    SuccessMetricsReportScopeDTO scope = new SuccessMetricsReportScopeDTO();
    scope.organizationIds = organizationIds;
    scope.applicationIds = applicationIds;

    return tempEntity
        .newSuccessMetricsReport(username == null ? getUsername() : username, "report", JsonUtils.format(scope));
  }
}

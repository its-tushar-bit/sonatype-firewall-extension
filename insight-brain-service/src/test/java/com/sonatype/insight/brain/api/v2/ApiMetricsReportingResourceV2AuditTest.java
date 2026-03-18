/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.audit.ApplicationAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.OrganizationAuditDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.joda.time.LocalDate;
import org.junit.Test;

import static java.util.Arrays.asList;

@Category(SlowTest.class)
public class ApiMetricsReportingResourceV2AuditTest
    extends AbstractAuditTest
{
  private static final LocalDate BEGIN_DATE = new LocalDate(2017, 11, 1);

  @Test
  public void testGetMetrics_Json() throws Exception {
    testExportSuccessMetricsForContentType("application/json");
  }

  @Test
  public void testGetMetrics_Csv() throws Exception {
    testExportSuccessMetricsForContentType("text/csv");
  }

  private void testExportSuccessMetricsForContentType(final String contentType) throws Exception {
    Organization org1 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    LocalDate today = new LocalDate();

    String unknownOrganizationId = "unknownOrganizationId";
    Set<String> queryOrdIds = new HashSet<>(asList(org1.getId(), unknownOrganizationId));
    String unknownApplicationId = "unknownApplicationId";
    Set<String> queryAppIds = new HashSet<>(asList(app1.getId(), app2.getId(), unknownApplicationId));

    metricsReportRequest(contentType).body(makeQueryDTO(queryOrdIds, queryAppIds)).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_SUCCESS_METRICS, null);
    assertCustomData(auditDTO, "beginDate", BEGIN_DATE.toString());
    assertCustomData(auditDTO, "endDate", today.toString());
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(unknownOrganizationId, null),
        new OrganizationAuditDTO(org1.getId(), org1));
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(unknownApplicationId, null),
        new ApplicationAuditDTO(app2.getId(), app2));
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
  }

  private HttpRequest metricsReportRequest(String acceptType) {
    return restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiMetricsReportingResourceV2.PATH)
        .header("Accept", acceptType);
  }

  private ApiMetricsReportingQueryDTOV2 makeQueryDTO(Set<String> orgIds, Set<String> appIds) {
    return new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, BEGIN_DATE.toString("yyyy-MM"), null, appIds, orgIds);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.ApplicationAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.OrganizationAuditDTO;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class SuccessMetricsReportResourceAuditTest
    extends AbstractAuditTest
{
  private SuccessMetricsReportDAO successMetricsReportDAO;

  private static final String METRICS_NAME = "metricsName";

  @Before
  public void setUp() {
    successMetricsReportDAO = lookup(SuccessMetricsReportDAO.class);
  }

  @After
  public void cleanup() {
    successMetricsReportDAO.getByUsername("admin").forEach(successMetricsReportDAO::delete);
  }

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

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    SuccessMetricsReportScopeDTO successMetricsScopeDTO = new SuccessMetricsReportScopeDTO(toIds(app1, app2),
        toIds(org1, org2));
    SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO(METRICS_NAME, successMetricsScopeDTO);
    HttpResponse response = successMetricsReportRequest().body(successMetricsDTO).post();
    SuccessMetricsReportDTO result = response.getBody(SuccessMetricsReportDTO.class);

    AuditDTO auditDTO = assertReportData(AuditEvent.CREATE_SUCCESS_METRICS_REPORT, result.id, result.name);
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(app1.getId(), app1),
        new ApplicationAuditDTO(app2.getId(), app2));
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(org1.getId(), org1),
        new OrganizationAuditDTO(org2.getId(), org2));
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser_SelectedAppAndParentOrg() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    SuccessMetricsReportScopeDTO successMetricsScopeDTO = new SuccessMetricsReportScopeDTO(toIds(app), toIds(org));
    SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO(METRICS_NAME, successMetricsScopeDTO);
    HttpResponse response = successMetricsReportRequest().body(successMetricsDTO).post();
    SuccessMetricsReportDTO result = response.getBody(SuccessMetricsReportDTO.class);

    AuditDTO auditDTO = assertReportData(AuditEvent.CREATE_SUCCESS_METRICS_REPORT, result.id, result.name);
    assertSelectedApplications(auditDTO);
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(org.getId(), org));
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser_SelectedAppDoesNotExist() throws Exception {
    Organization org = tempEntity.newOrganization();
    String appId = "appId";
    SuccessMetricsReportScopeDTO successMetricsScopeDTO = new SuccessMetricsReportScopeDTO(
        new HashSet<>(Collections.singletonList(appId)), toIds(org));
    SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO(METRICS_NAME, successMetricsScopeDTO);
    HttpResponse response = successMetricsReportRequest().body(successMetricsDTO).post();
    SuccessMetricsReportDTO result = response.getBody(SuccessMetricsReportDTO.class);

    AuditDTO auditDTO = assertReportData(AuditEvent.CREATE_SUCCESS_METRICS_REPORT, result.id, result.name);
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(appId, null));
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(org.getId(), org));
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser_SelectedOrgDoesNotExist() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String orgId = "orgId";
    SuccessMetricsReportScopeDTO successMetricsScopeDTO = new SuccessMetricsReportScopeDTO(toIds(app),
        new HashSet<>(Collections.singletonList(orgId)));
    SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO(METRICS_NAME, successMetricsScopeDTO);
    HttpResponse response = successMetricsReportRequest().body(successMetricsDTO).post();
    SuccessMetricsReportDTO result = response.getBody(SuccessMetricsReportDTO.class);

    AuditDTO auditDTO = assertReportData(AuditEvent.CREATE_SUCCESS_METRICS_REPORT, result.id, result.name);
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(app.getId(), app));
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(orgId, null));
  }

  @Test
  public void testCreateSuccessMetricsReportForCurrentUser_NoOrgSelected() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    SuccessMetricsReportScopeDTO successMetricsScopeDTO = new SuccessMetricsReportScopeDTO(toIds(app),
        Collections.emptySet());
    SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO(METRICS_NAME, successMetricsScopeDTO);
    HttpResponse response = successMetricsReportRequest().body(successMetricsDTO).post();
    SuccessMetricsReportDTO result = response.getBody(SuccessMetricsReportDTO.class);

    AuditDTO auditDTO = assertReportData(AuditEvent.CREATE_SUCCESS_METRICS_REPORT, result.id, result.name);
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(app.getId(), app));
    assertSelectedOrganizations(auditDTO);
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    SuccessMetricsReport report = createSuccessMetricsReport(Collections.singleton(org.getId()),
        Collections.singleton(app.getId()), null);
    successMetricsReportRequest().subpath("{successMetricsId}").parameter(report.getId()).delete();

    AuditDTO auditDTO = assertReportData(AuditEvent.DELETE_SUCCESS_METRICS_REPORT, report.getId(), report.getName());
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(app.getId(), app));
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(org.getId(), org));
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser_SelectedAppAndParentOrg() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    SuccessMetricsReport report = createSuccessMetricsReport(Collections.singleton(org.getId()),
        Collections.singleton(app.getId()), null);
    successMetricsReportRequest().subpath("{successMetricsId}").parameter(report.getId()).delete();

    AuditDTO auditDTO = assertReportData(AuditEvent.DELETE_SUCCESS_METRICS_REPORT, report.getId(), report.getName());
    assertSelectedApplications(auditDTO);
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(org.getId(), org));
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser_SelectedOrgDoesNotExist() throws Exception {
    String orgId = "orgId";
    SuccessMetricsReport report = createSuccessMetricsReport(Collections.singleton(orgId), Collections.emptySet(),
        null);
    successMetricsReportRequest().subpath("{successMetricsId}").parameter(report.getId()).delete();

    AuditDTO auditDTO = assertReportData(AuditEvent.DELETE_SUCCESS_METRICS_REPORT, report.getId(), report.getName());
    assertSelectedApplications(auditDTO);
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(orgId, null));
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser_SelectedAppDoesNotExist() throws Exception {
    String appId = "appId";
    SuccessMetricsReport report = createSuccessMetricsReport(Collections.emptySet(), Collections.singleton(appId),
        null);
    successMetricsReportRequest().subpath("{successMetricsId}").parameter(report.getId()).delete();

    AuditDTO auditDTO = assertReportData(AuditEvent.DELETE_SUCCESS_METRICS_REPORT, report.getId(), report.getName());
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(appId, null));
    assertSelectedOrganizations(auditDTO);
  }

  @Test
  public void testDeleteSuccessMetricsReportForCurrentUser_Unauthorized() throws Exception {
    SuccessMetricsReport report = createSuccessMetricsReport(Collections.emptySet(), Collections.emptySet(), null);
    successMetricsReportRequest().subpath("{successMetricsId}")
        .parameter(report.getId())
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SUCCESS_METRICS_REPORT, "unauthorized",
        getUnauthorizedUsername());
    assertThat(auditDTO.data).isNull();
  }

  private void testViewSuccessMetricsData_Unauthorized(final String resourceSubpath) throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    SuccessMetricsReport report = createSuccessMetricsReport(null, Collections.singleton(app.getId()),
        getUnauthorizedUsername());

    successMetricsReportRequest().path(resourceSubpath)
        .with(unauthorizedUser())
        .parameter(report.getId())
        .get();

    // user allowed to access resource but no apps included in report
    assertViewSuccessMetricsReport_Unauthorized(getUnauthorizedUsername());
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

  private void assertViewSuccessMetricsReport_Unauthorized(final String username) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SUCCESS_METRICS_REPORT, "unauthorized", username);
    assertThat(auditDTO.data).isNull();
  }

  private void assertViewSuccessMetricsReport(
      final SuccessMetricsReport report,
      final int includedApplicationCount,
      final String username)
  {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SUCCESS_METRICS_REPORT, null, username);
    assertCustomData(auditDTO, "reportId", report.getId());
    assertCustomData(auditDTO, "reportName", report.getName());
    assertCustomData(auditDTO, "inspectedApplicationCount", includedApplicationCount);
  }

  private AuditDTO assertReportData(final AuditEvent auditEvent, final String id, final String name) {
    AuditDTO auditDTO = assertAuditLog(auditEvent, null);
    assertCustomData(auditDTO, "reportId", id);
    assertCustomData(auditDTO, "reportName", name);
    return auditDTO;
  }

  private HttpRequest successMetricsReportRequest() {
    return restRequest().path(SuccessMetricsReportResource.RESOURCE_PATH);
  }

  private SuccessMetricsReport createSuccessMetricsReport(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final String username)
  {
    SuccessMetricsReportScopeDTO scope = new SuccessMetricsReportScopeDTO();
    scope.organizationIds = organizationIds;
    scope.applicationIds = applicationIds;

    return tempEntity
        .newSuccessMetricsReport(username == null ? getUsername() : username, "report", JsonUtils.format(scope));
  }

  private Set<String> toIds(Owner... owners) {
    return Arrays.stream(owners).map(Owner::getId).collect(Collectors.toSet());
  }
}

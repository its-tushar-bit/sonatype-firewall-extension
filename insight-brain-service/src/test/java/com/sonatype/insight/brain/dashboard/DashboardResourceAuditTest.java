/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.ApplicationAuditDTO;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.OrganizationAuditDTO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.DashboardFilterService.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.security.User.ADMIN_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardResourceAuditTest
    extends AbstractAuditTest
{
  private DashboardFilterDAO dashboardFilterDAO;

  @Before
  public void before() {
    dashboardFilterDAO = lookup(DashboardFilterDAO.class);
  }

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
  public void testDeleteDashboardFilterForCurrentUserByFilterName() throws Exception {
    String filterName = "some-filter";
    DashboardFilter filter = tempEntity.newDashboardFilter(ADMIN_USERNAME, InternalRealm.ID, filterName,
        JsonUtils.format(createNamedDashboardFilter(filterName)));

    dashboardRequest().path(DashboardResource.DELETE_NAMED_FILTER_PATH).query("filterName", filterName).post();

    List<AuditDTO> auditDTOS = assertAuditLogs(AuditEvent.DELETE_DASHBOARD_FILTER, 1, null);
    assertDashboardFilterAuditData(filter, auditDTOS.get(0));
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName_NotFound() throws Exception {
    dashboardRequest().path(DashboardResource.DELETE_NAMED_FILTER_PATH)
        .query("filterName", "non-existent-filter")
        .post();

    assertAuditLog(AuditEvent.DELETE_DASHBOARD_FILTER, "not-found");
  }

  @After
  public void after() {
    // required in order to avoid clashes between create/delete tests
    dashboardFilterDAO.getByUsernameAndRealmId(ADMIN_USERNAME, InternalRealm.ID).forEach(dashboardFilterDAO::delete);
  }

  private void assertDashboardFilterAudit(final String filterName) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SAVE_DASHBOARD_FILTER, null);
    DashboardFilter persistedFilter =
        dashboardFilterDAO.getByUsernameAndRealmIdAndName(ADMIN_USERNAME, InternalRealm.ID, filterName);
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

  private void testGetRisks(String restPath, AuditEvent expectedAuditEvent) throws Exception {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplication(org2.getId());
    Tag appCategory1 = tempEntity.newTag(org1.getId());
    Tag appCategory2 = tempEntity.newTag(org2.getId());
    tempEntity.newApplicationTag(app1.getId(), appCategory1.getId());
    tempEntity.newApplicationTag(app2.getId(), appCategory2.getId());
    Policy policy = tempEntity.newPolicy(org1);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app1.getId(), StageTypes.BUILD.getId(), "scanId");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    String unknownOrgId = "unknownOrgId";
    String unknownAppId = "unknownAppId";
    String unknownAppCategoryId = "unknownAppCategoryId";
    String uncategorizedAppCategoryId = null;

    RisksFilterDTO risksFilterDTO = new RisksFilterDTO();
    risksFilterDTO.organizationIds = new HashSet<>(Arrays.asList(org1.getId(), unknownOrgId));
    risksFilterDTO.applicationIds = new HashSet<>(Arrays.asList(app1.getId(), app2.getId(), unknownAppId));
    risksFilterDTO.tagIds = new HashSet<>(
        Arrays.asList(appCategory1.getId(), appCategory2.getId(), unknownAppCategoryId, uncategorizedAppCategoryId));

    dashboardRequest(restPath, risksFilterDTO);

    AuditDTO auditDTO = assertAuditLog(expectedAuditEvent, null);
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(unknownOrgId, null),
        new OrganizationAuditDTO(org1.getId(), org1));
    // app1 is not in the audit list of selected applications because its parent org is in the list of selected orgs.
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(unknownAppId, null),
        new ApplicationAuditDTO(app2.getId(), app2));
    assertSelectedApplicationCategories(auditDTO, new ApplicationCategoryAuditDTO(unknownAppCategoryId, null),
        new ApplicationCategoryAuditDTO(null, "(Uncategorized)"), new ApplicationCategoryAuditDTO(appCategory1),
        new ApplicationCategoryAuditDTO(appCategory2));
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
  }

  @Test
  public void testGetViolationRisks() throws Exception {
    testGetRisks(DashboardResource.GET_VIOLATION_RISKS_PATH, AuditEvent.VIEW_DASHBOARD_VIOLATION_LIST);
  }

  @Test
  public void testGetApplicationRisks() throws Exception {
    testGetRisks(DashboardResource.GET_APPLICATION_RISKS_PATH, AuditEvent.VIEW_DASHBOARD_APPLICATION_LIST);
  }

  @Test
  public void testGetComponentRisks() throws Exception {
    testGetRisks(DashboardResource.GET_COMPONENT_RISKS_PATH, AuditEvent.VIEW_DASHBOARD_COMPONENT_LIST);
  }

  @Test
  public void testGetComponentRisksExport() throws Exception {
    testGetRisks(DashboardResource.GET_COMPONENT_RISKS_EXPORT_PATH, AuditEvent.EXPORT_DASHBOARD_COMPONENT_LIST);
  }

  @Test
  public void testGetViolationRisksExport() throws Exception {
    testGetRisks(DashboardResource.GET_VIOLATION_RISKS_EXPORT_PATH, AuditEvent.EXPORT_DASHBOARD_VIOLATION_LIST);
  }

  @Test
  public void testGetApplicationRisksExport() throws Exception {
    testGetRisks(DashboardResource.GET_APPLICATION_RISKS_EXPORT_PATH, AuditEvent.EXPORT_DASHBOARD_APPLICATION_LIST);
  }

  private void testGetRisks_EmptyFilter(String restPath, AuditEvent expectedAuditEvent) throws Exception {
    tempEntity.newApplicationWithParent();

    RisksFilterDTO risksFilterDTO = new RisksFilterDTO();

    dashboardRequest(restPath, risksFilterDTO);

    AuditDTO auditDTO = assertAuditLog(expectedAuditEvent, null);
    assertSelectedOrganizations(auditDTO);
    assertSelectedApplications(auditDTO);
    assertSelectedApplicationCategories(auditDTO);
    assertCustomData(auditDTO, "inspectedApplicationCount", 1);
  }

  @Test
  public void testGetViolationRisks_EmptyFilter() throws Exception {
    testGetRisks_EmptyFilter(DashboardResource.GET_VIOLATION_RISKS_PATH, AuditEvent.VIEW_DASHBOARD_VIOLATION_LIST);
  }

  @Test
  public void testGetApplicationRisks_EmptyFilter() throws Exception {
    testGetRisks_EmptyFilter(DashboardResource.GET_APPLICATION_RISKS_PATH, AuditEvent.VIEW_DASHBOARD_APPLICATION_LIST);
  }

  @Test
  public void testGetComponentRisks_EmptyFilter() throws Exception {
    testGetRisks_EmptyFilter(DashboardResource.GET_COMPONENT_RISKS_PATH, AuditEvent.VIEW_DASHBOARD_COMPONENT_LIST);
  }

  @Test
  public void testGetComponentRisksExport_EmptyFilter() throws Exception {
    testGetRisks_EmptyFilter(DashboardResource.GET_COMPONENT_RISKS_EXPORT_PATH,
        AuditEvent.EXPORT_DASHBOARD_COMPONENT_LIST);
  }

  @Test
  public void testGetViolationRisksExport_EmptyFilter() throws Exception {
    testGetRisks_EmptyFilter(DashboardResource.GET_VIOLATION_RISKS_EXPORT_PATH,
        AuditEvent.EXPORT_DASHBOARD_VIOLATION_LIST);
  }

  @Test
  public void testGetApplicationRisksExport_EmptyFilter() throws Exception {
    testGetRisks_EmptyFilter(DashboardResource.GET_APPLICATION_RISKS_EXPORT_PATH,
        AuditEvent.EXPORT_DASHBOARD_APPLICATION_LIST);
  }

  @Test
  public void testGetPolicyWaivers() throws Exception {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplication(org2.getId());

    Policy policyOrg1 = tempEntity.newPolicy(org1);
    tempEntity.newWaiver(policyOrg1.getId(), app1.getId());
    tempEntity.newWaiver(policyOrg1.getId(), org1.getId());
    Policy policyOrg2 = tempEntity.newPolicy(org1);
    tempEntity.newWaiver(policyOrg2.getId(), app2.getId());
    tempEntity.newWaiver(policyOrg2.getId(), org2.getId());

    String unknownOrgId = "unknownOrgId";
    String unknownAppId = "unknownAppId";

    RisksFilterDTO risksFilterDTO = new RisksFilterDTO();
    risksFilterDTO.organizationIds = new HashSet<>(Arrays.asList(org1.getId(), unknownOrgId));
    risksFilterDTO.applicationIds = new HashSet<>(Arrays.asList(app1.getId(), app2.getId(), unknownAppId));

    dashboardRequest(DashboardResource.GET_POLICY_WAIVERS_PATH, risksFilterDTO);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_WAIVER_LIST, null);
    assertCustomData(auditDTO, "filteredOwnersCount", 5);
  }

  @Test
  public void testGetPolicyWaivers_EmptyFilter() throws Exception {
    createDashboardWaivers();

    RisksFilterDTO risksFilterDTO = new RisksFilterDTO();

    dashboardRequest(DashboardResource.GET_POLICY_WAIVERS_PATH, risksFilterDTO);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_WAIVER_LIST, null);
    assertCustomData(auditDTO, "filteredOwnersCount", 6);
  }

  private void createDashboardWaivers() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplication(org2.getId());

    Policy policyOrg1 = tempEntity.newPolicy(org1);
    tempEntity.newWaiver(policyOrg1.getId(), app1.getId());
    tempEntity.newWaiver(policyOrg1.getId(), org1.getId());
    Policy policyOrg2 = tempEntity.newPolicy(org1);
    tempEntity.newWaiver(policyOrg2.getId(), app2.getId());
    tempEntity.newWaiver(policyOrg2.getId(), org2.getId());
  }

  @Test
  public void testGetPolicyWaivers_RootOrgFilter() throws Exception {
    createDashboardWaivers();

    tempEntity.newRepository();
    Policy policyRepository = tempEntity.newPolicy(RepositoryContainer.SINGLETON);
    tempEntity.newWaiver(policyRepository.getId(), Organization.ROOT_ORGANIZATION_ID);

    RisksFilterDTO risksFilterDTO = new RisksFilterDTO();
    risksFilterDTO.organizationIds = Collections.singleton(Organization.ROOT_ORGANIZATION_ID);

    dashboardRequest(DashboardResource.GET_POLICY_WAIVERS_PATH, risksFilterDTO);

    // Should only have information regarding the repositories and the root org
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_WAIVER_LIST, null);
    assertCustomData(auditDTO, "filteredOwnersCount", 1);
  }

  @Test
  public void testGetPolicyWaiversExport() throws Exception {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplication(org2.getId());

    Policy policyOrg1 = tempEntity.newPolicy(org1);
    tempEntity.newWaiver(policyOrg1.getId(), app1.getId());
    tempEntity.newWaiver(policyOrg1.getId(), org1.getId());
    Policy policyOrg2 = tempEntity.newPolicy(org1);
    tempEntity.newWaiver(policyOrg2.getId(), app2.getId());
    tempEntity.newWaiver(policyOrg2.getId(), org2.getId());

    String unknownOrgId = "unknownOrgId";
    String unknownAppId = "unknownAppId";

    RisksFilterDTO risksFilterDTO = new RisksFilterDTO();
    risksFilterDTO.organizationIds = new HashSet<>(Arrays.asList(org1.getId(), unknownOrgId));
    risksFilterDTO.applicationIds = new HashSet<>(Arrays.asList(app1.getId(), app2.getId(), unknownAppId));

    dashboardRequest(DashboardResource.GET_POLICY_WAIVERS_EXPORT_PATH, risksFilterDTO);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_DASHBOARD_WAIVER_LIST, null);
    assertCustomData(auditDTO, "filteredOwnersCount", 5);
  }

  @Test
  public void testGetPolicyWaiversExport_EmptyFilter() throws Exception {
    createDashboardWaivers();

    RisksFilterDTO risksFilterDTO = new RisksFilterDTO();

    dashboardRequest(DashboardResource.GET_POLICY_WAIVERS_EXPORT_PATH, risksFilterDTO);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_DASHBOARD_WAIVER_LIST, null);
    assertCustomData(auditDTO, "filteredOwnersCount", 6);
  }

  private void dashboardRequest(String restPath, RisksFilterDTO risksFilterDTO) throws Exception {
    HttpRequest request = restRequest().path(DashboardResource.RESOURCE_PATH).path(restPath);
    if (restPath.startsWith("export/")) {
      request.part("filter", risksFilterDTO).post();
    }
    else {
      request.body(risksFilterDTO).post();
    }
  }

  private void assertSelectedApplicationCategories(AuditDTO auditDTO, ApplicationCategoryAuditDTO... expected) {
    ApplicationCategoryAuditDTO[] actuals = JSON
        .convertValue(auditDTO.data.get("selectedApplicationCategories"), ApplicationCategoryAuditDTO[].class);
    assertThat(actuals).containsExactlyInAnyOrder(expected);
  }
}

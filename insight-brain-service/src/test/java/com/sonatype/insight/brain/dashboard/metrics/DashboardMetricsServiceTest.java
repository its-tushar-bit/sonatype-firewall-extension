/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsShadowComparisonService;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsScopeResolver;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlCoordinator;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlMode;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlModeProvider;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlReadiness;
import com.sonatype.insight.brain.dashboard.metrics.sql.ResolvedScope;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.ConflictException;

import jakarta.inject.Inject;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;

/**
 * Dashboard metrics service: Lucene integration and coalescing cache (CLM-40927).
 */
public class DashboardMetricsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private DashboardMetricsService dashboardMetricsService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Before
  public void setUpClient() {
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
  }

  @Test
  public void testGetMetrics_WaiversCountAndBreakdownFromSql() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(org.getId());

    tempEntity.newWaiver("hashExisting1", policy.getId(), app.getId());
    tempEntity.newWaiver("hashExisting2", policy.getId(), app.getId());

    PolicyWaiverRequest requested = new PolicyWaiverRequest("hashRequested", policy.getId(), app.getId(), "pending");
    requested.setStatus(PolicyWaiverRequestStatus.REQUESTED);
    tempEntity.newPolicyWaiverRequest(requested);

    PolicyWaiverRequest approved = new PolicyWaiverRequest("hashApproved", policy.getId(), app.getId(), "approved");
    approved.setStatus(PolicyWaiverRequestStatus.APPROVED);
    tempEntity.newPolicyWaiverRequest(approved);

    User reader = tempEntity.newUser("metrics-waivers-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.waivers.total).isEqualTo(3);
    assertThat(metrics.waivers.source).isEqualTo("sql");
    assertThat(metrics.waivers.breakdown).containsEntry("existing", 2L);
    assertThat(metrics.waivers.breakdown).containsEntry("requested", 1L);
  }

  @Test
  public void testGetMetrics_ViolationsCountAndBreakdownFromIndex() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "violationsMetricsReport");
    ReportTestUtils.createReportFile(evaluation.getApplicationId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir), lookup(InsightWork.class));

    Policy pCrit = tempEntity.newPolicy(org.getId(), "Security - Critical");
    Policy pSev = tempEntity.newPolicy(org.getId(), "Security - Severe");
    Policy pMod = tempEntity.newPolicy(org.getId(), "Legal - Moderate");
    Policy pLow = tempEntity.newPolicy(org.getId(), "Quality - Low");

    tempEntity.newPolicyViolation(evaluation, pCrit, 10, PolicyThreatCategory.SECURITY,
        "com.crit", "crit10", "1.0", "hashCrit10000000000");
    tempEntity.newPolicyViolation(evaluation, pCrit, 8, PolicyThreatCategory.SECURITY,
        "com.crit", "crit8", "1.0", "hashCrit800000000000");
    tempEntity.newPolicyViolation(evaluation, pSev, 5, PolicyThreatCategory.SECURITY,
        "com.sev", "sev", "1.0", "hashSev000000000000");
    tempEntity.newPolicyViolation(evaluation, pMod, 3, PolicyThreatCategory.LICENSE,
        "com.mod", "mod", "1.0", "hashMod000000000000");
    tempEntity.newPolicyViolation(evaluation, pLow, 1, PolicyThreatCategory.QUALITY,
        "com.low", "low1", "1.0", "hashLow1000000000000");

    User reader = tempEntity.newUser("metrics-violations-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of(org.getId());

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(request);

    assertThat(metrics.violations.total).isEqualTo(5);
    assertThat(metrics.violations.source).isEqualTo("index");
    assertThat(metrics.violations.breakdown).containsEntry("critical", 2L);
    assertThat(metrics.violations.breakdown).containsEntry("severe", 1L);
    assertThat(metrics.violations.breakdown).containsEntry("moderate", 1L);
    assertThat(metrics.violations.breakdown).containsEntry("low", 1L);

    assertThat(metrics.policies.total).isEqualTo(4);
    assertThat(metrics.policies.source).isEqualTo("index");

    assertThat(metrics.legal.source).isEqualTo("index");
    assertThat(metrics.legal.breakdown).containsEntry("applications", 1L);
    assertThat(metrics.legal.breakdown).containsEntry("components", 6L);
    assertThat(metrics.legal.total).isEqualTo(7L);
  }

  @Test
  public void testGetMetrics_ViolationsOrganizationFilterHierarchyInclusive() throws Exception {
    Organization parentOrg = tempEntity.newOrganization("violations-parent-org");
    Organization childOrg = tempEntity.newOrganization("violations-child-org", parentOrg);
    Organization siblingOrg = tempEntity.newOrganization("violations-sibling-org");

    Application childApp = tempEntity.newApplication(childOrg.getId());
    Application siblingApp = tempEntity.newApplication(siblingOrg.getId());

    seedPolicyViolation(childOrg, childApp, "childViolationReport", 10);
    seedPolicyViolation(childOrg, childApp, "childViolationReport2", 5);
    seedPolicyViolation(siblingOrg, siblingApp, "siblingViolationReport", 8);

    User reader = tempEntity.newUser("violations-hierarchy-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO filterByChild = new DashboardMetricsRequestDTO();
    filterByChild.organizationIds = Set.of(childOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByChild).violations.total).isEqualTo(2);

    DashboardMetricsRequestDTO filterByParent = new DashboardMetricsRequestDTO();
    filterByParent.organizationIds = Set.of(parentOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByParent).violations.total).isEqualTo(2);

    DashboardMetricsRequestDTO filterBySibling = new DashboardMetricsRequestDTO();
    filterBySibling.organizationIds = Set.of(siblingOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterBySibling).violations.total).isEqualTo(1);
  }

  @Test
  public void testGetMetrics_ViolationsBreakdownSumMatchesTotalForOutOfRangeThreatLevel() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    seedPolicyViolation(org, app, "outOfRangeViolationReport", 15);

    User reader = tempEntity.newUser("violations-out-of-range-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.violations.total).isEqualTo(1);
    assertThat(metrics.violations.breakdown).containsEntry("critical", 1L);
    assertThat(metrics.violations.breakdown.values().stream().mapToLong(Long::longValue).sum())
        .isEqualTo(metrics.violations.total);
  }

  @Test
  public void testGetMetrics_ViolationsFailsClosed_UserWithNoReadContexts() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    seedPolicyViolation(org, app, "noPermViolationReport", 10);

    User userWithNoPermissions = tempEntity.newUser("violations-no-permissions");

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(userWithNoPermissions);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.violations.total).isZero();
    assertThat(metrics.violations.source).isEqualTo("index");
    assertThat(metrics.violations.breakdown).containsEntry("critical", 0L);
    assertThat(metrics.violations.breakdown).containsEntry("severe", 0L);
    assertThat(metrics.violations.breakdown).containsEntry("moderate", 0L);
    assertThat(metrics.violations.breakdown).containsEntry("low", 0L);
    assertThat(metrics.waivers.total).isZero();
    assertThat(metrics.waivers.source).isEqualTo("sql");
    assertThat(metrics.waivers.breakdown).containsEntry("existing", 0L);
    assertThat(metrics.waivers.breakdown).containsEntry("requested", 0L);
  }

  @Test
  public void testGetMetrics_ScannedComponents_VulnerableComponentWithMultipleCvesCountsOnce() throws Exception {
    // componentsMetricReport indexes 1 clean component and 3 vulnerable components where one has 2 CVEs
    // (4 SECURITY_VULNERABILITY docs). distinct(clean)=1 + distinct(vulnerable)=3 => 4 scanned components.
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    seedComponentsReport(app, "componentsMetricsReport");

    User reader = tempEntity.newUser("metrics-components-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of(org.getId());

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(request);

    assertThat(metrics.components.total).isEqualTo(4);
    assertThat(metrics.components.source).isEqualTo("index");
    assertThat(metrics.components.breakdown).isNull();

    assertThat(metrics.vulnerabilities.total).isEqualTo(4);
    assertThat(metrics.vulnerabilities.source).isEqualTo("index");
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("critical", 1L);
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("high", 1L);
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("medium", 1L);
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("low", 1L);
  }

  @Test
  public void testGetMetrics_Vulnerabilities_MultiStageEvaluationDoesNotDoubleCount() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    seedComponentsReport(app, Stage.ID_BUILD, "componentsBuildStage");
    seedComponentsReport(app, ReleaseStageType.ID, "componentsReleaseStage");

    User reader = tempEntity.newUser("metrics-vuln-multistage-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    // Same CVEs indexed at Build + Release: 4 distinct vulnerabilityIds, not 8 per-stage docs.
    assertThat(metrics.vulnerabilities.total).isEqualTo(4);
    assertThat(metrics.components.total).isEqualTo(4);
  }

  @Test
  public void testGetMetrics_Vulnerabilities_SameCveAcrossApplicationsCountsOnce() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(org.getId());
    Application appTwo = tempEntity.newApplication(org.getId());
    seedComponentsReport(appOne, "componentsMetricsReport");
    seedComponentsReport(appTwo, "componentsMetricsReportAppTwo");

    User reader = tempEntity.newUser("metrics-vuln-estate-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    // componentsMetricsReport fixture has 4 distinct CVEs; indexing it on two apps still yields 4 estate CVEs.
    assertThat(metrics.vulnerabilities.total).isEqualTo(4);
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("critical", 1L);
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("high", 1L);
    assertThat(metrics.components.total).isEqualTo(8);
  }

  @Test
  public void testGetMetrics_ScannedComponents_OrganizationFilterHierarchyInclusive() throws Exception {
    Organization parentOrg = tempEntity.newOrganization("components-parent-org");
    Organization childOrg = tempEntity.newOrganization("components-child-org", parentOrg);
    Organization siblingOrg = tempEntity.newOrganization("components-sibling-org");

    Application childApp = tempEntity.newApplication(childOrg.getId());
    Application siblingApp = tempEntity.newApplication(siblingOrg.getId());
    seedComponentsReport(childApp, "componentsChildReport");
    seedComponentsReport(siblingApp, "componentsSiblingReport");

    User reader = tempEntity.newUser("components-hierarchy-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO filterByChild = new DashboardMetricsRequestDTO();
    filterByChild.organizationIds = Set.of(childOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByChild).components.total).isEqualTo(4);

    DashboardMetricsRequestDTO filterByParent = new DashboardMetricsRequestDTO();
    filterByParent.organizationIds = Set.of(parentOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByParent).components.total).isEqualTo(4);

    DashboardMetricsRequestDTO filterBySibling = new DashboardMetricsRequestDTO();
    filterBySibling.organizationIds = Set.of(siblingOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterBySibling).components.total).isEqualTo(4);
  }

  @Test
  public void testGetMetrics_ScannedComponents_FailsClosed_UserWithNoReadContexts() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    seedComponentsReport(app, "componentsFailClosedReport");

    User userWithNoPermissions = tempEntity.newUser("components-no-permissions");

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(userWithNoPermissions);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.components.total).isZero();
    assertThat(metrics.components.source).isEqualTo("index");
  }

  @Test
  public void testGetMetrics_OrganizationFilterHierarchyInclusive() {
    Organization parentOrg = tempEntity.newOrganization("metrics-parent-org");
    Organization childOrg = tempEntity.newOrganization("metrics-child-org", parentOrg);
    Organization siblingOrg = tempEntity.newOrganization("metrics-sibling-org");

    tempEntity.newApplication(parentOrg.getId());
    tempEntity.newApplication(childOrg.getId());
    tempEntity.newApplication(childOrg.getId());
    Application siblingApp = tempEntity.newApplication(siblingOrg.getId());
    tempEntity.newApplication(siblingOrg.getId());
    tempEntity.newApplication(siblingOrg.getId());

    User reader = tempEntity.newUser("metrics-hierarchy-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO unfiltered = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());
    assertThat(unfiltered.applications.total).isEqualTo(6);
    assertIndexSourcedMetric(unfiltered);
    long readableOrganizationCount = unfiltered.organizations.total;

    DashboardMetricsRequestDTO filterByChild = new DashboardMetricsRequestDTO();
    filterByChild.organizationIds = Set.of(childOrg.getId());
    DashboardMetricsDTO childScoped = dashboardMetricsService.getMetrics(filterByChild);
    assertThat(childScoped.applications.total).isEqualTo(2);
    assertIndexSourcedMetric(childScoped);

    DashboardMetricsRequestDTO filterByParent = new DashboardMetricsRequestDTO();
    filterByParent.organizationIds = Set.of(parentOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByParent).applications.total).isEqualTo(3);

    DashboardMetricsRequestDTO filterBySibling = new DashboardMetricsRequestDTO();
    filterBySibling.organizationIds = Set.of(siblingOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterBySibling).applications.total).isEqualTo(3);

    DashboardMetricsRequestDTO filterByRoot = new DashboardMetricsRequestDTO();
    filterByRoot.organizationIds = Set.of(Organization.ROOT_ORGANIZATION_ID);
    assertThat(dashboardMetricsService.getMetrics(filterByRoot).applications.total).isEqualTo(6);

    DashboardMetricsRequestDTO filterByMultipleOrgs = new DashboardMetricsRequestDTO();
    filterByMultipleOrgs.organizationIds = Set.of(parentOrg.getId(), siblingOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByMultipleOrgs).applications.total).isEqualTo(6);

    DashboardMetricsRequestDTO applicationOnly = new DashboardMetricsRequestDTO();
    applicationOnly.applicationIds = Set.of(siblingApp.getId());
    assertThat(dashboardMetricsService.getMetrics(applicationOnly).applications.total).isEqualTo(1);
    assertThat(dashboardMetricsService.getMetrics(applicationOnly).organizations.total)
        .isEqualTo(readableOrganizationCount);

    DashboardMetricsRequestDTO combinedOrgAndApp = new DashboardMetricsRequestDTO();
    combinedOrgAndApp.organizationIds = Set.of(parentOrg.getId());
    combinedOrgAndApp.applicationIds = Set.of(siblingApp.getId());
    assertThat(dashboardMetricsService.getMetrics(combinedOrgAndApp).applications.total).isEqualTo(4);

    DashboardMetricsRequestDTO unknownOrg = new DashboardMetricsRequestDTO();
    unknownOrg.organizationIds = Set.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertThat(dashboardMetricsService.getMetrics(unknownOrg).applications.total).isZero();
  }

  @Test
  public void testGetMetrics_OrganizationFilterRejectsOversizedExpansion() {
    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
    when(organizationDAO.getAllChildOrganizationIds(Set.of("big-org")))
        .thenReturn(Set.of("expanded-org-0", "expanded-org-1", "expanded-org-2", "expanded-org-3", "expanded-org-4",
            "expanded-org-5"));

    Configuration configuration = mock(Configuration.class);
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(5);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));

    DashboardMetricsService service =
        newServiceWithMocks(
            mock(SearchIndexClient.class),
            new MetricFilterValidator(),
            organizationDAO,
            configuration,
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("big-org");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getMetrics(request))
        .withMessageContaining("too many organizations");
  }

  @Test
  public void testGetMetrics_OrganizationFilterAcceptsExpansionAtMaxClauseCount() {
    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
    when(organizationDAO.getAllChildOrganizationIds(Set.of("max-org")))
        .thenReturn(Set.of("expanded-org-0", "expanded-org-1", "expanded-org-2", "expanded-org-3", "expanded-org-4"));

    Configuration configuration = mock(Configuration.class);
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(5);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));

    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(1L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            organizationDAO,
            configuration,
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("max-org");

    assertThat(service.getMetrics(request).applications.total).isEqualTo(1);
  }

  @Test
  public void testGetMetrics_OrganizationExpansionComputedOncePerRequest() {
    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
    when(organizationDAO.getAllChildOrganizationIds(Set.of("parent-org")))
        .thenReturn(Set.of("child-org"));

    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(1L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    Configuration configuration = mock(Configuration.class);
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            organizationDAO,
            configuration,
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("parent-org");
    service.getMetrics(request);

    verify(organizationDAO, times(1)).getAllChildOrganizationIds(Set.of("parent-org"));
  }

  @Test
  public void testGetMetrics_ApplicationsCountFromIndex() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());

    User reader = tempEntity.newUser("metrics-org-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(request);

    assertThat(metrics.applications.total).isEqualTo(3);
    assertThat(metrics.applications.source).isEqualTo("index");
    assertThat(metrics.applications.breakdown).containsEntry(
        "stages",
        (long) lookup(StageTypeService.class).getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT).size());
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.violations.source).isEqualTo("index");
    assertThat(metrics.lastUpdatedAt).isNotNull();

    assertThat(metrics.organizations.total).isEqualTo(1);
    assertThat(metrics.organizations.source).isEqualTo("index");
    assertThat(metrics.organizations.breakdown).isNull();

    DashboardMetricsRequestDTO filterByApp = new DashboardMetricsRequestDTO();
    filterByApp.applicationIds = Set.of(app.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByApp).applications.total).isEqualTo(1);
    assertThat(dashboardMetricsService.getMetrics(filterByApp).organizations.total).isEqualTo(1);
  }

  @Test
  public void testGetMetrics_OffServesIndexSources() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "OFF");
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newPolicy(org.getId());
    User reader = tempEntity.newUser("metrics-off-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.applications.source).isEqualTo("index");
    assertThat(metrics.organizations.source).isEqualTo("index");
    assertThat(metrics.policies.source).isEqualTo("index");
    assertThat(metrics.violations.source).isEqualTo("index");
  }

  @Test
  public void testGetMetrics_ShadowServesCompletedIndexDtoAndSchedulesComparison() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(3L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1234L);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("shadow-user", "shadow-user", User.INTERNAL_REALM_ID));
    DashboardMetricsSqlModeProvider modeProvider = mock(DashboardMetricsSqlModeProvider.class);
    when(modeProvider.configuredMode()).thenReturn(DashboardMetricsSqlMode.SHADOW);
    DashboardMetricsSqlReadiness readiness = mock(DashboardMetricsSqlReadiness.class);
    when(readiness.effectiveMode(DashboardMetricsSqlMode.SHADOW)).thenReturn(DashboardMetricsSqlMode.SHADOW);
    DashboardMetricsScopeResolver scopeResolver = mock(DashboardMetricsScopeResolver.class);
    when(scopeResolver.resolve(any())).thenReturn(ResolvedScope.denyAll(ResolvedScope.DenyReason.NO_ACCESS));
    DashboardMetricsShadowComparisonService shadowComparisonService =
        mock(DashboardMetricsShadowComparisonService.class);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        modeProvider,
        readiness,
        scopeResolver,
        mock(DashboardMetricsSqlCoordinator.class),
        shadowComparisonService,
        new DashboardIndexDimensionQueryBuilder(mock(OrganizationDAO.class), mockConfiguration()),
        mockConfiguration(),
        mock(StageTypeService.class),
        currentUser);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications.source).isEqualTo("index");
    assertThat(metrics.lastUpdatedAt).isEqualTo(1234L);
    verify(shadowComparisonService).maybeSchedule(same(request), same(metrics), any(), eq(1234L));
  }

  @Test
  public void testGetMetrics_ShadowHeavyOnlyPopulatesLastUpdatedAtForPersistentClassification() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(5678L);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("shadow-heavy-user", "shadow-heavy-user", User.INTERNAL_REALM_ID));
    DashboardMetricsSqlModeProvider modeProvider = mock(DashboardMetricsSqlModeProvider.class);
    when(modeProvider.configuredMode()).thenReturn(DashboardMetricsSqlMode.SHADOW);
    DashboardMetricsSqlReadiness readiness = mock(DashboardMetricsSqlReadiness.class);
    when(readiness.effectiveMode(DashboardMetricsSqlMode.SHADOW)).thenReturn(DashboardMetricsSqlMode.SHADOW);
    DashboardMetricsShadowComparisonService shadowComparisonService =
        mock(DashboardMetricsShadowComparisonService.class);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        modeProvider,
        readiness,
        mock(DashboardMetricsScopeResolver.class),
        mock(DashboardMetricsSqlCoordinator.class),
        shadowComparisonService,
        new DashboardIndexDimensionQueryBuilder(mock(OrganizationDAO.class), mockConfiguration()),
        mockConfiguration(),
        mock(StageTypeService.class),
        currentUser);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = true;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications).isNull();
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.lastUpdatedAt).isEqualTo(5678L);
    verify(shadowComparisonService).maybeSchedule(same(request), same(metrics), any(), eq(5678L));
  }

  @Test
  public void testGetMetrics_OnServesSqlSourcesForFourMigratedMetrics() throws Exception {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "ON");
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newPolicy(org.getId(), "metrics-on-policy");
    seedPolicyViolation(org, app, "metricsOnViolation", 10);
    User reader = tempEntity.newUser("metrics-on-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.applications.total).isEqualTo(1);
    assertThat(metrics.policies.total).isEqualTo(2);
    assertThat(metrics.violations.total).isEqualTo(1);
    assertThat(metrics.applications.source).isEqualTo("sql");
    assertThat(metrics.organizations.source).isEqualTo("sql");
    assertThat(metrics.policies.source).isEqualTo("sql");
    assertThat(metrics.violations.source).isEqualTo("sql");
  }

  @Test
  public void testGetMetrics_OnResolvesScopeOncePerTier() {
    DashboardMetricsScopeResolver scopeResolver = queryableScopeResolver();
    DashboardMetricsSqlCoordinator coordinator = sqlCoordinatorReturning(1L);
    DashboardMetricsService service = newSqlModeService(
        mock(SearchIndexClient.class),
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        scopeResolver,
        coordinator);

    service.getMetrics(new DashboardMetricsRequestDTO());

    verify(scopeResolver, times(1)).resolve(any());
  }

  @Test
  public void testGetMetrics_OnNoAccessReturnsZerosWithoutDaoCalls() {
    PolicyWaiverDAO waiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO waiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    DashboardMetricsScopeResolver scopeResolver = mock(DashboardMetricsScopeResolver.class);
    ResolvedScope noAccess = ResolvedScope.denyAll(ResolvedScope.DenyReason.NO_ACCESS);
    when(scopeResolver.resolve(any())).thenReturn(noAccess);
    DashboardMetricsSqlCoordinator coordinator = mock(DashboardMetricsSqlCoordinator.class);
    when(coordinator.countApplications(noAccess)).thenReturn(sqlMetric(0L));
    when(coordinator.countOrganizations(noAccess)).thenReturn(sqlMetric(0L));
    when(coordinator.countPolicies(noAccess)).thenReturn(sqlMetric(0L));
    when(coordinator.countViolations(noAccess)).thenReturn(
        new MetricValueDTO(0L, Map.of("low", 0L, "moderate", 0L, "severe", 0L, "critical", 0L), "sql"));
    DashboardMetricsService service =
        newSqlModeService(mock(SearchIndexClient.class), waiverDAO, waiverRequestDAO, scopeResolver, coordinator);

    DashboardMetricsDTO metrics = service.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.applications.total).isZero();
    assertThat(metrics.organizations.total).isZero();
    assertThat(metrics.policies.total).isZero();
    assertThat(metrics.violations.total).isZero();
    assertThat(metrics.waivers.total).isZero();
    verify(waiverDAO, never()).selectCount(any());
    verify(waiverRequestDAO, never()).selectCount(any());
  }

  @Test
  public void testGetMetrics_OnResolutionFailedReturnsMetricUnavailableWithoutFallback() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    DashboardMetricsScopeResolver scopeResolver = mock(DashboardMetricsScopeResolver.class);
    ResolvedScope failed = ResolvedScope.denyAll(ResolvedScope.DenyReason.RESOLUTION_FAILED);
    when(scopeResolver.resolve(any())).thenReturn(failed);
    DashboardMetricsSqlCoordinator coordinator = mock(DashboardMetricsSqlCoordinator.class);
    when(coordinator.countApplications(failed)).thenReturn(MetricValueDTO.unavailable("sql"));
    when(coordinator.countOrganizations(failed)).thenReturn(MetricValueDTO.unavailable("sql"));
    when(coordinator.countPolicies(failed)).thenReturn(MetricValueDTO.unavailable("sql"));
    when(coordinator.countViolations(failed)).thenReturn(MetricValueDTO.unavailable("sql"));
    DashboardMetricsService service = newSqlModeService(
        searchIndexClient,
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        scopeResolver,
        coordinator);

    DashboardMetricsDTO metrics = service.getMetrics(new DashboardMetricsRequestDTO());

    assertUnavailable(metrics.applications);
    assertUnavailable(metrics.organizations);
    assertUnavailable(metrics.policies);
    assertUnavailable(metrics.violations);
    assertUnavailable(metrics.waivers);
    verify(searchIndexClient, never()).count(anyString());
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any());
  }

  @Test
  public void testGetMetrics_OnDaoFailureIsolatedToOneMetric() {
    ResolvedScope scope = queryableScope();
    DashboardMetricsScopeResolver scopeResolver = mock(DashboardMetricsScopeResolver.class);
    when(scopeResolver.resolve(any())).thenReturn(scope);
    DashboardMetricsSqlCoordinator coordinator = mock(DashboardMetricsSqlCoordinator.class);
    when(coordinator.countApplications(scope)).thenReturn(MetricValueDTO.unavailable("sql"));
    when(coordinator.countOrganizations(scope)).thenReturn(sqlMetric(2L));
    when(coordinator.countPolicies(scope)).thenReturn(sqlMetric(3L));
    when(coordinator.countViolations(scope)).thenReturn(sqlMetric(4L));
    DashboardMetricsService service = newSqlModeService(
        mock(SearchIndexClient.class),
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        scopeResolver,
        coordinator);

    DashboardMetricsDTO metrics = service.getMetrics(new DashboardMetricsRequestDTO());

    assertUnavailable(metrics.applications);
    assertThat(metrics.organizations.total).isEqualTo(2);
    assertThat(metrics.policies.total).isEqualTo(3);
    assertThat(metrics.violations.total).isEqualTo(4);
  }

  @Test
  public void testGetMetrics_OnKeepsViolationsHeavy() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    DashboardMetricsScopeResolver scopeResolver = queryableScopeResolver();
    DashboardMetricsSqlCoordinator coordinator = sqlCoordinatorReturning(5L);
    DashboardMetricsService service = newSqlModeService(
        searchIndexClient,
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        scopeResolver,
        coordinator);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = true;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications).isNull();
    assertThat(metrics.violations.total).isEqualTo(5);
    assertThat(metrics.violations.source).isEqualTo("sql");
    assertThat(metrics.components.source).isEqualTo("index");
    verify(scopeResolver, times(1)).resolve(request);
  }

  @Test
  public void testGetMetrics_OnKeepsLastUpdatedAtIndexBacked() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1234L);
    DashboardMetricsService service = newSqlModeService(
        searchIndexClient,
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        queryableScopeResolver(),
        sqlCoordinatorReturning(1L));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.lastUpdatedAt).isEqualTo(1234L);
    verify(searchIndexClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testGetMetrics_TagAndStageUnsupportedInEveryMode() {
    for (DashboardMetricsSqlMode mode : DashboardMetricsSqlMode.values()) {
      DashboardMetricsScopeResolver stageSummaryResolver = mock(DashboardMetricsScopeResolver.class);
      DashboardMetricsSqlCoordinator stageSummaryCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      DashboardMetricsService stageSummaryService =
          newModeService(mode, stageSummaryResolver, stageSummaryCoordinator);
      DashboardMetricsRequestDTO stageSummary = new DashboardMetricsRequestDTO();
      stageSummary.stageIds = Set.of(Stage.ID_BUILD);
      stageSummary.includeHeavyMetrics = false;

      DashboardMetricsDTO stageSummaryMetrics = stageSummaryService.getMetrics(stageSummary);

      assertUnsupported(stageSummaryMetrics.applications, "stageIds");
      assertUnsupported(stageSummaryMetrics.organizations, "stageIds");
      assertUnsupported(stageSummaryMetrics.policies, "stageIds");
      assertUnsupported(stageSummaryMetrics.waivers, "stageIds");
      verify(stageSummaryResolver, never()).resolve(any());
      verifyNoInteractions(stageSummaryCoordinator);

      DashboardMetricsScopeResolver tagSummaryResolver = queryableScopeResolver();
      DashboardMetricsSqlCoordinator tagSummaryCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      DashboardMetricsService tagSummaryService =
          newModeService(mode, tagSummaryResolver, tagSummaryCoordinator);
      DashboardMetricsRequestDTO tagSummary = new DashboardMetricsRequestDTO();
      tagSummary.tagIds = Set.of("tag-1");
      tagSummary.includeHeavyMetrics = false;

      DashboardMetricsDTO tagSummaryMetrics = tagSummaryService.getMetrics(tagSummary);

      assertUnsupported(tagSummaryMetrics.applications, "tagIds");
      assertUnsupported(tagSummaryMetrics.organizations, "tagIds");
      assertUnsupported(tagSummaryMetrics.policies, "tagIds");
      assertThat(tagSummaryMetrics.waivers.source).isEqualTo("sql");
      verify(tagSummaryResolver, times(1)).resolve(tagSummary);
      verifyNoInteractions(tagSummaryCoordinator);

      DashboardMetricsScopeResolver stageHeavyResolver = mock(DashboardMetricsScopeResolver.class);
      DashboardMetricsSqlCoordinator stageHeavyCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      DashboardMetricsService stageHeavyService =
          newModeService(mode, stageHeavyResolver, stageHeavyCoordinator);
      DashboardMetricsRequestDTO stageHeavy = new DashboardMetricsRequestDTO();
      stageHeavy.stageIds = Set.of(Stage.ID_BUILD);
      stageHeavy.includeHeavyMetrics = true;

      DashboardMetricsDTO stageHeavyMetrics = stageHeavyService.getMetrics(stageHeavy);

      assertUnsupported(stageHeavyMetrics.violations, "stageIds");
      assertUnsupported(stageHeavyMetrics.components, "stageIds");
      assertUnsupported(stageHeavyMetrics.vulnerabilities, "stageIds");
      assertUnsupported(stageHeavyMetrics.legal, "stageIds");
      verify(stageHeavyResolver, never()).resolve(any());
      verifyNoInteractions(stageHeavyCoordinator);

      DashboardMetricsScopeResolver tagHeavyResolver = mock(DashboardMetricsScopeResolver.class);
      DashboardMetricsSqlCoordinator tagHeavyCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      DashboardMetricsService tagHeavyService =
          newModeService(mode, tagHeavyResolver, tagHeavyCoordinator);
      DashboardMetricsRequestDTO tagHeavy = new DashboardMetricsRequestDTO();
      tagHeavy.tagIds = Set.of("tag-1");
      tagHeavy.includeHeavyMetrics = true;

      DashboardMetricsDTO tagHeavyMetrics = tagHeavyService.getMetrics(tagHeavy);

      assertUnsupported(tagHeavyMetrics.violations, "tagIds");
      assertUnsupported(tagHeavyMetrics.components, "tagIds");
      assertUnsupported(tagHeavyMetrics.vulnerabilities, "tagIds");
      assertUnsupported(tagHeavyMetrics.legal, "tagIds");
      verify(tagHeavyResolver, never()).resolve(any());
      verifyNoInteractions(tagHeavyCoordinator);
    }
  }

  @Test
  public void testGetMetrics_ApplicationFilterDoesNotNarrowOrganizationsInOn() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "OFF");
    Organization parent = tempEntity.newOrganization("metrics-parity-parent");
    Organization child = tempEntity.newOrganization("metrics-parity-child", parent);
    Application app = tempEntity.newApplication(child.getId());
    tempEntity.newApplication(child.getId());
    User reader = tempEntity.newUser("metrics-on-organization-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(child.getId(), readRole.getId(), reader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO filteredRequest = new DashboardMetricsRequestDTO();
    filteredRequest.applicationIds = Set.of(app.getId());
    DashboardMetricsDTO off = dashboardMetricsService.getMetrics(filteredRequest);

    tempEntity.deleteSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE);
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "ON");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
    DashboardMetricsDTO on = dashboardMetricsService.getMetrics(filteredRequest);

    assertThat(off.organizations.total).isEqualTo(1);
    assertThat(on.organizations.total).isEqualTo(off.organizations.total);
    assertThat(on.organizations.source).isEqualTo("sql");
  }

  @Test
  public void testGetMetrics_UnreadableApplicationFilterPreservesOrganizationParityInOn() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "OFF");
    Organization readable = tempEntity.newOrganization("metrics-unreadable-app-parity");
    tempEntity.newApplication(readable.getId());
    User reader = tempEntity.newUser("metrics-unreadable-app-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(readable.getId(), readRole.getId(), reader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO filteredRequest = new DashboardMetricsRequestDTO();
    filteredRequest.applicationIds = Set.of("nonexistent-or-unreadable-application");
    DashboardMetricsDTO off = dashboardMetricsService.getMetrics(filteredRequest);

    tempEntity.deleteSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE);
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "ON");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
    DashboardMetricsDTO on = dashboardMetricsService.getMetrics(filteredRequest);

    assertThat(off.organizations.total).isEqualTo(1);
    assertThat(on.organizations.total).isEqualTo(off.organizations.total);
    assertThat(on.organizations.source).isEqualTo("sql");
    assertThat(on.applications.total).isEqualTo(off.applications.total).isZero();
    assertThat(on.policies.total).isEqualTo(off.policies.total).isZero();
    assertThat(on.waivers.total).isEqualTo(off.waivers.total).isZero();
    assertThat(on.violations.total).isEqualTo(off.violations.total).isZero();
  }

  @Test
  public void testGetMetrics_CacheKeyStillExcludesMode() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "OFF");
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    User reader = tempEntity.newUser("mode-cache-user");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    DashboardMetricsDTO off = dashboardMetricsService.getMetrics(request);
    tempEntity.deleteSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE);
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "ON");
    DashboardMetricsDTO cachedAfterModeFlip = dashboardMetricsService.getMetrics(request);

    assertThat(off.applications.source).isEqualTo("index");
    assertThat(cachedAfterModeFlip).isSameAs(off);
    assertThat(cachedAfterModeFlip.applications.source).isEqualTo("index");
  }

  @Test
  public void testGetMetrics_FailsClosed_UserWithNoReadContexts() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());

    User userWithNoPermissions = tempEntity.newUser("user-with-no-permissions");

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(userWithNoPermissions);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.applications.total).isZero();
    assertThat(metrics.applications.source).isEqualTo("index");
    assertThat(metrics.organizations.total).isZero();
    assertThat(metrics.policies.total).isZero();
    assertThat(metrics.vulnerabilities.total).isZero();
    assertThat(metrics.legal.total).isZero();
    assertThat(metrics.waivers.total).isZero();
    assertThat(metrics.waivers.source).isEqualTo("sql");
    assertThat(metrics.waivers.breakdown).containsEntry("existing", 0L);
    assertThat(metrics.waivers.breakdown).containsEntry("requested", 0L);
  }

  @Test
  public void testGetMetrics_PoliciesRespectApplicationFilterForAppScopedPolicies() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    tempEntity.newPolicy(org.getId(), "org-level-policy");
    tempEntity.newPolicy(app1.getId(), "app1-policy");
    tempEntity.newPolicy(app2.getId(), "app2-policy");

    User reader = tempEntity.newUser("metrics-policy-filter-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    assertThat(dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO()).policies.total).isEqualTo(3);

    DashboardMetricsRequestDTO filterByApp1 = new DashboardMetricsRequestDTO();
    filterByApp1.applicationIds = Set.of(app1.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByApp1).policies.total).isEqualTo(1);
  }

  @Test
  public void testGetMetrics_NoIndex_ThrowsConflictException() throws Exception {
    DashboardMetricsTestSupport.runWithoutSearchIndex(
        lookup(InsightWork.class).getSearchIndexDir(),
        () -> assertThatExceptionOfType(ConflictException.class)
            .isThrownBy(() -> dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO()))
            .withMessageContaining("Search index not found")
            .withMessageContaining("Re-indexing is required")
            .satisfies(ex -> {
              assertThat(ex.getMessage()).doesNotContain("Exception");
              assertThat(ex.getMessage()).doesNotContain("query");
              assertThat(ex.getMessage()).doesNotContain("stack");
              assertThat(ex.getMessage()).doesNotContain("at com.");
            }));
  }

  @Test
  public void testGetMetrics_CoalescingCacheWithinTtl() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("cache-test-user", "cache-test-user", User.INTERNAL_REALM_ID));
    when(searchIndexClient.count(anyString())).thenReturn(7L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(99_000L);

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    DashboardMetricsDTO first = service.getMetrics(request);
    DashboardMetricsDTO second = service.getMetrics(request);

    assertThat(first.applications.total).isEqualTo(7);
    assertThat(second.applications.total).isEqualTo(7);
    // loadMetrics runs once (coalesced): count() x3, countDistinct() x10 (components x2, vulnerabilities x5, legal x3).
    verify(searchIndexClient, times(3)).count(anyString());
    verify(searchIndexClient, times(10)).countDistinct(anyString(), any());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any());
    verify(searchIndexClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testCacheTtl_RetainsFiveSecondFreshnessContract() {
    assertThat(ReflectionTestUtils.getField(DashboardMetricsService.class, "CACHE_TTL"))
        .isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  public void testGetMetrics_NullRequestReturnsCompleteCompatibilityPayload() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(7L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(99_000L);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("null-request-user", "null-request-user", User.INTERNAL_REALM_ID));
    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsDTO metrics = service.getMetrics(null);

    assertThat(metrics.applications).isNotNull();
    assertThat(metrics.organizations).isNotNull();
    assertThat(metrics.policies).isNotNull();
    assertThat(metrics.waivers).isNotNull();
    assertThat(metrics.lastUpdatedAt).isNotNull();
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.components).isNotNull();
    assertThat(metrics.vulnerabilities).isNotNull();
    assertThat(metrics.legal).isNotNull();
    verify(searchIndexClient, times(3)).count(anyString());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any());
    verify(searchIndexClient, times(10)).countDistinct(anyString(), any());
    verify(searchIndexClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testGetMetrics_CacheSeparatesSummaryHeavyAndCompatibilityLoads() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(4L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(88_000L);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("tier-cache-user", "tier-cache-user", User.INTERNAL_REALM_ID));
    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO summaryRequest = new DashboardMetricsRequestDTO();
    summaryRequest.includeHeavyMetrics = false;
    DashboardMetricsRequestDTO heavyRequest = new DashboardMetricsRequestDTO();
    heavyRequest.includeHeavyMetrics = true;

    DashboardMetricsDTO summary = service.getMetrics(summaryRequest);
    DashboardMetricsDTO heavy = service.getMetrics(heavyRequest);
    DashboardMetricsDTO compatibility = service.getMetrics(null);

    assertThat(summary.applications).isNotNull();
    assertThat(summary.violations).isNull();
    assertThat(heavy.applications).isNull();
    assertThat(heavy.violations).isNotNull();
    assertThat(compatibility.applications).isNotNull();
    assertThat(compatibility.violations).isNotNull();

    service.getMetrics(summaryRequest);
    service.getMetrics(heavyRequest);
    service.getMetrics(null);

    verify(searchIndexClient, times(6)).count(anyString());
    verify(searchIndexClient, times(2)).aggregateCountByField(anyString(), anyString(), any());
    verify(searchIndexClient, times(20)).countDistinct(anyString(), any());
    verify(searchIndexClient, times(3)).getLastIndexTime();
  }

  @Test
  public void testGetMetrics_SummaryTierSkipsHeavyIndexComputations() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(1L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("summary-user", "summary-user", User.INTERNAL_REALM_ID));

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications).isNotNull();
    assertThat(metrics.violations).isNull();
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any());
    verify(searchIndexClient, never()).countDistinct(anyString(), any());
  }

  @Test
  public void testGetMetrics_HeavyTierSkipsSummaryComputations() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(42L);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("heavy-user", "heavy-user", User.INTERNAL_REALM_ID));

    PolicyWaiverDAO policyWaiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO policyWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        mock(DashboardMetricsSqlModeProvider.class),
        mock(DashboardMetricsSqlReadiness.class),
        mock(DashboardMetricsScopeResolver.class),
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mock(OrganizationDAO.class), mockConfiguration()),
        mockConfiguration(),
        mock(StageTypeService.class),
        currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = true;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications).isNull();
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.lastUpdatedAt).isNotNull();
    verify(searchIndexClient, never()).count(anyString());
    verify(searchIndexClient, times(1)).getLastIndexTime();
    verify(policyWaiverDAO, never()).selectCount(any());
    verify(policyWaiverRequestDAO, never()).selectCount(any());
  }

  @Test
  public void testGetMetrics_StageFilteredSummaryReturnsUnavailableMetricsWithoutComputingBroaderScope() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("stage-filter-user", "stage-filter-user", User.INTERNAL_REALM_ID));

    PolicyWaiverDAO policyWaiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO policyWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        mock(DashboardMetricsSqlModeProvider.class),
        mock(DashboardMetricsSqlReadiness.class),
        mock(DashboardMetricsScopeResolver.class),
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mock(OrganizationDAO.class), mockConfiguration()),
        mockConfiguration(),
        mock(StageTypeService.class),
        currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertUnsupported(metrics.applications, "stageIds");
    assertUnsupported(metrics.organizations, "stageIds");
    assertUnsupported(metrics.policies, "stageIds");
    assertUnsupported(metrics.waivers, "stageIds");
    assertThat(metrics.violations).isNull();
    verify(searchIndexClient, never()).count(anyString());
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any());
    verify(searchIndexClient, never()).countDistinct(anyString(), any());
    verify(policyWaiverDAO, never()).selectCount(any());
    verify(policyWaiverRequestDAO, never()).selectCount(any());
  }

  @Test
  public void testGetMetrics_TagFilteredSummaryComputesWaiversButNotUnsupportedIndexMetrics() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("tag-filter-user", "tag-filter-user", User.INTERNAL_REALM_ID));

    PolicyWaiverDAO policyWaiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO policyWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    DashboardMetricsScopeResolver scopeResolver = mock(DashboardMetricsScopeResolver.class);
    when(scopeResolver.resolve(any())).thenReturn(
        new ResolvedScope(
            ResolvedScope.Kind.RESTRICTED,
            null,
            Set.of("tagged-owner"),
            Set.of("tagged-owner"),
            Set.of(),
            Set.of(),
            true));
    when(policyWaiverDAO.selectCount(Set.of("tagged-owner"))).thenReturn(2L);
    when(policyWaiverRequestDAO.selectCount(Set.of("tagged-owner"))).thenReturn(3L);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        offModeProvider(),
        offSqlReadiness(),
        scopeResolver,
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mock(OrganizationDAO.class), mockConfiguration()),
        mockConfiguration(),
        mock(StageTypeService.class),
        currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("tag-1");
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertUnsupported(metrics.applications, "tagIds");
    assertUnsupported(metrics.organizations, "tagIds");
    assertUnsupported(metrics.policies, "tagIds");
    assertThat(metrics.waivers.total).isEqualTo(5);
    assertThat(metrics.waivers.breakdown).containsEntry("existing", 2L).containsEntry("requested", 3L);
    verify(searchIndexClient, never()).count(anyString());
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any());
    verify(searchIndexClient, never()).countDistinct(anyString(), any());
    verify(scopeResolver, times(1)).resolve(request);
  }

  @Test
  public void testGetMetrics_StageAndTagFilteredHeavyTierReturnsUnavailableWithoutIndexComputations() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("heavy-filter-user", "heavy-filter-user", User.INTERNAL_REALM_ID));
    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);
    request.tagIds = Set.of("tag-1");
    request.includeHeavyMetrics = true;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications).isNull();
    assertUnsupported(metrics.violations, "stageIds", "tagIds");
    assertUnsupported(metrics.components, "stageIds", "tagIds");
    assertUnsupported(metrics.vulnerabilities, "stageIds", "tagIds");
    assertUnsupported(metrics.legal, "stageIds", "tagIds");
    verify(searchIndexClient, never()).count(anyString());
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any());
    verify(searchIndexClient, never()).countDistinct(anyString(), any());
  }

  @Test
  public void testCacheKey_DifferentiatesAllMetricTiers() {
    UserPrincipal principal =
        new UserPrincipal("cache-tier-user", "cache-tier-user", User.INTERNAL_REALM_ID);
    DashboardMetricsRequestDTO compatibilityRequest = new DashboardMetricsRequestDTO();
    DashboardMetricsRequestDTO summaryRequest = new DashboardMetricsRequestDTO();
    summaryRequest.includeHeavyMetrics = false;
    DashboardMetricsRequestDTO heavyRequest = new DashboardMetricsRequestDTO();
    heavyRequest.includeHeavyMetrics = true;

    DashboardMetricsCacheKey compatibilityKey =
        DashboardMetricsCacheKey.forRequest(principal, compatibilityRequest);
    DashboardMetricsCacheKey summaryKey = DashboardMetricsCacheKey.forRequest(principal, summaryRequest);
    DashboardMetricsCacheKey heavyKey = DashboardMetricsCacheKey.forRequest(principal, heavyRequest);

    assertThat(List.of(compatibilityKey, summaryKey, heavyKey)).doesNotHaveDuplicates();
  }

  @Test
  public void testCacheKey_DifferentiatesStageAndTagFiltersFromUnfilteredRequests() {
    UserPrincipal principal =
        new UserPrincipal("cache-filter-user", "cache-filter-user", User.INTERNAL_REALM_ID);
    DashboardMetricsRequestDTO unfilteredRequest = new DashboardMetricsRequestDTO();
    DashboardMetricsRequestDTO stageRequest = new DashboardMetricsRequestDTO();
    stageRequest.stageIds = Set.of(Stage.ID_BUILD);
    DashboardMetricsRequestDTO tagRequest = new DashboardMetricsRequestDTO();
    tagRequest.tagIds = Set.of("tag-1");

    assertThat(List.of(
        DashboardMetricsCacheKey.forRequest(principal, unfilteredRequest),
        DashboardMetricsCacheKey.forRequest(principal, stageRequest),
        DashboardMetricsCacheKey.forRequest(principal, tagRequest)))
            .doesNotHaveDuplicates();
  }

  @Test
  public void testGetMetrics_CacheKeyDifferentiatesNullRealmFromExplicitRealm() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(3L, 11L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    UserPrincipal nullRealmPrincipal = new UserPrincipal("shared-user", "shared-user", null);
    UserPrincipal explicitRealmPrincipal =
        new UserPrincipal("shared-user", "shared-user", User.INTERNAL_REALM_ID);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal())
        .thenReturn(nullRealmPrincipal, explicitRealmPrincipal, nullRealmPrincipal, explicitRealmPrincipal);

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    assertThat(service.getMetrics(request).applications.total).isEqualTo(3);
    assertThat(service.getMetrics(request).applications.total).isEqualTo(11);

    service.getMetrics(request);
    service.getMetrics(request);

    // Two cache misses x loadMetrics: count() x3, countDistinct() x10 per miss.
    verify(searchIndexClient, times(6)).count(anyString());
    verify(searchIndexClient, times(20)).countDistinct(anyString(), any());
    verify(searchIndexClient, times(2)).aggregateCountByField(anyString(), anyString(), any());
  }

  @Test
  public void testGetMetrics_CacheKeyDifferentiatesSameUsernameDifferentRealm() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(5L, 9L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    UserPrincipal internalPrincipal =
        new UserPrincipal("shared-user", "shared-user", User.INTERNAL_REALM_ID);
    UserPrincipal ldapPrincipal =
        new UserPrincipal("shared-user", "shared-user", "ldap-realm-id");

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal())
        .thenReturn(internalPrincipal, ldapPrincipal, internalPrincipal, ldapPrincipal);

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    assertThat(service.getMetrics(request).applications.total).isEqualTo(5);
    assertThat(service.getMetrics(request).applications.total).isEqualTo(9);

    service.getMetrics(request);
    service.getMetrics(request);

    // Two cache misses x loadMetrics: count() x3, countDistinct() x10 per miss.
    verify(searchIndexClient, times(6)).count(anyString());
    verify(searchIndexClient, times(20)).countDistinct(anyString(), any());
    verify(searchIndexClient, times(2)).aggregateCountByField(anyString(), anyString(), any());
  }

  @Test
  public void testGetMetrics_CacheCoalescesAcrossEmptyFilterRequests() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));
    when(searchIndexClient.count(anyString())).thenReturn(2L);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO requestA = new DashboardMetricsRequestDTO();
    DashboardMetricsRequestDTO requestB = new DashboardMetricsRequestDTO();

    assertThat(service.getMetrics(requestA).applications.total).isEqualTo(2);
    assertThat(service.getMetrics(requestB).applications.total).isEqualTo(2);

    service.getMetrics(requestA);
    service.getMetrics(requestB);

    // Empty filter requests share one cache key => loadMetrics runs once (coalesced).
    verify(searchIndexClient, times(3)).count(anyString());
    verify(searchIndexClient, times(10)).countDistinct(anyString(), any());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any());
  }

  private static void stubEmptySearchIndexResults(SearchIndexClient searchIndexClient) {
    when(searchIndexClient.aggregateCountByField(anyString(), anyString(), any())).thenReturn(
        new MetricAggregationResult(0L, Map.of("critical", 0L, "severe", 0L, "moderate", 0L, "low", 0L)));
    when(searchIndexClient.countDistinct(anyString(), any())).thenReturn(0L);
  }

  private static void assertUnsupported(Object metric, String... unsupportedDimensions) {
    assertThat(metric).extracting("total").isNull();
    assertThat(metric).extracting("errorCode").isEqualTo("UNSUPPORTED_FILTER_COMBINATION");
    assertThat(metric).extracting("unsupportedDimensions").isEqualTo(List.of(unsupportedDimensions));
  }

  private void seedComponentsReport(Application app, String scanId) throws Exception {
    seedComponentsReport(app, Stage.ID_BUILD, scanId);
  }

  private void seedComponentsReport(Application app, String stageId, String scanId) throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageId, scanId);
    ReportTestUtils.createReportFile(evaluation.getApplicationId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/componentsMetricReport", tempDir), lookup(InsightWork.class));
  }

  private void seedPolicyViolation(
      Organization org,
      Application app,
      String scanId,
      int threatLevel) throws Exception
  {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ReportTestUtils.createReportFile(evaluation.getApplicationId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir), lookup(InsightWork.class));
    Policy policy = tempEntity.newPolicy(org.getId(), "Security - Critical " + scanId);
    tempEntity.newPolicyViolation(evaluation, policy, threatLevel, PolicyThreatCategory.SECURITY,
        "com.example", "artifact", "1.0", DashboardMetricsTestSupport.violationComponentHash(scanId));
  }

  private static void assertIndexSourcedMetric(DashboardMetricsDTO metrics) {
    assertThat(metrics.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);
    assertThat(metrics.applications.breakdown).containsKey("stages");
    assertThat(metrics.lastUpdatedAt).isNotNull();
  }

  private static Configuration mockConfiguration() {
    return mock(Configuration.class);
  }

  private static DashboardMetricsSqlModeProvider offModeProvider() {
    DashboardMetricsSqlModeProvider modeProvider = mock(DashboardMetricsSqlModeProvider.class);
    when(modeProvider.configuredMode()).thenReturn(DashboardMetricsSqlMode.OFF);
    return modeProvider;
  }

  private static DashboardMetricsSqlReadiness offSqlReadiness() {
    DashboardMetricsSqlReadiness readiness = mock(DashboardMetricsSqlReadiness.class);
    when(readiness.effectiveMode(any())).thenReturn(DashboardMetricsSqlMode.OFF);
    return readiness;
  }

  private static DashboardMetricsScopeResolver queryableScopeResolver() {
    DashboardMetricsScopeResolver scopeResolver = mock(DashboardMetricsScopeResolver.class);
    lenient().when(scopeResolver.resolve(any())).thenReturn(queryableScope());
    return scopeResolver;
  }

  private static ResolvedScope queryableScope() {
    return new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        Set.of("owner-1"),
        Set.of("owner-1"),
        Set.of("organization-1"),
        Set.of("application-1"),
        false);
  }

  private static MetricValueDTO sqlMetric(final long total) {
    return new MetricValueDTO(total, null, "sql");
  }

  private static DashboardMetricsSqlCoordinator sqlCoordinatorReturning(final long total) {
    ResolvedScope scope = queryableScope();
    DashboardMetricsSqlCoordinator coordinator = mock(DashboardMetricsSqlCoordinator.class);
    lenient().when(coordinator.countApplications(scope)).thenReturn(sqlMetric(total));
    lenient().when(coordinator.countOrganizations(scope)).thenReturn(sqlMetric(total));
    lenient().when(coordinator.countPolicies(scope)).thenReturn(sqlMetric(total));
    lenient().when(coordinator.countViolations(scope)).thenReturn(sqlMetric(total));
    return coordinator;
  }

  private static DashboardMetricsService newSqlModeService(
      final SearchIndexClient searchIndexClient,
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyWaiverRequestDAO policyWaiverRequestDAO,
      final DashboardMetricsScopeResolver scopeResolver,
      final DashboardMetricsSqlCoordinator coordinator)
  {
    return newModeService(
        DashboardMetricsSqlMode.ON,
        searchIndexClient,
        policyWaiverDAO,
        policyWaiverRequestDAO,
        scopeResolver,
        coordinator);
  }

  private static DashboardMetricsService newModeService(
      final DashboardMetricsSqlMode mode,
      final DashboardMetricsScopeResolver scopeResolver,
      final DashboardMetricsSqlCoordinator coordinator)
  {
    PolicyWaiverDAO policyWaiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO policyWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    lenient().when(policyWaiverDAO.selectCount(any())).thenReturn(0L);
    lenient().when(policyWaiverRequestDAO.selectCount(any())).thenReturn(0L);
    return newModeService(
        mode,
        mock(SearchIndexClient.class),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        scopeResolver,
        coordinator);
  }

  private static DashboardMetricsService newModeService(
      final DashboardMetricsSqlMode mode,
      final SearchIndexClient searchIndexClient,
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyWaiverRequestDAO policyWaiverRequestDAO,
      final DashboardMetricsScopeResolver scopeResolver,
      final DashboardMetricsSqlCoordinator coordinator)
  {
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("sql-mode-user", "sql-mode-user", User.INTERNAL_REALM_ID));
    DashboardMetricsSqlModeProvider modeProvider = mock(DashboardMetricsSqlModeProvider.class);
    when(modeProvider.configuredMode()).thenReturn(mode);
    DashboardMetricsSqlReadiness readiness = mock(DashboardMetricsSqlReadiness.class);
    when(readiness.effectiveMode(mode)).thenReturn(mode);
    return new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        modeProvider,
        readiness,
        scopeResolver,
        coordinator,
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mock(OrganizationDAO.class), mockConfiguration()),
        mockConfiguration(),
        mock(StageTypeService.class),
        currentUser);
  }

  private static void assertUnavailable(final MetricValueDTO metric) {
    assertThat(metric.total).isNull();
    assertThat(metric.source).isEqualTo("sql");
    assertThat(metric.errorCode).isEqualTo(MetricValueDTO.METRIC_UNAVAILABLE);
  }

  private static DashboardMetricsService newServiceWithMocks(
      SearchIndexClient searchIndexClient,
      MetricFilterValidator metricFilterValidator,
      OrganizationDAO organizationDAO,
      Configuration configuration,
      CurrentUser currentUser)
  {
    return newServiceWithMocks(
        searchIndexClient,
        metricFilterValidator,
        organizationDAO,
        configuration,
        mock(StageTypeService.class),
        currentUser);
  }

  private static DashboardMetricsService newServiceWithMocks(
      SearchIndexClient searchIndexClient,
      MetricFilterValidator metricFilterValidator,
      OrganizationDAO organizationDAO,
      Configuration configuration,
      StageTypeService stageTypeService,
      CurrentUser currentUser)
  {
    PolicyWaiverDAO policyWaiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO policyWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    lenient().when(policyWaiverDAO.selectCount(any())).thenReturn(0L);
    lenient().when(policyWaiverRequestDAO.selectCount(any())).thenReturn(0L);
    lenient()
        .when(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT))
        .thenReturn(List.of());
    return new DashboardMetricsService(
        searchIndexClient,
        metricFilterValidator,
        policyWaiverDAO,
        policyWaiverRequestDAO,
        offModeProvider(),
        offSqlReadiness(),
        queryableScopeResolver(),
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration),
        configuration,
        stageTypeService,
        currentUser);
  }

  private void loginAs(final User user) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(user.getUsername(), user.getUsername(), User.INTERNAL_REALM_ID),
        User.INTERNAL_REALM_ID);

    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());

    Subject authenticatedSubject = new Subject.Builder(lookup(SecurityManager.class))
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(authenticatedSubject);
  }
}

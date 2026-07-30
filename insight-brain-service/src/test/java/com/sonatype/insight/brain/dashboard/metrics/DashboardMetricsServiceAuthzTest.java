/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsScopeResolver;
import com.sonatype.insight.brain.dashboard.metrics.sql.ResolvedScope;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationMoveService;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import jakarta.inject.Inject;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * RBAC isolation for dashboard metrics (CLM-40927 Task 8).
 */
public class DashboardMetricsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private DashboardMetricsService dashboardMetricsService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Inject
  private ApplicationMoveService applicationMoveService;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private Organization orgA;

  private Organization orgB;

  private User userA;

  private User userB;

  private Application orgAApp;

  private Application orgALegalApp;

  private Policy orgAPolicy;

  @Before
  public void setUpClient() {
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
  }

  @Override
  protected void setUpSecurity() {
    orgA = tempEntity.newOrganization();
    orgB = tempEntity.newOrganization();
    orgAApp = tempEntity.newApplication(orgA.getId());
    orgALegalApp = tempEntity.newApplication(orgA.getId());
    tempEntity.newApplication(orgB.getId());
    tempEntity.newApplication(orgB.getId());
    tempEntity.newApplication(orgB.getId());

    orgAPolicy = tempEntity.newPolicy(orgA.getId(), "org-a-policy");
    tempEntity.newPolicy(orgB.getId(), "org-b-policy-1");
    tempEntity.newPolicy(orgB.getId(), "org-b-policy-2");

    userA = tempEntity.newUser();
    userB = tempEntity.newUser();

    Role roleA = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(orgA.getId(), roleA.getId(), userA.getUsername());
    Role roleB = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(orgB.getId(), roleB.getId(), userB.getUsername());

    subject = new Subject.Builder(lookup(SecurityManager.class)).buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(subject);
  }

  @Test
  public void testGetMetrics_RbacIsolation_ScopesCountsToReadableOrganizations() throws Exception {
    seedComponentsReport(orgAApp, "authzOrgAComponents");
    seedPolicyViolation(orgALegalApp, orgAPolicy, "authzOrgALegal", 3);
    tempEntity.newWaiver("authzOrgAWaiver", orgAPolicy.getId(), orgAApp.getId());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);
    DashboardMetricsDTO metricsForUserA = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());
    assertThat(metricsForUserA.applications.total).isEqualTo(2);
    assertThat(metricsForUserA.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);
    assertThat(metricsForUserA.organizations.total).isEqualTo(1);
    assertThat(metricsForUserA.policies.total).isEqualTo(1);
    // orgAApp componentsMetricReport => 4 distinct CVEs; orgALegalApp policyViolationReport => +1 CVE.
    assertThat(metricsForUserA.vulnerabilities.total).isEqualTo(5);
    // policyViolationReport licenses => 7 distinct (app, component, license) obligations on orgALegalApp.
    assertThat(metricsForUserA.legal.total).isEqualTo(7);
    assertThat(metricsForUserA.waivers.total).isEqualTo(1);
    assertThat(metricsForUserA.waivers.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);

    loginAs(userB);
    DashboardMetricsDTO metricsForUserB = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());
    assertThat(metricsForUserB.applications.total).isEqualTo(3);
    assertThat(metricsForUserB.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);
    assertThat(metricsForUserB.organizations.total).isEqualTo(1);
    assertThat(metricsForUserB.policies.total).isEqualTo(2);
    assertThat(metricsForUserB.vulnerabilities.total).isZero();
    assertThat(metricsForUserB.legal.total).isZero();
    assertThat(metricsForUserB.waivers.total).isZero();
    assertThat(metricsForUserB.waivers.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  @Test
  public void testGetMetrics_OnRbacIsolationScopesSqlMetricsToReadableOrganizations() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, "ON");
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);
    DashboardMetricsDTO metricsForUserA = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());
    loginAs(userB);
    DashboardMetricsDTO metricsForUserB = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metricsForUserA.applications.total).isEqualTo(2);
    assertThat(metricsForUserB.applications.total).isEqualTo(3);
    assertThat(metricsForUserA.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
    assertThat(metricsForUserB.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilterCannotProbeUnreadableOrg() {
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);

    DashboardMetricsRequestDTO filterToUnreadableOrg = new DashboardMetricsRequestDTO();
    filterToUnreadableOrg.organizationIds = Set.of(orgB.getId());
    DashboardMetricsDTO unreadableOrgMetrics = dashboardMetricsService.getMetrics(filterToUnreadableOrg);
    assertThat(unreadableOrgMetrics.applications.total).isZero();
    assertThat(unreadableOrgMetrics.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);

    DashboardMetricsRequestDTO filterToReadableOrg = new DashboardMetricsRequestDTO();
    filterToReadableOrg.organizationIds = Set.of(orgA.getId());
    assertThat(dashboardMetricsService.getMetrics(filterToReadableOrg).applications.total).isEqualTo(2);
  }

  @Test
  public void testGetMetrics_ExplicitApplicationFilterCannotProbeUnreadableApp() {
    String unreadableAppId = tempEntity.newApplication(orgB.getId()).getId();
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);

    DashboardMetricsRequestDTO filterToUnreadableApp = new DashboardMetricsRequestDTO();
    filterToUnreadableApp.applicationIds = Set.of(unreadableAppId);
    assertThat(dashboardMetricsService.getMetrics(filterToUnreadableApp).applications.total).isZero();
  }

  @Test
  public void testGetMetrics_GlobalReadWithApplicationFilterDoesNotBroadenWaiverScope() {
    Application globalAppA = tempEntity.newApplication(orgA.getId());
    Application globalAppB = tempEntity.newApplication(orgB.getId());
    tempEntity.newWaiver("global-filter-waiver-a", orgAPolicy.getId(), globalAppA.getId());
    tempEntity.newWaiver("global-filter-waiver-b", orgAPolicy.getId(), globalAppB.getId());
    User globalReader = tempEntity.newUser();
    Role globalReadRole = tempEntity.newRole(true /* global */, Permission.READ);
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, globalReadRole.getId(), globalReader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(globalReader);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.applicationIds = Set.of(globalAppA.getId());

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(request);

    assertThat(metrics.waivers.total).isEqualTo(1);
    assertThat(metrics.waivers.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  @Test
  public void testGetMetrics_TagFilteredPrincipalIncludesOnlyAuthorizedApplications() {
    Application readableApp = orgAApp;
    Application unreadableApp = tempEntity.newApplication(orgB.getId());
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "shared-dashboard-tag");
    tempEntity.newApplicationTag(readableApp.getId(), tag.getId());
    tempEntity.newApplicationTag(unreadableApp.getId(), tag.getId());
    tempEntity.newWaiver("readable-tagged-waiver", orgAPolicy.getId(), readableApp.getId());
    tempEntity.newWaiver("unreadable-tagged-waiver", orgAPolicy.getId(), unreadableApp.getId());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of(tag.getId());

    ResolvedScope scope = lookup(DashboardMetricsScopeResolver.class).resolve(request);
    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(request);

    assertThat(scope.applicationIds()).contains(readableApp.getId()).doesNotContain(unreadableApp.getId());
    assertThat(metrics.applications.unsupportedDimensions).containsExactly("tagIds");
    assertThat(metrics.waivers.total).isEqualTo(1);
    assertThat(metrics.waivers.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  @Test
  public void testGetMetrics_MovedApplicationUsesCurrentSqlAuthorizationTruth() {
    Organization moveSource = tempEntity.newOrganization("dashboard-move-source");
    Organization moveDestination = tempEntity.newOrganization("dashboard-move-destination");
    Application movedApplication = tempEntity.newApplication(moveSource.getId());
    Role sourceReadRole = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(moveSource.getId(), sourceReadRole.getId(), userA.getUsername());
    User mover = tempEntity.newUser("dashboard-move-authorizer");
    Role moveRole = tempEntity.newRole(true, Permission.READ, Permission.WRITE, Permission.ADD_APPLICATION);
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, moveRole.getId(), mover.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.applicationIds = Set.of(movedApplication.getId());
    loginAs(userA);
    assertThat(metricsInMode("OFF", request).applications.total).isEqualTo(1);

    loginAs(mover);
    applicationMoveService.moveApplication(movedApplication.getId(), moveDestination.getId());

    loginAs(userA);
    DashboardMetricsDTO staleIndex = metricsInMode("OFF", request);
    DashboardMetricsDTO currentSql = metricsInMode("ON", request);

    assertThat(staleIndex.applications.total).isEqualTo(1);
    assertThat(currentSql.applications.total).isZero();
    assertThat(currentSql.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  @Test
  public void testGetMetrics_JustRevokedGrantUsesCurrentSqlAuthorizationTruth() {
    Application revokedApplication = tempEntity.newApplication(orgB.getId());
    User revokedReader = tempEntity.newUser("dashboard-revoked-reader");
    Role revokedRole = tempEntity.newRole(false, Permission.READ);
    MembershipMapping grant = tempEntity.newMembershipMapping(
        revokedApplication.getId(), revokedRole.getId(), revokedReader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.applicationIds = Set.of(revokedApplication.getId());
    loginAs(revokedReader);
    assertThat(metricsInMode("OFF", request).applications.total).isEqualTo(1);

    membershipMappingDAO.setMembershipMappingsForContextAndRole(
        grant.getContextId(), grant.getRoleId(), List.of());
    loginAs(revokedReader);

    DashboardMetricsDTO staleIndex = metricsInMode("OFF", request);
    DashboardMetricsDTO currentSql = metricsInMode("ON", request);

    assertThat(staleIndex.applications.total).isBetween(0L, 1L);
    assertThat(currentSql.applications.total).isZero();
    assertThat(currentSql.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  @Test
  public void testGetMetrics_DirectApplicationGrantHasAuthorizationParity() {
    // Tag-mediated authorization has no repository API in this test harness, so it cannot be exercised here.
    // This fixture pins the observable direct-application authorization boundary only.
    Application directlyGrantedApplication = tempEntity.newApplication(orgB.getId());
    User directApplicationReader = tempEntity.newUser("dashboard-direct-application-reader");
    Role applicationReadRole = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(
        directlyGrantedApplication.getId(), applicationReadRole.getId(), directApplicationReader.getUsername());
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(directApplicationReader);
    DashboardMetricsDTO off = metricsInMode("OFF", new DashboardMetricsRequestDTO());
    DashboardMetricsDTO on = metricsInMode("ON", new DashboardMetricsRequestDTO());

    assertThat(off.applications.total).isEqualTo(1);
    assertThat(on.applications.total).isEqualTo(off.applications.total);
    assertThat(on.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_SQL);
  }

  private DashboardMetricsDTO metricsInMode(
      final String mode,
      final DashboardMetricsRequestDTO request)
  {
    tempEntity.deleteSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE);
    tempEntity.newSystemConfigurationProperty(DASHBOARD_METRICS_SQL_MODE, mode);
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
    return dashboardMetricsService.getMetrics(request);
  }

  private void loginAs(User user) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(user.getUsername(), user.getUsername(), User.INTERNAL_REALM_ID),
        User.INTERNAL_REALM_ID);

    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());

    subject = new Subject.Builder(lookup(SecurityManager.class))
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(subject);
  }

  private void seedComponentsReport(Application app, String scanId) throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/componentsMetricReport", tempDir), lookup(InsightWork.class));
  }

  private void seedPolicyViolation(
      Application app,
      Policy policy,
      String scanId,
      int threatLevel) throws Exception
  {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir), lookup(InsightWork.class));
    tempEntity.newPolicyViolation(evaluation, policy, threatLevel, PolicyThreatCategory.SECURITY,
        "com.example", "artifact", "1.0", DashboardMetricsTestSupport.violationComponentHash(scanId));
  }
}

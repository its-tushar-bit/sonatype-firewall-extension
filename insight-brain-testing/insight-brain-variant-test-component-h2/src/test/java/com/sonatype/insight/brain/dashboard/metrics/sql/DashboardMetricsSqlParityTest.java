/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsDTO;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsRequestDTO;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsService;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsTestSupport;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import jakarta.inject.Inject;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Synchronized SQL/index acceptance parity for dashboard metrics.
 */
@ComponentH2Test
public class DashboardMetricsSqlParityTest
    extends AbstractComponentH2Test
{
  @Inject
  private DashboardMetricsService dashboardMetricsService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @BeforeEach
  public void setUpClient() {
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
  }

  @Test
  public void synchronizedViolationPopulationsAreAnswerIdentical() throws Exception {
    Organization organization = tempEntity.newOrganization("sql-parity-violations");
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization.getId(), "sql-parity-policy");
    // Split fixtures across stages so SQL (null stage filter) and index (unscoped) agree under multi-stage data.
    PolicyEvaluation buildEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "sqlParityViolationPopulationBuild");
    PolicyEvaluation releaseEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "sqlParityViolationPopulationRelease");
    ReportTestUtils.createReportFile(buildEvaluation.getOwnerId(), buildEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir), lookup(InsightWork.class));
    ReportTestUtils.createReportFile(releaseEvaluation.getOwnerId(), releaseEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir), lookup(InsightWork.class));

    unfixedOpenViolation(buildEvaluation, policy, 0, "sql-parity-open-0");
    unfixedOpenViolation(releaseEvaluation, policy, 1, "sql-parity-open-1");
    unfixedWaivedViolation(buildEvaluation, policy, 2, "sql-parity-waived-2");
    unfixedWaivedViolation(releaseEvaluation, policy, 3, "sql-parity-waived-3");
    unfixedLegacyViolation(buildEvaluation, policy, 4, "sql-parity-legacy-4");
    unfixedLegacyViolation(releaseEvaluation, policy, 7, "sql-parity-legacy-7");
    unfixedOpenViolation(buildEvaluation, policy, 8, "sql-parity-open-8");
    unfixedWaivedViolation(releaseEvaluation, policy, 10, "sql-parity-waived-10");
    fixedViolation(buildEvaluation, policy, 8, "fixed-excluded-8");
    fixedViolation(releaseEvaluation, policy, 10, "fixed-excluded-10");
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    DashboardMetricsDTO off = metricsInMode("OFF", new DashboardMetricsRequestDTO());
    DashboardMetricsDTO on = metricsInMode("ON", new DashboardMetricsRequestDTO());

    assertMigratedMetricsEqual(off, on);
    assertThat(off.violations.total).isEqualTo(8);
    assertThat(off.violations.breakdown)
        .containsEntry("low", 2L)
        .containsEntry("moderate", 2L)
        .containsEntry("severe", 2L)
        .containsEntry("critical", 2L);
  }

  @Test
  public void synchronizedPrincipalAndFilterMatrixIsAnswerIdentical() {
    Organization parent = tempEntity.newOrganization("sql-parity-parent");
    Organization child = tempEntity.newOrganization("sql-parity-child", parent);
    Organization other = tempEntity.newOrganization("sql-parity-other");
    tempEntity.newApplication(parent.getId());
    tempEntity.newApplication(child.getId());
    tempEntity.newApplication(child.getId());
    Application otherApp = tempEntity.newApplication(other.getId());
    Application directlyGrantedApp = tempEntity.newApplication(other.getId());

    tempEntity.newPolicy(child.getId(), "sql-parity-child-policy");
    tempEntity.newPolicy(other.getId(), "sql-parity-other-policy");

    User globalReader = reader("sql-parity-global");
    Role globalRead = tempEntity.newRole(true, Permission.READ);
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, globalRead.getId(), globalReader.getUsername());

    User subtreeReader = reader("sql-parity-subtree");
    Role subtreeRead = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(parent.getId(), subtreeRead.getId(), subtreeReader.getUsername());

    User wideReader = reader("sql-parity-wide");
    Role wideRead = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(child.getId(), wideRead.getId(), wideReader.getUsername());
    tempEntity.newMembershipMapping(other.getId(), wideRead.getId(), wideReader.getUsername());

    User directApplicationReader = reader("sql-parity-direct-application");
    Role applicationRead = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(
        directlyGrantedApp.getId(), applicationRead.getId(), directApplicationReader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    SoftAssertions softly = new SoftAssertions();
    assertParity(softly, "global principal", globalReader, new DashboardMetricsRequestDTO());
    assertParity(softly, "subtree principal", subtreeReader, new DashboardMetricsRequestDTO());
    assertParity(softly, "wide-grant principal", wideReader, new DashboardMetricsRequestDTO());
    // Tag-mediated authorization has no repository API in this test harness, so it cannot be exercised here.
    DashboardMetricsDTO directlyGranted = assertParity(
        softly, "direct application principal", directApplicationReader, new DashboardMetricsRequestDTO());
    softly.assertThat(directlyGranted.applications.total).as("directly granted applications").isEqualTo(1);

    DashboardMetricsRequestDTO organizationFilter = new DashboardMetricsRequestDTO();
    organizationFilter.organizationIds = Set.of(child.getId());
    assertParity(softly, "organization filter", globalReader, organizationFilter);

    DashboardMetricsRequestDTO applicationFilter = new DashboardMetricsRequestDTO();
    applicationFilter.applicationIds = Set.of(otherApp.getId());
    assertParity(softly, "application filter", globalReader, applicationFilter);

    DashboardMetricsRequestDTO organizationApplicationUnion = new DashboardMetricsRequestDTO();
    organizationApplicationUnion.organizationIds = Set.of(parent.getId());
    organizationApplicationUnion.applicationIds = Set.of(otherApp.getId());
    assertParity(softly, "organization/application union", globalReader, organizationApplicationUnion);
    softly.assertAll();
  }

  @Test
  public void policyCountsUseDirectDocumentOwnershipNotHierarchyApplicability() {
    Organization selectedOrganization = tempEntity.newOrganization("sql-parity-policy-org");
    Application explicitApplication = tempEntity.newApplication(selectedOrganization.getId());

    tempEntity.newPolicy(selectedOrganization.getId(), "direct-org-policy");
    tempEntity.newPolicy(explicitApplication.getId(), "direct-app-policy");
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "applicable-only-root-policy");
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of(selectedOrganization.getId());
    request.applicationIds = Set.of(explicitApplication.getId());

    DashboardMetricsDTO off = metricsInMode("OFF", request);
    DashboardMetricsDTO on = metricsInMode("ON", request);

    assertThat(off.policies.total).isEqualTo(2);
    assertMigratedMetricsEqual(off, on);
  }

  private PolicyViolation unfixedOpenViolation(
      final PolicyEvaluation evaluation,
      final Policy policy,
      final int threatLevel,
      final String hash)
  {
    return newViolation(evaluation, policy, threatLevel, hash);
  }

  private PolicyViolation unfixedWaivedViolation(
      final PolicyEvaluation evaluation,
      final Policy policy,
      final int threatLevel,
      final String hash)
  {
    PolicyViolation violation = newViolation(evaluation, policy, threatLevel, hash);
    violation.setWaiveTime(new Date());
    tempEntity.updatePolicyViolation(violation);
    return violation;
  }

  private PolicyViolation unfixedLegacyViolation(
      final PolicyEvaluation evaluation,
      final Policy policy,
      final int threatLevel,
      final String hash)
  {
    PolicyViolation violation = newViolation(evaluation, policy, threatLevel, hash);
    violation.setLegacyViolationTime(new Date());
    tempEntity.updatePolicyViolation(violation);
    return violation;
  }

  private PolicyViolation fixedViolation(
      final PolicyEvaluation evaluation,
      final Policy policy,
      final int threatLevel,
      final String hash)
  {
    PolicyViolation violation = newViolation(evaluation, policy, threatLevel, hash);
    violation.setFixTime(new Date());
    tempEntity.updatePolicyViolation(violation);
    return violation;
  }

  private PolicyViolation newViolation(
      final PolicyEvaluation evaluation,
      final Policy policy,
      final int threatLevel,
      final String hash)
  {
    return tempEntity.newPolicyViolation(
        evaluation,
        policy,
        threatLevel,
        PolicyThreatCategory.SECURITY,
        "com.example",
        "artifact-" + threatLevel,
        "1.0",
        hash);
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

  private DashboardMetricsDTO assertParity(
      final SoftAssertions softly,
      final String scenario,
      final User user,
      final DashboardMetricsRequestDTO request)
  {
    loginAs(user);
    DashboardMetricsDTO off = metricsInMode("OFF", request);
    DashboardMetricsDTO on = metricsInMode("ON", request);
    softly.assertThat(on.applications.total).as(scenario + " applications").isEqualTo(off.applications.total);
    softly.assertThat(on.organizations.total).as(scenario + " organizations").isEqualTo(off.organizations.total);
    softly.assertThat(on.policies.total).as(scenario + " policies").isEqualTo(off.policies.total);
    softly.assertThat(on.violations.total).as(scenario + " violations").isEqualTo(off.violations.total);
    softly.assertThat(on.violations.breakdown)
        .as(scenario + " violation breakdown")
        .isEqualTo(off.violations.breakdown);
    softly.assertThat(off.applications.source).as(scenario + " OFF source").isEqualTo("index");
    softly.assertThat(on.applications.source).as(scenario + " ON source").isEqualTo("sql");
    return on;
  }

  private User reader(final String username) {
    return tempEntity.newUser(username);
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

  private static void assertMigratedMetricsEqual(
      final DashboardMetricsDTO index,
      final DashboardMetricsDTO sql)
  {
    assertThat(sql.applications.total).isEqualTo(index.applications.total);
    assertThat(sql.organizations.total).isEqualTo(index.organizations.total);
    assertThat(sql.policies.total).isEqualTo(index.policies.total);
    assertThat(sql.violations.total).isEqualTo(index.violations.total);
    assertThat(sql.violations.breakdown).isEqualTo(index.violations.breakdown);
  }
}

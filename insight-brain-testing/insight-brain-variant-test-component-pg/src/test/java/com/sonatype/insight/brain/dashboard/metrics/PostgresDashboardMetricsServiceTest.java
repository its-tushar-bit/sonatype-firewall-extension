/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
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
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
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
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexFilterRestriction;
import com.sonatype.insight.brain.search.index.IndexTermSetRestriction;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryRequest;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse;
import com.sonatype.insight.brain.search.indexquery.IndexQueryService;
import com.sonatype.insight.brain.search.indexquery.IndexQueryType;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.ConflictException;

import jakarta.inject.Inject;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;

/**
 * Dashboard metrics service: Lucene integration and coalescing cache (CLM-40927).
 */
@ComponentPgTest
public class PostgresDashboardMetricsServiceTest
    extends AbstractComponentPgTest
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
  public void testGetMetrics_WaiversCountAndBreakdownFromSql() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(org.getId());

    tempEntity.newWaiver("hashExisting1", policy.getId(), app.getId());
    tempEntity.newWaiver("hashExisting2", policy.getId(), app.getId());
    Instant expiringWindowUpperBound = DashboardMetricsService.expiringCountUpperBound().toInstant();
    Date expiringSoon = Date.from(expiringWindowUpperBound.minus(1, ChronoUnit.DAYS));
    tempEntity.newWaiver("hashExpiring", policy.getId(), app.getId(), null, "expiring soon", new Date(),
        expiringSoon);
    tempEntity.newWaiver("hashOutsideExpiring", policy.getId(), app.getId(), null, "not expiring soon", new Date(),
        Date.from(expiringWindowUpperBound.plus(1, ChronoUnit.DAYS)));

    // Auto-waivers are indexed alongside manual waivers on the Waivers page (POLICY_WAIVER docs
    // with policyWaiverAuto=true), so the dashboard "existing" total counts them too — otherwise
    // the tile would under-report vs the Waivers list on any tenant with auto-waivers.
    int autoWaiverThreatLevel = 7;
    boolean autoWaiverReachable = true;
    boolean autoWaiverPathForward = false;
    tempEntity.newAutoPolicyWaiver(app.getId(), autoWaiverThreatLevel, autoWaiverReachable, autoWaiverPathForward);

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

    assertThat(metrics.waivers.total).isEqualTo(6);
    assertThat(metrics.waivers.source).isEqualTo("sql");
    // "existing" folds manual + auto (both are already-granted waivers, indexed on the Waivers page).
    // 4 manual waivers (hashExisting1, hashExisting2, hashExpiring, hashOutsideExpiring — all
    // non-expired) plus 1 auto-waiver = 5.
    assertThat(metrics.waivers.breakdown).containsEntry("existing", 5L);
    assertThat(metrics.waivers.breakdown).containsEntry("requested", 1L);
    assertThat(metrics.waivers.breakdown).containsEntry("expiring", 1L);
    assertThat(metrics.waivers.total)
        .isEqualTo(metrics.waivers.breakdown.get("existing") + metrics.waivers.breakdown.get("requested"));

    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    try {
      IndexQueryService indexQueryService = lookup(IndexQueryService.class);
      IndexQueryResponse waivers = indexQueryService.query(
          IndexQueryType.WAIVER, new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true));
      long railExpiringCount = waivers.facets()
          .get("status")
          .stream()
          .filter(bucket -> "expiring".equals(bucket.value()))
          .findFirst()
          .orElseThrow()
          .count();
      assertThat(metrics.waivers.breakdown.get("expiring")).isEqualTo(railExpiringCount);
    }
    finally {
      SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    }
  }

  @Test
  public void testGetMetrics_WaiverExistingCountMatchesWaiversPageExistingSlice() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(org.getId());

    tempEntity.newWaiver("hashActive1", policy.getId(), app.getId());
    tempEntity.newWaiver("hashActive2", policy.getId(), app.getId());

    // Expired manual waiver: excluded by both the SQL tile (EXPIRY_TIME > now()) and the Waivers
    // page default (policyWaiverExpiryStatus in [Active, Never]). Adding it here proves both
    // surfaces symmetrically drop expired waivers rather than one hiding while the other counts.
    Date past = new Date(System.currentTimeMillis() - Duration.ofDays(1).toMillis());
    tempEntity.newWaiver("hashExpired", policy.getId(), app.getId(), "expired-fixture", past);

    int autoWaiverThreatLevel = 7;
    boolean autoWaiverReachable = true;
    boolean autoWaiverPathForward = false;
    tempEntity.newAutoPolicyWaiver(app.getId(), autoWaiverThreatLevel, autoWaiverReachable, autoWaiverPathForward);

    PolicyWaiverRequest requested = new PolicyWaiverRequest("hashRequested", policy.getId(), app.getId(), "pending");
    requested.setStatus(PolicyWaiverRequestStatus.REQUESTED);
    tempEntity.newPolicyWaiverRequest(requested);

    User reader = tempEntity.newUser("metrics-waivers-parity-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    // Query the WAIVER index for the slice this PR reconciles: POLICY_WAIVER docs regardless of
    // {@code policyWaiverAuto}, with the page's default lifecycle filter (every bucket except
    // {@code expired}). The page's {@code existing} filter maps to manual-only
    // ({@code policyWaiverAuto:"false"}) and {@code excluded} maps to auto
    // ({@code policyWaiverAuto:"true"}); folding both gives the same "already granted" universe
    // the dashboard tile's {@code existing} breakdown reports on. Requests and container-image
    // group waivers are outside this slice by design (request-doc parity is pre-existing;
    // container-image group parity is the noted deferral). Nexus One preview must be enabled so
    // {@code IndexQueryService.query()} accepts the WAIVER index-query request.
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    try {
      IndexQueryService indexQueryService = lookup(IndexQueryService.class);
      IndexQueryRequest pageRequest = new IndexQueryRequest(
          "WAIVER",
          Map.of(
              "waiverStates", List.of("existing", "excluded"),
              "lifecycleStatus", List.of("active", "expiring", "auto-waived")),
          1,
          25,
          null,
          null,
          false);
      IndexQueryResponse pageResponse = indexQueryService.query(IndexQueryType.WAIVER, pageRequest);

      // Auto-waivers now fold into the tile's "existing" count (the fix this PR delivers), so
      // the tile and the Waivers-page "already-granted + non-expired" slice agree on 3 waivers.
      Long existingBreakdown = (Long) metrics.waivers.breakdown.get("existing");
      assertThat(existingBreakdown).isEqualTo(pageResponse.totalEstimate());
    }
    finally {
      SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    }
  }

  @Test
  public void testGetMetrics_ViolationsCountAndBreakdownFromIndex() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "violationsMetricsReport");
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
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
    assertThat(metrics.legal.total).isZero();
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
    assertThat(metrics.waivers.breakdown).containsEntry("expiring", 0L);
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
  public void testGetMetrics_StageAndTagFiltersNarrowIndexBackedMetrics() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application buildTaggedApp = tempEntity.newApplication(org.getId());
    Application releaseTaggedApp = tempEntity.newApplication(org.getId());
    Application buildUntaggedApp = tempEntity.newApplication(org.getId());
    Application buildTaggedComponentsApp = tempEntity.newApplication(org.getId());
    Application buildUntaggedComponentsApp = tempEntity.newApplication(org.getId());
    Tag selectedTag = tempEntity.newTag(org.getId(), "metrics-stage-tag");
    Tag otherTag = tempEntity.newTag(org.getId(), "metrics-other-tag");
    tempEntity.newApplicationTag(buildTaggedApp.getId(), selectedTag.getId());
    tempEntity.newApplicationTag(releaseTaggedApp.getId(), selectedTag.getId());
    tempEntity.newApplicationTag(buildTaggedComponentsApp.getId(), selectedTag.getId());
    tempEntity.newApplicationTag(buildUntaggedApp.getId(), otherTag.getId());
    tempEntity.newApplicationTag(buildUntaggedComponentsApp.getId(), otherTag.getId());

    Policy policy = tempEntity.newPolicy(org.getId(), "metrics-stage-tag-policy");
    seedPolicyViolation(org, buildTaggedApp, Stage.ID_BUILD, "stageTagBuild", policy, 10);
    seedPolicyViolation(org, releaseTaggedApp, ReleaseStageType.ID, "stageTagRelease", policy, 8);
    seedPolicyViolation(org, buildUntaggedApp, Stage.ID_BUILD, "stageTagUntagged", policy, 5);
    seedComponentsReport(buildTaggedComponentsApp, Stage.ID_BUILD, "stageTagTaggedComponents");
    seedComponentsReport(buildUntaggedComponentsApp, Stage.ID_BUILD, "stageTagUntaggedComponents");

    User reader = tempEntity.newUser("metrics-stage-tag-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);
    request.tagIds = Set.of(selectedTag.getId());

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(request);

    assertThat(metrics.applications.total).isEqualTo(1);
    assertThat(metrics.applications.errorCode).isNull();
    assertThat(metrics.violations.total).isEqualTo(1);
    // Stage-filtered scanned components are violation-scoped: only the tagged Build violation's
    // hash counts. The tagged app's clean components report at Build must not inflate the total
    // (that was the pre-fix per-component-doc path).
    assertThat(metrics.components.total).isEqualTo(1);
    assertThat(metrics.vulnerabilities.total).isPositive();
    assertThat(metrics.legal.breakdown).containsEntry("applications", 1L);
    assertThat(metrics.waivers.errorCode).isEqualTo("UNSUPPORTED_FILTER_COMBINATION");
    assertThat(metrics.waivers.unsupportedDimensions).containsExactly("stageIds");
  }

  @Test
  public void testGetMetrics_TagFilter_SameCategoryNameAcrossOrgs_UsesTagIdNotName() throws Exception {
    // Categories/tags are per-org and may share display names. Filtering by tag id must not pull
    // apps from another org that happens to reuse the same category name.
    Organization orgA = tempEntity.newOrganization("tag-name-org-a");
    Organization orgB = tempEntity.newOrganization("tag-name-org-b");
    Application appA = tempEntity.newApplication(orgA.getId());
    Application appB = tempEntity.newApplication(orgB.getId());
    Tag tagA = tempEntity.newTag(orgA.getId(), "shared-category-name");
    Tag tagB = tempEntity.newTag(orgB.getId(), "shared-category-name");
    tempEntity.newApplicationTag(appA.getId(), tagA.getId());
    tempEntity.newApplicationTag(appB.getId(), tagB.getId());

    Policy policyA = tempEntity.newPolicy(orgA.getId(), "tag-name-policy-a");
    Policy policyB = tempEntity.newPolicy(orgB.getId(), "tag-name-policy-b");
    seedPolicyViolation(orgA, appA, Stage.ID_BUILD, "tagNameA", policyA, 10);
    seedPolicyViolation(orgB, appB, Stage.ID_BUILD, "tagNameB", policyB, 9);
    seedComponentsReport(appA, Stage.ID_BUILD, "tagNameComponentsA");
    seedComponentsReport(appB, Stage.ID_BUILD, "tagNameComponentsB");

    User reader = tempEntity.newUser("metrics-tag-name-collision-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    // Stage forces the index path for migrated summary/heavy tiles so the assertion hits Lucene
    // tag→appId scoping rather than SQL scope resolution.
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);
    request.tagIds = Set.of(tagA.getId());

    DashboardMetricsDTO metricsA = dashboardMetricsService.getMetrics(request);

    // Name-based category matching would count both orgs (shared display name). Tag-id → app-id
    // scoping must keep each selection to a single org's app.
    assertThat(metricsA.applications.total).isEqualTo(1);
    assertThat(metricsA.violations.total).isEqualTo(1);

    request.tagIds = Set.of(tagB.getId());
    DashboardMetricsDTO metricsB = dashboardMetricsService.getMetrics(request);
    assertThat(metricsB.applications.total).isEqualTo(1);
    assertThat(metricsB.violations.total).isEqualTo(1);
    assertThat(metricsA.applications.total + metricsB.applications.total).isEqualTo(2);
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

    // componentsMetricsReport fixture has 4 distinct CVEs and 4 distinct component hashes; indexing it on two apps
    // still yields estate-level distinct counts rather than per-app duplicates.
    assertThat(metrics.vulnerabilities.total).isEqualTo(4);
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("critical", 1L);
    assertThat(metrics.vulnerabilities.breakdown).containsEntry("high", 1L);
    assertThat(metrics.components.total).isEqualTo(4);
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

  /**
   * Organization scope reaches the index as a {@code parentOrganizationId} term set carrying the
   * requested ids verbatim, and never as a clause in the query string. The indexed ancestor closure
   * resolves each id's subtree, so the ids are not expanded.
   */
  @Test
  public void testGetMetrics_OrganizationScopeTravelsAsParentOrganizationTermSet() {
    Configuration configuration = mock(Configuration.class);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));

    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<java.util.List> restrictions = ArgumentCaptor.forClass(java.util.List.class);
    when(searchIndexClient.count(queries.capture(), restrictions.capture())).thenReturn(0L);

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            configuration,
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("parent-org", "sibling-org");

    assertThat(service.getMetrics(request).applications.total).isZero();
    assertThat(queries.getAllValues()).isNotEmpty();
    assertThat(queries.getAllValues()).noneMatch(query -> query.contains("rganizationId"));
    assertThat(restrictions.getAllValues()).isNotEmpty();
    assertThat(restrictions.getAllValues()).allSatisfy(list -> {
      assertThat(list).hasSize(1);
      IndexTermSetRestriction orgRestriction = (IndexTermSetRestriction) list.get(0);
      assertThat(orgRestriction.field()).isEqualTo(FieldIdentifier.PARENT_ORGANIZATION_ID.label);
      assertThat(orgRestriction.ids()).containsExactlyInAnyOrder("parent-org", "sibling-org");
    });
  }

  /**
   * Term sets are not charged against {@code maxAdvancedSearchClauseCount}, so an organization
   * selection larger than the budget resolves instead of failing with a 400 (CLM-44783).
   */
  @Test
  public void testGetMetrics_OrganizationTermSetIsExemptFromClauseBudget() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(1);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));

    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    ArgumentCaptor<java.util.List> restrictions = ArgumentCaptor.forClass(java.util.List.class);
    when(searchIndexClient.count(anyString(), restrictions.capture())).thenReturn(1L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    DashboardMetricsService service =
        newServiceWithMocks(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            configuration,
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("org-a", "org-b", "org-c");

    assertThat(service.getMetrics(request).applications.total).isEqualTo(1);
    assertThat(restrictions.getAllValues()).isNotEmpty();
    assertThat(restrictions.getAllValues()).allSatisfy(list -> {
      IndexTermSetRestriction orgRestriction = (IndexTermSetRestriction) list.get(0);
      assertThat(orgRestriction.ids()).containsExactlyInAnyOrder("org-a", "org-b", "org-c");
    });
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
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(3L);
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
        mock(AutoPolicyWaiverDAO.class),
        modeProvider,
        readiness,
        scopeResolver,
        mock(DashboardMetricsSqlCoordinator.class),
        shadowComparisonService,
        new DashboardIndexDimensionQueryBuilder(mockConfiguration()),
        mock(OwnerDAO.class),
        mock(StageTypeService.class),
        currentUser,
        mock(ShutdownHandler.class));
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
        mock(AutoPolicyWaiverDAO.class),
        modeProvider,
        readiness,
        mock(DashboardMetricsScopeResolver.class),
        mock(DashboardMetricsSqlCoordinator.class),
        shadowComparisonService,
        new DashboardIndexDimensionQueryBuilder(mockConfiguration()),
        mock(OwnerDAO.class),
        mock(StageTypeService.class),
        currentUser,
        mock(ShutdownHandler.class));
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
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    DashboardMetricsScopeResolver scopeResolver = queryableScopeResolver();
    DashboardMetricsSqlCoordinator coordinator = sqlCoordinatorReturning(1L);
    DashboardMetricsService service = newSqlModeService(
        searchIndexClient,
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        mock(AutoPolicyWaiverDAO.class),
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
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    DashboardMetricsService service =
        newSqlModeService(searchIndexClient, waiverDAO, waiverRequestDAO,
            mock(AutoPolicyWaiverDAO.class), scopeResolver, coordinator);

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
    stubEmptySearchIndexResults(searchIndexClient);
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
        mock(AutoPolicyWaiverDAO.class),
        scopeResolver,
        coordinator);

    DashboardMetricsDTO metrics = service.getMetrics(new DashboardMetricsRequestDTO());

    assertUnavailable(metrics.applications);
    assertUnavailable(metrics.organizations);
    assertUnavailable(metrics.policies);
    assertUnavailable(metrics.violations);
    assertUnavailable(metrics.waivers);
    verify(searchIndexClient, never()).count(anyString(), anyList());
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any(), anyList());
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
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    DashboardMetricsService service = newSqlModeService(
        searchIndexClient,
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        mock(AutoPolicyWaiverDAO.class),
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
    stubEmptySearchIndexResults(searchIndexClient);
    DashboardMetricsScopeResolver scopeResolver = queryableScopeResolver();
    DashboardMetricsSqlCoordinator coordinator = sqlCoordinatorReturning(5L);
    DashboardMetricsService service = newSqlModeService(
        searchIndexClient,
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        mock(AutoPolicyWaiverDAO.class),
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
        mock(AutoPolicyWaiverDAO.class),
        queryableScopeResolver(),
        sqlCoordinatorReturning(1L));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.lastUpdatedAt).isEqualTo(1234L);
    verify(searchIndexClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testGetMetrics_TagAndStageFiltersReturnIndexValuesInIndexServingModes() {
    for (DashboardMetricsSqlMode mode : List.of(DashboardMetricsSqlMode.OFF, DashboardMetricsSqlMode.SHADOW)) {
      DashboardMetricsScopeResolver stageSummaryResolver = mock(DashboardMetricsScopeResolver.class);
      DashboardMetricsSqlCoordinator stageSummaryCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      SearchIndexClient stageSummaryIndex = mock(SearchIndexClient.class);
      when(stageSummaryIndex.count(anyString(), anyList())).thenReturn(7L);
      when(stageSummaryIndex.countDistinct(anyString(), any(), anyList())).thenReturn(7L);
      when(stageSummaryIndex.getLastIndexTime()).thenReturn(1L);
      DashboardMetricsService stageSummaryService =
          newModeService(mode, stageSummaryIndex, mock(PolicyWaiverDAO.class), mock(PolicyWaiverRequestDAO.class),
              mock(AutoPolicyWaiverDAO.class), stageSummaryResolver, stageSummaryCoordinator);
      DashboardMetricsRequestDTO stageSummary = new DashboardMetricsRequestDTO();
      stageSummary.stageIds = Set.of(Stage.ID_BUILD);
      stageSummary.includeHeavyMetrics = false;

      DashboardMetricsDTO stageSummaryMetrics = stageSummaryService.getMetrics(stageSummary);

      assertThat(stageSummaryMetrics.applications.total).isEqualTo(7);
      assertThat(stageSummaryMetrics.organizations.total).isEqualTo(7);
      assertThat(stageSummaryMetrics.policies.total).isEqualTo(7);
      assertUnsupported(stageSummaryMetrics.waivers, "stageIds");
      verify(stageSummaryResolver, never()).resolve(any());
      verifyNoInteractions(stageSummaryCoordinator);

      DashboardMetricsScopeResolver tagSummaryResolver = queryableScopeResolver();
      DashboardMetricsSqlCoordinator tagSummaryCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      PolicyWaiverDAO tagSummaryWaiverDAO = mock(PolicyWaiverDAO.class);
      PolicyWaiverRequestDAO tagSummaryWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
      SearchIndexClient tagSummaryIndex = mock(SearchIndexClient.class);
      when(tagSummaryIndex.count(anyString(), anyList())).thenReturn(5L);
      when(tagSummaryIndex.getLastIndexTime()).thenReturn(2L);
      DashboardMetricsService tagSummaryService =
          newModeService(mode, tagSummaryIndex, tagSummaryWaiverDAO, tagSummaryWaiverRequestDAO,
              mock(AutoPolicyWaiverDAO.class), tagSummaryResolver, tagSummaryCoordinator);
      DashboardMetricsRequestDTO tagSummary = new DashboardMetricsRequestDTO();
      tagSummary.tagIds = Set.of("tag-1");
      tagSummary.includeHeavyMetrics = false;

      DashboardMetricsDTO tagSummaryMetrics = tagSummaryService.getMetrics(tagSummary);

      assertThat(tagSummaryMetrics.applications.total).isEqualTo(5);
      assertThat(tagSummaryMetrics.organizations.total).isEqualTo(5);
      assertThat(tagSummaryMetrics.policies.total).isEqualTo(5);
      assertThat(tagSummaryMetrics.waivers.source).isEqualTo("sql");
      verify(tagSummaryResolver, times(1)).resolve(tagSummary);
      verifyNoInteractions(tagSummaryCoordinator);

      DashboardMetricsScopeResolver stageHeavyResolver = mock(DashboardMetricsScopeResolver.class);
      DashboardMetricsSqlCoordinator stageHeavyCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      SearchIndexClient stageHeavyIndex = mock(SearchIndexClient.class);
      stubEmptySearchIndexResults(stageHeavyIndex);
      when(stageHeavyIndex.getLastIndexTime()).thenReturn(3L);
      DashboardMetricsService stageHeavyService =
          newModeService(mode, stageHeavyIndex, mock(PolicyWaiverDAO.class), mock(PolicyWaiverRequestDAO.class),
              mock(AutoPolicyWaiverDAO.class), stageHeavyResolver, stageHeavyCoordinator);
      DashboardMetricsRequestDTO stageHeavy = new DashboardMetricsRequestDTO();
      stageHeavy.stageIds = Set.of(Stage.ID_BUILD);
      stageHeavy.includeHeavyMetrics = true;

      DashboardMetricsDTO stageHeavyMetrics = stageHeavyService.getMetrics(stageHeavy);

      assertThat(stageHeavyMetrics.violations.total).isZero();
      assertThat(stageHeavyMetrics.components.total).isZero();
      assertThat(stageHeavyMetrics.vulnerabilities.total).isZero();
      assertThat(stageHeavyMetrics.legal.total).isZero();
      verify(stageHeavyResolver, never()).resolve(any());
      verifyNoInteractions(stageHeavyCoordinator);

      DashboardMetricsScopeResolver tagHeavyResolver = mock(DashboardMetricsScopeResolver.class);
      DashboardMetricsSqlCoordinator tagHeavyCoordinator = mock(DashboardMetricsSqlCoordinator.class);
      SearchIndexClient tagHeavyIndex = mock(SearchIndexClient.class);
      stubEmptySearchIndexResults(tagHeavyIndex);
      when(tagHeavyIndex.getLastIndexTime()).thenReturn(4L);
      DashboardMetricsService tagHeavyService =
          newModeService(mode, tagHeavyIndex, mock(PolicyWaiverDAO.class), mock(PolicyWaiverRequestDAO.class),
              mock(AutoPolicyWaiverDAO.class), tagHeavyResolver, tagHeavyCoordinator);
      DashboardMetricsRequestDTO tagHeavy = new DashboardMetricsRequestDTO();
      tagHeavy.tagIds = Set.of("tag-1");
      tagHeavy.includeHeavyMetrics = true;

      DashboardMetricsDTO tagHeavyMetrics = tagHeavyService.getMetrics(tagHeavy);

      assertThat(tagHeavyMetrics.violations.total).isZero();
      assertThat(tagHeavyMetrics.components.total).isZero();
      assertThat(tagHeavyMetrics.vulnerabilities.total).isZero();
      assertThat(tagHeavyMetrics.legal.total).isZero();
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
    assertThat(metrics.waivers.breakdown).containsEntry("expiring", 0L);
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
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(7L);
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
    // loadMetrics runs once (coalesced): count() x3, countDistinct() x1 (components),
    // countDistinctNamed() x1 (legal), countDistinctAndFloatBands() x1 (vulnerabilities).
    verify(searchIndexClient, times(3)).count(anyString(), anyList());
    verify(searchIndexClient, times(1)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
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
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(7L);
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
    verify(searchIndexClient, times(3)).count(anyString(), anyList());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testGetMetrics_CacheSeparatesSummaryHeavyAndCompatibilityLoads() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(4L);
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

    verify(searchIndexClient, times(6)).count(anyString(), anyList());
    verify(searchIndexClient, times(2)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
    verify(searchIndexClient, times(3)).getLastIndexTime();
  }

  @Test
  public void testGetMetrics_SummaryTierSkipsHeavyIndexComputations() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(1L);
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
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, never()).countDistinct(anyString(), any(), anyList());
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
    AutoPolicyWaiverDAO autoPolicyWaiverDAO = mock(AutoPolicyWaiverDAO.class);
    lenient().when(autoPolicyWaiverDAO.selectCount(any())).thenReturn(0L);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        autoPolicyWaiverDAO,
        mock(DashboardMetricsSqlModeProvider.class),
        mock(DashboardMetricsSqlReadiness.class),
        mock(DashboardMetricsScopeResolver.class),
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mockConfiguration()),
        mock(OwnerDAO.class),
        mock(StageTypeService.class),
        currentUser,
        mock(ShutdownHandler.class));

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = true;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications).isNull();
    assertThat(metrics.violations).isNotNull();
    assertThat(metrics.lastUpdatedAt).isNotNull();
    verify(searchIndexClient, never()).count(anyString(), anyList());
    verify(searchIndexClient, times(1)).getLastIndexTime();
    verify(policyWaiverDAO, never()).selectCount(any());
    verify(policyWaiverRequestDAO, never()).selectCount(any());
  }

  @Test
  public void testGetMetrics_StageFilteredSummaryComputesIndexMetricsAndLeavesWaiversUnsupported() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(4L);
    when(searchIndexClient.countDistinct(anyString(), any(), anyList())).thenReturn(4L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("stage-filter-user", "stage-filter-user", User.INTERNAL_REALM_ID));

    PolicyWaiverDAO policyWaiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO policyWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    AutoPolicyWaiverDAO autoPolicyWaiverDAO = mock(AutoPolicyWaiverDAO.class);
    lenient().when(autoPolicyWaiverDAO.selectCount(any())).thenReturn(0L);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        autoPolicyWaiverDAO,
        mock(DashboardMetricsSqlModeProvider.class),
        mock(DashboardMetricsSqlReadiness.class),
        mock(DashboardMetricsScopeResolver.class),
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mockConfiguration()),
        mock(OwnerDAO.class),
        mock(StageTypeService.class),
        currentUser,
        mock(ShutdownHandler.class));

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications.total).isEqualTo(4);
    assertThat(metrics.organizations.total).isEqualTo(4);
    assertThat(metrics.policies.total).isEqualTo(4);
    assertUnsupported(metrics.waivers, "stageIds");
    assertThat(metrics.violations).isNull();
    // Orgs + policies still use count(); applications uses countDistinct on POLICY_VIOLATION.
    verify(searchIndexClient, times(2)).count(anyString(), anyList());
    verify(searchIndexClient, times(1)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(policyWaiverDAO, never()).selectCount(any());
    verify(policyWaiverRequestDAO, never()).selectCount(any());
  }

  @Test
  public void testGetMetrics_TagFilteredSummaryComputesWaiversAndIndexMetrics() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    ArgumentCaptor<String> countQuery = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IndexFilterRestriction>> restrictions =
        ArgumentCaptor.forClass(List.class);
    when(searchIndexClient.count(countQuery.capture(), restrictions.capture())).thenReturn(6L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("tag-filter-user", "tag-filter-user", User.INTERNAL_REALM_ID));

    PolicyWaiverDAO policyWaiverDAO = mock(PolicyWaiverDAO.class);
    PolicyWaiverRequestDAO policyWaiverRequestDAO = mock(PolicyWaiverRequestDAO.class);
    AutoPolicyWaiverDAO autoPolicyWaiverDAO = mock(AutoPolicyWaiverDAO.class);
    lenient().when(autoPolicyWaiverDAO.selectCount(any())).thenReturn(0L);
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
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    Owner taggedApp = mock(Owner.class);
    when(taggedApp.getType()).thenReturn(OwnerType.APPLICATION);
    when(taggedApp.getId()).thenReturn("taggedApp");
    when(ownerDAO.getOwnersByAppTagsAndOrgs(any(), any(), any())).thenReturn(List.of(taggedApp));
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        autoPolicyWaiverDAO,
        offModeProvider(),
        offSqlReadiness(),
        scopeResolver,
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mockConfiguration()),
        ownerDAO,
        mock(StageTypeService.class),
        currentUser,
        mock(ShutdownHandler.class));

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("tag-1");
    request.includeHeavyMetrics = false;

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.applications.total).isEqualTo(6);
    assertThat(metrics.organizations.total).isEqualTo(6);
    assertThat(metrics.policies.total).isEqualTo(6);
    assertThat(metrics.waivers.total).isEqualTo(5);
    assertThat(metrics.waivers.breakdown).containsEntry("existing", 2L).containsEntry("requested", 3L);
    assertThat(countQuery.getAllValues())
        .anyMatch(q -> q.contains("itemType:policy"));
    assertThat(countQuery.getAllValues())
        .noneMatch(q -> q.contains("applicationId:(taggedApp)"));
    assertThat(restrictions.getAllValues())
        .anyMatch(list -> hasApplicationIdTermSet(list, "taggedApp"));
    verify(searchIndexClient, times(3)).count(anyString(), anyList());
    verify(searchIndexClient, never()).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, never()).countDistinct(anyString(), any(), anyList());
    verify(scopeResolver, times(1)).resolve(request);
  }

  @Test
  public void testGetMetrics_OversizedTagFilterUsesTermSetRestriction() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IndexFilterRestriction>> restrictions =
        ArgumentCaptor.forClass(List.class);
    when(searchIndexClient.count(anyString(), restrictions.capture())).thenReturn(3L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("oversized-tag-user", "oversized-tag-user", User.INTERNAL_REALM_ID));
    Configuration configuration = mock(Configuration.class);
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    List<Owner> taggedApps = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      Owner owner = mock(Owner.class);
      when(owner.getType()).thenReturn(OwnerType.APPLICATION);
      when(owner.getId()).thenReturn("app-" + i);
      taggedApps.add(owner);
    }
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(ownerDAO.getOwnersByAppTagsAndOrgs(any(), any(), any())).thenReturn(taggedApps);
    DashboardMetricsService service = new DashboardMetricsService(
        searchIndexClient,
        new MetricFilterValidator(),
        mock(PolicyWaiverDAO.class),
        mock(PolicyWaiverRequestDAO.class),
        mock(AutoPolicyWaiverDAO.class),
        offModeProvider(),
        offSqlReadiness(),
        mock(DashboardMetricsScopeResolver.class),
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(configuration),
        ownerDAO,
        mock(StageTypeService.class),
        currentUser,
        mock(ShutdownHandler.class));

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("broad-tag");
    request.stageIds = Set.of(Stage.ID_BUILD);

    DashboardMetricsDTO metrics = service.getMetrics(request);

    assertThat(metrics.organizations.total).isEqualTo(3);
    assertThat(metrics.policies.total).isEqualTo(3);
    assertThat(metrics.applications.errorCode).isNull();
    assertThat(metrics.violations.errorCode).isNull();
    assertThat(metrics.legal.errorCode).isNull();
    assertUnsupported(metrics.waivers, "stageIds");
    assertThat(restrictions.getAllValues())
        .anyMatch(list -> hasApplicationIdTermSet(list, "app-0", "app-1", "app-2"));
    verify(searchIndexClient, times(2)).count(anyString(), anyList());
    verify(searchIndexClient, times(2)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
  }

  @Test
  public void testGetMetrics_StageAndTagFilteredHeavyTierComputesIndexMetrics() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    stubEmptySearchIndexResults(searchIndexClient);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);
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
    assertThat(metrics.violations.total).isZero();
    assertThat(metrics.components.total).isZero();
    assertThat(metrics.vulnerabilities.total).isZero();
    assertThat(metrics.legal.total).isZero();
    verify(searchIndexClient, never()).count(anyString(), anyList());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
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
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(3L, 11L);
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

    // Two cache misses x loadMetrics: count() x3, countDistinct() x1, countDistinctNamed() x1,
    // countDistinctAndFloatBands() x1 per miss.
    verify(searchIndexClient, times(6)).count(anyString(), anyList());
    verify(searchIndexClient, times(2)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
  }

  @Test
  public void testGetMetrics_CacheKeyDifferentiatesSameUsernameDifferentRealm() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(5L, 9L);
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

    // Two cache misses x loadMetrics: count() x3, countDistinct() x1, countDistinctNamed() x1,
    // countDistinctAndFloatBands() x1 per miss.
    verify(searchIndexClient, times(6)).count(anyString(), anyList());
    verify(searchIndexClient, times(2)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(2)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
  }

  @Test
  public void testGetMetrics_CacheCoalescesAcrossEmptyFilterRequests() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));
    when(searchIndexClient.count(anyString(), anyList())).thenReturn(2L);
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
    verify(searchIndexClient, times(3)).count(anyString(), anyList());
    verify(searchIndexClient, times(1)).countDistinct(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctNamed(anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).aggregateCountByField(anyString(), anyString(), any(), anyList());
    verify(searchIndexClient, times(1)).countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList());
  }

  private static void stubEmptySearchIndexResults(SearchIndexClient searchIndexClient) {
    lenient().when(searchIndexClient.aggregateCountByField(anyString(), anyString(), any(), anyList()))
        .thenReturn(
            new MetricAggregationResult(0L, Map.of("critical", 0L, "severe", 0L, "moderate", 0L, "low", 0L)));
    lenient().when(searchIndexClient.countDistinctAndFloatBands(anyString(), any(), anyString(), any(), anyList()))
        .thenReturn(
            new MetricAggregationResult(0L, Map.of("critical", 0L, "high", 0L, "medium", 0L, "low", 0L)));
    lenient().when(searchIndexClient.countDistinct(anyString(), any(), anyList())).thenReturn(0L);
    lenient().when(searchIndexClient.countDistinctNamed(anyString(), any(), anyList()))
        .thenReturn(
            Map.of("applications", 0L, "components", 0L));
  }

  private static void assertUnsupported(Object metric, String... unsupportedDimensions) {
    assertThat(metric).extracting("total").isNull();
    assertThat(metric).extracting("errorCode").isEqualTo("UNSUPPORTED_FILTER_COMBINATION");
    assertThat(metric).extracting("unsupportedDimensions").isEqualTo(List.of(unsupportedDimensions));
  }

  private static boolean hasApplicationIdTermSet(List<IndexFilterRestriction> restrictions, String... ids) {
    return restrictions.stream()
        .anyMatch(restriction -> restriction instanceof IndexTermSetRestriction termSet
            && FieldIdentifier.APPLICATION_ID.label.equals(termSet.field())
            && termSet.ids().containsAll(List.of(ids)));
  }

  private void seedComponentsReport(Application app, String scanId) throws Exception {
    seedComponentsReport(app, Stage.ID_BUILD, scanId);
  }

  private void seedComponentsReport(Application app, String stageId, String scanId) throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageId, scanId);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/componentsMetricReport", tempDir), lookup(InsightWork.class));
  }

  private void seedPolicyViolation(
      Organization org,
      Application app,
      String scanId,
      int threatLevel) throws Exception
  {
    seedPolicyViolation(org, app, Stage.ID_BUILD, scanId, tempEntity.newPolicy(org.getId(), "Security - Critical "
        + scanId), threatLevel);
  }

  private void seedPolicyViolation(
      Organization org,
      Application app,
      String stageId,
      String scanId,
      Policy policy,
      int threatLevel) throws Exception
  {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageId, scanId);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir), lookup(InsightWork.class));
    tempEntity.newPolicyViolation(evaluation, policy, threatLevel, PolicyThreatCategory.SECURITY,
        "com.example", "artifact", "1.0", DashboardMetricsTestSupport.violationComponentHash(scanId));
  }

  private static void assertIndexSourcedMetric(DashboardMetricsDTO metrics) {
    assertThat(metrics.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);
    assertThat(metrics.applications.breakdown).containsKey("stages");
    assertThat(metrics.lastUpdatedAt).isNotNull();
  }

  private static Configuration mockConfiguration() {
    Configuration configuration = mock(Configuration.class);
    // Tag→appId metrics path uses buildEscapedApplicationFilterClause (size-capped).
    lenient().when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2048);
    return configuration;
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
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final DashboardMetricsScopeResolver scopeResolver,
      final DashboardMetricsSqlCoordinator coordinator)
  {
    return newModeService(
        DashboardMetricsSqlMode.ON,
        searchIndexClient,
        policyWaiverDAO,
        policyWaiverRequestDAO,
        autoPolicyWaiverDAO,
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
    AutoPolicyWaiverDAO autoPolicyWaiverDAO = mock(AutoPolicyWaiverDAO.class);
    lenient().when(autoPolicyWaiverDAO.selectCount(any())).thenReturn(0L);
    lenient().when(policyWaiverDAO.selectCount(any())).thenReturn(0L);
    lenient().when(policyWaiverRequestDAO.selectCount(any())).thenReturn(0L);
    return newModeService(
        mode,
        mock(SearchIndexClient.class),
        policyWaiverDAO,
        policyWaiverRequestDAO,
        autoPolicyWaiverDAO,
        scopeResolver,
        coordinator);
  }

  private static DashboardMetricsService newModeService(
      final DashboardMetricsSqlMode mode,
      final SearchIndexClient searchIndexClient,
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyWaiverRequestDAO policyWaiverRequestDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
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
        autoPolicyWaiverDAO,
        modeProvider,
        readiness,
        scopeResolver,
        coordinator,
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(mockConfiguration()),
        mock(OwnerDAO.class),
        mock(StageTypeService.class),
        currentUser,
        mock(ShutdownHandler.class));
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
    AutoPolicyWaiverDAO autoPolicyWaiverDAO = mock(AutoPolicyWaiverDAO.class);
    lenient().when(autoPolicyWaiverDAO.selectCount(any())).thenReturn(0L);
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
        autoPolicyWaiverDAO,
        offModeProvider(),
        offSqlReadiness(),
        queryableScopeResolver(),
        mock(DashboardMetricsSqlCoordinator.class),
        mock(DashboardMetricsShadowComparisonService.class),
        new DashboardIndexDimensionQueryBuilder(configuration),
        mock(OwnerDAO.class),
        stageTypeService,
        currentUser,
        mock(ShutdownHandler.class));
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

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskDTO;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.license.LegalObligationsDashboardResponse.Variant;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupCount;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link LegalObligationsDashboardService}. Mocks the collaborators ({@link ProductLicense},
 * {@link ApplicationService}, {@link LicenseThreatGroupService}, {@link PolicyViolationDAO}, {@link CurrentUser}) and
 * exercises:
 * <ul>
 * <li>The four discriminated payload shapes (ALP, top-legal-violations, permission-denied, empty).</li>
 * </ul>
 * Payload-shape tests call {@link LegalObligationsDashboardService#buildAlpResponse} /
 * {@link LegalObligationsDashboardService#buildTopViolationsResponse} with an explicit scope so they stay stable in
 * the distributed Failsafe B-L shard.
 */
public class LegalObligationsDashboardServiceTest
{
  private static final Set<String> SCOPE_ONE_APP = Set.of("app-1");

  private static final Set<String> SCOPE_TWO_APPS = Set.of("app-1", "app-2");

  private ProductLicense productLicense;

  private ApplicationService applicationService;

  private LicenseThreatGroupService licenseThreatGroupService;

  private PolicyViolationDAO policyViolationDAO;

  private DashboardViolationRiskService dashboardViolationRiskService;

  private CurrentUser currentUser;

  private LegalObligationsDashboardService service;

  private SecurityManager securityManager;

  private Subject subject;

  @Before
  public void setUp() {
    securityManager = mock(SecurityManager.class, withSettings().strictness(Strictness.LENIENT));
    subject = mock(Subject.class, withSettings().strictness(Strictness.LENIENT));
    lenient().when(subject.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(subject.associateWith(any(java.util.concurrent.Callable.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);

    productLicense = mock(ProductLicense.class);
    applicationService = mock(ApplicationService.class);
    licenseThreatGroupService = mock(LicenseThreatGroupService.class);
    policyViolationDAO = mock(PolicyViolationDAO.class);
    currentUser = mock(CurrentUser.class);

    when(currentUser.getUsername()).thenReturn("test-user");
    when(applicationService.getApplications()).thenReturn(List.of(app("app-1")));

    dashboardViolationRiskService = mock(DashboardViolationRiskService.class);
    service = new LegalObligationsDashboardService(productLicense, applicationService, licenseThreatGroupService,
        policyViolationDAO, dashboardViolationRiskService, currentUser);
  }

  @After
  public void tearDownShiro() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  private Application app(String id) {
    Application a = new Application();
    a.setId(id);
    return a;
  }

  private static DashboardViolationRiskDTO violationRow(String policyId, String policyName) {
    DashboardViolationRiskDTO row = new DashboardViolationRiskDTO();
    row.policyId = policyId;
    row.policyName = policyName;
    return row;
  }

  private static DashboardResultsDTO<DashboardViolationRiskDTO> risksWith(
      DashboardViolationRiskDTO... rows)
  {
    DashboardResultsDTO<DashboardViolationRiskDTO> dto = new DashboardResultsDTO<>();
    dto.dashboardResults = List.of(rows);
    return dto;
  }

  private void stubNonAlpViolationRisks(DashboardViolationRiskDTO... rows) {
    when(dashboardViolationRiskService.get(
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        eq(0),
        eq(LegalObligationsDashboardService.VIOLATION_FETCH_PAGE_SIZE)))
            .thenReturn(risksWith(rows));
  }

  @Test
  public void alpVariant_returnsExpectedPayloadShape() {
    List<LicenseThreatGroupCount> counts = new ArrayList<>();
    // Zero-unreviewed rows are intentionally filtered out — configured LTGs with no review workload.
    counts.add(new LicenseThreatGroupCount("ltg-zero", "Zero", 10, 0L));
    counts.add(new LicenseThreatGroupCount("ltg-1", "Banned", 10, 5L));
    counts.add(new LicenseThreatGroupCount("ltg-2", "Copyleft", 7, 2L));
    when(licenseThreatGroupService.getUnreviewedComponentCountsByApplicationIds(anyCollection())).thenReturn(counts);

    when(policyViolationDAO.countOpenInWindowByCategory(anyCollection(), eq(PolicyThreatCategory.LICENSE), any(),
        any())).thenReturn(8L, 4L); // current 8, prior 4 → +100%

    LegalObligationsDashboardResponse response = service.buildAlpResponse(SCOPE_TWO_APPS);

    assertThat(response.variant).isEqualTo(Variant.ALP);
    assertThat(response.permissionDenied).isNull();
    assertThat(response.empty).isNull();
    assertThat(response.groups).hasSize(2);
    assertThat(response.groups.get(0).id()).isEqualTo("ltg-1");
    assertThat(response.groups.get(0).name()).isEqualTo("Banned");
    assertThat(response.groups.get(0).reviewCount()).isEqualTo(5L);
    assertThat(response.groups.get(0).trendPct()).isEqualTo(100.0);
    assertThat(response.groups.get(1).id()).isEqualTo("ltg-2");
    assertThat(response.groups.get(1).reviewCount()).isEqualTo(2L);
  }

  @Test
  public void alpVariant_emptyAfterFilteringZeros_returnsEmpty() {
    when(licenseThreatGroupService.getUnreviewedComponentCountsByApplicationIds(anyCollection()))
        .thenReturn(List.of(new LicenseThreatGroupCount("ltg-zero", "Zero", 10, 0L)));

    LegalObligationsDashboardResponse response = service.buildAlpResponse(SCOPE_ONE_APP);

    assertThat(response.empty).isTrue();
    assertThat(response.variant).isNull();
    assertThat(response.groups).isNull();
    // No window queries when there are zero non-zero rows — saves two DB round-trips on an empty tile.
    verify(policyViolationDAO, never()).countOpenInWindowByCategory(anyCollection(), any(), any(), any());
  }

  @Test
  public void alpVariant_priorWindowZero_trendPctIsZero() {
    when(licenseThreatGroupService.getUnreviewedComponentCountsByApplicationIds(anyCollection())).thenReturn(
        List.of(new LicenseThreatGroupCount("ltg-1", "Banned", 10, 3L)));
    when(policyViolationDAO.countOpenInWindowByCategory(anyCollection(), eq(PolicyThreatCategory.LICENSE), any(),
        any())).thenReturn(7L, 0L); // prior=0 → flat (UI shows no arrow)

    LegalObligationsDashboardResponse response = service.buildAlpResponse(SCOPE_ONE_APP);

    assertThat(response.variant).isEqualTo(Variant.ALP);
    assertThat(response.groups).hasSize(1);
    assertThat(response.groups.get(0).trendPct()).isEqualTo(0.0);
  }

  @Test
  public void alpVariant_resultIsCappedAt10() {
    List<LicenseThreatGroupCount> twenty = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      twenty.add(new LicenseThreatGroupCount("ltg-" + i, "Group " + i, 10, 5L + i));
    }
    when(licenseThreatGroupService.getUnreviewedComponentCountsByApplicationIds(anyCollection())).thenReturn(twenty);
    when(policyViolationDAO.countOpenInWindowByCategory(anyCollection(), any(), any(), any())).thenReturn(1L);

    LegalObligationsDashboardResponse response = service.buildAlpResponse(SCOPE_ONE_APP);

    assertThat(response.variant).isEqualTo(Variant.ALP);
    assertThat(response.groups).hasSize(LegalObligationsDashboardService.ALP_LIMIT);
  }

  @Test
  public void nonAlpVariant_returnsExpectedPayloadShape() {
    List<DashboardViolationRiskDTO> rows = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      rows.add(violationRow("p1", "License - Banned"));
    }
    for (int i = 0; i < 7; i++) {
      rows.add(violationRow("p2", "License - Copyleft"));
    }
    stubNonAlpViolationRisks(rows.toArray(DashboardViolationRiskDTO[]::new));

    LegalObligationsDashboardResponse response = service.buildTopViolationsResponse();

    assertThat(response.variant).isEqualTo(Variant.TOP_LEGAL_VIOLATIONS);
    assertThat(response.permissionDenied).isNull();
    assertThat(response.empty).isNull();
    assertThat(response.violations).hasSize(2);
    assertThat(response.violations.get(0).getPolicyId()).isEqualTo("p1");
    assertThat(response.violations.get(0).getPolicyName()).isEqualTo("License - Banned");
    assertThat(response.violations.get(0).getOpenViolationCount()).isEqualTo(12L);
    assertThat(response.violations.get(1).getPolicyId()).isEqualTo("p2");
    // Crucial: a non-ALP request must never trigger any ALP-shaped query path.
    verify(licenseThreatGroupService, never()).getUnreviewedComponentCountsByApplicationIds(anyCollection());
  }

  @Test
  public void nonAlpVariant_noViolations_returnsEmpty() {
    stubNonAlpViolationRisks();

    LegalObligationsDashboardResponse response = service.buildTopViolationsResponse();

    assertThat(response.empty).isTrue();
    assertThat(response.variant).isNull();
    assertThat(response.violations).isNull();
  }

  @Test
  public void permissionDenied_whenUserHasNoScopedApplications() {
    when(productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)).thenReturn(true);
    when(applicationService.getApplications()).thenReturn(Collections.emptyList());

    LegalObligationsDashboardResponse response = service.getResponse();

    assertThat(response.permissionDenied).isTrue();
    assertThat(response.variant).isNull();
    assertThat(response.empty).isNull();
    assertThat(response.groups).isNull();
    assertThat(response.violations).isNull();
    // Critical: a permission-denied request must never query the data layer — no information leakage.
    verify(licenseThreatGroupService, never()).getUnreviewedComponentCountsByApplicationIds(anyCollection());
    verify(dashboardViolationRiskService, never()).get(
        any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
    verify(policyViolationDAO, never()).countOpenInWindowByCategory(anyCollection(), any(), any(), any());
  }

  @Test
  public void permissionDenied_whenApplicationServiceReturnsNull() {
    when(productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)).thenReturn(false);
    when(applicationService.getApplications()).thenReturn(null);

    LegalObligationsDashboardResponse response = service.getResponse();

    assertThat(response.permissionDenied).isTrue();
  }

  @Test
  public void computeTrendPct_handlesCorrectly() {
    assertThat(LegalObligationsDashboardService.computeTrendPct(10, 5)).isEqualTo(100.0);
    assertThat(LegalObligationsDashboardService.computeTrendPct(5, 10)).isEqualTo(-50.0);
    assertThat(LegalObligationsDashboardService.computeTrendPct(0, 5)).isEqualTo(-100.0);
    // Prior=0 must NOT produce Infinity / NaN. Tile is keyed off this.
    assertThat(LegalObligationsDashboardService.computeTrendPct(7, 0)).isEqualTo(0.0);
    assertThat(LegalObligationsDashboardService.computeTrendPct(0, 0)).isEqualTo(0.0);
  }

  @Test
  public void alpVariant_neverCallsTopOpenByCategory() {
    // Defense-in-depth: ensures the ALP branch path does not accidentally fall through to the non-ALP DAO query.
    when(licenseThreatGroupService.getUnreviewedComponentCountsByApplicationIds(anyCollection())).thenReturn(
        List.of(new LicenseThreatGroupCount("ltg-1", "Banned", 10, 1L)));
    when(policyViolationDAO.countOpenInWindowByCategory(anyCollection(), any(), any(), any())).thenReturn(1L, 1L);

    service.buildAlpResponse(SCOPE_ONE_APP);

    verify(dashboardViolationRiskService, never()).get(
        any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
    verify(policyViolationDAO, atMost(2)).countOpenInWindowByCategory(anyCollection(), any(), any(), any());
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskDTO;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupCount;
import com.sonatype.insight.brain.model.policy.PolicyOpenViolationSummary;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backs the dedicated Legal Obligations dashboard tile endpoint (CLM-39604 / P1.5-D-2). Server-side branching on
 * {@link LicensedFeature#ADVANCED_LEGAL_PACK ALP} entitlement returns either:
 * <ul>
 * <li>An ALP per-license-threat-group breakdown (top 10, includes a 30-day-over-prior-30-day trend), or</li>
 * <li>A non-ALP top-4 license-category policy breakdown.</li>
 * </ul>
 *
 * <p>
 * Both variants are filtered by the caller's already-authorized scoped-application set (resolved here via
 * {@link ApplicationService#getApplications()}, which is decorated with the same Shiro-backed authz filter the
 * rest of the dashboard uses). A user with no scoped apps gets
 * {@link LegalObligationsDashboardResponse#permissionDenied()}
 * — the tile renders a graceful greyed state rather than a 5xx or spinner.
 *
 * @since 1.205
 */
@Named
@Singleton
public class LegalObligationsDashboardService
{
  private static final Logger log = LoggerFactory.getLogger(LegalObligationsDashboardService.class);

  static final int ALP_LIMIT = 10;

  static final int NON_ALP_LIMIT = 4;

  /** Upper bound when aggregating license violations via {@link DashboardViolationRiskService}. */
  static final int VIOLATION_FETCH_PAGE_SIZE = 10_000;

  static final Duration TREND_WINDOW = Duration.ofDays(30);

  private final ProductLicense productLicense;

  private final ApplicationService applicationService;

  private final LicenseThreatGroupService licenseThreatGroupService;

  private final PolicyViolationDAO policyViolationDAO;

  /**
   * Source of truth for the non-ALP top-N license-policy variant of the tile. Reads from the same
   * `DashboardViolationRiskService.get(...)` path the rest of the dashboard uses (Violations tab,
   * Top Policy Violations tile, Severity Strip), so the Legal Obligations tile is consistent with
   * what the user sees everywhere else.
   *
   * <p>
   * Why not query the {@code policy_violation} table directly via the DAO: dev/H2 deployments and
   * some scan paths can have a populated evaluation cache but an empty {@code policy_violation}
   * table for the same violations; the DAO query under-reports in that case. The
   * `DashboardViolationRiskService` path is the production-proven aggregate the customer-facing
   * dashboard already uses at scale, so we share its cost.
   */
  private final DashboardViolationRiskService dashboardViolationRiskService;

  private final CurrentUser currentUser;

  @Inject
  public LegalObligationsDashboardService(
      final ProductLicense productLicense,
      final ApplicationService applicationService,
      final LicenseThreatGroupService licenseThreatGroupService,
      final PolicyViolationDAO policyViolationDAO,
      final DashboardViolationRiskService dashboardViolationRiskService,
      final CurrentUser currentUser)
  {
    this.productLicense = productLicense;
    this.applicationService = applicationService;
    this.licenseThreatGroupService = licenseThreatGroupService;
    this.policyViolationDAO = policyViolationDAO;
    this.dashboardViolationRiskService = dashboardViolationRiskService;
    this.currentUser = currentUser;
  }

  /**
   * Computes the discriminated payload for the current user. Branches on ALP entitlement server-side; never
   * leaks ALP-shaped data to non-ALP tenants.
   */
  public LegalObligationsDashboardResponse getResponse() {
    // The non-ALP branch re-reads scope inside DashboardViolationRiskService.get(null, …) by design (see
    // buildTopViolationsResponse); a theoretical TOCTOU within this single synchronous request is accepted —
    // both reads are Shiro-filtered and the window is sub-millisecond.
    Set<String> scopedAppIds = resolveScopedApplicationIds();
    if (scopedAppIds.isEmpty()) {
      // The user has no apps in scope — there is nothing legal-relevant they could see. Return permission-denied
      // (a 200 with a discriminated payload) rather than 403, so the tile renders a polite greyed state.
      return LegalObligationsDashboardResponse.permissionDenied();
    }

    boolean alp = productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    log.debug("Legal-obligations user={} variant={} scopedApps={}",
        currentUser.getUsername(), alp ? "ALP" : "TOP_LEGAL_VIOLATIONS", scopedAppIds.size());
    return alp ? buildAlpResponse(scopedAppIds) : buildTopViolationsResponse();
  }

  /**
   * Returns the ALP variant for the supplied scope. Public to enable a service-level test without going through
   * Shiro / the resource layer. Caller must have already passed the entitlement and scope checks.
   */
  LegalObligationsDashboardResponse buildAlpResponse(final Set<String> scopedAppIds) {
    List<LicenseThreatGroupCount> counts =
        licenseThreatGroupService.getUnreviewedComponentCountsByApplicationIds(scopedAppIds);

    // The counter returns LTGs with zero unreviewed components (so admins see configured groups even when
    // everything is reviewed) — but the dashboard tile is hunting for review workload, so we drop zero rows here.
    List<LicenseThreatGroupCount> nonZero = counts.stream()
        .filter(c -> c.getUnreviewedComponentCount() > 0L)
        .limit(ALP_LIMIT)
        .collect(Collectors.toList());
    if (nonZero.isEmpty()) {
      return LegalObligationsDashboardResponse.empty();
    }

    // Trend = (current 30 days - prior 30 days) / prior 30 days * 100. Use one count query per window (two total
    // round-trips) over the LICENSE category across the user's scoped apps; the per-LTG trend is approximated as
    // the overall license-category trend, which is what the F11 UX spec actually displays at the tile level. A
    // per-LTG trend query would N-multiply DB pressure for a number that only ever animates a small arrow.
    Instant now = Instant.now();
    Date windowEnd = Date.from(now);
    Date windowStart = Date.from(now.minus(TREND_WINDOW));
    Date priorStart = Date.from(now.minus(TREND_WINDOW.multipliedBy(2)));

    long current =
        policyViolationDAO.countOpenInWindowByCategory(scopedAppIds, PolicyThreatCategory.LICENSE, windowStart,
            windowEnd);
    long prior =
        policyViolationDAO.countOpenInWindowByCategory(scopedAppIds, PolicyThreatCategory.LICENSE, priorStart,
            windowStart);
    double trendPct = computeTrendPct(current, prior);

    List<LegalObligationsAlpGroupDTO> groups = new ArrayList<>(nonZero.size());
    for (LicenseThreatGroupCount c : nonZero) {
      groups.add(new LegalObligationsAlpGroupDTO(
          c.getLicenseThreatGroupId(),
          c.getLicenseThreatGroupName(),
          c.getUnreviewedComponentCount(),
          trendPct));
    }
    return LegalObligationsDashboardResponse.alp(groups);
  }

  LegalObligationsDashboardResponse buildTopViolationsResponse() {
    // Use the same production-proven path the rest of the dashboard reads from. See the
    // `dashboardViolationRiskService` field doc for why we don't query `policy_violation` directly.
    //
    // This method takes no scope argument on purpose: `DashboardViolationRiskService.get(...)` already
    // runs through the same Shiro-scoped `ApplicationService.getApplications()` the rest of the dashboard
    // uses, which returns only apps the current user is authorized to read. Passing the pre-resolved
    // scoped-application ids here would constrain the inner ApplicationStageView lookup in a way that
    // drops evaluation rows we ARE authorized to see (verified: H2 path returns 2 license violations with
    // appIds=null and 0 with appIds=<the same 10 ids>).
    // CRITICAL: pass null (not default-constructed) for the threat-level and state filters.
    // PolicyViolationLoader treats a null filter as "no filter, load everything", but a
    // default-constructed filter with an empty set as "load violations where the state is in
    // {}" — i.e. zero rows. The REST resource passes null for these when the request omits
    // them; we mirror that exactly.
    DashboardResultsDTO<DashboardViolationRiskDTO> risks = dashboardViolationRiskService.get(
        null,
        null,
        null,
        null,
        new PolicyThreatCategoryFilter(EnumSet.of(PolicyThreatCategory.LICENSE)),
        null,
        null,
        null,
        null,
        0,
        VIOLATION_FETCH_PAGE_SIZE);

    List<DashboardViolationRiskDTO> rows = risks.dashboardResults == null
        ? List.of()
        : risks.dashboardResults;

    // Aggregate (policyId, policyName) -> count. A violation row missing a policyId is dropped (defensive,
    // matches the frontend aggregator's behavior). Tie-break on equal counts by policyName ASC.
    Map<String, long[]> countById = new HashMap<>();
    Map<String, String> nameById = new HashMap<>();
    for (DashboardViolationRiskDTO row : rows) {
      if (row.policyId == null) {
        continue;
      }
      countById.computeIfAbsent(row.policyId, k -> new long[1])[0]++;
      nameById.putIfAbsent(row.policyId, row.policyName);
    }

    if (countById.isEmpty()) {
      return LegalObligationsDashboardResponse.empty();
    }

    List<PolicyOpenViolationSummary> top = countById.entrySet()
        .stream()
        .map(e -> new PolicyOpenViolationSummary(
            e.getKey(),
            nameById.get(e.getKey()),
            e.getValue()[0]))
        .sorted(Comparator
            .comparing(PolicyOpenViolationSummary::getOpenViolationCount, Comparator.reverseOrder())
            .thenComparing(dto -> dto.getPolicyName() == null ? "" : dto.getPolicyName()))
        .limit(NON_ALP_LIMIT)
        .collect(Collectors.toList());

    return LegalObligationsDashboardResponse.topLegalViolations(top);
  }

  static double computeTrendPct(final long current, final long prior) {
    if (prior <= 0L) {
      // No baseline → don't return +Infinity / NaN; show flat. The UI treats 0 as "no trend arrow".
      return 0.0;
    }
    double raw = ((double) (current - prior) / (double) prior) * 100.0;
    return Math.round(raw);
  }

  /**
   * Resolves the current user's authorized application IDs. {@link ApplicationService#getApplications()} is
   * decorated with an {@code @AuthzFilter(READ, APPLICATION)} so this never returns apps the user cannot read.
   */
  private Set<String> resolveScopedApplicationIds() {
    List<Application> apps = applicationService.getApplications();
    if (apps == null || apps.isEmpty()) {
      return Set.of();
    }
    return apps.stream()
        .map(Application::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}

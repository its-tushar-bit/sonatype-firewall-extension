/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyOpenViolationSummary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Discriminated payload for the Legal Obligations dashboard tile (CLM-39604 / P1.5-D-2). Exactly one of the
 * following shapes is populated in any single response, chosen server-side from the user's tenant entitlement and
 * scope:
 * <ul>
 * <li>{@code variant = ALP} + {@code groups} — license-threat-group breakdown, max 10 rows, for tenants licensed
 * for the Advanced Legal Pack.</li>
 * <li>{@code variant = TOP_LEGAL_VIOLATIONS} + {@code violations} — top-4 license-policy violations across the
 * user's scoped applications, for tenants without Advanced Legal Pack.</li>
 * <li>{@code permissionDenied = true} — the caller has no scoped applications (cannot see any legal data).
 * Returned with HTTP 200 so the tile renders a graceful greyed state instead of an error.</li>
 * <li>{@code empty = true} — the caller is entitled but there is no legal data in scope. Returned with HTTP 200
 * so the tile renders the "no legal obligations in scope" empty state.</li>
 * </ul>
 * Fields not relevant to the chosen variant are omitted from JSON serialization via {@link Include#NON_NULL}, so
 * the wire shape matches the discriminated-union TypeScript contract documented in the F11 epic §5.3.
 *
 * @since 1.205
 */
@JsonInclude(Include.NON_NULL)
public class LegalObligationsDashboardResponse
{
  public enum Variant
  {
    ALP,
    TOP_LEGAL_VIOLATIONS
  }

  public Variant variant;

  public List<LegalObligationsAlpGroupDTO> groups;

  public List<PolicyOpenViolationSummary> violations;

  public Boolean permissionDenied;

  public Boolean empty;

  public static LegalObligationsDashboardResponse alp(List<LegalObligationsAlpGroupDTO> groups) {
    LegalObligationsDashboardResponse r = new LegalObligationsDashboardResponse();
    r.variant = Variant.ALP;
    r.groups = groups != null ? List.copyOf(groups) : List.of();
    return r;
  }

  public static LegalObligationsDashboardResponse topLegalViolations(List<PolicyOpenViolationSummary> violations) {
    LegalObligationsDashboardResponse r = new LegalObligationsDashboardResponse();
    r.variant = Variant.TOP_LEGAL_VIOLATIONS;
    r.violations = violations != null ? List.copyOf(violations) : List.of();
    return r;
  }

  public static LegalObligationsDashboardResponse permissionDenied() {
    LegalObligationsDashboardResponse r = new LegalObligationsDashboardResponse();
    r.permissionDenied = Boolean.TRUE;
    return r;
  }

  public static LegalObligationsDashboardResponse empty() {
    LegalObligationsDashboardResponse r = new LegalObligationsDashboardResponse();
    r.empty = Boolean.TRUE;
    return r;
  }
}

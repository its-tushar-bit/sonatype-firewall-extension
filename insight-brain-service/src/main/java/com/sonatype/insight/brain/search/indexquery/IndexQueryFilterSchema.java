/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.Map;

public final class IndexQueryFilterSchema
{
  public enum Kind
  {
    TEXT,
    TERMS,
    RANGE,
    /** Violation OPEN/WAIVED state, compiled against the waiver-status field (OPEN = not waived). */
    STATE,
    /** Violation AUTO/MANUAL waiver type, compiled to the AutoWaived / Waived waiver-status token. */
    WAIVER_TYPE,
    /**
     * The {@code includeAutoWaivers} include-toggle on WAIVER queries. Compiled specially against the
     * indexed {@code policyWaiverAuto} discriminator:
     * <ul>
     * <li>{@code true} OR absent — include BOTH manual and auto waivers (no clause added);</li>
     * <li>{@code false} — exclude auto waivers, i.e. add {@code policyWaiverAuto:"false"}
     * so only manual waivers match.</li>
     * </ul>
     * Classic include-toggle: absent means both. Auto-only uses the separate {@code isAuto} TERMS
     * filter rather than overloading {@code includeAutoWaivers:true}.
     */
    AUTO_WAIVER_TOGGLE,
    /**
     * The {@code expiry} active-vs-expired status filter on WAIVER queries. Compiled against the
     * indexed {@code policyWaiverExpiresAtEpochMs} numeric-long point vs the server clock at request
     * time:
     * <ul>
     * <li>{@code "active"} — the waiver has not expired: {@code expiresAt} is absent (never expires) OR
     * strictly after now;</li>
     * <li>{@code "expired"} — the waiver has expired: {@code expiresAt} is present AND at or before now.</li>
     * </ul>
     * A missing expiry epoch means "never expires" and is therefore always active. Only
     * "active"/"expired" are accepted; any other value is a 400.
     */
    EXPIRY_STATUS,
    /**
     * The Ana {@code expiryStatus} TERMS filter over the denormalized
     * {@code policyWaiverExpiryStatus} keyword. Values are validated against
     * {@link com.sonatype.insight.brain.search.index.PolicyWaiverExpiryStatuses} (active/expired/never);
     * unrecognized values are a 400. Distinct from {@link #EXPIRY_STATUS}, which is clock-relative.
     * <p>
     * Prefer {@link #EXPIRY_STATUS} ({@code expiry}) for Active/Expired toggles — that path treats
     * never-expiring waivers as active. {@code expiryStatus:"active"} also includes {@code never}
     * so an Active chip wired here does not hide permanent waivers; use {@code never}/{@code expired}
     * for exact denormalized buckets.
     */
    EXPIRY_STATUS_TERMS,
    /**
     * Boolean string TERMS ({@code "true"}/{@code "false"} only). Used by Ana {@code isAuto};
     * unrecognized values are a 400 (unlike plain {@link #TERMS}, which would silently match nothing).
     */
    BOOLEAN_TERMS,
    /**
     * The {@code waiverStates} multi-select on WAIVER queries, spanning both item types:
     * <ul>
     * <li>{@code existing} — committed waivers ({@code itemType:policy_waiver});</li>
     * <li>{@code requested} — pending requests ({@code itemType:policy_waiver_request} AND
     * {@code policyWaiverRequestStatus:"REQUESTED"});</li>
     * <li>{@code rejected} — rejected requests ({@code itemType:policy_waiver_request} AND
     * {@code policyWaiverRequestStatus:"REJECTED"});</li>
     * <li>{@code excluded} — auto-waiver exclusions, compiled to auto waivers
     * ({@code itemType:policy_waiver} AND {@code policyWaiverAuto:"true"}).</li>
     * </ul>
     * Multiple selected states are OR'd. Approved requests are indexed but selected by no state.
     * Absent (no waiverStates) leaves the WAIVER item-type union unrestricted.
     */
    WAIVER_STATES
  }

  public record FilterDef(String field, Kind kind)
  {
  }

  /** The {@code query} filter has no index field: it becomes the bare-token query, not a field chip. */
  public static final FilterDef FREE_TEXT_QUERY = new FilterDef(null, Kind.TEXT);

  private static final Map<IndexQueryType, Map<String, FilterDef>> SCHEMA = buildSchema();

  private IndexQueryFilterSchema() {
  }

  public static Map<String, FilterDef> forQueryType(final IndexQueryType queryType) {
    return SCHEMA.getOrDefault(queryType, Map.of());
  }

  private static Map<IndexQueryType, Map<String, FilterDef>> buildSchema() {
    return Map.of(
        // stages/policyTypes/violationStates/policyThreatLevel are now honoured on APPLICATION docs via
        // denormalized aggregates written in the one violations rollup pass (DocumentBuilderHelper):
        // applicationViolationStage/PolicyType/State are precomputed multi-valued keyword sets (TERMS,
        // NOT Kind.STATE — the state set already lists open/waived/legacy), and applicationMaxPolicyThreatLevel
        // is the max-threat int (RANGE). age is a RANGE over the existing applicationLastEvaluationTimeEpochMs
        // (no index change), taking a two-element [minEpochMs, maxEpochMs] numeric bound.
        // applicationCategoryName is denormalized (multi-valued) onto
        // APPLICATION docs, so the categories filter is honoured. Filter semantics: values within one filter are
        // OR'd; distinct filters AND-narrow (standard faceted-search rail — see the filter compiler).
        IndexQueryType.APPLICATION, Map.ofEntries(
            Map.entry("query", FREE_TEXT_QUERY),
            Map.entry("organizations", new FilterDef("organizationName", Kind.TERMS)),
            Map.entry("applications", new FilterDef("applicationName", Kind.TERMS)),
            Map.entry("applicationCategories", new FilterDef("applicationCategoryName", Kind.TERMS)),
            Map.entry("stages", new FilterDef("applicationViolationStage", Kind.TERMS)),
            Map.entry("policyTypes", new FilterDef("applicationViolationPolicyType", Kind.TERMS)),
            Map.entry("violationStates", new FilterDef("applicationViolationState", Kind.TERMS)),
            Map.entry("policyThreatLevel", new FilterDef("applicationMaxPolicyThreatLevel", Kind.RANGE)),
            // age: RANGE over applicationLastEvaluationTimeEpochMs; caller resolves window -> [fromEpochMs, toEpochMs]
            // epoch bounds. No server-side window resolution is applied here.
            Map.entry("age", new FilterDef("applicationLastEvaluationTimeEpochMs", Kind.RANGE))),
        // applicationCategoryName is now denormalized (multi-valued) onto violation docs, so the
        // categories filter is honoured here too. states/waiverType compile against
        // policyViolationWaiverStatus (OPEN = not waived). Values within one filter are OR'd; distinct
        // filters AND-narrow (standard faceted-search rail).
        IndexQueryType.VIOLATION, Map.ofEntries(
            Map.entry("query", FREE_TEXT_QUERY),
            Map.entry("organizations", new FilterDef("organizationName", Kind.TERMS)),
            Map.entry("applications", new FilterDef("applicationName", Kind.TERMS)),
            Map.entry("applicationCategories", new FilterDef("applicationCategoryName", Kind.TERMS)),
            Map.entry("stages", new FilterDef("policyEvaluationStage", Kind.TERMS)),
            Map.entry("policyTypes", new FilterDef("policyViolationThreatCategory", Kind.TERMS)),
            Map.entry("states", new FilterDef("policyViolationWaiverStatus", Kind.STATE)),
            Map.entry("waiverType", new FilterDef("policyViolationWaiverStatus", Kind.WAIVER_TYPE)),
            // policyViolationThreatLevel is set only on POLICY_VIOLATION docs; LEGAL_VIOLATION docs
            // carry no queryable threat-level field, so this range narrows policy violations only.
            Map.entry("policyThreatLevel", new FilterDef("policyViolationThreatLevel", Kind.RANGE))),
        IndexQueryType.POLICY, Map.of(
            "query", FREE_TEXT_QUERY,
            "policyTypes", new FilterDef("policyThreatCategory", Kind.TERMS),
            "organizations", new FilterDef("organizationName", Kind.TERMS),
            "policyThreatLevel", new FilterDef("policyThreatLevel", Kind.RANGE)),
        // policyThreatLevel resolves to POLICY_WAIVER_THREAT_LEVEL via the policyWaiverThreatLevel
        // FieldMap key. includeAutoWaivers filters on the indexed policyWaiverAuto discriminator:
        // true/absent -> both kinds; false -> manual only (Classic; see Kind.AUTO_WAIVER_TOGGLE).
        // Ana Auto/Manual uses isAuto (BOOLEAN_TERMS) on policyWaiverIsAuto.
        // applications/applicationId narrow to app-scoped waivers only: org-scoped waivers carry no
        // applicationName/Id. organizations matches both scopes — app- and org-scoped waivers both
        // carry parentOrganizationName/Id (full ancestor chain).
        // policy = policy NAME; policyIds = policy ID (Ana sidebar). `policies` is a deprecated alias
        // of policyIds kept so older clients do not break.
        // Prefer expiry (clock, Kind.EXPIRY_STATUS) for Active/Expired toggles — never-expiring docs
        // count as active. expiryStatus is the denormalized active/expired/never keyword.
        // waiverStates spans both item types (see Kind.WAIVER_STATES). status filters requests by the
        // policyWaiverRequestStatus discriminator; scope by the indexed scope; policyTypes by
        // policyWaiverPolicyType. All three are OR-within / AND-across standard TERMS.
        IndexQueryType.WAIVER, Map.ofEntries(
            Map.entry("query", FREE_TEXT_QUERY),
            Map.entry("organizations", new FilterDef("organizationName", Kind.TERMS)),
            Map.entry("applications", new FilterDef("applicationName", Kind.TERMS)),
            Map.entry("applicationId", new FilterDef("applicationId", Kind.TERMS)),
            Map.entry("policy", new FilterDef("policyWaiverPolicyName", Kind.TERMS)),
            Map.entry("policyTypes", new FilterDef("policyWaiverPolicyType", Kind.TERMS)),
            Map.entry("scope", new FilterDef("policyWaiverScope", Kind.TERMS)),
            Map.entry("status", new FilterDef("policyWaiverRequestStatus", Kind.TERMS)),
            // WAIVER_STATES self-compiles its item-type/status clauses (see compileWaiverStates), so
            // it takes no index field — null like FREE_TEXT_QUERY.
            Map.entry("waiverStates", new FilterDef(null, Kind.WAIVER_STATES)),
            Map.entry("policyIds", new FilterDef("policyWaiverPolicyId", Kind.TERMS)),
            // Deprecated alias of policyIds (same index field).
            Map.entry("policies", new FilterDef("policyWaiverPolicyId", Kind.TERMS)),
            Map.entry("policyThreatLevel", new FilterDef("policyWaiverThreatLevel", Kind.RANGE)),
            Map.entry("expiry", new FilterDef("policyWaiverExpiresAtEpochMs", Kind.EXPIRY_STATUS)),
            Map.entry("expiryStatus", new FilterDef("policyWaiverExpiryStatus", Kind.EXPIRY_STATUS_TERMS)),
            // Classic: false → manual only; true/absent → both. Auto-only uses isAuto below.
            Map.entry("includeAutoWaivers", new FilterDef("policyWaiverAuto", Kind.AUTO_WAIVER_TOGGLE)),
            Map.entry("isAuto", new FilterDef("policyWaiverIsAuto", Kind.BOOLEAN_TERMS))));
  }
}

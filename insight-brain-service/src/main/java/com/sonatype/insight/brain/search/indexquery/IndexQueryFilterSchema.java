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
     * The WAIVER lifecycle status rail: active / expiring / expired / auto-waived. Distinct from the
     * {@code status} request-status filter over {@code policyWaiverRequestStatus}.
     */
    WAIVER_LIFECYCLE_STATUS,
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
            // applicationCategoryIds is the id-keyed structured filter: the categories facet
            // aggregates on applicationCategoryId (see IndexQueryService FACET_FIELDS), so this
            // must compile to that same field or the own-clause removal in computeFacets never
            // finds it and the facet collapses to the current selection.
            Map.entry("applicationCategoryIds", new FilterDef("applicationCategoryId", Kind.TERMS)),
            // organizationIds/applicationIds are the id-keyed structured filters, mirroring
            // applicationCategoryIds above and the WAIVER schema below: the organizations facet
            // aggregates on parentOrganizationId and the applications facet on applicationId, so a
            // rail sending bucket ids straight back must compile to those same fields. A name-keyed
            // filter would not match an id at all, and own-clause removal keys on the aggregated
            // field, so the rail would also collapse to the current selection.
            Map.entry("organizationIds", new FilterDef("parentOrganizationId", Kind.TERMS)),
            Map.entry("applicationIds", new FilterDef("applicationId", Kind.TERMS)),
            Map.entry("stages", new FilterDef("applicationViolationStage", Kind.TERMS)),
            Map.entry("policyTypes", new FilterDef("applicationViolationPolicyType", Kind.TERMS)),
            Map.entry("violationStates", new FilterDef("applicationViolationState", Kind.TERMS)),
            Map.entry("policyThreatLevel", new FilterDef("applicationMaxPolicyThreatLevel", Kind.RANGE)),
            // age: RANGE over applicationLastEvaluationTimeEpochMs; caller resolves window -> [fromEpochMs, toEpochMs]
            // epoch bounds. No server-side window resolution is applied here.
            Map.entry("age", new FilterDef("applicationLastEvaluationTimeEpochMs", Kind.RANGE))),
        // applicationCategoryName is denormalized (multi-valued) onto violation docs, so the
        // categories filter is honoured here too. states/waiverType compile against
        // policyViolationWaiverStatus (OPEN = not waived). Values within one filter are OR'd; distinct
        // filters AND-narrow (standard faceted-search rail).
        IndexQueryType.VIOLATION, Map.ofEntries(
            Map.entry("query", FREE_TEXT_QUERY),
            Map.entry("organizations", new FilterDef("organizationName", Kind.TERMS)),
            Map.entry("applications", new FilterDef("applicationName", Kind.TERMS)),
            Map.entry("applicationCategories", new FilterDef("applicationCategoryName", Kind.TERMS)),
            // See the APPLICATION applicationCategoryIds comment above -- same id-keyed field, same
            // reason (matches the applicationCategoryId aggregation the categories facet uses here too).
            Map.entry("applicationCategoryIds", new FilterDef("applicationCategoryId", Kind.TERMS)),
            // organizationIds/applicationIds are the id-keyed structured filters, mirroring
            // applicationCategoryIds above and the WAIVER schema below: the organizations facet
            // aggregates on parentOrganizationId and the applications facet on applicationId, so a
            // rail sending bucket ids straight back must compile to those same fields. A name-keyed
            // filter would not match an id at all, and own-clause removal keys on the aggregated
            // field, so the rail would also collapse to the current selection.
            Map.entry("organizationIds", new FilterDef("parentOrganizationId", Kind.TERMS)),
            Map.entry("applicationIds", new FilterDef("applicationId", Kind.TERMS)),
            Map.entry("stages", new FilterDef("policyEvaluationStage", Kind.TERMS)),
            Map.entry("policyTypes", new FilterDef("policyViolationThreatCategory", Kind.TERMS)),
            Map.entry("states", new FilterDef("policyViolationWaiverStatus", Kind.STATE)),
            Map.entry("waiverType", new FilterDef("policyViolationWaiverStatus", Kind.WAIVER_TYPE)),
            // policyViolationThreatLevel is set only on POLICY_VIOLATION docs; LEGAL_VIOLATION docs
            // carry no queryable threat-level field, so this range narrows policy violations only.
            Map.entry("policyThreatLevel", new FilterDef("policyViolationThreatLevel", Kind.RANGE))),
        // organizationIds is the id-keyed structured filter matching the organizations facet's
        // parentOrganizationId aggregation. Policy documents carry no applicationId, so there is no
        // applicationIds counterpart.
        IndexQueryType.POLICY, Map.of(
            "query", FREE_TEXT_QUERY,
            "policyTypes", new FilterDef("policyThreatCategory", Kind.TERMS),
            "organizations", new FilterDef("organizationName", Kind.TERMS),
            "organizationIds", new FilterDef("parentOrganizationId", Kind.TERMS),
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
        // policyWaiverRequestStatus discriminator; lifecycleStatus filters the committed-waiver
        // active/expiring/expired/auto-waived rail; scope by the indexed scope; policyTypes by
        // policyWaiverPolicyType. All standard TERMS are OR-within / AND-across.
        // organizationIds/applicationIds are the id-keyed structured filters: the frontend rail
        // sends facet bucket ids straight back. organizationIds compiles directly to
        // parentOrganizationId -- the same field the organizations facet aggregates on (see
        // IndexQueryService FACET_FIELDS) -- rather than organizationId, which relies on the
        // organizationId->parentOrganizationId rewrite in AbstractSearchIndexClient#createInitialQuery.
        // That rewrite runs on the raw query string AFTER IndexQueryFilterCompiler builds
        // clausesByField (keyed by the field the filter COMPILED to, pre-rewrite), so computeFacets'
        // own-clause removal -- compiled.clausesByField().get(facet.indexField()) -- would never find
        // an organizationId clause under the facet's parentOrganizationId key, and the org facet would
        // collapse to just the selected org whenever a client filtered by organizationIds. Targeting
        // parentOrganizationId directly sidesteps the rewrite entirely (the name-keyed organizations
        // filter still relies on the equivalent organizationName rewrite).
        IndexQueryType.WAIVER, Map.ofEntries(
            Map.entry("query", FREE_TEXT_QUERY),
            Map.entry("organizations", new FilterDef("organizationName", Kind.TERMS)),
            Map.entry("organizationIds", new FilterDef("parentOrganizationId", Kind.TERMS)),
            Map.entry("applications", new FilterDef("applicationName", Kind.TERMS)),
            Map.entry("applicationIds", new FilterDef("applicationId", Kind.TERMS)),
            // Deprecated alias of applicationIds, kept for back-compat with an existing structured filter.
            Map.entry("applicationId", new FilterDef("applicationId", Kind.TERMS)),
            Map.entry("policy", new FilterDef("policyWaiverPolicyName", Kind.TERMS)),
            Map.entry("policyTypes", new FilterDef("policyWaiverPolicyType", Kind.TERMS)),
            Map.entry("scope", new FilterDef("policyWaiverScope", Kind.TERMS)),
            Map.entry("status", new FilterDef("policyWaiverRequestStatus", Kind.TERMS)),
            Map.entry("lifecycleStatus", new FilterDef(null, Kind.WAIVER_LIFECYCLE_STATUS)),
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

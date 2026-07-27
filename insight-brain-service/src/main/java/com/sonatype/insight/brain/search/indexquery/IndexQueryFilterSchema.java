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
     * <li>{@code true} — include BOTH manual and auto waivers (no clause added);</li>
     * <li>{@code false} OR absent — exclude auto waivers, i.e. add {@code policyWaiverAuto:"false"}
     * so only manual waivers match.</li>
     * </ul>
     * The default (absent) is manual-only, matching the request shape where {@code includeAutoWaivers:
     * true} explicitly opts in to also seeing auto waivers.
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
    EXPIRY_STATUS
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
        // No policyThreatLevel/violationStates/stages on APPLICATION: they are aggregations over the
        // app's violations, not indexed application attributes (policyEvaluationStage is written only on
        // violation/vuln docs, never on an APPLICATION doc), so they cannot be honoured here.
        // applicationCategoryName is now denormalized (multi-valued) onto APPLICATION docs, so the
        // categories filter is honoured. Filter semantics: values within one filter are OR'd; distinct
        // filters AND-narrow (standard faceted-search rail — see the filter compiler).
        IndexQueryType.APPLICATION, Map.of(
            "query", FREE_TEXT_QUERY,
            "organizations", new FilterDef("organizationName", Kind.TERMS),
            "applications", new FilterDef("applicationName", Kind.TERMS),
            "applicationCategories", new FilterDef("applicationCategoryName", Kind.TERMS)),
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
        // true -> both kinds; false/absent -> manual only (see Kind.AUTO_WAIVER_TOGGLE).
        // applications/applicationId narrow to app-scoped waivers only: org-scoped waivers carry no
        // applicationName/Id (setOwner writes them only for an Application owner), the same owner
        // asymmetry as organizations (which narrows org-scoped waivers only). policy filters on the
        // waiver's policy NAME, mirroring how the other entity types filter by human-readable names.
        // expiry is the active-vs-expired status toggle (see Kind.EXPIRY_STATUS), compiled against the
        // policyWaiverExpiresAtEpochMs numeric point vs server-now with null-expiry treated as active.
        IndexQueryType.WAIVER, Map.of(
            "query", FREE_TEXT_QUERY,
            "organizations", new FilterDef("organizationName", Kind.TERMS),
            "applications", new FilterDef("applicationName", Kind.TERMS),
            "applicationId", new FilterDef("applicationId", Kind.TERMS),
            "policy", new FilterDef("policyWaiverPolicyName", Kind.TERMS),
            "policyThreatLevel", new FilterDef("policyWaiverThreatLevel", Kind.RANGE),
            "expiry", new FilterDef("policyWaiverExpiresAtEpochMs", Kind.EXPIRY_STATUS),
            "includeAutoWaivers", new FilterDef("policyWaiverAuto", Kind.AUTO_WAIVER_TOGGLE)));
  }
}

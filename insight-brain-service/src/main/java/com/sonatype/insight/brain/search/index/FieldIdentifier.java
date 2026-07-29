/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

public enum FieldIdentifier
{
  ITEM_TYPE("itemType"),
  ORGANIZATION_ID("organizationId"),
  ORGANIZATION_NAME("organizationName"),
  APPLICATION_ID("applicationId"),
  APPLICATION_NAME("applicationName"),
  APPLICATION_PUBLIC_ID("applicationPublicId"),
  POLICY_EVALUATION_STAGE("policyEvaluationStage"),
  APPLICATION_VERSION("applicationVersion"),
  REPORT_ID("reportId"),
  COMPONENT_HASH("componentHash"),
  COMPONENT_FORMAT("componentFormat"),
  COMPONENT_NAME("componentName"),
  COMPONENT_COORDINATE("componentCoordinate"),
  COMPONENT_COORDINATE_ARTIFACT_ID("componentCoordinateArtifactId"),
  COMPONENT_COORDINATE_GROUP_ID("componentCoordinateGroupId"),
  COMPONENT_COORDINATE_NAME("componentCoordinateName"),
  COMPONENT_COORDINATE_EXTENSION("componentCoordinateExtension"),
  COMPONENT_COORDINATE_ARCHITECTURE("componentCoordinateArchitecture"),
  COMPONENT_COORDINATE_PLATFORM("componentCoordinatePlatform"),
  COMPONENT_COORDINATE_VERSION("componentCoordinateVersion"),
  COMPONENT_COORDINATE_CLASSIFIER("componentCoordinateClassifier"),
  COMPONENT_COORDINATE_QUALIFIER("componentCoordinateQualifier"),
  COMPONENT_COORDINATE_PACKAGE_ID("componentCoordinatePackageId"),
  VULNERABILITY_ID("vulnerabilityId"),
  VULNERABILITY_SEVERITY("vulnerabilitySeverity"),
  VULNERABILITY_STATUS("vulnerabilityStatus"),
  VULNERABILITY_DESCRIPTION("vulnerabilityDescription"),
  APPLICATION_CATEGORY_ID("applicationCategoryId"),
  APPLICATION_CATEGORY_NAME("applicationCategoryName"),
  APPLICATION_CATEGORY_COLOR("applicationCategoryColor"),
  APPLICATION_CATEGORY_DESCRIPTION("applicationCategoryDescription"),
  COMPONENT_LABEL_ID("componentLabelId"),
  COMPONENT_LABEL_NAME("componentLabelName"),
  COMPONENT_LABEL_COLOR("componentLabelColor"),
  COMPONENT_LABEL_DESCRIPTION("componentLabelDescription"),
  POLICY_ID("policyId"),
  POLICY_NAME("policyName"),
  POLICY_THREAT_CATEGORY("policyThreatCategory"),
  POLICY_THREAT_LEVEL("policyThreatLevel"),
  PARENT_ORGANIZATION_NAME("parentOrganizationName"),
  PARENT_ORGANIZATION_ID("parentOrganizationId"),
  SBOM_SPECIFICATION("sbomSpecification"),

  POLICY_VIOLATION_ID("policyViolationId"),
  POLICY_VIOLATION_THREAT_CATEGORY("policyViolationThreatCategory"),
  POLICY_VIOLATION_THREAT_LEVEL("policyViolationThreatLevel"),
  POLICY_VIOLATION_POLICY_NAME("policyViolationPolicyName"),
  POLICY_VIOLATION_POLICY_ID("policyViolationPolicyId"),
  POLICY_VIOLATION_WAIVER_STATUS("policyViolationWaiverStatus"),
  POLICY_VIOLATION_CONSTRAINT_NAME("policyViolationConstraintName"),

  COMPONENT_EFFECTIVE_LICENSE_ID("componentEffectiveLicenseId"),
  COMPONENT_EFFECTIVE_LICENSE_NAME("componentEffectiveLicenseName"),
  COMPONENT_LICENSE_THREAT_GROUP_NAME("componentLicenseThreatGroupName"),
  COMPONENT_LICENSE_THREAT_LEVEL("componentLicenseThreatLevel"),

  /**
   * Denormalized, multi-valued permission filter: every context ID granting READ on this document
   * — the owning app ID (if any) plus the doc's org and its ancestors, excluding the root sentinel
   * — matched by a single terms query against the caller's READ context IDs.
   * Case-sensitive: store/query the raw {@code Owner.getId()} verbatim (no normalizer). This is
   * safe — and equivalent to the legacy lowercase-normalized ORGANIZATION_ID/APPLICATION_ID path
   * — because entity IDs are lowercase-hex UUIDs ({@code IdUtil.newUUID()}), so raw already equals
   * lowercased. If a non-lowercase context ID is ever introduced on either the index or the
   * permission-lookup side, the failure mode is a silent filter miss (docs disappear), not an
   * error; keep both sides emitting the raw lowercase-hex id.
   */
  ALLOWED_CONTEXT_IDS("allowedContextIds"),

  /** Content-derived cursor tie-breaker; stable across reindex and both backends, unlike a transient docId/_id. */
  DOCUMENT_KEY("documentKey"),

  // Policy waiver fields — written when ItemType.POLICY_WAIVER documents are indexed.
  POLICY_WAIVER_ID("policyWaiverId"),
  POLICY_WAIVER_POLICY_NAME("policyWaiverPolicyName"),
  POLICY_WAIVER_POLICY_ID("policyWaiverPolicyId"),
  POLICY_WAIVER_REASON("policyWaiverReason"),
  POLICY_WAIVER_COMMENT("policyWaiverComment"),
  POLICY_WAIVER_CREATED_AT("policyWaiverCreatedAt"),
  // Numeric-long twin of POLICY_WAIVER_CREATED_AT (epoch millis) backing the WAIVER default
  // created-desc sort. The keyword POLICY_WAIVER_CREATED_AT stays for display; the numeric sort
  // doc-values twin is emitted in LuceneIndexingContext (Lucene) / the long mapping (OpenSearch),
  // mirroring POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.
  POLICY_WAIVER_CREATED_AT_EPOCH_MS("policyWaiverCreatedAtEpochMs"),
  POLICY_WAIVER_EXPIRES_AT("policyWaiverExpiresAt"),
  // Range-queryable numeric-long twin of POLICY_WAIVER_EXPIRES_AT (epoch millis), for the active-vs-
  // expired filter. The keyword POLICY_WAIVER_EXPIRES_AT stays for display/sort; this is filter-only.
  POLICY_WAIVER_EXPIRES_AT_EPOCH_MS("policyWaiverExpiresAtEpochMs"),
  POLICY_WAIVER_SCOPE_OWNER_ID("policyWaiverScopeOwnerId"),
  POLICY_WAIVER_SCOPE_OWNER_TYPE("policyWaiverScopeOwnerType"),
  POLICY_WAIVER_THREAT_LEVEL("policyWaiverThreatLevel"),
  POLICY_WAIVER_WAIVED_BY("policyWaiverWaivedBy"),
  // Auto-vs-manual discriminator: "true" for AutoPolicyWaiver docs, "false" for PolicyWaiver docs.
  POLICY_WAIVER_AUTO("policyWaiverAuto"),
  // Ana Waivers list alias of POLICY_WAIVER_AUTO (same true/false keyword values). Kept as a distinct
  // field so the Ana {@code isAuto} TERMS filter and row projection stay stable if the left-nav
  // discriminator label ever diverges.
  POLICY_WAIVER_IS_AUTO("policyWaiverIsAuto"),
  // Denormalized Active/Expired/Never keyword for the Ana {@code expiryStatus} TERMS filter.
  // Complements POLICY_WAIVER_EXPIRES_AT_EPOCH_MS (query-time active-vs-expired range on left-nav).
  POLICY_WAIVER_EXPIRY_STATUS("policyWaiverExpiryStatus"),
  // Denormalized policy threat category (SECURITY/LICENSE/QUALITY/OTHER), written on both POLICY_WAIVER
  // and POLICY_WAIVER_REQUEST docs so the policyType facet/filter resolves without a per-row policy load.
  // Namespaced (not POLICY_THREAT_CATEGORY) so the waiver policyType filter cannot collide with the
  // POLICY entity's own threat-category field. Null (unresolvable policy) reads back as OTHER.
  POLICY_WAIVER_POLICY_TYPE("policyWaiverPolicyType"),
  // Facet/filter scope granularity (application/organization/component) written on both POLICY_WAIVER
  // and POLICY_WAIVER_REQUEST docs. Distinct from POLICY_WAIVER_SCOPE_OWNER_TYPE (the RBAC/href owner
  // type, always application/organization): a waiver owned by an app/org but TARGETING a specific
  // component reports scope "component" here while keeping its owner type for RBAC and display.
  POLICY_WAIVER_SCOPE("policyWaiverScope"),

  // Policy waiver REQUEST fields — written only on ItemType.POLICY_WAIVER_REQUEST documents.
  // Discriminator keyword {REQUESTED,APPROVED,REJECTED}; the waiverStates filter selects requested/
  // rejected requests by this value (approved requests are indexed but never selected).
  POLICY_WAIVER_REQUEST_STATUS("policyWaiverRequestStatus"),
  REQUESTER_NAME("requesterName"),
  REVIEWER_NAME("reviewerName"),
  // Review time stored as an ISO-8601 keyword token (display only), mirroring the waiver date fields.
  REVIEW_TIME("reviewTime"),
  REJECTION_REASON("rejectionReason"),
  NOTE_TO_REVIEWER("noteToReviewer"),

  // Application evaluation denormalization — written on ItemType.APPLICATION docs.
  /**
   * Epoch-millis of the application's latest evaluation (max evaluation time across stages).
   * Range-queryable {@link org.apache.lucene.document.LongPoint} + numeric sort doc-values +
   * stored display value. Null when the application has never been evaluated.
   */
  APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS("applicationLastEvaluationTimeEpochMs"),

  /**
   * Per-stage x severity violation breakdown for the evaluation-card pills. Multi-valued keyword;
   * each value encodes {@code "stage:severity:count"} (e.g. {@code "build:critical:3"}), where
   * stage is the {@link com.sonatype.clm.dto.model.stage.StageType} id, severity is the lowercase
   * {@link com.sonatype.insight.brain.utils.ThreatLevel} name, and count is the number of active
   * (unfixed and unwaived) violations in that (stage, severity) bucket. Absent stages/severities emit
   * no entry (sparse).
   */
  APPLICATION_STAGE_SEVERITY_COUNT("applicationStageSeverityCount"),

  // Component violation denormalization — written on ItemType.NON_VULNERABLE_COMPONENT docs so the
  // Components leg can be filtered/sorted by the policy violations on that (app, stage) component.
  // NON_VULNERABLE_COMPONENT docs otherwise carry no violation data; the values come from the same
  // getUnfixedByApplicationIdAndStageId pass that builds the POLICY_VIOLATION docs (no extra query).
  /**
   * Multi-valued keyword: the distinct policy threat categories of the component's unfixed policy
   * violations, lower-cased ({@code security}/{@code license}/{@code quality}/{@code other}). A union,
   * so a component with a security and a license violation carries both. Absent when the component has
   * no policy violation on this (app, stage) doc.
   */
  COMPONENT_VIOLATION_POLICY_TYPE("componentViolationPolicyType"),

  /**
   * Multi-valued keyword: the distinct API violation states of the component's unfixed policy
   * violations, lower-cased ({@code open}/{@code waived}/{@code legacy}). Derived from the same
   * waiver-status classification as {@code policyViolationWaiverStatus} (Active&nbsp;&rarr;&nbsp;open,
   * pure-legacy&nbsp;&rarr;&nbsp;legacy, Waived/AutoWaived&nbsp;&rarr;&nbsp;waived). Legacy is a
   * distinct grandfathered-in state. Absent when the component has no policy violation.
   */
  COMPONENT_VIOLATION_STATE("componentViolationState"),

  /**
   * The maximum policy threat level (0&ndash;10) across the component's unfixed policy violations on
   * this (app, stage) doc. Range-queryable {@link org.apache.lucene.document.IntPoint} + numeric sort
   * doc-values + stored display value. Absent when the component has no policy violation.
   */
  COMPONENT_MAX_POLICY_THREAT_LEVEL("componentMaxPolicyThreatLevel"),

  // Local vulnerability first-seen denormalization — written on ItemType.SECURITY_VULNERABILITY docs.
  /**
   * Epoch-millis of when this IQ first detected the vulnerability, resolved from the earliest
   * {@code open_time} across the policy violations triggered by this vuln refId on this (app, stage)
   * doc. Display-backing {@link org.apache.lucene.document.LongPoint} (range-queryable for the
   * "first seen (within ...)" window filter) + stored display value; no sort doc-values twin (the
   * local Vulnerabilities tab has no first-seen sort). Absent when the vuln triggers no policy
   * violation (informational / non-triggering) so no first-detection time exists.
   */
  VULNERABILITY_FIRST_SEEN_EPOCH_MS("vulnerabilityFirstSeenEpochMs"),

  /**
   * Max raw policy threat level (0-10) across the application's active (unfixed, unwaived, non-legacy)
   * violations. Range-queryable {@link org.apache.lucene.document.IntPoint} + numeric sort doc-values +
   * stored display value, mirroring {@link #APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS}. Absent when the
   * app has no active violations (so the doc omits the field and reads as "no threat"). Backs the
   * Applications policy-threat-level range filter and the policy-threat-level sort (desc).
   */
  APPLICATION_MAX_POLICY_THREAT_LEVEL("applicationMaxPolicyThreatLevel"),

  /**
   * Set of {@link com.sonatype.insight.brain.model.policy.StageType} ids that have at least one active
   * violation on the application. Multi-valued keyword (validated against the global stage registry);
   * a stage with no active violation emits no entry. Backs the Applications stages filter.
   */
  APPLICATION_VIOLATION_STAGE("applicationViolationStage"),

  /**
   * Set of policy threat categories ({@code security}/{@code license}/{@code quality}/{@code other},
   * lowercased) present among the application's active violations. Multi-valued keyword. Backs the
   * Applications policy-types filter.
   */
  APPLICATION_VIOLATION_POLICY_TYPE("applicationViolationPolicyType"),

  /**
   * Set of violation states ({@code open}/{@code waived}/{@code legacy}, lowercased) present among the
   * application's unfixed violations. Multi-valued keyword. Distinct from the active-only stage/severity
   * rollup: this classifies the wider unfixed set so waived and legacy states surface, but the
   * active-only display pills ({@link #APPLICATION_STAGE_SEVERITY_COUNT}) are unaffected. Backs the
   * Applications violation-state filter.
   */
  APPLICATION_VIOLATION_STATE("applicationViolationState"),

  /**
   * Worst (minimum) violation-state priority across the application's states, Open=0/Waived=1/Legacy=2
   * (the prototype's VIOLATION_STATE_PRIORITY). Range-queryable {@link org.apache.lucene.document.IntPoint}
   * + numeric sort doc-values + stored value. Absent when the app has no violations, so under an ascending
   * "violation state" sort (Open first) an app with no violations sorts last. Backs the violation-state sort.
   */
  APPLICATION_VIOLATION_STATE_SORT_ORDINAL("applicationViolationStateSortOrdinal");

  public final String label;

  FieldIdentifier(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}

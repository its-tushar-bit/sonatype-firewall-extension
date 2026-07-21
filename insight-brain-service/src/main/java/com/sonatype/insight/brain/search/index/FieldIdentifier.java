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
  POLICY_WAIVER_EXPIRES_AT("policyWaiverExpiresAt"),
  POLICY_WAIVER_SCOPE_OWNER_ID("policyWaiverScopeOwnerId"),
  POLICY_WAIVER_SCOPE_OWNER_TYPE("policyWaiverScopeOwnerType"),
  POLICY_WAIVER_THREAT_LEVEL("policyWaiverThreatLevel"),
  POLICY_WAIVER_WAIVED_BY("policyWaiverWaivedBy");

  public final String label;

  FieldIdentifier(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}

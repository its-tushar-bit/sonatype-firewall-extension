/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Data-only filter tree for the global-search "add search terms" filter builder.
 *
 * Each leaf carries a human label plus the exact query syntax it inserts into
 * the search input (e.g. `itemType:APPLICATION`). The FilterBar (omnibar) and the
 * results-page inline filter bar (a later branch) both render this tree, so it
 * lives here as pure data with no React / rendering concerns.
 *
 * Ported from the Nexus One prototype's
 * `src/components/global-search/products/lifecycle-v1.ts` FILTER_TREE, then
 * cross-checked field-by-field against the backend query grammar's field
 * registry (FieldMap.java under
 * insight-brain-service/.../search/global/fieldmap). Every field token below is
 * accepted by that registry, so an inserted leaf never produces an
 * "Unknown filter" parser warning. Two enum value lists were corrected to the
 * backend's indexed vocabulary (see notes on SBOM_SPECIFICATIONS and
 * WAIVER_STATUSES) — the prototype's values would have been silently ignored.
 */

/** A selectable leaf: a full or partial `field:value` predicate to insert. */
export interface FilterLeaf {
  /** Visible human label (e.g. "Public ID"). */
  readonly label: string;
  /**
   * Syntax inserted into the input when chosen. A trailing `:` or `:""` marks an
   * "incomplete" leaf that expects the user to type a value; anything else is a
   * "complete" leaf (a full predicate). A trailing `""` lands the caret between
   * the quotes so the user can type the value immediately.
   */
  readonly syntax: string;
  /**
   * Optional fixed enum values. When present the leaf renders one more submenu
   * level (a value flyout); picking a value inserts `${syntax}${value}`, with the
   * value double-quoted when it contains whitespace (see quoteEnumValue) so the
   * backend parser reads it as a single phrase rather than splitting on the space.
   */
  readonly values?: readonly string[];
}

/** A named group of leaves rendered as a 2nd-level submenu (e.g. Application › Category). */
export interface FilterGroup {
  readonly label: string;
  readonly leaves: readonly FilterLeaf[];
}

/** A top-level filter category button. May carry direct leaves and/or grouped submenus. */
export interface FilterNode {
  readonly label: string;
  /** Leaves rendered directly under the category button. */
  readonly leaves?: readonly FilterLeaf[];
  /** Grouped leaves rendered as nested submenus. */
  readonly groups?: readonly FilterGroup[];
}

/**
 * A leaf is "complete" (a full predicate that can commit/run as-is) when its
 * syntax carries a value already — i.e. it does NOT end with a bare `:` or `:""`.
 * Enum leaves become complete only after a value is appended, so the raw leaf
 * syntax (ending `:`) is treated as incomplete here; the resolved
 * `${syntax}${value}` string is complete. Shared by the omnibar (defer to Enter)
 * and the results page (commit immediately).
 */
export function isCompleteSyntax(syntax: string): boolean {
  return !syntax.endsWith(':') && !syntax.endsWith(':""');
}

// -----------------------------------------------------------------------------
// Enum value lists. Values match the backend FieldMap's indexed vocabulary so a
// chosen value never produces a parser warning.
// -----------------------------------------------------------------------------

/** FieldMap CATEGORY_COLORS — shared by applicationCategoryColor + componentLabelColor. */
const CATEGORY_COLORS: readonly string[] = [
  'light-red',
  'light-green',
  'light-blue',
  'light-purple',
  'dark-red',
  'dark-green',
  'dark-blue',
  'dark-purple',
  'orange',
  'yellow',
];

/** FieldMap THREAT_CATEGORIES. */
const THREAT_CATEGORIES: readonly string[] = ['security', 'license', 'quality', 'other'];

/** FieldMap EVALUATION_STAGES. */
const EVALUATION_STAGES: readonly string[] = [
  'proxy',
  'develop',
  'source',
  'build',
  'stage-release',
  'release',
  'operate',
  'compliance',
];

/** FieldMap VULNERABILITY_STATUSES. */
const VULNERABILITY_STATUSES: readonly string[] = ['Open', 'Acknowledged', 'Not Applicable', 'Confirmed'];

/**
 * FieldMap SBOM_SPECIFICATIONS. The prototype listed `CycloneDx`; the backend
 * indexes `CycloneDX`, so the corrected casing is used to avoid a no-match.
 */
const SBOM_SPECIFICATIONS: readonly string[] = ['CycloneDX', 'SPDX'];

/**
 * FieldMap WAIVER_STATUSES for policyViolationWaiverStatus. The prototype listed
 * `Active` / `Expired`; the backend's indexed vocabulary is `Active` / `Waived` /
 * `AutoWaived`, so those are used (the prototype's `Expired` was never a valid
 * value for this field and would have matched nothing).
 */
const WAIVER_STATUSES: readonly string[] = ['Active', 'Waived', 'AutoWaived'];

/**
 * The filter tree. Categories, order, labels and syntax mirror the prototype's
 * FILTER_TREE; every field token is present in the backend FieldMap.
 *
 * Prototype leaves dropped: none — all leaves map to a FieldMap-known field.
 * Enum values corrected to the FieldMap vocabulary: sbomSpecification (CycloneDX)
 * and policyViolationWaiverStatus (Active/Waived/AutoWaived).
 */
export const FILTER_TREE: readonly FilterNode[] = [
  {
    label: 'Type',
    leaves: [
      { label: 'Application', syntax: 'itemType:APPLICATION' },
      { label: 'Component', syntax: 'itemType:COMPONENT' },
      { label: 'Security Vulnerability', syntax: 'itemType:SECURITY_VULNERABILITY' },
      { label: 'Policy Violation', syntax: 'itemType:POLICY_VIOLATION' },
      { label: 'Legal Violation', syntax: 'itemType:LEGAL_VIOLATION' },
    ],
  },
  {
    label: 'Application',
    groups: [
      {
        label: 'Category',
        leaves: [
          { label: 'Color', syntax: 'applicationCategoryColor:', values: CATEGORY_COLORS },
          { label: 'Description', syntax: 'applicationCategoryDescription:""' },
          { label: 'ID', syntax: 'applicationCategoryId:' },
          { label: 'Name', syntax: 'applicationCategoryName:' },
        ],
      },
    ],
    leaves: [
      { label: 'ID', syntax: 'applicationId:' },
      { label: 'Name', syntax: 'applicationName:""' },
      { label: 'Public ID', syntax: 'applicationPublicId:' },
      { label: 'SBOM Specification', syntax: 'sbomSpecification:', values: SBOM_SPECIFICATIONS },
      { label: 'Version', syntax: 'applicationVersion:' },
    ],
  },
  {
    label: 'Component',
    groups: [
      {
        label: 'Label',
        leaves: [
          { label: 'Color', syntax: 'componentLabelColor:', values: CATEGORY_COLORS },
          { label: 'Description', syntax: 'componentLabelDescription:""' },
          { label: 'ID', syntax: 'componentLabelId:' },
          { label: 'Name', syntax: 'componentLabelName:' },
        ],
      },
    ],
    leaves: [
      { label: 'Coordinate Artifact ID', syntax: 'componentCoordinateArtifactId:' },
      { label: 'Coordinate Group ID', syntax: 'componentCoordinateGroupId:' },
      { label: 'Coordinate Name', syntax: 'componentCoordinateName:""' },
      { label: 'File Extension', syntax: 'componentCoordinateExtension:' },
      { label: 'Format', syntax: 'componentFormat:' },
      { label: 'Hash', syntax: 'componentHash:' },
      { label: 'Name', syntax: 'componentName:' },
      { label: 'Package ID', syntax: 'componentCoordinatePackageId:' },
      { label: 'Target Architecture', syntax: 'componentCoordinateArchitecture:' },
      { label: 'Target Platform', syntax: 'componentCoordinatePlatform:' },
      { label: 'Version', syntax: 'componentCoordinateVersion:' },
      { label: 'Version Classifier', syntax: 'componentCoordinateClassifier:' },
      { label: 'Version Qualifier', syntax: 'componentCoordinateQualifier:' },
    ],
  },
  {
    label: 'License',
    leaves: [
      { label: 'ID', syntax: 'componentEffectiveLicenseId:' },
      { label: 'Name', syntax: 'componentEffectiveLicenseName:' },
      { label: 'Threat Group Name', syntax: 'componentLicenseThreatGroupName:""' },
      { label: 'Threat Level (e.g. [8 TO 10])', syntax: 'componentLicenseThreatLevel:' },
    ],
  },
  {
    label: 'Organization',
    leaves: [
      { label: 'ID', syntax: 'organizationId:' },
      { label: 'Name', syntax: 'organizationName:""' },
    ],
  },
  {
    label: 'Policy',
    leaves: [
      { label: 'Policy ID', syntax: 'policyId:' },
      { label: 'Policy Name', syntax: 'policyName:' },
      { label: 'Threat Category', syntax: 'policyThreatCategory:', values: THREAT_CATEGORIES },
      { label: 'Threat Security Level (e.g. [7 TO 10])', syntax: 'policyThreatLevel:' },
    ],
  },
  {
    label: 'Violation',
    leaves: [
      { label: 'Name', syntax: 'policyViolationPolicyName:""' },
      { label: 'Constraint', syntax: 'policyViolationConstraintName:""' },
      { label: 'Policy ID', syntax: 'policyViolationPolicyId:' },
      { label: 'Threat Category', syntax: 'policyViolationThreatCategory:', values: THREAT_CATEGORIES },
      { label: 'Threat Level (e.g. [7 TO 10])', syntax: 'policyViolationThreatLevel:' },
      { label: 'Waiver Status', syntax: 'policyViolationWaiverStatus:', values: WAIVER_STATUSES },
    ],
  },
  {
    label: 'Vulnerability',
    leaves: [
      { label: 'CVE or Vulnerability ID', syntax: 'vulnerabilityId:' },
      { label: 'CVSS Severity Score (e.g. 7.1)', syntax: 'vulnerabilitySeverity:' },
      { label: 'Description', syntax: 'vulnerabilityDescription:""' },
      { label: 'Evaluation Stage', syntax: 'policyEvaluationStage:', values: EVALUATION_STAGES },
      { label: 'Report ID', syntax: 'reportId:' },
      { label: 'Status', syntax: 'vulnerabilityStatus:', values: VULNERABILITY_STATUSES },
    ],
  },
];

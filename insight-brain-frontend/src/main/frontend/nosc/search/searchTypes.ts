/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * P1-F13 / CLM-39549: TypeScript types for IQ's multi-entity global search.
 *
 * Mirrors the shape returned by GET /api/v2/search/advanced (backed by
 * IQ's existing OpenSearch index — see
 * insight-brain-service/src/main/java/com/sonatype/insight/brain/search/
 * results/SearchResultItemDTO.java for the canonical Java DTO).
 *
 * Each indexed entity is represented as a SearchResultItemDTO with `itemType`
 * + a populated subset of fields. We model that as a discriminated union
 * here so React row renderers can pivot cleanly on `itemType`.
 */

/**
 * The 8 ItemType values from
 * com.sonatype.insight.brain.search.index.ItemType. Phase 1 omnibar shows
 * 6 of them as result rows (we omit APPLICATION_CATEGORY and
 * COMPONENT_LABEL because they're filter values, not search destinations).
 */
export type ItemType =
  | 'APPLICATION'
  | 'ORGANIZATION'
  | 'NON_VULNERABLE_COMPONENT'
  | 'SECURITY_VULNERABILITY'
  | 'APPLICATION_CATEGORY'
  | 'COMPONENT_LABEL'
  | 'POLICY'
  | 'SBOM_METADATA';

export interface ComponentIdentifier {
  format: string;
  coordinates: Record<string, string>;
}

/**
 * Raw row shape returned by the backend. Most fields are optional —
 * which fields are populated depends on `itemType`. Use the type guards
 * below to narrow.
 */
export interface SearchResultItemDTO {
  itemType: ItemType;
  resultIndex: number;

  organizationId?: string;
  organizationName?: string;
  applicationId?: string;
  applicationPublicId?: string;
  applicationName?: string;
  applicationVersion?: string;
  sbomSpecification?: string;
  policyEvaluationStage?: string;
  reportId?: string;

  componentHash?: string;
  componentIdentifier?: ComponentIdentifier;
  componentName?: string;

  vulnerabilityId?: string;
  vulnerabilityDescription?: string;
  vulnerabilityStatus?: string;

  applicationCategoryId?: string;
  applicationCategoryName?: string;
  applicationCategoryColor?: string;
  applicationCategoryDescription?: string;

  componentLabelId?: string;
  componentLabelName?: string;
  componentLabelColor?: string;
  componentLabelDescription?: string;

  policyId?: string;
  policyName?: string;
  policyThreatCategory?: string;
  policyThreatLevel?: number;
}

/**
 * Result rows are nested inside a grouping wrapper. The Advanced Search
 * endpoint groups by VULNERABILITY_ID (or other group keys) so multiple
 * apps affected by the same CVE aren't returned as separate top-level
 * rows. For the typeahead we flatten every group's items back to a flat
 * list of SearchResultItemDTO.
 */
export interface GroupingByDTO {
  groupIdentifier?: string;
  groupBy?: string;
  additionalInfo?: string;
  // Optional: a group can legitimately come back without an items array (the
  // backend may omit it), and `flattenGroups` already treats it as possibly
  // undefined when calling `consume()`. Typing it required was a mismatch.
  searchResultItemDTOS?: SearchResultItemDTO[];
}

/**
 * Top-level GET /api/v2/search/advanced response shape.
 *
 * Note: the endpoint returns `groupingByDTOS` at the top level (NOT a
 * flat `searchResultItemDTOS` field as you might expect from the Java
 * SearchResultDTO source). Each group contains a `searchResultItemDTOS`
 * array. Hooks must flatten the groups before consumers see results.
 */
export interface SearchResultDTO {
  searchQuery: string;
  page: number;
  pageSize: number;
  totalNumberOfHits: number;
  isExactTotalNumberOfHits: boolean;
  groupingByDTOS?: GroupingByDTO[];
  /**
   * Some legacy code paths return items at the top level instead of
   * nested in groups. We tolerate both for forward compatibility.
   */
  searchResultItemDTOS?: SearchResultItemDTO[];
}

// -----------------------------------------------------------------------------
// Type guards
// -----------------------------------------------------------------------------
//
// One per ItemType the omnibar surfaces. Callers narrow before reading the
// type-specific fields. APPLICATION_CATEGORY and COMPONENT_LABEL are
// included in the guards (so we can filter them OUT of result rendering)
// but Phase 1 doesn't render them.

export function isApplication(r: SearchResultItemDTO): boolean {
  return r.itemType === 'APPLICATION';
}

export function isOrganization(r: SearchResultItemDTO): boolean {
  return r.itemType === 'ORGANIZATION';
}

export function isComponent(r: SearchResultItemDTO): boolean {
  return r.itemType === 'NON_VULNERABLE_COMPONENT';
}

export function isVulnerability(r: SearchResultItemDTO): boolean {
  return r.itemType === 'SECURITY_VULNERABILITY';
}

export function isPolicy(r: SearchResultItemDTO): boolean {
  return r.itemType === 'POLICY';
}

export function isSbomMetadata(r: SearchResultItemDTO): boolean {
  return r.itemType === 'SBOM_METADATA';
}

export function isApplicationCategory(r: SearchResultItemDTO): boolean {
  return r.itemType === 'APPLICATION_CATEGORY';
}

export function isComponentLabel(r: SearchResultItemDTO): boolean {
  return r.itemType === 'COMPONENT_LABEL';
}

/**
 * Phase 1: which item types we render as rows in the typeahead and full
 * results page.
 *
 * Excluded:
 *   - APPLICATION_CATEGORY, COMPONENT_LABEL — these are filter values
 *     (used in the sidebar), not destinations.
 *   - SBOM_METADATA — the indexed fields are limited to sbomSpecification
 *     ("CycloneDX 1.6", "SPDX 2.3") + sbomVersion, which aren't useful
 *     free-text typeahead signals; the bucket also covers ONLY
 *     third-party SBOMs uploaded via SBOM Manager (it does NOT include
 *     SBOMs IQ generates from scans). Components and vulnerabilities
 *     *inside* third-party SBOMs already surface as regular component /
 *     vulnerability rows. Deferred to Phase 2 — see useGlobalSearch.ts
 *     ENTITY_BUCKETS comment for rationale.
 */
export const RENDERED_ITEM_TYPES: readonly ItemType[] = [
  'APPLICATION',
  'ORGANIZATION',
  'NON_VULNERABLE_COMPONENT',
  'SECURITY_VULNERABILITY',
  'POLICY',
];

export function isRenderedType(r: SearchResultItemDTO): boolean {
  return (RENDERED_ITEM_TYPES as readonly string[]).includes(r.itemType);
}

/**
 * Display name for a result row. Used in the typeahead row primary text
 * and in result cards. Falls back through reasonable defaults if the
 * primary name field is missing.
 */
export function displayNameFor(r: SearchResultItemDTO): string {
  if (isApplication(r)) return r.applicationName || r.applicationPublicId || 'Unknown application';
  if (isOrganization(r)) return r.organizationName || 'Unknown organization';
  if (isComponent(r)) return r.componentName || r.componentHash || 'Unknown component';
  if (isVulnerability(r)) return r.vulnerabilityId || 'Unknown vulnerability';
  if (isPolicy(r)) return r.policyName || 'Unknown policy';
  if (isSbomMetadata(r)) return r.applicationName || r.reportId || 'SBOM document';
  if (isApplicationCategory(r)) return r.applicationCategoryName || '';
  if (isComponentLabel(r)) return r.componentLabelName || '';
  return '';
}

/**
 * Stable React key for a search result. Falls back to multiple identity
 * fields because the backend doesn't promise a single ID across types.
 */
export function reactKeyFor(r: SearchResultItemDTO): string {
  if (isApplication(r)) return `app:${r.applicationId ?? r.applicationPublicId}`;
  if (isOrganization(r)) return `org:${r.organizationId}`;
  if (isComponent(r)) return `comp:${r.componentHash}`;
  if (isVulnerability(r)) return `vuln:${r.vulnerabilityId}`;
  if (isPolicy(r)) return `policy:${r.policyId}`;
  if (isSbomMetadata(r)) return `sbom:${r.reportId ?? r.applicationId}`;
  if (isApplicationCategory(r)) return `cat:${r.applicationCategoryId}`;
  if (isComponentLabel(r)) return `label:${r.componentLabelId}`;
  return `${r.itemType}:${r.resultIndex}`;
}

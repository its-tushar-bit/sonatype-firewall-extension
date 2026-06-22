/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComponentDetails } from '@guide/ui-core/types';

/**
 * Forward-looking canonical type for the self-hosted Guide policy-compliance payload, mirroring the
 * backend record `GuidePolicyCompliance` 1:1 (insight-brain-guide .../api/dto/policy/). Lives in the
 * Guide SPA for now; intended to be lifted into `@guide/ui-core/types` once Guide SaaS emits the
 * same shape, at which point this file is deleted and the cast in getGuidePolicyCompliance vanishes.
 * Tracked for the ui-core lift / deletion under GUIDE-2833.
 */
export type GuidePolicyComplianceLevel = 'PASS' | 'WARN' | 'FAIL';

export interface GuideTriggerReference {
  type?: string | null;
  value?: string | null;
}

export interface GuideViolationReason {
  reason: string;
  reference?: GuideTriggerReference | null;
}

export interface GuideConstraintViolation {
  constraintId: string;
  constraintName: string;
  reasons: GuideViolationReason[];
}

export interface GuideWaiverInfo {
  scopeOwnerType?: string | null;
  scopeOwnerId?: string | null;
  expiryTime?: string | null; // ISO-8601 instant
  comment?: string | null;
}

export interface GuidePolicyViolation {
  policyId: string;
  policyName: string;
  threatLevel: number;
  actions: string[];
  waived: boolean;
  waiver?: GuideWaiverInfo | null;
  constraintViolations: GuideConstraintViolation[];
}

export interface GuidePolicyComplianceSummary {
  highestThreatLevel: number;
  worstAction: string;
  activeViolationCount: number;
  waivedViolationCount: number;
  violationCountsByCategory: Record<string, number>;
}

export interface GuidePolicyCompliance {
  compliant: boolean;
  complianceLevel: GuidePolicyComplianceLevel;
  stage?: string;
  ownerId?: string;
  summary?: GuidePolicyComplianceSummary; // absent in badge-only responses
  violations?: GuidePolicyViolation[]; // absent in badge-only responses
}

// Mirrors the backend root-organization id (see OrganizationDAO.ROOT_ORGANIZATION_ID).
export const ROOT_ORGANIZATION_ID = 'ROOT_ORGANIZATION_ID';

export type ThreatAccent = 'blue' | 'indigo' | 'yellow' | 'orange' | 'red';

/**
 * Map an IQ policy threat level (0..10) to a Radix Themes accent name, matching Lifecycle's policy
 * threat-level bands (RSC `categoryByPolicyThreatLevel` / `$policy-threat-levels`):
 * 0 none, 1 low, 2-3 moderate, 4-7 severe, 8-10 critical.
 */
export function threatAccent(level: number): ThreatAccent {
  if (level >= 8) return 'red';
  if (level >= 4) return 'orange';
  if (level >= 2) return 'yellow';
  if (level >= 1) return 'indigo';
  return 'blue';
}

/**
 * Display label for a waiver's scope. The payload only carries scopeOwnerType + scopeOwnerId (no org
 * name), so the root organization gets a friendly label and any other scope falls back to the
 * capitalized owner type (e.g. "organization" -> "Organization").
 */
export function waiverScopeLabel(waiver: GuideWaiverInfo): string {
  if (waiver.scopeOwnerId === ROOT_ORGANIZATION_ID) {
    return 'Root Organization';
  }
  const type = waiver.scopeOwnerType ?? '';
  return type ? type.charAt(0).toUpperCase() + type.slice(1) : 'Inherited';
}

/**
 * Display label for the evaluated policy stage. The payload carries the lowercase IQ stage type id
 * (e.g. "release" — self-hosted Guide always evaluates at the release stage), which we capitalize and
 * suffix with "Stage" for display (e.g. "release" -> "Release Stage").
 */
export function stageLabel(stage: string): string {
  return `${stage.charAt(0).toUpperCase() + stage.slice(1)} Stage`;
}

/**
 * Bridge ui-core's thin `ComponentDetails.policyCompliance` type ({ compliant, conditions[] }) to the
 * richer runtime shape the self-hosted backend actually sends. This single cast is the only type seam;
 * it disappears when `@guide/ui-core` adopts `GuidePolicyCompliance`.
 */
export function getGuidePolicyCompliance(
  component: ComponentDetails
): GuidePolicyCompliance | undefined {
  return component.policyCompliance as unknown as GuidePolicyCompliance | undefined;
}

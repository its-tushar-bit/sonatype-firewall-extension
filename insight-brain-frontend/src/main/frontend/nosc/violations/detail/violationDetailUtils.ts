/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { violationSidebarHref } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import type {
  ComponentDisplayNameDTO,
  ViolationDetailTabId,
  ViolationDetailsDTO,
  ViolationStageDataDTO,
} from 'MainRoot/nosc/violations/detail/violationDetailTypes';

export type { ViolationDetailTabId };

export const VIOLATION_DETAIL_TAB_IDS: ViolationDetailTabId[] = ['overview', 'vulnerability', 'waivers'];

const NEXUS_ONE_VIOLATION_DETAIL_PARENT_STATE = 'nexusOneViolationDetail';
const DEFAULT_TAB: ViolationDetailTabId = 'overview';

function isViolationDetailTabId(value: string): value is ViolationDetailTabId {
  return VIOLATION_DETAIL_TAB_IDS.includes(value as ViolationDetailTabId);
}

export function isSecurityPolicyCategory(category: string | undefined): boolean {
  return category?.toLowerCase() === 'security';
}

export function tabFromViolationDetailStateName(stateName: string | undefined): ViolationDetailTabId {
  if (!stateName?.startsWith(`${NEXUS_ONE_VIOLATION_DETAIL_PARENT_STATE}.`)) {
    return DEFAULT_TAB;
  }

  const suffix = stateName.slice(NEXUS_ONE_VIOLATION_DETAIL_PARENT_STATE.length + 1);
  return isViolationDetailTabId(suffix) ? suffix : DEFAULT_TAB;
}

export function violationDetailStateNameForTab(tab: ViolationDetailTabId): string {
  return `${NEXUS_ONE_VIOLATION_DETAIL_PARENT_STATE}.${tab}`;
}

/** Classic violation-detail sidebar deep-link (context-path / MTIQ aware). */
export function classicViolationHref(id: string): string {
  return violationSidebarHref(id);
}

/** Classic vulnerability detail deep-link (context-path / MTIQ aware). */
export function classicVulnerabilityHref(id: string): string {
  return bundleIndexUrl('classic', `/vulnerabilities/${encodeURIComponent(id)}`);
}

export function getMostRecentStageEntry(
  stageData: ViolationDetailsDTO['stageData'] | undefined,
): {
  readonly stageId: string;
  readonly scanId?: string;
  readonly evaluationTime: string;
} | null {
  return Object.entries(stageData ?? {}).reduce<{
    readonly stageId: string;
    readonly scanId?: string;
    readonly evaluationTime: string;
  } | null>((selected, [stageId, current]) => {
    const currentMs = Date.parse(current.mostRecentEvaluationTime);
    const selectedMs = selected ? Date.parse(selected.evaluationTime) : Number.NaN;
    if (
      !selected ||
      (Number.isFinite(currentMs) && (!Number.isFinite(selectedMs) || currentMs > selectedMs))
    ) {
      return {
        stageId,
        scanId: current.mostRecentScanId,
        evaluationTime: current.mostRecentEvaluationTime,
      };
    }
    return selected;
  }, null);
}

export function getMostRecentScanId(
  stageData: Record<string, ViolationStageDataDTO> | undefined,
): string | undefined {
  return getMostRecentStageEntry(stageData)?.scanId;
}

/**
 * First security vulnerability ref id from constraint reasons, when the detail
 * payload already exposes {@code SECURITY_VULNERABILITY_REFID}. Does not invent ids.
 */
export function getSecurityVulnerabilityRefId(
  details: Pick<ViolationDetailsDTO, 'constraintViolations'> | null | undefined,
): string | null {
  for (const constraint of details?.constraintViolations ?? []) {
    const reason = constraint.reasons?.find(
      (item) => item.reference?.type === 'SECURITY_VULNERABILITY_REFID',
    );
    if (reason?.reference?.value) {
      return reason.reference.value;
    }
  }
  return null;
}

export function componentDisplayNameLabel(
  displayName: ViolationDetailsDTO['displayName'] | undefined,
  fallback?: string,
): string {
  if (typeof displayName === 'string' && displayName) {
    return displayName;
  }

  const parts = (displayName as ComponentDisplayNameDTO | null | undefined)?.parts ?? [];
  // ComponentDisplayNameDTO contract: Backend builds display strings by interleaving
  // format-specific separator parts (field=null) between field parts.
  // The canonical renderer is ComponentDisplayName.toString(), which concatenates every part value.
  // Mirroring that approach fixes all formats (Maven, NuGet, Conda, RPM, etc.).
  const label = parts
    .map((part) => part.value)
    .filter(Boolean)
    .join('');
  return label || fallback || '';
}

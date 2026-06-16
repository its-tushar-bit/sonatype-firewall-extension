/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import type { FlatViolation } from './applicationDetailTypes';
import type { ApplicationDetailState } from './applicationDetailSlice';
import {
  selectApplicationPolicyThreatsState,
  selectApplicationRawReportState,
  selectApplicationReportsState,
} from './applicationDetailSlice';
import {
  extractScanId,
  flattenViolations,
  getAllViolations,
  pickLatestReport,
} from './applicationDetailUtils';

interface ApplicationDetailRootState {
  readonly applicationDetail: ApplicationDetailState;
}

const selectPolicyThreatsData = (state: ApplicationDetailRootState) =>
  selectApplicationPolicyThreatsState(state).data;

const selectRawReportData = (state: ApplicationDetailRootState) =>
  selectApplicationRawReportState(state).data;

export const selectLatestReport = createSelector(
  [selectApplicationReportsState],
  (reportsState) => (reportsState.data ? pickLatestReport(reportsState.data) : null),
);

export const selectScanId = createSelector([selectLatestReport], (latestReport) =>
  latestReport ? extractScanId(latestReport) : null,
);

export const selectViolations = createSelector([selectPolicyThreatsData], (policyThreats) =>
  flattenViolations(policyThreats),
);

export interface ViolationSummary {
  readonly violations: ReadonlyArray<FlatViolation>;
  readonly totalViolations: number;
  readonly openViolations: number;
  readonly waivedViolations: number;
  readonly criticalCount: number;
  readonly severeCount: number;
  readonly moderateCount: number;
  readonly maliciousCount: number;
}

export const selectViolationSummary = createSelector(
  [selectViolations],
  (violations): ViolationSummary => {
    let waived = 0;
    let critical = 0;
    let severe = 0;
    let moderate = 0;
    let malicious = 0;
    for (const v of violations) {
      if (v.waived) {
        waived += 1;
        continue;
      }
      // Malware Detected count excludes waived violations.
      // Previously the inline loop in ApplicationDetail.tsx counted waived malware too;
      // the Overview "Malware Detected" metric now reflects open (non-waived) malware only.
      if (/malicious/i.test(v.policyThreatCategory) || /malicious/i.test(v.policyName)) {
        malicious += 1;
      }
      const level = v.policyThreatLevel;
      if (level >= 8) critical += 1;
      else if (level >= 4) severe += 1;
      else if (level >= 2) moderate += 1;
    }
    const total = violations.length;
    return {
      violations,
      totalViolations: total,
      waivedViolations: waived,
      openViolations: total - waived,
      criticalCount: critical,
      severeCount: severe,
      moderateCount: moderate,
      maliciousCount: malicious,
    };
  },
);

export const selectViolationCountByHash = createSelector(
  [selectPolicyThreatsData],
  (policyThreats): Readonly<Record<string, number>> => {
    const countsByHash: Record<string, number> = {};
    for (const c of policyThreats?.aaData ?? []) {
      if (!c.hash || c.hash === 'null') continue;
      const active = getAllViolations(c).filter((v) => !v.waived && !v.legacyViolation);
      countsByHash[c.hash] = active.length;
    }
    return countsByHash;
  },
);

export const selectComponentCount = createSelector(
  [selectPolicyThreatsData],
  (policyThreats) => policyThreats?.aaData?.length ?? 0,
);

export const selectTotalComponentsScanned = createSelector(
  [selectRawReportData],
  (rawReport) => rawReport?.components?.length ?? 0,
);

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createDefaultLegalFilterState } from 'MainRoot/nosc/legal/legalListApi';
import {
  ViolationsListQueryState,
  violationsFiltersEqual,
} from 'MainRoot/nosc/violations/violationsListQuery';
import {
  DEFAULT_VIOLATION_THREAT_RANGE,
  isDefaultThreatRange,
} from 'MainRoot/nosc/violations/violationsListApi';
import {
  VIOLATION_THREAT_MAX,
  VIOLATION_THREAT_MIN,
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  asString,
  parseCsvParam,
  parsePageIndex,
  parseThreatRangeParam,
  serializeCsvParam,
  serializeThreatRangeParam,
} from 'MainRoot/nosc/list/listQueryCodec';

export type LegalListQueryState = ViolationsListQueryState;

/**
 * Parse Legal list URL params.
 * {@code category} tokens are free-form license threat group names (not Policy Type enums).
 * State / waiver URL tokens are ignored (not applicable to LEGAL_VIOLATION).
 */
export function parseLegalListParams(params: Record<string, unknown>): LegalListQueryState {
  const search = typeof params.q === 'string' ? params.q.trim() : '';
  const page = parsePageIndex(params.page);

  return {
    search,
    page,
    filters: {
      ...createDefaultLegalFilterState(),
      threatCategories: new Set(parseCsvParam(params.category)),
      stageIds: new Set(parseCsvParam(params.stage)),
      organizationIds: new Set(parseCsvParam(params.org)),
      applicationIds: new Set(parseCsvParam(params.app)),
      threatRange: parseThreatRangeParam(params.threat, {
        minDomain: VIOLATION_THREAT_MIN,
        maxDomain: VIOLATION_THREAT_MAX,
        defaultRange: DEFAULT_VIOLATION_THREAT_RANGE,
      }),
    },
  };
}

/**
 * Serialize Legal list state. {@code category} carries selected LTG names.
 */
export function buildLegalListRouteParams(
  state: LegalListQueryState,
): Record<string, string | undefined> {
  return {
    q: state.search.trim() || undefined,
    page: state.page > 0 ? String(state.page + 1) : undefined,
    category: serializeCsvParam(state.filters.threatCategories),
    stage: serializeCsvParam(state.filters.stageIds),
    org: serializeCsvParam(state.filters.organizationIds),
    app: serializeCsvParam(state.filters.applicationIds),
    threat: serializeThreatRangeParam(state.filters.threatRange, isDefaultThreatRange),
  };
}

export function rawLegalListParamsSnapshot(params: Record<string, unknown>): string {
  return JSON.stringify({
    q: asString(params.q),
    page: asString(params.page),
    category: asString(params.category),
    stage: asString(params.stage),
    org: asString(params.org),
    app: asString(params.app),
    threat: asString(params.threat),
  });
}

export { violationsFiltersEqual };

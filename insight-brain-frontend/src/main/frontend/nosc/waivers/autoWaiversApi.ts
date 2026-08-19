/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getApplicableAutoWaiverUrl,
  getApplicableAutoWaiversURL,
  getAutoWaiverExclusionsByAutoWaiverIdUrl,
  getAutoWaiverExclusionsByExclusionIdUrl,
  getAutoWaiverExclusionsUrl,
  getAutoWaiversConfigurationURLWaiver,
  getAutoWaiversConfigurationURLnoStatus,
} from 'MainRoot/util/CLMLocation';
import type { ApiAutoPolicyWaiverDTO } from 'MainRoot/nosc/waivers/waiverTypes';

export type AutoWaiverOwnerType = 'application' | 'organization';

/** Default max threat level for new auto-waiver configs (matches Classic). */
export const DEFAULT_AUTO_WAIVER_THREAT_LEVEL = 7;

/** Match strategies accepted by the exclusion create API. */
export type AutoWaiverExclusionMatchStrategy =
  | 'EXACT_COMPONENT'
  | 'ALL_VERSIONS'
  | 'POLICY_VIOLATION';

/**
 * Row from {@code GET .../applicableAutoWaivers} — local + inherited configs for one owner.
 * Round-trips: one HTTP call (server walks hierarchy). Do not fan out per org/app from the FE.
 */
export interface ApiAutoPolicyWaiverStatusDTO {
  readonly isAutoWaiverEnabled?: boolean;
  readonly isInherited?: boolean | null;
  readonly autoPolicyWaiverId: string;
  readonly autoPolicyWaiverOwnerId: string;
  readonly autoPolicyWaiverOwnerName?: string | null;
  readonly autoPolicyWaiverOwnerType: string;
  readonly createTime?: string | null;
  readonly threatLevel?: number | null;
  readonly hasNotReachable?: boolean | null;
  readonly hasNoPathForward?: boolean | null;
  readonly scopesOperatorAny?: boolean | null;
}

export interface AutoWaiverConfigPayload {
  readonly threatLevel: number;
  readonly reachability: boolean;
  readonly pathForward: boolean;
  readonly scopesOperatorAny: boolean;
  readonly autoPolicyWaiverId?: string;
  readonly ownerId?: string;
  readonly ownerType?: string;
}

export interface AutoWaiverExclusionRequest {
  readonly applicationPublicId: string;
  readonly ownerId: string;
  readonly scanId: string;
  readonly policyViolationId: string;
  readonly autoPolicyWaiverId: string;
  readonly matchStrategy: AutoWaiverExclusionMatchStrategy;
}

export interface ApiAutoPolicyWaiverExclusionDTO {
  readonly autoPolicyWaiverExclusionId: string;
  readonly autoPolicyWaiverId: string;
  readonly ownerId?: string | null;
  readonly ownerName?: string | null;
  readonly ownerType?: string | null;
  readonly ownerPublicId?: string | null;
  readonly createTime?: string | null;
  readonly threatLevel?: number | null;
  readonly policyName?: string | null;
  readonly componentDisplayName?: string | null;
  readonly policyViolationId?: string | null;
  readonly scanId?: string | null;
}

export const MAX_LOCAL_AUTO_WAIVERS = 3;
export const DEFAULT_AUTO_WAIVER_OWNER_TYPE: AutoWaiverOwnerType = 'organization';
export const DEFAULT_AUTO_WAIVER_OWNER_ID = 'ROOT_ORGANIZATION_ID';

export async function fetchApplicableAutoWaivers(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
}): Promise<ReadonlyArray<ApiAutoPolicyWaiverStatusDTO>> {
  const { data } = await axios.get<ReadonlyArray<ApiAutoPolicyWaiverStatusDTO>>(
    getApplicableAutoWaiversURL(params.ownerType, params.ownerId),
  );
  return data ?? [];
}

export async function fetchAutoPolicyWaiver(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly autoPolicyWaiverId: string;
}): Promise<ApiAutoPolicyWaiverDTO> {
  const { data } = await axios.get<ApiAutoPolicyWaiverDTO>(
    getAutoWaiversConfigurationURLWaiver(
      params.ownerType,
      params.ownerId,
      params.autoPolicyWaiverId,
    ),
  );
  return data;
}

export async function createAutoPolicyWaiver(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly body: AutoWaiverConfigPayload;
}): Promise<ApiAutoPolicyWaiverDTO> {
  const { data } = await axios.post<ApiAutoPolicyWaiverDTO>(
    getAutoWaiversConfigurationURLnoStatus(params.ownerType, params.ownerId),
    params.body,
  );
  return data;
}

export async function updateAutoPolicyWaiver(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly autoPolicyWaiverId: string;
  readonly body: AutoWaiverConfigPayload;
}): Promise<ApiAutoPolicyWaiverDTO> {
  const { data } = await axios.put<ApiAutoPolicyWaiverDTO>(
    getAutoWaiversConfigurationURLWaiver(
      params.ownerType,
      params.ownerId,
      params.autoPolicyWaiverId,
    ),
    {
      ...params.body,
      autoPolicyWaiverId: params.autoPolicyWaiverId,
      ownerId: params.ownerId,
      ownerType: params.ownerType,
    },
  );
  return data;
}

export async function deleteAutoPolicyWaiver(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly autoPolicyWaiverId: string;
}): Promise<void> {
  await axios.delete(
    getAutoWaiversConfigurationURLWaiver(
      params.ownerType,
      params.ownerId,
      params.autoPolicyWaiverId,
    ),
  );
}

export async function fetchAutoWaiverExclusions(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly autoPolicyWaiverId: string;
  readonly page?: number;
  readonly pageSize?: number;
}): Promise<ReadonlyArray<ApiAutoPolicyWaiverExclusionDTO>> {
  const { data } = await axios.get<ReadonlyArray<ApiAutoPolicyWaiverExclusionDTO>>(
    getAutoWaiverExclusionsByAutoWaiverIdUrl(
      params.ownerType,
      params.ownerId,
      params.autoPolicyWaiverId,
    ),
    {
      params: {
        page: params.page ?? 1,
        pageSize: params.pageSize ?? 25,
      },
    },
  );
  return data ?? [];
}

export async function createAutoWaiverExclusion(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly body: AutoWaiverExclusionRequest;
}): Promise<ApiAutoPolicyWaiverExclusionDTO> {
  const { data } = await axios.post<ApiAutoPolicyWaiverExclusionDTO>(
    getAutoWaiverExclusionsUrl(params.ownerType, params.ownerId),
    params.body,
  );
  return data;
}

export async function deleteAutoWaiverExclusion(params: {
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly autoPolicyWaiverId: string;
  readonly autoPolicyWaiverExclusionId: string;
}): Promise<void> {
  await axios.delete(
    getAutoWaiverExclusionsByExclusionIdUrl(
      params.ownerType,
      params.ownerId,
      params.autoPolicyWaiverId,
      params.autoPolicyWaiverExclusionId,
    ),
  );
}

/** Applicable auto-waiver config for a single violation (O(1) — for Exclude from violation detail). */
export async function fetchApplicableAutoWaiverForViolation(
  policyViolationId: string,
): Promise<ApiAutoPolicyWaiverDTO | null> {
  try {
    const { data } = await axios.get<ApiAutoPolicyWaiverDTO>(
      getApplicableAutoWaiverUrl(policyViolationId),
    );
    return data ?? null;
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status;
    if (status === 404) return null;
    throw err;
  }
}

export function normalizeAutoWaiverOwnerType(raw: string | null | undefined): AutoWaiverOwnerType {
  const value = (raw ?? '').toLowerCase();
  if (value === 'application') return 'application';
  return 'organization';
}

export function formatAutoWaiverConditions(row: {
  readonly hasNotReachable?: boolean | null;
  readonly hasNoPathForward?: boolean | null;
  readonly reachability?: boolean | null;
  readonly pathForward?: boolean | null;
}): string {
  const parts: string[] = [];
  if (row.hasNotReachable || row.reachability) parts.push('Not reachable');
  if (row.hasNoPathForward || row.pathForward) parts.push('No path forward');
  return parts.length > 0 ? parts.join(' · ') : '—';
}

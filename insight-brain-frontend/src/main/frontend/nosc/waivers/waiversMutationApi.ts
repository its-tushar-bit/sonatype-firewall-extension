/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  deleteWaiverUrl,
  getAddPolicyViolationWaiverUrl,
  getCreatePolicyWaiverRequestUrl,
  getOwnerContextHierarchyUrl,
  getPolicyWaiverReasonsUrl,
  getReviewPolicyWaiverRequestUrl,
  getViewOrUpdatePolicyWaiverRequestUrl,
  getWaiverDetailsUrl,
} from 'MainRoot/util/CLMLocation';

/** Wire matcher tokens accepted by {@code ApiWaiverOptionsDTO.matcherStrategy}. */
export type WaiverMatcherStrategy =
  | 'DEFAULT'
  | 'EXACT_COMPONENT'
  | 'ALL_COMPONENTS'
  | 'ALL_VERSIONS';

export type WaiverOwnerType =
  | 'application'
  | 'organization'
  | 'repository'
  | 'repository_manager'
  | 'repository_container';

export type WaiverRequestReviewStatus = 'APPROVED' | 'REJECTED';

export interface WaiverOptionsPayload {
  readonly comment?: string | null;
  readonly matcherStrategy: WaiverMatcherStrategy;
  readonly expiryTime?: string | null;
  readonly waiverReasonId?: string | null;
  readonly expireWhenRemediationAvailable?: boolean;
}

export interface WaiverRequestOptionsPayload extends WaiverOptionsPayload {
  readonly noteToReviewer?: string | null;
}

export interface WaiverRequestReviewPayload extends WaiverOptionsPayload {
  readonly status: WaiverRequestReviewStatus;
  readonly rejectionReason?: string | null;
}

export interface WaiverScopeTarget {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly ownerName: string;
}

export interface PolicyWaiverReason {
  readonly id: string;
  readonly reasonText: string;
}

export interface PolicyWaiverRequestDTO {
  readonly policyWaiverRequestId?: string;
  readonly id?: string;
  readonly status?: string | null;
  readonly comment?: string | null;
  readonly noteToReviewer?: string | null;
  readonly rejectionReason?: string | null;
  readonly matcherStrategy?: string | null;
  readonly expiryTime?: string | null;
  readonly expireWhenRemediationAvailable?: boolean;
  readonly policyWaiverReasonId?: string | null;
  /** Per-caller WAIVE permission from ApiPolicyWaiverRequestDTO.canReview. */
  readonly canReview?: boolean | null;
  readonly requesterName?: string | null;
  readonly reviewerName?: string | null;
  readonly policyName?: string | null;
  readonly threatLevel?: number;
  readonly ownerType?: string | null;
  readonly ownerId?: string | null;
  readonly ownerName?: string | null;
}

type OwnerHierarchyNode = {
  readonly type?: string;
  readonly id?: string;
  readonly name?: string;
  readonly children?: ReadonlyArray<OwnerHierarchyNode> | null;
};

function flattenOwnerHierarchy(node: OwnerHierarchyNode | null | undefined): WaiverScopeTarget[] {
  if (!node?.type || !node.id) return [];
  const ownerType = String(node.type).toLowerCase() as WaiverOwnerType;
  const self: WaiverScopeTarget = {
    ownerType,
    ownerId: node.id,
    ownerName: node.name?.trim() || node.id,
  };
  const child = node.children?.[0];
  return flattenOwnerHierarchy(child).concat(self);
}

/** End-of-day local ISO for a YYYY-MM-DD calendar pick (matches Classic add-waiver). */
export function expiryDateToIsoEndOfDay(dateYyyyMmDd: string): string {
  const [year, month, day] = dateYyyyMmDd.split('-').map((part) => Number(part));
  const local = new Date(year, (month || 1) - 1, day || 1, 23, 59, 59, 999);
  const pad = (n: number, width = 2) => String(n).padStart(width, '0');
  const offsetMin = -local.getTimezoneOffset();
  const sign = offsetMin >= 0 ? '+' : '-';
  const abs = Math.abs(offsetMin);
  const tz = `${sign}${pad(Math.floor(abs / 60))}${pad(abs % 60)}`;
  return (
    `${local.getFullYear()}-${pad(local.getMonth() + 1)}-${pad(local.getDate())}`
    + `T${pad(local.getHours())}:${pad(local.getMinutes())}:${pad(local.getSeconds())}`
    + `.${pad(local.getMilliseconds(), 3)}${tz}`
  );
}

export async function fetchWaiverScopeTargets(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyId: string;
}): Promise<ReadonlyArray<WaiverScopeTarget>> {
  const { data } = await axios.get<OwnerHierarchyNode>(
    getOwnerContextHierarchyUrl(params.ownerType, params.ownerId, params.policyId),
  );
  return flattenOwnerHierarchy(data);
}

export async function fetchPolicyWaiverReasons(): Promise<ReadonlyArray<PolicyWaiverReason>> {
  const { data } = await axios.get<ReadonlyArray<{ id?: string; reasonText?: string }>>(
    getPolicyWaiverReasonsUrl(),
  );
  return (data ?? [])
    .filter((row): row is { id: string; reasonText: string } => Boolean(row.id && row.reasonText))
    .map((row) => ({ id: row.id, reasonText: row.reasonText }));
}

export async function createPolicyWaiver(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyViolationId: string;
  readonly options: WaiverOptionsPayload;
}): Promise<void> {
  await axios.post(
    getAddPolicyViolationWaiverUrl(params.ownerType, params.ownerId, params.policyViolationId),
    params.options,
  );
}

export async function updatePolicyWaiver(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyWaiverId: string;
  readonly options: WaiverOptionsPayload;
}): Promise<void> {
  await axios.put(
    getWaiverDetailsUrl(params.ownerType, params.ownerId, params.policyWaiverId),
    params.options,
  );
}

export async function deletePolicyWaiver(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyWaiverId: string;
}): Promise<void> {
  await axios.delete(deleteWaiverUrl(params.ownerType, params.ownerId, params.policyWaiverId));
}

export async function createPolicyWaiverRequest(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyViolationId: string;
  readonly options: WaiverRequestOptionsPayload;
}): Promise<PolicyWaiverRequestDTO> {
  const { data } = await axios.post<PolicyWaiverRequestDTO>(
    getCreatePolicyWaiverRequestUrl(params.ownerType, params.ownerId, params.policyViolationId),
    params.options,
  );
  return data;
}

export async function fetchPolicyWaiverRequest(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyWaiverRequestId: string;
}): Promise<PolicyWaiverRequestDTO> {
  const { data } = await axios.get<PolicyWaiverRequestDTO>(
    getViewOrUpdatePolicyWaiverRequestUrl(
      params.ownerType,
      params.ownerId,
      params.policyWaiverRequestId,
    ),
  );
  return data;
}

export async function reviewPolicyWaiverRequest(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyWaiverRequestId: string;
  readonly review: WaiverRequestReviewPayload;
}): Promise<PolicyWaiverRequestDTO> {
  const { data } = await axios.post<PolicyWaiverRequestDTO>(
    getReviewPolicyWaiverRequestUrl(
      params.ownerType,
      params.ownerId,
      params.policyWaiverRequestId,
    ),
    params.review,
  );
  return data;
}

export async function withdrawPolicyWaiverRequest(params: {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly policyWaiverRequestId: string;
}): Promise<void> {
  await axios.delete(
    getViewOrUpdatePolicyWaiverRequestUrl(
      params.ownerType,
      params.ownerId,
      params.policyWaiverRequestId,
    ),
  );
}

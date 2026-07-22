/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ViolationDetailState } from 'MainRoot/nosc/violations/detail/violationDetailSlice';

interface ViolationDetailRootState {
  readonly violationDetail: ViolationDetailState;
}

export const selectViolationDetailState = (state: ViolationDetailRootState): ViolationDetailState =>
  state.violationDetail;

export const selectViolationDetailId = (state: ViolationDetailRootState): string | null =>
  selectViolationDetailState(state).violationId;

export const selectViolationDetailIdentityState = (state: ViolationDetailRootState) =>
  selectViolationDetailState(state).identity;

export const selectViolationDetailWaiversState = (state: ViolationDetailRootState) =>
  selectViolationDetailState(state).waivers;

export const selectViolationDetailVulnerabilitySummaryState = (state: ViolationDetailRootState) =>
  selectViolationDetailState(state).vulnerabilitySummary;

export const selectViolationHasPermissionForAppWaivers = (state: ViolationDetailRootState): boolean | null =>
  selectViolationDetailState(state).hasPermissionForAppWaivers;

export const selectViolationWaiverPermissionError = (state: ViolationDetailRootState): string | null =>
  selectViolationDetailState(state).waiverPermissionError;

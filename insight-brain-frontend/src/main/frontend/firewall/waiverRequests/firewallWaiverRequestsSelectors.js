/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { path, prop } from 'ramda';

export const selectFirewallWaiverRequestsSlice = prop('firewallWaiverRequests');

export const selectWaiverRequestsLoading = createSelector(selectFirewallWaiverRequestsSlice, prop('loading'));

export const selectWaiverRequestsError = createSelector(selectFirewallWaiverRequestsSlice, prop('error'));

export const selectWaiverRequests = createSelector(selectFirewallWaiverRequestsSlice, prop('waiverRequests'));

export const selectReviewPage = createSelector(selectFirewallWaiverRequestsSlice, prop('reviewPage'));

export const selectReviewPageLoading = createSelector(selectReviewPage, prop('loading'));

export const selectReviewPageError = createSelector(selectReviewPage, prop('error'));

export const selectReviewPageWaiverRequest = createSelector(selectReviewPage, prop('waiverRequest'));

export const selectReviewPageIsSubmitting = createSelector(selectReviewPage, prop('isSubmitting'));

export const selectReviewPageSubmitError = createSelector(selectReviewPage, prop('submitError'));

export const selectRejectionReason = createSelector(selectReviewPage, prop('rejectionReason'));

export const selectReviewPageHasWaivePermission = createSelector(selectReviewPage, prop('hasWaivePermission'));

const isContainerRequest = (r) =>
  r.scopeOwnerType === 'all_repositories' || r.scopeOwnerId === 'REPOSITORY_CONTAINER_ID';

export const selectComponentRequestedCount = createSelector(
  selectWaiverRequests,
  (requests) => requests.filter((r) => !isContainerRequest(r) && r.status === 'REQUESTED').length
);

export const selectContainerRequestedCount = createSelector(
  selectWaiverRequests,
  (requests) => requests.filter((r) => isContainerRequest(r) && r.status === 'REQUESTED').length
);

export const selectContainerExistingCount = createSelector(path(['containerImageWaivers', 'waivers']), (waivers) =>
  waivers ? waivers.length : 0
);

export const selectComponentExistingCount = createSelector(path(['dashboard', 'waivers', 'results']), (results) =>
  results ? results.filter((w) => w.ownerType !== 'all_repositories').length : 0
);

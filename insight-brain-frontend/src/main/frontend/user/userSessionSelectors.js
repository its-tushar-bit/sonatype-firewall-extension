/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectUserSessionSlice = prop('userSession');

// User session data selectors
export const selectUserSessionData = createSelector(selectUserSessionSlice, prop('data'));
export const selectUserSessionLoading = createSelector(selectUserSessionSlice, prop('loading'));
export const selectUserSessionError = createSelector(selectUserSessionSlice, prop('error'));

// Computed selectors from user session data
export const selectCurrentUser = selectUserSessionData; // Alias for clarity
export const selectUsername = createSelector(selectUserSessionData, (data) => data?.username || null);
export const selectIsLoggedIn = createSelector(selectUsername, Boolean);
export const selectIsDefaultUser = createSelector(selectUsername, (username) => username === 'admin');
export const selectCanChangePassword = createSelector(selectUserSessionData, (data) => data?.internalUser ?? false);
export const selectSessionTimeoutMilliseconds = createSelector(
  selectUserSessionData,
  (data) => data?.sessionTimeoutMilliseconds
);

// Password warning selectors
export const selectShouldDisplayPasswordWarning = createSelector(
  selectUserSessionSlice,
  prop('shouldDisplayPasswordWarning')
);

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectUser = prop('user');
export const selectCurrentUser = createSelector(selectUser, prop('currentUser'));
export const selectSessionTimeoutMilliseconds = createSelector(selectCurrentUser, prop('sessionTimeoutMilliseconds'));
export const selectUsername = createSelector(selectCurrentUser, prop('username'));
export const selectIsLoggedIn = createSelector(selectUsername, Boolean);
export const selectIsDefaultUser = createSelector(selectUser, prop('isDefaultUser'));
export const selectShouldDisplayNotice = createSelector(selectUser, prop('shouldDisplayNotice'));

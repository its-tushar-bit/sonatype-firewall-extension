/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectUserSessionSlice = prop('userSession');

export const selectUserSessionData = createSelector(selectUserSessionSlice, prop('data'));

export const selectUserSessionLoading = createSelector(selectUserSessionSlice, prop('loading'));

export const selectUserSessionError = createSelector(selectUserSessionSlice, prop('error'));

export const selectUsername = createSelector(selectUserSessionData, (data) => data?.username || null);

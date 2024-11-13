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

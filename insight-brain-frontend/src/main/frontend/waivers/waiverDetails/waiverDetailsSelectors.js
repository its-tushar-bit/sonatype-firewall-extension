/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

const selectWaiverSlice = prop('waiverDetails');
const selectDeleteWaiver = prop('deleteWaiver');

export const selectWaiverDetails = createSelector(selectWaiverSlice, prop('waiverDetails'));
export const selectWaiverDetailsLoading = createSelector(selectWaiverSlice, prop('loading'));
export const selectWaiverDetailsError = createSelector(selectWaiverSlice, prop('loadError'));
export const selectWaiverToDelete = createSelector(selectDeleteWaiver, prop('waiverToDelete'));

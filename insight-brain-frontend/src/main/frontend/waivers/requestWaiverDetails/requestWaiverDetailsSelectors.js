/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

const selectWaiverRequestSlice = prop('requestWaiverDetails');

export const selectWaiverRequestDetails = createSelector(selectWaiverRequestSlice, prop('waiverRequestDetails'));
export const selectWaiverRequestDetailsLoading = createSelector(selectWaiverRequestSlice, prop('loading'));
export const selectWaiverRequestDetailsError = createSelector(selectWaiverRequestSlice, prop('loadError'));

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

const selectAutoWaiverDetailsSlice = prop('autoWaiverDetails');

export const selectAutoWaiverDetails = createSelector(selectAutoWaiverDetailsSlice, prop('waiverDetails'));
export const selectAutoWaiverDetailsLoading = createSelector(selectAutoWaiverDetailsSlice, prop('loading'));
export const selectAutoWaiverDetailsError = createSelector(selectAutoWaiverDetailsSlice, prop('loadError'));

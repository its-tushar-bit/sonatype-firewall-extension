/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectAutoWaiversSlice } from '../autoWaiversSelectors';

const selectAutoWaiverExclusionsSlice = createSelector(selectAutoWaiversSlice, prop('autoWaiverDetailsExclusions'));
export const selectAutoWaiverExclusions = createSelector(selectAutoWaiverExclusionsSlice, prop('data'));
export const selectAutoWaiverExclusionsLoading = createSelector(selectAutoWaiverExclusionsSlice, prop('loading'));
export const selectAutoWaiverExclusionError = createSelector(selectAutoWaiverExclusionsSlice, prop('loadError'));

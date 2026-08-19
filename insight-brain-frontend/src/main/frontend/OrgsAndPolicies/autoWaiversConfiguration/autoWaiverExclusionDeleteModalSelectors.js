/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectAutoWaiversSlice } from '../autoWaiversSelectors';

export const selectAutoWaiverExclusionDeleteModalSlice = createSelector(
  selectAutoWaiversSlice,
  prop('autoWaiverExclusionDeleteModal')
);

export const selectAutoWaiverExclusionDeleteModalData = createSelector(
  selectAutoWaiverExclusionDeleteModalSlice,
  prop('data')
);

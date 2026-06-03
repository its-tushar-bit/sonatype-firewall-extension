/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { path, prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectAddContainerImageWaiverPage = prop('addContainerImageWaiverPage');

export const selectWaiverSlice = prop('waivers');

export const selectWaiverReasons = createSelector(selectWaiverSlice, path(['waiverReasons', 'data']));

export const selectHasWaivePermission = createSelector(selectAddContainerImageWaiverPage, prop('hasWaivePermission'));

export const selectHasCreateWaiverRequestPermission = createSelector(
  selectAddContainerImageWaiverPage,
  prop('hasCreateWaiverRequestPermission')
);

export const selectNoteToReviewer = createSelector(selectAddContainerImageWaiverPage, prop('noteToReviewer'));

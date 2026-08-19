/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectMainHeaderSlice = prop('mainHeader');

export const selectPermissions = createSelector(selectMainHeaderSlice, prop('permissions'));

export const selectShouldShowLoginButton = createSelector(selectMainHeaderSlice, prop('shouldShowLoginButton'));

export const selectLoading = createSelector(selectMainHeaderSlice, prop('loading'));

export const selectLoadError = createSelector(selectMainHeaderSlice, prop('loadError'));

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectReact2ShellSlice = prop('react2shell');

export const selectDownloadLoading = createSelector(selectReact2ShellSlice, prop('downloadLoading'));

export const selectDownloadError = createSelector(selectReact2ShellSlice, prop('error'));

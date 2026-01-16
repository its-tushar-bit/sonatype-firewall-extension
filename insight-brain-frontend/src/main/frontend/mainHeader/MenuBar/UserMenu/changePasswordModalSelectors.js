/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectChangePasswordModalSlice = prop('changePasswordModal');

export const selectChangePasswordStatus = createSelector(selectChangePasswordModalSlice, prop('status'));
export const selectChangePasswordErrorMessage = createSelector(selectChangePasswordModalSlice, prop('errorMessage'));

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';

import { selectOrgsAndPoliciesSlice } from '../orgsAndPoliciesSelectors';

export const selectOwnerModalSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  ({ ownerActions: { ownerModal } }) => ownerModal
);

export const selectNewOwnerName = createSelector(selectOwnerModalSlice, prop('ownerName'));
export const selectNewOwnerAppId = createSelector(selectOwnerModalSlice, prop('appId'));
export const selectValidationError = createSelector(selectOwnerModalSlice, ({ validationErrors, ownerName }) => {
  if (ownerName.value === '') {
    return GLOBAL_FORM_VALIDATION_ERROR;
  }
  return isNilOrEmpty(validationErrors) ? null : GLOBAL_FORM_VALIDATION_ERROR;
});

export const selectIsApplication = createSelector(selectOwnerModalSlice, prop('isApplication'));

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectConstraintSlice = createSelector(selectOrgsAndPoliciesSlice, prop('constraint'));

export const selectLoadError = createSelector(selectConstraintSlice, prop('loadError'));
export const selectIsDirty = createSelector(selectConstraintSlice, prop('isDirty'));
export const selectIsLoading = createSelector(selectConstraintSlice, prop('loading'));

export const selectEditConstraintMap = createSelector(selectConstraintSlice, prop('editConstraintMap'));
export const selectConditionTypesMap = createSelector(selectConstraintSlice, prop('conditionTypesMap'));
export const selectConditionTypes = createSelector(selectConstraintSlice, prop('conditionTypes'));

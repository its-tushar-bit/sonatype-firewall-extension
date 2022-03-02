/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop, split, contains, curryN } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectRouterSlice = prop('router');
export const selectRouterCurrentParams = createSelector(selectRouterSlice, prop('currentParams'));
export const selectRouterState = createSelector(selectRouterSlice, prop('currentState'));

export const selectCurrentRouteName = createSelector(selectRouterState, prop('name'));

export const selectRouterPrevState = createSelector(selectRouterSlice, prop('prevState'));

export const selectPreviousRouteName = createSelector(selectRouterPrevState, prop('name'));

const includesNamePart = curryN(2, (part, str) => contains(part, split('.', str)));
const nameIncludesOrganization = includesNamePart('organization');

export const selectIsOrganization = createSelector(selectCurrentRouteName, nameIncludesOrganization);

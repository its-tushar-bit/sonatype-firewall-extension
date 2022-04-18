/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

import {
  selectApplicationId,
  selectIsApplication,
  selectIsOrganization,
  selectOrganizationId,
} from '../reduxUiRouter/routerSelectors';

export const selectOrgsAndPoliciesSlice = prop('orgsAndPolicies');
export const selectRootSlice = createSelector(selectOrgsAndPoliciesSlice, prop('root'));
export const selectOwnerName = createSelector(selectRootSlice, prop('ownerName'));
export const selectOwnerProperties = createSelector(
  selectRouterCurrentParams,
  ({ applicationPublicId, organizationId }) => ({
    ownerType: applicationPublicId ? 'application' : 'organization',
    ownerId: applicationPublicId || organizationId,
  })
);

export const selectEntityId = createSelector(
  selectIsOrganization,
  selectIsApplication,
  selectOrganizationId,
  selectApplicationId,
  (isOrganization, isApplication, orgId, appId) => (isApplication ? appId : isOrganization ? orgId : 'global')
);

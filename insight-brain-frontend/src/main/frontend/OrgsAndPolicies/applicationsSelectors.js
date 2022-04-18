/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

import { selectEntityId, selectOrgsAndPoliciesSlice, selectOwnerName } from './orgsAndPoliciesSelectors';

export const selectApplicationsSlice = createSelector(selectOrgsAndPoliciesSlice, prop('applications'));

export const selectLoadingApplications = createSelector(selectApplicationsSlice, prop('loadingApplications'));
export const selectLoadApplicationsError = createSelector(selectApplicationsSlice, prop('loadApplicationsError'));
export const selectApplications = createSelector(selectApplicationsSlice, prop('applications'));

export const selectLoadEmptyError = createSelector(
  selectLoadingApplications,
  selectLoadApplicationsError,
  selectOwnerName,
  selectEntityId,
  (loading, loadError, ownerName, entityId) =>
    !loading && !ownerName ? loadError || `Could not find an application with ID ${entityId}.` : null
);

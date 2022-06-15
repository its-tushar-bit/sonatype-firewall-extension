/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

import { selectOrgsAndPoliciesSlice } from './orgsAndPoliciesSelectors';

export const selectOrganizationsSlice = createSelector(selectOrgsAndPoliciesSlice, prop('organizations'));

export const selectOrganizations = createSelector(selectOrganizationsSlice, prop('organizations'));
export const selectLoadingOrganizations = createSelector(selectOrganizationsSlice, prop('loading'));
export const selectLoadErrorOrganizations = createSelector(selectOrganizationsSlice, prop('loadError'));

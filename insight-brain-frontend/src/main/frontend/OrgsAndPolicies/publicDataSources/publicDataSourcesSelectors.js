/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { prop } from 'ramda';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';

export const selectPublicDataSourcesSlice = createSelector(selectOrgsAndPoliciesSlice, prop('publicDataSources'));
export const selectCpeConfiguration = createSelector(selectPublicDataSourcesSlice, prop('data'));
export const selectPublicDatasourcesLinkParams = createSelector(selectRouterSlice, (router) =>
  deriveEditRoute(router, 'public-data-sources-editor')
);
export const selectLoading = createSelector(selectPublicDataSourcesSlice, prop('loading'));
export const selectLoadError = createSelector(selectPublicDataSourcesSlice, prop('loadError'));

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { CHART_VISIBILITY_REDUCER_NAME } from 'MainRoot/development/developmentDashboard/slices/chartVisibilitySlice';

const integrationsSlice = prop('integrations');
export const selectUsageOverTimeChartVisibilitySlice = createSelector(
  integrationsSlice,
  prop(CHART_VISIBILITY_REDUCER_NAME)
);

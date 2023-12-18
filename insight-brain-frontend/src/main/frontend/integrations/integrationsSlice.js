/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { combineReducers } from 'redux';
import ideIntegrationsSlice from './sections/overview/ideIntegrationsCard/ideIntegrationsSlice';
import appIntegrationsAndRiskSlice from './sections/AppIntegrationsAndRiskTable/appIntegrationsAndRiskSlice';
import chartVisibilitySlice, { CHART_VISIBILITY_REDUCER_NAME } from 'MainRoot/integrations/slices/chartVisibilitySlice';
import developerDashboardGraphsSlice from './slices/developerDashboardGraphsSlice';

export const APP_INTEGRATIONS_AND_RISK = 'appIntegrationsAndRisk';
export const DEVELOPER_GRAPHS = 'developerDashboardGraphs';

export default combineReducers({
  ideIntegrations: ideIntegrationsSlice,
  [APP_INTEGRATIONS_AND_RISK]: appIntegrationsAndRiskSlice,
  [CHART_VISIBILITY_REDUCER_NAME]: chartVisibilitySlice,
  [DEVELOPER_GRAPHS]: developerDashboardGraphsSlice,
});

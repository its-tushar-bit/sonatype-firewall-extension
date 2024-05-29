/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import ideIntegrationsSlice, { IDE_INTEGRATIONS_REDUCER_NAME } from './ideIntegrationsSlice';
import appIntegrationsAndRiskSlice, { APP_INTEGRATIONS_AND_RISK_REDUCER_NAME } from './appIntegrationsAndRiskSlice';
import developerDashboardGraphsSlice, {
  DEVELOPER_DASHBOARD_GRAPHS_REDUCER_NAME,
} from './developerDashboardGraphsSlice';

export default combineReducers({
  [IDE_INTEGRATIONS_REDUCER_NAME]: ideIntegrationsSlice,
  [APP_INTEGRATIONS_AND_RISK_REDUCER_NAME]: appIntegrationsAndRiskSlice,
  [DEVELOPER_DASHBOARD_GRAPHS_REDUCER_NAME]: developerDashboardGraphsSlice,
});

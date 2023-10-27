/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import ideIntegrationsSlice from './sections/overview/ideIntegrationsCard/ideIntegrationsSlice';
import appIntegrationsAndRiskSlice from './sections/AppIntegrationsAndRiskTable/appIntegrationsAndRiskSlice';

export const INTEGRATIONS = 'integrations';
export const APP_INTEGRATIONS_AND_RISK = 'appIntegrationsAndRisk';

export default combineReducers({
  ideIntegrations: ideIntegrationsSlice,
  [APP_INTEGRATIONS_AND_RISK]: appIntegrationsAndRiskSlice,
});

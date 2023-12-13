/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import ideIntegrationsSlice from './sections/overview/ideIntegrationsCard/ideIntegrationsSlice';
import appIntegrationsAndRiskSlice from './sections/AppIntegrationsAndRiskTable/appIntegrationsAndRiskSlice';
import adoptionGraphSlice from './slices/adoptionGraphSlice';
import chartVisibilitySlice, { CHART_VISIBILITY_REDUCER_NAME } from 'MainRoot/integrations/slices/chartVisibilitySlice';
import riskRemediationAndMttrGraphSlice from './slices/riskRemediationAndMttrGraphSlice';

export const APP_INTEGRATIONS_AND_RISK = 'appIntegrationsAndRisk';
export const ADOPTION_GRAPH = 'adoptionGraph';
export const RISK_REMEDIATION_AND_MTTR_GRAPH = 'riskRemediationAndMttrGraph';

export default combineReducers({
  ideIntegrations: ideIntegrationsSlice,
  [APP_INTEGRATIONS_AND_RISK]: appIntegrationsAndRiskSlice,
  [ADOPTION_GRAPH]: adoptionGraphSlice,
  [RISK_REMEDIATION_AND_MTTR_GRAPH]: riskRemediationAndMttrGraphSlice,
  [CHART_VISIBILITY_REDUCER_NAME]: chartVisibilitySlice,
});

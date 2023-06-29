/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import ciUsageRequestSlice from './sections/overview/ciUsageSlice';
import ideIntegrationsSlice from './sections/overview/ideIntegrationsCard/ideIntegrationsSlice';
import appsWithoutRecentCiUsagePreviewSlice from 'MainRoot/integrations/slices/appsWithoutRecentCiUsagePreviewSlice';
import appsWithoutCiIntegrationsSlice from './sections/AppsWithoutCiIntegrations/appsWithoutCiIntegrationsSlice';

export const INTEGRATIONS = 'integrations';
export const CI_USAGE = 'ciUsage';
export const CI_APPS_WITHOUT_RECENT_CI_USAGE_PREVIEW = 'appsWithoutRecentCiUsagePreview';

export default combineReducers({
  [CI_USAGE]: ciUsageRequestSlice,
  ideIntegrations: ideIntegrationsSlice,
  [CI_APPS_WITHOUT_RECENT_CI_USAGE_PREVIEW]: appsWithoutRecentCiUsagePreviewSlice,
  appsWithoutCiIntegrations: appsWithoutCiIntegrationsSlice,
});

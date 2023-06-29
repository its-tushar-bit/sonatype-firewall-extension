/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop } from 'ramda';
import {
  CI_APPS_WITHOUT_RECENT_CI_USAGE_PREVIEW,
  CI_USAGE,
  INTEGRATIONS,
} from 'MainRoot/integrations/integrationsSlice';
import { createSelector } from '@reduxjs/toolkit';

const selectIntegrationsSlice = prop(INTEGRATIONS);

export const selectCiUsageSlice = createSelector(selectIntegrationsSlice, prop(CI_USAGE));

export const selectappsWithoutRecentCiUsagePreviewSlice = createSelector(
  selectIntegrationsSlice,
  prop(CI_APPS_WITHOUT_RECENT_CI_USAGE_PREVIEW)
);

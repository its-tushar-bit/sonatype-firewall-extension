/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop } from 'ramda';
import { CI_USAGE, INTEGRATIONS } from 'MainRoot/integrations/integrationsSlice';
import { createSelector } from '@reduxjs/toolkit';

const selectCiUsageRequestSlice = prop(INTEGRATIONS);

export const selectCiUsageSlice = createSelector(selectCiUsageRequestSlice, prop(CI_USAGE));

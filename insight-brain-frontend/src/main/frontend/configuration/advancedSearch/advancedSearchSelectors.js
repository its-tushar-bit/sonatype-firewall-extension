/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { path, prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectAdvancedSearchConfigSlice = prop('advancedSearchConfig');

export const selectIsAdvancedSearchEnabled = createSelector(selectAdvancedSearchConfigSlice, (state) =>
  path(['serverData', 'isEnabled'], state)
);

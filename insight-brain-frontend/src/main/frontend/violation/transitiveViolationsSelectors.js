/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectPreviousRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectTransitiveViolations = prop('transitiveViolations');

export const selectShouldGoBackToComponentDetails = createSelector(
  selectPreviousRouteName,
  (prevState) => 'applicationReport.policy' !== prevState
);

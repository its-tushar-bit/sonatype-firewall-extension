/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectLatestReportForStage = prop('latestReportForStage');

export const selectLatestReportForStageId = (state) => selectLatestReportForStage(state)?.latestReportForStage?.id;

export const selectIsLatestReportForStageRequestPending = createSelector(selectLatestReportForStage, (state) => {
  return state.loading || state.uninitialized;
});

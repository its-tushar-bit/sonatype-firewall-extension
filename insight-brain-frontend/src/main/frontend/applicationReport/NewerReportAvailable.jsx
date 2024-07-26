/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { NxTextLink, NxWarningAlert } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectLatestReportForStageId } from 'MainRoot/applicationReport/latestReportForStageSelectors';
import { selectReportStageId } from 'MainRoot/applicationReport/applicationReportSelectors';
import { createSelector } from '@reduxjs/toolkit';
import { sendGainsightCustomEvent } from 'MainRoot/util/gainsightUtils';

const selectShouldShowNewReportMessage = createSelector(
  [selectRouterCurrentParams, selectLatestReportForStageId, selectReportStageId],
  ({ scanId }, newScanId, stageId) => {
    const sameAsCurrentScan = newScanId === scanId;
    return !sameAsCurrentScan && stageId !== 'develop';
  }
);

export function NewerReportAvailable() {
  const EXPIRED_APP_REPORT_BANNER_SHOWN = 'EXPIRED_APP_REPORT_BANNER_SHOWN';
  const uiRouterState = useRouterState();
  const { publicId } = useSelector(selectRouterCurrentParams);
  const newScanId = useSelector(selectLatestReportForStageId);
  const shouldShowNewReportMessage = useSelector(selectShouldShowNewReportMessage);

  useEffect(() => {
    if (shouldShowNewReportMessage) {
      sendGainsightCustomEvent(EXPIRED_APP_REPORT_BANNER_SHOWN);
    }
  }, [shouldShowNewReportMessage]);

  if (!shouldShowNewReportMessage) {
    return null;
  }

  return (
    <NxWarningAlert data-testid="new-report-available-warning">
      <p>
        A new version of this report is available.{' '}
        <NxTextLink
          href={uiRouterState.href('applicationReport.policy', {
            publicId,
            scanId: newScanId,
          })}
        >
          Click here
        </NxTextLink>{' '}
        to navigate to the latest report.
      </p>
    </NxWarningAlert>
  );
}

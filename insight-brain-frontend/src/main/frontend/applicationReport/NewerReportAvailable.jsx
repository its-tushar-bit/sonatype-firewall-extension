/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { selectRouterCurrentParams, selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
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
  const FIREWALL_REPORT_ROUTE = 'firewall.containerReport';
  const uiRouterState = useRouterState();
  const { publicId, hrcId, componentDisplayName } = useSelector(selectRouterCurrentParams);
  const newScanId = useSelector(selectLatestReportForStageId);
  const currentRouteName = useSelector(selectCurrentRouteName);
  const shouldShowNewReportMessage = useSelector(selectShouldShowNewReportMessage);

  // Route the "Click here" link to the right report page based on the current context:
  // - HRC report → HRC report policy tab
  // - Firewall container report → firewall container report
  // - Otherwise → application report policy tab
  const isFirewallContainerReport = currentRouteName === FIREWALL_REPORT_ROUTE;
  const isHrcReport = !!hrcId;
  const targetState = isHrcReport
    ? 'hostedRepositoryComponentReport.policy'
    : isFirewallContainerReport
    ? FIREWALL_REPORT_ROUTE
    : 'applicationReport.policy';
  // Forward componentDisplayName on HRC so the friendly page title survives the navigation
  // to the newer scan; without it the page falls back to rendering the raw HRC UUID until
  // metadata resolves (CLM-42090).
  const targetParams = isHrcReport
    ? { hrcId, scanId: newScanId, componentDisplayName }
    : { publicId, scanId: newScanId };

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
        <NxTextLink href={uiRouterState.href(targetState, targetParams)}>Click here</NxTextLink> to navigate to the
        latest report.
      </p>
    </NxWarningAlert>
  );
}

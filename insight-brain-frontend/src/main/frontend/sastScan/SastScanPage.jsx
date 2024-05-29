/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { selectIsDeveloperDashboardEnabled } from '../productFeatures/productFeaturesSelectors';
import SastScanTitle from 'MainRoot/sastScan/SastScanTitle';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import { NxLoadingSpinner, NxPageMain } from '@sonatype/react-shared-components';
import SastScanContent from 'MainRoot/sastScan/SastScanContent';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import useLoadSastScan from 'MainRoot/sastScan/sastHooks';
import { formatDate, STANDARD_DATE_TIME_FORMAT } from 'MainRoot/util/dateUtils';

export default function SastScanPage() {
  const routerCurrentParams = useSelector(selectRouterCurrentParams);
  const { applicationPublicId, sastScanId } = routerCurrentParams;
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const hookValue = useLoadSastScan({ applicationPublicId, sastScanId });

  const { loading, result: sastScan } = hookValue;

  if (loading || !sastScan) {
    return <NxLoadingSpinner />;
  }

  //TODO: need to add a loading screen while the feature flag api is called
  if (!isDeveloperDashboardEnabled) {
    return <LicenseLockScreen />;
  }

  return (
    <NxPageMain>
      {/*TODO: Temporarily pointing to the Development page. This needs to be adjusted to the correct all sast scans page.*/}
      <MenuBarBackButton
        data-analytics-id="sonatype-developer-sast-back-button"
        text="Back to Developer Dashboard"
        stateName={'integrations'}
      />
      <SastScanTitle title={getTitle()} description={getDescription()} />
      <SastScanContent sastScan={sastScan} />
    </NxPageMain>
  );

  function getTitle() {
    return applicationPublicId + ' SAST Scan';
  }

  function getDescription() {
    return (
      <>
        {getFormattedDate()} {getFormattedCommitHash()} {getFormattedBranchName()}
      </>
    );
  }

  function getFormattedDate() {
    return (
      <>
        <strong>Triggered on:</strong> {formatDate(sastScan.createdAt, STANDARD_DATE_TIME_FORMAT)}
      </>
    );
  }

  function getFormattedCommitHash() {
    const commitHash = sastScan?.sastScmScanContext?.commitHash;

    return !!commitHash ? (
      <>
        <b>Commit:</b> {commitHash}
      </>
    ) : (
      ''
    );
  }

  function getFormattedBranchName() {
    const branchName = sastScan?.sastScmScanContext?.branchName;
    // Using <div> will break the line.
    return !!branchName ? (
      <div>
        <b>Branch:</b> {branchName}
      </div>
    ) : (
      ''
    );
  }
}

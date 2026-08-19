/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useState } from 'react';
import {
  NxH1,
  NxInfoAlert,
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsSbomManagerOnlyLicense } from 'MainRoot/productFeatures/productLicenseSelectors';
import TotalSbomsStoredTile from './totalSbomsStoredTile/TotalSbomsStoredTile';
import ApplicationsHistoryTile from './applicationsHistoryTile/ApplicationsHistoryTile';
import HighPriorityVulnerabilitiesTile from './highPriorityVulnerabilitiesTile/HighPriorityVulnerabilitiesTile';
import VulnerabilitiesByThreatLevelTile from './vulnerabilitiesByThreatLevelTile/VulnerabilitiesByThreatLevelTile';
import RecentlyImportedSbomsTile from './recentlyImportedSbomsTile/RecentlyImportedSbomsTile';
import SbomReleaseStatusTile from './sbomReleaseStatusTile/SbomReleaseStatusTile';
import { actions } from './sbomCountsSlice';
import { selectSbomCounts } from 'MainRoot/sbomManager/features/dashboard/sbomManagerDashboardSelectors';
import { selectIsCpeMatchingSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

import './SbomManagerDashboard.scss';

const ALERT_DISMISSED_KEY = 'sbomManagerDashboardInfoAlertDismissed';

export default function SbomManagerDashboard() {
  const dispatch = useDispatch();
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const sbomCounts = useSelector(selectSbomCounts);
  const isCpeMatchingSupported = useSelector(selectIsCpeMatchingSupported);
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);

  const [showInfoAlert, setShowInfoAlert] = useState(() => {
    return localStorage.getItem(ALERT_DISMISSED_KEY) !== 'true';
  });

  const handleDismiss = () => {
    setShowInfoAlert(false);
    localStorage.setItem(ALERT_DISMISSED_KEY, 'true');
  };

  const load = () => {
    dispatch(actions.load());
  };

  useEffect(() => {
    load();
  }, []);

  function retryHandler() {}

  return (
    <NxPageMain id="sbom-manager-dashboard">
      <NxLoadWrapper
        retryHandler={retryHandler}
        loading={isProductFeaturesLoading}
        error={errorLoadingProductFeatures || noSbomManagerEnabledError}
      >
        {showInfoAlert && isCpeMatchingSupported && !isSbomManagerOnlyLicense && (
          <NxInfoAlert onClose={handleDismiss}>
            SBOM Manager <strong>now supports C/C++.</strong> See the{' '}
            <NxTextLink
              href={'https://links.sonatype.com/products/insight/public-data-sources'}
              target="_blank"
              rel="noopener noreferrer"
              noReferrer
              newTab
            >
              Public Data Sources documentation
            </NxTextLink>{' '}
            for more details.
          </NxInfoAlert>
        )}
        <NxPageTitle>
          <NxH1>SBOM Manager Dashboard</NxH1>
        </NxPageTitle>
        <div className="sbom-manager-dashboard-tiles">
          <TotalSbomsStoredTile {...sbomCounts} load={load} />
          <ApplicationsHistoryTile />
          <HighPriorityVulnerabilitiesTile />
          <VulnerabilitiesByThreatLevelTile />
          <SbomReleaseStatusTile {...sbomCounts} load={load} />
          <RecentlyImportedSbomsTile />
        </div>
      </NxLoadWrapper>
    </NxPageMain>
  );
}

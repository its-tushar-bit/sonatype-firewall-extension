/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH1, NxLoadWrapper, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import TotalSbomsStoredTile from './totalSbomsStoredTile/TotalSbomsStoredTile';
// import ApplicationsHistoryTile from './applicationsHistoryTile/ApplicationsHistoryTile';
// import VulnerabilitiesByThreatLevelTile from './vulnerabilitiesByThreatLevelTile/VulnerabilitiesByThreatLevelTile';
// import HighPriorityVulnerabilitiesTile from './highPriorityVulnerabilitiesTile/HighPriorityVulnerabilitiesTile';
// import SbomReleaseStatusTile from './sbomReleaseStatusTile/SbomReleaseStatusTile';
// import RecentlyImportedSbomsTile from './recentlyImportedSbomsTile/RecentlyImportedSbomsTile';

import './SbomManagerDashboard.scss';

export default function SbomManagerDashboard() {
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);

  function retryHandler() {}

  return (
    <NxPageMain id="sbom-manager-dashboard">
      <NxLoadWrapper
        retryHandler={retryHandler}
        loading={isProductFeaturesLoading}
        error={errorLoadingProductFeatures || noSbomManagerEnabledError}
      >
        <>
          <NxPageTitle>
            <NxH1>SBOM Manager Dashboard</NxH1>
          </NxPageTitle>
          <div className="sbom-manager-dashboard-tiles">
            <TotalSbomsStoredTile />
            {/*
              CLM-30199
              Incomplete dashboard tiles are hidden
              until they are implemented.

              <ApplicationsHistoryTile />
              <VulnerabilitiesByThreatLevelTile />
              <HighPriorityVulnerabilitiesTile />
              <SbomReleaseStatusTile />
              <RecentlyImportedSbomsTile />
            */}
          </div>
        </>
      </NxLoadWrapper>
    </NxPageMain>
  );
}

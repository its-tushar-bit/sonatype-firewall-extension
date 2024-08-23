/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { NxH1, NxLoadWrapper, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import TotalSbomsStoredTile from './totalSbomsStoredTile/TotalSbomsStoredTile';
import ApplicationsHistoryTile from './applicationsHistoryTile/ApplicationsHistoryTile';
import HighPriorityVulnerabilitiesTile from './highPriorityVulnerabilitiesTile/HighPriorityVulnerabilitiesTile';
import VulnerabilitiesByThreatLevelTile from './vulnerabilitiesByThreatLevelTile/VulnerabilitiesByThreatLevelTile';
import RecentlyImportedSbomsTile from './recentlyImportedSbomsTile/RecentlyImportedSbomsTile';
import SbomReleaseStatusTile from './sbomReleaseStatusTile/SbomReleaseStatusTile';
import { actions } from './sbomCountsSlice';
import { selectSbomCounts } from 'MainRoot/sbomManager/features/dashboard/sbomManagerDashboardSelectors';

import './SbomManagerDashboard.scss';

export default function SbomManagerDashboard() {
  const dispatch = useDispatch();
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);
  const sbomCounts = useSelector(selectSbomCounts);

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

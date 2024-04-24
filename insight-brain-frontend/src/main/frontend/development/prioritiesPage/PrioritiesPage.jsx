/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxLoadingSpinner, NxLoadWrapper, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import PrioritiesPageHeader from 'MainRoot/development/prioritiesPage/PrioritiesPageHeader';
import PrioritiesPageTable from 'MainRoot/development/prioritiesPage/PrioritiesPageTable';
import {
  selectLoadingFeatures,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { setReportParameters, loadReportIfNeeded } from 'MainRoot/applicationReport/applicationReportActions';
import { selectApplicationReportSlice } from 'MainRoot/applicationReport/applicationReportSelectors';
import { pick } from 'ramda';

export default function PrioritiesPage() {
  return (
    <NxPageMain className="iq-priorities-page">
      <PageContents />
    </NxPageMain>
  );
}

function PageContents() {
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const productFeaturesLoading = useSelector(selectLoadingFeatures);

  if (productFeaturesLoading) {
    return <NxLoadingSpinner />;
  } else if (isDeveloperDashboardEnabled) {
    return <PrioritiesPageContents />;
  } else {
    return <LicenseLockScreen />;
  }
}

function PrioritiesPageContents() {
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const applicationReport = useSelector(selectApplicationReportSlice);
  const loading =
    !applicationReport.loadError && (!!applicationReport.pendingLoads.size || !applicationReport.metadata);
  const { loadError } = pick(['loadError'], applicationReport);

  const dispatch = useDispatch();

  const doLoad = () => {
    dispatch(setReportParameters(publicAppId, scanId));
    dispatch(loadReportIfNeeded());
  };

  useEffect(() => {
    if (publicAppId && scanId) {
      doLoad();
    }
  }, [publicAppId, scanId]);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
      <NxPageTitle>
        <PrioritiesPageHeader />
      </NxPageTitle>
      <PrioritiesPageTable />
    </NxLoadWrapper>
  );
}

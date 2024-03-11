/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxLoadingSpinner, NxLoadWrapper, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import DevelopmentReportHeader from 'MainRoot/development/developmentReport/DevelopmentReportHeader';
import {
  selectLoadingFeatures,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { setReportParameters, loadReportIfNeeded } from 'MainRoot/applicationReport/applicationReportActions';
import { selectApplicationReportSlice } from 'MainRoot/applicationReport/applicationReportSelectors';
import { pick } from 'ramda';

export default function DevelopmentReport() {
  return (
    <NxPageMain className="iq-development-report">
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
    return <DevelopmentReportContents />;
  } else {
    return <LicenseLockScreen />;
  }
}

function DevelopmentReportContents() {
  const { appId, scanId } = useSelector(selectRouterCurrentParams);
  const applicationReport = useSelector(selectApplicationReportSlice);
  const loading =
    !applicationReport.loadError && (!!applicationReport.pendingLoads.size || !applicationReport.metadata);
  const { loadError } = pick(['loadError'], applicationReport);

  const dispatch = useDispatch();

  const doLoad = () => {
    dispatch(setReportParameters(appId, scanId));
    dispatch(loadReportIfNeeded());
  };

  useEffect(() => {
    doLoad();
  }, [appId, scanId]);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
      <NxPageTitle>
        <DevelopmentReportHeader />
      </NxPageTitle>
    </NxLoadWrapper>
  );
}

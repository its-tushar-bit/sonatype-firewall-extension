/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxH1, NxLoadWrapper, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { setReportParameters, loadReportIfNeeded } from 'MainRoot/applicationReport/applicationReportActions';
import { selectApplicationReportSlice } from 'MainRoot/applicationReport/applicationReportSelectors';
import { pick } from 'ramda';

export default function DevelopmentReport() {
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
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

  if (!isDeveloperDashboardEnabled) {
    return <LicenseLockScreen />;
  }

  return (
    <NxPageMain>
      <NxPageTitle>
        <NxH1>Sonatype Development Report</NxH1>
      </NxPageTitle>
      <div>
        <strong>appId:</strong> {appId} <strong>scanId:</strong> {scanId}
      </div>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <div>Data Load Complete</div>
      </NxLoadWrapper>
    </NxPageMain>
  );
}

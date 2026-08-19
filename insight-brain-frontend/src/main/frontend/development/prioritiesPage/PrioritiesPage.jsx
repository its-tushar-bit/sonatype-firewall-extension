/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxLoadingSpinner, NxLoadWrapper, NxPageMain } from '@sonatype/react-shared-components';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import PrioritiesPageHeader from 'MainRoot/development/prioritiesPage/PrioritiesPageHeader';
import PrioritiesPageTable from 'MainRoot/development/prioritiesPage/PrioritiesPageTable';
import {
  selectLoadingFeatures,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectApplicationReportMetaData,
  selectIsLoading,
  selectLoadError,
  selectReportParameters,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { setReportParameters, loadReportIfNeeded } from 'MainRoot/applicationReport/applicationReportActions';

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
  const dispatch = useDispatch();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const metadata = useSelector(selectApplicationReportMetaData);
  const isLoading = useSelector(selectIsLoading);
  const loadError = useSelector(selectLoadError);
  const reportParameters = useSelector(selectReportParameters);
  const reportParametersExist = reportParameters?.appId === publicAppId && reportParameters?.scanId === scanId;

  const doLoad = () => {
    dispatch(setReportParameters(publicAppId, scanId));
    dispatch(loadReportIfNeeded());
  };

  useEffect(() => {
    if (!reportParametersExist) {
      doLoad();
    }

    return () => dispatch(actions.resetState());
  }, []);

  return (
    <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={doLoad}>
      {metadata && reportParametersExist && (
        <>
          <PrioritiesPageHeader />
          <PrioritiesPageTable />
        </>
      )}
    </NxLoadWrapper>
  );
}

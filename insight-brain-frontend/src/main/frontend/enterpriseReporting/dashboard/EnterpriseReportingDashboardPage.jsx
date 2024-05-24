/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxLoadWrapper, NxPageMain } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';

import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import {
  selectError,
  selectLoading,
} from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';
import {
  selectDataInsightsLicenseError,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import useLookerDashboard from 'MainRoot/react/useLookerDashboard';

export default function EnterpriseReportingDashboardPage() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const loadingFeatures = useSelector(selectLoadingFeatures);
  const apiError = useSelector(selectError);
  const licenseError = useSelector(selectDataInsightsLicenseError);
  const load = () => dispatch(actions.load());

  const { loadingDashboard, iframeError } = useLookerDashboard();

  const isLoading = loading || loadingFeatures || loadingDashboard;

  useEffect(() => {
    load();
  }, []);

  const combinedError = licenseError || apiError || iframeError;

  return (
    <NxPageMain id="enterprise-reporting-dashboard-page" className="nx-viewport-sized">
      <NxLoadWrapper
        loading={isLoading}
        retryHandler={() => dispatch(stateGo('enterpriseReporting'))}
        error={combinedError}
      ></NxLoadWrapper>
      {/* The dashboard container should be outside of the load wrapper or the looker sdk wont be able to embed the iframe */}
      {!iframeError && (
        <div
          className="enterprise-reporting-dashboard-container"
          id="dashboard"
          role="enterprise-reporting-dashboard"
        />
      )}
    </NxPageMain>
  );
}

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
  const isLoading = loading || loadingFeatures;
  const apiError = useSelector(selectError);
  const licenseError = useSelector(selectDataInsightsLicenseError);
  const load = () => dispatch(actions.load());

  const { iframeError } = useLookerDashboard();

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
      >
        <div
          className="enterprise-reporting-dashboard-container"
          id="dashboard"
          role="enterprise-reporting-dashboard"
        />
      </NxLoadWrapper>
    </NxPageMain>
  );
}

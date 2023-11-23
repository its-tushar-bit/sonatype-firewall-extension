/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { NxLoadWrapper, NxPageMain } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import { LookerEmbedSDK } from '@looker/embed-sdk';
import {
  selectEmbedUrlData,
  selectError,
  selectLoading,
} from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';

export default function EnterpriseReportingDashboardPage() {
  const dispatch = useDispatch();
  const embedUrlData = useSelector(selectEmbedUrlData);
  const loading = useSelector(selectLoading);
  const apiError = useSelector(selectError);
  const [iframeError, setIframeError] = useState(false);
  const load = () => dispatch(actions.load());

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    const embedDashboard = async () => {
      try {
        const ssoEmbedUrl = embedUrlData.url;
        if (ssoEmbedUrl && typeof ssoEmbedUrl === 'string') {
          await LookerEmbedSDK.createDashboardWithUrl(ssoEmbedUrl).appendTo('#dashboard').build().connect();
        }
      } catch (error) {
        setIframeError(true);
      }
    };

    if (embedUrlData) {
      LookerEmbedSDK.init(embedUrlData.baseUrl);
      embedDashboard();
    }
  }, [embedUrlData]);

  const combinedError = apiError || iframeError;

  return (
    <NxPageMain id="enterprise-reporting-dashboard-page" className="nx-viewport-sized">
      <NxLoadWrapper loading={loading} retryHandler={load} error={combinedError}>
        <div
          className="enterprise-reporting-dashboard-container"
          id="dashboard"
          role="enterprise-reporting-dashboard"
        />
      </NxLoadWrapper>
    </NxPageMain>
  );
}

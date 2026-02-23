/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import classNames from 'classnames';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
  NxH1,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faArrowLeft } from '@fortawesome/free-solid-svg-icons';

import EnterpriseReportingSupportInfo from 'MainRoot/enterpriseReporting/supportInfo/EnterpiseReportingSupportInfo';
import { actions } from '../firewallEnterpriseReportingSlice';
import {
  selectLoading,
  selectLoadError,
  selectBaseUrl,
  selectSelectedDashboard,
  selectSelectedDashboardName,
} from '../firewallEnterpriseReportingSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import useLookerDashboard from 'MainRoot/react/useLookerDashboard';

export default function FirewallEnterpriseReportingDashboardPage() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const { id } = useSelector(selectRouterCurrentParams);
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const baseUrl = useSelector(selectBaseUrl);
  const selectedDashboard = useSelector(selectSelectedDashboard);
  const selectedDashboardName = useSelector(selectSelectedDashboardName);
  const dashboards = useSelector((state) => state.firewallEnterpriseReporting.dashboards);

  // Pass Firewall-specific selectors to useLookerDashboard hook
  const { loadingDashboard, iframeError } = useLookerDashboard('#dashboard', {
    selectBaseUrl,
    selectSelectedDashboard,
  });

  const isLoading = loading || loadingDashboard;
  const combinedError = loadError || iframeError;

  const iframeClassNames = classNames('fw-enterprise-reporting-dashboard__container', {
    loading: loadingDashboard,
  });

  // Back to landing page link
  const backToLandingHref = uiRouterState.href('firewall.enterpriseReporting');

  // Load dashboard data on mount
  useEffect(() => {
    dispatch(actions.loadDashboardDetail());
  }, [dispatch]);

  useEffect(() => {
    if (id && dashboards && dashboards.length > 0) {
      dispatch(actions.updateSelectedDashboard(id));
    }
  }, [dispatch, id, dashboards]);

  const iframeContainerHtml = (
    <>
      {(isLoading || combinedError) && (
        <NxLoadWrapper
          loading={isLoading}
          retryHandler={() => dispatch(actions.loadDashboardDetail())}
          error={combinedError}
        />
      )}
      {!iframeError && !combinedError && (
        <div className={iframeClassNames} id="dashboard" role="firewall-enterprise-reporting-dashboard" />
      )}
    </>
  );

  return (
    <NxPageMain id="fw-enterprise-reporting-dashboard-page" className="nx-viewport-sized">
      <NxTextLink href={backToLandingHref} className="fw-enterprise-reporting-dashboard__back-link">
        <NxFontAwesomeIcon icon={faArrowLeft} />
        <span>Back to Enterprise Reporting</span>
      </NxTextLink>

      <NxPageTitle>
        <NxH1>{selectedDashboardName || 'Loading Dashboard...'}</NxH1>
      </NxPageTitle>

      {!baseUrl || !selectedDashboard ? (
        <NxLoadWrapper loading={loading} retryHandler={() => dispatch(actions.loadDashboardDetail())} error={loadError}>
          <div>Preparing dashboard...</div>
        </NxLoadWrapper>
      ) : (
        iframeContainerHtml
      )}

      <EnterpriseReportingSupportInfo />
    </NxPageMain>
  );
}

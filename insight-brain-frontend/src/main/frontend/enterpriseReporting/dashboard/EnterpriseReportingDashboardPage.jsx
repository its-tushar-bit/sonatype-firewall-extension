/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper, NxPageMain, NxTextLink, NxH3, NxTooltip } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { filter, propEq } from 'ramda';

import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import { selectEnterpriseReportingDashboard } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';
import { selectRouterCurrentParams, selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectEnterpriseReportingLicenseError,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import useLookerDashboard from 'MainRoot/react/useLookerDashboard';

export default function EnterpriseReportingDashboardPage() {
  const dispatch = useDispatch();
  const loadingFeatures = useSelector(selectLoadingFeatures);
  const licenseError = useSelector(selectEnterpriseReportingLicenseError);
  const { loading, loadError, dashboardsData } = useSelector(selectEnterpriseReportingDashboard);
  const { id } = useSelector(selectRouterCurrentParams); //need to pull the dashboard's id from the URL to query Looker
  const routerState = useSelector(selectRouterState);
  const load = () => dispatch(actions.load());
  const setSelectedDashboard = (value) => dispatch(actions.setSelectedDashboard(value));

  const { loadingDashboard, iframeError } = useLookerDashboard();

  const isLoading = loading || loadingFeatures || loadingDashboard;

  const enterpriseDashboards = dashboardsData?.filter((dashboard) => dashboard.category === 'enterprise');
  const dataInsightsDashboards = dashboardsData?.filter((dashboard) => dashboard.category === 'dataInsight');

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (dashboardsData) {
      const dashboardFromUrl = filter(propEq('dashboardId', id), dashboardsData)[0];
      if (dashboardFromUrl) {
        setSelectedDashboard(dashboardFromUrl);
      } else if (routerState.name === 'enterpriseReportingDashboard') {
        dispatch(stateGo('enterpriseReporting'));
      }
    }
  }, [id, dashboardsData]);

  const combinedError = licenseError || loadError || iframeError;
  return (
    <NxPageMain id="enterprise-reporting-dashboard-page" className="nx-viewport-sized">
      <nav className="enterprise-reporting-dashboard__navigation-bar">
        <NavigationBarRow activeDashboard={id} dashboards={enterpriseDashboards} title="Enterprise Dashboards" />
        <NavigationBarRow activeDashboard={id} dashboards={dataInsightsDashboards} title="Data Insights" />
      </nav>
      <NxLoadWrapper
        loading={isLoading}
        retryHandler={() => dispatch(stateGo('enterpriseReporting'))}
        error={combinedError}
      ></NxLoadWrapper>
      {/* The dashboard container should be outside of the load wrapper or the looker sdk wont be able to embed the iframe */}
      {!iframeError && (
        <div
          className="enterprise-reporting-dashboard__container"
          id="dashboard"
          role="enterprise-reporting-dashboard"
        />
      )}
    </NxPageMain>
  );
}

function NavigationBarRow({ dashboards, title, activeDashboard }) {
  const DASHBOARD_SELECTOR = '.enterprise-reporting-dashboard__container iframe';
  const clmVersion = window.clmServerVersion.split('.')[1];

  const uiRouterState = useRouterState();
  const cleanTitle = (title) => {
    const cleanedTitle = title.includes('Dashboard:') ? title.split(':')[1] : title;
    return cleanedTitle;
  };
  const disableLink = (dashboard) =>
    !!dashboard.sinceIQVersion && !!clmVersion && parseInt(clmVersion) < parseInt(dashboard.sinceIQVersion);

  // This is to override a bug from MUI Tooltips causing iframes to resize on tooltip render:
  // https://github.com/mui/material-ui/issues/23266
  // It is resolved by setting a fixed width on the iframe. This is called to override the iframe's width of 100% when
  // a tooltip renders, on initial load of the page, and after a window resizing event.
  const handleIframeSizing = () => {
    const dashboard = document.querySelector(DASHBOARD_SELECTOR);
    if (dashboard) {
      const dashboardSize = dashboard.getBoundingClientRect();
      dashboard.style.width = `${dashboardSize.width}px`;
    }
  };

  useEffect(() => {
    // find the iframe once it renders on page load
    const findIframe = setInterval(() => {
      const iframe = document.querySelector(DASHBOARD_SELECTOR);
      if (iframe) {
        handleIframeSizing();
        clearInterval(findIframe);
        return iframe;
      }
    }, 1000);

    // set the iframe's width back to 100% to allow it to resize with its parent container, then set fixed width
    const handleWindowResize = () => {
      const dashboard = document.querySelector(DASHBOARD_SELECTOR);
      dashboard.style.width = '100%';

      setTimeout(() => {
        handleIframeSizing();
      }, 200);
    };

    window.addEventListener('resize', handleWindowResize);
    return () => window.removeEventListener('resize', handleWindowResize);
  }, []);

  return (
    <div className="enterprise-reporting-dashboard__navigation-links">
      <NxH3 className="enterprise-reporting-dashboard__type">{title}:</NxH3>
      <ul className="enterprise-reporting-dashboard__link-list">
        {dashboards?.length &&
          dashboards.map((dashboard) => (
            <li
              className={`enterprise-reporting-dashboard__link-item item--${dashboard.dashboardId}`}
              key={dashboard.dashboardId}
            >
              {dashboard.dashboardId === activeDashboard ? (
                <span>{cleanTitle(dashboard.title)}</span>
              ) : (
                <NxTooltip
                  onOpen={handleIframeSizing}
                  title={
                    disableLink(dashboard)
                      ? `Upgrade to IQ version ${dashboard.sinceIQVersion} to access this insight`
                      : null
                  }
                >
                  <NxTextLink
                    className="enterprise-reporting-dashboard__text-link"
                    href={uiRouterState.href('enterpriseReportingDashboard', { id: dashboard.dashboardId })}
                    disabled={disableLink(dashboard)}
                  >
                    {cleanTitle(dashboard.title)}
                  </NxTextLink>
                </NxTooltip>
              )}
            </li>
          ))}
      </ul>
    </div>
  );
}

const dashboardPropType = PropTypes.shape({
  title: PropTypes.string.isRequired,
  dashboardId: PropTypes.string.isRequired,
});

NavigationBarRow.propTypes = {
  dashboards: PropTypes.arrayOf(dashboardPropType),
  title: PropTypes.string.isRequired,
  activeDashboard: PropTypes.string,
};

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import classNames from 'classnames';
import {
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
  NxH1,
  NxH3,
  NxTooltip,
  NxTabs,
  NxTabList,
  NxTab,
  NxTabPanel,
  NxErrorAlert,
  NxButtonBar,
  NxButton,
  NxTag,
} from '@sonatype/react-shared-components';
import { find, includes, replace, pluck, test, when } from 'ramda';
import { useDispatch, useSelector } from 'react-redux';

import EnterpriseReportingSupportInfo from '../supportInfo/EnterpiseReportingSupportInfo';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import {
  selectEnterpriseReportingDashboard,
  selectVisibleDashboards,
  selectEnterpriseDashboards,
  selectDataInsightsDashboards,
} from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';
import { selectRouterCurrentParams, selectRouterPrevState } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectEnterpriseReportingFilter,
  selectIsFilterDirty,
} from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSelectors';
import {
  actions as drawerActions,
  EI_DEFAULT_FILTER_NAME,
} from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectEnterpriseReportingLicenseError,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { getUpgradeVersion, isElementDisabled } from '../utils';
import useLookerDashboard from 'MainRoot/react/useLookerDashboard';
import EnterpriseReportingFilter from 'MainRoot/enterpriseReporting/filter/EnterpriseReportingFilter';
import { sendGainsightCustomEvent } from 'MainRoot/util/gainsightUtils';

/* global CLM_SERVER_VERSION */
export default function EnterpriseReportingDashboardPage() {
  const clmServerVersion = CLM_SERVER_VERSION; // Provided by webpack DefinePlugin
  const dispatch = useDispatch();

  const loadingFeatures = useSelector(selectLoadingFeatures);
  const licenseError = useSelector(selectEnterpriseReportingLicenseError);
  const { id, groupId } = useSelector(selectRouterCurrentParams); //need to pull the dashboard's id from the URL to query Looker
  const routerPrevState = useSelector(selectRouterPrevState);
  const {
    loading,
    loadError,
    dashboardTabs,
    activeDashboardTab,
    selectedDashboardName,
    selectedDashboard,
  } = useSelector(selectEnterpriseReportingDashboard);
  const { appliedFilterName } = useSelector(selectEnterpriseReportingFilter);
  const isFilterDirty = useSelector(selectIsFilterDirty);
  const combinedDashboards = useSelector(selectVisibleDashboards);
  const enterpriseDashboards = useSelector(selectEnterpriseDashboards);
  const dataInsightsDashboards = useSelector(selectDataInsightsDashboards);
  const toggleShowFilter = () => dispatch(drawerActions.toggleShowFilter());

  const { loadingDashboard, iframeError } = useLookerDashboard();

  const clmVersion = parseInt(clmServerVersion.split('.')[1]);
  const isDashboardDisabled = (dashboard) => clmVersion < parseInt(dashboard.sinceIQVersion);

  const isLoading = loadingFeatures || loadingDashboard;
  const combinedError = licenseError || iframeError;
  const iframeClassNames = classNames('enterprise-reporting-dashboard__container', {
    loading: loadingDashboard,
  });

  useEffect(() => {
    if (!['enterpriseReportingDashboardGroup', 'enterpriseReportingDashboard'].includes(routerPrevState.name)) {
      dispatch(actions.load());
    }
  }, [dispatch, routerPrevState.name]);

  useEffect(() => {
    if (combinedDashboards?.length) {
      dispatch(actions.updateDashboardPage(id, groupId, isDashboardDisabled));
    }
  }, [dispatch, combinedDashboards, id, groupId]);

  useEffect(() => {
    // Use URL id param as fallback for group cards where selectedDashboard may not have dashboardId
    // id is authoritative from the URL - fires exactly once per navigation
    const dashboardId = selectedDashboard?.dashboardId || id;
    if (dashboardId) {
      sendGainsightCustomEvent('enterprise-reporting-dashboard-viewed', { dashboardId });
    }
  }, [id]);

  const onTabSelect = (index, groupId) => {
    dispatch(
      stateGo(
        'enterpriseReportingDashboardGroup',
        { groupId, id: dashboardTabs[index].dashboardId },
        { notify: false, location: 'replace' }
      )
    );
  };

  const iframeContainerHtml = (
    <>
      <NxLoadWrapper
        loading={isLoading}
        retryHandler={() => dispatch(stateGo('enterpriseReporting'))}
        error={combinedError}
      ></NxLoadWrapper>
      {/* The dashboard container should be outside of the load wrapper or the looker sdk wont be able to embed the iframe */}
      {!iframeError && <div className={iframeClassNames} id="dashboard" role="enterprise-reporting-dashboard" />}
    </>
  );

  const calculateTabTitle = when(test(/^view\s+/i), replace(/^view\s+/i, ''));

  return (
    <>
      {selectedDashboard?.category === 'enterprise' && <EnterpriseReportingFilter />}
      <NxPageMain id="enterprise-reporting-dashboard-page" className="nx-viewport-sized">
        <nav className="enterprise-reporting-dashboard__navigation-bar">
          <NavigationBarRow
            activeDashboard={id}
            dashboards={enterpriseDashboards}
            title="Enterprise Dashboards"
            isDashboardDisabled={isDashboardDisabled}
          />
          <NavigationBarRow
            activeDashboard={id}
            dashboards={dataInsightsDashboards}
            title="Data Insights"
            isDashboardDisabled={isDashboardDisabled}
          />
        </nav>
        <NxPageTitle>
          <NxH1>{selectedDashboardName}</NxH1>
          {selectedDashboard?.category === 'enterprise' && (
            <NxButtonBar>
              <div className="enterprise-reporting-dashboard__filter-tag">
                <NxH3>Current Filter Set:</NxH3>
                <NxTag color="sky">
                  {isFilterDirty && '*'}
                  {appliedFilterName || EI_DEFAULT_FILTER_NAME}
                </NxTag>
              </div>
              <NxButton className="filter-button" onClick={toggleShowFilter}>
                Save / Apply Filters
              </NxButton>
            </NxButtonBar>
          )}
        </NxPageTitle>

        <NxLoadWrapper
          loading={loading}
          retryHandler={() => dispatch(stateGo('enterpriseReporting'))}
          error={loadError}
        >
          {dashboardTabs.length ? (
            <>
              <NxTabs
                activeTab={activeDashboardTab}
                className="enterprise-reporting-dashboard__tabs"
                onTabSelect={(tabIndex) => onTabSelect(tabIndex, groupId)}
              >
                <NxTabList>
                  {dashboardTabs.map((dashboard) => (
                    <NxTab
                      key={dashboard.dashboardId}
                      className={`enterprise-reporting-dashboard-${dashboard.dashboardId}`}
                    >
                      {calculateTabTitle(dashboard.accessButtonText)}
                    </NxTab>
                  ))}
                </NxTabList>
                {dashboardTabs.map((dashboard) => (
                  <NxTabPanel key={dashboard.dashboardId} id={dashboard.dashboardId}>
                    {isDashboardDisabled(dashboard) ? (
                      <NxErrorAlert className="dashboard-disabled">
                        You&apos;re using a version of Lifecycle that does not support this dashboard. To unlock this
                        feature,{' '}
                        <NxTextLink external href="https://links.sonatype.com/products/clm/download">
                          update to version {dashboard.sinceIQVersion} or later
                        </NxTextLink>
                      </NxErrorAlert>
                    ) : (
                      iframeContainerHtml
                    )}
                  </NxTabPanel>
                ))}
              </NxTabs>
            </>
          ) : (
            iframeContainerHtml
          )}
        </NxLoadWrapper>
        <EnterpriseReportingSupportInfo />
      </NxPageMain>
    </>
  );
}

EnterpriseReportingDashboardPage.propTypes = {
  clmServerVersion: PropTypes.string,
};

function NavigationBarRow({ dashboards, title, activeDashboard, isDashboardDisabled }) {
  const DASHBOARD_SELECTOR = '.enterprise-reporting-dashboard__container iframe';
  const uiRouterState = useRouterState();

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

  const calculateFirstEnabledDashboard = (dashboards) => {
    const firstEnabledChild = find((el) => !isDashboardDisabled(el), dashboards);
    return firstEnabledChild || dashboards[0];
  };

  const calculateHref = (dashboard) => {
    if (dashboard.groupedDashboards) {
      return uiRouterState.href('enterpriseReportingDashboardGroup', {
        groupId: dashboard.groupId,
        id: calculateFirstEnabledDashboard(dashboard.groupedDashboards).dashboardId,
      });
    } else {
      return uiRouterState.href('enterpriseReportingDashboard', {
        id: dashboard.dashboardId,
      });
    }
  };

  const isDashboardActive = (dashboard) => {
    if (dashboard.groupedDashboards) {
      return includes(activeDashboard, pluck('dashboardId', dashboard.groupedDashboards));
    } else {
      return dashboard.dashboardId === activeDashboard;
    }
  };

  return (
    <div className="enterprise-reporting-dashboard__navigation-links">
      <NxH3 className="enterprise-reporting-dashboard__type">{title}:</NxH3>
      <ul className="enterprise-reporting-dashboard__link-list">
        {dashboards?.length &&
          dashboards.map((dashboard, idx) => (
            <li
              className={`enterprise-reporting-dashboard__link-item item--${
                dashboard.dashboardId || dashboard.groupId
              }`}
              key={idx}
            >
              {isDashboardActive(dashboard) ? (
                <span>{dashboard.title}</span>
              ) : (
                <NxTooltip
                  onOpen={handleIframeSizing}
                  title={
                    isElementDisabled(dashboard, isDashboardDisabled)
                      ? `Upgrade to IQ version ${getUpgradeVersion(dashboard)} to access this insight`
                      : null
                  }
                >
                  <NxTextLink
                    className="enterprise-reporting-dashboard__text-link"
                    href={calculateHref(dashboard)}
                    disabled={!!isElementDisabled(dashboard, isDashboardDisabled)}
                  >
                    {dashboard.title}
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
  dashboardId: PropTypes.string,
  groupId: PropTypes.string,
});

NavigationBarRow.propTypes = {
  dashboards: PropTypes.arrayOf(dashboardPropType),
  title: PropTypes.string.isRequired,
  activeDashboard: PropTypes.string,
  isDashboardDisabled: PropTypes.func,
};

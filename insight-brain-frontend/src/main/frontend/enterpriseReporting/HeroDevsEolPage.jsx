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
  NxP,
  NxButton,
  NxButtonBar,
  NxTag,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faEnvelope } from '@fortawesome/free-solid-svg-icons';
import { find, includes, pluck } from 'ramda';
import { useDispatch, useSelector } from 'react-redux';

import EnterpriseReportingSupportInfo from './supportInfo/EnterpiseReportingSupportInfo';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import {
  selectEnterpriseDashboards,
  selectDataInsightsDashboards,
  selectPartnerDashboards,
} from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';
import { selectRouterPrevState } from 'MainRoot/reduxUiRouter/routerSelectors';
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
import { getUpgradeVersion, isElementDisabled } from './utils';
import useLookerDashboard from 'MainRoot/react/useLookerDashboard';
import EnterpriseReportingFilter from 'MainRoot/enterpriseReporting/filter/EnterpriseReportingFilter';

/* global CLM_SERVER_VERSION */
export default function HeroDevsEolPage() {
  const clmServerVersion = CLM_SERVER_VERSION; // Provided by webpack DefinePlugin
  const dispatch = useDispatch();

  const loadingFeatures = useSelector(selectLoadingFeatures);
  const licenseError = useSelector(selectEnterpriseReportingLicenseError);
  const routerPrevState = useSelector(selectRouterPrevState);
  const enterpriseDashboards = useSelector(selectEnterpriseDashboards);
  const dataInsightsDashboards = useSelector(selectDataInsightsDashboards);
  const partnerDashboards = useSelector(selectPartnerDashboards);
  const { appliedFilterName } = useSelector(selectEnterpriseReportingFilter);
  const isFilterDirty = useSelector(selectIsFilterDirty);
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
    if (
      !['enterpriseReportingDashboardGroup', 'enterpriseReportingDashboard', 'heroDevsEol'].includes(
        routerPrevState.name
      )
    ) {
      dispatch(actions.load());
    }
  }, [dispatch, routerPrevState.name]);

  // Load HeroDevs dashboard metadata from HDS (deployed via Jenkins)
  useEffect(() => {
    const combinedDashboards = [
      ...(enterpriseDashboards || []),
      ...(dataInsightsDashboards || []),
      ...(partnerDashboards || []),
    ];
    const heroDevsDashboard = combinedDashboards.find((d) => d.dashboardId === 'herodevs_eol');

    if (heroDevsDashboard) {
      dispatch(actions.setSelectedDashboard(heroDevsDashboard));
      dispatch(actions.setSelectedDashboardName(heroDevsDashboard.title));
    }
  }, [dispatch, enterpriseDashboards, dataInsightsDashboards, partnerDashboards]);

  return (
    <>
      <EnterpriseReportingFilter />
      <NxPageMain id="enterprise-reporting-dashboard-page" className="nx-viewport-sized">
        <nav className="enterprise-reporting-dashboard__navigation-bar">
          <NavigationBarRow
            activeDashboard="herodevs_eol"
            dashboards={enterpriseDashboards}
            title="Enterprise Dashboards"
            isDashboardDisabled={isDashboardDisabled}
          />
          <NavigationBarRow
            activeDashboard="herodevs_eol"
            dashboards={dataInsightsDashboards}
            title="Data Insights"
            isDashboardDisabled={isDashboardDisabled}
          />
        </nav>
        <NxPageTitle>
          <NxH1>HeroDevs End Of Life Components</NxH1>
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
        </NxPageTitle>

        <NxLoadWrapper
          loading={isLoading}
          retryHandler={() => dispatch(stateGo('enterpriseReporting'))}
          error={combinedError}
        ></NxLoadWrapper>
        {/* The dashboard container should be outside of the load wrapper or the looker sdk wont be able to embed the iframe */}
        {!iframeError && <div className={iframeClassNames} id="dashboard" role="enterprise-reporting-dashboard" />}
        <EnterpriseReportingSupportInfo />
      </NxPageMain>
    </>
  );
}

HeroDevsEolPage.propTypes = {
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
      if (dashboard) {
        dashboard.style.width = '100%';

        setTimeout(() => {
          handleIframeSizing();
        }, 200);
      }
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

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
  NxH1,
  NxH3,
  NxButton,
  NxButtonBar,
  NxTag,
} from '@sonatype/react-shared-components';

import EnterpriseReportingSupportInfo from 'MainRoot/enterpriseReporting/supportInfo/EnterpiseReportingSupportInfo';
import EnterpriseReportingFilter from 'MainRoot/enterpriseReporting/filter/EnterpriseReportingFilter';
import {
  actions as filterActions,
  EI_DEFAULT_FILTER_NAME,
} from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import {
  selectEnterpriseReportingFilter,
  selectIsFilterDirty,
} from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSelectors';
import { actions } from '../firewallEnterpriseReportingSlice';
import {
  selectLoading,
  selectLoadError,
  selectBaseUrl,
  selectSelectedDashboard,
  selectSelectedDashboardName,
  selectDashboards,
  selectAllDashboards,
} from '../firewallEnterpriseReportingSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import useLookerDashboard from 'MainRoot/react/useLookerDashboard';

function FirewallNavigationBarRow({ dashboards, title, activeDashboard }) {
  const uiRouterState = useRouterState();

  if (!dashboards?.length) return null;

  return (
    <div className="fw-enterprise-reporting-dashboard__navigation-links">
      <NxH3 className="fw-enterprise-reporting-dashboard__type">{title}:</NxH3>
      <ul className="fw-enterprise-reporting-dashboard__link-list">
        {dashboards.map((dashboard) => (
          <li
            className={`fw-enterprise-reporting-dashboard__link-item item--${dashboard.dashboardId}`}
            key={dashboard.dashboardId}
          >
            {dashboard.dashboardId === activeDashboard ? (
              <span>{dashboard.title}</span>
            ) : (
              <NxTextLink
                className="fw-enterprise-reporting-dashboard__text-link"
                href={uiRouterState.href('firewall.enterpriseReportingDashboard', {
                  id: dashboard.dashboardId,
                })}
              >
                {dashboard.title}
              </NxTextLink>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

FirewallNavigationBarRow.propTypes = {
  dashboards: PropTypes.arrayOf(
    PropTypes.shape({
      dashboardId: PropTypes.string.isRequired,
      title: PropTypes.string.isRequired,
    })
  ),
  title: PropTypes.string.isRequired,
  activeDashboard: PropTypes.string,
};

export default function FirewallEnterpriseReportingDashboardPage() {
  const dispatch = useDispatch();

  const { id } = useSelector(selectRouterCurrentParams);
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const baseUrl = useSelector(selectBaseUrl);
  const selectedDashboard = useSelector(selectSelectedDashboard);
  const selectedDashboardName = useSelector(selectSelectedDashboardName);
  const allDashboards = useSelector(selectAllDashboards);
  const firewallDashboards = useSelector(selectDashboards);
  const { appliedFilterName } = useSelector(selectEnterpriseReportingFilter);
  const isFilterDirty = useSelector(selectIsFilterDirty);

  // Pass Firewall-specific selectors to useLookerDashboard hook
  const { loadingDashboard, iframeError } = useLookerDashboard('#fw-dashboard', {
    selectBaseUrl,
    selectSelectedDashboard,
  });

  const isLoading = loading || loadingDashboard;
  const combinedError = loadError || iframeError;

  const iframeClassNames = classNames('fw-enterprise-reporting-dashboard__container', {
    loading: loadingDashboard,
  });

  const toggleShowFilter = () => dispatch(filterActions.toggleShowFilter());

  // Load dashboard data on mount
  useEffect(() => {
    dispatch(actions.loadDashboardDetail());
  }, [dispatch]);

  useEffect(() => {
    if (id && allDashboards && allDashboards.length > 0) {
      dispatch(actions.updateSelectedDashboard(id));
    }
  }, [dispatch, id, allDashboards]);

  const iframeContainerHtml = (
    <>
      <NxLoadWrapper
        loading={isLoading}
        retryHandler={() => dispatch(actions.loadDashboardDetail())}
        error={combinedError}
      />
      {!iframeError && (
        <div className={iframeClassNames} id="fw-dashboard" role="firewall-enterprise-reporting-dashboard" />
      )}
    </>
  );

  return (
    <>
      {selectedDashboard?.category === 'firewall' && <EnterpriseReportingFilter />}
      <NxPageMain id="fw-enterprise-reporting-dashboard-page" className="nx-viewport-sized">
        {firewallDashboards?.length > 0 && (
          <nav className="fw-enterprise-reporting-dashboard__navigation-bar">
            <FirewallNavigationBarRow
              activeDashboard={id}
              dashboards={firewallDashboards}
              title="Firewall Dashboards"
            />
          </nav>
        )}

        <NxPageTitle>
          <NxH1>{selectedDashboardName || 'Loading Dashboard...'}</NxH1>
          {selectedDashboard?.category === 'firewall' && (
            <NxButtonBar>
              <div className="fw-enterprise-reporting-dashboard__filter-tag">
                <NxH3>Current Filter Set:</NxH3>
                <NxTag color="sky">
                  {isFilterDirty && '*'}
                  {appliedFilterName || EI_DEFAULT_FILTER_NAME}
                </NxTag>
              </div>
              <NxButton className="fw-enterprise-reporting-dashboard__filter-button" onClick={toggleShowFilter}>
                Save / Apply Filters
              </NxButton>
            </NxButtonBar>
          )}
        </NxPageTitle>

        {!baseUrl || !selectedDashboard ? (
          <NxLoadWrapper
            loading={loading}
            retryHandler={() => dispatch(actions.loadDashboardDetail())}
            error={loadError}
          >
            <div>Preparing dashboard...</div>
          </NxLoadWrapper>
        ) : (
          iframeContainerHtml
        )}

        <EnterpriseReportingSupportInfo />
      </NxPageMain>
    </>
  );
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxGlobalSidebar,
  NxGlobalSidebarNavigation,
  NxGlobalSidebarNavigationLink,
  useToggle,
} from '@sonatype/react-shared-components';
import { faArrowToLeft, faBars, faStars } from '@fortawesome/pro-regular-svg-icons';
import { faDatabase } from '@fortawesome/pro-solid-svg-icons';
import {
  faChartArea,
  faFileChartLine,
  faGavel,
  faHome,
  faMicroscope,
  faSearch,
  faShieldCheck,
  faSitemap,
} from '@fortawesome/pro-solid-svg-icons';

import { useRouterState } from '../RouterStateContext';
import IqSidebarNavFooter from './IqSidebarNavFooter';

import { getProductLogo } from '../../util/productLogoUtils';
import { isLeftNavigationOpen, setLeftNavigationOpen } from '../../util/preferenceStore';

function IqSidebarNav(props) {
  const uiRouterState = useRouterState();
  const [isOpen, toggleOpen] = useToggle(isLeftNavigationOpen());

  const {
    productEdition,
    releaseVersion,
    isLoggedIn,
    isLicensed,
    isDashboardAvailable,
    isReportsListAvailable,
    isSuccessMetricsEnabled,
    isAdvancedSearchEnabled,
    isFirewallEnabled,
    isLegalEnabled,
    isApiPageEnabled,
    isDataInsightsEnabled,
  } = props;

  const logo = getProductLogo(productEdition);

  const apiHref = uiRouterState.href('api');
  const dashboardHref = uiRouterState.href('dashboard.overview.violations');
  const orgsPoliciesHref = uiRouterState.href('management.view');
  const reportsHref = uiRouterState.href('violations');
  const successMetricsHref = uiRouterState.href('labs.successMetrics');
  const vulnSearchHref = uiRouterState.href('vulnerabilitySearch');
  const advSearchHref = uiRouterState.href('advancedSearch');
  const firewallHref = uiRouterState.href('firewall.firewallPage');
  const legalHref = uiRouterState.href('legal.dashboard');
  const dataInsightsHref = uiRouterState.href('dataInsights');

  const isSelected = (entryName) => {
    return uiRouterState.includes(entryName);
  };

  const isVulnerabilitySearchSelected = isSelected('vulnerabilitySearch') || isSelected('vulnerabilitySearchDetail');
  const isFirewallSelected = isSelected('firewall') || isSelected('firewallAutoUnquarantine');
  const isReportsSelected = isSelected('violations') || isSelected('transitiveViolations');

  useEffect(() => {
    setLeftNavigationOpen(isOpen);
  }, [isOpen]);

  return (
    <NxGlobalSidebar
      isOpen={isOpen}
      onToggleClick={toggleOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faBars}
      logoImg={logo}
      logoAltText={productEdition}
      logoLink="#"
    >
      {isLoggedIn && (
        <NxGlobalSidebarNavigation id="main-header-buttons">
          {isDashboardAvailable && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('dashboard')}
              id="dashboard-navigation-button"
              icon={faHome}
              text="Dashboard"
              href={dashboardHref}
            />
          )}
          {isLicensed && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('management')}
              id="policies-navigation-button"
              icon={faSitemap}
              text="Orgs and Policies"
              href={orgsPoliciesHref}
            />
          )}
          {isReportsListAvailable && (
            <NxGlobalSidebarNavigationLink
              isSelected={isReportsSelected}
              id="reporting-navigation-button"
              icon={faFileChartLine}
              text="Reports"
              href={reportsHref}
            />
          )}
          {isSuccessMetricsEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('labs')}
              id="labs-navigation-button"
              icon={faChartArea}
              text="Success Metrics"
              href={successMetricsHref}
            />
          )}
          {isLicensed && (
            <NxGlobalSidebarNavigationLink
              isSelected={isVulnerabilitySearchSelected}
              id="vulnerability-navigation-button"
              icon={faMicroscope}
              text="Vulnerability Search"
              href={vulnSearchHref}
            />
          )}
          {isLicensed && isAdvancedSearchEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('advancedSearch')}
              id="search-navigation-button"
              icon={faSearch}
              text="Advanced Search"
              href={advSearchHref}
            />
          )}
          {isLicensed && isFirewallEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isFirewallSelected}
              id="firewall-navigation-button"
              icon={faShieldCheck}
              text="Firewall"
              href={firewallHref}
            />
          )}
          {isLicensed && isLegalEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('legal')}
              id="advanced-legal-navigation-button"
              icon={faGavel}
              text="Legal"
              href={legalHref}
            />
          )}
          {isApiPageEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('api')}
              id="api-navigation-button"
              icon={faStars}
              text="API"
              href={apiHref}
            />
          )}
          {isDataInsightsEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('dataInsights')}
              id="data-insights-navigation-button"
              icon={faDatabase}
              text={
                <>
                  <span className="nx-global-sidebar__label-container">
                    <span>Data Insights</span>
                    <span className="nx-global-sidebar__hidden-label">Data Insights</span>
                  </span>
                  <span className="nx-global-sidebar__navigation-badge">Labs</span>
                </>
              }
              href={dataInsightsHref}
            />
          )}
        </NxGlobalSidebarNavigation>
      )}
      {productEdition && releaseVersion && (
        <IqSidebarNavFooter productName={productEdition} releaseNumber={releaseVersion} />
      )}
    </NxGlobalSidebar>
  );
}

IqSidebarNav.propTypes = {
  currentState: PropTypes.object,
  productEdition: PropTypes.string,
  releaseVersion: PropTypes.string,
  isLoggedIn: PropTypes.bool,
  isLicensed: PropTypes.bool,
  isDashboardAvailable: PropTypes.bool,
  isReportsListAvailable: PropTypes.bool,
  isSuccessMetricsEnabled: PropTypes.bool,
  isAdvancedSearchEnabled: PropTypes.bool,
  isFirewallEnabled: PropTypes.bool,
  isLegalEnabled: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
  isDataInsightsEnabled: PropTypes.bool,
};
export default IqSidebarNav;

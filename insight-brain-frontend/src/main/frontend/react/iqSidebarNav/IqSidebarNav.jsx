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
import { faArrowToLeft, faBars, faStars, faWrench } from '@fortawesome/pro-regular-svg-icons';
import {
  faChartArea,
  faFileChartLine,
  faGavel,
  faHome,
  faMicroscope,
  faSearch,
  faShieldCheck,
  faSitemap,
  faDatabase,
  faChartPieAlt,
} from '@fortawesome/pro-solid-svg-icons';

import { useRouterState } from '../RouterStateContext';
import IqSidebarNavFooter from './IqSidebarNavFooter';

import { getProductLogo } from '../../util/productLogoUtils';
import { isLeftNavigationOpen, setLeftNavigationOpen } from '../../util/preferenceStore';

function IqSidebarNav(props) {
  const uiRouterState = useRouterState();
  const [isOpen, toggleOpen, setToggleOpen] = useToggle(isLeftNavigationOpen());

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
    isShowVersionEnabled,
    isFirewallOnlyLicense,
    isDeveloperDashboardEnabled,
    isLookerIntegratedEnterpriseReportingEnabled,
  } = props;

  const logo = getProductLogo(productEdition);

  const apiHref = uiRouterState.href('api');
  const enterpriseReportingHref = uiRouterState.href('enterpriseReporting');
  const dashboardHref = isFirewallOnlyLicense
    ? uiRouterState.href('dashboard.overview.waivers')
    : uiRouterState.href('dashboard.overview.violations');
  const logoHref = uiRouterState.href('home');
  const orgsPoliciesHref = uiRouterState.href('management.view');
  const reportsHref = uiRouterState.href('violations');
  const successMetricsHref = uiRouterState.href('labs.successMetrics');
  const vulnSearchHref = uiRouterState.href('vulnerabilitySearch');
  const advSearchHref = uiRouterState.href('advancedSearch');
  const firewallHref = uiRouterState.href('firewall.firewallPage');
  const legalHref = uiRouterState.href('legal.dashboard');
  const dataInsightsHref = uiRouterState.href('dataInsights');
  const integrationsHref = uiRouterState.href('integrations');

  const isSelected = (entryName) => {
    return uiRouterState.includes(entryName);
  };

  const isVulnerabilitySearchSelected = isSelected('vulnerabilitySearch') || isSelected('vulnerabilitySearchDetail');
  const isFirewallSelected = isSelected('firewall') || isSelected('firewallAutoUnquarantine');
  const isReportsSelected = isSelected('violations') || isSelected('transitiveViolations');

  useEffect(() => {
    const handleStorage = () => setToggleOpen(isLeftNavigationOpen());
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

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
      logoLink={logoHref}
    >
      {isLoggedIn && (
        <NxGlobalSidebarNavigation id="global-sidebar-buttons">
          {(isDashboardAvailable || isFirewallOnlyLicense) && (
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
          {isReportsListAvailable && !isFirewallOnlyLicense && (
            <NxGlobalSidebarNavigationLink
              isSelected={isReportsSelected}
              id="reporting-navigation-button"
              icon={faFileChartLine}
              text="Reports"
              href={reportsHref}
            />
          )}
          {isSuccessMetricsEnabled && !isFirewallOnlyLicense && (
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
              text="Vulnerability Lookup"
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
          {((isLicensed && isFirewallEnabled) || isFirewallOnlyLicense) && (
            <NxGlobalSidebarNavigationLink
              isSelected={isFirewallSelected}
              id="firewall-navigation-button"
              icon={faShieldCheck}
              text="Firewall"
              href={firewallHref}
            />
          )}
          {isLicensed && isLegalEnabled && !isFirewallOnlyLicense && (
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
              className="iq-data-insights-nav-link"
              icon={faDatabase}
              text={
                <>
                  <span>Data Insights</span>
                  <span className="iq-data-insights-nav-link__navigation-badge">
                    {/* The space and parens should be in the tooltip but not visibly in the link text itself */}
                    <span className="iq-data-insights-nav-link__tooltip-only-text"> (</span>
                    Labs
                    <span className="iq-data-insights-nav-link__tooltip-only-text">)</span>
                  </span>
                </>
              }
              href={dataInsightsHref}
            />
          )}
          {isLookerIntegratedEnterpriseReportingEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('enterpriseReporting')}
              id="enterprise-reporting-navigation-button"
              icon={faChartPieAlt}
              text="Rolling Recap (V2)"
              href={enterpriseReportingHref}
            />
          )}
          {isDeveloperDashboardEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('integrations')}
              id="integrations-navigation-button"
              className="iq-integrations-nav-link"
              icon={faWrench}
              text={
                <>
                  <span>Developer</span>
                  <span className="iq-integrations-nav-link__navigation-badge preview">
                    {/* The space and parens should be in the tooltip but not visibly in the link text itself */}
                    <span className="iq-integrations-nav-link__tooltip-only-text"> (</span>
                    Preview
                    <span className="iq-integrations-nav-link__tooltip-only-text">)</span>
                  </span>
                </>
              }
              href={integrationsHref}
            />
          )}
        </NxGlobalSidebarNavigation>
      )}
      {productEdition && releaseVersion && (
        <IqSidebarNavFooter releaseNumber={releaseVersion} isShowVersionEnabled={isShowVersionEnabled} />
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
  isShowVersionEnabled: PropTypes.bool,
  isFirewallOnlyLicense: PropTypes.bool,
  isDeveloperDashboardEnabled: PropTypes.bool,
  isLookerIntegratedEnterpriseReportingEnabled: PropTypes.bool,
};
export default IqSidebarNav;

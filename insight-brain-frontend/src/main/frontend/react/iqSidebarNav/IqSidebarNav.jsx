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
import {
  faChartArea,
  faFileChartLine,
  faGavel,
  faHome,
  faMicroscope,
  faSearch,
  faSitemap,
  faChartPieAlt,
} from '@fortawesome/pro-solid-svg-icons';

import { useRouterState } from '../RouterStateContext';
import IqSidebarNavFooter from './IqSidebarNavFooter';

import { getProductLogo } from '../../util/productLogoUtils';
import { isLeftNavigationOpen, setLeftNavigationOpen } from '../../util/preferenceStore';
import SbomManagerSidebar from 'MainRoot/sbomManager/sidebar/SbomManagerSidebar';
import SonatypeDeveloperSidebar from 'MainRoot/development/SonatypeDeveloperSidebar';
import DefaultEmptyIqSidebar from 'MainRoot/react/iqSidebarNav/DefaultEmptyIqSidebar';
import FirewallSidebar from 'MainRoot/firewall/FirewallSidebar';

function IqSidebarNav(props) {
  const uiRouterState = useRouterState();
  const [isOpen, toggleOpen, setToggleOpen] = useToggle(isLeftNavigationOpen());

  const {
    productEdition,
    releaseVersion,
    isLoggedIn,
    isLicensed,
    isDashboardAvailable,
    isDashboardWaiversAvailable,
    isReportsListAvailable,
    isSuccessMetricsEnabled,
    isAdvancedSearchEnabled,
    isLegalEnabled,
    isApiPageEnabled,
    isShowVersionEnabled,
    isOrgsAndAppsEnabled,
    isSbomManagerEnabled,
    isIntegratedEnterpriseReportingSupported,
    isSbomManager,
    isProductFeaturesLoading,
    isSbomManagerOnlyLicense,
    isProductsLoading,
    isStandaloneDeveloper,
    isStandaloneFirewall,
    isFirewallOnlyLicense,
  } = props;
  const logo = getProductLogo(productEdition);

  const apiHref = uiRouterState.href('api');
  const enterpriseReportingHref = uiRouterState.href('enterpriseReporting');
  const dashboardHref = isDashboardAvailable
    ? uiRouterState.href('dashboard.overview.violations')
    : uiRouterState.href('dashboard.overview.waivers');
  const logoHref = uiRouterState.href('home');
  const orgsPoliciesHref = uiRouterState.href('management.view');
  const reportsHref = uiRouterState.href('violations');
  const successMetricsHref = uiRouterState.href('labs.successMetrics');
  const vulnSearchHref = uiRouterState.href('vulnerabilitySearch');
  const advSearchHref = uiRouterState.href('advancedSearch');
  const legalHref = uiRouterState.href('legal.dashboard');

  const isSelected = (entryName) => {
    return uiRouterState.includes(entryName);
  };

  const isVulnerabilitySearchSelected = isSelected('vulnerabilitySearch') || isSelected('vulnerabilitySearchDetail');
  const isReportsSelected = isSelected('violations') || isSelected('transitiveViolations');

  useEffect(() => {
    const handleStorage = () => setToggleOpen(isLeftNavigationOpen());
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  useEffect(() => {
    setLeftNavigationOpen(isOpen);
  }, [isOpen]);

  const sbomManagerSidebar = (
    <SbomManagerSidebar
      isLoggedIn={isLoggedIn}
      isSbomManagerEnabled={isSbomManagerEnabled}
      isApiPageEnabled={isApiPageEnabled}
    />
  );
  const sonatypeDeveloperSidebar = (
    <SonatypeDeveloperSidebar
      isLoggedIn={isLoggedIn}
      isAdvancedSearchEnabled={isAdvancedSearchEnabled}
      isApiPageEnabled={isApiPageEnabled}
    />
  );
  const sonatypeFirewallSidebar = <FirewallSidebar isLoggedIn={isLoggedIn} isApiPageEnabled={isApiPageEnabled} />;

  const iqSidebar = (
    <NxGlobalSidebar
      isOpen={isOpen}
      onToggleClick={toggleOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faBars}
      logoImg={logo}
      logoAltText={productEdition}
      logoLink={logoHref}
    >
      {isLoggedIn && !isProductsLoading && !isStandaloneFirewall && (
        <NxGlobalSidebarNavigation id="global-sidebar-buttons">
          {(isDashboardAvailable || isDashboardWaiversAvailable) && (
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
          {isReportsListAvailable && isOrgsAndAppsEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isReportsSelected}
              id="reporting-navigation-button"
              icon={faFileChartLine}
              text="Reports"
              href={reportsHref}
            />
          )}
          {isSuccessMetricsEnabled && isOrgsAndAppsEnabled && (
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
          {isLicensed && isLegalEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('legal')}
              id="advanced-legal-navigation-button"
              icon={faGavel}
              text="Legal"
              href={legalHref}
            />
          )}
          {isLicensed && isIntegratedEnterpriseReportingSupported && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('enterpriseReporting')}
              id="data-insights-button"
              className="iq-enterprise-reporting-nav-link"
              icon={faChartPieAlt}
              text="Data Insights"
              href={enterpriseReportingHref}
            />
          )}
          {isApiPageEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected('api')}
              id="api-navigation-button"
              className="iq-api-nav-link"
              icon={faStars}
              text={
                <>
                  <span>API</span>
                  <span className="iq-api-nav-link__navigation-badge">
                    {/* The space and parens should be in the tooltip but not visibly in the link text itself */}
                    <span className="iq-api-nav-link__tooltip-only-text"> (</span>
                    NEW
                    <span className="iq-api-nav-link__tooltip-only-text">)</span>
                  </span>
                </>
              }
              href={apiHref}
            />
          )}
        </NxGlobalSidebarNavigation>
      )}
      {productEdition && releaseVersion && (
        <IqSidebarNavFooter releaseNumber={releaseVersion} isShowVersionEnabled={isShowVersionEnabled} />
      )}
    </NxGlobalSidebar>
  );

  if (isProductFeaturesLoading) {
    // Empty sidebar until product info is fully loaded
    return <DefaultEmptyIqSidebar />;
  }

  if (isSbomManagerOnlyLicense || isSbomManager) {
    return sbomManagerSidebar;
  } else if (isStandaloneDeveloper) {
    return sonatypeDeveloperSidebar;
  } else if (isStandaloneFirewall || isFirewallOnlyLicense) {
    return sonatypeFirewallSidebar;
  }

  return iqSidebar;
}

IqSidebarNav.propTypes = {
  currentState: PropTypes.object,
  productEdition: PropTypes.string,
  releaseVersion: PropTypes.string,
  isLoggedIn: PropTypes.bool,
  isLicensed: PropTypes.bool,
  isDashboardAvailable: PropTypes.bool,
  isDashboardWaiversAvailable: PropTypes.bool,
  isReportsListAvailable: PropTypes.bool,
  isSuccessMetricsEnabled: PropTypes.bool,
  isAdvancedSearchEnabled: PropTypes.bool,
  isFirewallEnabled: PropTypes.bool,
  isLegalEnabled: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
  isShowVersionEnabled: PropTypes.bool,
  isDeveloperDashboardEnabled: PropTypes.bool,
  isOrgsAndAppsEnabled: PropTypes.bool,
  isSbomManagerEnabled: PropTypes.bool,
  isIntegratedEnterpriseReportingSupported: PropTypes.bool,
  isSbomManager: PropTypes.bool,
  isProductFeaturesLoading: PropTypes.bool,
  isSbomManagerOnlyLicense: PropTypes.bool,
  isProductsLoading: PropTypes.bool,
  isStandaloneDeveloper: PropTypes.bool,
  isStandaloneFirewall: PropTypes.bool,
  isFirewallOnlyLicense: PropTypes.bool,
};
export default IqSidebarNav;

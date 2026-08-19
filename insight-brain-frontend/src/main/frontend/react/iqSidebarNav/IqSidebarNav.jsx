/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxGlobalSidebar2, NxGlobalSidebar2NavigationLink, useToggle } from '@sonatype/react-shared-components';
import {
  faHouse,
  faSitemap,
  faChartColumn,
  faMicroscope,
  faMagnifyingGlass,
  faGavel,
  faChartPie,
  faChartArea,
  faArrowToLeft,
  faStars,
  faDatabase,
} from '@fortawesome/pro-regular-svg-icons';

import { useRouterState } from '../RouterStateContext';

import { isLeftNavigationOpen, setLeftNavigationOpen } from '../../util/preferenceStore';
import SbomManagerSidebar from 'MainRoot/sbomManager/sidebar/SbomManagerSidebar';
import SonatypeDeveloperSidebar from 'MainRoot/development/SonatypeDeveloperSidebar';
import DefaultEmptyIqSidebar from 'MainRoot/react/iqSidebarNav/DefaultEmptyIqSidebar';
import FirewallSidebar from 'MainRoot/firewall/FirewallSidebar';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';

function IqSidebarNav(props) {
  const uiRouterState = useRouterState();
  const [isOpen, toggleOpen, setToggleOpen] = useToggle(isLeftNavigationOpen());

  const {
    currentState,
    isLoggedIn,
    isLicensed,
    isDashboardAvailable,
    isReportsListAvailable,
    isSuccessMetricsEnabled,
    isAdvancedSearchEnabled,
    isLegalEnabled,
    isApiPageEnabled,
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
    isAlpForSbomManagerEnabled,
    isFirewallEnterpriseReportingEnabled,
    isHostedRepositoryEvaluationEnabled,
  } = props;

  const apiHref = uiRouterState.href('api');
  const enterpriseReportingHref = uiRouterState.href('enterpriseReporting');
  const operationalReportingHref = uiRouterState.href('operationalReporting');
  const dashboardHref = uiRouterState.href('dashboard.overview.violations');
  const orgsPoliciesHref = uiRouterState.href('management.view');
  const reportsHref = uiRouterState.href('violations');
  const successMetricsHref = uiRouterState.href('labs.successMetrics');
  const vulnSearchHref = uiRouterState.href('vulnerabilitySearch');
  const advSearchHref = uiRouterState.href('advancedSearch');
  const legalHref = uiRouterState.href('legal.dashboard');
  const hostedReposHref = uiRouterState.href('hostedRepos');

  // Use currentState from Redux props (triggers re-render) instead of uiRouterState.includes()
  // which may return stale data when the re-render is driven by Redux state changes
  const currentStateName = currentState?.name || '';
  const isSelected = (entryName) => {
    return currentStateName === entryName || currentStateName.startsWith(entryName + '.');
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
      currentState={currentState}
      isLoggedIn={isLoggedIn}
      isSbomManagerEnabled={isSbomManagerEnabled}
      isApiPageEnabled={isApiPageEnabled}
      isAlpForSbomManagerEnabled={isAlpForSbomManagerEnabled}
      isLegalEnabled={isLegalEnabled}
    />
  );
  const sonatypeDeveloperSidebar = (
    <SonatypeDeveloperSidebar
      currentState={currentState}
      isLoggedIn={isLoggedIn}
      isAdvancedSearchEnabled={isAdvancedSearchEnabled}
      isApiPageEnabled={isApiPageEnabled}
    />
  );
  const sonatypeFirewallSidebar = (
    <FirewallSidebar
      currentState={currentState}
      isLoggedIn={isLoggedIn}
      isApiPageEnabled={isApiPageEnabled}
      isFirewallEnterpriseReportingEnabled={isFirewallEnterpriseReportingEnabled}
    />
  );

  const iqSidebar = (
    <>
      <NxGlobalSidebar2
        isOpen={isOpen}
        onToggleClick={toggleOpen}
        toggleOpenIcon={faArrowToLeft}
        toggleCloseIcon={faArrowToRight}
        className="iq-lifecycle-sidebar"
      >
        {isLoggedIn && !isProductsLoading && !isStandaloneFirewall && (
          <>
            {isDashboardAvailable && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('dashboard')}
                id="dashboard-navigation-button"
                icon={faHouse}
                text="Dashboard"
                href={dashboardHref}
              />
            )}
            {isLicensed && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('management')}
                id="policies-navigation-button"
                icon={faSitemap}
                text="Orgs and Policies"
                href={orgsPoliciesHref}
              />
            )}
            {isLicensed && isHostedRepositoryEvaluationEnabled && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('hostedRepos')}
                id="hosted-repos-navigation-button"
                icon={faDatabase}
                text={
                  <>
                    <span>Hosted Repos</span>
                    <span className="iq-api-nav-link__navigation-badge">
                      {/* The space and parens should be in the tooltip but not visibly in the link text itself */}
                      <span className="iq-api-nav-link__tooltip-only-text"> (</span>
                      NEW
                      <span className="iq-api-nav-link__tooltip-only-text">)</span>
                    </span>
                  </>
                }
                href={hostedReposHref}
              />
            )}
            {isReportsListAvailable && isOrgsAndAppsEnabled && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isReportsSelected}
                id="reporting-navigation-button"
                icon={faChartColumn}
                text="Reports"
                href={reportsHref}
              />
            )}
            {isSuccessMetricsEnabled && isOrgsAndAppsEnabled && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('labs')}
                id="labs-navigation-button"
                icon={faChartArea}
                text="Success Metrics"
                href={successMetricsHref}
              />
            )}
            {isLicensed && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isVulnerabilitySearchSelected}
                id="vulnerability-navigation-button"
                icon={faMicroscope}
                text="Vulnerability Lookup"
                href={vulnSearchHref}
              />
            )}
            {isLicensed && isAdvancedSearchEnabled && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('advancedSearch')}
                id="search-navigation-button"
                icon={faMagnifyingGlass}
                text="Advanced Search"
                href={advSearchHref}
              />
            )}
            {isLicensed && isLegalEnabled && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('legal')}
                id="advanced-legal-navigation-button"
                icon={faGavel}
                text="Legal"
                href={legalHref}
              />
            )}
            {isLicensed && isIntegratedEnterpriseReportingSupported && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('enterpriseReporting')}
                id="enterprise-reporting-button"
                className="iq-enterprise-reporting-nav-link"
                icon={faChartPie}
                text="Enterprise Reporting"
                href={enterpriseReportingHref}
              />
            )}
            {isLicensed && !isIntegratedEnterpriseReportingSupported && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('operationalReporting')}
                id="operational-reporting-button"
                className="iq-operational-reporting-nav-link"
                icon={faChartArea}
                text="Operational Reporting"
                href={operationalReportingHref}
              />
            )}
            {isApiPageEnabled && (
              <NxGlobalSidebar2NavigationLink
                isSelected={isSelected('api')}
                id="api-navigation-button"
                className="iq-api-nav-link"
                icon={faStars}
                text="API"
                href={apiHref}
              />
            )}
          </>
        )}
      </NxGlobalSidebar2>
    </>
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
  isFirewallEnterpriseReportingEnabled: PropTypes.bool,
  isHostedRepositoryEvaluationEnabled: PropTypes.bool,
};
export default IqSidebarNav;

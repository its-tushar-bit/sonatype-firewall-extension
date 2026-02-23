/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector, useStore } from 'react-redux';
import { load as loadAdvancedSearchConfig } from '../configuration/advancedSearch/advancedSearchConfigActions';
import { selectIsAdvancedSearchEnabled } from '../configuration/advancedSearch/advancedSearchSelectors';
import { loadConfiguration as loadSuccessMetricsConfig } from '../configuration/successMetricsConfiguration/successMetricsConfigurationActions';
import { selectIsSuccessMetricsEnabled } from '../configuration/successMetricsConfiguration/successMetricsConfigurationSelectors';
import { load as loadProductLicense } from '../configuration/license/productLicenseActions';
import { actions as firewallOnboardingActions } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import {
  selectIsAdvancedLegalPackSupported,
  selectIsFirewallSupportedForNavigationContainer,
  selectIsDashboardSupported,
  selectIsDashboardWaiversSupported,
  selectIsReportListSupported,
  selectIsApiPageSupported,
  selectIsShowVersionEnabled,
  selectIsDeveloperDashboardEnabled,
  selectIsOrgsAndAppsEnabled,
  selectIsSbomManagerEnabled,
  selectIsIntegratedEnterpriseReportingSupported,
  selectLoadingFeatures,
  selectIsAlpForSbomManagerEnabled,
  selectIsFirewallEnterpriseReportingEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsStandaloneFirewall,
  selectIsSbomManager,
  selectIsStandaloneDeveloper,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsSbomManagerOnlyLicense,
  selectIsFirewallOnlyLicense,
  selectLoadingProducts,
  selectProductEdition,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import { getReleaseVersion } from 'MainRoot/util/versionUtil';
import { waitForLogin } from 'MainRoot/user/userSessionUtils';
import { selectIsLoggedIn } from 'MainRoot/user/userSessionSelectors';
import { selectIsLicensed } from 'MainRoot/productFeatures/productLicenseSelectors';
import IqSidebarNav from 'MainRoot/react/iqSidebarNav/IqSidebarNav';

export default function NavigationContainer({ clmServerVersion, $rootScope, $state }) {
  const dispatch = useDispatch();
  const store = useStore();
  const [currentState, setCurrentState] = useState($state.current);

  useEffect(() => {
    const unsubscribe = $rootScope.$on('$stateChangeSuccess', () => {
      setCurrentState($state.current);
    });
    return () => unsubscribe();
  }, [$rootScope, $state]);

  const productEdition = useSelector(selectProductEdition);
  const isAdvancedSearchEnabled = useSelector(selectIsAdvancedSearchEnabled);
  const isSuccessMetricsEnabled = useSelector(selectIsSuccessMetricsEnabled);
  const isFirewallSupported = useSelector(selectIsFirewallSupportedForNavigationContainer);
  const isAdvancedLegalPackSupported = useSelector(selectIsAdvancedLegalPackSupported);
  const isDashboardSupported = useSelector(selectIsDashboardSupported);
  const isDashboardWaiversSupported = useSelector(selectIsDashboardWaiversSupported);
  const isReportListSupported = useSelector(selectIsReportListSupported);
  const isApiPageEnabled = useSelector(selectIsApiPageSupported);
  const isShowVersionEnabled = useSelector(selectIsShowVersionEnabled);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isSbomManagerEnabled = useSelector(selectIsSbomManagerEnabled);
  const isIntegratedEnterpriseReportingSupported = useSelector(selectIsIntegratedEnterpriseReportingSupported);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);
  const isProductsLoading = useSelector(selectLoadingProducts);
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const isFirewallOnlyLicense = useSelector(selectIsFirewallOnlyLicense);
  const isAlpForSbomManagerEnabled = useSelector(selectIsAlpForSbomManagerEnabled);
  const isFirewallEnterpriseReportingEnabled = useSelector(selectIsFirewallEnterpriseReportingEnabled);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const isLicensed = useSelector(selectIsLicensed);

  useEffect(() => {
    const { loadUnconfiguredRepoManagers } = firewallOnboardingActions;
    waitForLogin().then(function () {
      dispatch(loadAdvancedSearchConfig());
      dispatch(loadSuccessMetricsConfig());
      dispatch(loadProductLicense());
      dispatch(loadUnconfiguredRepoManagers());
    });
  }, [dispatch, store]);

  const getReleaseVersionValue = () => {
    if (clmServerVersion) {
      return getReleaseVersion(clmServerVersion);
    }
    return '';
  };

  const isDashboardAvailable = () => {
    return isDashboardSupported;
  };

  const isDashboardWaiversAvailable = () => {
    return isDashboardWaiversSupported;
  };

  const isReportsListAvailable = () => {
    return isReportListSupported;
  };

  return (
    <IqSidebarNav
      currentState={currentState}
      productEdition={productEdition}
      releaseVersion={getReleaseVersionValue()}
      isLoggedIn={isLoggedIn}
      isLicensed={isLicensed}
      isDashboardAvailable={isDashboardAvailable()}
      isDashboardWaiversAvailable={isDashboardWaiversAvailable()}
      isReportsListAvailable={isReportsListAvailable()}
      isSuccessMetricsEnabled={isSuccessMetricsEnabled}
      isAdvancedSearchEnabled={isAdvancedSearchEnabled}
      isFirewallEnabled={isFirewallSupported}
      isLegalEnabled={isAdvancedLegalPackSupported}
      isApiPageEnabled={isApiPageEnabled}
      isShowVersionEnabled={isShowVersionEnabled}
      isDeveloperDashboardEnabled={isDeveloperDashboardEnabled}
      isOrgsAndAppsEnabled={isOrgsAndAppsEnabled}
      isSbomManagerEnabled={isSbomManagerEnabled}
      isIntegratedEnterpriseReportingSupported={isIntegratedEnterpriseReportingSupported}
      isSbomManager={isSbomManager}
      isProductFeaturesLoading={isProductFeaturesLoading}
      isSbomManagerOnlyLicense={isSbomManagerOnlyLicense}
      isProductsLoading={isProductsLoading}
      isStandaloneDeveloper={isStandaloneDeveloper}
      isStandaloneFirewall={isStandaloneFirewall}
      isFirewallOnlyLicense={isFirewallOnlyLicense}
      isAlpForSbomManagerEnabled={isAlpForSbomManagerEnabled}
      isFirewallEnterpriseReportingEnabled={isFirewallEnterpriseReportingEnabled}
    />
  );
}

NavigationContainer.propTypes = {
  clmServerVersion: PropTypes.string,
  $rootScope: PropTypes.object.isRequired,
  $state: PropTypes.object.isRequired,
};

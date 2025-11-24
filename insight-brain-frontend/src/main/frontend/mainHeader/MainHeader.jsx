/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useSelector, useStore, useDispatch } from 'react-redux';
import MenuBar from './MenuBar/MenuBar.jsx';
import {
  selectIsAutomationSupported,
  selectIsWebhooksSupported,
  selectIsCrowdIntegrationSupported,
  selectIsWebhookConfigurationEnabled,
  selectIsProductLicenseConfigurationEnabled,
  selectIsLdapConfigurationEnabled,
  selectIsEmailConfigurationEnabled,
  selectIsProxyConfigurationEnabled,
  selectIsSystemNoticeConfigurationEnabled,
  selectIsSuccessMetricsConfigurationEnabled,
  selectIsAutomaticApplicationConfigurationEnabled,
  selectIsAutomaticScmConfigurationEnabled,
  selectIsAdvancedSearchConfigurationEnabled,
  selectIsShowNotificationMenuEnabled,
  selectIsBaseUrlConfigurationEnabled,
  selectIsMonitoringSupported,
  selectTenantMode,
  selectIsSsoIdpManagedBySonatype,
  selectIsOrgsAndAppsEnabled,
  selectIsZscalerEnabled,
  selectIsSAMLEnabled,
  selectIsOAuth2Enabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsSbomManagerOnlyLicense,
  selectIsFirewallOnlyLicense,
  selectHasLifecycleLicense,
  selectHasAuditorLicense,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import {
  selectIsStandaloneDeveloper,
  selectIsStandaloneFirewall,
  selectIsStandaloneSbomManager,
  selectCurrentRouteName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { fetchUser } from 'MainRoot/user/userSessionUtils';
import { selectIsLoggedIn } from 'MainRoot/user/userSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { actions } from './mainHeaderSlice';
import { selectPermissions, selectShouldShowLoginButton } from './mainHeaderSelectors';
import userActions from 'MainRoot/user/userActions';

export function MainHeader({ clmServerVersion = '' }) {
  const store = useStore();
  const dispatch = useDispatch();
  const globalMajorMinorVersion = useMemo(
    () => (clmServerVersion ? `${clmServerVersion}` : '').split('.').splice(0, 2).join('.'),
    [clmServerVersion]
  );

  // Redux selectors
  const permissions = useSelector(selectPermissions);
  const shouldShowLoginButton = useSelector(selectShouldShowLoginButton);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const isWebhooksSupported = useSelector(selectIsWebhooksSupported);
  const isSourceControlSupported = useSelector(selectIsAutomationSupported);
  const isCrowdIntegrationEnabled = useSelector(selectIsCrowdIntegrationSupported);
  const isWebhookConfigurationEnabled = useSelector(selectIsWebhookConfigurationEnabled);
  const isProductLicenseConfigurationEnabled = useSelector(selectIsProductLicenseConfigurationEnabled);
  const isLdapConfigurationEnabled = useSelector(selectIsLdapConfigurationEnabled);
  const isEmailConfigurationEnabled = useSelector(selectIsEmailConfigurationEnabled);
  const isProxyConfigurationEnabled = useSelector(selectIsProxyConfigurationEnabled);
  const isSystemNoticeConfigurationEnabled = useSelector(selectIsSystemNoticeConfigurationEnabled);
  const isSuccessMetricsConfigurationEnabled = useSelector(selectIsSuccessMetricsConfigurationEnabled);
  const isAutomaticApplicationConfigurationEnabled = useSelector(selectIsAutomaticApplicationConfigurationEnabled);
  const isAutomaticScmConfigurationEnabled = useSelector(selectIsAutomaticScmConfigurationEnabled);
  const isAdvancedSearchConfigurationEnabled = useSelector(selectIsAdvancedSearchConfigurationEnabled);
  const isShowNotificationMenuEnabled = useSelector(selectIsShowNotificationMenuEnabled);
  const isBaseUrlConfigurationEnabled = useSelector(selectIsBaseUrlConfigurationEnabled);
  const isSamlConfigurationEnabled = useSelector(selectIsSAMLEnabled);
  const isOAuth2ConfigurationEnabled = useSelector(selectIsOAuth2Enabled);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isSsoIdpManagedBySonatype = useSelector(selectIsSsoIdpManagedBySonatype);
  const tenantMode = useSelector(selectTenantMode);
  const isSingleTenant = tenantMode !== 'multi-tenant';
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isFirewallOnlyLicense = useSelector(selectIsFirewallOnlyLicense);
  const isZscalerEnabled = useSelector(selectIsZscalerEnabled);
  const isStandaloneSbomManager = useSelector(selectIsStandaloneSbomManager);
  const hasLifecycleLicense = useSelector(selectHasLifecycleLicense);
  const currentRouteName = useSelector(selectCurrentRouteName);
  const hasRoutesResolved = !isNilOrEmpty(currentRouteName);
  const hasAuditorLicense = useSelector(selectHasAuditorLicense);

  // Check whether to show login button
  useEffect(() => {
    dispatch(actions.checkShowLoginButton());
  }, [dispatch, isLoggedIn, currentRouteName]);

  // Load permissions only when user is logged in
  useEffect(() => {
    if (isLoggedIn) {
      dispatch(actions.loadPermissions());
    }
  }, [dispatch, isLoggedIn]);

  const handleLogin = () => {
    fetchUser(store);
  };

  return (
    <MenuBar
      majorMinorVersion={globalMajorMinorVersion}
      permissions={permissions}
      isWebhooksSupported={isWebhooksSupported}
      isSourceControlSupported={isSourceControlSupported}
      login={handleLogin}
      isLoggedIn={isLoggedIn}
      shouldShowLoginButton={shouldShowLoginButton}
      isCrowdIntegrationEnabled={isCrowdIntegrationEnabled}
      isWebhookConfigurationEnabled={isWebhookConfigurationEnabled}
      isProductLicenseConfigurationEnabled={isProductLicenseConfigurationEnabled}
      isLdapConfigurationEnabled={isLdapConfigurationEnabled}
      isEmailConfigurationEnabled={isEmailConfigurationEnabled}
      isProxyConfigurationEnabled={isProxyConfigurationEnabled}
      isSystemNoticeConfigurationEnabled={isSystemNoticeConfigurationEnabled}
      isSuccessMetricsConfigurationEnabled={isSuccessMetricsConfigurationEnabled}
      isAutomaticApplicationConfigurationEnabled={isAutomaticApplicationConfigurationEnabled}
      isAutomaticScmConfigurationEnabled={isAutomaticScmConfigurationEnabled}
      isAdvancedSearchConfigurationEnabled={isAdvancedSearchConfigurationEnabled}
      isShowNotificationMenuEnabled={isShowNotificationMenuEnabled}
      isBaseUrlConfigurationEnabled={isBaseUrlConfigurationEnabled}
      isSamlConfigurationEnabled={isSamlConfigurationEnabled}
      isOAuth2ConfigurationEnabled={isOAuth2ConfigurationEnabled}
      isMonitoringSupported={isMonitoringSupported}
      isSsoIdpManagedBySonatype={isSsoIdpManagedBySonatype}
      isSingleTenant={isSingleTenant}
      isSbomManagerOnlyLicense={isSbomManagerOnlyLicense}
      isStandaloneDeveloper={isStandaloneDeveloper}
      isStandaloneFirewall={isStandaloneFirewall}
      isOrgsAndAppsEnabled={isOrgsAndAppsEnabled}
      isFirewallOnlyLicense={isFirewallOnlyLicense}
      isZscalerEnabled={isZscalerEnabled}
      isStandaloneSbomManager={isStandaloneSbomManager}
      hasLifecycleLicense={hasLifecycleLicense}
      hasRoutesResolved={hasRoutesResolved}
      hasAuditorLicense={hasAuditorLicense}
      userActions={userActions}
    />
  );
}

MainHeader.propTypes = {
  clmServerVersion: PropTypes.string,
};

export default MainHeader;

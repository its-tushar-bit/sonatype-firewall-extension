/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Children } from 'react';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import { faCog } from '@fortawesome/pro-solid-svg-icons';
import { NxTooltip } from '@sonatype/react-shared-components';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectProductLicense } from 'MainRoot/productFeatures/productLicenseSelectors';

import { MenuButton, MenuTitle, NavLink } from '../MenuButton/MenuButton';

export const SystemPreferencesMenu = ({
  permissions = {},
  isWebhooksSupported = false,
  isCrowdIntegrationEnabled = false,
  isWebhookConfigurationEnabled = false,
  isProductLicenseConfigurationEnabled = false,
  isLdapConfigurationEnabled = false,
  isEmailConfigurationEnabled = false,
  isProxyConfigurationEnabled = false,
  isSystemNoticeConfigurationEnabled = false,
  isSuccessMetricsConfigurationEnabled = false,
  isAutomaticApplicationConfigurationEnabled = false,
  isAutomaticScmConfigurationEnabled = false,
  isAdvancedSearchConfigurationEnabled = false,
  isBaseUrlConfigurationEnabled = false,
  isSamlConfigurationEnabled = false,
  isMonitoringSupported = false,
  isSsoIdpManagedBySonatype = false,
  isSingleTenant = false,
  isSbomManagerOnlyLicense = false,
  isStandaloneFirewall = false,
  isOrgsAndAppsEnabled = false,
  isFirewallOnlyLicense = false,
}) => {
  const {
    CONFIGURE_SYSTEM = false,
    VIEW_ROLES = false,
    MANAGE_AUTOMATIC_APPLICATION_CREATION = false,
    MANAGE_AUTOMATIC_SCM_CONFIGURATION = false,
  } = permissions;

  const isSbomManager = useSelector(selectIsSbomManager);
  const productLicense = useSelector(selectProductLicense);
  const firewallPrefix = isFirewallOnlyLicense ? 'firewall' : '';
  const sbomManagerPrefix = isSbomManager ? 'sbomManager' : '';

  return (
    <MenuButton icon={faCog} iconLabel="System Preferences" id="system-configuration-menu">
      <MenuTitle>System Preferences</MenuTitle>
      <NavLink
        stateName="users"
        id="system-configuration-users"
        showIf={CONFIGURE_SYSTEM && (isSingleTenant || isSsoIdpManagedBySonatype) && productLicense}
        prefix={firewallPrefix}
      >
        Users
      </NavLink>
      <NavLink
        stateName="rolesList"
        id="system-configuration-roles"
        showIf={VIEW_ROLES && productLicense}
        prefix={firewallPrefix}
      >
        Roles
      </NavLink>
      <NavLink
        stateName="administrators"
        id="system-configuration-administrators"
        showIf={CONFIGURE_SYSTEM && productLicense}
        prefix={firewallPrefix}
      >
        Administrators
      </NavLink>
      <NavLink
        stateName="productlicense"
        id="system-configuration-product-license"
        showIf={CONFIGURE_SYSTEM && (isProductLicenseConfigurationEnabled || !productLicense)}
        prefix={firewallPrefix}
      >
        Product License
      </NavLink>
      <NavLink
        stateName="ldap-list"
        id="system-configuration-ldap"
        showIf={CONFIGURE_SYSTEM && isLdapConfigurationEnabled}
        prefix={firewallPrefix}
      >
        LDAP
      </NavLink>
      <NavLink
        stateName="saml"
        id="system-configuration-saml"
        showIf={CONFIGURE_SYSTEM && isSamlConfigurationEnabled}
        prefix={firewallPrefix}
      >
        SAML
      </NavLink>
      <NavLink
        stateName="waivedComponentUpgradesConfiguration"
        id="system-configuration-waived-component-upgrades"
        showIf={CONFIGURE_SYSTEM && isMonitoringSupported && !isSbomManagerOnlyLicense}
        prefix={firewallPrefix}
      >
        Waived Components
      </NavLink>
      <NavLink
        stateName="atlassianCrowdConfiguration"
        showIf={CONFIGURE_SYSTEM && isCrowdIntegrationEnabled && !isSbomManagerOnlyLicense}
        prefix={firewallPrefix}
      >
        Atlassian Crowd
      </NavLink>
      <NavLink
        stateName="mailConfig"
        id="system-configuration-email"
        showIf={CONFIGURE_SYSTEM && isEmailConfigurationEnabled}
        prefix={firewallPrefix || sbomManagerPrefix}
      >
        Email
      </NavLink>
      <NavLink
        stateName="proxyConfig"
        id="system-configuration-proxy"
        showIf={CONFIGURE_SYSTEM && isProxyConfigurationEnabled}
        prefix={firewallPrefix}
      >
        Proxy
      </NavLink>
      {CONFIGURE_SYSTEM && (
        <NxTooltip title={isWebhooksSupported ? undefined : 'Webhooks feature is not supported by your license'}>
          <span>
            <NavLink
              stateName="listWebhooks"
              id="system-configuration-webhooks"
              disabled={!isWebhooksSupported}
              showIf={isWebhookConfigurationEnabled}
              prefix={firewallPrefix}
            >
              Webhooks
            </NavLink>
          </span>
        </NxTooltip>
      )}
      <NavLink
        stateName="systemNoticeConfiguration"
        id="system-configuration-system-notice"
        showIf={CONFIGURE_SYSTEM && isSystemNoticeConfigurationEnabled}
        prefix={firewallPrefix}
      >
        System Notice
      </NavLink>
      <NavLink
        stateName="successMetricsConfiguration"
        id="system-configuration-success-metrics"
        showIf={
          CONFIGURE_SYSTEM &&
          isSuccessMetricsConfigurationEnabled &&
          !isSbomManagerOnlyLicense &&
          isOrgsAndAppsEnabled &&
          !isStandaloneFirewall &&
          !isFirewallOnlyLicense
        }
      >
        Success Metrics
      </NavLink>
      <NavLink
        stateName="automaticApplicationsConfiguration"
        id="system-configuration-automatic-applications"
        showIf={
          MANAGE_AUTOMATIC_APPLICATION_CREATION &&
          isAutomaticApplicationConfigurationEnabled &&
          !isSbomManagerOnlyLicense &&
          !isStandaloneFirewall &&
          !isFirewallOnlyLicense
        }
      >
        Automatic Applications
      </NavLink>
      <NavLink
        stateName="automaticSourceControlConfiguration"
        id="system-configuration-automatic-scm-configuration"
        showIf={
          MANAGE_AUTOMATIC_SCM_CONFIGURATION &&
          isAutomaticScmConfigurationEnabled &&
          !isSbomManagerOnlyLicense &&
          !isStandaloneFirewall &&
          !isFirewallOnlyLicense
        }
      >
        Automatic SCM Configuration
      </NavLink>
      <NavLink
        stateName="baseUrlConfiguration"
        id="system-configuration-base-url"
        showIf={CONFIGURE_SYSTEM && isBaseUrlConfigurationEnabled}
        prefix={firewallPrefix || sbomManagerPrefix}
      >
        Base URL
      </NavLink>
      <NavLink
        stateName="advancedSearchConfig"
        id="system-configuration-advanced-search"
        showIf={
          CONFIGURE_SYSTEM &&
          isAdvancedSearchConfigurationEnabled &&
          !isSbomManagerOnlyLicense &&
          !isStandaloneFirewall &&
          !isFirewallOnlyLicense
        }
      >
        Advanced Search
      </NavLink>
      {/* Will be enabled in: NEXUS-46126
      {CONFIGURE_SYSTEM && (
        <NavLink
          stateName="roiConfiguration"
          id="system-configuration-roi-configuration"
          showIf={hasLifecycleLicense || hasFirewallLicense}
          prefix={firewallPrefix}
        >
          ROI Configuration
        </NavLink>
      )}
      */}
      <EarlyAccessLinks></EarlyAccessLinks>
    </MenuButton>
  );
};

SystemPreferencesMenu.propTypes = {
  permissions: PropTypes.shape({
    CONFIGURE_SYSTEM: PropTypes.bool,
    MANAGE_PROPRIETARY: PropTypes.bool,
    VIEW_ROLES: PropTypes.bool,
    MANAGE_AUTOMATIC_APPLICATION_CREATION: PropTypes.bool,
    MANAGE_AUTOMATIC_SCM_CONFIGURATION: PropTypes.bool,
  }),
  isWebhooksSupported: PropTypes.bool,
  isSourceControlSupported: PropTypes.bool,
  isCrowdIntegrationEnabled: PropTypes.bool,
  isWebhookConfigurationEnabled: PropTypes.bool,
  isProductLicenseConfigurationEnabled: PropTypes.bool,
  isLdapConfigurationEnabled: PropTypes.bool,
  isEmailConfigurationEnabled: PropTypes.bool,
  isProxyConfigurationEnabled: PropTypes.bool,
  isSystemNoticeConfigurationEnabled: PropTypes.bool,
  isSuccessMetricsConfigurationEnabled: PropTypes.bool,
  isAutomaticApplicationConfigurationEnabled: PropTypes.bool,
  isAutomaticScmConfigurationEnabled: PropTypes.bool,
  isAdvancedSearchConfigurationEnabled: PropTypes.bool,
  isBaseUrlConfigurationEnabled: PropTypes.bool,
  isMonitoringSupported: PropTypes.bool,
  isSamlConfigurationEnabled: PropTypes.bool,
  isSsoIdpManagedBySonatype: PropTypes.bool,
  isSingleTenant: PropTypes.bool,
  isSbomManagerOnlyLicense: PropTypes.bool,
  isFirewallLicense: PropTypes.bool,
  isorgsAndAppsEnabled: PropTypes.bool,
  isStandaloneFirewall: PropTypes.bool,
  isFirewallOnlyLicense: PropTypes.bool,
};

const EarlyAccessLinks = ({ children }) => {
  if (Children.count(children) === 0) {
    return null;
  }
  return (
    <div id="early-access-header">
      <hr />
      <h5>Early Access</h5>
      {children}
    </div>
  );
};

EarlyAccessLinks.propTypes = {
  children: PropTypes.node,
};

export default SystemPreferencesMenu;

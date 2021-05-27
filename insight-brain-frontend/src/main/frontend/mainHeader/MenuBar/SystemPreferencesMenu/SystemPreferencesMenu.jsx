/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Children } from 'react';
import PropTypes from 'prop-types';
import { faCog } from '@fortawesome/pro-solid-svg-icons';
import { NxTooltip } from '@sonatype/react-shared-components';
import { MenuButton, MenuTitle, NavLink } from '../MenuButton/MenuButton';

export const SystemPreferencesMenu = ({
  permissions = {},
  isWebhooksSupported = false,
  dataInsightsEnabled = false,
}) => {
  const {
    CONFIGURE_SYSTEM = false,
    VIEW_ROLES = false,
    MANAGE_AUTOMATIC_APPLICATION_CREATION = false,
    MANAGE_AUTOMATIC_SCM_CONFIGURATION = false,
  } = permissions;

  return (
    <MenuButton icon={faCog} iconLabel="System Preferences" id="system-configuration-menu">
      <MenuTitle>System Preferences</MenuTitle>
      <NavLink stateName="users" id="system-configuration-users" showIf={CONFIGURE_SYSTEM}>
        Users
      </NavLink>
      <NavLink stateName="rolesList" id="system-configuration-roles" showIf={VIEW_ROLES}>
        Roles
      </NavLink>
      <NavLink stateName="administrators" id="system-configuration-administrators" showIf={CONFIGURE_SYSTEM}>
        Administrators
      </NavLink>
      <NavLink stateName="productlicense" id="system-configuration-product-license" showIf={CONFIGURE_SYSTEM}>
        Product License
      </NavLink>
      <NavLink stateName="ldap-servers" id="system-configuration-ldap" showIf={CONFIGURE_SYSTEM}>
        LDAP
      </NavLink>
      <NavLink stateName="saml" showIf={CONFIGURE_SYSTEM}>
        SAML
      </NavLink>
      <NavLink stateName="mailConfig" id="system-configuration-email" showIf={CONFIGURE_SYSTEM}>
        Email
      </NavLink>
      <NavLink stateName="proxyConfig" id="system-configuration-proxy" showIf={CONFIGURE_SYSTEM}>
        Proxy
      </NavLink>
      {CONFIGURE_SYSTEM && (
        <NxTooltip title={isWebhooksSupported ? undefined : 'Webhooks feature is not supported by your license'}>
          <span>
            <NavLink stateName="webhooks.list" id="system-configuration-webhooks" disabled={!isWebhooksSupported}>
              Webhooks
            </NavLink>
          </span>
        </NxTooltip>
      )}
      <NavLink stateName="systemNoticeConfiguration" id="system-configuration-system-notice" showIf={CONFIGURE_SYSTEM}>
        System Notice
      </NavLink>
      <NavLink
        stateName="successMetricsConfiguration"
        id="system-configuration-success-metrics"
        showIf={CONFIGURE_SYSTEM}
      >
        Success Metrics
      </NavLink>
      <NavLink
        stateName="automaticApplicationsConfiguration"
        id="system-configuration-automatic-applications"
        showIf={MANAGE_AUTOMATIC_APPLICATION_CREATION}
      >
        Automatic Applications
      </NavLink>
      <NavLink stateName="automaticSourceControlConfiguration" showIf={MANAGE_AUTOMATIC_SCM_CONFIGURATION}>
        Automatic SCM Configuration
      </NavLink>
      <NavLink
        stateName="scmOnboarding"
        id="system-configuration-scm-onboarding"
        showIf={MANAGE_AUTOMATIC_SCM_CONFIGURATION}
      >
        SCM Onboarding
      </NavLink>
      <NavLink stateName="advancedSearchConfig" id="system-configuration-advanced-search" showIf={CONFIGURE_SYSTEM}>
        Advanced Search
      </NavLink>

      <NavLink stateName="dataInsights" id="system-labs-data-insights" showIf={dataInsightsEnabled}>
        Data Insights
      </NavLink>
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
  dataInsightsEnabled: PropTypes.bool,
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

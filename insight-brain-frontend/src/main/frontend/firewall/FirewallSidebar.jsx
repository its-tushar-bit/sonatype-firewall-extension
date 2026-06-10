/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxGlobalSidebar2, useToggle, NxGlobalSidebar2NavigationLink } from '@sonatype/react-shared-components';
import {
  faArrowToLeft,
  faArrowToRight,
  faStars,
  faHouse,
  faSitemap,
  faMicroscope,
  faChartPie,
  faFileCheck,
} from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';

export default function FirewallSidebar(props) {
  const { currentState, isLoggedIn, isApiPageEnabled, isFirewallEnterpriseReportingEnabled } = props;
  const uiRouterState = useRouterState();
  const firewallState = 'firewall.firewallPage';
  const firewallRepositoriesState = 'firewall.management.view';
  const apiState = 'firewall.api';
  const vulnSearchState = 'firewall.vulnerabilitySearch';
  const vulnSearchDetailState = 'firewall.vulnerabilitySearchDetail';
  const enterpriseReportingState = 'firewall.enterpriseReporting';
  const firewallWaiversState = 'firewall.waivers';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const firewallHref = uiRouterState.href(firewallState);
  const firewallRepositoriesHref = uiRouterState.href(firewallRepositoriesState);
  const apiHref = uiRouterState.href(apiState);
  const vulnSearchHref = uiRouterState.href(vulnSearchState);
  const enterpriseReportingHref = uiRouterState.href(enterpriseReportingState);
  const firewallWaiversHref = uiRouterState.href(firewallWaiversState);

  // Use currentState from Redux props (triggers re-render) instead of uiRouterState.includes()
  // which may return stale data when the re-render is driven by Redux state changes
  const currentStateName = currentState?.name || '';
  const isSelected = (entryName) => {
    return currentStateName === entryName || currentStateName.startsWith(entryName + '.');
  };

  return (
    <NxGlobalSidebar2
      isOpen={sidebarOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faArrowToRight}
      onToggleClick={onToggleCollapse}
      className="iq-firewall-sidebar"
    >
      {isLoggedIn && (
        <>
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(firewallState)}
            id="sonatype-firewall-dashboard-navigation-button"
            icon={faHouse}
            text="Dashboard"
            href={firewallHref}
          />
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(firewallRepositoriesState)}
            id="sonatype-firewall-repositories-navigation-button"
            icon={faSitemap}
            text="Repos and Policies"
            href={firewallRepositoriesHref}
          />
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(vulnSearchState) || isSelected(vulnSearchDetailState)}
            id="vulnerability-navigation-button"
            icon={faMicroscope}
            text="Vulnerability Lookup"
            href={vulnSearchHref}
          />
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(firewallWaiversState)}
            id="sonatype-firewall-waivers-navigation-button"
            icon={faFileCheck}
            text={
              <>
                <span>Waivers</span>
                <span className="iq-api-nav-link__navigation-badge">
                  {/* The space and parens should be in the tooltip but not visibly in the link text itself */}
                  <span className="iq-api-nav-link__tooltip-only-text"> (</span>
                  NEW
                  <span className="iq-api-nav-link__tooltip-only-text">)</span>
                </span>
              </>
            }
            href={firewallWaiversHref}
          />
          {isFirewallEnterpriseReportingEnabled && (
            <NxGlobalSidebar2NavigationLink
              isSelected={isSelected(enterpriseReportingState)}
              id="enterprise-reporting-button"
              className="iq-enterprise-reporting-nav-link"
              icon={faChartPie}
              text="Enterprise Reporting"
              href={enterpriseReportingHref}
            />
          )}
          {isApiPageEnabled && (
            <NxGlobalSidebar2NavigationLink
              isSelected={isSelected(apiState)}
              id="sonatype-firewall-api-navigation-button"
              className="iq-api-nav-link"
              icon={faStars}
              text={
                <>
                  <span>API</span>
                </>
              }
              href={apiHref}
            />
          )}
        </>
      )}
    </NxGlobalSidebar2>
  );
}

FirewallSidebar.propTypes = {
  currentState: PropTypes.object,
  isLoggedIn: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
  isFirewallEnterpriseReportingEnabled: PropTypes.bool,
};

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxGlobalSidebar2, useToggle, NxGlobalSidebar2NavigationLink } from '@sonatype/react-shared-components';
import { faArrowToLeft, faArrowToRight, faStars, faHouse, faSitemap } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';

export default function FirewallSidebar(props) {
  const { isLoggedIn, isApiPageEnabled } = props;
  const uiRouterState = useRouterState();
  const firewallState = 'firewall.firewallPage';
  const firewallRepositoriesState = 'firewall.management.view';
  const apiState = 'firewall.api';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const firewallHref = uiRouterState.href(firewallState);
  const firewallRepositoriesHref = uiRouterState.href(firewallRepositoriesState);
  const apiHref = uiRouterState.href(apiState);

  const isSelected = (entryName) => uiRouterState.includes(entryName);

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
          {isApiPageEnabled && (
            <NxGlobalSidebar2NavigationLink
              isSelected={isSelected(apiState)}
              id="sonatype-firewall-api-navigation-button"
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
        </>
      )}
    </NxGlobalSidebar2>
  );
}

FirewallSidebar.propTypes = {
  isLoggedIn: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
};

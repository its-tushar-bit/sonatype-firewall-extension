/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxGlobalSidebar,
  NxGlobalSidebarNavigation,
  useToggle,
  NxGlobalSidebarNavigationLink,
} from '@sonatype/react-shared-components';
import { faFileChartLine, faHome, faSitemap } from '@fortawesome/pro-solid-svg-icons';
import { faArrowToLeft, faBars } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';
import IqSidebarNavFooter from 'MainRoot/react/iqSidebarNav/IqSidebarNavFooter';

const logoImg = require('../img/nexus_firewall.svg');

export default function FirewallSidebar(props) {
  const { isLoggedIn } = props;
  const uiRouterState = useRouterState();
  const firewallState = 'firewall.firewallPage';
  const firewallRepositoriesState = 'firewall.management.view';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const firewallHref = uiRouterState.href(firewallState);
  const firewallRepositoriesHref = uiRouterState.href(firewallRepositoriesState);

  const isSelected = (entryName) => uiRouterState.includes(entryName);

  return (
    <NxGlobalSidebar
      isOpen={sidebarOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faBars}
      onToggleClick={onToggleCollapse}
      logoImg={logoImg}
      logoAltText="sonatype firewall"
      logoLink={firewallHref}
    >
      {isLoggedIn && (
        <NxGlobalSidebarNavigation>
          <NxGlobalSidebarNavigationLink
            isSelected={isSelected(firewallState)}
            id="sonatype-firewall-dashboard-navigation-button"
            icon={faHome}
            text="Dashboard"
            href={firewallHref}
          />
          <NxGlobalSidebarNavigationLink
            isSelected={isSelected(firewallRepositoriesState)}
            id="sonatype-firewall-repositories-navigation-button"
            icon={faSitemap}
            text="Repos and Policies"
            href={firewallRepositoriesHref}
          />
        </NxGlobalSidebarNavigation>
      )}
      <IqSidebarNavFooter />
    </NxGlobalSidebar>
  );
}

FirewallSidebar.propTypes = {
  isLoggedIn: PropTypes.bool,
};

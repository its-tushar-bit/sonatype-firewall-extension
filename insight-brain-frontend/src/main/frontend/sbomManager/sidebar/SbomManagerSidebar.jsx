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
import { faHome, faSitemap, faSearch } from '@fortawesome/pro-solid-svg-icons';
import { faArrowToLeft, faBars } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';
import IqSidebarNavFooter from 'MainRoot/react/iqSidebarNav/IqSidebarNavFooter';

const logoImg = require('../assets/sbom-manager.svg');

export default function SbomManagerSidebar(props) {
  const { isLoggedIn, isSbomManagerEnabled } = props;
  const uiRouterState = useRouterState();
  const dashboardState = 'sbomManager.dashboard';
  const sbomManagerOrgsState = 'sbomManager.management.view';
  const advancedSearchState = 'sbomManager.advancedSearch';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const dashboardHref = uiRouterState.href(dashboardState);
  const sbomManagerOrgsHref = uiRouterState.href(sbomManagerOrgsState);
  const advancedSearchHref = uiRouterState.href(advancedSearchState);

  const isSelected = (entryName) => uiRouterState.includes(entryName);

  return (
    <NxGlobalSidebar
      isOpen={sidebarOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faBars}
      onToggleClick={onToggleCollapse}
      logoImg={logoImg}
      logoAltText="sonatype sbom manager"
      logoLink={dashboardHref}
    >
      {isLoggedIn && isSbomManagerEnabled && (
        <NxGlobalSidebarNavigation>
          <NxGlobalSidebarNavigationLink
            isSelected={isSelected(dashboardState)}
            id="sbom-manager-dashboard-navigation-button"
            icon={faHome}
            text="Dashboard"
            href={dashboardHref}
          />
          <NxGlobalSidebarNavigationLink
            isSelected={isSelected(sbomManagerOrgsState)}
            id="sbom-manager-organizations-navigation-button"
            icon={faSitemap}
            text="Organizations"
            href={sbomManagerOrgsHref}
          />
          <NxGlobalSidebarNavigationLink
            isSelected={isSelected(advancedSearchState)}
            id="sbom-manager-search-navigation-button"
            icon={faSearch}
            text="Advanced Search"
            href={advancedSearchHref}
          />
        </NxGlobalSidebarNavigation>
      )}
      <IqSidebarNavFooter />
    </NxGlobalSidebar>
  );
}

SbomManagerSidebar.propTypes = {
  isLoggedIn: PropTypes.bool,
  isSbomManagerEnabled: PropTypes.bool,
};

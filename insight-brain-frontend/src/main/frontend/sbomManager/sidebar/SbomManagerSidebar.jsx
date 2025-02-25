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
import { faHome, faSitemap, faSearch, faGrid2Plus } from '@fortawesome/pro-solid-svg-icons';
import { faArrowToLeft, faBars, faStars } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';
import IqSidebarNavFooter from 'MainRoot/react/iqSidebarNav/IqSidebarNavFooter';

const logoImg = require('../assets/sbom-manager.svg');

export default function SbomManagerSidebar(props) {
  const { isLoggedIn, isSbomManagerEnabled, isApiPageEnabled } = props;
  const uiRouterState = useRouterState();
  const dashboardState = 'sbomManager.dashboard';
  const applicationsState = 'sbomManager.applications';
  const sbomManagerOrgsState = 'sbomManager.management.view';
  const advancedSearchState = 'sbomManager.advancedSearch';
  const apiState = 'sbomManager.api';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const dashboardHref = uiRouterState.href(dashboardState);
  const applicationsHref = uiRouterState.href(applicationsState);
  const sbomManagerOrgsHref = uiRouterState.href(sbomManagerOrgsState);
  const advancedSearchHref = uiRouterState.href(advancedSearchState);
  const apiHref = uiRouterState.href(apiState);

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
            isSelected={isSelected(applicationsState)}
            id="sbom-manager-applications-navigation-button"
            icon={faGrid2Plus}
            text="Applications"
            href={applicationsHref}
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
          {isApiPageEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected(apiState)}
              id="sbom-manager-api-navigation-button"
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
      <IqSidebarNavFooter />
    </NxGlobalSidebar>
  );
}

SbomManagerSidebar.propTypes = {
  isLoggedIn: PropTypes.bool,
  isSbomManagerEnabled: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
};

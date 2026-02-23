/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxGlobalSidebar2, useToggle, NxGlobalSidebar2NavigationLink } from '@sonatype/react-shared-components';
import {
  faHome,
  faSitemap,
  faSearch,
  faGrid2Plus,
  faGavel,
  faArrowToLeft,
  faArrowToRight,
  faStars,
} from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';

export default function SbomManagerSidebar(props) {
  const {
    currentState,
    isLoggedIn,
    isSbomManagerEnabled,
    isApiPageEnabled,
    isAlpForSbomManagerEnabled,
    isLegalEnabled,
  } = props;
  const uiRouterState = useRouterState();
  const dashboardState = 'sbomManager.dashboard';
  const applicationsState = 'sbomManager.applications';
  const sbomManagerOrgsState = 'sbomManager.management.view';
  const advancedSearchState = 'sbomManager.advancedSearch';
  const sbomManagerLegalState = 'sbomManager.legal.dashboard';
  const apiState = 'sbomManager.api';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const dashboardHref = uiRouterState.href(dashboardState);
  const applicationsHref = uiRouterState.href(applicationsState);
  const sbomManagerOrgsHref = uiRouterState.href(sbomManagerOrgsState);
  const advancedSearchHref = uiRouterState.href(advancedSearchState);
  const legalHref = uiRouterState.href(sbomManagerLegalState);
  const apiHref = uiRouterState.href(apiState);

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
      className="iq-sbom-manager-sidebar"
    >
      {isLoggedIn && isSbomManagerEnabled && (
        <>
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(dashboardState)}
            id="sbom-manager-dashboard-navigation-button"
            icon={faHome}
            text="Dashboard"
            href={dashboardHref}
          />
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(applicationsState)}
            id="sbom-manager-applications-navigation-button"
            icon={faGrid2Plus}
            text="Applications"
            href={applicationsHref}
          />
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(sbomManagerOrgsState)}
            id="sbom-manager-organizations-navigation-button"
            icon={faSitemap}
            text="Organizations"
            href={sbomManagerOrgsHref}
          />
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(advancedSearchState)}
            id="sbom-manager-search-navigation-button"
            icon={faSearch}
            text="Advanced Search"
            href={advancedSearchHref}
          />
          {isLegalEnabled && isAlpForSbomManagerEnabled && (
            <NxGlobalSidebar2NavigationLink
              isSelected={isSelected('sbomManager.legal')}
              id="sbom-manager-legal-button"
              icon={faGavel}
              text="Legal"
              href={legalHref}
            />
          )}
          {isApiPageEnabled && (
            <NxGlobalSidebar2NavigationLink
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
        </>
      )}
    </NxGlobalSidebar2>
  );
}

SbomManagerSidebar.propTypes = {
  currentState: PropTypes.object,
  isLoggedIn: PropTypes.bool,
  isSbomManagerEnabled: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
  isAlpForSbomManagerEnabled: PropTypes.bool,
  isLegalEnabled: PropTypes.bool,
};

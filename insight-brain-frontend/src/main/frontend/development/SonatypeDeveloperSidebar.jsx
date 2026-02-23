/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxGlobalSidebar2, useToggle, NxGlobalSidebar2NavigationLink } from '@sonatype/react-shared-components';
import {
  faHouse,
  faStars,
  faArrowToLeft,
  faArrowToRight,
  faListOl,
  faPlug,
  faMagnifyingGlass,
} from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';

export default function SonatypeDeveloperSidebar(props) {
  const { currentState, isLoggedIn, isAdvancedSearchEnabled, isApiPageEnabled } = props;
  const uiRouterState = useRouterState();
  const dashboardState = 'developer.dashboard';
  const prioritiesState = 'developer.priorities';
  const advancedSearchState = 'developer.advancedSearch';
  const apiState = 'developer.api';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const dashboardHref = uiRouterState.href(dashboardState);
  const prioritiesHref = uiRouterState.href(prioritiesState);
  const advancedSearchHref = uiRouterState.href(advancedSearchState);
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
      className="iq-developer-sidebar"
    >
      {isLoggedIn && (
        <>
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(dashboardState)}
            id="sonatype-developer-dashboard-navigation-button"
            icon={faHouse}
            text="Dashboard"
            href={dashboardHref}
          />
          <NxGlobalSidebar2NavigationLink
            isSelected={isSelected(prioritiesState)}
            id="sonatype-developer-reports-navigation-button"
            icon={faListOl}
            text="Priorities"
            href={prioritiesHref}
          />
          {isAdvancedSearchEnabled && (
            <NxGlobalSidebar2NavigationLink
              isSelected={isSelected(advancedSearchState)}
              id="sonatype-developer-search-navigation-button"
              icon={faMagnifyingGlass}
              text="Advanced Search"
              href={advancedSearchHref}
            />
          )}
          <NxGlobalSidebar2NavigationLink
            id="sonatype-developer-integrations-help-navigation-button"
            icon={faPlug}
            text="Integrations Help"
            href="https://links.sonatype.com/products/nxiq/doc/iq-server-integrations"
            target="_blank"
          />
          {isApiPageEnabled && (
            <NxGlobalSidebar2NavigationLink
              isSelected={isSelected(apiState)}
              id="sonatype-developer-api-navigation-button"
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

SonatypeDeveloperSidebar.propTypes = {
  currentState: PropTypes.object,
  isLoggedIn: PropTypes.bool,
  isAdvancedSearchEnabled: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
};

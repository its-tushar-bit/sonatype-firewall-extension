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
import { faHome, faFileChartLine } from '@fortawesome/pro-solid-svg-icons';
import { faArrowToLeft, faBars, faSearch, faStars } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';
import IqSidebarNavFooter from 'MainRoot/react/iqSidebarNav/IqSidebarNavFooter';

const logoImg = require('./assets/sonatype-developer-logo-white.svg');

export default function SonatypeDeveloperSidebar(props) {
  const { isLoggedIn, isAdvancedSearchEnabled, isApiPageEnabled } = props;
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

  const isSelected = (entryName) => uiRouterState.includes(entryName);

  return (
    <NxGlobalSidebar
      isOpen={sidebarOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faBars}
      onToggleClick={onToggleCollapse}
      logoImg={logoImg}
      logoAltText="sonatype developer"
      logoLink={dashboardHref}
    >
      {isLoggedIn && (
        <NxGlobalSidebarNavigation>
          <NxGlobalSidebarNavigationLink
            isSelected={isSelected(dashboardState)}
            id="sonatype-developer-dashboard-navigation-button"
            icon={faHome}
            text="Dashboard"
            href={dashboardHref}
          />
          <NxGlobalSidebarNavigationLink
            isSelected={isSelected(prioritiesState)}
            id="sonatype-developer-reports-navigation-button"
            icon={faFileChartLine}
            text="Priorities"
            href={prioritiesHref}
          />
          {isAdvancedSearchEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected(advancedSearchState)}
              id="sonatype-developer-search-navigation-button"
              icon={faSearch}
              text="Advanced Search"
              href={advancedSearchHref}
            />
          )}
          {isApiPageEnabled && (
            <NxGlobalSidebarNavigationLink
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
        </NxGlobalSidebarNavigation>
      )}
      <IqSidebarNavFooter />
    </NxGlobalSidebar>
  );
}

SonatypeDeveloperSidebar.propTypes = {
  isLoggedIn: PropTypes.bool,
  isAdvancedSearchEnabled: PropTypes.bool,
  isApiPageEnabled: PropTypes.bool,
};

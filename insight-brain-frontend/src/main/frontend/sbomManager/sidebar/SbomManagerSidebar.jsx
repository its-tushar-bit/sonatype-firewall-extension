/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useState } from 'react';
import {
  NxGlobalSidebar,
  NxGlobalSidebarNavigation,
  useToggle,
  NxGlobalSidebarNavigationLink,
} from '@sonatype/react-shared-components';
import { faHome } from '@fortawesome/pro-solid-svg-icons';
import { faArrowToLeft, faBars } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';

const logoImg = require('../assets/sbom-manager.svg');

export default function SbomManagerSidebar(props) {
  const { isLoggedIn, isSbomManagerEnabled } = props;
  const uiRouterState = useRouterState();
  const currentStateName = useCurrentStateName(uiRouterState.router);
  const dashboardState = 'sbomManager.dashboard';

  const [sidebarOpen, onToggleCollapse] = useToggle(true);

  const dashboardHref = uiRouterState.href(dashboardState);
  const sbomManagerHref = uiRouterState.href('sbomManager');

  const isSelected = (entryName) => {
    return currentStateName === entryName;
  };

  function useCurrentStateName(router) {
    const [currentStateName, setCurrentStateName] = useState(router.globals.current.name);

    useEffect(() => {
      const deregister = router.transitionService.onSuccess({}, (transition) => {
        setCurrentStateName(transition.to().name);
      });

      return () => deregister();
    }, [router]);

    return currentStateName;
  }

  return (
    <NxGlobalSidebar
      isOpen={sidebarOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faBars}
      onToggleClick={onToggleCollapse}
      logoImg={logoImg}
      logoAltText="sonatype sbom manager"
      logoLink={sbomManagerHref}
    >
      {isLoggedIn && (
        <NxGlobalSidebarNavigation>
          {isSbomManagerEnabled && (
            <NxGlobalSidebarNavigationLink
              isSelected={isSelected(dashboardState)}
              id="sbom-manager-dashboard-navigation-button"
              icon={faHome}
              text="Dashboard"
              href={dashboardHref}
            />
          )}
        </NxGlobalSidebarNavigation>
      )}
    </NxGlobalSidebar>
  );
}

SbomManagerSidebar.propTypes = {
  isLoggedIn: PropTypes.bool,
  isSbomManagerEnabled: PropTypes.bool,
};

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { faQuestionCircle } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectIsSbomManager, selectIsStandaloneDeveloper } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';
import {
  NxH4,
  NxNavigationDropdown,
  NxStatefulNavigationDropdown,
  NxTextLink,
} from '@sonatype/react-shared-components';

export const HelpMenu = ({ majorMinorVersion = '' }) => {
  const uiRouterState = useRouterState();

  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const isSbomManager = useSelector(selectIsSbomManager);

  const gettingStartedUrl = uiRouterState.href('gettingStarted');

  const getHelpUrl = () => {
    if (isSbomManager) {
      return 'http://links.sonatype.com/products/sbom/doc';
    }

    if (isStandaloneDeveloper) {
      return 'http://links.sonatype.com/products/nxiq/doc/sonatype-developer';
    }

    return `http://links.sonatype.com/products/clm/doc/${majorMinorVersion}`;
  };

  return (
    <NxStatefulNavigationDropdown icon={faQuestionCircle} title="Support Options" id="help-menu-dropdown">
      <NxNavigationDropdown.MenuHeader>
        <NxH4>Support Options</NxH4>
      </NxNavigationDropdown.MenuHeader>
      {!isStandaloneDeveloper && (
        <NxTextLink id="getting-started-link" href={gettingStartedUrl} className="nx-dropdown-link">
          Getting Started
        </NxTextLink>
      )}

      <NxTextLink id="documentation-link" href={getHelpUrl()} newTab className="nx-dropdown-link">
        Online Help
      </NxTextLink>

      <NxTextLink
        id="support-link"
        href="http://links.sonatype.com/products/clm/support"
        newTab
        className="nx-dropdown-link"
      >
        Request Support
      </NxTextLink>
    </NxStatefulNavigationDropdown>
  );
};

HelpMenu.propTypes = {
  majorMinorVersion: PropTypes.string,
};

export default HelpMenu;

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { faQuestionCircle } from '@fortawesome/pro-solid-svg-icons';
import { MenuButton, MenuTitle, NavLink } from '../MenuButton/MenuButton';
import { selectIsSbomManager, selectIsStandaloneDeveloper } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';

export const HelpMenu = ({ majorMinorVersion = '' }) => {
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const isSbomManager = useSelector(selectIsSbomManager);

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
    <MenuButton icon={faQuestionCircle} iconLabel="Support Options" id="help-menu-dropdown">
      <MenuTitle>Support Options</MenuTitle>

      {!isStandaloneDeveloper && (
        <NavLink id="getting-started-link" stateName="gettingStarted" title="Getting Started">
          Getting Started
        </NavLink>
      )}

      <NavLink id="documentation-link" href={getHelpUrl()} title="Go to Online Help" openInNewTab>
        Online Help
      </NavLink>

      <NavLink
        id="support-link"
        href="http://links.sonatype.com/products/clm/support"
        title="Submit a request to Sonatype support"
        openInNewTab
      >
        Request Support
      </NavLink>
    </MenuButton>
  );
};

HelpMenu.propTypes = {
  majorMinorVersion: PropTypes.string,
};

export default HelpMenu;

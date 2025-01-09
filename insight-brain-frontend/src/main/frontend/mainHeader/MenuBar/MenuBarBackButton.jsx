/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import * as ReactDOM from 'react-dom';
import * as PropTypes from 'prop-types';
import { NxBackButton } from '@sonatype/react-shared-components';
import { pathOr } from 'ramda';

import { useRouterState } from '../../react/RouterStateContext';

const BACK_BUTTON_CONTAINER_NODE_ID = 'menu-bar__back-button-container';

const MenuBarBackButton = ({ stateName, text, href }) => {
  // Portal configuration
  const container = document.getElementById(BACK_BUTTON_CONTAINER_NODE_ID);
  const containerRef = useRef(null);
  containerRef.current = containerRef.current || container;

  // Back button rendering and routing
  const uiRouterState = useRouterState();
  const resolvedStateName =
    stateName && !stateName.includes('firewall') && uiRouterState.includes('firewall')
      ? `firewall.${stateName}`
      : stateName;
  const targetState = resolvedStateName ? uiRouterState.get(resolvedStateName) : null;
  const hrefToNavigate = href || (targetState && uiRouterState.href(targetState));
  const targetPageTitle = pathOr(null, ['data', 'title'], targetState);

  const renderComponentInsidePortal = (componentToRender) =>
    containerRef.current && ReactDOM.createPortal(componentToRender, containerRef.current);

  return hrefToNavigate
    ? renderComponentInsidePortal(<NxBackButton href={hrefToNavigate} text={text} targetPageTitle={targetPageTitle} />)
    : null;
};

MenuBarBackButton.propTypes = {
  stateName: PropTypes.string,
  text: PropTypes.string,
  href: PropTypes.string,
};

export default MenuBarBackButton;

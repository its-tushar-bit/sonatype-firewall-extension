/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton } from '@sonatype/react-shared-components';
import { pathOr } from 'ramda';

import { useRouterState } from '../../react/RouterStateContext';

const MenuBarBackButton = ({ stateName, text, href }) => {
  const uiRouterState = useRouterState();

  // Back button rendering and routing
  const resolvedStateName =
    stateName && !stateName.includes('firewall') && uiRouterState.includes('firewall')
      ? `firewall.${stateName}`
      : stateName;
  const targetState = resolvedStateName ? uiRouterState.get(resolvedStateName) : null;
  const hrefToNavigate = href || (targetState && uiRouterState.href(targetState));
  const targetPageTitle = pathOr(null, ['data', 'title'], targetState);

  return hrefToNavigate ? <NxBackButton href={hrefToNavigate} text={text} targetPageTitle={targetPageTitle} /> : null;
};

MenuBarBackButton.propTypes = {
  stateName: PropTypes.string,
  text: PropTypes.string,
  href: PropTypes.string,
};

export default MenuBarBackButton;

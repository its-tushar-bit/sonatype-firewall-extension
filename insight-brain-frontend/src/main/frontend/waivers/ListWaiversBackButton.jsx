/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';

import { useRouterState } from '../react/RouterStateContext';

export default function ListWaiversBackButton(props) {
  const {
    violationId,
    sidebarReference,
    type,
    hash,
    scanId,
    publicId,
    previousRouterStateNameForComponentDetails,
  } = props;

  const componentDetailsPropsPresent = hash && scanId && publicId && previousRouterStateNameForComponentDetails;

  const uiRouterState = useRouterState();

  const backButtonTitle = componentDetailsPropsPresent ? 'Back to Component Details' : 'Back to Violation Details';

  const backButtonHref = componentDetailsPropsPresent
    ? uiRouterState.href(previousRouterStateNameForComponentDetails, { hash, scanId, publicId })
    : uiRouterState.href('sidebarView.violation', { id: violationId, type, sidebarReference });

  return <MenuBarBackButton text={backButtonTitle} href={backButtonHref} />;
}

ListWaiversBackButton.propTypes = {
  violationId: PropTypes.string,
  sidebarReference: PropTypes.string,
  type: PropTypes.string,
  hash: PropTypes.string,
  scanId: PropTypes.string,
  publicId: PropTypes.string,
  previousRouterStateNameForComponentDetails: PropTypes.string,
};

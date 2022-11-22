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
    repositoryPolicyId,
    componentIdentifier,
    matchState,
    pathname,
  } = props;

  const componentDetailsPropsPresent = hash && scanId && publicId && previousRouterStateNameForComponentDetails;

  const uiRouterState = useRouterState();

  let backButtonTitle;
  let backButtonHref;

  if (componentDetailsPropsPresent) {
    backButtonTitle = 'Back to Component Details';
    backButtonHref = uiRouterState.href(previousRouterStateNameForComponentDetails, { hash, scanId, publicId });
  } else {
    if (previousRouterStateNameForComponentDetails?.includes('firewall.componentDetailsPage')) {
      backButtonTitle = 'Back to Component Details';
      backButtonHref = uiRouterState.href('firewall.componentDetailsPage', {
        repositoryId: repositoryPolicyId,
        componentIdentifier,
        componentHash: hash,
        matchState,
        pathname,
      });
    } else {
      backButtonTitle = 'Back to Violation Details';
      backButtonHref = uiRouterState.href('sidebarView.violation', { id: violationId, type, sidebarReference });
    }
  }

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
  pathname: PropTypes.string,
  matchState: PropTypes.string,
  componentIdentifier: PropTypes.string,
  repositoryPolicyId: PropTypes.string,
};

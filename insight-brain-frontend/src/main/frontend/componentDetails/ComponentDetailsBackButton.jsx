/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';
import { useRouterState } from '../react/RouterStateContext';

export default function ComponentDetailsBackButton(props) {
  const { scanId, publicId } = props;
  const dependencyTreePropsPresent = scanId && publicId;

  if (!dependencyTreePropsPresent) {
    return <MenuBarBackButton stateName="applicationReport.policy" />;
  }

  const text = 'Back To Dependency Tree';
  const href = useRouterState().href('applicationReport.dependencyTree', { scanId, publicId });

  return <MenuBarBackButton text={text} href={href} />;
}

ComponentDetailsBackButton.propTypes = {
  scanId: PropTypes.string,
  publicId: PropTypes.string,
};

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';
import { useRouterState } from '../react/RouterStateContext';
import { useSelector } from 'react-redux';
import { selectPreviousRouteName, selectRouterPrevParams } from '../reduxUiRouter/routerSelectors';

export default function ComponentDetailsBackButton(props) {
  const { scanId, publicId } = props;
  const dependencyTreePropsPresent = scanId && publicId;

  const prevRouteName = useSelector(selectPreviousRouteName);
  const prevParams = useSelector(selectRouterPrevParams);
  const uiRouterState = useRouterState();

  if (!dependencyTreePropsPresent) {
    if (prevRouteName && prevRouteName === 'prioritiesPage') {
      const prioritiesPageHref = uiRouterState.href('prioritiesPage', { ...prevParams });
      return <MenuBarBackButton href={prioritiesPageHref} text="Back to Priorities" />;
    }
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

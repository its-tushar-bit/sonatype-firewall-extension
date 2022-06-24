/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { RouterStateProvider, routerPropType } from '../react/RouterStateContext';

export default function withRouterStateProvider(WrappedComponent) {
  function WithRouterStateProvider({ $state, ...props }) {
    return (
      <RouterStateProvider value={$state}>
        {/* $state prop provided explicitly for old components that receive it as a prop rather than through context */}
        <WrappedComponent {...props} $state={$state} uiRouterState={$state} />
      </RouterStateProvider>
    );
  }

  WithRouterStateProvider.displayName = `withRouterStateProvider(${getDisplayName(WrappedComponent)})`;
  WithRouterStateProvider.propTypes = {
    $state: routerPropType,
  };
  return WithRouterStateProvider;
}

withRouterStateProvider.propTypes = {
  WrappedComponent: PropTypes.func.isRequired,
};

function getDisplayName(WrappedComponent) {
  return WrappedComponent.displayName || WrappedComponent.name || 'AnonymousComponent';
}

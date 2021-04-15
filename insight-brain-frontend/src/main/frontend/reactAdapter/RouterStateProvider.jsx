/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import RouterStateContext, {
  routerPropType,
} from '../react/RouterStateContext';

export default function withRouterStateProvider(WrappedComponent) {
  function RouterStateProvider({ $state, ...props }) {
    return (
      <RouterStateContext.Provider value={$state}>
        <WrappedComponent {...props} />
      </RouterStateContext.Provider>
    );
  }

  RouterStateProvider.displayName = `withRouterStateProvider(${getDisplayName(
    WrappedComponent
  )})`;
  RouterStateProvider.propTypes = {
    $state: routerPropType,
  };
  return RouterStateProvider;
}

withRouterStateProvider.propTypes = {
  WrappedComponent: PropTypes.func.isRequired,
};

function getDisplayName(WrappedComponent) {
  return (
    WrappedComponent.displayName ||
    WrappedComponent.name ||
    'AnonymousComponent'
  );
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Provider } from 'react-redux';
import React from 'react';
import * as PropTypes from 'prop-types';
import store from 'MainRoot/reduxConfig/store';

export default function withStoreProvider(WrappedComponent) {
  function StoreProvider(props) {
    return (
      <Provider store={store}>
        <WrappedComponent {...props} />
      </Provider>
    );
  }
  StoreProvider.displayName = `withStoreProvider(${getDisplayName(WrappedComponent)})`;
  return StoreProvider;
}

withStoreProvider.propTypes = {
  WrappedComponent: PropTypes.func.isRequired,
};

function getDisplayName(WrappedComponent) {
  return WrappedComponent.displayName || WrappedComponent.name || 'AnonymousComponent';
}

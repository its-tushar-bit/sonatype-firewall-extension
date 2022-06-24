/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { identity } from 'ramda';
import { react2angular } from 'react2angular';
import withStoreProvider from './StoreProvider';
import withRouterStateProvider from './RouterStateProvider';

export default function iqReact2Angular(Component, bindings, injections) {
  const hasStore = injections?.includes('$ngRedux'),
    hasState = injections?.includes('$state'),
    _withStoreProvider = hasStore ? withStoreProvider : identity,
    _withRouterStateProvider = hasState ? withRouterStateProvider : identity,
    WrappedComponent = _withStoreProvider(_withRouterStateProvider(Component));

  return react2angular(WrappedComponent, bindings, injections);
}

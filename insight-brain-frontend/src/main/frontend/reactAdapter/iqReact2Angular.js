/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from './StoreProvider';
import withRouterStateProvider from './RouterStateProvider';

export default function iqReact2Angular(Component, bindings, injections) {
  const hasState = injections?.includes('$state');

  // Always wrap with StoreProvider (no longer conditional on $ngRedux)
  const _withRouterStateProvider = hasState ? withRouterStateProvider : (c) => c;
  const WrappedComponent = withStoreProvider(_withRouterStateProvider(Component));

  // No need to filter injections anymore since we removed all $ngRedux references
  const filteredInjections = injections;

  return react2angular(WrappedComponent, bindings, filteredInjections);
}

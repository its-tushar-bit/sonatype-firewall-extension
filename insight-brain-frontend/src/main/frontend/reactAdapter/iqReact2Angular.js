/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from './StoreProvider';

export default function iqReact2Angular(Component, bindings, injections) {
  // Always wrap with StoreProvider
  const WrappedComponent = withStoreProvider(Component);

  // Filter out $state from injections since we now use React ui-router directly via useRouterState hook
  const filteredInjections = injections?.filter((i) => i !== '$state');

  return react2angular(WrappedComponent, bindings, filteredInjections);
}

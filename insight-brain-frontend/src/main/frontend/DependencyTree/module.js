/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import DependencyTreeContainer from './DependencyTreeContainer';

const dependencyTreeModule = angular
  .module('dependencyTree', ['ui.router'])
  .component(
    'dependencyTree',
    react2angular(withStoreProvider(withRouterStateProvider(DependencyTreeContainer)), [], ['$ngRedux', '$state'])
  );

export default dependencyTreeModule;

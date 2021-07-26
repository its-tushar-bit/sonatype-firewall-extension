/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import RequestWaiversContainer from './RequestWaiversContainer';

const requestWaiversModule = angular
  .module('requestWaivers', ['ui.router'])
  .component(
    'requestWaivers',
    react2angular(withStoreProvider(withRouterStateProvider(RequestWaiversContainer)), [], ['$ngRedux', '$state'])
  );

export default requestWaiversModule;

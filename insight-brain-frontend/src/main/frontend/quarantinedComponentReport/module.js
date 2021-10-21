/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import withStoreProvider from '../reactAdapter/StoreProvider';
import QuarantinedComponentContainer from './QuarantinedComponentContainer';
import withRouterStateProvider from 'MainRoot/reactAdapter/RouterStateProvider';

export default angular
  .module('quarantinedComponentReportModule', ['ngRedux'])
  .component(
    'quarantinedComponentReport',
    react2angular(withStoreProvider(withRouterStateProvider(QuarantinedComponentContainer)), [], ['$ngRedux', '$state'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('quarantinedComponentReport', {
    component: 'quarantinedComponentReport',
    data: {
      title: 'Quarantined Component Report',
      authenticationRequired: false,
    },
    url: '/repositories/quarantinedComponent/{token}',
  });
}

routes.$inject = ['$stateProvider'];

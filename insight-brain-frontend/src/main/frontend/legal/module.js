/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import ComponentLegalOverviewContainer from './ComponentLegalOverviewContainer';
import withStoreProvider from '../reactAdapter/StoreProvider';

export default angular.module('legalModule', [])
    .component('componentLegalOverview',
        react2angular(withStoreProvider(ComponentLegalOverviewContainer), [], ['$ngRedux']))
    .config(routes);

function routes($stateProvider) {
  $stateProvider
      .state('componentLegalOverview', {
        url: '/legal/component/{hash}',
        component: 'componentLegalOverview',
        data: {
          title: 'Component - Legal Overview'
        }
      });
}

routes.$inject = ['$stateProvider'];

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import ViolationPageContainer from './ViolationPageContainer';
import withStoreProvider from '../reactAdapter/StoreProvider';

export default angular.module('violationPage', [])
    .component('violationPage', react2angular(withStoreProvider(ViolationPageContainer), [], ['$ngRedux', '$state']))
    .config(routes);

function routes($stateProvider) {
  $stateProvider
      .state('violation', {
        component: 'violationPage',
        data: {
          title: 'Policy Violation'
        },
        url: '/violation/{id}?type&sidebarReference&sidebarId'
      });
}

routes.$inject = ['$stateProvider'];

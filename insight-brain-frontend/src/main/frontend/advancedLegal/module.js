/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import AdvancedLegalApplicationsContainer from './AdvancedLegalApplicationsContainer';
import AdvancedLegalApplicationContainer from './AdvancedLegalApplicationContainer';
import withStoreProvider from '../reactAdapter/StoreProvider';

export default angular.module('advancedLegalApplications', [])
    .component('advancedLegalApplications',
        react2angular(withStoreProvider(AdvancedLegalApplicationsContainer), [], ['$ngRedux', '$state']))
    .component('advancedLegalApplication',
        react2angular(withStoreProvider(AdvancedLegalApplicationContainer), [], ['$ngRedux', '$state']))
    .config(routes);

function routes($stateProvider) {
  $stateProvider
      .state('advancedLegal', {
        component: 'advancedLegalApplications',
        data: {
          title: 'Advanced Legal Pack - Applications'
        },
        url: '/advancedLegal'
      })
      .state('advancedLegalApplication', {
        component: 'advancedLegalApplication',
        data: {
          title: 'Advanced Legal Pack - Application'
        },
        url: '/advancedLegal/application/{publicId}'
      });
}

routes.$inject = ['$stateProvider'];

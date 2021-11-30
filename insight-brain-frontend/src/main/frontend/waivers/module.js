/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import AddWaiverPageContainer from './AddWaiverPageContainer';
import RequestWaiverPageContainer from './RequestWaiverPageContainer';
import ListWaiversPageContainer from './ListWaiversPageContainer';
import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';

export default angular
  .module('waivers', [])
  .component('addWaiverPage', react2angular(withStoreProvider(AddWaiverPageContainer), [], ['$ngRedux']))
  .component(
    'requestWaiverPage',
    react2angular(withRouterStateProvider(withStoreProvider(RequestWaiverPageContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'listWaiversPage',
    react2angular(withRouterStateProvider(withStoreProvider(ListWaiversPageContainer)), [], ['$ngRedux', '$state'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('addWaiver', {
      component: 'addWaiverPage',
      data: {
        title: 'Add Waiver',
        isDirty: ['addWaiver', 'isDirty'],
      },
      url: '/addWaiver/{violationId}',
    })
    .state('requestWaiver', {
      component: 'requestWaiverPage',
      data: {
        title: 'Request Waiver',
      },
      url: '/requestWaiver/{violationId}',
    })
    .state('listWaivers', {
      component: 'listWaiversPage',
      data: {
        title: 'Waivers',
      },
      url: '/waivers/{violationId}?type&sidebarReference',
    });
}

routes.$inject = ['$stateProvider'];

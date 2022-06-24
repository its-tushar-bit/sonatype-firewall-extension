/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import AddWaiverPageContainer from './AddWaiverPageContainer';
import RequestWaiverPageContainer from './RequestWaiverPageContainer';
import ListWaiversPageContainer from './ListWaiversPageContainer';

export default angular
  .module('waivers', [])
  .component('addWaiverPage', iqReact2Angular(AddWaiverPageContainer, [], ['$ngRedux', '$state']))
  .component('requestWaiverPage', iqReact2Angular(RequestWaiverPageContainer, [], ['$ngRedux', '$state']))
  .component('listWaiversPage', iqReact2Angular(ListWaiversPageContainer, [], ['$ngRedux', '$state']))
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

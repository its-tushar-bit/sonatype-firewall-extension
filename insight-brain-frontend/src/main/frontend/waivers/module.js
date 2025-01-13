/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import AddWaiverPageContainer from './AddWaiverPageContainer';
import RequestWaiverPage from './RequestWaiverPage';
import sidebarView from 'MainRoot/sidebarNav/sidebarView';
import WaiverDetailsContainer from './waiverDetails/WaiverDetailsContainer';

export default angular
  .module('waivers', [])
  .component('addWaiverPage', iqReact2Angular(AddWaiverPageContainer, [], ['$ngRedux', '$state']))
  .component('requestWaiverPage', iqReact2Angular(RequestWaiverPage, [], ['$ngRedux', '$state']))
  .component('waiverSidebarView', sidebarView)
  .component('waiverDetailsContainer', iqReact2Angular(WaiverDetailsContainer, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('addWaiver', {
      component: 'addWaiverPage',
      data: {
        title: 'Add Waiver',
        isDirty: ['addWaiver', 'isDirty'],
      },
      url: '/addWaiver/{violationId}?comments&reasonId',
    })
    .state('requestWaiver', {
      component: 'requestWaiverPage',
      data: {
        title: 'Request Waiver',
        isDirty: ['requestWaiver', 'isDirty'],
      },
      url: '/requestWaiver/{violationId}',
    })
    .state('waiver', {
      abstract: true,
      component: 'waiverSidebarView',
      url: '/waiver',
    })
    .state('waiver.details', {
      component: 'waiverDetailsContainer',
      data: {
        title: 'Waiver detail view',
      },
      url: '/{ownerType}/{ownerId}/{waiverId}?type&sidebarReference&sidebarId&page',
    });
}

routes.$inject = ['$stateProvider'];

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ViolationPageContainer from './ViolationPageContainer';
import sidebarView from '../sidebarNav/sidebarView';
import SidebarNavListContainer from '../sidebarNav/SidebarNavListContainer';
import TransitiveViolationsPageContainer from './TransitiveViolationsPageContainer';

export default angular
  .module('violationPage', [])
  .component('sidebarView', sidebarView)
  .component('sidebarNavList', iqReact2Angular(SidebarNavListContainer, [], ['$ngRedux', '$state']))
  .component('violationPage', iqReact2Angular(ViolationPageContainer, [], ['$ngRedux', '$state']))
  .component(
    'transitiveViolationsPageContainer',
    iqReact2Angular(TransitiveViolationsPageContainer, [], ['$ngRedux', '$state'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('sidebarView', {
      abstract: true,
      component: 'sidebarView',
      url: '/violation',
    })
    .state('sidebarView.violation', {
      component: 'violationPage',
      data: {
        title: 'Policy Violation',
      },
      url: '/{id}?type&sidebarReference&sidebarId',
    })
    .state('transitiveViolations', {
      component: 'transitiveViolationsPageContainer',
      data: {
        title: 'Transitive Policy Violations',
      },
      url: '/{ownerType}/{ownerId}/{scanId}/component/{hash}/transitiveViolations',
    });
}

routes.$inject = ['$stateProvider'];

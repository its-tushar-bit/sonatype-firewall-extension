/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import Integrations from './Integrations';

export const SECTIONS = {
  OVERVIEW: 'overview',
  CICD: 'cicd',
  SCM: 'scm',
  ISSUE_TRACKING: 'issuetracking',
  IDE: 'ide',
};

const integrationsModule = angular
  .module('integrationsModule', ['ngRedux'])
  .component('integrations', iqReact2Angular(Integrations, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('integrations', {
      url: '/integrations',
      component: 'integrations',
      data: {},
    })
    .state(`integrations.${SECTIONS.OVERVIEW}`, {
      url: '/overview',
    })
    .state(`integrations.${SECTIONS.CICD}`, {
      url: '/ci-cd',
    })
    .state(`integrations.${SECTIONS.SCM}`, {
      url: '/scm',
    })
    .state(`integrations.${SECTIONS.ISSUE_TRACKING}`, {
      url: '/issue-tracking',
    })
    .state(`integrations.${SECTIONS.IDE}`, {
      url: '/ide',
    });
}

routes.$inject = ['$stateProvider'];

export default integrationsModule;

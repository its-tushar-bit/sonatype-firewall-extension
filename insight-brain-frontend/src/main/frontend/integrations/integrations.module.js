/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import integrationsTemplate from './integrations.html';
import IntegrationsNavigation from './IntegrationsNavigation';
import Overview from './sections/overview/Overview';
import CiCd from './sections/CiCd';
import Scm from './sections/Scm';
import IssueTracking from './sections/IssueTracking';
import Ide from 'MainRoot/integrations/sections/Ide';

export const SECTIONS = {
  OVERVIEW: 'overview',
  CICD: 'cicd',
  SCM: 'scm',
  ISSUE_TRACKING: 'issuetracking',
  IDE: 'ide',
};

const integrationsModule = angular
  .module('integrationsModule', ['ngRedux'])
  .component('integrationsNavigation', iqReact2Angular(IntegrationsNavigation, [], ['$ngRedux', '$state']))
  .component('overview', iqReact2Angular(Overview, [], ['$ngRedux']))
  .component('ciCd', iqReact2Angular(CiCd, [], ['$ngRedux']))
  .component('scm', iqReact2Angular(Scm, [], ['$ngRedux']))
  .component('issueTracking', iqReact2Angular(IssueTracking, [], ['$ngRedux']))
  .component('ide', iqReact2Angular(Ide, [], ['$ngRedux']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('integrations', {
      url: '/integrations',
      template: integrationsTemplate,
      redirectTo: `integrations.${SECTIONS.OVERVIEW}`,
    })
    .state(`integrations.${SECTIONS.OVERVIEW}`, {
      url: '/overview',
      component: 'overview',
    })
    .state(`integrations.${SECTIONS.CICD}`, {
      url: '/ci-cd',
      component: 'ciCd',
    })
    .state(`integrations.${SECTIONS.SCM}`, {
      url: '/scm',
      component: 'scm',
    })
    .state(`integrations.${SECTIONS.ISSUE_TRACKING}`, {
      url: '/issue-tracking',
      component: 'issueTracking',
    })
    .state(`integrations.${SECTIONS.IDE}`, {
      url: '/ide',
      component: 'ide',
    });
}

routes.$inject = ['$stateProvider'];

export default integrationsModule;

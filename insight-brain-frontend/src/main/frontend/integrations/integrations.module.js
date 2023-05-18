/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import integrationsTemplate from './integrations.html';
import IntegrationsNavigation from './IntegrationsNavigation';
import Overview from './sections/Overview';
import CiCd from './sections/CiCd';
import Scm from './sections/Scm';
import IssueTracking from './sections/IssueTracking';
import Others from './sections/Others';
import Ide from 'MainRoot/integrations/sections/Ide';

export const SECTIONS = {
  OVERVIEW: 'overview',
  CICD: 'cicd',
  SCM: 'scm',
  ISSUE_TRACKING: 'issuetracking',
  IDE: 'ide',
  OTHERS: 'others',
};

export const INTEGRATIONS_OVERVIEW = `integrations.${SECTIONS.OVERVIEW}`;
export const INTEGRATIONS_CICD = `integrations.${SECTIONS.CICD}`;
export const INTEGRATIONS_SCM = `integrations.${SECTIONS.SCM}`;
export const INTEGRATIONS_ISSUE_TRACKING = `integrations.${SECTIONS.ISSUE_TRACKING}`;
export const INTEGRATIONS_IDE = `integrations.${SECTIONS.IDE}`;
export const INTEGRATIONS_OTHERS = `integrations.${SECTIONS.OTHERS}`;

export default angular
  .module('integrationsModule', ['ngRedux'])
  .component('integrationsNavigation', iqReact2Angular(IntegrationsNavigation, [], ['$ngRedux', '$state']))
  .component('overview', iqReact2Angular(Overview, [], ['$ngRedux']))
  .component('ciCd', iqReact2Angular(CiCd, [], ['$ngRedux']))
  .component('scm', iqReact2Angular(Scm, [], ['$ngRedux']))
  .component('issueTracking', iqReact2Angular(IssueTracking, [], ['$ngRedux']))
  .component('ide', iqReact2Angular(Ide, [], ['$ngRedux']))
  .component('others', iqReact2Angular(Others, [], ['$ngRedux']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('integrations', {
      url: '/integrations',
      template: integrationsTemplate,
      data: {},
    })
    .state(`integrations.${SECTIONS.OVERVIEW}`, {
      url: '/overview',
      component: 'overview',
      data: {},
    })
    .state(`integrations.${SECTIONS.CICD}`, {
      url: '/ci-cd',
      component: 'ciCd',
      data: {},
    })
    .state(`integrations.${SECTIONS.SCM}`, {
      url: '/scm',
      component: 'scm',
      data: {},
    })
    .state(`integrations.${SECTIONS.ISSUE_TRACKING}`, {
      url: '/issue-tracking',
      component: 'issueTracking',
      data: {},
    })
    .state(`integrations.${SECTIONS.IDE}`, {
      url: '/ide',
      component: 'ide',
      data: {},
    })
    .state(`integrations.${SECTIONS.OTHERS}`, {
      url: '/others',
      component: 'overview',
      data: {},
    });
}

routes.$inject = ['$stateProvider'];

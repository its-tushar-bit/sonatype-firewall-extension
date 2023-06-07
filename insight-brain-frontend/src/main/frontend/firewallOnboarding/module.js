/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import FirewallOnboardingPage from './FirewallOnboardingPage';
import firewallOnboarding from './firewallOnboarding';

export default angular
  .module('firewallOnboardingModule', ['ngRedux'])
  .component('firewallOnboarding', firewallOnboarding)
  .component('firewallOnboardingPage', iqReact2Angular(FirewallOnboardingPage, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('firewallOnboarding', {
      component: 'firewallOnboarding',
      abstract: true,
    })
    .state('firewallOnboarding.firewallOnboardingPage', {
      url: '/firewallOnboarding',
      component: 'firewallOnboardingPage',
      data: {
        title: 'Firewall Onboarding',
      },
      params: {
        embeddable: false,
      },
    });
}

routes.$inject = ['$stateProvider'];

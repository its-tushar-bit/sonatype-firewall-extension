/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import FirewallPageContainer from './FirewallPageContainer';
import FirewallAutoUnqaurantinePageContainer from './autounquarantine/FirewallAutoUnquarantinePageContainer';
import firewallCipModalModule from './firewallCipModal/module';
import firewall from './firewall';

export default angular
  .module('firewallModule', [firewallCipModalModule.name, 'ngRedux'])
  .component('firewall', firewall)
  .component(
    'firewallPage',
    react2angular(withStoreProvider(withRouterStateProvider(FirewallPageContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'firewallAutoUnquarantinePage',
    react2angular(
      withStoreProvider(withRouterStateProvider(FirewallAutoUnqaurantinePageContainer)),
      [],
      ['$ngRedux', '$state']
    )
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('firewall', {
      component: 'firewall',
      abstract: true,
    })
    .state('firewall.firewallPage', {
      url: '/firewall',
      component: 'firewallPage',
      data: {
        title: 'Firewall',
      },
    })
    .state('firewall.firewallAutoUnquarantinePage', {
      url: '/firewall/autoReleaseQuarantine',
      component: 'firewallAutoUnquarantinePage',
      data: {
        title: 'Auto Release Quarantine',
      },
    });
}

routes.$inject = ['$stateProvider'];

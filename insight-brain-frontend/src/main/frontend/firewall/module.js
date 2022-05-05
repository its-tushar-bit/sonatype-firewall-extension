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
import FirewallComponentDetailsPageContainer from './firewallComponentDetailsPage/FirewallComponentDetailsPageContainer';

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
  .component(
    'firewallComponentDetailsPage',
    react2angular(
      withStoreProvider(withRouterStateProvider(FirewallComponentDetailsPageContainer)),
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
    })
    .state('firewall.componentDetailPage', {
      url:
        '/firewall/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}?proprietary&identificationSource&scanId',
      component: 'firewallComponentDetailsPage',
      data: {
        title: 'Firewall Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('firewall.componentDetailPage.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('firewall.componentDetailPage.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('firewall.componentDetailPage.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('firewall.componentDetailPage.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('firewall.componentDetailPage.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    });
}

routes.$inject = ['$stateProvider'];

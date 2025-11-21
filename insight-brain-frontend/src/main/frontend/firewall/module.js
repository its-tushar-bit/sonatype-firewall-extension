/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import FirewallPageContainer from './FirewallPageContainer';
import FirewallAutoUnqaurantinePageContainer from './autounquarantine/FirewallAutoUnquarantinePageContainer';
import firewall from './firewall';
import FirewallComponentDetailsPage from './firewallComponentDetailsPage/FirewallComponentDetailsPage';

export default angular
  .module('firewallModule', ['ui.router'])
  .component('firewall', firewall)
  .component('firewallPage', iqReact2Angular(FirewallPageContainer, [], ['$state']))
  .component('firewallAutoUnquarantinePage', iqReact2Angular(FirewallAutoUnqaurantinePageContainer, [], ['$state']))
  .component('firewallComponentDetailsPage', iqReact2Angular(FirewallComponentDetailsPage, [], ['$state']))
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
    .state('firewall.componentDetailsPage', {
      url:
        '/firewall/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}?pathname&componentDisplayName',
      component: 'firewallComponentDetailsPage',
      data: {
        title: 'Firewall Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('firewall.componentDetailsPage.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('firewall.componentDetailsPage.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('firewall.componentDetailsPage.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('firewall.componentDetailsPage.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('firewall.componentDetailsPage.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('firewall.componentDetailsPage.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('firewall.violationWaivers', {
      url:
        '/firewall/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/waivers/{violationId}?pathname&componentDisplayName',
      component: 'listWaiversPage',
    })
    .state('firewall.addWaiver', {
      url:
        '/firewall/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/addWaiver/{violationId}?pathname&componentDisplayName',
      component: 'addWaiverPage',
      data: {
        title: 'Add Waiver',
      },
    })
    .state('firewall.vulnerabilityCustomize', {
      url:
        '/firewall/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?pathname&componentDisplayName&' +
        'componentIdentifier&repositoryId&matchState&componentHash&tabId&isFirewall',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    });

  $stateProvider
    .state('repository', {
      component: 'firewall',
      abstract: true,
    })
    .state('repository.componentDetailsPage', {
      url:
        '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}?pathname&componentDisplayName',
      component: 'firewallComponentDetailsPage',
      data: {
        title: 'Repository Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('repository.componentDetailsPage.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('repository.componentDetailsPage.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('repository.componentDetailsPage.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('repository.componentDetailsPage.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('repository.componentDetailsPage.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('repository.componentDetailsPage.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('repository.violationWaivers', {
      url:
        '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/waivers/{violationId}?pathname&componentDisplayName',
      component: 'listWaiversPage',
    })
    .state('repository.addWaiver', {
      url:
        '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/addWaiver/{violationId}?pathname&componentDisplayName',
      component: 'addWaiverPage',
      data: {
        title: 'Add Waiver',
      },
    })
    .state('repository.vulnerabilityCustomize', {
      url:
        '/repository/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?pathname&componentDisplayName&' +
        'repositoryId&matchState&componentHash&tabId&isRepository&componentIdentifier',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    });
}

routes.$inject = ['$stateProvider'];

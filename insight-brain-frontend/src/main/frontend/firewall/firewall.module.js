/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import viewTemplate from 'MainRoot/owner.manager/state/owner.manager.view.html';
import editTemplate from 'MainRoot/owner.manager/state/owner.manager.edit.html';
import FirewallPageContainer from './FirewallPageContainer';
import FirewallAutoUnqaurantinePageContainer from './autounquarantine/FirewallAutoUnquarantinePageContainer';
import firewall from './firewall';
import FirewallComponentDetailsPage from './firewallComponentDetailsPage/FirewallComponentDetailsPage';
import { selectIsDirty as policyEditorSelectIsDirty } from 'MainRoot/OrgsAndPolicies/policySelectors';
import {
  QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED,
  ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
} from 'MainRoot/utility/services/routeStateUtilService';
import { QUARANTINE, WAIVERS, ROI } from 'MainRoot/firewall/firewallConstants';

export default angular
  .module('firewallModule', ['ngRedux'])
  .component('firewall', firewall)
  .component('firewallPage', iqReact2Angular(FirewallPageContainer, [], ['$ngRedux', '$state']))
  .component(
    'firewallAutoUnquarantinePage',
    iqReact2Angular(FirewallAutoUnqaurantinePageContainer, [], ['$ngRedux', '$state'])
  )
  .component('firewallComponentDetailsPage', iqReact2Angular(FirewallComponentDetailsPage, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  var ownerTypesForFirewall = [
    {
      type: 'organization',
      name: 'Organization',
      id: 'organizationId',
      component: 'ownerSummary',
    },
    {
      type: 'repository_container',
      name: 'Repository Managers',
      id: 'repositoryContainerId',
      component: 'repositoriesSummaryView',
      hideOverflowY: true,
    },
    {
      type: 'repository_manager',
      name: 'Repository manager',
      id: 'repositoryManagerId',
      component: 'repositoryManagerSummaryView',
      hideOverflowY: true,
    },
    {
      type: 'repository',
      name: 'Repository',
      id: 'repositoryId',
      component: 'repositorySummaryView',
      hideOverflowY: true,
    },
  ];

  $stateProvider
    .state('firewall', {
      url: '/malware-defense',
      abstract: true,
    })
    .state('firewall.home', {
      url: '/',
      redirectTo: 'root',
    })
    .state('firewall.firewallPage', {
      url: '/dashboard?roiEnabled',
      component: 'firewallPage',
      data: {
        title: 'Firewall Dashboard',
      },
    })
    .state('firewall.firewallPage.quarantine', {
      url: '/quarantine',
      data: {
        title: 'Firewall Dashboard - Quarantine',
        activeTab: QUARANTINE,
      },
    })
    .state('firewall.firewallPage.waivers', {
      url: '/waivers',
      data: {
        title: 'Firewall Dashboard - Waivers',
        activeTab: WAIVERS,
      },
    })
    .state('firewall.firewallPage.roi', {
      url: '/roi',
      data: {
        title: 'Firewall Dashboard - ROI',
        activeTab: ROI,
      },
    })
    .state('firewall.waiver', {
      abstract: true,
      component: 'waiverSidebarView',
      url: '/waiver',
    })
    .state('firewall.waiver.details', {
      component: 'waiverDetailsContainer',
      data: {
        title: 'Waiver Details',
      },
      url: '/{ownerType}/{ownerId}/{waiverId}?type&sidebarReference&sidebarId&page',
    })
    .state('firewall.management', {
      url: '/management',
      abstract: true,
    })
    .state('firewall.management.view', {
      url: '/view',
      template: viewTemplate,
      data: {
        title: 'Management',
        authenticationRequired: true,
      },
    })
    .state('firewall.management.tree', {
      url: '/tree',
      data: {
        title: 'Inheritance Hierarchy',
        authenticationRequired: true,
      },
      component: 'ownersTreePage',
    })
    .state('firewall.management.edit', {
      abstract: true,
    })
    .state('firewall.firewallAutoUnquarantinePage', {
      url: '/autoReleaseQuarantine',
      component: 'firewallAutoUnquarantinePage',
      data: {
        title: 'Auto Release Quarantine',
      },
    })
    .state('firewall.componentDetailsPage', {
      url:
        '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}?pathname&componentDisplayName',
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
    .state('firewall.quarantinedComponentReport', {
      url: '/repositories/quarantinedComponent/{token}',
      component: 'quarantinedComponentReport',
      data: {
        title: 'Quarantined Component Report',
        authenticationRequired: QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED,
      },
    })
    .state('firewall.violationWaivers', {
      url:
        '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/waivers/{violationId}?pathname&componentDisplayName',
      component: 'listWaiversPage',
    })
    .state('firewall.addWaiver', {
      url:
        '/repository/{repositoryId}/component/{componentIdentifier}/{componentHash}/{matchState}/{tabId}/addWaiver/{violationId}?pathname&componentDisplayName',
      component: 'addWaiverPage',
      data: {
        title: 'Add Waiver',
      },
    })
    .state('firewall.vulnerabilityCustomize', {
      url:
        '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?pathname&componentDisplayName&' +
        'componentIdentifier&repositoryId&matchState&componentHash&tabId&isFirewall',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state('firewall.management.edit.organization.category', {
      url: '/category/{categoryId}',
      data: {
        title: 'Organization Category',
        isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
      },
      component: 'createEditApplicationCategory',
    })
    .state('firewall.management.edit.organization.create-category', {
      url: '/category',
      data: {
        title: 'Organization Category',
        isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
      },
      component: 'createEditApplicationCategory',
    })
    .state('firewall.management.edit.organization.create-license-threat-group', {
      data: {
        title: 'Organization License Threat Group',
        isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
      },
      url: '/licenseThreatGroup',
      component: 'licenseThreatGroupEditor',
    })
    .state('firewall.management.edit.organization.edit-license-threat-group', {
      data: {
        title: 'Organization License Threat Group',
        isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
      },
      url: '/licenseThreatGroup/{licenseThreatGroupId}',
      component: 'licenseThreatGroupEditor',
    })
    .state('firewall.management.edit.organization.edit-data-retention', {
      url: '/data-retention',
      data: {
        title: 'Organization Data Retention',
        isDirty: ['orgsAndPolicies', 'retention', 'isDirty'],
      },
      component: 'dataRetentionEditor',
    })
    .state('firewall.repositoryBaseConfigurations', {
      abstract: true,
      url: '/management/edit',
      component: 'innerSourceRepositoryBaseConfigurations',
      data: {
        title: 'Repository Configurations',
        isDirty: ['innerSourceRepositoryBaseConfigurations', 'isDirty'],
      },
    })
    .state('firewall.repositoryBaseConfigurations.organization', {
      url: '/organization/{organizationId}/repositoryBaseConfigurations',
    })
    .state('firewall.repository-report', {
      url: '/repository/{repositoryId}/result?hideBackButton={hideButton}',
      component: 'repositoryResultsSummaryPage',
    })
    .state('firewall.users', {
      url: '/users',
      component: 'users',
      data: {
        title: 'Users',
      },
    })
    .state('firewall.createUser', {
      url: '/users/_new_',
      component: 'createUser',
      data: {
        title: 'Add New User',
        isDirty: ['userConfiguration', 'isDirty'],
      },
    })
    .state('firewall.editUser', {
      url: '/users/{userId}',
      component: 'editUser',
      data: {
        title: 'Edit User',
        isDirty: ['userConfiguration', 'isDirty'],
      },
    })
    .state('firewall.rolesList', {
      url: '/roles',
      component: 'roles',
      data: {
        title: 'Roles',
      },
    })
    .state('firewall.addRole', {
      url: '/roles/_new_',
      component: 'roleEditor',
      data: {
        title: 'Create a role',
        isDirty: ['roleEditor', 'isDirty'],
      },
    })
    .state('firewall.editRole', {
      url: '/roles/{roleId}',
      component: 'roleEditor',
      data: {
        title: 'Edit a Role',
        isDirty: ['roleEditor', 'isDirty'],
      },
    })
    .state('firewall.administrators', {
      component: 'administratorsConfig',
      url: '/administrators',
      data: {
        title: 'Administrator Config',
      },
    })
    .state('firewall.administratorsEdit', {
      component: 'administratorsEdit',
      url: '/administrators/{roleId}',
      data: {
        title: 'Administrator Edit',
        isDirty: ['administratorsConfig', 'isDirty'],
      },
    })
    .state('firewall.productlicense', {
      url: '/productlicense',
      component: 'productLicenseDetail',
      data: {
        title: 'Product License',
      },
    })
    .state('firewall.create-ldap', {
      url: '/ldap/create',
      component: 'createLdap',
      data: {
        title: 'Create LDAP Configuration',
        isDirty: ['ldapConfig', 'isDirty'],
      },
    })
    .state('firewall.edit-ldap-connection', {
      url: '/ldap/edit/{ldapId}',
      component: 'editLdapConnection',
      data: {
        title: 'Edit LDAP Configuration',
        isDirty: ['ldapConfig', 'isDirty'],
      },
    })
    .state('firewall.edit-ldap-usermapping', {
      url: '/ldap/edit/{ldapId}/userMapping',
      component: 'editLdapUserMapping',
      data: {
        title: 'Edit LDAP Configuration',
        isDirty: ['ldapConfig', 'isDirty'],
      },
    })
    .state('firewall.ldap-list', {
      url: '/ldap-servers',
      component: 'ldapList',
      data: {
        title: 'LDAP Servers',
        isDirty: ['ldapList', 'isDirty'],
      },
    })
    .state('firewall.saml', {
      url: '/saml',
      component: 'samlConfigurationPage',
      data: {
        title: 'SAML',
        isDirty: ['samlConfiguration', 'isDirty'],
      },
    })
    .state('firewall.waivedComponentUpgradesConfiguration', {
      component: 'waivedComponentUpgradesConfiguration',
      url: '/waivedComponentUpgradesConfiguration',
      data: {
        title: 'Waived Component Upgrades',
        isDirty: ['waivedComponentUpgradesConfiguration', 'isDirty'],
      },
    })
    .state('firewall.atlassianCrowdConfiguration', {
      url: '/crowd',
      component: 'atlassianCrowdConfiguration',
      data: {
        title: 'Atlassian Crowd Configuration',
        isDirty: ['atlassianCrowdConfiguration', 'isDirty'],
      },
    })
    .state('firewall.mailConfig', {
      component: 'mailConfig',
      url: '/mailConfig',
      data: {
        title: 'Mail Config',
        isDirty: ['mailConfig', 'isDirty'],
      },
      resolve: {
        isAuthorized: [
          'PermissionService',
          function (PermissionService) {
            return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          },
        ],
      },
    })
    .state('firewall.proxyConfig', {
      component: 'proxyConfig',
      url: '/proxyConfig',
      data: {
        title: 'Proxy',
        isDirty: ['proxyConfig', 'isDirty'],
      },
    })
    .state('firewall.listWebhooks', {
      url: '/webhooks/list',
      component: 'listWebhooks',
      data: {
        title: 'Webhooks',
      },
    })
    .state('firewall.addWebhook', {
      url: '/webhooks/create',
      component: 'editWebhook',
      data: {
        title: 'Create Webhook',
        isDirty: ['webhooks', 'isDirty'],
      },
    })
    .state('firewall.editWebhook', {
      url: '/webhooks/{webhookId}',
      component: 'editWebhook',
      data: {
        title: 'Edit Webhook',
        isDirty: ['webhooks', 'isDirty'],
      },
    })
    .state('firewall.systemNoticeConfiguration', {
      component: 'systemNoticeConfiguration',
      url: '/systemNoticeConfiguration',
      data: {
        title: 'System Notice',
        isDirty: ['systemNoticeConfiguration', 'viewState', 'isDirty'],
      },
    })
    .state('firewall.baseUrlConfiguration', {
      url: '/baseUrl',
      component: 'baseUrlConfiguration',
      data: {
        title: 'Base URL Configuration',
        isDirty: ['baseUrlConfiguration', 'isDirty'],
      },
    })
    .state('firewall.gettingStarted', {
      component: 'gettingStarted',
      url: '/gettingStarted',
      data: {
        title: 'Getting Started',
      },
    })
    .state('firewall.roiConfiguration', {
      component: 'roiConfiguration',
      url: '/roiConfiguration',
      data: {
        title: 'ROI Configuration',
      },
    })
    .state('firewall.editRoiConfiguration', {
      component: 'editRoiConfiguration',
      url: '/roiConfiguration/edit',
      data: {
        title: 'Edit ROI Configuration',
      },
    })
    .state('firewall.api', {
      url: '/api',
      data: {
        title: 'API',
        authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
      },
      component: 'apiPage',
    });

  ownerTypesForFirewall.forEach(function (ownerType) {
    $stateProvider
      .state('firewall.management.view.' + ownerType.type, {
        url: '/' + ownerType.type + '/{' + ownerType.id + '}',
        data: {
          title: ownerType.name + ' Management',
          viewportSized: true,
          hideOverflowY: ownerType.hideOverflowY,
        },
        component: ownerType.component,
      })
      .state('firewall.management.edit.' + ownerType.type, {
        url: '/edit/' + ownerType.type + '/{' + ownerType.id + '}',
        data: {
          title: ownerType.name + ' Management',
        },
        template: editTemplate,
      })
      .state('firewall.management.edit.' + ownerType.type + '.label', {
        url: '/label/{labelId}',
        data: {
          title: ownerType.name + ' Labels',
          isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
        },
        component: 'createComponentLabel',
      })
      .state('firewall.management.edit.' + ownerType.type + '.create-label', {
        url: '/label',
        data: {
          title: ownerType.name + ' Labels',
          isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
        },
        component: 'createComponentLabel',
      })
      .state('firewall.management.edit.' + ownerType.type + '.policy', {
        url: '/policy/{policyId}',
        data: {
          title: ownerType.name + ' Policy',
          isDirty: policyEditorSelectIsDirty,
        },
        component: 'policyEditor',
      })
      .state('firewall.management.edit.' + ownerType.type + '.create-policy', {
        url: '/policy',
        data: {
          title: ownerType.name + ' Policy',
          isDirty: policyEditorSelectIsDirty,
        },
        component: 'policyEditor',
      })
      .state('firewall.management.edit.' + ownerType.type + '.add-access', {
        url: '/access',
        data: {
          title: ownerType.name + ' Access',
          isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
        },
        component: 'accessPage',
      })
      .state('firewall.management.edit.' + ownerType.type + '.edit-access', {
        url: '/access/{roleId}',
        data: {
          title: ownerType.name + ' Access',
          isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
        },
        component: 'accessPage',
      })
      .state('firewall.management.edit.' + ownerType.type + '.legacy-violations', {
        url: '/legacyViolations',
        data: {
          title: ownerType.name + ' Legacy Violations',
          isDirty: ['orgsAndPolicies', 'legacyViolations', 'isDirty'],
        },
        component: 'legacyViolationsEditor',
      })
      .state('firewall.management.edit.' + ownerType.type + '.monitor-policy', {
        url: '/monitoring',
        data: {
          title: ownerType.name + ' Continuous Monitoring',
          isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
        },
        component: 'continuousMonitoring',
      })
      .state('firewall.management.edit.' + ownerType.type + '.proprietary-config-policy', {
        url: '/proprietary',
        data: {
          title: ownerType.name + ' Proprietary Components',
          isDirty: ['orgsAndPolicies', 'proprietary', 'isDirty'],
        },
        component: 'proprietaryComponentConfiguration',
      })
      .state('firewall.management.edit.' + ownerType.type + '.edit-source-control', {
        url: '/source-control',
        data: {
          title: 'Source Control',
          isDirty: ['orgsAndPolicies', 'sourceControlConfiguration', 'isDirty'],
        },
        component: 'sourceControlConfiguration',
      })
      .state('firewall.management.edit.' + ownerType.type + '.edit-waivers', {
        url: '/waivers',
        data: {
          title: ownerType.name + ' Waivers Configuration',
        },
        component: 'waiversConfiguration',
      });
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

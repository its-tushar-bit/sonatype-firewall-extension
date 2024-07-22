/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import SbomManagerDashboard from 'MainRoot/sbomManager/features/dashboard/SbomManagerDashboard';
import BillOfMaterials from 'MainRoot/sbomManager/features/billOfMaterials/BillOfMaterials';
import viewTemplate from 'MainRoot/owner.manager/state/owner.manager.view.html';
import editTemplate from 'MainRoot/owner.manager/state/owner.manager.edit.html';
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import advancedSearchModule from 'MainRoot/advancedSearch/module';
import ComponentDetailsPage from 'MainRoot/sbomManager/features/componentDetails/ComponentDetailsPage';

export default angular
  .module('sbomManagerModule', ['ngRedux', advancedSearchModule.name])
  .component('sbomManagerDashboard', iqReact2Angular(SbomManagerDashboard, [], ['$ngRedux', '$state']))
  .component('billOfMaterials', iqReact2Angular(BillOfMaterials, [], ['$ngRedux', '$state']))
  .component('sbomManagerComponentDetails', iqReact2Angular(ComponentDetailsPage, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  const ownerTypesForSbomManager = [
    {
      type: 'organization',
      name: 'Organization',
      id: 'organizationId',
      component: 'ownerSummary',
    },
    {
      type: 'application',
      name: 'Application',
      id: 'applicationPublicId',
      component: 'ownerSummary',
    },
  ];

  $stateProvider
    .state('sbomManager', {
      url: '/sbomManager',
      abstract: true,
    })
    .state('sbomManager.dashboard', {
      url: '/dashboard',
      component: 'sbomManagerDashboard',
      data: {
        title: 'SBOM Manager - Dashboard',
        authenticationRequired: true,
      },
    })
    .state('sbomManager.advancedSearch', {
      url: '/advancedSearch?search',
      component: 'advancedSearch',
      data: {
        title: 'SBOM Manager - Advanced Search',
        authenticationRequired: true,
      },
    })
    .state('sbomManager.management', {
      url: '/management',
      abstract: true,
    })
    .state('sbomManager.management.view', {
      url: '/view',
      template: viewTemplate,
      data: {
        title: 'Management',
        authenticationRequired: true,
      },
    })
    .state('sbomManager.management.tree', {
      url: '/tree',
      data: {
        title: 'Inheritance Hierarchy',
        authenticationRequired: true,
      },
      component: 'ownersTreePage',
    })
    .state('sbomManager.management.edit', {
      abstract: true,
    })
    .state('sbomManager.management.view.bom', {
      url: '/application/{applicationPublicId}/bom/{versionId}/overview',
      component: 'billOfMaterials',
      data: {
        title: 'SBOM Manager - Bill Of Materials',
        authenticationRequired: true,
        noSidebar: true,
      },
    })
    .state('sbomManager.component', {
      url: '/application/{applicationPublicId}/bom/{sbomVersion}/componentDetails/{componentHash}/overview',
      component: 'sbomManagerComponentDetails',
      data: {
        title: 'SBOM Manager - Component Details',
        authenticationRequired: true,
      },
    });

  ownerTypesForSbomManager.forEach(function (ownerType) {
    $stateProvider
      .state('sbomManager.management.view.' + ownerType.type, {
        url: '/' + ownerType.type + '/{' + ownerType.id + '}',
        data: {
          title: ownerType.name + ' Management',
          viewportSized: true,
        },
        component: ownerType.component,
      })
      .state('sbomManager.management.edit.' + ownerType.type, {
        url: '/edit/' + ownerType.type + '/{' + ownerType.id + '}',
        data: {
          title: ownerType.name + ' Management',
        },
        template: editTemplate,
      })
      .state('sbomManager.management.edit.' + ownerType.type + '.add-access', {
        url: '/access',
        data: {
          title: ownerType.name + ' Access',
          isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
        },
        component: 'accessPage',
      })
      .state('sbomManager.management.edit.' + ownerType.type + '.edit-access', {
        url: '/access/{roleId}',
        data: {
          title: ownerType.name + ' Access',
          isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
        },
        component: 'accessPage',
      });
  });
}
routes.$inject = ['$stateProvider'];

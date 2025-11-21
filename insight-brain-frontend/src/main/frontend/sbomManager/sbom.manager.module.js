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
import legalModule from 'MainRoot/legal/legal.module';
import ComponentDetailsPage from 'MainRoot/sbomManager/features/componentDetails/ComponentDetailsPage';
import SbomContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/сontinuousMonitoringEditor/SbomContinuousMonitoringEditor';
import LearnMoreSbomManager from 'MainRoot/sbomManager/features/LearnMoreSbomManager';
import SbomApplicationsPage from 'MainRoot/sbomManager/features/sbomApplicationsPage/SbomApplicationsPage';
import { selectHasSbomManagerLicense } from 'MainRoot/productFeatures/productLicenseSelectors';
import { load as loadProductLicense } from 'MainRoot/configuration/license/productLicenseActions';
import store from 'MainRoot/reduxConfig/store';
import { ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE } from 'MainRoot/utility/services/routeStateUtilService';
import { createLegalRoutes, LEGAL_SBOM_MANAGER_PARENT_ROUTE } from 'MainRoot/legal/legalUtility';
import { isAuthorized } from 'MainRoot/util/permissionService';

export default angular
  .module('sbomManagerModule', [advancedSearchModule.name, legalModule.name])
  .component('sbomManagerDashboard', iqReact2Angular(SbomManagerDashboard, [], ['$state']))
  .component('billOfMaterials', iqReact2Angular(BillOfMaterials, [], ['$state']))
  .component('sbomManagerComponentDetails', iqReact2Angular(ComponentDetailsPage, [], ['$state']))
  .component('sbomContinuousMonitoring', iqReact2Angular(SbomContinuousMonitoringEditor, [], ['$state']))
  .component('sbomManagerApplicationsPage', iqReact2Angular(SbomApplicationsPage, [], ['$state']))
  .component('learnMoreSbomManager', iqReact2Angular(LearnMoreSbomManager, [], ['$state']))
  .config(routes)
  .run(checkLicense);

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
      data: {
        product: 'SBOM Manager',
        favicon: 'productIcons/SBOM',
      },
    })
    .state('sbomManager.dashboard', {
      url: '/dashboard',
      component: 'sbomManagerDashboard',
      data: {
        title: 'Dashboard',
        authenticationRequired: true,
      },
    })
    .state('sbomManager.advancedSearch', {
      url: '/advancedSearch?search',
      component: 'advancedSearch',
      data: {
        title: 'Advanced Search',
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
        title: 'Bill Of Materials',
        authenticationRequired: true,
        noSidebar: true,
      },
    })
    .state('sbomManager.component', {
      url: '/application/{applicationPublicId}/bom/{sbomVersion}/componentDetails/{componentHash}/overview',
      component: 'sbomManagerComponentDetails',
      data: {
        title: 'Component Details',
        authenticationRequired: true,
        hideFooter: true,
      },
    })
    .state('sbomManager.applications', {
      url: '/applications?sortBy?sortDirection',
      component: 'sbomManagerApplicationsPage',
      data: {
        title: 'Applications',
        authenticationRequired: true,
      },
    })
    .state('sbomManager.mailConfig', {
      component: 'mailConfig',
      url: '/mailConfig',
      data: {
        title: 'Mail Config',
        isDirty: ['mailConfig', 'isDirty'],
      },
      resolve: {
        isAuthorized: function () {
          return isAuthorized(['CONFIGURE_SYSTEM']);
        },
      },
    })
    .state('sbomManager.baseUrlConfiguration', {
      url: '/baseUrl',
      component: 'baseUrlConfiguration',
      data: {
        title: 'Base URL Configuration',
        isDirty: ['baseUrlConfiguration', 'isDirty'],
      },
    })
    .state('sbomManager.learnMore', {
      url: '/learnMore',
      component: 'learnMoreSbomManager',
      data: {
        title: 'Learn More',
      },
    })
    .state('sbomManager.api', {
      url: '/api',
      data: {
        title: 'API',
        authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
      },
      component: 'apiPage',
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
      .state('sbomManager.management.edit.' + ownerType.type + '.policy', {
        url: '/policy/{policyId}',
        data: {
          title: ownerType.name + ' Policy',
          isDirty: ['orgsAndPolicies', 'policy', 'isDirty'],
        },
        component: 'policyEditor',
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
      })
      .state('sbomManager.management.edit.' + ownerType.type + '.monitor-policy', {
        url: '/monitoring',
        data: {
          title: ownerType.name + ' Continuous Monitoring',
          isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
        },
        component: 'sbomContinuousMonitoring',
      })
      .state('sbomManager.management.edit.' + ownerType.type + '.public-data-sources-editor', {
        url: '/publicDataSourcesEditor',
        data: {
          title: ownerType.name + ' Public Data Sources',
          isDirty: ['orgsAndPolicies', 'publicDataSources', 'isDirty'],
        },
        component: 'publicDataSourcesEditor',
      });
  });

  createLegalRoutes($stateProvider, LEGAL_SBOM_MANAGER_PARENT_ROUTE);
}

routes.$inject = ['$stateProvider'];

function checkLicense($transitions, $state) {
  $transitions.onBefore({ to: 'sbomManager.**' }, (transition) => {
    return store.dispatch(loadProductLicense()).then(() => {
      const state = store.getState();
      const isSbomManagerEnabled = selectHasSbomManagerLicense(state);
      const transitionTo = transition.to().name;
      const sbomManagerLearnMoreState = 'sbomManager.learnMore';

      if (!isSbomManagerEnabled && transitionTo !== sbomManagerLearnMoreState) {
        return $state.target(sbomManagerLearnMoreState);
      } else if (isSbomManagerEnabled && transitionTo === sbomManagerLearnMoreState) {
        return $state.target('sbomManager.dashboard');
      }
    });
  });
}
checkLicense.$inject = ['$transitions', '$state'];

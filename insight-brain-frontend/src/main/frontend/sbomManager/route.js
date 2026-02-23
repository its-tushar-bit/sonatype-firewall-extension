/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import SbomManagerDashboard from './features/dashboard/SbomManagerDashboard';
import BillOfMaterials from './features/billOfMaterials/BillOfMaterials';
import ComponentDetailsPage from './features/componentDetails/ComponentDetailsPage';
import SbomContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/сontinuousMonitoringEditor/SbomContinuousMonitoringEditor';
import LearnMoreSbomManager from './features/LearnMoreSbomManager';
import SbomApplicationsPage from './features/sbomApplicationsPage/SbomApplicationsPage';
import { selectHasSbomManagerLicense } from 'MainRoot/productFeatures/productLicenseSelectors';
import { load as loadProductLicense } from 'MainRoot/configuration/license/productLicenseActions';
import store from 'MainRoot/reduxConfig/store';
import { ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE } from 'MainRoot/utility/services/routeStateUtilService';
import { OwnerManagerViewWrapper } from 'MainRoot/owner.manager/state/OwnerManagerViewWrapper';
import { OwnerManagerEditWrapper } from 'MainRoot/owner.manager/state/OwnerManagerEditWrapper';

// Import React components for routes
import AdvancedSearchContainer from 'MainRoot/advancedSearch/AdvancedSearchContainer';
import OwnersTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import MailConfigContainer from 'MainRoot/configuration/mail/MailConfigContainer';
import BaseUrlConfiguration from 'MainRoot/configuration/baseUrl/BaseUrlConfiguration';
import ApiPage from 'MainRoot/api/ApiPage';
import OwnerSummary from 'MainRoot/OrgsAndPolicies/ownerSummary/OwnerSummary';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import AccessPage from 'MainRoot/OrgsAndPolicies/access/AccessPage';
import PublicDataSourcesEditor from 'MainRoot/OrgsAndPolicies/publicDataSources/PublicDataSourcesEditor';

// Abstract parent state
router.stateRegistry.register({
  name: 'sbomManager',
  url: '/sbomManager',
  abstract: true,
  component: UIView,
  data: {
    product: 'SBOM Manager',
    favicon: 'productIcons/SBOM',
  },
});

router.stateRegistry.register({
  name: 'sbomManager.dashboard',
  url: '/dashboard',
  component: SbomManagerDashboard,
  data: {
    title: 'Dashboard',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'sbomManager.advancedSearch',
  url: '/advancedSearch?search',
  component: AdvancedSearchContainer,
  data: {
    title: 'Advanced Search',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'sbomManager.management',
  url: '/management',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'sbomManager.management.view',
  url: '/view',
  component: OwnerManagerViewWrapper,
  data: {
    title: 'Management',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'sbomManager.management.tree',
  url: '/tree',
  component: OwnersTreePage,
  data: {
    title: 'Inheritance Hierarchy',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'sbomManager.management.edit',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'sbomManager.management.view.bom',
  url: '/application/{applicationPublicId}/bom/{versionId}/overview',
  component: BillOfMaterials,
  data: {
    title: 'Bill Of Materials',
    authenticationRequired: true,
    noSidebar: true,
  },
});

router.stateRegistry.register({
  name: 'sbomManager.component',
  url: '/application/{applicationPublicId}/bom/{sbomVersion}/componentDetails/{componentHash}/overview',
  component: ComponentDetailsPage,
  data: {
    title: 'Component Details',
    authenticationRequired: true,
    hideFooter: true,
  },
});

router.stateRegistry.register({
  name: 'sbomManager.applications',
  url: '/applications?sortBy?sortDirection',
  component: SbomApplicationsPage,
  data: {
    title: 'Applications',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'sbomManager.mailConfig',
  url: '/mailConfig',
  component: MailConfigContainer,
  data: {
    title: 'Mail Config',
    isDirty: ['mailConfig', 'isDirty'],
  },
  resolve: [
    {
      token: 'isAuthorized',
      resolveFn: () => {
        const { isAuthorized } = require('MainRoot/util/permissionService');
        return isAuthorized(['CONFIGURE_SYSTEM']);
      },
    },
  ],
});

router.stateRegistry.register({
  name: 'sbomManager.baseUrlConfiguration',
  url: '/baseUrl',
  component: BaseUrlConfiguration,
  data: {
    title: 'Base URL Configuration',
    isDirty: ['baseUrlConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'sbomManager.learnMore',
  url: '/learnMore',
  component: LearnMoreSbomManager,
  data: {
    title: 'Learn More',
  },
});

router.stateRegistry.register({
  name: 'sbomManager.api',
  url: '/api',
  component: ApiPage,
  data: {
    title: 'API',
    authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
  },
});

// Dynamic routes for owner types
const ownerTypesForSbomManager = [
  {
    type: 'organization',
    name: 'Organization',
    id: 'organizationId',
    component: OwnerSummary,
  },
  {
    type: 'application',
    name: 'Application',
    id: 'applicationPublicId',
    component: OwnerSummary,
  },
];

ownerTypesForSbomManager.forEach((ownerType) => {
  router.stateRegistry.register({
    name: `sbomManager.management.view.${ownerType.type}`,
    url: `/${ownerType.type}/{${ownerType.id}}`,
    component: ownerType.component,
    data: {
      title: `${ownerType.name} Management`,
      viewportSized: true,
    },
  });

  router.stateRegistry.register({
    name: `sbomManager.management.edit.${ownerType.type}`,
    url: `/edit/${ownerType.type}/{${ownerType.id}}`,
    component: OwnerManagerEditWrapper,
    data: {
      title: `${ownerType.name} Management`,
    },
  });

  router.stateRegistry.register({
    name: `sbomManager.management.edit.${ownerType.type}.policy`,
    url: '/policy/{policyId}',
    component: PolicyEditor,
    data: {
      title: `${ownerType.name} Policy`,
      isDirty: ['orgsAndPolicies', 'policy', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `sbomManager.management.edit.${ownerType.type}.add-access`,
    url: '/access',
    component: AccessPage,
    data: {
      title: `${ownerType.name} Access`,
      isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `sbomManager.management.edit.${ownerType.type}.edit-access`,
    url: '/access/{roleId}',
    component: AccessPage,
    data: {
      title: `${ownerType.name} Access`,
      isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `sbomManager.management.edit.${ownerType.type}.monitor-policy`,
    url: '/monitoring',
    component: SbomContinuousMonitoringEditor,
    data: {
      title: `${ownerType.name} Continuous Monitoring`,
      isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `sbomManager.management.edit.${ownerType.type}.public-data-sources-editor`,
    url: '/publicDataSourcesEditor',
    component: PublicDataSourcesEditor,
    data: {
      title: `${ownerType.name} Public Data Sources`,
      isDirty: ['orgsAndPolicies', 'publicDataSources', 'isDirty'],
    },
  });
});

// SBOM Manager Legal routes
// Note: Legal routes for SBOM Manager are registered in legal/sbomManager/route.js
// with parent route 'sbomManager.legal'

// Transition hook for license checking
router.transitionService.onBefore({ to: 'sbomManager.**' }, (transition) => {
  return store.dispatch(loadProductLicense()).then(() => {
    const state = store.getState();
    const isSbomManagerEnabled = selectHasSbomManagerLicense(state);
    const transitionTo = transition.to().name;
    const sbomManagerLearnMoreState = 'sbomManager.learnMore';

    if (!isSbomManagerEnabled && transitionTo !== sbomManagerLearnMoreState) {
      return router.stateService.target(sbomManagerLearnMoreState);
    } else if (isSbomManagerEnabled && transitionTo === sbomManagerLearnMoreState) {
      return router.stateService.target('sbomManager.dashboard');
    }
  });
});

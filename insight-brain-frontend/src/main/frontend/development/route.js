/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import SonatypeDeveloperPage from './developmentDashboard/SonatypeDeveloperPage';
import { ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE } from 'MainRoot/utility/services/routeStateUtilService';
import ReportsPage from 'MainRoot/report/react/ReportsPage';
import AdvancedSearchContainer from 'MainRoot/advancedSearch/AdvancedSearchContainer';
import AddWaiverPageContainer from 'MainRoot/waivers/AddWaiverPageContainer';
import RequestWaiverPage from 'MainRoot/waivers/RequestWaiverPage';
import ApiPage from 'MainRoot/api/ApiPage';

export const SECTIONS = {
  OVERVIEW: 'overview',
  CICD: 'cicd',
  SCM: 'scm',
  ISSUE_TRACKING: 'issuetracking',
  IDE: 'ide',
};

// Abstract parent state
router.stateRegistry.register({
  name: 'developer',
  url: '/developer',
  abstract: true,
  component: UIView,
  data: {
    product: 'Developer',
    favicon: 'productIcons/Developer',
  },
});

router.stateRegistry.register({
  name: 'developer.dashboard',
  url: '/dashboard',
  component: SonatypeDeveloperPage,
  redirectTo: `developer.dashboard.${SECTIONS.OVERVIEW}`,
  data: {
    title: 'Dashboard',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: `developer.dashboard.${SECTIONS.OVERVIEW}`,
  url: '/overview',
  data: {
    title: 'Overview',
  },
});

router.stateRegistry.register({
  name: `developer.dashboard.${SECTIONS.CICD}`,
  url: '/ci-cd',
});

router.stateRegistry.register({
  name: `developer.dashboard.${SECTIONS.SCM}`,
  url: '/scm',
});

router.stateRegistry.register({
  name: `developer.dashboard.${SECTIONS.ISSUE_TRACKING}`,
  url: '/issue-tracking',
});

router.stateRegistry.register({
  name: `developer.dashboard.${SECTIONS.IDE}`,
  url: '/ide',
});

router.stateRegistry.register({
  name: 'developer.priorities',
  url: '/priorities',
  component: ReportsPage,
  data: {
    title: 'Priorities',
  },
});

router.stateRegistry.register({
  name: 'developer.advancedSearch',
  url: '/advancedSearch?search',
  component: AdvancedSearchContainer,
  data: {
    title: 'Advanced Search',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'developer.addWaiver',
  url: '/addWaiver/{violationId}?comments&reasonId',
  component: AddWaiverPageContainer,
  data: {
    title: 'Add Waiver',
    isDirty: ['addWaiver', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'developer.requestWaiver',
  url: '/requestWaiver/{violationId}',
  component: RequestWaiverPage,
  data: {
    title: 'Request Waiver',
    isDirty: ['requestWaiver', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'developer.api',
  url: '/api',
  component: ApiPage,
  data: {
    title: 'API',
    authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
  },
});

// URL rewrite: Redirect from old URL to new URL
router.urlService.rules.when('/developer/reports', (matchValues, _urlParts, router) =>
  router.stateService.go('developer.priorities', matchValues)
);

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import LegalDashboardContainer from './dashboard/LegalDashboardContainer';
import {
  LEGAL_APPLICATIONS_DASHBOARD_DATA,
  LEGAL_APPLICATIONS_DASHBOARD_URL,
  LEGAL_COMPONENTS_DASHBOARD_DATA,
  LEGAL_COMPONENTS_DASHBOARD_URL,
} from './dashboard/legalDashboardRouteData';
import { LEGAL_DEEP_LINK_STATES } from './legalDeepLinkStates';

// Abstract parent state
router.stateRegistry.register({
  name: 'legal',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'legal.applicationsDashboard',
  url: LEGAL_APPLICATIONS_DASHBOARD_URL,
  component: LegalDashboardContainer,
  data: LEGAL_APPLICATIONS_DASHBOARD_DATA,
});

router.stateRegistry.register({
  name: 'legal.componentsDashboard',
  url: LEGAL_COMPONENTS_DASHBOARD_URL,
  component: LegalDashboardContainer,
  data: LEGAL_COMPONENTS_DASHBOARD_DATA,
});

// Every other legal.* state (application details, component overview and its other entry-point
// shapes, attribution report generation, and the copyright/notice/license-file/license-details
// deep-link families) is defined once in legalDeepLinkStates.ts and shared with the Nexus One
// embedded mount (nexus-one/routes.tsx) so the two can't drift apart.
LEGAL_DEEP_LINK_STATES.forEach((stateDef) => {
  router.stateRegistry.register({
    name: stateDef.name,
    url: stateDef.url,
    component: stateDef.component,
    ...(stateDef.data ? { data: stateDef.data } : {}),
    ...(stateDef.abstract ? { abstract: true } : {}),
  });
});

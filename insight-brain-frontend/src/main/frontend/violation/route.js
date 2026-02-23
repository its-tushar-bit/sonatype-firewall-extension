/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ViolationPageContainer from './ViolationPageContainer';
import SidebarLayout from 'MainRoot/sidebarNav/SidebarLayout';
import TransitiveViolationsPage from 'MainRoot/violation/TransitiveViolationsPage';

// Sidebar view abstract state
router.stateRegistry.register({
  name: 'sidebarView',
  abstract: true,
  component: SidebarLayout,
  url: '/violation',
});

// Violation details
router.stateRegistry.register({
  name: 'sidebarView.violation',
  url: '/{id}?type&sidebarReference&sidebarId&page',
  component: ViolationPageContainer,
  data: {
    title: 'Policy Violation',
  },
});

// Transitive violations
router.stateRegistry.register({
  name: 'transitiveViolations',
  url: '/{ownerType}/{ownerId}/{scanId}/component/{hash}/transitiveViolations',
  component: TransitiveViolationsPage,
  data: {
    title: 'Transitive Policy Violations',
  },
});

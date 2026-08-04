/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import AddWaiverPageContainer from './AddWaiverPageContainer';
import RequestWaiverPage from './RequestWaiverPage';
import RequestWaiverReviewPage from './RequestWaiverReviewPage';
import FirewallReviewWaiverRequestPage from 'MainRoot/firewall/waiverRequests/FirewallReviewWaiverRequestPage';
import SidebarLayout from 'MainRoot/sidebarNav/SidebarLayout';
import WaiverDetailsContainer from './waiverDetails/WaiverDetailsContainer';
import { shellWrappedWaiverPage } from './waiversNexusOneShell';

// Add waiver
router.stateRegistry.register({
  name: 'addWaiver',
  url: '/addWaiver/{violationId}?comments&reasonId',
  component: shellWrappedWaiverPage(AddWaiverPageContainer),
  data: {
    title: 'Add Waiver',
    isDirty: ['addWaiver', 'isDirty'],
  },
});

// Request waiver
router.stateRegistry.register({
  name: 'requestWaiver',
  url: '/requestWaiver/{violationId}',
  component: shellWrappedWaiverPage(RequestWaiverPage),
  data: {
    title: 'Request Waiver',
    isDirty: ['requestWaiver', 'isDirty'],
  },
});

// Request waiver review
router.stateRegistry.register({
  name: 'requestWaiverReview',
  url: '/requestWaiverReview/{ownerType}/{ownerId}/{policyWaiverRequestId}',
  component: shellWrappedWaiverPage(RequestWaiverReviewPage),
  data: {
    title: 'Review Requested Waiver',
    isDirty: ['requestWaiver', 'isDirty'],
  },
});

// Waiver sidebar abstract state
router.stateRegistry.register({
  name: 'waiver',
  abstract: true,
  url: '/waiver',
  component: SidebarLayout,
});

// Waiver details
router.stateRegistry.register({
  name: 'waiver.details',
  url: '/{ownerType}/{ownerId}/{waiverId}?type&sidebarReference&sidebarId&page',
  component: WaiverDetailsContainer,
  data: {
    title: 'Waiver detail view',
  },
});

// Review a Firewall waiver request from within the LC/dashboard context.
// Uses FirewallReviewWaiverRequestPage (which handles repository-scoped waiver requests
// correctly) but registered outside the firewall parent state so it renders in the LC layout.
router.stateRegistry.register({
  name: 'dashboardFirewallWaiverRequestReview',
  url: '/dashboardFirewallWaiverRequestReview/{ownerType}/{ownerId}/{waiverRequestId}?origin',
  component: shellWrappedWaiverPage(FirewallReviewWaiverRequestPage),
  data: {
    title: 'Review Requested Waiver',
  },
});

// Request waiver update
router.stateRegistry.register({
  name: 'requestWaiverUpdate',
  url: '/requestWaiverUpdate/{ownerType}/{ownerId}/{policyWaiverRequestId}',
  component: shellWrappedWaiverPage(RequestWaiverPage),
  data: {
    title: 'Request Waiver',
    isDirty: ['requestWaiverUpdate', 'isDirty'],
  },
});

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import MailConfigContainer from './mail/MailConfigContainer';
import AutomaticSourceControlConfigurationContainer from './automaticSourceControlConfiguration/AutomaticSourceControlConfigurationContainer';
import ProxyConfigContainer from './proxy/ProxyConfigContainer';
import ScmOnboardingContainer from './scmOnboarding/ScmOnboardingContainer';
import LabsDataInsightsContainer from './labsDataInsights/LabsDataInsightsContainer';
import AdvancedSearchConfigContainer from './advancedSearch/AdvancedSearchConfigContainer';
import SuccessMetricsConfiguration from 'MainRoot/configuration/successMetricsConfiguration/SuccessMetricsConfiguration';
import SystemNoticeConfigurationContainer from './systemNoticeConfiguration/SystemNoticeConfigurationContainer';
import AutomaticApplicationsConfiguration from './automaticApplicationsConfiguration/AutomaticApplicationsConfigurationContainer';
import GettingStartedContainer from './gettingStarted/GettingStartedContainer';
import WaivedComponentUpgradesConfiguration from './waivedComponentUpgradesConfiguration/WaivedComponentUpgradesConfiguration';
import UserTokensConfiguration from './userTokensConfiguration/UserTokensConfiguration';
import RoiConfigurationPage from './roiConfiguration/RoiConfigurationPage';
import EditRoiConfigurationPage from './editRoiConfiguration/EditRoiConfigurationPage';
import UserActivityDetailsContainer from './userActivityOverview/UserActivityDetailsContainer';
import { submitData, DEPARTED_ACTION } from './gettingStarted/gettingStartedTelemetryServiceHelper';
import { isAuthorized } from '../util/permissionService';

export const GETTING_STARTED_STATE = 'gettingStarted';

// Data Insights
router.stateRegistry.register({
  name: 'dataInsights',
  url: '/dataInsights',
  component: LabsDataInsightsContainer,
  data: {
    title: 'Data Insights',
  },
});

// Mail Configuration
router.stateRegistry.register({
  name: 'mailConfig',
  url: '/mailConfig',
  component: MailConfigContainer,
  data: {
    title: 'Mail Config',
    isDirty: ['mailConfig', 'isDirty'],
  },
  resolve: {
    isAuthorized: function () {
      return isAuthorized(['CONFIGURE_SYSTEM']);
    },
  },
});

// Proxy Configuration
router.stateRegistry.register({
  name: 'proxyConfig',
  url: '/proxyConfig',
  component: ProxyConfigContainer,
  data: {
    title: 'Proxy',
    isDirty: ['proxyConfig', 'isDirty'],
  },
});

// Advanced Search Configuration
router.stateRegistry.register({
  name: 'advancedSearchConfig',
  url: '/advancedSearchConfig',
  component: AdvancedSearchConfigContainer,
  data: {
    title: 'Advanced Search Config',
    isDirty: ['advancedSearchConfig', 'viewState', 'isDirty'],
  },
  resolve: {
    isAuthorized: function () {
      return isAuthorized(['CONFIGURE_SYSTEM']);
    },
  },
});

// Success Metrics Configuration
router.stateRegistry.register({
  name: 'successMetricsConfiguration',
  url: '/successMetricsConfiguration',
  component: SuccessMetricsConfiguration,
  data: {
    title: 'Success Metrics',
    isDirty: ['successMetricsConfiguration', 'viewState', 'isDirty'],
  },
});

// Waived Component Upgrades Configuration
router.stateRegistry.register({
  name: 'waivedComponentUpgradesConfiguration',
  url: '/waivedComponentUpgradesConfiguration',
  component: WaivedComponentUpgradesConfiguration,
  data: {
    title: 'Success Metrics',
    isDirty: ['waivedComponentUpgradesConfiguration', 'isDirty'],
  },
});

// User Tokens Configuration
router.stateRegistry.register({
  name: 'userTokensConfiguration',
  url: '/userTokensConfiguration',
  component: UserTokensConfiguration,
  data: {
    title: 'User Tokens',
    isDirty: ['userTokensConfiguration', 'isDirty'],
  },
});

// System Notice Configuration
router.stateRegistry.register({
  name: 'systemNoticeConfiguration',
  url: '/systemNoticeConfiguration',
  component: SystemNoticeConfigurationContainer,
  data: {
    title: 'System Notice',
    isDirty: ['systemNoticeConfiguration', 'viewState', 'isDirty'],
  },
});

// SCM Onboarding
const scmOnboardingRouteCommonProps = {
  component: ScmOnboardingContainer,
  data: {
    title: 'Onboarding',
  },
};

router.stateRegistry.register({
  name: 'scmOnboarding',
  ...scmOnboardingRouteCommonProps,
  url: '/onboarding',
});

router.stateRegistry.register({
  name: 'scmOnboardingOrg',
  ...scmOnboardingRouteCommonProps,
  url: '/onboarding/{organizationId}',
});

// Automatic Source Control Configuration
router.stateRegistry.register({
  name: 'automaticSourceControlConfiguration',
  url: '/automaticSourceControlConfiguration',
  component: AutomaticSourceControlConfigurationContainer,
  data: {
    title: 'Automatic Source Control',
    isDirty: ['automaticSourceControlConfiguration', 'viewState', 'isDirty'],
  },
});

// Automatic Applications Configuration
router.stateRegistry.register({
  name: 'automaticApplicationsConfiguration',
  url: '/automaticApplicationsConfiguration',
  component: AutomaticApplicationsConfiguration,
  data: {
    title: 'Automatic Applications',
    isDirty: ['automaticApplicationsConfiguration', 'isDirty'],
  },
});

// Getting Started
router.stateRegistry.register({
  name: 'gettingStarted',
  url: '/gettingStarted',
  component: GettingStartedContainer,
  data: {
    title: 'Getting Started',
  },
});

// ROI Configuration
router.stateRegistry.register({
  name: 'roiConfiguration',
  url: '/roiConfiguration',
  component: RoiConfigurationPage,
  data: {
    title: 'ROI Configuration',
  },
});

router.stateRegistry.register({
  name: 'editRoiConfiguration',
  url: '/roiConfiguration/edit',
  component: EditRoiConfigurationPage,
  data: {
    title: 'Edit ROI Configuration',
  },
});

// User Activity Details
router.stateRegistry.register({
  name: 'userActivityDetails',
  url: '/users/activity/{username}',
  component: UserActivityDetailsContainer,
  data: {
    title: 'User Activity Details',
  },
  resolve: {
    isAuthorized: function () {
      return isAuthorized(['CONFIGURE_SYSTEM', 'ACCESS_AUDIT_LOG']);
    },
  },
});

// Track transitions from gettingStarted page for telemetry
router.transitionService.onFinish({ from: GETTING_STARTED_STATE }, (transition) => {
  return submitData(DEPARTED_ACTION, {
    departedTo: transition.to().name,
  });
});

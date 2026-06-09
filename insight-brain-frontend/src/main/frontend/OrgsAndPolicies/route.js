/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import { OwnerManagerViewWrapper } from 'MainRoot/owner.manager/state/OwnerManagerViewWrapper';
import { OwnerManagerEditWrapper } from 'MainRoot/owner.manager/state/OwnerManagerEditWrapper';
import { selectIsDirty as policyEditorSelectIsDirty } from './policySelectors';
import { selectLabelsIsDirty } from './labelsSelectors';
import { selectLicenseThreatGroupIsDirty } from './licenseThreatGroupSelectors';
import { selectIsDirty as applicationCategoriesSelectIsDirty } from './createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import { selectIsDirty as assignApplicationCategoriesSelectIsDirty } from './assignApplicationCategoriesSelectors';

// Import React components for routes
import OwnersTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import OwnerSummary from 'MainRoot/OrgsAndPolicies/ownerSummary/OwnerSummary';
import RepositoriesSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesSummaryView';
import RepositoryManagerSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerSummaryView';
import RepositorySummaryView from 'MainRoot/OrgsAndPolicies/repositorySummaryView/RepositorySummaryView';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import AccessPage from 'MainRoot/OrgsAndPolicies/access/AccessPage';
import LegacyViolationsEditor from 'MainRoot/OrgsAndPolicies/legacyViolationsEditor/LegacyViolationsEditor';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/continuousMonitoringEditor/ContinuousMonitoringEditor';
import ProprietaryComponentConfiguration from 'MainRoot/OrgsAndPolicies/proprietaryComponentConfig/ProprietaryComponentConfiguration';
import SourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlConfiguration';
import AutoWaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';
import AutoWaiverDetails from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverDetails';
import PublicDataSourcesEditor from 'MainRoot/OrgsAndPolicies/publicDataSources/PublicDataSourcesEditor';
import CreateEditApplicationCategory from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/CreateEditApplicationCategory';
import LicenseThreatGroupEditor from 'MainRoot/OrgsAndPolicies/licenseThreatGroupEditor/LicenseThreatGroupEditor';
import DataRetentionEditor from 'MainRoot/OrgsAndPolicies/dataRetentionEditor/DataRetentionEditor';
import WaiverExpirationNotificationEditor from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationEditor/WaiverExpirationNotificationEditor';
import AssignAppCategory from 'MainRoot/OrgsAndPolicies/assignAppCategory/AssignAppCategory';
import ManageGitHubApps from 'MainRoot/OrgsAndPolicies/manageGitHubApps/ManageGitHubApps';

// Abstract parent state
router.stateRegistry.register({
  name: 'management',
  url: '/management',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'management.view',
  url: '/view',
  component: OwnerManagerViewWrapper,
  data: {
    title: 'Management',
  },
});

router.stateRegistry.register({
  name: 'management.tree',
  url: '/tree',
  component: OwnersTreePage,
  data: {
    title: 'Inheritance Hierarchy',
  },
});

router.stateRegistry.register({
  name: 'management.edit',
  abstract: true,
  component: UIView,
});

// Dynamic routes for owner types
const ownerTypes = [
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
  {
    type: 'repository_container',
    name: 'Repository Managers',
    id: 'repositoryContainerId',
    component: RepositoriesSummaryView,
    hideOverflowY: true,
  },
  {
    type: 'virtual_repository_container',
    name: 'Virtual Repository Managers',
    id: 'repositoryContainerId',
    component: RepositoriesSummaryView,
    hideOverflowY: true,
  },
  {
    type: 'repository_manager',
    name: 'Repository manager',
    id: 'repositoryManagerId',
    component: RepositoryManagerSummaryView,
    hideOverflowY: true,
  },
  {
    type: 'repository',
    name: 'Repository',
    id: 'repositoryId',
    component: RepositorySummaryView,
    hideOverflowY: true,
  },
];

ownerTypes.forEach((ownerType) => {
  router.stateRegistry.register({
    name: `management.view.${ownerType.type}`,
    url: `/${ownerType.type}/{${ownerType.id}}`,
    component: ownerType.component,
    data: {
      title: `${ownerType.name} Management`,
      viewportSized: true,
      hideOverflowY: ownerType.hideOverflowY,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}`,
    url: `/edit/${ownerType.type}/{${ownerType.id}}`,
    component: OwnerManagerEditWrapper,
    data: {
      title: `${ownerType.name} Management`,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.label`,
    url: '/label/{labelId}',
    component: CreateComponentLabel,
    data: {
      title: `${ownerType.name} Labels`,
      isDirty: selectLabelsIsDirty,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.create-label`,
    url: '/label',
    component: CreateComponentLabel,
    data: {
      title: `${ownerType.name} Labels`,
      isDirty: selectLabelsIsDirty,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.policy`,
    url: '/policy/{policyId}',
    component: PolicyEditor,
    data: {
      title: `${ownerType.name} Policy`,
      isDirty: policyEditorSelectIsDirty,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.create-policy`,
    url: '/policy',
    component: PolicyEditor,
    data: {
      title: `${ownerType.name} Policy`,
      isDirty: policyEditorSelectIsDirty,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.add-access`,
    url: '/access',
    component: AccessPage,
    data: {
      title: `${ownerType.name} Access`,
      isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.edit-access`,
    url: '/access/{roleId}',
    component: AccessPage,
    data: {
      title: `${ownerType.name} Access`,
      isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.legacy-violations`,
    url: '/legacyViolations',
    component: LegacyViolationsEditor,
    data: {
      title: `${ownerType.name} Legacy Violations`,
      isDirty: ['orgsAndPolicies', 'legacyViolations', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.monitor-policy`,
    url: '/monitoring',
    component: ContinuousMonitoringEditor,
    data: {
      title: `${ownerType.name} Continuous Monitoring`,
      isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.proprietary-config-policy`,
    url: '/proprietary',
    component: ProprietaryComponentConfiguration,
    data: {
      title: `${ownerType.name} Proprietary Components`,
      isDirty: ['orgsAndPolicies', 'proprietary', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.edit-source-control`,
    url: '/source-control?githubAppId',
    component: SourceControlConfiguration,
    data: {
      title: 'Source Control',
      isDirty: ['orgsAndPolicies', 'sourceControlConfiguration', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.manage-github-apps`,
    url: '/manage-github-apps',
    component: ManageGitHubApps,
    data: {
      title: 'Manage GitHub Apps',
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.auto-waivers-config`,
    url: '/autowaivers',
    component: AutoWaiversConfiguration,
    data: {
      title: `${ownerType.name} Auto Waivers Configuration`,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.auto-waiver-details`,
    url: '/ownertype/{ownerType}/autowaiverowner/{autoWaiverOwnerId}/autowaiver/{autoWaiverId}',
    component: AutoWaiverDetails,
    data: {
      title: `${ownerType.name} Auto Waiver Details`,
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.public-data-sources-editor`,
    url: '/publicDataSourcesEditor',
    component: PublicDataSourcesEditor,
    data: {
      title: `${ownerType.name} Public Data Sources`,
      isDirty: ['orgsAndPolicies', 'publicDataSources', 'isDirty'],
    },
  });

  router.stateRegistry.register({
    name: `management.edit.${ownerType.type}.edit-waiver-expiration-notification`,
    url: '/waiver-expiration-notification',
    component: WaiverExpirationNotificationEditor,
    data: {
      title: `${ownerType.name} Waiver Expiration Notifications`,
      isDirty: ['orgsAndPolicies', 'waiverExpirationNotification', 'isDirty'],
    },
  });
});

// Owner-type-specific routes for organization
router.stateRegistry.register({
  name: 'management.edit.organization.category',
  url: '/category/{categoryId}',
  component: CreateEditApplicationCategory,
  data: {
    title: 'Organization Category',
    isDirty: applicationCategoriesSelectIsDirty,
  },
});

router.stateRegistry.register({
  name: 'management.edit.organization.create-category',
  url: '/category',
  component: CreateEditApplicationCategory,
  data: {
    title: 'Organization Category',
    isDirty: applicationCategoriesSelectIsDirty,
  },
});

router.stateRegistry.register({
  name: 'management.edit.organization.create-license-threat-group',
  url: '/licenseThreatGroup',
  component: LicenseThreatGroupEditor,
  data: {
    title: 'Organization License Threat Group',
    isDirty: selectLicenseThreatGroupIsDirty,
  },
});

router.stateRegistry.register({
  name: 'management.edit.organization.edit-license-threat-group',
  url: '/licenseThreatGroup/{licenseThreatGroupId}',
  component: LicenseThreatGroupEditor,
  data: {
    title: 'Organization License Threat Group',
    isDirty: selectLicenseThreatGroupIsDirty,
  },
});

router.stateRegistry.register({
  name: 'management.edit.organization.edit-data-retention',
  url: '/data-retention',
  component: DataRetentionEditor,
  data: {
    title: 'Organization Data Retention',
    isDirty: ['orgsAndPolicies', 'retention', 'isDirty'],
  },
});

// Owner-type-specific route for application
router.stateRegistry.register({
  name: 'management.edit.application.category',
  url: '/category',
  component: AssignAppCategory,
  data: {
    title: 'Application Categories',
    isDirty: assignApplicationCategoriesSelectIsDirty,
  },
});

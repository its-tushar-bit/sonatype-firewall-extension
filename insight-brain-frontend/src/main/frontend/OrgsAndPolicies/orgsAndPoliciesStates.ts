/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ReactStateDeclaration, UIView } from '@uirouter/react';
import { OwnerManagerViewWrapper } from 'MainRoot/owner.manager/state/OwnerManagerViewWrapper';
import { OwnerManagerEditWrapper } from 'MainRoot/owner.manager/state/OwnerManagerEditWrapper';
import { selectIsDirty as policyEditorSelectIsDirty } from './policySelectors';
import { selectLabelsIsDirty } from './labelsSelectors';
import { selectLicenseThreatGroupIsDirty } from './licenseThreatGroupSelectors';
import { selectIsDirty as applicationCategoriesSelectIsDirty } from './createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import { selectIsDirty as assignApplicationCategoriesSelectIsDirty } from './assignApplicationCategoriesSelectors';

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

/**
 * Single source of truth for the `management.*` (Orgs and Policies) state tree.
 *
 * Classic's `OrgsAndPolicies/route.js` and the Nexus One embedded mount (`nexus-one/routes.tsx`)
 * both register every one of these under their own bundle's router instance, from this one array,
 * so the two can't drift apart (mirrors `legal/legalDeepLinkStates.ts`).
 *
 * Authoritative note on chrome wrapping (nexus-one/routes.tsx points here): the three chrome-carrying
 * components in {@link ORGS_AND_POLICIES_CHROME_COMPONENTS} - {@link OwnerManagerViewWrapper}
 * (`management.view`), {@link OwnerManagerEditWrapper} (`management.edit.{ownerType}`) and
 * {@link OwnersTreePage} (`management.tree`) - each render their own sidebar + breadcrumb + nested
 * `<UIView />`. In the Nexus One bundle those get wrapped once in the embed mount; every deeper state
 * renders inside that `<UIView />` and registers unwrapped.
 */
export interface OrgsAndPoliciesStateDef {
  readonly name: string;
  readonly url?: string;
  readonly component: React.ComponentType;
  readonly data?: Record<string, unknown>;
  readonly abstract?: boolean;
}

export const ORGS_AND_POLICIES_CHROME_COMPONENTS: ReadonlySet<React.ComponentType> = new Set([
  OwnerManagerViewWrapper,
  OwnerManagerEditWrapper,
  OwnersTreePage,
]);

interface OwnerType {
  readonly type: string;
  readonly name: string;
  readonly id: string;
  readonly component: React.ComponentType;
  readonly hideOverflowY?: boolean;
}

const ownerTypes: readonly OwnerType[] = [
  { type: 'organization', name: 'Organization', id: 'organizationId', component: OwnerSummary },
  { type: 'application', name: 'Application', id: 'applicationPublicId', component: OwnerSummary },
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

function ownerTypeStates(ownerType: OwnerType): OrgsAndPoliciesStateDef[] {
  return [
    {
      name: `management.view.${ownerType.type}`,
      url: `/${ownerType.type}/{${ownerType.id}}`,
      component: ownerType.component,
      data: {
        title: `${ownerType.name} Management`,
        viewportSized: true,
        hideOverflowY: ownerType.hideOverflowY,
      },
    },
    {
      name: `management.edit.${ownerType.type}`,
      url: `/edit/${ownerType.type}/{${ownerType.id}}`,
      component: OwnerManagerEditWrapper,
      data: { title: `${ownerType.name} Management` },
    },
    {
      name: `management.edit.${ownerType.type}.label`,
      url: '/label/{labelId}',
      component: CreateComponentLabel,
      data: { title: `${ownerType.name} Labels`, isDirty: selectLabelsIsDirty },
    },
    {
      name: `management.edit.${ownerType.type}.create-label`,
      url: '/label',
      component: CreateComponentLabel,
      data: { title: `${ownerType.name} Labels`, isDirty: selectLabelsIsDirty },
    },
    {
      name: `management.edit.${ownerType.type}.policy`,
      url: '/policy/{policyId}',
      component: PolicyEditor,
      data: { title: `${ownerType.name} Policy`, isDirty: policyEditorSelectIsDirty },
    },
    {
      name: `management.edit.${ownerType.type}.create-policy`,
      url: '/policy',
      component: PolicyEditor,
      data: { title: `${ownerType.name} Policy`, isDirty: policyEditorSelectIsDirty },
    },
    {
      name: `management.edit.${ownerType.type}.add-access`,
      url: '/access',
      component: AccessPage,
      data: { title: `${ownerType.name} Access`, isDirty: ['orgsAndPolicies', 'access', 'isDirty'] },
    },
    {
      name: `management.edit.${ownerType.type}.edit-access`,
      url: '/access/{roleId}',
      component: AccessPage,
      data: { title: `${ownerType.name} Access`, isDirty: ['orgsAndPolicies', 'access', 'isDirty'] },
    },
    {
      name: `management.edit.${ownerType.type}.legacy-violations`,
      url: '/legacyViolations',
      component: LegacyViolationsEditor,
      data: {
        title: `${ownerType.name} Legacy Violations`,
        isDirty: ['orgsAndPolicies', 'legacyViolations', 'isDirty'],
      },
    },
    {
      name: `management.edit.${ownerType.type}.monitor-policy`,
      url: '/monitoring',
      component: ContinuousMonitoringEditor,
      data: {
        title: `${ownerType.name} Continuous Monitoring`,
        isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
      },
    },
    {
      name: `management.edit.${ownerType.type}.proprietary-config-policy`,
      url: '/proprietary',
      component: ProprietaryComponentConfiguration,
      data: {
        title: `${ownerType.name} Proprietary Components`,
        isDirty: ['orgsAndPolicies', 'proprietary', 'isDirty'],
      },
    },
    {
      name: `management.edit.${ownerType.type}.edit-source-control`,
      url: '/source-control?githubAppId',
      component: SourceControlConfiguration,
      data: { title: 'Source Control', isDirty: ['orgsAndPolicies', 'sourceControlConfiguration', 'isDirty'] },
    },
    {
      name: `management.edit.${ownerType.type}.manage-github-apps`,
      url: '/manage-github-apps',
      component: ManageGitHubApps,
      data: { title: 'Manage GitHub Apps' },
    },
    {
      name: `management.edit.${ownerType.type}.auto-waivers-config`,
      url: '/autowaivers',
      component: AutoWaiversConfiguration,
      data: { title: `${ownerType.name} Auto Waivers Configuration` },
    },
    {
      name: `management.edit.${ownerType.type}.auto-waiver-details`,
      url: '/ownertype/{ownerType}/autowaiverowner/{autoWaiverOwnerId}/autowaiver/{autoWaiverId}',
      component: AutoWaiverDetails,
      data: { title: `${ownerType.name} Auto Waiver Details` },
    },
    {
      name: `management.edit.${ownerType.type}.public-data-sources-editor`,
      url: '/publicDataSourcesEditor',
      component: PublicDataSourcesEditor,
      data: {
        title: `${ownerType.name} Public Data Sources`,
        isDirty: ['orgsAndPolicies', 'publicDataSources', 'isDirty'],
      },
    },
    {
      name: `management.edit.${ownerType.type}.edit-waiver-expiration-notification`,
      url: '/waiver-expiration-notification',
      component: WaiverExpirationNotificationEditor,
      data: {
        title: `${ownerType.name} Waiver Expiration Notifications`,
        isDirty: ['orgsAndPolicies', 'waiverExpirationNotification', 'isDirty'],
      },
    },
  ];
}

/**
 * Order matches Classic's `OrgsAndPolicies/route.js`: abstract parents first so UI-Router's
 * nested-url resolution has them registered before their children.
 */
export const ORGS_AND_POLICIES_STATES: readonly OrgsAndPoliciesStateDef[] = [
  { name: 'management', url: '/management', abstract: true, component: UIView },
  { name: 'management.view', url: '/view', component: OwnerManagerViewWrapper, data: { title: 'Management' } },
  { name: 'management.tree', url: '/tree', component: OwnersTreePage, data: { title: 'Inheritance Hierarchy' } },
  { name: 'management.edit', abstract: true, component: UIView },

  ...ownerTypes.flatMap(ownerTypeStates),

  {
    name: 'management.edit.organization.category',
    url: '/category/{categoryId}',
    component: CreateEditApplicationCategory,
    data: { title: 'Organization Category', isDirty: applicationCategoriesSelectIsDirty },
  },
  {
    name: 'management.edit.organization.create-category',
    url: '/category',
    component: CreateEditApplicationCategory,
    data: { title: 'Organization Category', isDirty: applicationCategoriesSelectIsDirty },
  },
  {
    name: 'management.edit.organization.create-license-threat-group',
    url: '/licenseThreatGroup',
    component: LicenseThreatGroupEditor,
    data: { title: 'Organization License Threat Group', isDirty: selectLicenseThreatGroupIsDirty },
  },
  {
    name: 'management.edit.organization.edit-license-threat-group',
    url: '/licenseThreatGroup/{licenseThreatGroupId}',
    component: LicenseThreatGroupEditor,
    data: { title: 'Organization License Threat Group', isDirty: selectLicenseThreatGroupIsDirty },
  },
  {
    name: 'management.edit.organization.edit-data-retention',
    url: '/data-retention',
    component: DataRetentionEditor,
    data: { title: 'Organization Data Retention', isDirty: ['orgsAndPolicies', 'retention', 'isDirty'] },
  },

  {
    name: 'management.edit.application.category',
    url: '/category',
    component: AssignAppCategory,
    data: { title: 'Application Categories', isDirty: assignApplicationCategoriesSelectIsDirty },
  },
];

/**
 * Builds the `router.stateRegistry.register(...)` argument for one management state. Both the Classic
 * `route.js` and the Nexus One `routes.tsx` register from {@link ORGS_AND_POLICIES_STATES} through
 * this one mapping; `routes.tsx` passes a mount-wrapped `component` for the chrome states.
 */
export function toManagementStateRegistration(
  stateDef: OrgsAndPoliciesStateDef,
  component: React.ComponentType = stateDef.component
): ReactStateDeclaration {
  return {
    name: stateDef.name,
    component,
    ...(stateDef.url !== undefined ? { url: stateDef.url } : {}),
    ...(stateDef.data ? { data: stateDef.data } : {}),
    ...(stateDef.abstract ? { abstract: true } : {}),
  } as ReactStateDeclaration;
}

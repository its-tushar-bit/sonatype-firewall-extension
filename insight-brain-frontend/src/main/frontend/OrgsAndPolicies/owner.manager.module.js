/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import OwnerSideNav from 'MainRoot/OrgsAndPolicies/ownerSideNav/OwnerSideNav';
import OwnersTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import InsufficientPermissionOwnerHierarchyTree from 'MainRoot/OrgsAndPolicies/insufficientPermissionOwnerHierarchyTree/InsufficientPermissionOwnerHierarchyTree';
import OwnerSummary from 'MainRoot/OrgsAndPolicies/ownerSummary/OwnerSummary';
import viewTemplate from 'MainRoot/owner.manager/state/owner.manager.view.html';
import editTemplate from 'MainRoot/owner.manager/state/owner.manager.edit.html';
import RepositoriesSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesSummaryView';
import RepositoryManagerSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerSummaryView';
import RepositorySummaryView from 'MainRoot/OrgsAndPolicies/repositorySummaryView/RepositorySummaryView';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/сontinuousMonitoringEditor/ContinuousMonitoringEditor';
import LicenseThreatGroupEditor from 'MainRoot/OrgsAndPolicies/licenseThreatGroupEditor/LicenseThreatGroupEditor';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import CreateEditApplicationCategory from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/CreateEditApplicationCategory';
import ProprietaryComponentConfiguration from 'MainRoot/OrgsAndPolicies/proprietaryComponentConfig/ProprietaryComponentConfiguration';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import AccessPage from 'MainRoot/OrgsAndPolicies/access/AccessPage';
import AssignAppCategory from 'MainRoot/OrgsAndPolicies/assignAppCategory/AssignAppCategory';
import LegacyViolationsEditor from 'MainRoot/OrgsAndPolicies/legacyViolationsEditor/LegacyViolationsEditor';
import DataRetentionEditor from 'MainRoot/OrgsAndPolicies/dataRetentionEditor/DataRetentionEditor';
import OwnerDetailSidebar from 'MainRoot/OrgsAndPolicies/navigation/OwnerDetailSidebar';
import RepositoriesPills from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesPills';
import SourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlConfiguration';
import OwnerSummaryPills from 'MainRoot/OrgsAndPolicies/OwnerSummaryPills/OwnerSummaryPills';
import ActionDropdown from 'MainRoot/OrgsAndPolicies/actionDropdown/ActionDropdown';
import { selectIsDirty as policyEditorSelectIsDirty } from 'MainRoot/OrgsAndPolicies/policySelectors';
import AutoWaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';
import AutoWaiverDetails from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverDetails';
import PublicDataSourcesEditor from 'MainRoot/OrgsAndPolicies/publicDataSources/PublicDataSourcesEditor';
import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';

export default angular
  .module('owner.manager.module', ['ui.router'])
  .component('ownerSideNav', iqReact2Angular(OwnerSideNav, [], ['$state']))
  .component('ownersTreePage', iqReact2Angular(OwnersTreePage, [], ['$state']))
  .component(
    'insufficientPermissionOwnerHierarchyTree',
    iqReact2Angular(InsufficientPermissionOwnerHierarchyTree, [], ['$state'])
  )
  .component('ownerSummary', iqReact2Angular(OwnerSummary, [], ['$state']))
  .component('repositoriesSummaryView', iqReact2Angular(RepositoriesSummaryView, [], ['$state']))
  .component('repositoryManagerSummaryView', iqReact2Angular(RepositoryManagerSummaryView, [], ['$state']))
  .component('repositorySummaryView', iqReact2Angular(RepositorySummaryView, [], ['$state']))
  .component('licenseThreatGroupEditor', iqReact2Angular(LicenseThreatGroupEditor, [], []))
  .component('continuousMonitoring', iqReact2Angular(ContinuousMonitoringEditor, [], []))
  .component('createComponentLabel', iqReact2Angular(CreateComponentLabel, [], ['$state']))
  .component('accessPage', iqReact2Angular(AccessPage, [], ['$state']))
  .component('policyEditor', iqReact2Angular(PolicyEditor, [], ['$state']))
  .component('proprietaryComponentConfiguration', iqReact2Angular(ProprietaryComponentConfiguration, [], []))
  .component('createEditApplicationCategory', iqReact2Angular(CreateEditApplicationCategory, [], []))
  .component('assignAppCategory', iqReact2Angular(AssignAppCategory, [], []))
  .component('legacyViolationsEditor', iqReact2Angular(LegacyViolationsEditor, [], []))
  .component('dataRetentionEditor', iqReact2Angular(DataRetentionEditor, [], []))
  .component('ownerSummaryPills', iqReact2Angular(OwnerSummaryPills, [], []))
  .component('sourceControlConfiguration', iqReact2Angular(SourceControlConfiguration, [], []))
  .component('actionDropdown', iqReact2Angular(ActionDropdown, [], ['$state']))
  .component('ownerDetailSidebar', iqReact2Angular(OwnerDetailSidebar, [], ['$state']))
  .component('repositoriesPills', iqReact2Angular(RepositoriesPills, [], []))
  .component('autoWaiversConfiguration', iqReact2Angular(AutoWaiversConfiguration, [], ['$state']))
  .component('autoWaiverDetails', iqReact2Angular(AutoWaiverDetails, [], ['$state']))
  .component('publicDataSourcesEditor', iqReact2Angular(PublicDataSourcesEditor, [], ['$state']))
  .component('menuBarStatefulBreadcrumb', iqReact2Angular(MenuBarStatefulBreadcrumb, [], ['$state']))
  .config([
    '$stateProvider',
    function ($stateProvider) {
      var ownerTypes = [
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
        {
          type: 'repository_container',
          name: 'Repository Managers',
          id: 'repositoryContainerId',
          component: 'repositoriesSummaryView',
          hideOverflowY: true,
        },
        {
          type: 'repository_manager',
          name: 'Repository manager',
          id: 'repositoryManagerId',
          component: 'repositoryManagerSummaryView',
          hideOverflowY: true,
        },
        {
          type: 'repository',
          name: 'Repository',
          id: 'repositoryId',
          component: 'repositorySummaryView',
          hideOverflowY: true,
        },
      ];

      $stateProvider
        .state('management', {
          url: '/management',
          abstract: true,
        })
        .state('management.view', {
          url: '/view',
          template: viewTemplate,
          data: {
            title: 'Management',
          },
        })
        .state('management.tree', {
          url: '/tree',
          data: {
            title: 'Inheritance Hierarchy',
          },
          component: 'ownersTreePage',
        })
        .state('management.edit', {
          abstract: true,
        });

      ownerTypes.forEach(function (ownerType) {
        $stateProvider
          .state('management.view.' + ownerType.type, {
            url: '/' + ownerType.type + '/{' + ownerType.id + '}',
            data: {
              title: ownerType.name + ' Management',
              viewportSized: true,
              hideOverflowY: ownerType.hideOverflowY,
            },
            component: ownerType.component,
          })
          .state('management.edit.' + ownerType.type, {
            url: '/edit/' + ownerType.type + '/{' + ownerType.id + '}',
            data: {
              title: ownerType.name + ' Management',
            },
            template: editTemplate,
          })
          .state('management.edit.' + ownerType.type + '.label', {
            url: '/label/{labelId}',
            data: {
              title: ownerType.name + ' Labels',
              isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
            },
            component: 'createComponentLabel',
          })
          .state('management.edit.' + ownerType.type + '.create-label', {
            url: '/label',
            data: {
              title: ownerType.name + ' Labels',
              isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
            },
            component: 'createComponentLabel',
          })
          .state('management.edit.' + ownerType.type + '.policy', {
            url: '/policy/{policyId}',
            data: {
              title: ownerType.name + ' Policy',
              isDirty: policyEditorSelectIsDirty,
            },
            component: 'policyEditor',
          })
          .state('management.edit.' + ownerType.type + '.create-policy', {
            url: '/policy',
            data: {
              title: ownerType.name + ' Policy',
              isDirty: policyEditorSelectIsDirty,
            },
            component: 'policyEditor',
          })
          .state('management.edit.' + ownerType.type + '.add-access', {
            url: '/access',
            data: {
              title: ownerType.name + ' Access',
              isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
            },
            component: 'accessPage',
          })
          .state('management.edit.' + ownerType.type + '.edit-access', {
            url: '/access/{roleId}',
            data: {
              title: ownerType.name + ' Access',
              isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
            },
            component: 'accessPage',
          })
          .state('management.edit.' + ownerType.type + '.legacy-violations', {
            url: '/legacyViolations',
            data: {
              title: ownerType.name + ' Legacy Violations',
              isDirty: ['orgsAndPolicies', 'legacyViolations', 'isDirty'],
            },
            component: 'legacyViolationsEditor',
          })
          .state('management.edit.' + ownerType.type + '.monitor-policy', {
            url: '/monitoring',
            data: {
              title: ownerType.name + ' Continuous Monitoring',
              isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
            },
            component: 'continuousMonitoring',
          })
          .state('management.edit.' + ownerType.type + '.proprietary-config-policy', {
            url: '/proprietary',
            data: {
              title: ownerType.name + ' Proprietary Components',
              isDirty: ['orgsAndPolicies', 'proprietary', 'isDirty'],
            },
            component: 'proprietaryComponentConfiguration',
          })
          .state('management.edit.' + ownerType.type + '.edit-source-control', {
            url: '/source-control',
            data: {
              title: 'Source Control',
              isDirty: ['orgsAndPolicies', 'sourceControlConfiguration', 'isDirty'],
            },
            component: 'sourceControlConfiguration',
          })
          .state('management.edit.' + ownerType.type + '.auto-waivers-config', {
            url: '/autowaivers',
            data: {
              title: ownerType.name + ' Auto Waivers Configuration',
            },
            component: 'autoWaiversConfiguration',
          })
          .state('management.edit.' + ownerType.type + '.auto-waiver-details', {
            url: `/ownertype/{ownerType}/autowaiverowner/{autoWaiverOwnerId}/autowaiver/{autoWaiverId}`,
            data: {
              title: ownerType.name + ' Auto Waiver Details',
            },
            component: 'autoWaiverDetails',
          })
          .state('management.edit.' + ownerType.type + '.public-data-sources-editor', {
            url: '/publicDataSourcesEditor',
            data: {
              title: ownerType.name + ' Public Data Sources',
              isDirty: ['orgsAndPolicies', 'publicDataSources', 'isDirty'],
            },
            component: 'publicDataSourcesEditor',
          });
      });

      $stateProvider
        .state('management.edit.organization.category', {
          url: '/category/{categoryId}',
          data: {
            title: 'Organization Category',
            isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
          },
          component: 'createEditApplicationCategory',
        })
        .state('management.edit.organization.create-category', {
          url: '/category',
          data: {
            title: 'Organization Category',
            isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
          },
          component: 'createEditApplicationCategory',
        })
        .state('management.edit.application.category', {
          data: {
            title: 'Application Categories',
            isDirty: ['orgsAndPolicies', 'applicationCategories', 'assign', 'isDirty'],
          },
          url: '/category',
          component: 'assignAppCategory',
        })
        .state('management.edit.organization.create-license-threat-group', {
          data: {
            title: 'Organization License Threat Group',
            isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
          },
          url: '/licenseThreatGroup',
          component: 'licenseThreatGroupEditor',
        })
        .state('management.edit.organization.edit-license-threat-group', {
          data: {
            title: 'Organization License Threat Group',
            isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
          },
          url: '/licenseThreatGroup/{licenseThreatGroupId}',
          component: 'licenseThreatGroupEditor',
        })
        .state('management.edit.organization.edit-data-retention', {
          url: '/data-retention',
          data: {
            title: 'Organization Data Retention',
            isDirty: ['orgsAndPolicies', 'retention', 'isDirty'],
          },
          component: 'dataRetentionEditor',
        });
    },
  ]);

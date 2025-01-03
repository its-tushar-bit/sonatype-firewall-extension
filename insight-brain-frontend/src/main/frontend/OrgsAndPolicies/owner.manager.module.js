/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import CLMLocationModule from '../util/CLMLocation';
import utilityServicesModule from '../utility/services/utility.services.module';
import utilityModule from '../utility/utility.module';
import permissionServiceModule from '../utilAngular/PermissionService';
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
import WaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';

export default angular
  .module('owner.manager.module', [
    'ui.bootstrap',
    'ui.router',
    utilityModule.name,
    permissionServiceModule.name,
    CLMLocationModule.name,
    utilityServicesModule.name,
  ])
  .component('ownerSideNav', iqReact2Angular(OwnerSideNav, [], ['$ngRedux', '$state']))
  .component('ownersTreePage', iqReact2Angular(OwnersTreePage, [], ['$ngRedux', '$state']))
  .component(
    'insufficientPermissionOwnerHierarchyTree',
    iqReact2Angular(InsufficientPermissionOwnerHierarchyTree, [], ['$ngRedux', '$state'])
  )
  .component('ownerSummary', iqReact2Angular(OwnerSummary, [], ['$ngRedux', '$state']))
  .component('repositoriesSummaryView', iqReact2Angular(RepositoriesSummaryView, [], ['$ngRedux', '$state']))
  .component('repositoryManagerSummaryView', iqReact2Angular(RepositoryManagerSummaryView, [], ['$ngRedux', '$state']))
  .component('repositorySummaryView', iqReact2Angular(RepositorySummaryView, [], ['$ngRedux', '$state']))
  .component('licenseThreatGroupEditor', iqReact2Angular(LicenseThreatGroupEditor, [], ['$ngRedux']))
  .component('continuousMonitoring', iqReact2Angular(ContinuousMonitoringEditor, [], ['$ngRedux']))
  .component('autoWaiversConfiguration', iqReact2Angular(WaiversConfiguration, [], ['$ngRedux']))
  .component('createComponentLabel', iqReact2Angular(CreateComponentLabel, [], ['$ngRedux', '$state']))
  .component('accessPage', iqReact2Angular(AccessPage, [], ['$ngRedux', '$state']))
  .component('policyEditor', iqReact2Angular(PolicyEditor, [], ['$ngRedux', '$state']))
  .component('proprietaryComponentConfiguration', iqReact2Angular(ProprietaryComponentConfiguration, [], ['$ngRedux']))
  .component('createEditApplicationCategory', iqReact2Angular(CreateEditApplicationCategory, [], ['$ngRedux']))
  .component('assignAppCategory', iqReact2Angular(AssignAppCategory, [], ['$ngRedux']))
  .component('legacyViolationsEditor', iqReact2Angular(LegacyViolationsEditor, [], ['$ngRedux']))
  .component('dataRetentionEditor', iqReact2Angular(DataRetentionEditor, [], ['$ngRedux']))
  .component('ownerSummaryPills', iqReact2Angular(OwnerSummaryPills, [], ['$ngRedux']))
  .component('sourceControlConfiguration', iqReact2Angular(SourceControlConfiguration, [], ['$ngRedux']))
  .component('actionDropdown', iqReact2Angular(ActionDropdown, [], ['$ngRedux', '$state']))
  .component('ownerDetailSidebar', iqReact2Angular(OwnerDetailSidebar, [], ['$ngRedux', '$state']))
  .component('repositoriesPills', iqReact2Angular(RepositoriesPills, [], []))
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
          .state('management.edit.' + ownerType.type + '.edit-waivers', {
            url: '/waivers',
            data: {
              title: ownerType.name + ' Waivers Configuration',
            },
            component: 'autoWaiversConfiguration',
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

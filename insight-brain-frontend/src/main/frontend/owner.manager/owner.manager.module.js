/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import formsModule from '../FormsModule';
import angularCommonModule from '../utilAngular/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import utilityDirectivesModule from '../utility/directives/utility.directives.module';
import utilityServicesModule from '../utility/services/utility.services.module';
import utilityModule from '../utility/utility.module';
import permissionServiceModule from '../utilAngular/PermissionService';
import validatorsModule from '../utilAngular/Validators';
import storesModule from '../utilAngular/Stores';
import roleMembershipModule from '../role.membership/role.membership.module';
import SameOwnerStateNavigationService from './utility/same.owner.state.navigation.service';
import OwnerSideNav from 'MainRoot/OrgsAndPolicies/ownerSideNav/OwnerSideNav';
import OwnersTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import InsufficientPermissionOwnerHierarchyTree from 'MainRoot/OrgsAndPolicies/insufficientPermissionOwnerHierarchyTree/InsufficientPermissionOwnerHierarchyTree';
import MonitoredStageService from './utility/monitored.stage.service';
import OwnerImageDirective from './summary/owner.image.directive';
import OwnerSummaryController from './summary/owner.summary.controller';
import NumberInputWithStringValue from './utility/number.input.with.string.value';
import SameOwnerEditSref from './utility/same.owner.edit.sref.directive';
import SameOwnerViewSref from './utility/same.owner.view.sref.directive';
import sourceControlModule from './source.control/module';
import viewTemplate from './state/owner.manager.view.html';
import editTemplate from './state/owner.manager.edit.html';
import summaryViewTemplate from './summary/owner.summary.view.html';
import SourceControlService from './source.control/source.control.service';
import RepositoriesSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesSummaryView';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/сontinuousMonitoringEditor/ContinuousMonitoringEditor';
import LicenseThreatGroupEditor from 'MainRoot/OrgsAndPolicies/licenseThreatGroupEditor/LicenseThreatGroupEditor';
import LabelsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/labelsTile/LabelsTile';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import CreateEditApplicationCategory from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/CreateEditApplicationCategory';
import ProprietaryComponentConfiguration from 'MainRoot/OrgsAndPolicies/proprietaryComponentConfig/ProprietaryComponentConfiguration';
import OwnerSummaryTilesContainerController from './summary/owner.summary.tiles.container.controller';
import OwnerSummaryPills from 'MainRoot/OrgsAndPolicies/OwnerSummaryPills/OwnerSummaryPills';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import PolicyGrandfatheringTile from 'MainRoot/OrgsAndPolicies/ownerSummary/PolicyGrandfatheringTile';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';
import ProprietaryComponentConfigurationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ProprietaryComponentConfigurationTile';
import AccessPage from 'MainRoot/OrgsAndPolicies/access/AccessPage';
import SourceControlTile from 'MainRoot/OrgsAndPolicies/ownerSummary/SourceControlTile';
import ApplicationCategoriesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ApplicationCategoriesTile';
import ContinuousMonitoringSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ContinuousMonitoringSummaryTile';
import AssignAppCategory from 'MainRoot/OrgsAndPolicies/assignAppCategory/AssignAppCategory';
import DeleteOwnerModal from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/DeleteOwnerModal';
import PolicyViolationGrandfatheringEditor from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringEditor/PolicyViolationGrandfatheringEditor';
import GrandfatheringModal from 'MainRoot/OrgsAndPolicies/grandfatheringModal/GrandfatheringModal';
import ChangeApplicationIdModal from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/ChangeApplicationIdModal';
import RevokeGrandfatheringModal from 'MainRoot/OrgsAndPolicies/revokeGrandfatheringModal/RevokeGrandfatheringModal';
import ImportPoliciesModal from 'MainRoot/OrgsAndPolicies/importPoliciesModal/ImportPoliciesModal';
import RetentionTile from 'MainRoot/OrgsAndPolicies/ownerSummary/retentionTile/RetentionTile';
import InnerSourceRepositoryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/InnerSourceRepositoryTile';
import MoveOwnerModal from 'MainRoot/OrgsAndPolicies/moveOwner/MoveOwnerModal';
import DataRetentionEditor from 'MainRoot/OrgsAndPolicies/dataRetentionEditor/DataRetentionEditor';
import LicenseThreatGroupSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/LicenseThreatGroupSummaryTile';
import SelectContactModal from 'MainRoot/OrgsAndPolicies/selectContactModal/SelectContactModal';
import EvaluateApplicationModal from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/EvaluateApplicationModal';
import ActionDropdown from 'MainRoot/OrgsAndPolicies/actionDropdown/ActionDropdown';
import OwnerDetailSidebar from 'MainRoot/owner.manager/navigation/OwnerDetailSidebar';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import RepositoriesPills from 'MainRoot/owner.manager/repositories/RepositoriesPills/RepositoriesPills';
import ArtifactoryRepositoryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ArtifactoryRepositoryTile';

export default angular
  .module('owner.manager.module', [
    storesModule.name,
    'ui.bootstrap',
    'ui.router',
    angularCommonModule.name,
    formsModule.name,
    utilityModule.name,
    utilityDirectivesModule.name,
    permissionServiceModule.name,
    CLMLocationModule.name,
    utilityServicesModule.name,
    validatorsModule.name,
    roleMembershipModule.name,
    sourceControlModule.name,
  ])
  .component('ownerSideNav', iqReact2Angular(OwnerSideNav, [], ['$ngRedux', '$state']))
  .component('ownersTreePage', iqReact2Angular(OwnersTreePage, [], ['$ngRedux', '$state']))
  .component(
    'insufficientPermissionOwnerHierarchyTree',
    iqReact2Angular(InsufficientPermissionOwnerHierarchyTree, [], ['$ngRedux', '$state'])
  )
  .service('SameOwnerStateNavigationService', SameOwnerStateNavigationService)
  .service('monitored.stage.service', MonitoredStageService)
  .directive('ownerImage', OwnerImageDirective)
  .controller('OwnerSummaryController', OwnerSummaryController)
  .controller('OwnerSummaryTilesContainerController', OwnerSummaryTilesContainerController)
  .service('SourceControlService', SourceControlService)
  .directive('numberInputWithStringValue', NumberInputWithStringValue)
  .directive('sameOwnerEditSref', SameOwnerEditSref)
  .directive('sameOwnerViewSref', SameOwnerViewSref)
  .component('accessTile', iqReact2Angular(AccessTile, [], ['$ngRedux', '$state']))
  .component('repositoriesSummaryView', iqReact2Angular(RepositoriesSummaryView, [], ['$ngRedux', '$state']))
  .component('licenseThreatGroupEditor', iqReact2Angular(LicenseThreatGroupEditor, [], ['$ngRedux']))
  .component('policyGrandfatheringTile', iqReact2Angular(PolicyGrandfatheringTile, [], ['$ngRedux', '$state']))
  .component('policiesTile', iqReact2Angular(PoliciesTile, [], ['$ngRedux']))
  .component(
    'proprietaryComponentConfigurationTile',
    iqReact2Angular(ProprietaryComponentConfigurationTile, [], ['$ngRedux', '$state'])
  )
  .component('continuousMonitoring', iqReact2Angular(ContinuousMonitoringEditor, [], ['$ngRedux']))
  .component('createComponentLabel', iqReact2Angular(CreateComponentLabel, [], ['$ngRedux', '$state']))
  .component('accessPage', iqReact2Angular(AccessPage, [], ['$ngRedux', '$state']))
  .component('sourceControlTile', iqReact2Angular(SourceControlTile, [], ['$ngRedux', '$state']))
  .component('policyEditor', iqReact2Angular(PolicyEditor, [], ['$ngRedux']))
  .component('labelsTile', iqReact2Angular(LabelsTile, [], ['$ngRedux', '$state']))
  .component('retentionTile', iqReact2Angular(RetentionTile, [], ['$ngRedux']))
  .component('applicationCategoriesTile', iqReact2Angular(ApplicationCategoriesTile, [], ['$ngRedux', '$state']))
  .component('proprietaryComponentConfiguration', iqReact2Angular(ProprietaryComponentConfiguration, [], ['$ngRedux']))
  .component(
    'continuousMonitoringSummaryTile',
    iqReact2Angular(ContinuousMonitoringSummaryTile, [], ['$ngRedux', '$state'])
  )
  .component('innerSourceRepositoryTile', iqReact2Angular(InnerSourceRepositoryTile, [], ['$ngRedux']))
  .component('artifactoryRepositoryTile', iqReact2Angular(ArtifactoryRepositoryTile, [], ['$ngRedux']))
  .component('deleteOwnerModal', iqReact2Angular(DeleteOwnerModal, [], ['$ngRedux']))
  .component('changeApplicationIdModal', iqReact2Angular(ChangeApplicationIdModal, [], ['$ngRedux']))
  .component('revokeGrandfatheringModal', iqReact2Angular(RevokeGrandfatheringModal, [], ['$ngRedux']))
  .component('createEditApplicationCategory', iqReact2Angular(CreateEditApplicationCategory, [], ['$ngRedux']))
  .component('grandfatheringModal', iqReact2Angular(GrandfatheringModal, [], ['$ngRedux']))
  .component('assignAppCategory', iqReact2Angular(AssignAppCategory, [], ['$ngRedux']))
  .component('importPoliciesModal', iqReact2Angular(ImportPoliciesModal, [], ['$ngRedux']))
  .component(
    'policyViolationGrandfatheringEditor',
    iqReact2Angular(PolicyViolationGrandfatheringEditor, [], ['$ngRedux'])
  )
  .component('moveOwnerModal', iqReact2Angular(MoveOwnerModal, [], ['$ngRedux']))
  .component('dataRetentionEditor', iqReact2Angular(DataRetentionEditor, [], ['$ngRedux']))
  .component('ownerSummaryPills', iqReact2Angular(OwnerSummaryPills, [], ['$ngRedux']))
  .component(
    'licenseThreatGroupSummaryTile',
    iqReact2Angular(LicenseThreatGroupSummaryTile, [], ['$ngRedux', '$state'])
  )
  .component('selectContactModal', iqReact2Angular(SelectContactModal, [], ['$ngRedux']))
  .component('evaluateApplicationModal', iqReact2Angular(EvaluateApplicationModal, [], ['$ngRedux']))
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
        },
        {
          type: 'application',
          name: 'Application',
          id: 'applicationPublicId',
        },
        {
          type: 'repository_container',
          name: 'Repositories',
          id: 'repositoryContainerId',
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
        })
        .state('management.view.repository_container', {
          url: '/repository_container/REPOSITORY_CONTAINER_ID',
          data: {
            title: 'Repositories Management',
            viewportSized: true,
          },
          component: 'repositoriesSummaryView',
        });

      ownerTypes.forEach(function (ownerType) {
        if (ownerType.type !== 'repository_container') {
          $stateProvider.state('management.view.' + ownerType.type, {
            url: '/' + ownerType.type + '/{' + ownerType.id + '}',
            data: {
              title: ownerType.name + ' Management',
              viewportSized: true,
            },
            template: summaryViewTemplate,
          });
        }
        $stateProvider
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
              isDirty: ['orgsAndPolicies', 'policy', 'isDirty'],
            },
            component: 'policyEditor',
          })
          .state('management.edit.' + ownerType.type + '.create-policy', {
            url: '/policy',
            data: {
              title: ownerType.name + ' Policy',
              isDirty: ['orgsAndPolicies', 'policy', 'isDirty'],
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
          .state('management.edit.' + ownerType.type + '.violation-grandfathering-policy', {
            url: '/grandfathering',
            data: {
              title: ownerType.name + ' Violation Grandfathering',
              isDirty: ['orgsAndPolicies', 'policyViolationGrandfathering', 'isDirty'],
            },
            component: 'policyViolationGrandfatheringEditor',
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
            },
            component: 'sourceControlEditor',
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

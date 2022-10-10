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
import AccessTileController from './access/access.tile.controller';
import AccessTile from './access/access.tile.directive';
import SameOwnerStateNavigationService from './utility/same.owner.state.navigation.service';
import OwnerDetailTreeViewController from './navigation/owner.detail.tree.view.controller';
import OwnerDetailTreeViewDirective from './navigation/owner.detail.tree.view.directive';
import ownerTreeView from './navigation/owner.tree.view.directive';
import MonitoredStageService from './utility/monitored.stage.service';
import ConfigurationTileController from './repositories/repositories.configuration.tile.controller';
import OwnerEditorController from './summary/owner.editor.controller';
import OwnerEditorService from './summary/owner.editor.service';
import OwnerImageDirective from './summary/owner.image.directive';
import SelectApplicationContactService from './summary/select.application.contact.service';
import OwnerSummaryController from './summary/owner.summary.controller';
import EvaluateApplicationModalService from './utility/services/evaluate.application.modal.service';
import SelectApplicationContactController from './summary/select.application.contact.controller';
import EvaluateApplicationModalController from './utility/services/evaluate.application.modal.controller';
import NumberInputWithStringValue from './utility/number.input.with.string.value';
import SameOwnerEditSref from './utility/same.owner.edit.sref.directive';
import SameOwnerViewSref from './utility/same.owner.view.sref.directive';
import retentionModule from './retention/module';
import sourceControlModule from './source.control/module';
import viewTemplate from './state/owner.manager.view.html';
import repoSummaryTemplate from './repositories/repositories.summary.view.html';
import summaryViewTemplate from './summary/owner.summary.view.html';
import SourceControlService from './source.control/source.control.service';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/сontinuousMonitoringEditor/ContinuousMonitoringEditor';
import artifactoryRepositoryModule from './artifactory.repository/module';
import LicenseThreatGroupEditor from 'MainRoot/OrgsAndPolicies/licenseThreatGroupEditor/LicenseThreatGroupEditor';
import LabelsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/labelsTile/LabelsTile';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import CreateEditApplicationCategory from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/CreateEditApplicationCategory';
import ProprietaryComponentConfiguration from 'MainRoot/OrgsAndPolicies/proprietaryComponentConfig/ProprietaryComponentConfiguration';
import OwnerSummaryTilesContainerController from './summary/owner.summary.tiles.container.controller';
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
import MoveApplicationModal from 'MainRoot/OrgsAndPolicies/moveApplicationModal/MoveApplicationModal';
import LicenseThreatGroupSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/LicenseThreatGroupSummaryTile';

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
    retentionModule.name,
    sourceControlModule.name,
    artifactoryRepositoryModule.name,
  ])
  .controller('AccessTileController', AccessTileController)
  .directive('accessTile', AccessTile)
  .controller('OwnerDetailTreeViewController', OwnerDetailTreeViewController)
  .directive('ownerDetailTreeView', OwnerDetailTreeViewDirective)
  .directive('ownerTreeView', ownerTreeView)
  .service('SameOwnerStateNavigationService', SameOwnerStateNavigationService)
  .service('monitored.stage.service', MonitoredStageService)
  .controller('repositories.configuration.tile.controller', ConfigurationTileController)
  .controller('owner.editor.controller', OwnerEditorController)
  .service('OwnerEditorService', OwnerEditorService)
  .directive('ownerImage', OwnerImageDirective)
  .service('SelectApplicationContactService', SelectApplicationContactService)
  .controller('OwnerSummaryController', OwnerSummaryController)
  .controller('OwnerSummaryTilesContainerController', OwnerSummaryTilesContainerController)
  .service('evaluate.application.modal.service', EvaluateApplicationModalService)
  .controller('select.application.contact.controller', SelectApplicationContactController)
  .controller('evaluate.application.modal.controller', EvaluateApplicationModalController)
  .service('SourceControlService', SourceControlService)
  .directive('numberInputWithStringValue', NumberInputWithStringValue)
  .directive('sameOwnerEditSref', SameOwnerEditSref)
  .directive('sameOwnerViewSref', SameOwnerViewSref)
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
  .component('moveApplicationModal', iqReact2Angular(MoveApplicationModal, [], ['$ngRedux']))
  .component(
    'licenseThreatGroupSummaryTile',
    iqReact2Angular(LicenseThreatGroupSummaryTile, [], ['$ngRedux', '$state'])
  )
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
      ];

      $stateProvider
        .state('management', {
          url: '/management',
          abstract: true,
          template: viewTemplate,
        })
        .state('management.view', {
          url: '/view',
          data: {
            title: 'Management',
          },
          views: {
            'navigation@management': {
              template: '<owner-tree-view></owner-tree-view>',
            },
          },
        })
        .state('management.edit', {
          abstract: true,
          template: '<div ui-view></div>',
        })
        .state('management.view.repositories', {
          url: '/repositories',
          data: {
            title: 'Repositories Management',
            viewportSized: true,
          },
          views: {
            '@management': {
              template: repoSummaryTemplate,
            },
          },
        })
        .state('management.edit.repositories', {
          url: '/edit/repositories',
          data: {
            title: 'Repositories Management',
          },
          views: {
            'navigation@management': {
              template: '<owner-detail-tree-view></owner-detail-tree-view>',
            },
          },
        })
        .state('management.edit.repositories.add-access', {
          url: '/access',
          data: {
            isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
          },
          views: {
            '@management.edit': {
              component: 'accessPage',
            },
          },
        })
        .state('management.edit.repositories.edit-access', {
          url: '/access/{roleId}',
          data: {
            isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
          },
          views: {
            '@management.edit': {
              component: 'accessPage',
            },
          },
        });

      ownerTypes.forEach(function (ownerType) {
        $stateProvider
          .state('management.view.' + ownerType.type, {
            url: '/' + ownerType.type + '/{' + ownerType.id + '}',
            data: {
              title: ownerType.name + ' Management',
              viewportSized: true,
            },
            views: {
              '@management': {
                template: summaryViewTemplate,
              },
            },
          })
          .state('management.edit.' + ownerType.type, {
            url: '/edit/' + ownerType.type + '/{' + ownerType.id + '}',
            data: {
              title: ownerType.name + ' Management',
            },
            views: {
              'navigation@management': {
                template: '<owner-detail-tree-view></owner-detail-tree-view>',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.label', {
            url: '/label/{labelId}',
            data: {
              title: ownerType.name + ' Labels',
              isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'createComponentLabel',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.create-label', {
            url: '/label',
            data: {
              title: ownerType.name + ' Labels',
              isDirty: ['orgsAndPolicies', 'labels', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'createComponentLabel',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.policy', {
            url: '/policy/{policyId}',
            data: {
              title: ownerType.name + ' Policy',
              isDirty: ['orgsAndPolicies', 'policy', 'isDirty'],
            },
            views: {
              '@management': {
                component: 'policyEditor',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.create-policy', {
            url: '/policy',
            data: {
              title: ownerType.name + ' Policy',
              isDirty: ['orgsAndPolicies', 'policy', 'isDirty'],
            },
            views: {
              '@management': {
                component: 'policyEditor',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.add-access', {
            url: '/access',
            data: {
              title: ownerType.name + ' Access',
              isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'accessPage',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.edit-access', {
            url: '/access/{roleId}',
            data: {
              title: ownerType.name + ' Access',
              isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'accessPage',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.violation-grandfathering-policy', {
            url: '/grandfathering',
            data: {
              title: ownerType.name + ' Violation Grandfathering',
              isDirty: ['orgsAndPolicies', 'policyViolationGrandfathering', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'policyViolationGrandfatheringEditor',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.monitor-policy', {
            url: '/monitoring',
            data: {
              title: ownerType.name + ' Continuous Monitoring',
              isDirty: ['orgsAndPolicies', 'policyMonitoring', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'continuousMonitoring',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.proprietary-config-policy', {
            url: '/proprietary',
            data: {
              title: ownerType.name + ' Proprietary Components',
              isDirty: ['orgsAndPolicies', 'proprietary', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'proprietaryComponentConfiguration',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.edit-license-threat-group', {
            url: '/licenseThreatGroup/{licenseThreatGroupId}',
            data: {
              title: ownerType.name + ' License Threat Groups',
              isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
            },
            views: {
              '@management.edit': {
                component: 'licenseThreatGroupEditor',
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.edit-source-control', {
            url: '/source-control',
            data: {
              title: 'Source Control',
            },
            views: {
              '@management.edit': {
                component: 'sourceControlEditor',
              },
            },
          });
      });

      $stateProvider
        .state('management.edit.organization.category', {
          url: '/category/{categoryId}',
          data: {
            title: 'Organization Category',
            isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
          },
          views: {
            '@management.edit': {
              component: 'createEditApplicationCategory',
            },
          },
        })
        .state('management.edit.organization.create-category', {
          url: '/category',
          data: {
            title: 'Organization Category',
            isDirty: ['orgsAndPolicies', 'applicationCategories', 'createEdit', 'isDirty'],
          },
          views: {
            '@management.edit': {
              component: 'createEditApplicationCategory',
            },
          },
        })
        .state('management.edit.application.category', {
          data: {
            title: 'Application Categories',
            isDirty: ['orgsAndPolicies', 'applicationCategories', 'assign', 'isDirty'],
          },
          url: '/category',
          views: {
            '@management.edit': {
              component: 'assignAppCategory',
            },
          },
        })
        .state('management.edit.organization.create-license-threat-group', {
          data: {
            title: 'Organization License Threat Group',
            isDirty: ['orgsAndPolicies', 'licenseThreatGroups', 'isDirty'],
          },
          url: '/licenseThreatGroup',
          views: {
            '@management.edit': {
              component: 'licenseThreatGroupEditor',
            },
          },
        })
        .state('management.edit.organization.edit-data-retention', {
          url: '/data-retention',
          data: {
            title: 'Organization Data Retention',
          },
          views: {
            '@management.edit': {
              component: 'retentionEditor',
            },
          },
        });
    },
  ]);

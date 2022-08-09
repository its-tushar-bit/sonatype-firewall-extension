/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import moveApplicationModule from './move.application/module';
import formsModule from '../FormsModule';
import angularCommonModule from '../utilAngular/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import utilityDirectivesModule from '../utility/directives/utility.directives.module';
import utilityServicesModule from '../utility/services/utility.services.module';
import utilityModule from '../utility/utility.module';
import permissionServiceModule from '../utilAngular/PermissionService';
import validatorsModule from '../utilAngular/Validators';
import storesModule from '../utilAngular/Stores';
import ownerPolicyList from './summary/ownerPolicyList/ownerPolicyList';

import licenseThreatGroupModule from '../policy/LicenseThreatGroupsController';
import roleMembershipModule from '../role.membership/role.membership.module';
import AccessEditorController from './access/access.editor.controller';
import AccessTileController from './access/access.tile.controller';
import AccessTile from './access/access.tile.directive';
import LocalRoleService from './utility/local.role.service';
import SameOwnerStateNavigationService from './utility/same.owner.state.navigation.service';
import RoleMappingService from './access/role.mapping.service';
import LicenseThreatGroupEditorController from './license.threat.group/license.threat.group.editor.controller';
import LicenseThreatGroupTileController from './license.threat.group/license.threat.group.tile.controller';
import OwnerDetailTreeViewController from './navigation/owner.detail.tree.view.controller';
import OwnerDetailTreeViewDirective from './navigation/owner.detail.tree.view.directive';
import ownerTreeView from './navigation/owner.tree.view.directive';
import CoordinatesInput from './policy/coordinates.input.directive';
import NotificationWebhookService from './policy/notification.webhook.service';
import PolicyEditorActionsController from './policy/policy.editor.actions.controller';
import PolicyEditorConstraintsController from './policy/policy.editor.constraints.controller';
import PolicyEditorController from './policy/policy.editor.controller';
import PolicyEditorSummaryController from './policy/policy.editor.summary.controller';
import PolicyEditorFormContainerController from './policy/policy.editor.form.container.controller';
import PolicyTileController from './policy/policy.tile.controller';
import PolicyEditorActionsDirective from './policy/policy.editor.actions.directive';
import PolicyEditorNotificationsDirective from './policy/policy.editor.notifications.directive';
import PolicyEditorConstraintsDirective from './policy/policy.editor.constraints.directive';
import MonitoredStageService from './utility/monitored.stage.service';
import PolicyEditorNotificationsController from './policy/policy.editor.notifications.controller';
import ConfigurationTileController from './repositories/repositories.configuration.tile.controller';
import ChangeApplicationIdController from './summary/change.application.id.controller';
import OwnerEditorController from './summary/owner.editor.controller';
import OwnerEditorService from './summary/owner.editor.service';
import OwnerImageDirective from './summary/owner.image.directive';
import SelectApplicationContactService from './summary/select.application.contact.service';
import OwnerSummaryController from './summary/owner.summary.controller';
import EvaluateApplicationModalService from './utility/services/evaluate.application.modal.service';
import RevokeGrandfatheringModalService from './utility/services/revokeGrandfatheringModalService';
import GrandfatherModalService from './utility/services/grandfatherModalService';
import ImportPolicyModalService from './utility/services/import.policy.modal.service';
import SelectApplicationContactController from './summary/select.application.contact.controller';
import ChangeApplicationIdService from './summary/change.application.id.service';
import EvaluateApplicationModalController from './utility/services/evaluate.application.modal.controller';
import RevokeGrandfatheringModalController from './utility/services/revokeGrandfatheringModalController';
import GrandfatherModalController from './utility/services/grandfatherModalController';
import ImportPolicyModalController from './utility/services/import.policy.modal.controller';
import NumberInputWithStringValue from './utility/number.input.with.string.value';
import SameOwnerEditSref from './utility/same.owner.edit.sref.directive';
import SameOwnerViewSref from './utility/same.owner.view.sref.directive';
import PolicyViolationGrandfatheringModule from './policyViolationGrandfathering/module';
import retentionModule from './retention/module';
import sourceControlModule from './source.control/module';
import viewTemplate from './state/owner.manager.view.html';
import repoSummaryTemplate from './repositories/repositories.summary.view.html';
import accessEditorTemplate from './access/access.editor.view.html';
import summaryViewTemplate from './summary/owner.summary.view.html';
import policyEditorTemplate from './policy/policy.editor.view.html';
import ltgEditorTemplate from './license.threat.group/license.threat.group.editor.view.html';
import SourceControlService from './source.control/source.control.service';
import innerSourceRepositoryModule from './innersource.repository/module';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/сontinuousMonitoringEditor/ContinuousMonitoringEditor';
import artifactoryRepositoryModule from './artifactory.repository/module';
import LabelsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/labelsTile/LabelsTile';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import CreateEditApplicationCategory from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/CreateEditApplicationCategory';
import ProprietaryComponentConfiguration from 'MainRoot/OrgsAndPolicies/proprietaryComponentConfig/ProprietaryComponentConfiguration';
import OwnerSummaryTilesContainerController from './summary/owner.summary.tiles.container.controller';
import PolicyGrandfatheringTile from 'MainRoot/OrgsAndPolicies/ownerSummary/PolicyGrandfatheringTile';
import PoliciesHeaderTile from 'MainRoot/OrgsAndPolicies/ownerSummary/PoliciesHeaderTile';
import ProprietaryComponentConfigurationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ProprietaryComponentConfigurationTile';
import SourceControlTile from 'MainRoot/OrgsAndPolicies/ownerSummary/SourceControlTile';
import ApplicationCategoriesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ApplicationCategoriesTile';
import ContinuousMonitoringSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ContinuousMonitoringSummaryTile';
import AssignAppCategory from 'MainRoot/OrgsAndPolicies/assignAppCategory/AssignAppCategory';
import DeleteOwnerModal from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/DeleteOwnerModal';

export default angular
  .module('owner.manager.module', [
    storesModule.name,
    licenseThreatGroupModule.name,
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
    moveApplicationModule.name,
    PolicyViolationGrandfatheringModule.name,
    retentionModule.name,
    sourceControlModule.name,
    innerSourceRepositoryModule.name,
    artifactoryRepositoryModule.name,
  ])
  .component('ownerPolicyList', ownerPolicyList)
  .controller('access.editor.controller', AccessEditorController)
  .controller('AccessTileController', AccessTileController)
  .directive('accessTile', AccessTile)
  .controller('license.threat.group.editor.controller', LicenseThreatGroupEditorController)
  .controller('LicenseThreatGroupTileController', LicenseThreatGroupTileController)
  .controller('OwnerDetailTreeViewController', OwnerDetailTreeViewController)
  .directive('ownerDetailTreeView', OwnerDetailTreeViewDirective)
  .directive('ownerTreeView', ownerTreeView)
  .service('local.role.service', LocalRoleService)
  .service('SameOwnerStateNavigationService', SameOwnerStateNavigationService)
  .service('role.mapping.service', RoleMappingService)
  .directive('coordinatesInput', CoordinatesInput)
  .controller('policy.editor.actions.controller', PolicyEditorActionsController)
  .controller('policy.editor.constraints.controller', PolicyEditorConstraintsController)
  .controller('PolicyEditorController', PolicyEditorController)
  .controller('PolicyEditorSummaryController', PolicyEditorSummaryController)
  .controller('PolicyEditorFormContainerController', PolicyEditorFormContainerController)
  .controller('policy.tile.controller', PolicyTileController)
  .directive('policyEditorActions', PolicyEditorActionsDirective)
  .directive('policyEditorNotifications', PolicyEditorNotificationsDirective)
  .directive('policyEditorConstraints', PolicyEditorConstraintsDirective)
  .service('monitored.stage.service', MonitoredStageService)
  .service('notification.webhook.service', NotificationWebhookService)
  .controller('policy.editor.notifications.controller', PolicyEditorNotificationsController)
  .controller('repositories.configuration.tile.controller', ConfigurationTileController)
  .controller('change.application.id.controller', ChangeApplicationIdController)
  .controller('owner.editor.controller', OwnerEditorController)
  .service('OwnerEditorService', OwnerEditorService)
  .directive('ownerImage', OwnerImageDirective)
  .service('SelectApplicationContactService', SelectApplicationContactService)
  .controller('OwnerSummaryController', OwnerSummaryController)
  .controller('OwnerSummaryTilesContainerController', OwnerSummaryTilesContainerController)
  .service('evaluate.application.modal.service', EvaluateApplicationModalService)
  .service('RevokeGrandfatheringModalService', RevokeGrandfatheringModalService)
  .service('GrandfatherModalService', GrandfatherModalService)
  .service('import.policy.modal.service', ImportPolicyModalService)
  .controller('select.application.contact.controller', SelectApplicationContactController)
  .service('change.application.id.service', ChangeApplicationIdService)
  .controller('evaluate.application.modal.controller', EvaluateApplicationModalController)
  .controller('RevokeGrandfatheringModalController', RevokeGrandfatheringModalController)
  .controller('GrandfatherModalController', GrandfatherModalController)
  .controller('import.policy.modal.controller', ImportPolicyModalController)
  .service('SourceControlService', SourceControlService)
  .directive('numberInputWithStringValue', NumberInputWithStringValue)
  .directive('sameOwnerEditSref', SameOwnerEditSref)
  .directive('sameOwnerViewSref', SameOwnerViewSref)
  .component('policyGrandfatheringTile', iqReact2Angular(PolicyGrandfatheringTile, [], ['$ngRedux', '$state']))
  .component('policiesHeaderTile', iqReact2Angular(PoliciesHeaderTile, [], ['$ngRedux']))
  .component(
    'proprietaryComponentConfigurationTile',
    iqReact2Angular(ProprietaryComponentConfigurationTile, [], ['$ngRedux', '$state'])
  )
  .component('continuousMonitoring', iqReact2Angular(ContinuousMonitoringEditor, [], ['$ngRedux']))
  .component('createComponentLabel', iqReact2Angular(CreateComponentLabel, [], ['$ngRedux', '$state']))
  .component('sourceControlTile', iqReact2Angular(SourceControlTile, [], ['$ngRedux', '$state']))
  .component('labelsTile', iqReact2Angular(LabelsTile, [], ['$ngRedux', '$state']))
  .component('applicationCategoriesTile', iqReact2Angular(ApplicationCategoriesTile, [], ['$ngRedux', '$state']))
  .component('proprietaryComponentConfiguration', iqReact2Angular(ProprietaryComponentConfiguration, [], ['$ngRedux']))
  .component(
    'continuousMonitoringSummaryTile',
    iqReact2Angular(ContinuousMonitoringSummaryTile, [], ['$ngRedux', '$state'])
  )
  .component('deleteOwnerModal', iqReact2Angular(DeleteOwnerModal, [], ['$ngRedux']))
  .component('createEditApplicationCategory', iqReact2Angular(CreateEditApplicationCategory, [], ['$ngRedux']))
  .component('assignAppCategory', iqReact2Angular(AssignAppCategory, [], ['$ngRedux']))
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
          views: {
            '@management.edit': {
              controller: 'access.editor.controller',
              controllerAs: 'vm',
              template: accessEditorTemplate,
            },
          },
        })
        .state('management.edit.repositories.edit-access', {
          url: '/access/{roleId}',
          views: {
            '@management.edit': {
              controller: 'access.editor.controller',
              controllerAs: 'vm',
              template: accessEditorTemplate,
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
              viewportSized: true,
            },
            views: {
              '@management': {
                template: policyEditorTemplate,
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.create-policy', {
            url: '/policy',
            data: {
              title: ownerType.name + ' Policy',
              viewportSized: true,
            },
            views: {
              '@management': {
                template: policyEditorTemplate,
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.add-access', {
            url: '/access',
            data: {
              title: ownerType.name + ' Access',
            },
            views: {
              '@management.edit': {
                controller: 'access.editor.controller',
                controllerAs: 'vm',
                template: accessEditorTemplate,
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.edit-access', {
            url: '/access/{roleId}',
            data: {
              title: ownerType.name + ' Access',
            },
            views: {
              '@management.edit': {
                controller: 'access.editor.controller',
                controllerAs: 'vm',
                template: accessEditorTemplate,
              },
            },
          })
          .state('management.edit.' + ownerType.type + '.violation-grandfathering-policy', {
            url: '/grandfathering',
            data: {
              title: ownerType.name + ' Violation Grandfathering',
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
            },
            views: {
              '@management.edit': {
                controller: 'license.threat.group.editor.controller',
                controllerAs: 'vm',
                template: ltgEditorTemplate,
                clmBuildTimestamp,
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
          },
          url: '/licenseThreatGroup',
          views: {
            '@management.edit': {
              controller: 'license.threat.group.editor.controller',
              controllerAs: 'vm',
              template: ltgEditorTemplate,
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

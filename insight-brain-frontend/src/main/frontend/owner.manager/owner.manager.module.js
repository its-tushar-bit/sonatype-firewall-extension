/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import moveApplicationModule from './move.application/module';
import formsModule from '../FormsModule';
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import utilityDirectivesModule from '../utility/directives/utility.directives.module';
import utilityServicesModule from '../utility/services/utility.services.module';
import utilityModule from '../utility/utility.module';
import permissionServiceModule from '../util/PermissionService';
import validatorsModule from '../util/Validators';
import storesModule from '../util/Stores';
import ownerPolicyList from './summary/ownerPolicyList/ownerPolicyList';
import ProductFeaturesModule from '../util/ProductFeatures';

import labelsModule from '../policy/LabelController';
import tagsModule from '../policy/TagController';
import licenseThreatGroupModule from '../policy/LicenseThreatGroupsController';
import policyModule from '../policy/PolicyMonitoringStore';
import roleMembershipModule from '../role.membership/role.membership.module';
import AccessEditorController from './access/access.editor.controller';
import AccessTileController from './access/access.tile.controller';
import AccessTile from './access/access.tile.directive';
import LocalRoleService from './utility/local.role.service';
import SameOwnerStateNavigationService from './utility/same.owner.state.navigation.service';
import RoleMappingService from './access/role.mapping.service';
import ApplicationCategoryEditorController from './category/application.category.editor.controller';
import ApplicationCategoryTileControllerApp from './category/application.category.tile.controller.app';
import ApplicationCategoryTileControllerOrg from './category/application.category.tile.controller.org';
import CategoryEditorController from './category/category.editor.controller';
import LabelEditorController from './label/label.editor.controller';
import LabelTileController from './label/label.tile.controller';
import LicenseThreatGroupEditorController from './license.threat.group/license.threat.group.editor.controller';
import LicenseThreatGroupTileController from './license.threat.group/license.threat.group.tile.controller';
import OwnerDetailTreeViewController from './navigation/owner.detail.tree.view.controller';
import OwnerDetailTreeViewDirective from './navigation/owner.detail.tree.view.directive';
import ownerTreeView from './navigation/owner.tree.view.directive';
import CoordinatesInput from './policy/coordinates.input.directive';
import MonitoredStageEditorController from './policy/monitored.stage.editor.controller';
import PolicyEditorActionsController from './policy/policy.editor.actions.controller';
import PolicyEditorConstraintsController from './policy/policy.editor.constraints.controller';
import PolicyEditorController from './policy/policy.editor.controller';
import PolicyTileController from './policy/policy.tile.controller';
import PolicyEditorActionsDirective from './policy/policy.editor.actions.directive';
import PolicyEditorNotificationsDirective from './policy/policy.editor.notifications.directive';
import PolicyEditorConstraintsDirective from './policy/policy.editor.constraints.directive';
import MonitoredStageService from './utility/monitored.stage.service';
import ProprietaryConfigEditorController from './policy/proprietary.config.editor.controller';
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

export default
angular.module('owner.manager.module',
    [
      storesModule.name, labelsModule.name, tagsModule.name, licenseThreatGroupModule.name, 'ui.bootstrap', 'ui.router', angularCommonModule.name,
      formsModule.name, utilityModule.name, utilityDirectivesModule.name, permissionServiceModule.name, policyModule.name,
      CLMLocationModule.name, utilityServicesModule.name, validatorsModule.name, roleMembershipModule.name,
      moveApplicationModule.name, ProductFeaturesModule.name, PolicyViolationGrandfatheringModule.name,
      retentionModule.name
    ])
    .component('ownerPolicyList', ownerPolicyList)
    .controller('access.editor.controller', AccessEditorController)
    .controller('AccessTileController', AccessTileController)
    .directive('accessTile', AccessTile)
    .controller('application.category.editor.controller', ApplicationCategoryEditorController)
    .controller('ApplicationCategoryTileControllerApp', ApplicationCategoryTileControllerApp)
    .controller('ApplicationCategoryTileControllerOrg', ApplicationCategoryTileControllerOrg)
    .controller('category.editor.controller', CategoryEditorController)
    .controller('label.editor.controller', LabelEditorController)
    .controller('LabelTileController', LabelTileController)
    .controller('license.threat.group.editor.controller', LicenseThreatGroupEditorController)
    .controller('LicenseThreatGroupTileController', LicenseThreatGroupTileController)
    .controller('OwnerDetailTreeViewController', OwnerDetailTreeViewController)
    .directive('ownerDetailTreeView', OwnerDetailTreeViewDirective)
    .directive('ownerTreeView', ownerTreeView)
    .service('local.role.service', LocalRoleService)
    .service('SameOwnerStateNavigationService', SameOwnerStateNavigationService)
    .service('role.mapping.service', RoleMappingService)
    .directive('coordinatesInput', CoordinatesInput)
    .controller('monitored.stage.editor.controller', MonitoredStageEditorController)
    .controller('policy.editor.actions.controller', PolicyEditorActionsController)
    .controller('policy.editor.constraints.controller', PolicyEditorConstraintsController)
    .controller('policy.editor.controller', PolicyEditorController)
    .controller('policy.tile.controller', PolicyTileController)
    .directive('policyEditorActions', PolicyEditorActionsDirective)
    .directive('policyEditorNotifications', PolicyEditorNotificationsDirective)
    .directive('policyEditorConstraints', PolicyEditorConstraintsDirective)
    .service('monitored.stage.service', MonitoredStageService)
    .controller('proprietary.config.editor.controller', ProprietaryConfigEditorController)
    .controller('policy.editor.notifications.controller', PolicyEditorNotificationsController)
    .controller('repositories.configuration.tile.controller', ConfigurationTileController)
    .controller('change.application.id.controller', ChangeApplicationIdController)
    .controller('owner.editor.controller', OwnerEditorController)
    .service('OwnerEditorService', OwnerEditorService)
    .directive('ownerImage', OwnerImageDirective)
    .service('SelectApplicationContactService', SelectApplicationContactService)
    .controller('OwnerSummaryController', OwnerSummaryController)
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
    .directive('numberInputWithStringValue', NumberInputWithStringValue)
    .directive('sameOwnerEditSref', SameOwnerEditSref)
    .directive('sameOwnerViewSref', SameOwnerViewSref)
    .config([
      '$stateProvider', function($stateProvider) {
        var ownerTypes = [
          {
            type: 'organization',
            name: 'Organization',
            id: 'organizationId'
          },
          {
            type: 'application',
            name: 'Application',
            id: 'applicationPublicId'
          }
        ];

        $stateProvider.state('management', {
          url: '/management',
          abstract: true,
          templateUrl: 'owner.manager/state/owner.manager.view.html?' + clmBuildTimestamp
        }).state('management.view', {
          url: '/view',
          data: {
            title: 'Management'
          },
          views: {
            'navigation@management': {
              template: '<owner-tree-view></owner-tree-view>'
            }
          }
        }).state('management.edit', {
          abstract: true,
          template: '<div ui-view maximize-container-height></div>'
        }).state('management.view.repositories', {
          url: '/repositories',
          data: {
            title: 'Repositories Management'
          },
          views: {
            '@management': {
              templateUrl: 'owner.manager/repositories/repositories.summary.view.html?' + clmBuildTimestamp
            }
          }
        }).state('management.edit.repositories', {
          url: '/edit/repositories',
          data: {
            title: 'Repositories Management'
          },
          views: {
            'navigation@management': {
              template: '<owner-detail-tree-view></owner-detail-tree-view>'
            }
          }
        }).state('management.edit.repositories.add-access', {
          url: '/access',
          views: {
            '@management.edit': {
              controller: 'access.editor.controller',
              controllerAs: 'vm',
              templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
            }
          }
        }).state('management.edit.repositories.edit-access', {
          url: '/access/{roleId}',
          views: {
            '@management.edit': {
              controller: 'access.editor.controller',
              controllerAs: 'vm',
              templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
            }
          }
        });

        ownerTypes.forEach(function(ownerType) {
          $stateProvider.state('management.view.' + ownerType.type, {
            url: '/' + ownerType.type + '/{' + ownerType.id + '}',
            data: {
              title: ownerType.name + ' Management'
            },
            views: {
              '@management': {
                templateUrl: 'owner.manager/summary/owner.summary.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type, {
            url: '/edit/' + ownerType.type + '/{' + ownerType.id + '}',
            data: {
              title: ownerType.name + ' Management'
            },
            views: {
              'navigation@management': {
                template: '<owner-detail-tree-view></owner-detail-tree-view>'
              }
            }
          }).state('management.edit.' + ownerType.type + '.label', {
            url: '/label/{labelId}',
            data: {
              title: ownerType.name + ' Labels'
            },
            views: {
              '@management.edit': {
                controller: 'label.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.create-label', {
            url: '/label',
            data: {
              title: ownerType.name + ' Labels'
            },
            views: {
              '@management.edit': {
                controller: 'label.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.policy', {
            url: '/policy/{policyId}',
            data: {
              title: ownerType.name + ' Policy'
            },
            views: {
              // do not attach to @management.edit because policy.editor.view has its own maximize-container-height
              // directive
              '@management': {
                controller: 'policy.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/policy/policy.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.create-policy', {
            url: '/policy',
            data: {
              title: ownerType.name + ' Policy'
            },
            views: {
              // do not attach to @management.edit because policy.editor.view has its own maximize-container-height
              // directive
              '@management': {
                controller: 'policy.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/policy/policy.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.add-access', {
            url: '/access',
            data: {
              title: ownerType.name + ' Access'
            },
            views: {
              '@management.edit': {
                controller: 'access.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.edit-access', {
            url: '/access/{roleId}',
            data: {
              title: ownerType.name + ' Access'
            },
            views: {
              '@management.edit': {
                controller: 'access.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.violation-grandfathering-policy', {
            url: '/grandfathering',
            data: {
              title: ownerType.name + ' Violation Grandfathering'
            },
            views: {
              '@management.edit': {
                component: 'policyViolationGrandfatheringEditor'
              }
            }
          }).state('management.edit.' + ownerType.type + '.monitor-policy', {
            url: '/monitoring',
            data: {
              title: ownerType.name + ' Continuous Monitoring'
            },
            views: {
              '@management.edit': {
                controller: 'monitored.stage.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/policy/monitored.stage.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.proprietary-config-policy', {
            url: '/proprietary',
            data: {
              title: ownerType.name + ' Proprietary Components'
            },
            views: {
              '@management.edit': {
                controller: 'proprietary.config.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/policy/proprietary.config.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.' + ownerType.type + '.edit-license-threat-group', {
            url: '/licenseThreatGroup/{licenseThreatGroupId}',
            data: {
              title: ownerType.name + ' License Threat Groups'
            },
            views: {
              '@management.edit': {
                controller: 'license.threat.group.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/license.threat.group/license.threat.group.editor.view.html?' +
                clmBuildTimestamp
              }
            }
          });
        });

        $stateProvider.state('management.edit.organization.category', {
          url: '/category/{categoryId}',
          data: {
            title: 'Organization Category'
          },
          views: {
            '@management.edit': {
              templateUrl: 'owner.manager/category/category.editor.view.html?' + clmBuildTimestamp,
              controller: 'category.editor.controller',
              controllerAs: 'vm'
            }
          }
        }).state('management.edit.organization.create-category', {
          url: '/category',
          data: {
            title: 'Organization Category'
          },
          views: {
            '@management.edit': {
              templateUrl: 'owner.manager/category/category.editor.view.html?' + clmBuildTimestamp,
              controller: 'category.editor.controller',
              controllerAs: 'vm'
            }
          }
        }).state('management.edit.application.category', {
          data: {
            title: 'Application Categories'
          },
          url: '/category',
          views: {
            '@management.edit': {
              controller: 'application.category.editor.controller',
              controllerAs: 'vm',
              templateUrl: 'owner.manager/category/application.category.editor.view.html?' + clmBuildTimestamp
            }
          }
        }).state('management.edit.organization.create-license-threat-group', {
          data: {
            title: 'Organization License Threat Group'
          },
          url: '/licenseThreatGroup',
          views: {
            '@management.edit': {
              controller: 'license.threat.group.editor.controller',
              controllerAs: 'vm',
              templateUrl: 'owner.manager/license.threat.group/license.threat.group.editor.view.html?' +
              clmBuildTimestamp
            }
          }
        }).state('management.edit.organization.edit-data-retention', {
          url: '/data-retention',
          data: {
            title: 'Organization Data Retention'
          },
          views: {
            '@management.edit': {
              component: 'retentionEditor'
            }
          }
        });
      }
    ]);

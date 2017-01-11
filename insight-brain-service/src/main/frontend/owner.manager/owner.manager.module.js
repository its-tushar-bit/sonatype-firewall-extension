/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  angular.module('owner.manager.module',
      [
        'Stores', 'Labels', 'Tags', 'LicenseThreatGroup', 'ui.bootstrap', 'ui.router', 'AngularCommon', 'FormsModule',
        'utility', 'utility.directives', 'PermissionServiceModule', 'Policy', 'CLMLocation', 'utility.services',
        'Validators', 'role.membership.module'
      ])
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
            parent: 'management',
            url: '/view',
            data : {
              title : 'Management'
            },
            views: {
              'navigation@management': {
                template: '<owner-tree-view></owner-tree-view>'
              }
            }
          }).state('management.edit', {
            parent: 'management',
            abstract: true
          }).state('management.view.repositories', {
            parent: 'management.view',
            url: '/repositories',
            data : {
              title : 'Repositories Management'
            },
            views: {
              '@management': {
                templateUrl: 'owner.manager/repositories/repositories.summary.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.repositories', {
            parent: 'management.edit',
            url: '/edit/repositories',
            data : {
              title : 'Repositories Management'
            },
            views: {
              'navigation@management': {
                template: '<owner-detail-tree-view></owner-detail-tree-view>'
              }
            }
          }).state('management.edit.repositories.add-access', {
            parent: 'management.edit.repositories',
            url: '/access',
            views: {
              '@management': {
                controller: 'access.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.repositories.edit-access', {
            parent: 'management.edit.repositories',
            url: '/access/{roleId}',
            views: {
              '@management': {
                controller: 'access.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
              }
            }
          });

          ownerTypes.forEach(function(ownerType) {
            $stateProvider.state('management.view.' + ownerType.type, {
              parent: 'management.view',
              url: '/' + ownerType.type + '/{' + ownerType.id + '}',
              data : {
                title : ownerType.name + ' Management'
              },
              views: {
                '@management': {
                  templateUrl: 'owner.manager/summary/owner.summary.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type, {
              parent: 'management.edit',
              url: '/edit/' + ownerType.type + '/{' + ownerType.id + '}',
              data : {
                title : ownerType.name + ' Management'
              },
              views: {
                'navigation@management': {
                  template: '<owner-detail-tree-view></owner-detail-tree-view>'
                }
              }
            }).state('management.edit.' + ownerType.type + '.label', {
              parent: 'management.edit.' + ownerType.type,
              url: '/label/{labelId}',
              data : {
                title : ownerType.name + ' Labels'
              },
              views: {
                '@management': {
                  controller: 'label.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.create-label', {
              parent: 'management.edit.' + ownerType.type,
              url: '/label',
              data : {
                title : ownerType.name + ' Labels'
              },
              views: {
                '@management': {
                  controller: 'label.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.policy', {
              parent: 'management.edit.' + ownerType.type,
              url: '/policy/{policyId}',
              data : {
                title : ownerType.name + ' Policy'
              },
              views: {
                '@management': {
                  controller: 'policy.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/policy/policy.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.create-policy', {
              parent: 'management.edit.' + ownerType.type,
              url: '/policy',
              data : {
                title : ownerType.name + ' Policy'
              },
              views: {
                '@management': {
                  controller: 'policy.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/policy/policy.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.add-access', {
              parent: 'management.edit.' + ownerType.type,
              url: '/access',
              data : {
                title : ownerType.name + ' Access'
              },
              views: {
                '@management': {
                  controller: 'access.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.edit-access', {
              parent: 'management.edit.' + ownerType.type,
              url: '/access/{roleId}',
              data : {
                title : ownerType.name + ' Access'
              },
              views: {
                '@management': {
                  controller: 'access.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/access/access.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.monitor-policy', {
              parent: 'management.edit.' + ownerType.type,
              url: '/monitoring',
              data : {
                title : ownerType.name + ' Continuous Monitoring'
              },
              views: {
                '@management': {
                  controller: 'monitored.stage.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/policy/monitored.stage.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.proprietary-config-policy', {
              parent: 'management.edit.' + ownerType.type,
              url: '/proprietary',
              data : {
                title : ownerType.name + ' Proprietary Components'
              },
              views: {
                '@management': {
                  controller: 'proprietary.config.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/policy/proprietary.config.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.type + '.edit-license-threat-group', {
              parent: 'management.edit.' + ownerType.type,
              url: '/licenseThreatGroup/{licenseThreatGroupId}',
              data : {
                title : ownerType.name + ' License Threat Groups'
              },
              views: {
                '@management': {
                  controller: 'license.threat.group.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/license.threat.group/license.threat.group.editor.view.html?' +
                  clmBuildTimestamp
                }
              }
            });
          });

          $stateProvider.state('management.edit.organization.category', {
            parent: 'management.edit.organization',
            url: '/category/{categoryId}',
            data : {
              title : 'Organization Category'
            },
            views: {
              '@management': {
                templateUrl: 'owner.manager/category/category.editor.view.html?' + clmBuildTimestamp,
                controller: 'category.editor.controller',
                controllerAs: 'vm'
              }
            }
          }).state('management.edit.organization.create-category', {
            parent: 'management.edit.organization',
            url: '/category',
            data : {
              title : 'Organization Category'
            },
            views: {
              '@management': {
                templateUrl: 'owner.manager/category/category.editor.view.html?' + clmBuildTimestamp,
                controller: 'category.editor.controller',
                controllerAs: 'vm'
              }
            }
          }).state('management.edit.application.category', {
            parent: 'management.edit.application',
            data : {
              title : 'Application Categories'
            },
            url: '/category',
            views: {
              '@management': {
                controller: 'application.category.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/category/application.category.editor.view.html?' + clmBuildTimestamp
              }
            }
          }).state('management.edit.organization.create-license-threat-group', {
            parent: 'management.edit.organization',
            data : {
              title : 'Organization License Threat Group'
            },
            url: '/licenseThreatGroup',
            views: {
              '@management': {
                controller: 'license.threat.group.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/license.threat.group/license.threat.group.editor.view.html?' +
                clmBuildTimestamp
              }
            }
          });
        }
      ]);
}(angular));

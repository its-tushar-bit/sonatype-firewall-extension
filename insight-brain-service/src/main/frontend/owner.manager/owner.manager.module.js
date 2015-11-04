/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  angular.module('owner.manager.module',
      ['Stores', 'Labels', 'Tags', 'ui.bootstrap', 'ui.router', 'AngularCommon', 'FormsModule', 'utility'])
      .config([
        '$stateProvider', function($stateProvider) {
          var ownerTypes = [
            {
              name: 'organization',
              id: 'organizationId'
            },
            {
              name: 'application',
              id: 'applicationPublicId'
            }
          ];

          $stateProvider.state('management', {
            url: '/management',
            abstract: true,
            templateUrl: 'owner.manager/state/owner.manager.view.html?' + clmBuildTimestamp,
            controller: 'owner.manager.controller',
            controllerAs: 'vm'
          }).state('management.view', {
            parent: 'management',
            url: '/view',
            views: {
              'navigation@management': {
                template: '<owner-tree-view></owner-tree-view>'
              }
            }
          }).state('management.edit', {
            parent: 'management',
            abstract: true
          });

          ownerTypes.forEach(function(ownerType) {
            $stateProvider.state('management.view.' + ownerType.name, {
              parent: 'management.view',
              url: '/' + ownerType.name + '/{' + ownerType.id + '}',
              views: {
                '@management': {
                  templateUrl: 'owner.manager/summary/owner.summary.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.name, {
              parent: 'management.edit',
              url: '/edit/' + ownerType.name + '/{' + ownerType.id + '}',
              views: {
                'navigation@management': {
                  template: '<owner-detail-tree-view></owner-detail-tree-view>'
                }
              }
            }).state('management.edit.' + ownerType.name + '.label', {
              parent: 'management.edit.' + ownerType.name,
              url: '/label/{labelId}',
              views: {
                '@management': {
                  controller: 'label.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp
                }
              }
            }).state('management.edit.' + ownerType.name + '.create-label', {
              parent: 'management.edit.' + ownerType.name,
              url: '/label',
              views: {
                '@management': {
                  controller: 'label.editor.controller',
                  controllerAs: 'vm',
                  templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp
                }
              }
            });
          });

          $stateProvider.state('management.edit.organization.category', {
            parent: 'management.edit.organization',
            url: '/category/{categoryId}',
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
            views: {
              '@management': {
                templateUrl: 'owner.manager/category/category.editor.view.html?' + clmBuildTimestamp,
                controller: 'category.editor.controller',
                controllerAs: 'vm'
              }
            }
          }).state('management.edit.application.category', {
            parent: 'management.edit.application',
            url: '/category',
            views: {
              '@management': {
                controller: 'application.category.editor.controller',
                controllerAs: 'vm',
                templateUrl: 'owner.manager/category/application.category.editor.view.html?' + clmBuildTimestamp,
              }
            }
          });
        }
      ]);
}(angular));

/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  angular.module('owner.manager.module', ['Stores', 'Labels', 'Tags', 'ui.bootstrap', 'ui.router', 'AngularCommon', 'FormsModule', 'utility'])
      .config(['$stateProvider', function($stateProvider) {
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
          templateUrl: 'owner.manager/state/owner.manager.view.html?' + clmBuildTimestamp,
          controller: 'owner.manager.controller',
          controllerAs: 'vm'
        });

        $stateProvider.state('management.organization.edit-category', {
          parent: 'management.organization',
          url: '/category/{categoryId}/edit',
          templateUrl: 'owner.manager/category/category.editor.view.html?' + clmBuildTimestamp,
          controller: 'category.editor.controller',
          controllerAs: 'vm',
          data: {
            isEditView: true
          }
        });

        $stateProvider.state('management.organization.create-category', {
          parent: 'management.organization',
          url: '/category/create',
          templateUrl: 'owner.manager/category/category.editor.view.html?' + clmBuildTimestamp,
          controller: 'category.editor.controller',
          controllerAs: 'vm',
          data: {
            isEditView: true
          }
        });

        ownerTypes.forEach(function(ownerType) {
          $stateProvider.state('management.' + ownerType.name, {
            parent: 'management',
            abstract: true,
            url: '/' + ownerType.name + '/{' + ownerType.id + '}',
            template: '<div ui-view class="template-container"></div>'
          }).state('management.' + ownerType.name + '.view', {
            parent: 'management.' + ownerType.name,
            url: '/view',
            templateUrl: 'owner.manager/summary/owner.summary.view.html?' + clmBuildTimestamp
          }).state('management.' + ownerType.name + '.edit-label', {
            parent: 'management.' + ownerType.name,
            url: '/label/{labelId}/edit',
            templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp,
            controller: 'label.editor.controller',
            controllerAs: 'vm',
            data: {
              isEditView: true
            }
          }).state('management.' + ownerType.name + '.create-label', {
            parent: 'management.' + ownerType.name,
            url: '/label/create',
            templateUrl: 'owner.manager/label/label.editor.view.html?' + clmBuildTimestamp,
            controller: 'label.editor.controller',
            controllerAs: 'vm',
            data: {
              isEditView: true
            }
          });
        });
      }]);
}(angular));

/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  angular.module('owner.manager.module', ['Stores', 'ui.bootstrap', 'ui.router', 'FormsModule', 'utility', 'Labels'])
      .config(['$stateProvider', function($stateProvider) {
        
        var defaultConfig = {
          parent: 'management',
          templateUrl: 'owner.manager/label/label.editor.view.html',
          controller: 'label.editor.controller',
          controllerAs: 'vm',
          data: {
            isEditView: true
          }
        };
        
        $stateProvider.state('management.organization-edit-label', 
                angular.extend({url: '/organization/{organizationId}/label/{labelId}/edit'}, defaultConfig)
        );
        
        $stateProvider.state('management.application-edit-label', 
                angular.extend({url: '/application/{applicationPublicId}/label/{labelId}/edit'}, defaultConfig)
        );
        
        $stateProvider.state('management.organization-create-label', 
                angular.extend({url: '/organization/{organizationId}/label/create'}, defaultConfig)
        );
        
        $stateProvider.state('management.application-create-label', 
                angular.extend({url: '/application/{applicationPublicId}/label/create'}, defaultConfig)
        );
      }]);
}(angular));

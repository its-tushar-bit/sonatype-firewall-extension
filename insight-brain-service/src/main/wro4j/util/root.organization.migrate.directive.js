/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, clmBuildTimestamp*/
(function() {
  'use strict';

  function RootOrganizationMigrateDirective() {
    return {
      templateUrl: 'components/angular.common/root.organization.migrate.directive.html?' + clmBuildTimestamp,
      scope: {},
      controllerAs: 'vm',
      controller: [
        'ProductFeatures', 'RootOrganizationMigrateModalService', 'PermissionService',
        function(ProductFeatures, RootOrganizationMigrateModalService, PermissionService) {
          var vm = this;

          vm.doMigrate = doMigrate;
          vm.migrationDone = undefined;
          vm.migrationNeeded = undefined;
          vm.permitted = undefined;

          function doLoad() {
            vm.migrationDone = ProductFeatures.isAvailable('root-org');
            vm.migrationNeeded = ProductFeatures.isAvailable('root-org-migrate');
            PermissionService.isAuthorized(['WRITE'], true).then(function(permitted){
              vm.permitted = permitted;
            });
          }

          function doMigrate() {
            RootOrganizationMigrateModalService.openModal().then(function() {
              ProductFeatures.load(true).then(function() {
                doLoad();
              });
            });
          }

          doLoad();
        }
      ]
    };
  }

  angular.module('root.organization.migrate').directive('rootOrganizationMigrate', RootOrganizationMigrateDirective);
}());

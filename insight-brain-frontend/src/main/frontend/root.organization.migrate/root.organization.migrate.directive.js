/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './root.organization.migrate.directive.html';

/*global angular, clmBuildTimestamp, clmServerVersion*/
export default function RootOrganizationMigrateDirective() {
  return {
    template,
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
        vm.majorMinorVersion = clmServerVersion.split('.').splice(0, 2).join('.');

        function doLoad() {
          ProductFeatures.load().then(function() {
            vm.migrationDone = ProductFeatures.isAvailable('root-org');
            vm.migrationNeeded = ProductFeatures.isAvailable('root-org-migrate');
          });

          PermissionService.isAuthorized(['WRITE'], true).then(function(permitted) {
            vm.permitted = permitted;
          });
        }

        function doMigrate() {
          RootOrganizationMigrateModalService.openModal().then(doLoad);
        }

        doLoad();
      }
    ]
  };
}

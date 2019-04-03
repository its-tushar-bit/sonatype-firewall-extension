/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, clmServerVersion*/
export default function RootOrganizationMigrateModalController(Messages, OrganizationStore, CLMLocations, $http, $scope,
                                                               $timeout, $window) {
  var vm = this;

  vm.error = undefined;
  vm.loadError = undefined;
  vm.organizations = undefined;
  vm.selectTemplate = selectTemplate;
  vm.migrateSelection = 'selectOrganization';
  vm.organization = undefined;
  vm.doLoad = doLoad;
  vm.reloadApp = reloadApp;
  vm.waitingForReload = undefined;
  vm.majorMinorVersion = clmServerVersion.split('.').splice(0, 2).join('.');

  function selectTemplate() {
    var orgId = vm.migrateSelection === 'selectOrganization' ? vm.organization.id : null,
        url = CLMLocations.getRootOrganizationConfigMigrationUrl(orgId);
    delete vm.error;
    $http.post(url).then(function() {
      if (vm.migrateSelection === 'selectOrganization') {
        $scope.$close();
      }
      else {
        vm.waitingForReload = true;
        $timeout(reloadApp, 5000);
      }
    }, function(error) {
      vm.error = Messages.getHttpErrorMessage(error);
    });
  }

  function doLoad() {
    delete vm.loadError;
    OrganizationStore.refresh().then(function(data) {
      vm.organizations = data;
    }, function(error) {
      vm.loadError = Messages.getHttpErrorMessage(error);
    });
  }

  function reloadApp() {
    $window.location.reload();
  }

  doLoad();
}

RootOrganizationMigrateModalController.$inject =
    ['Messages', 'OrganizationStore', 'CLMLocations', '$http', '$scope', '$timeout', '$window'];

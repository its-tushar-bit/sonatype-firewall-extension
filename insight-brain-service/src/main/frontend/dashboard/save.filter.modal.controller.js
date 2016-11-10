/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SaveFilterModalController($scope, $http, CLMLocations, filterJson, filterName, existingFilters, $timeout, Messages)
  {
    var vm = this,
        confirmed = false;

    vm.confirm = false;
    vm.formMask = undefined;
    vm.saveError = undefined;
    vm.filterName = filterName;
    vm.saveFilter = saveFilter;
    vm.doSave = doSave;

    $scope.$watch('vm.filterName', function() {
      confirmed = false;
    });

    function saveFilter(newConfirmed) {
      var duplicate = existingFilters.some(function(filter) {
        return vm.filterName === filter.name;
      });

      confirmed = confirmed || newConfirmed;
      vm.confirm = false;

      if (duplicate && !confirmed) {
        vm.confirm = true;
      }
      else {
        doSave();
      }
    }

    function doSave() {
      var namedFilter = {
        name: vm.filterName,
        filter: filterJson
      };
      // we do this asynchronously because the confirmation may have been displayed
      $timeout(function() {
        vm.formMask.wrap($http.put(CLMLocations.getDashboardSavedFilters(), namedFilter)).then(function() {
          $scope.$close(namedFilter.name);
        }, function(error) {
          vm.saveError = Messages.getHttpErrorMessage(error);
        });
      }, 0);
    }
  }

  SaveFilterModalController.$inject = ['$scope', '$http', 'CLMLocations', 'filterJson', 'filterName',
      'existingFilters', '$timeout', 'Messages'];

  angular.module('dashboard.module').controller('save.filter.modal.controller', SaveFilterModalController);

}(angular));

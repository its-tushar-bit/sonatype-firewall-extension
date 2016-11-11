/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DeleteFiltersModalController($scope, $http, CLMLocations, savedNamedFilters, Messages, DeleteModalService)
  {
    var vm = this,
        originalFilters;
    vm.deleteError = undefined;
    vm.deleteFilters = deleteFilters;
    vm.filters = undefined;
    vm.doLoad = doLoad;
    vm.isDirty = isDirty;
    vm.deleteMode = false;
    vm.unsavedModalVisible = false;
    vm.isLoading = false;
    vm.isArray = angular.isArray;

    $scope.$on('pageChangeStarted', function(event) {
      if (vm.isDirty()) {
        vm.unsavedModalVisible = true;
        event.preventDefault();
      }
    });

    $scope.$on('pageChangeCanceled', function() {
      vm.unsavedModalVisible = false;
    });

    $scope.$on('pageChangeAccepted', function() {
      $scope.$dismiss();
    });

    vm.doLoad();

    function doLoad() {
      var previousFilters = angular.copy(vm.filters);
      vm.filters = {};
      savedNamedFilters.forEach(function(filter) {
        vm.filters[filter.name] = previousFilters && previousFilters[filter.name] || false;
      });
      originalFilters = originalFilters || angular.copy(vm.filters);
    }

    function deleteFilters() {
      var filtersToDelete = Object.keys(vm.filters).filter(function(filter) {
        return vm.filters[filter] === true;
      });

      if (filtersToDelete && filtersToDelete.length > 0) {
        vm.isLoading = true;
        vm.deleteMode = true;
        DeleteModalService.deleteCustom('Delete Filters',
            'You are about to remove ' + filtersToDelete.length + ' filter(s). This action cannot be undone.', 'Removing',
            function() {
              return $http.post(CLMLocations.getDashboardDeleteFiltersUrl(), filtersToDelete);
            }, true).then(function() {
          $scope.$close();
        }, function(error) {
          error = Messages.getHttpErrorMessage(error);
          if (error) {
            if (angular.isArray(error)) {
              vm.deleteError =  error.map(function(err) {
                return 'Filter ' + err.name + ', ' + err.errorMessage;
              });
            }
            else {
              vm.deleteError = [error];
            }
          }
          vm.deleteMode = false;
          refreshSavedFilters();
        });
      }
    }

    function isDirty() {
      return !angular.equals(originalFilters, vm.filters);
    }

    function refreshSavedFilters() {
      $http.get(CLMLocations.getDashboardSavedFilters()).then(function(response) {
        savedNamedFilters = response.data;
        doLoad();
      }).finally(function() {
        vm.isLoading = false;
      });
    }
  }

  DeleteFiltersModalController.$inject = [
    '$scope', '$http', 'CLMLocations', 'savedNamedFilters', 'Messages', 'DeleteModalService'
  ];

  angular //
      .module('dashboard.module') //
      .controller('delete.filters.modal.controller', DeleteFiltersModalController);

}(angular));

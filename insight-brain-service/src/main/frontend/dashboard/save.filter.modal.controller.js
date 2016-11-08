/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SaveFilterModalController($scope, $http, CLMLocations, filterJson, filterName, Messages)
  {
    var vm = this;
    vm.formMask = undefined;
    vm.saveError = undefined;
    vm.filterName = filterName;
    vm.saveFilter = saveFilter;

    function saveFilter() {
      var namedFilter = {
        name: vm.filterName,
        filter: filterJson
      };
      vm.formMask.wrap($http.put(CLMLocations.getDashboardSavedFilters(), namedFilter)).then(function() {
        $scope.$close(namedFilter.name);
      }, function(error) {
        vm.saveError = Messages.getHttpErrorMessage(error);
      });
    }
  }

  SaveFilterModalController.$inject = ['$scope', '$http', 'CLMLocations', 'filterJson', 'filterName', 'Messages'];

  angular.module('dashboard.module').controller('save.filter.modal.controller', SaveFilterModalController);

}(angular));

/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LabelTileController($scope, $http, CLMAppLocations, SameOwnerStateNavigationService) {
    var vm = this;
    vm.ownerName = undefined;
    vm.applicableLabels = undefined;
    vm.error = undefined;
    vm.doLoad = doLoad;
    vm.editLabel = editLabel;

    vm.doLoad();

    $scope.$on('policy.imported', doLoad);

    function doLoad() {
      $http.get(CLMAppLocations.getApplicableLabelsUrl()).then(function(result) {
        vm.applicableLabels = result.data.labelsByOwner;
        vm.applicableLabels.forEach(function(labels, index) {
          labels.inherited = index > 0;
        });

        vm.ownerName = vm.applicableLabels[0].ownerName;
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    function editLabel(labelId, inherited) {
      if (!inherited) {
        SameOwnerStateNavigationService.goEdit('label', {labelId: labelId});
      }
    }
  }

  LabelTileController.$inject = ['$scope', '$http', 'CLMAppLocations', 'SameOwnerStateNavigationService'];

  angular
      .module('owner.manager.module')
      .controller('LabelTileController', LabelTileController);
}(angular));

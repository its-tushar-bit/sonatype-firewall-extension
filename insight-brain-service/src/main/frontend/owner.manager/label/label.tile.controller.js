/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LabelTileController($http, CLMAppLocations) {
    var vm = this;
    vm.ownerName = undefined;
    vm.applicableLabels = undefined;
    vm.error = undefined;
    vm.doLoad = doLoad;

    vm.doLoad();

    function doLoad() {
      $http.get(CLMAppLocations.getApplicableLabelsUrl()).then(function(result) {
        vm.applicableLabels = result.data.labelsByOwner;
        vm.applicableLabels.forEach(function(labels, index) {
          labels.inherited = index > 0;
        });

        vm.ownerName = vm.applicableLabels[0].ownerName;
      }, function() {
        vm.error = arguments;
      });

      delete vm.error;
    }
  }

  LabelTileController.$inject = ['$http', 'CLMAppLocations'];

  angular
      .module('owner.manager.module')
      .controller('LabelTileController', LabelTileController);
}(angular));

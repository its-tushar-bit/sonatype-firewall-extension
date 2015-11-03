/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerDetailTreeViewController($scope, $http, $state, CLMAppLocations) {
    var vm = this;

    vm.state = $state;
    vm.details = undefined;
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.categoryState = {isExpanded: vm.state.$current.name.endsWith('category')};
    vm.labelState = {isExpanded: vm.state.$current.name.endsWith('label')};

    vm.doLoad();

    function doLoad() {
      $http.get(CLMAppLocations.getOwnerDetailsUrl()).then(function(details) {
        vm.details = details.data;
      }, function() {
        vm.error = arguments;
      });

      delete vm.error;
    }

    $scope.$on('resource.data.modified', vm.doLoad);
  }

  OwnerDetailTreeViewController.$inject = ['$scope', '$http', '$state', 'CLMAppLocations'];

  angular //
      .module('owner.manager.module') //
      .controller('OwnerDetailTreeViewController', OwnerDetailTreeViewController);
}(angular));

/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DeleteModalController($scope, Messages, FormMaskDelay, resourceType, resourceName, resource, saveOnDelete) {
    var vm = this;

    vm.deleteResource = deleteResource;
    vm.error = undefined;
    vm.resourceName = resourceName;
    vm.resourceType = resourceType;
    vm.saveOnDelete = saveOnDelete;

    function deleteResource() {
      FormMaskDelay.wrap($scope, resource[saveOnDelete ? '$save' : '$delete']()).then(function() {
        $scope.$close();
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    }
  }

  DeleteModalController.$inject = ['$scope', 'Messages', 'FormMaskDelay', 'resourceType', 'resourceName', 'resource', 'saveOnDelete'];

  angular //
      .module('utility') //
      .controller('DeleteModalController', DeleteModalController);

}(angular));

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function ProprietaryMatchersModalController($scope, ownerAppId, pathNames, ProprietaryMatchersService)
  {
    var vm = this;

    vm.addMatchersForm = undefined;
    vm.appId = ownerAppId;
    vm.pathNames = pathNames;
    vm.selectedPathNames = angular.copy(pathNames);
    vm.error = undefined;
    vm.formMask = undefined;
    vm.basePath = CLM.path;
    vm.regex = undefined;

    vm.isSelected = isSelected;
    vm.toggleSelected = toggleSelected;
    vm.isValid = isValid;
    vm.save = save;

    function isSelected(path) {
      return vm.selectedPathNames.indexOf(path) >= 0;
    }

    function toggleSelected(path) {
      var index = vm.selectedPathNames.indexOf(path);
      if (index >= 0) {
        vm.selectedPathNames.splice(index, 1);
      }
      else {
        vm.selectedPathNames.push(path);
      }
    }
    
    function isValid() {
      return vm.selectedPathNames.length || vm.regex;
    }

    function save() {
      if (!isValid()) {
        return;
      }

      vm.formMask.wrap(ProprietaryMatchersService.addComponentMatchers(ownerAppId, vm.selectedPathNames, vm.regex))
          .then(function() {
            $scope.$close();
            delete vm.error;
          }, function(error) {
            vm.error = error;
          });
    }

  }

  ProprietaryMatchersModalController.$inject = [
    '$scope', 'ownerAppId', 'pathNames', 'proprietary.matchers.service'
  ];

  angular //
      .module('proprietary.matchers') //
      .controller('proprietary.matchers.modal.controller', ProprietaryMatchersModalController);

})();


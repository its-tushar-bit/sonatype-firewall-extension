/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getComponentName } from '../util/componentNameUtils';

import template from './componentDisplay.html';
import isFilenameOrUnknown from './isFilenameOrUnknown';

export default {
  controllerAs: 'vm',
  bindings: {
    component: '<',
    truncate: '<',
  },
  template,
  controller: ComponentDisplayController,
};

function ComponentDisplayController($scope, OwnerContext) {
  const vm = this;

  Object.assign(vm, {
    displayName: undefined,
    filename: undefined,

    $onInit() {
      $scope.$watchGroup(
        [
          'vm.component.displayName',
          'vm.component.filename',
          'vm.component.filenames',
          'vm.component.componentDisplayText',
        ],
        vm.updateDisplay
      );
      vm.updateDisplay();
    },

    updateDisplay() {
      if (vm.component) {
        if (OwnerContext.ownerType === 'repository') {
          vm.componentName = vm.component.componentDisplayText;
          vm.isFilenameOrUnknown = false;
        } else {
          vm.componentName = getComponentName(vm.component);
          vm.isFilenameOrUnknown = isFilenameOrUnknown(vm.component);
        }
        vm.ownerApplicationName = vm.component.ownerApplicationName || null;
        vm.innerSourceTDIndicator = vm.component.innerSourceTDIndicator;
        vm.dependencyType = vm.component.dependencyType || null;
      }
    },
  });
}

ComponentDisplayController.$inject = ['$scope', 'OwnerContext'];

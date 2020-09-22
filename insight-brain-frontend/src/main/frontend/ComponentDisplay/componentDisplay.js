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
    truncate: '<'
  },
  template,
  controller: ComponentDisplayController
};

function ComponentDisplayController($scope) {
  const vm = this;

  Object.assign(vm, {
    displayName: undefined,
    filename: undefined,

    $onInit() {
      $scope.$watchGroup(['vm.component.displayName', 'vm.component.filename', 'vm.component.filenames'],
          vm.updateDisplay);
      vm.updateDisplay();
    },

    updateDisplay() {
      vm.componentName = getComponentName(vm.component);
      vm.ownerApplicationName = vm.component.ownerApplicationName || null;
      vm.innerSourceIndicator = vm.component.innerSourceIndicator;
      vm.dependencyType = vm.component.dependencyType || null;
      vm.isFilenameOrUnknown = isFilenameOrUnknown(vm.component);
    }
  });
}

ComponentDisplayController.$inject = ['$scope'];

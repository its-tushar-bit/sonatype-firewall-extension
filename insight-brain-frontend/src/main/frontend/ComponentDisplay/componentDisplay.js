/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getComponentName } from '../util/componentNameUtils';

import template from './componentDisplay.html';

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
      const { displayName } = vm.component;

      vm.componentName = getComponentName(vm.component);
      vm.isDisplayName = !!displayName;
    }
  });
}

ComponentDisplayController.$inject = ['$scope'];

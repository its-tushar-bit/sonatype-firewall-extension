/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { join, map, pipe, prop, replace } from 'ramda';

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
      const { displayName, filename, filenames } = vm.component;

      vm.displayName = displayName && formatComponentDisplayName(displayName);
      vm.filename = filename || (filenames && join(', ', filenames));
    }
  });
}

ComponentDisplayController.$inject = ['$scope'];

// NOTE: You can't see it, but we are replacing the periods with a period followed by a zero-width space.
// This makes our periods into word breaking delimiters. Also, we only replace the periods in between words as
// to preserve version numbers.
const addWordBreakAfterPeriods = replace(/(?=\.\D+)\.(?=\D+)/g, '.​'),
    formatComponentDisplayName = pipe(
        prop('parts'),
        map(prop('value')),
        join(''),
        addWordBreakAfterPeriods
    );

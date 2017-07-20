/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

var iqBackButtonComponent = {
  templateUrl: 'components/iqBackButton/iqBackButton.html?' + clmBuildTimestamp,
  controller: controller,
  controllerAs: 'vm',
  bindings: {
    stateName: '@state'
  }
};

function controller($state) {
  var vm = this;
  vm.linkText = undefined;

  var stateObj = $state.get(this.stateName);
  if (stateObj) {
    if (stateObj.data && stateObj.data.title) {
      vm.linkText = 'Back to ' + stateObj.data.title;
    }
    else {
      vm.linkText = 'Back';
    }
  }
  else {
    throw new Error('Failed to display iq-back-button, provided state does not exist: ' + this.stateName);
  }
}

controller.$inject = ['$state'];

angular.module('components').component('iqBackButton', iqBackButtonComponent);

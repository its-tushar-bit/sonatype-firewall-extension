/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './components.html';
import resultsControllerMixinFactory from '../resultsControllerMixinFactory';

export default {
  template,
  controller: componentsController,
  controllerAs: 'vm'
};

const RESULTS_TYPE = 'components';

function componentsController($ngRedux, dashboardDataService, $scope, actions) {
  const vm = this;

  Object.assign(vm, resultsControllerMixinFactory($ngRedux, dashboardDataService, $scope, actions, RESULTS_TYPE), {
    goToComponentDetails(component) {
      vm.stateGo('dashboard.component', {hash: component.hash});
    }
  });
}

componentsController.$inject = [
  '$ngRedux', 'dashboard.data.service', '$scope', 'dashboardResultsActions'
];

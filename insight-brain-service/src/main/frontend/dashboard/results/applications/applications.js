/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './applications.html';
import resultsControllerMixinFactory from '../resultsControllerMixinFactory';

export default {
  template,
  controller: applicationsController,
  controllerAs: 'vm'
};

const RESULTS_TYPE = 'applications';

function applicationsController($ngRedux, dashboardDataService, $scope, actions) {
  const vm = this;

  Object.assign(vm, resultsControllerMixinFactory($ngRedux, dashboardDataService, $scope, actions, RESULTS_TYPE), {
    encodeURIComponent: window.encodeURIComponent
  });
}

applicationsController.$inject = [
  '$ngRedux', 'dashboard.data.service', '$scope', 'dashboardResultsActions'
];

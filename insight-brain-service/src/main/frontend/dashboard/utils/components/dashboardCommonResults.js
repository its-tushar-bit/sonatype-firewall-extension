/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import template from './dashboardCommonResults.html';

var dashboardCommonResults = {
  bindings: {
    results: '<',
    needsAcknowledgement: '<',
    maxResults: '<',
    maxDaysOld: '<',
    error: '<',
    reload: '&'
  },
  controllerAs: 'vm',
  controller: DashboardCommonResultsController,
  template: template,
  replace: true
};

function DashboardCommonResultsController($state) {
  var vm = this;

  vm.isViolationsState = isViolationsState;
  vm.loadCommonResults = loadCommonResults;

  function isViolationsState() {
    return $state.is('dashboard.overview.violations');
  }

  function loadCommonResults() {
    return !vm.results || vm.results.length === 0 || vm.results.length > vm.maxResults || vm.needsAcknowledgement;
  }
}

DashboardCommonResultsController.$inject = ['$state'];

export default dashboardCommonResults;

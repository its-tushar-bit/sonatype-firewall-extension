/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';

  function RepositoryReportController($http, $stateParams, CLMLocations) {
    var vm = this;
    
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.repository = null;
    vm.reportUrl = CLMLocations.getRepositoryReportUrl($stateParams.repositoryId);

    vm.doLoad();

    function doLoad() {
      delete vm.error;
      $http.get(CLMLocations.getRepositoryInfoUrl($stateParams.repositoryId)).success(function (repository) {
        vm.repository = repository;
      }).error(function () {
        vm.error = arguments;
      });
    }
  }
  RepositoryReportController.$inject = ['$http', '$stateParams', 'CLMLocations'];

  angular.module('Report').controller('repository.report.controller', RepositoryReportController);
}());

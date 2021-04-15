/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import utilityDirectiveModule from '../utility/directives/utility.directives.module';

var reportViolationsModule = angular.module('ReportViolations', [
  angularCommonModule.name,
  CLMLocationModule.name,
  utilityDirectiveModule.name,
  'vs-repeat',
]);

export default reportViolationsModule;

reportViolationsModule.controller('ReportViolationsController', [
  '$scope',
  '$http',
  '$q',
  'CLMLocations',
  '$filter',
  function ($scope, $http, $q, clmLocations) {
    const vm = this;
    const RESULTS_PER_PAGE = 50;

    let pages = 1;

    vm.encodeURIComponent = window.encodeURIComponent;
    vm.appFilter = '';
    vm.sortFields = ['name'];
    vm.applications = [];
    vm.hasMoreResults = true;

    vm.applicationHasViolationsForStage = function (application, stage) {
      const stageTypeId = stage.stageTypeId,
        results = application.policyEvaluationsResults,
        counts = results[stageTypeId];

      return !!(counts.criticalComponentCount + counts.severeComponentCount + counts.moderateComponentCount);
    };

    vm.doLoad = function () {
      vm.error = null;

      $http.get(clmLocations.getActionStageUrl()).then(
        function (results) {
          vm.stages = results.data;
        },
        function (error) {
          vm.error = error;
        }
      );
      getResults();
    };
    vm.doLoad();

    vm.sortAndFilter = function () {
      pages = 1;
      vm.hasMoreResults = true;
      getResults();
    };

    vm.loadMoreResults = function () {
      pages++;
      getResults();
    };

    vm.sortChange = function (sortFields) {
      vm.sortFields = sortFields;
      vm.sortAndFilter();
    };

    function getResults() {
      // Reset assuming there will be results to avoid empty space in table when going from no results to some results
      vm.noReports = false;
      vm.showReports = true;

      vm.loadingApps = true;
      if (pages === 1) {
        vm.applications.length = 0;
      }
      $http
        .get(clmLocations.getApplicationSummariesUrl(vm.appFilter, getOrder(), pages, RESULTS_PER_PAGE))
        .then(
          function (results) {
            vm.hasMoreResults = results.data.length === RESULTS_PER_PAGE;
            vm.applications.push.apply(vm.applications, results.data);
            vm.noReports = vm.applications.length === 0;
            vm.showReports = vm.applications.length > 0;
          },
          function (error) {
            vm.error = error;
          }
        )
        .finally(function () {
          vm.loadingApps = false;
        });
    }

    function getOrder() {
      let sort = vm.sortFields[0];
      switch (sort) {
        case 'name':
          return 'APP_NAME_ASC';
        case '-name':
          return 'APP_NAME_DESC';
        case 'organizationName':
          return 'ORG_NAME_ASC';
        case '-organizationName':
          return 'ORG_NAME_DESC';
        default:
          throw new Error('invalid sort: ' + sort);
      }
    }
  },
]);

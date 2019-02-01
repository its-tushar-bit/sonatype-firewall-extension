/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import sortableDirective from './violations/sortable.directive';
import sortColumnsDirective from './violations/sortColumns.directive';

var reportViolationsModule = angular.module('ReportViolations',
    [angularCommonModule.name, CLMLocationModule.name, 'vs-repeat'])
    .directive('sortable', sortableDirective)
    .directive('sortColumns', sortColumnsDirective);

export default reportViolationsModule;

reportViolationsModule.controller('ReportViolationsController', ['$scope', '$http', '$q', 'CLMLocations', '$filter',
  function($scope, $http, $q, clmLocations, $filter) {
    const vm = this;

    let allApplications = undefined;

    const isVisible = appFilter => item => {
      return !appFilter ||
          item.name.toLowerCase().indexOf(appFilter.toLowerCase()) > -1 ||
          item.organizationName.toLowerCase().indexOf(appFilter.toLowerCase()) > -1;
    };

    vm.encodeURIComponent = window.encodeURIComponent;
    vm.appFilter = '';

    vm.applicationHasViolationsForStage = function(application, stage) {
      const stageTypeId = stage.stageTypeId,
          results = application.policyEvaluationsResults,
          counts = results[stageTypeId];

      return !!(counts.criticalComponentCount + counts.severeComponentCount + counts.moderateComponentCount);
    };

    vm.doLoad = function() {
      vm.error = null;

      var promises = [];

      promises.push($http.get(clmLocations.getActionStageUrl()));
      promises.push($http.get(clmLocations.getApplicationSummariesUrl()));

      $q.all(promises).then(function(results) {
        vm.stages = results[0].data;
        allApplications = results[1].data;
        vm.noReports = allApplications.length === 0;
        vm.showReports = allApplications.length > 0;
        vm.applications = sortAndIndex(allApplications);
      }, function() {
        vm.error = arguments[0];
      });
    };
    vm.doLoad();

    $scope.$watchGroup(['vm.appFilter', 'getSortField()[0]'], () => {
      if (allApplications) {
        vm.applications = sortAndIndex(filter(allApplications));
      }
    });

    function sortAndIndex(apps) {
      return index(sort(apps));
    }

    function filter(apps) {
      return apps.filter(isVisible(vm.appFilter));
    }

    function sort(apps) {
      // getSortField is populated onto the scope by the `sortable` directive
      return $filter('orderBy')(apps, $scope.getSortField());
    }

    function index(apps) {
      return apps.map((app, index) => ({...app, index}));
    }
  }]);

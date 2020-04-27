/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window */
import { pick } from 'ramda';

import commonServicesModule from '../util/CommonServices';
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import RepositoryReEvaluateModalController from './repository.reevaluate.modal.controller';
import ReEvaluateModalService from './repository.reevaluate.service';
import RepositoryReportController from './repository.report.controller';
import appReportTemplate from './report/report.html';
import repoReportTemplate from './report/repository.report.html';

var reportModule = angular.module('Report',
    [CLMLocationModule.name, 'ui.router', angularCommonModule.name, commonServicesModule.name],
    ['$stateProvider', function($stateProvider) {
      $stateProvider.state('report', {
        url: '/reports/{publicId}/{scanId}',
        controller: 'ReportController',
        template: appReportTemplate,
        data: {
          title: 'Report'
        }
      });
      $stateProvider.state('repository-report', {
        url: '/repository/{repositoryId}/result',
        controller: 'repository.report.controller',
        controllerAs: 'vm',
        template: repoReportTemplate,
        data: {
          title: 'Repository Results'
        }
      });
    }])
    .controller('repository.reevaluate.modal.controller', RepositoryReEvaluateModalController)
    .service('ReEvaluateModal', ReEvaluateModalService)
    .controller('repository.report.controller', RepositoryReportController);

export default reportModule;

reportModule.controller('ReportController', ['$scope', '$state', '$http', '$q', 'StageTypeStore', 'CLMLocations',
  function($scope, $state, $http, $q, StageTypeStore, clmLocations) {
    $scope.doLoad = function() {
      $scope.error = null;

      $scope.reportUrl = clmLocations.getReportUrl($state.params.publicId, $state.params.scanId);
      Object.assign($scope, pick(['publicId', 'scanId'], $state.params));

      $http.get(clmLocations.getReportMetadataUrl($state.params.publicId, $state.params.scanId))
          .then(function(response) {
            var metadata = response.data;
            $scope.application = metadata.application;
            $scope.expandedCoverage = metadata.expandedCoverage;
            $scope.reportTime = metadata.reportTime;
            $scope.reportTitle = metadata.reportTitle;
          }, function(error) {
            $scope.error = error;
          });
    };
    $scope.doLoad();
  }
]);

reportModule.directive('expandableIframe', function() {
  return {
    template: '<iframe ng-src="{{url}}" width="100%" height="1000px" border="0" frameborder="0" scrolling="yes" ' +
        'style="overflow:auto;"/>',
    scope: {
      url: '=expandableIframe'
    },
    link: function(scope) {
      var resizeTimeoutId;

      function setDimensions() {
        var iframe = angular.element('iframe');
        if (!iframe || iframe.length === 0) {
          clearTimeout(resizeTimeoutId);
          return;
        }
        var windowHeight = $(window).height(),
            containerTop = iframe.offset().top,
            bottomPadding = 20,
            height = Math.max(400, windowHeight - containerTop - bottomPadding);

        iframe.css({ 'height': height + 'px' });
      }

      function dedupe() {
        clearTimeout(resizeTimeoutId);
        resizeTimeoutId = setTimeout(setDimensions, 100);
      }

      setTimeout(setDimensions, 100);
      window.onresize = dedupe;

      if (window.top.externalLinkClickHandler) {
        const iframe = angular.element('iframe');
        iframe.on('load', () => {
          iframe.contents().find('body').click(window.top.externalLinkClickHandler);
        });
      }

      scope.$on('$destroy', function() {
        clearTimeout(resizeTimeoutId);
      });
    }
  };
});

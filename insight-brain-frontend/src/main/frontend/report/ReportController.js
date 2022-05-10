/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

import commonServicesModule from '../utilAngular/CommonServices';
import angularCommonModule from '../utilAngular/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import RepositoryReEvaluateModalController from './repository.reevaluate.modal.controller';
import ReEvaluateModalService from './repository.reevaluate.service';
import RepositoryReportController from './repository.report.controller';
import appReportTemplate from './report/report.html';
import repoReportTemplate from './report/repository.report.html';

var reportModule = angular
  .module(
    'Report',
    [CLMLocationModule.name, 'ui.router', angularCommonModule.name, commonServicesModule.name],
    [
      '$stateProvider',
      function ($stateProvider) {
        $stateProvider.state('report', {
          url: '/reports/{publicId}/{scanId}',
          controller: 'ReportController',
          template: appReportTemplate,
          data: {
            title: 'Report',
          },
        });
        $stateProvider.state('repository-report', {
          url: '/repository/{repositoryId}/result',
          controller: 'repository.report.controller',
          controllerAs: 'vm',
          template: repoReportTemplate,
          data: {
            title: 'Repository Results',
          },
        });
      },
    ]
  )
  .controller('repository.reevaluate.modal.controller', RepositoryReEvaluateModalController)
  .service('ReEvaluateModal', ReEvaluateModalService)
  .controller('repository.report.controller', RepositoryReportController);

export default reportModule;

reportModule.controller('ReportController', [
  '$scope',
  '$state',
  '$http',
  '$q',
  'CLMLocations',
  function ($scope, $state, $http, $q, clmLocations) {
    $scope.doLoad = function () {
      $scope.error = null;

      $scope.reportUrl = clmLocations.getReportUrl($state.params.publicId, $state.params.scanId);
      Object.assign($scope, pick(['publicId', 'scanId'], $state.params));

      $http.get(clmLocations.getReportMetadataUrl($state.params.publicId, $state.params.scanId)).then(
        function (response) {
          var metadata = response.data;
          $scope.application = metadata.application;
          $scope.reportTime = metadata.reportTime;
          $scope.reportTitle = metadata.reportTitle;
        },
        function (error) {
          $scope.error = error;
        }
      );
    };
    $scope.doLoad();
  },
]);

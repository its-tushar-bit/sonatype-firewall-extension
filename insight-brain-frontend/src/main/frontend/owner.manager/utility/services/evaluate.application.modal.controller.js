/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectIsNotificationsSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function EvaluateApplicationModalController(
  $rootScope,
  $scope,
  $http,
  $state,
  $timeout,
  $window,
  $cookies,
  $q,
  Messages,
  CLMLocations,
  selectedApplication,
  StageTypeStore,
  $ngRedux
) {
  var validEvaluateBundleStages = ['build', 'stage-release', 'release', 'operate'],
    vm = this;

  vm.bundle = undefined;
  vm.csrfTokenName = $http.defaults.xsrfHeaderName;
  vm.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);
  vm.doSubmit = doSubmit;
  vm.error = undefined;
  vm.evaluationState = undefined;
  vm.evaluationStatus = undefined;
  vm.isFormValid = isFormValid;
  vm.getProgressWidth = getProgressWidth;
  vm.openReport = openReport;
  vm.pollingUrl = undefined;
  vm.retry = doLoad;
  vm.stages = [];
  vm.uploadBundleUrl = uploadBundleUrl;
  vm.isNotificationsSupported = undefined;

  doLoad();

  var reportListener = $scope.$watch(
    function () {
      return vm.evaluationStatus && vm.evaluationStatus.scanId;
    },
    function (isReportReady) {
      if (isReportReady) {
        reportListener();
        $rootScope.$broadcast('reload.app.report.data');
      }
    }
  );

  $scope.$on('$destroy', function () {
    reportListener();
  });

  $scope.$on('pageChangeAccepted', function () {
    $scope.$dismiss();
  });

  function doLoad() {
    vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
      loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
    })(vm);

    vm.evaluationState = 'loading';
    vm.bundle = {
      notify: 'true',
      applicationPublicId: selectedApplication ? selectedApplication.publicId : null,
      applicationName: selectedApplication ? selectedApplication.name : null,
    };

    if (!vm.bundle.applicationPublicId) {
      vm.evaluationState = 'ready';
      return setError('Cannot find the associated Application', doLoad);
    }

    const promises = [StageTypeStore.get(), vm.loadProductFeatures()];

    $q.all(promises).then(
      function (results) {
        unwrapResult(results[1]);
        vm.evaluationState = 'ready';

        results[0].forEach(function (stage) {
          if (validEvaluateBundleStages.indexOf(stage.stageTypeId) > -1) {
            vm.stages.push(stage);
          }
        });
      },
      function (error) {
        vm.evaluationState = 'ready';
        setError(Messages.getHttpErrorMessage(error), doLoad);
      }
    );
  }

  function doPoll() {
    if (!$scope.$$destroyed) {
      $http.get(vm.pollingUrl).then(
        function (response) {
          vm.evaluationStatus = response.data;
          if (vm.evaluationStatus.error) {
            setError(vm.evaluationStatus.error, doSubmit);
          } else if (vm.evaluationStatus.currentStep < vm.evaluationStatus.totalSteps) {
            $timeout(doPoll, 500);
          }
        },
        function (error) {
          setError(Messages.getHttpErrorMessage(error), doSubmit);
        }
      );
    }
  }

  function doSubmit() {
    var fileElement = angular.element('#bundle-file')[0];
    vm.evaluationState = 'polling';
    vm.evaluationStatus = {
      currentStep: 1,
      totalSteps: 1,
      currentStepName: 'Uploading',
    };
    vm.bundle.filename = parseFilename(fileElement.value);
    vm.error = null;
    vm.pollingUrl = null;

    var form = new FormData();
    form.append('file', fileElement.files[0]);
    // Explicitly add the filename as a form parameter since there is an encoding mismatch between the browsers and
    // server in the Content-Disposition filename header.
    form.append('filename', fileElement.files[0].name);
    $http.post(vm.uploadBundleUrl(), form).then(
      function (response) {
        vm.pollingUrl = CLMLocations.getEvaluationStatusUrl(vm.bundle.applicationPublicId, response.data.ticketId);
        doPoll();
      },
      function (errorResponse) {
        vm.evaluationStatus.error = Messages.getHttpErrorMessage(errorResponse);
        vm.evaluationStatus.currentStepName = 'Done';
        setError(vm.evaluationStatus.error, doSubmit);
      }
    );
  }

  function getProgressWidth() {
    return vm.evaluationStatus ? (vm.evaluationStatus.currentStep / vm.evaluationStatus.totalSteps) * 100 : '0';
  }

  function isFormValid() {
    return Boolean(
      vm.bundle &&
        vm.bundle.file &&
        vm.bundle.applicationPublicId &&
        vm.bundle.stage &&
        vm.bundle.stage.stageTypeId &&
        vm.bundle.notify
    );
  }

  function openReport() {
    if (vm.evaluationStatus && vm.evaluationStatus.scanId) {
      $window.open(
        $state.href('applicationReport.policy', {
          publicId: vm.evaluationStatus.applicationPublicId,
          scanId: vm.evaluationStatus.scanId,
        }),
        '_blank'
      );
    }
  }

  function parseFilename(filename) {
    var idx = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));

    if (idx > -1) {
      return filename.substring(idx + 1);
    }

    return filename;
  }

  function setError(message, retryFunction) {
    vm.retry = retryFunction ? retryFunction : vm.retry;

    if (message) {
      vm.error = message;
    } else {
      vm.error = 'Error uploading, please check the file.';
    }
  }

  function uploadBundleUrl() {
    if (isFormValid()) {
      return CLMLocations.getBundleUploadUrl(
        vm.bundle.applicationPublicId,
        vm.bundle.stage.stageTypeId,
        vm.bundle.notify
      );
    }
  }
}

const mapStateToThis = (state) => ({
  isNotificationsSupported: selectIsNotificationsSupported(state),
});

EvaluateApplicationModalController.$inject = [
  '$rootScope',
  '$scope',
  '$http',
  '$state',
  '$timeout',
  '$window',
  '$cookies',
  '$q',
  'Messages',
  'CLMLocations',
  'selectedApplication',
  'StageTypeStore',
  '$ngRedux',
];

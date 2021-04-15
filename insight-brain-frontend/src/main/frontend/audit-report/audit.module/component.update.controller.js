/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global Brain */
export default function ComponentUpdateController(
  $scope,
  $rootScope,
  $http,
  $q,
  Messages,
  OwnerContext,
  componentKey,
  reevaluate
) {
  var vm = this;

  vm.error = null;
  vm.doProcess = doProcess;
  vm.reevaluated = !reevaluate;

  doProcess();

  function doProcess() {
    if (!vm.reevaluated) {
      doReevaluate();
    } else {
      updateComponent();
    }
  }

  function updateComponent() {
    delete vm.error;

    // emit an event
    var promises = [];
    $rootScope.$broadcast('component.evaluation.updated', componentKey, promises);
    $q.all(promises).then(
      function () {
        $scope.$dismiss();
      },
      function (error) {
        vm.error = Messages.getHttpErrorMessage(error);
      }
    );
  }

  function doReevaluate() {
    delete vm.error;

    $http.post(Brain.getComponentReevaluationUrl(OwnerContext, componentKey.hash)).then(
      function () {
        vm.reevaluated = true;
        updateComponent();
      },
      function (error) {
        vm.error = Messages.getHttpErrorMessage(error);
      }
    );
  }
}
ComponentUpdateController.$inject = [
  '$scope',
  '$rootScope',
  '$http',
  '$q',
  'Messages',
  'OwnerContext',
  'componentKey',
  'reevaluate',
];

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function DeleteModalReduxController(
  $scope,
  $ngRedux,
  Messages,
  resourceType,
  resourceName,
  headerText,
  bodyText,
  maskText,
  continueAction,
  stateMapper
) {
  var vm = this;

  vm.deleteResource = continueAction;
  vm.deleteResourceMask = undefined;
  vm.error = undefined;
  vm.resourceName = resourceName;
  vm.resourceType = resourceType;
  vm.headerText = headerText;
  vm.bodyText = bodyText;
  vm.maskText = maskText;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);

  $scope.$on('pageChangeAccepted', function () {
    $scope.$dismiss();
  });

  $scope.$on('$destroy', vm.unsubscribe);

  $scope.$watchGroup(
    ['vm.deleting', 'vm.success'],
    function ([deleting, success]) {
      if (success) {
        vm.deleteResourceMask.showSuccessMaskBriefly().then(function () {
          $scope.$close();
        });
      } else {
        vm.deleteResourceMask[deleting ? 'activateMask' : 'removeMask']();
      }
    }
  );

  function mapStateToThis(state) {
    const mappedState = stateMapper(state);

    return Object.assign(
      { error: Messages.getHttpErrorMessage(mappedState.errorState) },
      mappedState
    );
  }
}

DeleteModalReduxController.$inject = [
  '$scope',
  '$ngRedux',
  'Messages',
  'resourceType',
  'resourceName',
  'headerText',
  'bodyText',
  'maskText',
  'continueAction',
  'stateMapper',
];

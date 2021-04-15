/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function DeleteModalController(
  $scope,
  Messages,
  resourceType,
  resourceName,
  resource,
  headerText,
  bodyText,
  maskText,
  continueAction,
  dismissOnError
) {
  var vm = this;

  vm.deleteResource = deleteResource;
  vm.deleteResourceMask = undefined;
  vm.error = undefined;
  vm.resourceName = resourceName;
  vm.resourceType = resourceType;
  vm.headerText = headerText;
  vm.bodyText = bodyText;
  vm.maskText = maskText;

  $scope.$on('pageChangeAccepted', function () {
    $scope.$dismiss();
  });

  function deleteResource() {
    vm.deleteResourceMask
      .wrap(continueAction ? continueAction() : resource.$delete())
      .then(
        function () {
          $scope.$close();
        },
        function (error) {
          if (dismissOnError === true) {
            $scope.$dismiss(error);
          } else {
            vm.error = Messages.getHttpErrorMessage(error);
          }
        }
      );
  }
}

DeleteModalController.$inject = [
  '$scope',
  'Messages',
  'resourceType',
  'resourceName',
  'resource',
  'headerText',
  'bodyText',
  'maskText',
  'continueAction',
  'dismissOnError',
];

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function UpdateSourceControlModalController($scope, Messages, continueAction,
                                                           dismissOnError) {
  var vm = this;

  vm.updateSourceControl = updateSourceControl;
  vm.updateSourceControlMask = undefined;
  vm.error = undefined;

  $scope.$on('pageChangeAccepted', function() {
    $scope.$dismiss();
  });

  function updateSourceControl() {
    vm.updateSourceControlMask.wrap(continueAction()).then(function() {
      $scope.$close();
    }, function(error) {
      if (dismissOnError === true) {
        $scope.$dismiss(error);
      }
      else {
        vm.error = Messages.getHttpErrorMessage(error);
      }
    });
  }
}

UpdateSourceControlModalController.$inject = [
  '$scope', 'Messages', 'continueAction', 'dismissOnError'
];

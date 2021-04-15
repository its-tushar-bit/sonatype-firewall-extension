/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */

function getUrl(OwnerContext, component) {
  return (
    CLM.path +
    'rest/repositories/' +
    OwnerContext.ownerId +
    '/unquarantine/' +
    encodeURIComponent(component.pathname).replace(/%2F/gi, '/')
  );
}

export default function ReleaseQuarantineController($scope, $http, Messages, SelectedComponent, OwnerContext) {
  var vm = this;

  vm.activeRequest = false;
  vm.error = null;
  vm.release = release;

  function release() {
    var component = SelectedComponent.get();
    vm.activeRequest = true;

    delete vm.error;
    $http
      .post(getUrl(OwnerContext, component))
      .then(
        function () {
          $scope.$emit('reload.component', { pathname: component.pathname });
          $scope.$close();
        },
        function (error) {
          vm.error = Messages.getHttpErrorMessage(error);
        }
      )
      .finally(function () {
        vm.activeRequest = false;
      });
  }
}
ReleaseQuarantineController.$inject = ['$scope', '$http', 'Messages', 'SelectedComponent', 'OwnerContext'];

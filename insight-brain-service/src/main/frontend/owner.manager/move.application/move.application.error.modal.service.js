/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
function MoveApplicationErrorModalService($modal) {
  return {
    open: openModal
  };

  function openModal(messages) {
    return $modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      windowClass: 'clm-modal',
      controller: MoveApplicationErrorModalController,
      templateUrl: 'owner.manager/move.application/move.application.error.modal.html',
      resolve: {
        messages: function() {
          return messages;
        }
      }
    }).result;
  }
}

function MoveApplicationErrorModalController($scope, messages) {
  $scope.messages = messages;
  $scope.$on('pageChangeAccepted', function() {
    $scope.$close();
  });
}

MoveApplicationErrorModalController.$inject = ['$scope', 'messages'];

MoveApplicationErrorModalService.$inject = ['$modal'];


/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './move.application.success.modal.html';

export default
function MoveApplicationSuccessModalService(Modal) {
  return {
    open: openModal
  };

  function openModal(messages) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: MoveApplicationSuccessModalController,
      template,
      resolve: {
        messages: function() {
          return messages;
        }
      }
    }).result;
  }
}

function MoveApplicationSuccessModalController($scope, messages) {
  $scope.messages = messages;
  $scope.$on('pageChangeAccepted', function() {
    $scope.$dismiss();
  });
}

MoveApplicationSuccessModalController.$inject = ['$scope', 'messages'];

MoveApplicationSuccessModalService.$inject = ['Modal'];

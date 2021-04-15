/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './move.application.error.modal.html';

export default function MoveApplicationErrorModalService(Modal) {
  return {
    open: openModal,
  };

  function openModal(messages) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: MoveApplicationErrorModalController,
      template,
      resolve: {
        messages: function () {
          return messages;
        },
      },
    }).result;
  }
}

function MoveApplicationErrorModalController($scope, messages) {
  $scope.messages = messages;
  $scope.$on('pageChangeAccepted', function () {
    $scope.$close();
  });
}

MoveApplicationErrorModalController.$inject = ['$scope', 'messages'];

MoveApplicationErrorModalService.$inject = ['Modal'];
